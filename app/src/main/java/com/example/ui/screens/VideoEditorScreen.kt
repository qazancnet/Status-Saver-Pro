package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Save
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import com.example.model.StatusItem
import com.example.util.t
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    status: StatusItem,
    onBack: () -> Unit,
    onSaveEdits: (StatusItem, Float, Float, Float, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val totalDurationSec = (status.durationMs / 1000f).coerceAtLeast(5f)

    // Trimming variables
    var trimRange by remember { mutableStateOf(0f..totalDurationSec) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }

    // Visual formatting and video filter presets
    var activeFormat by remember { mutableStateOf("9:16") } // "9:16", "1:1", "16:9", "4:5"
    var activeVideoFilterIndex by remember { mutableStateOf(0) }

    // Simulation states
    var isPlaying by remember { mutableStateOf(true) }
    var isRenderingVideo by remember { mutableStateOf(false) }
    var renderingProgress by remember { mutableFloatStateOf(0f) }

    // Auto update rendering simulation progress
    LaunchedEffect(isRenderingVideo) {
        if (isRenderingVideo) {
            renderingProgress = 0f
            while (renderingProgress < 1f) {
                delay(150)
                renderingProgress += 0.05f
            }
            onSaveEdits(status, trimRange.start, trimRange.endInclusive, playbackSpeed, isMuted)
            isRenderingVideo = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(t("Videonu Redaktə Et", "Edit Video", "Редактировать видео", "Videoyu Düzenle"), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("video_editor_back")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Geri", "Back", "Назад", "Geri"))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isRenderingVideo = true },
                        enabled = !isRenderingVideo,
                        modifier = Modifier.testTag("video_save_trigger")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = t("Yadda Saxla", "Save", "Сохранить", "Kaydet"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { innerPadding ->
        if (isRenderingVideo) {
            // Processing/Transcoding simulated state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        progress = { renderingProgress },
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 6.dp,
                        modifier = Modifier.size(80.dp).testTag("render_progress")
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = t("VİDEOROLİK EMAL OLUNUR", "PROCESSING VIDEO", "ОБРАБОТКА ВИДЕО", "VİDEO İŞLENİYOR"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = t(
                            az = "Trim oxu kəsilir, sürət ${(playbackSpeed * 10).roundToInt() / 10.0}x fiksasiya edilir və audio axın yenilənir...",
                            en = "Trim range is cut, speed is locked at ${(playbackSpeed * 10).roundToInt() / 10.0}x, and audio stream is updated...",
                            ru = "Интервал обрезки вырезается, скорость фиксируется на ${(playbackSpeed * 10).roundToInt() / 10.0}x, а аудиопоток обновляется...",
                            tr = "Kırpma aralığı kesiliyor, hız ${(playbackSpeed * 10).roundToInt() / 10.0}x olarak sabitleniyor ve ses akışı güncelleniyor..."
                        ),
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${(renderingProgress * 100).toInt()}%",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFF1E1E1E)),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. VIDEO VIEWER AREA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val ratio = when (activeFormat) {
                        "1:1" -> 1f
                        "16:9" -> 16f / 9f
                        "4:5" -> 4f / 5f
                        else -> 9f / 16f
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(12.dp)
                            .aspectRatio(ratio)
                            .background(Color.DarkGray)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        var playableUri by remember(status) { mutableStateOf<android.net.Uri?>(null) }
                        val context = androidx.compose.ui.platform.LocalContext.current
                        LaunchedEffect(status) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                val uriStr = status.uri.toString()
                                if (uriStr.startsWith("http") || uriStr.startsWith("file://") || uriStr.startsWith("/")) {
                                    playableUri = status.uri
                                } else {
                                    try {
                                        val cacheFile = java.io.File(context.cacheDir, "status_preview_edit_${status.fileName.hashCode()}.mp4")
                                        if (!cacheFile.exists() || cacheFile.length() <= 0) {
                                            context.contentResolver.openInputStream(status.uri)?.use { inStream ->
                                                java.io.FileOutputStream(cacheFile).use { outStream ->
                                                    inStream.copyTo(outStream)
                                                }
                                            }
                                        }
                                        playableUri = if (cacheFile.exists() && cacheFile.length() > 0) {
                                            android.net.Uri.fromFile(cacheFile)
                                        } else {
                                            status.uri
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        playableUri = status.uri
                                    }
                                }
                            }
                        }

                        val resolvedUri = playableUri
                        if (resolvedUri != null) {
                            AndroidView(
                                factory = { ctx ->
                                    android.widget.VideoView(ctx).apply {
                                        setVideoURI(resolvedUri)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            if (isPlaying) {
                                                start()
                                            }
                                        }
                                    }
                                },
                                update = { videoView ->
                                    if (isPlaying) {
                                        videoView.start()
                                    } else {
                                        videoView.pause()
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("video_edit_loading_indicator")
                                )
                            }
                        }

                        // Overlay play controls
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { isPlaying = !isPlaying },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pauza" else "Oynat",
                                tint = if (isPlaying) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Floating bottom timing stamp
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = t(
                                az = "Aktiv interval: ${String.format("%.1f", trimRange.start)}s - ${String.format("%.1f", trimRange.endInclusive)}s | Format: $activeFormat",
                                en = "Active interval: ${String.format("%.1f", trimRange.start)}s - ${String.format("%.1f", trimRange.endInclusive)}s | Format: $activeFormat",
                                ru = "Активный интервал: ${String.format("%.1f", trimRange.start)}с - ${String.format("%.1f", trimRange.endInclusive)}с | Формат: $activeFormat",
                                tr = "Etkin aralık: ${String.format("%.1f", trimRange.start)}s - ${String.format("%.1f", trimRange.endInclusive)}s | Format: $activeFormat"
                            ),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 2. VIDEO TRIMMING / EDITING PARAMETERS PANEL
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Section 1: Trim Timeline Sliders
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = t("Videoroliki Kəs (Trim)", "Cut Video (Trim)", "Вырезать видео (Trim)", "Videoyu Kes (Kırp)"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // RangeSlider showing trimming boundaries
                    RangeSlider(
                        value = trimRange,
                        onValueChange = { trimRange = it },
                        valueRange = 0f..totalDurationSec,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trim_range_slider"),
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            thumbColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = t(
                                az = "Başlanğıc: ${String.format("%.1f", trimRange.start)}s",
                                en = "Start: ${String.format("%.1f", trimRange.start)}s",
                                ru = "Начало: ${String.format("%.1f", trimRange.start)}с",
                                tr = "Başlangıç: ${String.format("%.1f", trimRange.start)}s"
                            ),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = t(
                                az = "Son: ${String.format("%.1f", trimRange.endInclusive)}s",
                                en = "End: ${String.format("%.1f", trimRange.endInclusive)}s",
                                ru = "Конец: ${String.format("%.1f", trimRange.endInclusive)}с",
                                tr = "Bitiş: ${String.format("%.1f", trimRange.endInclusive)}s"
                            ),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Section 2: Format / Aspect Ratio Selection
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = t("Ölçü Formatı (Crop)", "Aspect Ratio (Crop)", "Размерный формат (Crop)", "Boyut Formatı (Kırp)"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val formats = listOf("9:16", "1:1", "16:9", "4:5")
                        formats.forEach { format ->
                            val isSelected = activeFormat == format
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { activeFormat = format }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = format,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Section 3: Color Filters
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = t("Video Filtrləri", "Video Filters", "Видео Фильтры", "Video Filtreleri"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val videoFilters = listOf("Orijinal", "B&W", "Sepiya", "Soyuq", "Retro")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(videoFilters.size) { idx ->
                            val filterName = videoFilters[idx]
                            val filterDisplayName = when (filterName) {
                                "Orijinal" -> t("Orijinal", "Original", "Оригинал", "Orijinal")
                                "B&W" -> t("B&W", "B&W", "Ч/Б", "S&B")
                                "Sepiya" -> t("Sepiya", "Sepia", "Сепия", "Sepya")
                                "Soyuq" -> t("Soyuq", "Cool", "Холодный", "Soğuk")
                                "Retro" -> t("Retro", "Retro", "Ретро", "Retro")
                                else -> filterName
                            }
                            val isSelected = activeVideoFilterIndex == idx
                            Card(
                                modifier = Modifier
                                    .width(80.dp)
                                    .clickable { activeVideoFilterIndex = idx }
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(8.dp).fillMaxWidth()
                                ) {
                                    Text(
                                        text = filterDisplayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Section 4: Playback Speed and Audio Options
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Playback Speed Options
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SlowMotionVideo,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = t("Oynatma Sürəti", "Playback Speed", "Скорость воспроизведения", "Oynatma Hızı"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
                                speeds.forEach { speed ->
                                    val isSelected = playbackSpeed == speed
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { playbackSpeed = speed }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .testTag("speed_$speed")
                                    ) {
                                        Text(
                                            text = "${speed}x",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Mute Audio Option
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(80.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = if (isMuted) Color.Red else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isMuted) t("Səssiz", "Muted", "Без звука", "Sessiz") else t("Səs Aktiv", "Audio Active", "Звук Включен", "Ses Aktif"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Switch(
                                checked = isMuted,
                                onCheckedChange = { isMuted = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Red,
                                    checkedTrackColor = Color.Red.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.scale(0.8f).testTag("mute_switch")
                            )
                        }
                    }
                }
            }
        }
    }
}

// Simple modifier extension for scaling switches
fun Modifier.scale(scale: Float) = this.then(
    Modifier.size((48 * scale).dp) // simple bounds wrapper
)

// Dynamic Color Matrix Filters for Video rendering
fun getVideoColorFilter(index: Int): androidx.compose.ui.graphics.ColorFilter? {
    val matrix = when (index) {
        1 -> androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
            0.33f, 0.59f, 0.11f, 0f, 0f,
            0.33f, 0.59f, 0.11f, 0f, 0f,
            0.33f, 0.59f, 0.11f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        2 -> androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        3 -> androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
            0.8f, 0f, 0f, 0f, 0f,
            0f, 0.9f, 0f, 0f, 0f,
            0f, 0f, 1.2f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        4 -> androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
            1f, 0.2f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 0.8f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        else -> null
    }
    return matrix?.let { androidx.compose.ui.graphics.ColorFilter.colorMatrix(it) }
}
