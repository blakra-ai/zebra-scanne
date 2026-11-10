package com.stankovic.zebrascanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.stankovic.zebrascanner.config.AppLanguage
import com.stankovic.zebrascanner.databinding.ActivityLoginBinding
import com.stankovic.zebrascanner.network.ApiClient
import com.stankovic.zebrascanner.network.ErrorResponse
import com.stankovic.zebrascanner.network.LoginRequest
import com.stankovic.zebrascanner.network.NetworkErrors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

class login_form : AppCompatActivity() {
    private val apiService by lazy {
        ApiClient.createApiService()
    }
    private lateinit var bind: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        if (!prefs.getString("auth_token", null).isNullOrBlank()) {
            startActivity(Intent(this, ScannerActivity::class.java))
            finish()
            return
        }

        bind = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(bind.root)

        bind.btnLanguage.text = AppLanguage.supported[AppLanguage.current(this)]
        bind.btnLanguage.setOnClickListener { AppLanguage.showPicker(this) }

        bind.btnLogin.setOnClickListener { attemptLogin() }
        bind.ed3.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else false
        }
    }

    private fun attemptLogin() {
        val username = bind.logtxt.text?.toString()?.trim().orEmpty()
        val password = bind.ed3.text?.toString()?.trim().orEmpty()
        hideError()
        if (username.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.login_fill_all))
            return
        }
        fetchData(username, password)
    }

    private fun parseErrorMessage(resp: Response<*>): String {
        return try {
            val errorJson = resp.errorBody()?.string()
            val errorObj = Gson().fromJson(errorJson, ErrorResponse::class.java)
            errorObj?.data?.message ?: getString(R.string.login_failed)
        } catch (e: Exception) {
            getString(R.string.login_failed)
        }
    }

    private fun fetchData(username: String, password: String) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    apiService.login(LoginRequest(username, password))
                }

                if (!resp.isSuccessful) {
                    showError(parseErrorMessage(resp))
                    return@launch
                }

                val loginResp = resp.body()
                if (loginResp?.data == null) {
                    showError(getString(R.string.login_failed))
                    return@launch
                }

                val token = loginResp.data.token
                if (token.isNullOrBlank()) {
                    showError(loginResp.data.message ?: getString(R.string.login_no_token))
                    return@launch
                }
                getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putString("auth_token", token).apply()

                Toast.makeText(this@login_form, R.string.login_success, Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@login_form, ScannerActivity::class.java))
                finish()
            } catch (e: Exception) {
                // System exception texts are in the phone's language; show our own instead
                showError(NetworkErrors.message(this@login_form, e))
            } finally {
                showLoading(false)
            }
        }
    }

    // ---------- UI helpers ----------

    private fun showLoading(loading: Boolean) {
        bind.btnLogin.isEnabled = !loading
        bind.logtxt.isEnabled = !loading
        bind.ed3.isEnabled = !loading
        bind.btnLanguage.isEnabled = !loading
        bind.progressLogin.visibility = if (loading) View.VISIBLE else View.GONE
        bind.btnLogin.text = if (loading) "" else getString(R.string.login_sign_in)
    }

    private fun showError(message: String) {
        bind.tvError.text = message
        bind.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        bind.tvError.visibility = View.GONE
    }
}
