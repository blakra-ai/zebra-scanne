package com.example.zebrascanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GradientCard(
    title: String,
    value: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.CenterEnd)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
fun ModernButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.materialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
