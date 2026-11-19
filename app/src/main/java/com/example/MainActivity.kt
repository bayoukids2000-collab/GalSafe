package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.core.configureAppCheck
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.GalSafeTheme
import com.example.ui.viewmodel.AppContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check before Compose UI, ViewModels, or Firebase AI operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        val appContainer = AppContainer(applicationContext)

        setContent {
            GalSafeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(appContainer = appContainer)
                }
            }
        }
    }
}
