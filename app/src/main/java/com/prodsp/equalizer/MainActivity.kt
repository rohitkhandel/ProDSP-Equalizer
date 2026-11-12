package com.prodsp.equalizer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

private val AppBackground = Color(0xFF090B10)
private val PanelBackground = Color(0xFF151923)
private val PanelRaised = Color(0xFF202633)
private val Accent = Color(0xFF35E0B2)
private val PrimaryText = Color(0xFFF3F5F8)
private val SecondaryText = Color(0xFF929BAA)
private val TrackColor = Color(0xFF394250)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProDSPApp() }
    }
}

@Composable
fun ProDSPApp() {
    val context = LocalContext.current
    val engine = remember(context) { MusicPlayerEngine(context.applicationContext) }

    var playback by remember(engine) { mutableStateOf(engine.playbackSnapshot) }
    var equalizerEnabled by remember { mutableStateOf(true) }
    var bassBoostEnabled by remember { mutableStateOf(true) }
    var bassStrength by remember { mutableFloatStateOf(0f) }
    var trebleGain by remember { mutableFloatStateOf(0f) }
    var activeSection by remember { mutableStateOf("EQUALIZER") }
    var selectedPreset by remember { mutableStateOf("FLAT") }

    val bands = engine.equalizerBandFrequenciesHz
    val bandGains = remember(engine) { mutableStateMapOf<Int, Float>() }

    LaunchedEffect(bands) {
        bands.forEach { frequency ->
            if (bandGains[frequency] == null) bandGains[frequency] = 0f
        }
    }

    DisposableEffect(engine) {
        engine.onPlaybackChanged = { playback = it }
        playback = engine.playbackSnapshot
        onDispose {
            engine.onPlaybackChanged = null
            engine.release()
        }
    }

    LaunchedEffect(engine, playback.isPlaying) {
        while (playback.isPlaying) {
            delay(300)
            playback = engine.playbackSnapshot
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            secondary = Accent,
            background = AppBackground,
            surface = PanelBackground,
            onPrimary = AppBackground,
            onBackground = PrimaryText,
            onSurface = PrimaryText
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppHeader(isPlaying = playback.isPlaying)

                PlaybackPanel(
                    playback = playback,
                    engine = engine
                )

                SectionSelector(
                    selected = activeSection,
                    onSelected = { activeSection = it }
                )

                when (activeSection) {
                    "EQUALIZER" -> EqualizerPanel(
                        bands = bands,
                        gains = bandGains,
                        enabled = equalizerEnabled,
                        selectedPreset = selectedPreset,
                        onEnabledChange = {
                            equalizerEnabled = it
                            engine.setEqualizerEnabled(it)
                        },
                        onPresetSelected = { preset ->
                            selectedPreset = preset
                            bands.forEach { frequency ->
                                val gain = presetGain(preset, frequency)
                                bandGains[frequency] = gain
                                engine.setEqualizerBand(frequency, gain)
                            }
                        },
                        onBandChanged = { frequency, gain ->
                            selectedPreset = "CUSTOM"
                            bandGains[frequency] = gain
                            engine.setEqualizerBand(frequency, gain)
                        }
                    )

                    else -> TonePanel(
                        bassStrength = bassStrength,
                        bassEnabled = bassBoostEnabled,
                        trebleGain = trebleGain,
                        onBassEnabledChange = {
                            bassBoostEnabled = it
                            engine.setBassBoostEnabled(it)
                        },
                        onBassStrengthChange = {
                            bassStrength = it
                            engine.setBassBoost(it.toInt())
                        },
                        onTrebleChange = { value ->
                            trebleGain = value
                            val highBand = bands.maxOrNull() ?: 14_000
                            bandGains[highBand] = value
                            engine.setEqualizerBand(highBand, value)
                        }
                    )
                }

                playback.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = Color(0xFFFF8A8A),
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "PRODSP  •  LOCAL PLAYER  •  ANDROID AUDIO EFFECTS",
                    color = SecondaryText,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AppHeader(isPlaying: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "PRODSP",
                color = PrimaryText,
                fontSize = 27.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                text = "AUDIO CONTROL CENTER",
                color = SecondaryText,
                fontSize = 10.sp,
                letterSpacing = 1.6.sp
            )
        }

        Surface(
            color = PanelRaised,
            shape = RoundedCornerShape(50)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(if (isPlaying) Accent else SecondaryText, CircleShape)
                )
                Text(
                    text = if (isPlaying) "PLAYING" else "READY",
                    color = if (isPlaying) Accent else SecondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
private fun PlaybackPanel(
    playback: PlaybackSnapshot,
    engine: MusicPlayerEngine
) {
    val context = LocalContext.current
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }

    val progress = if (playback.durationMs > 0L) {
        (playback.positionMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBackground)
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "NOW PLAYING",
                color = Accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = PanelRaised
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "♫",
                            color = Accent,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playback.title,
                        color = PrimaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (playback.isReady) "Local audio • Ready" else "Select a music file to start",
                        color = SecondaryText,
                        fontSize = 11.sp
                    )
                }
            }

            Slider(
                value = if (isSeeking) seekValue else progress,
                onValueChange = {
                    isSeeking = true
                    seekValue = it
                },
                onValueChangeFinished = {
                    if (playback.durationMs > 0L) {
                        engine.seekTo((seekValue * playback.durationMs).toLong())
                    }
                    isSeeking = false
                },
                enabled = playback.hasTrack && playback.durationMs > 0L,
                valueRange = 0f..1f
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(if (isSeeking) (seekValue * playback.durationMs).toLong() else playback.positionMs), color = SecondaryText, fontSize = 10.sp)
                Text(formatTime(playback.durationMs), color = SecondaryText, fontSize = 10.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                FilledTonalButton(
                    onClick = { engine.togglePlayback() },
                    enabled = playback.hasTrack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (playback.isPlaying) "Ⅱ  PAUSE" else "▶  PLAY")
                }

                OutlinedButton(
                    onClick = { engine.stop() },
                    enabled = playback.hasTrack
                ) {
                    Text("STOP")
                }
            }

            MusicFilePicker(
                onFileSelected = { uri ->
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    engine.play(uri, queryDisplayName(context, uri))
                }
            )
        }
    }
}

