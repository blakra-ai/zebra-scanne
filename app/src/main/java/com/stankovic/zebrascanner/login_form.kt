package com.stankovic.zebrascanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stankovic.zebrascanner.databinding.ActivityLoginBinding
import com.stankovic.zebrascanner.network.ApiClient
import com.stankovic.zebrascanner.network.LoginRequest
import com.stankovic.zebrascanner.network.LoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import androidx.core.content.edit
import com.google.gson.Gson
import com.stankovic.zebrascanner.network.ErrorResponse

class login_form : AppCompatActivity() {
    private val apiService by lazy {
        ApiClient.createApiService()
    }
    private lateinit var bind: ActivityLoginBinding
    private var username: String = ""
    private var password: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(bind.root)
        val prefs = this.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)
        token?.length?.let {
            if(it>100) {
                startActivity(Intent(this, ScannerActivity::class.java))
                finish()
                return
            }
        }
        Log.d("ScanReceiver", "token: $token, response: ")

        bind.btnLogin.setOnClickListener {
            username = bind.logtxt.text.toString().trim()
            password = bind.ed3.text.toString().trim()

            hideError()

            if (username.isEmpty() || password.isEmpty()) {
                showError("Please fill all fields")
                return@setOnClickListener
            }

            fetchData(username, password)
        }
    }
    private fun parseErrorMessage(resp: Response<*>): String {
        return try {
            val errorJson = resp.errorBody()?.string()
            val errorObj = Gson().fromJson(errorJson, ErrorResponse::class.java)
            errorObj.data?.message ?: "Login failed"
        } catch (e: Exception) {
            "Login failed"
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
                    val message = parseErrorMessage(resp)
                    Toast.makeText(this@login_form, message, Toast.LENGTH_LONG).show()
                    return@launch
                }

                val loginResp = resp.body()
                if (loginResp?.data == null) {
                    showError("Empty response from server")
                    return@launch
                }

                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                val token = loginResp.data.token

                prefs.edit().apply {
                    token?.let { putString("auth_token", it) }

                    apply()
                }

                Toast.makeText(this@login_form, "Login successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@login_form, ScannerActivity::class.java))
                finish()

            } catch (e: Exception) {
                showError(e.localizedMessage ?: "Unexpected error")
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
        bind.progressLogin.visibility = if (loading) View.VISIBLE else View.GONE
        bind.btnLogin.text = if (loading) "Signing in..." else "Sign in"
    }

    private fun showError(message: String) {
        bind.tvError.text = message
        bind.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        bind.tvError.visibility = View.GONE
    }
}
