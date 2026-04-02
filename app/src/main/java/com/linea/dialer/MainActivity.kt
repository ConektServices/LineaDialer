// ─────────────────────────────────────────────────────────────────────────────
// FILE: MainActivity.kt
// ─────────────────────────────────────────────────────────────────────────────
package com.linea.dialer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.linea.dialer.navigation.LineaNavGraph
import com.linea.dialer.navigation.Screen
import com.linea.dialer.ui.theme.LineaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LineaTheme {
                val navController = rememberNavController()
                // Change startDestination to Screen.Main.route once onboarding is seen
                LineaNavGraph(
                    navController    = navController,
                    startDestination = Screen.Onboarding.route,
                )
            }
        }
    }
}
