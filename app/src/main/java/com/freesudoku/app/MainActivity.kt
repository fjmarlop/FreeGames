package com.freesudoku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freesudoku.app.ui.AppViewModel
import com.freesudoku.app.ui.navigation.FreeSudokuNavHost
import com.freesudoku.app.ui.theme.FreeSudokuTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
            FreeSudokuTheme(themeMode = themeMode) {
                FreeSudokuNavHost()
            }
        }
    }
}
