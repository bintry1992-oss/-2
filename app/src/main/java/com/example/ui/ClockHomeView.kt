package com.example.ui

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.AlarmAudioPlayer
import com.example.alarm.AlarmRingingManager
import com.example.data.Alarm
import com.example.data.City
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.Purple80
import com.example.ui.theme.MyApplicationTheme
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockHomeView(
    viewModel: ClockViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val currentSeconds by viewModel.currentSeconds.collectAsStateWithLifecycle()
    val currentDate by viewModel.currentDate.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isZenMode by viewModel.isZenMode.collectAsStateWithLifecycle()
    val zenBrightness by viewModel.zenBrightness.collectAsStateWithLifecycle()

    // Observe ringing state
    val ringingAlarm by AlarmRingingManager.instance.ringingAlarm.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showCitySheet by remember { mutableStateOf(false) }
    var clockTypeAnalog by remember { mutableStateOf(false) } // False for Digital, True for Analog

    // Main App Scaffold wrapping with our dynamic theme
    MyApplicationTheme(themeMode = themeMode) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isZenMode) {
                    // Bedside Zen / Fullscreen Ambient Night Stand view
                    ZenBedsideView(
                        currentTime = currentTime,
                        currentDate = currentDate,
                        weatherState = weatherState,
                        brightness = zenBrightness,
                        onUpdateBrightness = { viewModel.setZenBrightness(it) },
                        onExit = { viewModel.toggleZenMode() }
                    )
                } else {
                    // Primary App layout
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = "简约时钟",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                },
                                actions = {
                                    // Theme toggle menu
                                    ThemeSelector(
                                        currentTheme = themeMode,
                                        onSelectTheme = { viewModel.changeThemeMode(it) }
                                    )
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = Color.Transparent
                                )
                            )
                        },
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { showAddDialog = true },
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .testTag("add_alarm_button"),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "添加闹钟")
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Weather widget header
                            WeatherHeader(
                                selectedCity = selectedCity,
                                weatherState = weatherState,
                                onCityClick = { showCitySheet = true },
                                onRefresh = { viewModel.fetchWeather() }
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Interactive Clock Face Area
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                // Double tap clock face to enter Bedside Zen mode!
                                                viewModel.toggleZenMode()
                                            },
                                            onTap = {
                                                // Single tap switches clock faces (Digital <-> Analog)
                                                clockTypeAnalog = !clockTypeAnalog
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (clockTypeAnalog) {
                                    AnalogClockFace(
                                        currentTime = currentTime,
                                        currentSeconds = currentSeconds
                                    )
                                } else {
                                    DigitalClockFace(
                                        currentTime = currentTime,
                                        currentSeconds = currentSeconds,
                                        currentDate = currentDate
                                    )
                                }
                            }

                            Text(
                                text = "💡 轻触时钟切换表盘 | 双击进入夜间床头模式",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 10.dp)
                            )

                            Spacer(modifier = Modifier.height(30.dp))

                            // Alarm List header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "闹钟提醒",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${alarms.count { it.isEnabled }}个已开启",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (alarms.isEmpty()) {
                                EmptyAlarmsView()
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentPadding = PaddingValues(bottom = 80.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(alarms, key = { it.id }) { alarm ->
                                        AlarmItemCard(
                                            alarm = alarm,
                                            onToggle = { viewModel.toggleAlarm(alarm) },
                                            onDelete = { viewModel.deleteAlarm(alarm) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Global Ringing Lockscreen overlay
                AnimatedVisibility(
                    visible = ringingAlarm != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ringingAlarm?.let { alarm ->
                        AlarmRingingOverlay(
                            alarm = alarm,
                            onDismiss = { AlarmRingingManager.instance.dismiss(context) },
                            onSnooze = { AlarmRingingManager.instance.snooze(context) }
                        )
                    }
                }

                // Add Alarm dialogue
                if (showAddDialog) {
                    AddAlarmDialog(
                        onDismiss = { showAddDialog = false },
                        onConfirm = { hour, minute, label, repeat, ringtone, vibrate ->
                            viewModel.addAlarm(hour, minute, label, repeat, ringtone, vibrate)
                            showAddDialog = false
                        }
                    )
                }

                // City Selector Dialog Sheet
                if (showCitySheet) {
                    CityChooserDialog(
                        onDismiss = { showCitySheet = false },
                        onSelectCity = { city ->
                            viewModel.selectCity(city)
                            showCitySheet = false
                        }
                    )
                }
            }
        }
    }
}

