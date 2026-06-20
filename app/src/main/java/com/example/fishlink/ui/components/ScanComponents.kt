package com.example.fishlink.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fishlink.ui.viewmodel.AgenticShieldViewModel
import com.example.fishlink.ui.viewmodel.ScanState

@Composable
fun ScanScreen(viewModel: AgenticShieldViewModel, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "FishLink",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Advanced URL Analysis",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            when (viewModel.currentState) {
                ScanState.IDLE -> IdleStateContent(viewModel)
                ScanState.LOADING -> LoadingStateContent()
                ScanState.SAFE -> SafeStateContent(viewModel, onDismiss)
                ScanState.MALICIOUS -> MaliciousStateContent(viewModel, onDismiss)
                ScanState.ERROR -> ErrorStateContent(viewModel)
            }
        }
    }
}

@Composable
fun IdleStateContent(viewModel: AgenticShieldViewModel) {
    OutlinedTextField(
        value = viewModel.urlToScan,
        onValueChange = { viewModel.updateUrl(it) },
        placeholder = { Text("Enter URL to verify...", color = Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF3B82F6),
            unfocusedBorderColor = Color.DarkGray
        ),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = { viewModel.scanUrl(viewModel.urlToScan) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
    ) {
        Icon(Icons.Default.Search, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Scan Link", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LoadingStateContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
        Surface(
            modifier = Modifier
                .size(100.dp)
                .scale(scale),
            shape = CircleShape,
            color = Color(0xFF3B82F6).copy(alpha = 0.2f)
        ) {}
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = "Scanning",
            tint = Color(0xFF3B82F6),
            modifier = Modifier.size(80.dp)
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        "Checking link security...",
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun SafeStateContent(viewModel: AgenticShieldViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val result = viewModel.analysisResult

    Icon(
        Icons.Default.VerifiedUser,
        "Safe",
        tint = Color(0xFF22C55E),
        modifier = Modifier.size(100.dp)
    )
    Text(
        "Link is Safe",
        color = Color(0xFF22C55E),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp)
    )
    
    if (result != null) {
        Text(
            "Confidence: ${(result.confidence * 100).toInt()}%",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(viewModel.urlToScan))
            context.startActivity(intent)
            onDismiss()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
    ) {
        Text("Open in Browser", fontSize = 18.sp)
    }
    TextButton(
        onClick = { viewModel.reset() },
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Text("Scan Another", color = Color.Gray)
    }
}

@Composable
fun MaliciousStateContent(viewModel: AgenticShieldViewModel, onDismiss: () -> Unit) {
    val result = viewModel.analysisResult

    Icon(
        Icons.Default.Warning,
        "Danger",
        tint = Color(0xFFEF4444),
        modifier = Modifier.size(100.dp)
    )
    Text(
        "Threat Detected!",
        color = Color(0xFFEF4444),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp)
    )

    Surface(
        color = Color(0xFF450A0A),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Detection Details:",
                color = Color(0xFFFCA5A5),
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            if (result != null) {
                Text("Source: ${result.source}", color = Color.White)
                Text("Confidence: ${(result.confidence * 100).toInt()}%", color = Color.White)
            }
        }
    }

    Button(
        onClick = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
    ) {
        Text("Close App", fontSize = 18.sp)
    }
    TextButton(
        onClick = { viewModel.reset() },
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Text("Scan Another Anyway", color = Color.Gray)
    }
}

@Composable
fun ErrorStateContent(viewModel: AgenticShieldViewModel) {
    Icon(
        Icons.Default.ErrorOutline,
        "Error",
        tint = Color.Gray,
        modifier = Modifier.size(100.dp)
    )
    Text(
        "Analysis Failed",
        color = Color.White,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp)
    )
    Text(
        viewModel.errorMessage,
        color = Color.Gray,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
    )

    Button(
        onClick = { viewModel.scanUrl(viewModel.urlToScan) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
    ) {
        Icon(Icons.Default.Refresh, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Retry Scan", fontSize = 18.sp)
    }
}
