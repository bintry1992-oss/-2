package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Alarm
import com.example.data.AlarmDatabase
import com.example.data.AlarmRepository
import com.example.data.City
import com.example.data.WeatherApi
import com.example.data.getWeatherDescription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class WeatherUiState {
    object Loading : WeatherUiState()
    data class Success(val temperature: Double, val description: String, val humidity: Double?) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

class ClockViewModel(
    private val alarmRepository: AlarmRepository,
    private val weatherApi: WeatherApi
) : ViewModel() {

    // Current time ticks
    private val _currentTime = MutableStateFlow("")
    val currentTime = _currentTime.asStateFlow()

    private val _currentSeconds = MutableStateFlow("")
    val currentSeconds = _currentSeconds.asStateFlow()

    private val _currentDate = MutableStateFlow("")
    val currentDate = _currentDate.asStateFlow()

    // Alarm list
    val alarms: StateFlow<List<Alarm>> = alarmRepository.allAlarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Weather state
    private val _selectedCity = MutableStateFlow(City.PRESETS[0]) // Default Beijing
    val selectedCity = _selectedCity.asStateFlow()

    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherState = _weatherState.asStateFlow()

    // Theme settings: 0: Default Dark, 1: Warm Amber (Anti-Blue), 2: Forest Emerald, 3: Warm Sand Light
    private val _themeMode = MutableStateFlow(1)
    val themeMode = _themeMode.asStateFlow()

    // Bedside Zen Mode State
    private val _isZenMode = MutableStateFlow(false)
    val isZenMode = _isZenMode.asStateFlow()

    private val _zenBrightness = MutableStateFlow(0.4f) // brightness level 0.1f - 1.0f
    val zenBrightness = _zenBrightness.asStateFlow()

    private var timeTickJob: Job? = null

    init {
        startTimeTicks()
        fetchWeather()
    }

    private fun startTimeTicks() {
        timeTickJob?.cancel()
        timeTickJob = viewModelScope.launch(Dispatchers.Default) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val secFormat = SimpleDateFormat("ss", Locale.getDefault())
            val dateFormat = SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINESE)
            while (true) {
                val now = Date()
                _currentTime.value = timeFormat.format(now)
                _currentSeconds.value = secFormat.format(now)
                _currentDate.value = dateFormat.format(now)
                delay(1000)
            }
        }
    }

    fun selectCity(city: City) {
        _selectedCity.value = city
        fetchWeather()
    }

    fun fetchWeather() {
        viewModelScope.launch {
            _weatherState.value = WeatherUiState.Loading
            try {
                val city = _selectedCity.value
                val response = weatherApi.getForecast(city.latitude, city.longitude)
                val current = response.current
                if (current != null) {
                    _weatherState.value = WeatherUiState.Success(
                        temperature = current.temperature,
                        description = getWeatherDescription(current.weatherCode),
                        humidity = current.humidity
                    )
                } else {
                    _weatherState.value = WeatherUiState.Error("未能获取到当前天气数据")
                }
            } catch (e: Exception) {
                Log.e("ClockViewModel", "Failed to fetch weather", e)
                _weatherState.value = WeatherUiState.Error("获取天气失败，请检查网络连接")
            }
        }
    }

    // Theme Change
    fun changeThemeMode(mode: Int) {
        _themeMode.value = mode
    }

    // Zen Mode Actions
    fun toggleZenMode() {
        _isZenMode.value = !_isZenMode.value
    }

    fun setZenBrightness(brightness: Float) {
        _zenBrightness.value = brightness.coerceIn(0.1f, 1.0f)
    }

    // Alarm CRUD
    fun addAlarm(hour: Int, minute: Int, label: String, repeatDays: String, ringtoneId: String, isVibrate: Boolean) {
        viewModelScope.launch {
            val newAlarm = Alarm(
                hour = hour,
                minute = minute,
                label = label,
                isEnabled = true,
                repeatDays = repeatDays,
                ringtoneId = ringtoneId,
                isVibrate = isVibrate
            )
            alarmRepository.insertAlarm(newAlarm)
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepository.toggleAlarm(alarm)
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepository.updateAlarm(alarm)
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepository.deleteAlarm(alarm)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timeTickJob?.cancel()
    }
}

// Factory to simplify construction matching constructor injection rules
class ClockViewModelFactory(
    private val repository: AlarmRepository,
    private val weatherApi: WeatherApi
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClockViewModel::class.java)) {
            return ClockViewModel(repository, weatherApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
