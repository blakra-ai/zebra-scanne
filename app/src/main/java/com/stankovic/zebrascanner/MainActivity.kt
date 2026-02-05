package com.stankovic.zebrascanner

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// This activity previously provided the registration UI. Registration is removed;
// redirect any launches to the login screen so users can only sign in.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        prefs.edit().putString("auth_token", "token").apply()

        prefs.edit()
            .putString("user_full_name", "fullName")
            .putString("user_email", "email")
            .apply()
        startActivity(Intent(this, ScannerActivity::class.java))
        finish()
    }
}
