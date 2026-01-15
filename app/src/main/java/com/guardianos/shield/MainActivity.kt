// app/src/main/java/com/guardianos/shield/MainActivity.kt
package com.guardianos.shield

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GuardianShieldApp()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GuardianShieldApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Text("🛡️ GuardianOS Shield\nFiltro web para menores")
        }
    }
}
