package com.stankovic.zebrascanner.network

import android.content.Context
import com.stankovic.zebrascanner.R
import com.stankovic.zebrascanner.config.AppLanguage
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** Turns a network exception into a message in the app language (exception texts follow the phone's language). */
object NetworkErrors {
    fun message(context: Context, e: Throwable): String {
        val res = AppLanguage.localized(context)
        return when (e) {
            is UnknownHostException -> res.getString(R.string.net_no_internet)
            is SocketTimeoutException -> res.getString(R.string.net_timeout)
            is SSLException -> res.getString(R.string.net_ssl)
            is IOException -> res.getString(R.string.net_error)
            else -> res.getString(R.string.scan_unexpected, e.javaClass.simpleName)
        }
    }
}
