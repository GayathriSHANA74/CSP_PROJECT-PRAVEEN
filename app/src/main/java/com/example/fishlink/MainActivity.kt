package com.example.fishlink

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fishlink.ui.components.ScanScreen
import com.example.fishlink.ui.viewmodel.AgenticShieldViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)

        val sharedUrl = if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else null

        setContent {
            val viewModel: AgenticShieldViewModel = viewModel()
            val context = LocalContext.current

            LaunchedEffect(Unit) {
                if (sharedUrl != null) {
                    viewModel.scanUrl(sharedUrl)
                } else if (viewModel.urlToScan.isEmpty()) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    if (clipboard.hasPrimaryClip()) {
                        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        if (clipText.startsWith("http")) {
                            viewModel.updateUrl(clipText)
                        }
                    }
                }
            }

            AgenticShieldTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99000000)),
                    contentAlignment = Alignment.Center
                ) {
                    ScanScreen(viewModel, onDismiss = { finish() })
                }
            }
        }
    }
}

@Composable
fun AgenticShieldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color.Transparent,
            surface = Color(0xFF0F172A),
            primary = Color(0xFF3B82F6)
        ),
        content = content
    )
}
