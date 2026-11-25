package io.github.mcello23.onda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mcello23.onda.data.AppSettingsStore
import io.github.mcello23.onda.ui.AppScreen
import io.github.mcello23.onda.ui.OndaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettingsStore.init(this)
        enableEdgeToEdge()
        setContent {
            val settings by AppSettingsStore.settings.collectAsStateWithLifecycle()
            OndaTheme(settings = settings) {
                AppScreen()
            }
        }
    }
}
