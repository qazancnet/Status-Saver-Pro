package com.example.ui.components

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.t
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun AppInternalAudioPlayerDialog(
    audioUri: Uri,
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            AppInternalAudioPlayer(
                audioUri = audioUri,
                title = title,
                subtitle = subtitle,
                onClose = onDismiss,
                onShare = onShare,
                onDelete = onDelete,
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}

@Composable
fun AppInternalAudioPlayer(
    audioUri: Uri,
    title: String,
    subtitle: String? = null,
    onClose: () -> Unit,
    onShare: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var isLooping by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosMs by remember { mutableFloatStateOf(0f) }
    var hasError by remember { mutableStateOf(false) }

    val defaultTitleText = t("Daxili Səs Oxuducu", "Internal Audio Player", "Аудиоплеер", "Dahili Ses Oynatıcı")
    val defaultErrorText = t("Səs faylı oxunmadı", "Failed to play audio", "Ошибка воспроизведения аудио", "Ses dosyası oynatılamadı")

    // Cleanup MediaPlayer when disposable leaves composition
    DisposableEffect(audioUri) {
        val mp = MediaPlayer().apply {
            try {
                setDataSource(context, audioUri)
                setOnPreparedListener { preparedMp ->
                    durationMs = preparedMp.duration.toLong().coerceAtLeast(0L)
                    preparedMp.isLooping = isLooping
                    if (isPlaying) {
                        preparedMp.start()
                    }
                }
                setOnCompletionListener {
                    if (!isLooping) {
                        isPlaying = false
                        currentPosMs = 0L
                    }
                }
                setOnErrorListener { _, _, _ ->
                    hasError = true
                    isPlaying = false
                    true
                }
                prepareAsync()
            } catch (e: Exception) {
                hasError = true
                isPlaying = false
            }
        }
        mediaPlayerRef = mp

        onDispose {
            try {
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            } catch (_: Exception) {}
            mediaPlayerRef = null
        }
    }

    // Progress updates
    LaunchedEffect(isPlaying, isSeeking) {
        while (isPlaying && !isSeeking) {
            mediaPlayerRef?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        currentPosMs = mp.currentPosition.toLong().coerceAtLeast(0L)
                        if (durationMs <= 0L && mp.duration > 0) {
                            durationMs = mp.duration.toLong()
                        }
                    }
                } catch (_: Exception) {}
            }
            delay(200)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = defaultTitleText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle ?: t("Səs Yazısı", "Voice Note / Audio", "Голосовая запись", "Ses Notu"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Waveform Animation Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Sound Wave
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val barHeights = listOf(0.4f, 0.7f, 0.3f, 0.9f, 0.6f, 1.0f, 0.5f, 0.8f, 0.4f, 0.7f, 0.3f, 0.8f, 0.5f, 0.9f, 0.4f)
                    val infiniteTransition = rememberInfiniteTransition(label = "wave")

                    barHeights.forEachIndexed { index, baseFactor ->
                        val animScale by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 400 + (index * 70) % 500,
                                    easing = FastOutSlowInEasing
                                ),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "bar_$index"
                        )

                        val currentBarHeight = if (isPlaying) (baseFactor * animScale).coerceIn(0.15f, 1.0f) else baseFactor * 0.3f

                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .fillMaxHeight(currentBarHeight)
                                .clip(CircleShape)
                                .background(
                                    if (isPlaying) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (hasError) {
            Text(
                text = defaultErrorText,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Slider
        val activeValue = if (isSeeking) seekPosMs else currentPosMs.toFloat()
        val maxVal = durationMs.coerceAtLeast(1L).toFloat()

        Slider(
            value = activeValue.coerceIn(0f, maxVal),
            onValueChange = {
                isSeeking = true
                seekPosMs = it
            },
            onValueChangeFinished = {
                mediaPlayerRef?.seekTo(seekPosMs.toInt())
                currentPosMs = seekPosMs.toLong()
                isSeeking = false
            },
            valueRange = 0f..maxVal,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(activeValue.toLong()),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(durationMs),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Player Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed selector button
            Box {
                FilterChip(
                    selected = speed != 1.0f,
                    onClick = { showSpeedMenu = true },
                    label = { Text("${speed}x", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )

                DropdownMenu(
                    expanded = showSpeedMenu,
                    onDismissRequest = { showSpeedMenu = false }
                ) {
                    listOf(0.5f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                        DropdownMenuItem(
                            text = { Text("${spd}x", fontWeight = if (spd == speed) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                speed = spd
                                showSpeedMenu = false
                                mediaPlayerRef?.let { mp ->
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                        try {
                                            mp.playbackParams = mp.playbackParams.setSpeed(spd)
                                        } catch (_: Exception) {}
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Rewind -10s
            IconButton(
                onClick = {
                    mediaPlayerRef?.let { mp ->
                        val target = (mp.currentPosition - 10000).coerceAtLeast(0)
                        mp.seekTo(target)
                        currentPosMs = target.toLong()
                    }
                }
            ) {
                Icon(Icons.Default.FastRewind, contentDescription = "Rewind 10s")
            }

            // Play / Pause Main FAB
            FloatingActionButton(
                onClick = {
                    mediaPlayerRef?.let { mp ->
                        if (isPlaying) {
                            mp.pause()
                            isPlaying = false
                        } else {
                            mp.start()
                            isPlaying = true
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("audio_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(32.dp)
                )
            }

            // Fast Forward +10s
            IconButton(
                onClick = {
                    mediaPlayerRef?.let { mp ->
                        val maxD = if (durationMs > 0) durationMs.toInt() else mp.duration
                        val target = (mp.currentPosition + 10000).coerceAtMost(maxD)
                        mp.seekTo(target)
                        currentPosMs = target.toLong()
                    }
                }
            ) {
                Icon(Icons.Default.FastForward, contentDescription = "Forward 10s")
            }

            // Loop / Share
            IconButton(
                onClick = {
                    isLooping = !isLooping
                    mediaPlayerRef?.isLooping = isLooping
                }
            ) {
                Icon(
                    imageVector = if (isLooping) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Loop Toggle",
                    tint = if (isLooping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onShare != null) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
