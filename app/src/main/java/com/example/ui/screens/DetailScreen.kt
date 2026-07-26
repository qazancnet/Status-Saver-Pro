package com.example.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.StatusItem
import com.example.ui.components.AppInternalVideoPlayer
import com.example.util.t

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    status: StatusItem,
    onBack: () -> Unit,
    onEditClick: (StatusItem) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isVideoPlaying by remember { mutableStateOf(false) }
    val shareTitle = t("Statusu Paylaş", "Share Status", "Поделиться статусом", "Durumu Paylaş")

    // Automatically animate play status if it's a video for visual polish
    LaunchedEffect(status.id) {
        if (status.isVideo) {
            isVideoPlaying = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = status.fileName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (status.isVideo) t("Video Statusu", "Video Status", "Видео Статус", "Video Durumu") else t("Şəkil Statusu", "Image Status", "Фото Статус", "Resim Durumu"),
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                },
                navigationArrow = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = t("Geri", "Back", "Назад", "Geri"),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Core media display container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (status.isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        var playableUri by remember(status) { mutableStateOf<android.net.Uri?>(null) }
                        val context = LocalContext.current
                        LaunchedEffect(status) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                val uriStr = status.uri.toString()
                                if (uriStr.startsWith("http") || uriStr.startsWith("file://") || uriStr.startsWith("/")) {
                                    playableUri = status.uri
                                } else {
                                    try {
                                        val cacheFile = java.io.File(context.cacheDir, "status_preview_${status.fileName.hashCode()}.mp4")
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
                            AppInternalVideoPlayer(
                                videoUri = resolvedUri,
                                title = status.fileName,
                                onClose = onBack,
                                onShare = {
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "video/*"
                                        putExtra(android.content.Intent.EXTRA_STREAM, resolvedUri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Video"))
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
                                    modifier = Modifier.testTag("video_loading_indicator")
                                )
                            }
                        }
                    }
                } else {
                    // Photo Display with rich scale representation
                    AsyncImage(
                        model = status.uri,
                        contentDescription = status.fileName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Bottom Core Controls Panel matching Vibrant Palette HTML
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Actions Header
                Text(
                    text = t("STATUS FUNKSİYALARI", "STATUS FEATURES", "ФУНКЦИИ СТАТУСА", "DURUM ÖZELLİKLERİ"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Save to Gallery Button (Primary Core Action)
                Button(
                    onClick = onSaveClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (status.isSaved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(16.dp), // Premium rounded rectangular pill
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("action_save")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (status.isSaved) t("Status Qalereyada Saxlanılıb", "Status Saved to Gallery", "Статус сохранен в галерею", "Durum Galeriye Kaydedildi") else t("Statusu Yadda Saxla", "Save Status", "Сохранить статус", "Durumu Kaydet"),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Secondary Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit Button
                    ActionButtonWithLabel(
                        icon = Icons.Default.Edit,
                        label = t("Redaktə Et", "Edit", "Редактировать", "Düzenle"),
                        onClick = { onEditClick(status) },
                        modifier = Modifier.testTag("action_edit")
                    )

                    // Share Option
                    ActionButtonWithLabel(
                        icon = Icons.Default.Share,
                        label = "",
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_STREAM, status.uri)
                                type = if (status.isVideo) "video/*" else "image/*"
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, shareTitle))
                        },
                        modifier = Modifier.testTag("action_share")
                    )

                    // Delete Option
                    ActionButtonWithLabel(
                        icon = Icons.Default.Delete,
                        label = t("Sil", "Delete", "Удалить", "Sil"),
                        onClick = onDeleteClick,
                        isDestructive = true,
                        modifier = Modifier.testTag("action_delete")
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButtonWithLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isDestructive) MaterialTheme.colorScheme.errorContainer 
                    else MaterialTheme.colorScheme.secondaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isDestructive) MaterialTheme.colorScheme.onErrorContainer 
                       else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        if (label.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

// For compatibility backbutton parameter
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    navigationArrow: @Composable () -> Unit,
    colors: androidx.compose.material3.TopAppBarColors,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = title,
        navigationIcon = navigationArrow,
        colors = colors,
        modifier = modifier
    )
}
