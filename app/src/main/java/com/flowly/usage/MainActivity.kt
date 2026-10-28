package com.flowly.usage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.flowly.usage.ui.FlowlyApp
import com.flowly.usage.ui.theme.FlowlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            FlowlyTheme {
                FlowlyApp()
            }
        }
    }
}
