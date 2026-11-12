package com.stankovic.zebrascanner.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.stankovic.zebrascanner.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * In-app updates from the customer's Redmine project, where CI publishes every build
 * (solutions/ci-templates → redmine-publish.sh). Uses only Redmine's standard REST API.
 *
 * A build is offered when its file description says it is this app and this client/environment
 * ("luna-prod · 1.2.0 · apk · commit … · zebra-scanner · sha256:…") and its version is approved:
 * a release version ("zebra-scanner v1.2.0" or "zebra-scanner luna-v1.2.0") a manager has set to
 * Closed. Dev builds also follow "zebra-scanner Nightly". Before installing, the download must
 * match the published SHA-256, be this app, be newer, and be signed with the same key.
 */
object AppUpdater {
    const val APP_ID = "zebra-scanner"

    data class Update(val version: String, val fileId: Long, val fileName: String, val size: Long, val sha256: String)

    /** Why an update could not be done; the screen shows a message for each. */
    enum class Failure { NETWORK, DAMAGED, WRONG_APP, NOT_NEWER, OTHER_SIGNATURE, NO_SPACE }

    class UpdateException(val failure: Failure, cause: Throwable? = null) : Exception(failure.name, cause)

    val isConfigured: Boolean
        get() = BuildConfig.UPDATE_KEY.isNotBlank() && BuildConfig.UPDATE_PROJECT.isNotBlank()

    private val slot = "${BuildConfig.CLIENT}-${BuildConfig.ENVIRONMENT}"

    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /** The newest approved build newer than this one, or null when this is the latest. */
    suspend fun check(): Update? = withContext(Dispatchers.IO) {
        val base = "${BuildConfig.UPDATE_REDMINE_URL}/projects/${BuildConfig.UPDATE_PROJECT}"
        val versions = getJson("$base/versions.json").getAsJsonArray("versions")
            .map { it.asJsonObject }
            .associate { it.get("id").asLong to (it.get("name").asString to (it.get("status")?.asString ?: "open")) }

        getJson("$base/files.json").getAsJsonArray("files")
            .map { it.asJsonObject }
            .mapNotNull { file ->
                val meta = file.get("description")?.takeIf { !it.isJsonNull }?.asString?.split(" · ") ?: return@mapNotNull null
                if (meta.size < 6 || meta[0] != slot || meta[4] != APP_ID) return@mapNotNull null
                val sha = meta[5].removePrefix("sha256:")
                val versionId = file.getAsJsonObject("version")?.get("id")?.asLong ?: return@mapNotNull null
                val (name, status) = versions[versionId] ?: return@mapNotNull null
                if (!isApproved(name, status) || sha.length != 64) return@mapNotNull null
                Update(meta[1], file.get("id").asLong, file.get("filename").asString, file.get("filesize")?.asLong ?: 0, sha)
            }
            .filter { SemVer.parse(it.version) != null }
            .maxWithOrNull { a, b -> SemVer.parse(a.version)!!.compareTo(SemVer.parse(b.version)!!) }
            ?.takeIf { latest ->
                // dev/stage flavors append "-dev"/"-stage" to the version name
                val current = SemVer.parse(BuildConfig.VERSION_NAME.removeSuffix("-${BuildConfig.ENVIRONMENT}"))
                current == null || SemVer.parse(latest.version)!! > current
            }
    }

    private fun isApproved(versionName: String, status: String): Boolean {
        val tag = versionName.removePrefix("$APP_ID ").takeIf { versionName.startsWith("$APP_ID ") } ?: return false
        if (tag == "Nightly") return BuildConfig.ENVIRONMENT == "dev"
        // "v1.2.0" is for every client, "luna-v1.2.0" only for luna
        val forUs = Regex("""^(?:([a-z0-9]+)-)?v?\d+\.\d+\.\d+$""").matchEntire(tag)
            ?.groupValues?.get(1)?.let { it.isEmpty() || it == BuildConfig.CLIENT } ?: false
        return forUs && status == "closed"
    }

