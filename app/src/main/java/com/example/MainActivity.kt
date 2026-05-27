package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.alarm.AlarmSchedulerImpl
import com.example.data.AlarmDatabase
import com.example.data.AlarmRepository
import com.example.data.WeatherApi
import com.example.ui.ClockHomeView
import com.example.ui.ClockViewModel
import com.example.ui.ClockViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: ClockViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialise Room Database & Helpers
        val database = AlarmDatabase.getDatabase(this)
        val alarmScheduler = AlarmSchedulerImpl(this)
        val alarmRepository = AlarmRepository(database.alarmDao, alarmScheduler)
        
        // 2. Initialise Retrofit Weather Client
        val weatherApi = WeatherApi.create()

        // 3. Create ViewModel via Factory
        val factory = ClockViewModelFactory(alarmRepository, weatherApi)
        viewModel = ViewModelProvider(this, factory)[ClockViewModel::class.java]

        // 4. Proactively reschedule active alarms to make sure they are synchronised on boot / refresh
        lifecycleScope.launch {
            alarmRepository.rescheduleAllEnabledAlarms()
        }

        enableEdgeToEdge()
        setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                ClockHomeView(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
