package com.academy.sisu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.academy.sisu.ui.CalendarScreen
import com.academy.sisu.ui.HomeScreen
import com.academy.sisu.ui.theme.SisuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SisuTheme {
                val vm: AcademyViewModel = viewModel()
                var showCalendar by remember { mutableStateOf(false) }
                if (showCalendar) {
                    BackHandler { showCalendar = false }
                    CalendarScreen(vm, onBack = { showCalendar = false })
                } else {
                    HomeScreen(vm, onOpenStudent = { id ->
                        vm.select(id)
                        showCalendar = true
                    })
                }
            }
        }
    }
}
