
package com.prodsp.equalizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProDSPApp()
        }
    }
}

@Composable
fun ProDSPApp() {
    val bands = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    val values = remember {
        mutableStateListOf(0f, 0f, 0f, 0f, 0f)
    }

    val bg = Color(0xFF101116)
    val card = Color(0xFF1C1E27)
    val accent = Color(0xFF00D9A6)

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = bg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    "PRO DSP",
                    color = accent,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "PROFESSIONAL AUDIO ENGINE",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(25.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = card
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "AUDIO EQUALIZER",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(16.dp))

                        bands.forEachIndexed { index, band ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    band,
                                    color = Color.White,
                                    modifier = Modifier.width(65.dp),
                                    fontSize = 12.sp
                                )

                                Slider(
                                    value = values[index],
                                    onValueChange = {
                                        values[index] = it
                                    },
                                    valueRange = -12f..12f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = accent,
                                        activeTrackColor = accent
                                    )
                                )

                                Text(
                                    "${values[index].toInt()} dB",
                                    color = accent,
                                    fontSize = 11.sp,
                                    modifier = Modifier.width(42.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "AUDIO PRESETS",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Flat", "Bass", "Vocal").forEach { preset ->
                        OutlinedButton(
                            onClick = {
                                val presetValues = when (preset) {
                                    "Bass" -> listOf(8f, 5f, 0f, 0f, 2f)
                                    "Vocal" -> listOf(-2f, 0f, 5f, 4f, 2f)
                                    else -> listOf(0f, 0f, 0f, 0f, 0f)
                                }
                                presetValues.forEachIndexed { i, v ->
                                    values[i] = v
                                }
                            }
                        ) {
                            Text(preset)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    "BASS BOOST",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                var bass by remember { mutableFloatStateOf(0f) }

                Slider(
                    value = bass,
                    onValueChange = { bass = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent
                    )
                )

                Text(
                    "Bass Level: ${bass.toInt()}%",
                    color = accent
                )

                Spacer(Modifier.weight(1f))

                Text(
                    "ProDSP Equalizer  •  Version 1.0",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
