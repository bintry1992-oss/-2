package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "current") val current: CurrentWeather?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    @Json(name = "time") val time: String,
    @Json(name = "temperature_2m") val temperature: Double,
    @Json(name = "weather_code") val weatherCode: Int,
    @Json(name = "relative_humidity_2m") val humidity: Double?
)

data class City(
    val name: String,
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        val PRESETS = listOf(
            City("北京", 39.9042, 116.4074),
            City("上海", 31.2304, 121.4737),
            City("广州", 23.1291, 113.2644),
            City("深圳", 22.5431, 114.0579),
            City("杭州", 30.2741, 120.1551),
            City("成都", 30.5728, 104.0668),
            City("武汉", 30.5928, 114.3055),
            City("西安", 34.2658, 108.9541),
            City("纽约", 40.7128, -74.0060),
            City("伦敦", 51.5074, -0.1278),
            City("东京", 35.6762, 139.6503),
            City("巴黎", 48.8566, 2.3522)
        )
    }
}

fun getWeatherDescription(code: Int): String {
    return when (code) {
        0 -> "晴朗"
        1, 2 -> "多云"
        3 -> "阴天"
        45, 48 -> "有雾"
        51, 53, 55 -> "毛毛雨"
        61, 63 -> "中雨"
        65 -> "大雨"
        71, 73, 75 -> "小雪"
        77 -> "冰雹"
        80, 81, 82 -> "阵雨"
        85, 86 -> "阵雪"
        95, 96, 99 -> "雷阵雨"
        else -> "多云"
    }
}
