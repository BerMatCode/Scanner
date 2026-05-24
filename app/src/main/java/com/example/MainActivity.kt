package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.Modifier
import com.example.ui.ScanViewModel
import com.example.ui.screens.MainScreen
import com.example.ui.theme.BerMatScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Construct viewModel using standard factory
        val viewModel = ViewModelProvider(
            this, 
            ScanViewModel.Factory(application)
        )[ScanViewModel::class.java]

        setContent {
            BerMatScannerTheme {
                MainScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
