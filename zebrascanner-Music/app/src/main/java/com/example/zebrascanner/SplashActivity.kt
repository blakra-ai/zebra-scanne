package com.example.zebrascanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Scanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zebrascanner.ui.theme.ZebrascannerTheme
import com.example.zebrascanner.utils.TokenManager
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZebrascannerTheme {
                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(gradientBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // App Icon
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Scanner,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "ZEBRA SCANNER",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp,
                            color = Color.White
                        )
                        Text(
                            text = "PREMIUM LOGISTICS SOLUTIONS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    LaunchedEffect(Unit) {
                        delay(2000)
                        
                        val destination = if (TokenManager.isLoggedIn(this@SplashActivity)) {
                            HomeActivity::class.java
                        } else {
                            LoginActivity::class.java
                        }
                        
                        startActivity(Intent(this@SplashActivity, destination))
                        finish()
                    }
                }
            }
        }
    }
}