// ==========================================
// SUBCOMPONENTS
// ==========================================

@Composable
fun WeatherHeader(
    selectedCity: City,
    weatherState: WeatherUiState,
    onCityClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCityClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "定位城市",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = selectedCity.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "切换",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                when (weatherState) {
                    is WeatherUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is WeatherUiState.Success -> {
                        val icon = when {
                            weatherState.description.contains("晴") -> Icons.Default.WbSunny
                            weatherState.description.contains("雨") -> Icons.Default.Umbrella
                            weatherState.description.contains("云") || weatherState.description.contains("阴") -> Icons.Default.Cloud
                            else -> Icons.Default.Cloud
                        }
                        Icon(
                            icon,
                            contentDescription = weatherState.description,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${weatherState.temperature.roundToInt()}°C",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = weatherState.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    is WeatherUiState.Error -> {
                        IconButton(onClick = { onRefresh() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "重试获取获取天气", tint = Color.Red)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "网络连接故障",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DigitalClockFace(
    currentTime: String,
    currentSeconds: String,
    currentDate: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentTime.ifEmpty { "00:00" },
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 62.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = currentSeconds.ifEmpty { "00" },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.W300,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = currentDate.ifEmpty { "加载日期中..." },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun AnalogClockFace(
    currentTime: String,
    currentSeconds: String
) {
    val hrs = currentTime.substringBefore(":", "00").toIntOrNull() ?: 0
    val mins = currentTime.substringAfter(":", "00").toIntOrNull() ?: 0
    val secs = currentSeconds.toIntOrNull() ?: 0

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2.2f

        // Draw Clock Background Plate
        drawCircle(
            color = surfaceVariant.copy(alpha = 0.2f),
            radius = radius,
            center = center
        )
        // Outer rim
        drawCircle(
            color = primaryColor.copy(alpha = 0.15f),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Draw 12 Dial Hours ticks
        for (i in 0 until 12) {
            val angle = i * (360f / 12) * (Math.PI / 180f)
            val tickLength = if (i % 3 == 0) 14.dp.toPx() else 6.dp.toPx()
            val strokeW = if (i % 3 == 0) 3.dp.toPx() else 1.5f.dp.toPx()
            val tickColor = if (i % 3 == 0) primaryColor else primaryColor.copy(alpha = 0.4f)
            
            val startX = center.x + (radius - tickLength) * cos(angle).toFloat()
            val startY = center.y + (radius - tickLength) * sin(angle).toFloat()
            val endX = center.x + radius * cos(angle).toFloat()
            val endY = center.y + radius * sin(angle).toFloat()
            
            drawLine(
                color = tickColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }

        // Draw Hour Hand (Length ~ 50% radius)
        val hourAngle = ((hrs % 12 + mins / 60f) * 30 - 90) * (Math.PI / 180f)
        val hourHandLength = radius * 0.5f
        drawLine(
            color = primaryColor,
            start = center,
            end = Offset(
                center.x + hourHandLength * cos(hourAngle).toFloat(),
                center.y + hourHandLength * sin(hourAngle).toFloat()
            ),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Draw Minute Hand (Length ~ 75% radius)
        val minAngle = ((mins + secs / 60f) * 6 - 90) * (Math.PI / 180f)
        val minHandLength = radius * 0.75f
        drawLine(
            color = secondaryColor,
            start = center,
            end = Offset(
                center.x + minHandLength * cos(minAngle).toFloat(),
                center.y + minHandLength * sin(minAngle).toFloat()
            ),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Draw Second Hand (Length ~ 85% radius)
        val secAngle = (secs * 6 - 90) * (Math.PI / 180f)
        val secHandLength = radius * 0.85f
        drawLine(
            color = primaryColor.copy(alpha = 0.618f),
            start = center,
            end = Offset(
                center.x + secHandLength * cos(secAngle).toFloat(),
                center.y + secHandLength * sin(secAngle).toFloat()
            ),
            strokeWidth = 1.5f.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Center hub circle
        drawCircle(
            color = primaryColor,
            radius = 6.dp.toPx(),
            center = center
        )
        drawCircle(
            color = secondaryColor,
            radius = 2.dp.toPx(),
            center = center
        )
    }
}

@Composable
fun ThemeSelector(
    currentTheme: Int,
    onSelectTheme: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Settings, contentDescription = "主题色切换")
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("防蓝光琥珀 (深色睡眠推荐)") },
                onClick = { onSelectTheme(1); expanded = false },
                leadingIcon = { Icon(Icons.Default.NightsStay, contentDescription = null, tint = AmberPrimary) }
            )
            DropdownMenuItem(
                text = { Text("经典极度深灰 (减光弱影)") },
                onClick = { onSelectTheme(0); expanded = false },
                leadingIcon = { Icon(Icons.Default.Bedtime, contentDescription = null, tint = Purple80) }
            )
            DropdownMenuItem(
                text = { Text("舒缓森林翠绿 (放松视网膜)") },
                onClick = { onSelectTheme(2); expanded = false },
                leadingIcon = { Icon(Icons.Default.NightsStay, contentDescription = null, tint = EmeraldPrimary) }
            )
            DropdownMenuItem(
                text = { Text("自然温暖麦沙 (明亮舒适)") },
                onClick = { onSelectTheme(3); expanded = false },
                leadingIcon = { Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFF8D6E63)) }
            )
        }
    }
}

@Composable
fun EmptyAlarmsView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Alarm,
            contentDescription = "无闹钟",
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "暂未添加任何闹钟",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )
        Text(
            text = "点击右下角按钮添加新提醒",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun AlarmItemCard(
    alarm: Alarm,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = alarm.getFormattedTime(),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Light
                        ),
                        color = if (alarm.isEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (alarm.label.isNotEmpty()) alarm.label else "备忘闹钟",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (alarm.isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    }
                )

                val repeatPhrase = getRepeatText(alarm.repeatDays)
                Text(
                    text = "🔔 $repeatPhrase | 铃声: ${getRingtoneName(alarm.ringtoneId)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = { onDelete() }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除闹钟",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// Helper translations
fun getRepeatText(repeatDays: String): String {
    if (repeatDays.isEmpty()) return "单次"
    if (repeatDays == "1,2,3,4,5,6,7") return "每天"
    if (repeatDays == "1,2,3,4,5") return "工作日"
    if (repeatDays == "6,7") return "周末"
    
    val days = repeatDays.split(",").mapNotNull { it.toIntOrNull() }
    val dict = mapOf(1 to "一", 2 to "二", 3 to "三", 4 to "四", 5 to "五", 6 to "六", 7 to "日")
    return "周" + days.map { dict[it] ?: "" }.joinToString("、")
}

fun getRingtoneName(ringtoneId: String): String {
    return when (ringtoneId) {
        "zen_bell" -> "纯净风铃"
        "digital_beep" -> "经典滴滴"
        "gentle_breeze" -> "惬意微风"
        "deep_pulse" -> "律动深频"
        "system_default" -> "系统自带"
        else -> "琥珀风铃"
    }
}

// ==========================================
// ADD ALARM DIALOGUE with custom audio previews
// ==========================================

@Composable
fun AddAlarmDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int, label: String, repeat: String, ringtone: String, vibrate: Boolean) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    var label by remember { mutableStateOf("起床闹钟") }
    
    // Recurrence selection (Mon=1, Sun=7)
    val repeatList = remember { mutableStateListOf<Int>() }
    var isVibrate by remember { mutableStateOf(true) }
    var selectedRingtone by remember { mutableStateOf("zen_bell") }

    // Ringtone Playback preview manager
    var previewPlayer by remember { mutableStateOf<AlarmAudioPlayer?>(null) }
    
    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.stopPlaying()
        }
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "设置闹钟",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Time Pick helper button
                item {
                    Button(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    selectedHour = hour
                                    selectedMinute = minute
                                },
                                selectedHour,
                                selectedMinute,
                                true
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text(
                            text = String.format("⏰ 当时间: %02d:%02d (点击修改)", selectedHour, selectedMinute),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Title Input
                item {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("闹钟备注信息") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("alarm_label_input")
                    )
                }

                // Repeat Days Circle
                item {
                    Text(
                        text = "重复设定",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val listOfWeekdays = listOf(1, 2, 3, 4, 5, 6, 7)
                        val shortNames = mapOf(1 to "一", 2 to "二", 3 to "三", 4 to "四", 5 to "五", 6 to "六", 7 to "日")
                        
                        listOfWeekdays.forEach { dayOfWeek ->
                            val isSelected = repeatList.contains(dayOfWeek)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        if (isSelected) repeatList.remove(dayOfWeek)
                                        else repeatList.add(dayOfWeek)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = shortNames[dayOfWeek] ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Custom dynamic ringtone choosing area with immediate music preview
                item {
                    Text(
                        text = "极致微音铃声 (轻点试听)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val ringtoneOptions = listOf(
                        "zen_bell" to "🎐 纯净风铃",
                        "digital_beep" to "⏱️ 经典滴滴",
                        "gentle_breeze" to "💨 惬意微风",
                        "deep_pulse" to "🥁 律动深频",
                        "system_default" to "🔕 系统自带"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ringtoneOptions.forEach { (id, name) ->
                            val isSelected = selectedRingtone == id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        selectedRingtone = id
                                        // Play immediate preview
                                        if (previewPlayer == null) {
                                            previewPlayer = AlarmAudioPlayer(context)
                                        }
                                        previewPlayer?.startPlaying(id)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.VolumeUp,
                                        contentDescription = "预览播放中",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Vibration Toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "开启振动提醒",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = isVibrate,
                            onCheckedChange = { isVibrate = it }
                        )
                    }
                }

                // Save buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                previewPlayer?.stopPlaying()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消")
                        }
                        Button(
                            onClick = {
                                previewPlayer?.stopPlaying()
                                val repeats = repeatList.sorted().joinToString(",")
                                onConfirm(selectedHour, selectedMinute, label, repeats, selectedRingtone, isVibrate)
                            },
                            modifier = Modifier.weight(1f).testTag("save_alarm_button")
                        ) {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// CITY SELECTOR DIALOG
// ==========================================

@Composable
fun CityChooserDialog(
    onDismiss: () -> Unit,
    onSelectCity: (City) -> Unit
) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "选择气象城市",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(City.PRESETS) { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .clickable { onSelectCity(city) }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = city.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Text("取消")
                }
            }
        }
    }
}

// ==========================================
// BEDSIDE ZEN Ambient Screen View (Supports Drag Gestures!)
// ==========================================

@Composable
fun ZenBedsideView(
    currentTime: String,
    currentDate: String,
    weatherState: WeatherUiState,
    brightness: Float,
    onUpdateBrightness: (Float) -> Unit,
    onExit: () -> Unit
) {
    // Apply temporary soft dimmed container background using visual transparency
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Dark background for eye safety
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onExit() } // Exit bedside mode
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    // Touch interaction: drag vertical gestures to dynamically adjust brightness on screen
                    change.consume()
                    val delta = -dragAmount.y / 800f // Swipe up increases brightness, swipe down decreases
                    onUpdateBrightness(brightness + delta)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Glowing Overlay containing elements dimmed according to target brightness ratio
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Brightness indicator bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = String.format("🔆 夜间屏幕亮度: %d%% (上下滑动手势调节)", (brightness * 100).roundToInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberPrimary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = brightness,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(3.dp),
                    color = AmberPrimary.copy(alpha = brightness),
                    trackColor = Color.DarkGray.copy(alpha = 0.2f)
                )
            }

            // Big Clock Face
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = currentTime.ifEmpty { "00:00" },
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 82.sp,
                        fontWeight = FontWeight.ExtraLight,
                        letterSpacing = 4.sp
                    ),
                    color = AmberPrimary.copy(alpha = brightness) // Soft eye care color dynamically dimmed
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = maxOf(0.15f, brightness * 0.7f))
                )
            }

            // Bottom Weather indicator and info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (weatherState) {
                    is WeatherUiState.Success -> {
                        Text(
                            text = "${weatherState.description}   ${weatherState.temperature.roundToInt()}°C",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = maxOf(0.2f, brightness * 0.8f))
                        )
                    }
                    else -> {
                        Text(
                            text = "未检测到异常天气",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.15f)
                        )
                    }
                }

                Text(
                    text = "👇 双击屏幕退出暗光模式",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AmberPrimary.copy(alpha = maxOf(0.15f, brightness * 0.6f))
                )
            }
        }
    }
}

