package com.vatsalp.transitlens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.vatsalp.transitlens.ui.navigation.AppNavHost
import com.vatsalp.transitlens.ui.theme.TransitLensTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TransitLensTheme {
                AppNavHost()
            }
        }
    }
}