@Composable
private fun SectionSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        listOf("EQUALIZER", "TONE").forEach { section ->
            val isSelected = selected == section
            Surface(
                modifier = Modifier.weight(1f),
                color = if (isSelected) PanelRaised else PanelBackground,
                shape = RoundedCornerShape(14.dp),
                onClick = { onSelected(section) }
            ) {
                Text(
                    text = section,
                    color = if (isSelected) Accent else SecondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(vertical = 13.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun EqualizerPanel(
    bands: List<Int>,
    gains: Map<Int, Float>,
    enabled: Boolean,
    selectedPreset: String,
    onEnabledChange: (Boolean) -> Unit,
    onPresetSelected: (String) -> Unit,
    onBandChanged: (Int, Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBackground)
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "EQUALIZER",
                        color = PrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "DEVICE EQ  •  ${bands.size} BANDS",
                        color = SecondaryText,
                        fontSize = 10.sp,
                        letterSpacing = 0.9.sp
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                listOf("FLAT", "BASS", "VOCAL", "TREBLE").forEach { preset ->
                    FilterChip(
                        selected = selectedPreset == preset,
                        onClick = { onPresetSelected(preset) },
                        label = { Text(preset, fontSize = 10.sp) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 4.dp, bottom = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bands.forEach { frequency ->
                    val value = gains[frequency] ?: 0f
                    Column(
                        modifier = Modifier.width(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = String.format(Locale.US, "%+.1f", value),
                            color = if (enabled) Accent else SecondaryText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        VerticalEqControl(
                            value = value,
                            enabled = enabled,
                            onValueChange = { onBandChanged(frequency, it) }
                        )
                        Text(
                            text = formatFrequency(frequency),
                            color = PrimaryText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Text(
                text = "Gain range: −12 dB to +12 dB",
                color = SecondaryText,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun VerticalEqControl(
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    Canvas(
        modifier = Modifier
            .width(42.dp)
            .height(154.dp)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures { point ->
                        val normalized = (point.y / size.height.toFloat()).coerceIn(0f, 1f)
                        currentOnValueChange(12f - normalized * 24f)
                    }
                }
            }
            .pointerInput(enabled) {
                if (enabled) {
                    detectDragGestures(
                        onDragStart = { point ->
                            val normalized = (point.y / size.height.toFloat()).coerceIn(0f, 1f)
                            currentOnValueChange(12f - normalized * 24f)
                        },
                        onDrag = { change, _ ->
                            val normalized = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                            currentOnValueChange(12f - normalized * 24f)
                            change.consume()
                        }
                    )
                }
            }
    ) {
        val x = size.width / 2f
        val top = 5.dp.toPx()
        val bottom = size.height - top
        val neutralY = size.height / 2f
        val valueY = top + ((12f - value.coerceIn(-12f, 12f)) / 24f) * (bottom - top)
        val activeColor = if (enabled) Accent else SecondaryText

        drawLine(
            color = TrackColor,
            start = Offset(x, top),
            end = Offset(x, bottom),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = activeColor,
            start = Offset(x, neutralY),
            end = Offset(x, valueY),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = SecondaryText,
            start = Offset(x - 8.dp.toPx(), neutralY),
            end = Offset(x + 8.dp.toPx(), neutralY),
            strokeWidth = 1.dp.toPx()
        )
        drawCircle(
            color = activeColor,
            radius = 8.dp.toPx(),
            center = Offset(x, valueY)
        )
        drawCircle(
            color = PanelBackground,
            radius = 3.dp.toPx(),
            center = Offset(x, valueY)
        )
    }
}

@Composable
private fun TonePanel(
    bassStrength: Float,
    bassEnabled: Boolean,
    trebleGain: Float,
    onBassEnabledChange: (Boolean) -> Unit,
    onBassStrengthChange: (Float) -> Unit,
    onTrebleChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBackground)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("BASS ENHANCEMENT", color = PrimaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("ANDROID BASS BOOST EFFECT", color = SecondaryText, fontSize = 10.sp)
                }
                Switch(checked = bassEnabled, onCheckedChange = onBassEnabledChange)
            }

            Text(
                text = "${bassStrength.toInt()}%",
                color = Accent,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = bassStrength,
                onValueChange = onBassStrengthChange,
                valueRange = 0f..100f,
                enabled = bassEnabled
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PanelRaised,
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TREBLE GAIN", color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Uses the highest available EQ band", color = SecondaryText, fontSize = 10.sp)
                        }
                        Text(String.format(Locale.US, "%+.1f dB", trebleGain), color = Accent, fontSize = 12.sp)
                    }
                    Slider(
                        value = trebleGain,
                        onValueChange = onTrebleChange,
                        valueRange = -12f..12f
                    )
                }
            }
        }
    }
}

private fun presetGain(preset: String, frequencyHz: Int): Float {
    return when (preset) {
        "BASS" -> when {
            frequencyHz < 150 -> 8f
            frequencyHz < 500 -> 5f
            frequencyHz < 1_500 -> 2f
            frequencyHz < 6_000 -> 0f
            else -> 1f
        }
        "VOCAL" -> when {
            frequencyHz < 150 -> -2f
            frequencyHz < 500 -> 0f
            frequencyHz < 4_000 -> 5f
            frequencyHz < 8_000 -> 3f
            else -> 1f
        }
        "TREBLE" -> when {
            frequencyHz < 150 -> -2f
            frequencyHz < 500 -> -1f
            frequencyHz < 2_500 -> 0f
            else -> 5f
        }
        else -> 0f
    }
}

private fun formatFrequency(frequencyHz: Int): String {
    return if (frequencyHz >= 1_000) {
        val khz = frequencyHz / 1_000f
        if (khz >= 10f) String.format(Locale.US, "%.0fk", khz)
        else String.format(Locale.US, "%.1fk", khz)
    } else {
        "${frequencyHz}Hz"
    }
}

private fun formatTime(positionMs: Long): String {
    val totalSeconds = (positionMs.coerceAtLeast(0L) / 1_000L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    val displayName = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    return displayName?.takeIf { it.isNotBlank() }
        ?: uri.lastPathSegment?.substringAfterLast('/')
        ?: "Selected audio"
}
