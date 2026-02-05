package com.stankovic.zebrascanner

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.stankovic.zebrascanner.databinding.ActivityLoginBinding
import com.stankovic.zebrascanner.network.ApiClient
import com.stankovic.zebrascanner.network.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class login_form : AppCompatActivity() {
    private lateinit var bind: ActivityLoginBinding
    private var username: String = ""
    private var password: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(bind.root)
        bind.btnLogin.setOnClickListener {
            username = bind.logtxt.text.toString()
            password = bind.ed3.text.toString()
            bind.tvError.visibility = android.view.View.GONE
            if (username.isNotEmpty() && password.isNotEmpty()) {
                showLoading(true)
                fetch_data(username)
            } else {
                bind.tvError.text = "Please fill all fields"
                bind.tvError.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun fetch_data(username: String) {
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.service.login(LoginRequest(username, password))
                }

                if (resp.code() in 200..299) {
                    val loginResp = resp.body()
                    loginResp?.token?.let { token ->
                        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                        prefs.edit().putString("auth_token", token).apply()
                    }
                    loginResp?.user?.let { userMap ->
                        val fullName = (userMap["full_name"] ?: userMap["fullname"] ?: "") as? String ?: ""
                        val email = (userMap["email"] ?: "") as? String ?: ""
                        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                        prefs.edit()
                            .putString("user_full_name", fullName)
                            .putString("user_email", email)
                            .apply()
                    }
                    Toast.makeText(this@login_form, "Login successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@login_form, ScannerActivity::class.java))
                    finish()
                    showLoading(false)
                } else {
                    val message = resp.errorBody()?.string() ?: "Login failed"
                    bind.tvError.text = message
                    bind.tvError.visibility = android.view.View.VISIBLE
                    showLoading(false)
                }
            } catch (e: Exception) {
                bind.tvError.text = e.message ?: "Unexpected error"
                bind.tvError.visibility = android.view.View.VISIBLE
                showLoading(false)
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        bind.btnLogin.isEnabled = !loading
        bind.logtxt.isEnabled = !loading
        bind.ed3.isEnabled = !loading
        bind.progressLogin.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        bind.btnLogin.text = if (loading) "Signing in..." else "Sign in"
    }
}