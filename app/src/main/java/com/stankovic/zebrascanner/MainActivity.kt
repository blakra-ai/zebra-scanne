package com.stankovic.zebrascanner

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stankovic.zebrascanner.domain.usecase.InitializeAuthUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Entry point activity that redirects to the scanner screen
 * with proper dependency injection using Hilt
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var initializeAuthUseCase: InitializeAuthUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize auth data in background
        CoroutineScope(Dispatchers.Default).launch {

            
            // Navigate to scanner activity
            runOnUiThread {
                startActivity(Intent(this@MainActivity, ScannerActivity::class.java))
                finish()
            }
        }
    }
}