// ==========================================
// FULLSCREEN ALARM RINGING LOCKSCREEN GESTURES OVERLAY
// ==========================================

@Composable
fun AlarmRingingOverlay(
    alarm: Alarm,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    var dragOffset by remember { mutableStateOf(0f) }
    val maxDragLimit = 250f // Drag distance limit to trigger action in pixels
    
    // Continuous pulsing amber circle animations for signaling
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRatio by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Signal ring
            Column(
                modifier = Modifier.padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⏰ 设定的闹钟正在响铃...",
                    color = AmberPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = alarm.label,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.W300
                )
            }

            // Central pulsing visual bell structure
            Box(
                modifier = Modifier
                    .size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer pulsing ripples
                Box(
                    modifier = Modifier
                        .size((140 * pulseRatio).dp)
                        .clip(CircleShape)
                        .background(AmberPrimary.copy(alpha = 0.15f))
                )
                Box(
                    modifier = Modifier
                        .size((100 * pulseRatio).dp)
                        .clip(CircleShape)
                        .background(AmberPrimary.copy(alpha = 0.25f))
                )
                
                // Ringing Bell Circle
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(AmberPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = "Ringing Bell",
                        tint = Color.Black,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            // Dual slide-to-act interaction bar!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "👈 左侧滑动：稍后提醒（5分钟）   👉 右侧滑动：关闭闹钟",
                    color = Color.White.copy(alpha = 0.618f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Drag container
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(64.dp)
                        .background(Color.DarkGray.copy(alpha = 0.4f), RoundedCornerShape(32.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    if (dragOffset < -maxDragLimit) {
                                        onSnooze() // Trig snooze
                                    } else if (dragOffset > maxDragLimit) {
                                        onDismiss() // Trig dismiss
                                    }
                                    dragOffset = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset = (dragOffset + dragAmount.x).coerceIn(-maxDragLimit - 50f, maxDragLimit + 50f)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Left label (Snooze marker)
                    Text(
                        text = "稍后提醒",
                        color = Color.White.copy(alpha = if (dragOffset < 0) 0.9f else 0.35f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 24.dp)
                    )

                    // Right label (Dismiss marker)
                    Text(
                        text = "滑动关闭",
                        color = Color.White.copy(alpha = if (dragOffset > 0) 0.9f else 0.35f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 24.dp)
                    )

                    // Draggable middle key handle container
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(dragOffset.roundToInt(), 0) }
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                color = when {
                                    dragOffset < -maxDragLimit -> Color.Red
                                    dragOffset > maxDragLimit -> Color.Green
                                    else -> AmberPrimary
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                dragOffset < 0 -> Icons.Default.VolumeMute
                                dragOffset > 0 -> Icons.Default.Close
                                else -> Icons.Default.VolumeUp
                            },
                            contentDescription = "拉动按钮",
                            tint = Color.Black
                        )
                    }
                }
            }
        }
    }
}
