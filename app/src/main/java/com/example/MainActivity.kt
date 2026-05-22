package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.ui.screens.MainAppContent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ExamPortalViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: ExamPortalViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val systemTheme = isSystemInDarkTheme()
      val userDarkTheme = remember { mutableStateOf(systemTheme) }

      MyApplicationTheme(darkTheme = userDarkTheme.value) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
          MainAppContent(
            viewModel = viewModel,
            userDarkTheme = userDarkTheme
          )
        }
      }
    }
  }
}
