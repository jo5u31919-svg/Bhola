package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cronos.timeline.data.local.CronosDatabase
import com.cronos.timeline.data.repository.TimelineRepository
import com.cronos.timeline.ui.screens.CronosTimelineScreen
import com.cronos.timeline.ui.theme.CronosSystemTheme
import com.cronos.timeline.viewmodel.TimelineViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val database = CronosDatabase.getInstance(applicationContext)
        val repository = TimelineRepository(database.timelineBlockDao())

        setContent {
            CronosSystemTheme {
                val timelineViewModel: TimelineViewModel = viewModel(
                    factory = TimelineViewModel.Factory(repository)
                )
                CronosTimelineScreen(viewModel = timelineViewModel)
            }
        }
    }
}
