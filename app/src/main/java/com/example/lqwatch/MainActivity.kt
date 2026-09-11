package com.example.lqwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.lqwatch.navigation.AppNavHost
import com.example.lqwatch.ui.theme.LqWatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LqWatchTheme {
                AppNavHost()
            }
        }
    }
}
