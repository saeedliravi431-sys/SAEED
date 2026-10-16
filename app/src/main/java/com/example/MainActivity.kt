package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.MainAppScaffold
import com.example.ui.theme.EVPowertrainTheme
import com.example.viewmodel.EVLabViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: EVLabViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EVPowertrainTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }
}