    /** Downloads into the app's cache and checks it; returns the verified APK. */
    suspend fun download(context: Context, update: Update, onProgress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // only one update at a time
        val target = File(dir, "update.apk")
        val url = "${BuildConfig.UPDATE_REDMINE_URL}/attachments/download/${update.fileId}/${Uri.encode(update.fileName)}"

        val digest = MessageDigest.getInstance("SHA-256")
        try {
            http.newCall(request(url)).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body
                val total = body.contentLength().takeIf { it > 0 } ?: update.size
                if (total > 0 && dir.usableSpace < total * 2) throw UpdateException(Failure.NO_SPACE)
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var done = 0L
                        var shown = -1
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            digest.update(buffer, 0, n)
                            done += n
                            val percent = if (total > 0) (done * 100 / total).toInt().coerceIn(0, 100) else 0
                            if (percent != shown) { shown = percent; onProgress(percent) }
                        }
                    }
                }
            }
        } catch (e: UpdateException) {
            target.delete(); throw e
        } catch (e: IOException) {
            target.delete(); throw UpdateException(Failure.NETWORK, e)
        }

        val sha = digest.digest().joinToString("") { "%02x".format(it) }
        if (!sha.equals(update.sha256, ignoreCase = true)) { target.delete(); throw UpdateException(Failure.DAMAGED) }
        verifyPackage(context, target)
        target
    }

    /** Same app, newer, and signed like the installed one; otherwise Android would refuse anyway. */
    private fun verifyPackage(context: Context, apk: File) {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
        val archive = pm.getPackageArchiveInfo(apk.path, flags)
        val installed = pm.getPackageInfo(context.packageName, flags)
        fun fail(f: Failure): Nothing { apk.delete(); throw UpdateException(f) }

        if (archive == null || archive.packageName != context.packageName) fail(Failure.WRONG_APP)
        if (versionCode(archive) <= versionCode(installed)) fail(Failure.NOT_NEWER)
        if (signatures(archive) != signatures(installed)) fail(Failure.OTHER_SIGNATURE)
    }

    @Suppress("DEPRECATION")
    private fun versionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    @Suppress("DEPRECATION")
    private fun signatures(info: PackageInfo): Set<String> {
        val sigs = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        return sigs.orEmpty().map { it.toCharsString() }.toSet()
    }

    /**
     * Hands the APK to Android's installer, which asks the user to confirm. Returns false when
     * the user first has to allow this app to install updates (Settings is opened for that).
     */
    fun install(context: Context, apk: File): Boolean {
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }

    private fun request(url: String) = Request.Builder()
        .url(url)
        .header("X-Redmine-API-Key", BuildConfig.UPDATE_KEY)
        .header("Accept", "application/json")
        .build()

    private fun getJson(url: String): JsonObject = try {
        http.newCall(request(url)).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            JsonParser.parseString(response.body.string()).asJsonObject
        }
    } catch (e: IOException) {
        throw UpdateException(Failure.NETWORK, e)
    } catch (e: RuntimeException) { // not the JSON we expect
        throw UpdateException(Failure.NETWORK, e)
    }
}

/** X.Y.Z with an optional "-dev.N"; a release is newer than its dev builds. */
data class SemVer(val major: Int, val minor: Int, val patch: Int, val dev: Int?) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int = compareValuesBy(
        this, other, { it.major }, { it.minor }, { it.patch }, { it.dev ?: Int.MAX_VALUE }
    )

    companion object {
        private val PATTERN = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:-dev\.(\d+))?$""")

        fun parse(text: String): SemVer? = PATTERN.matchEntire(text.trim())?.destructured?.let { (a, b, c, d) ->
            SemVer(a.toInt(), b.toInt(), c.toInt(), d.toIntOrNull())
        }
    }
}
