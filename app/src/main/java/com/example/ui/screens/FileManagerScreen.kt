package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.FileItem
import com.example.ui.components.AppInternalAudioPlayerDialog
import com.example.ui.components.AppInternalDocumentViewerDialog
import com.example.ui.components.AppInternalStickerViewerDialog
import com.example.ui.components.AppInternalVideoPlayer
import com.example.util.t
import com.example.viewmodel.FileManagerViewModel
import com.example.viewmodel.StatusViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileManagerScreen(
    viewModel: StatusViewModel,
    fileManagerViewModel: FileManagerViewModel,
    onBack: () -> Unit
) {
    val files by fileManagerViewModel.files.collectAsState()
    val isLoading by fileManagerViewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // Trigger load on entering screen
    LaunchedEffect(Unit) {
        fileManagerViewModel.loadFiles()
    }

    var selectedCategory by remember { mutableStateOf("Images") }
    var selectedDirection by remember { mutableStateOf("ALL") } // ALL, RECEIVED, SENT
    var searchQuery by remember { mutableStateOf("") }
    var isGalleryGridMode by remember { mutableStateOf(true) } // Defaults to Gallery Grid view

    // Map internal raw category to localized name
    @Composable
    fun getCategoryLabel(catKey: String): String {
        return when (catKey) {
            "All" -> t("Hamısı", "All", "Все", "Hepsi")
            "Images" -> t("Şəkillər", "Images", "Изображения", "Fotoğraflar")
            "Videos" -> t("Videolar", "Videos", "Видео", "Videolar")
            "Voice Notes" -> t("Səs Yazıları", "Voice Notes", "Голосовые", "Ses Notları")
            "Audio" -> t("Audio", "Audio", "Аудио", "Sesler")
            "Documents" -> t("Sənədlər", "Documents", "Документы", "Belgeler")
            "Stickers" -> t("Stikerlər", "Stickers", "Стикеры", "Çıkartmalar")
            "GIFs" -> t("GIF-lər", "GIFs", "ГИФ-ки", "GIF'ler")
            "Profile Photos" -> t("Profil Şəkilləri", "Profile Photos", "Фото профиля", "Profil Fotoğrafları")
            "Wallpapers" -> t("Divar Kağızları", "Wallpapers", "Обои", "Duvar Kağıtları")
            else -> t("Digər", "Other", "Другое", "Diğer")
        }
    }

    fun getCategoryIcon(catKey: String): androidx.compose.ui.graphics.vector.ImageVector {
        return when (catKey) {
            "All" -> Icons.Default.AllInclusive
            "Images" -> Icons.Default.Image
            "Videos" -> Icons.Default.Videocam
            "Voice Notes" -> Icons.Default.Mic
            "Audio" -> Icons.Default.Audiotrack
            "Documents" -> Icons.Default.Description
            "Stickers" -> Icons.Default.EmojiEmotions
            "GIFs" -> Icons.Default.Gif
            "Profile Photos" -> Icons.Default.AccountCircle
            "Wallpapers" -> Icons.Default.Wallpaper
            else -> Icons.Default.Folder
        }
    }

    val categories = remember(files) {
        val predefinedOrder = listOf(
            "Images",
            "Videos",
            "Audio",
            "Voice Notes",
            "Documents",
            "Stickers",
            "GIFs",
            "Profile Photos",
            "Wallpapers"
        )
        val presentCats = files.map { it.category }.distinct().filter { it.isNotEmpty() }
        val orderedPresent = predefinedOrder.filter { it in presentCats } + (presentCats - predefinedOrder.toSet()).sorted()
        orderedPresent + "All"
    }

    val filteredFiles = remember(files, selectedCategory, selectedDirection, searchQuery) {
        files.filter { file ->
            val matchesCategory = if (selectedCategory == "All") true else file.category == selectedCategory
            val matchesDirection = when (selectedDirection) {
                "RECEIVED" -> !file.isSent
                "SENT" -> file.isSent
                else -> true
            }
            val matchesQuery = if (searchQuery.isBlank()) true else file.name.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesDirection && matchesQuery
        }
    }

    var fileToDelete by remember { mutableStateOf<FileItem?>(null) }
    var previewFile by remember { mutableStateOf<FileItem?>(null) }
    var fullScreenMediaFile by remember { mutableStateOf<FileItem?>(null) }
    var audioFileToPlay by remember { mutableStateOf<FileItem?>(null) }
    var stickerFileToView by remember { mutableStateOf<FileItem?>(null) }
    var docFileToView by remember { mutableStateOf<FileItem?>(null) }

    val fileDeletedMsg = t("Fayl bütün yerlərdən silindi", "File permanently deleted from everywhere", "Файл полностью удален", "Dosya her yerden silindi")
    val cannotOpenMsg = t("Faylı açmaq mümkün olmadı", "Cannot open file", "Не удалось открыть файл", "Dosya açılamadı")
    val cannotShareMsg = t("Paylaşmaq mümkün olmadı", "Cannot share file", "Не удалось поделиться", "Paylaşılamadı")

    fun openFileInternal(file: FileItem) {
        when {
            file.mimeType.startsWith("video/") -> {
                fullScreenMediaFile = file
            }
            file.mimeType.startsWith("audio/") || file.category == "Voice Notes" || file.category == "Audio" -> {
                audioFileToPlay = file
            }
            file.category == "Stickers" || file.category == "GIFs" || file.mimeType.contains("webp") -> {
                stickerFileToView = file
            }
            file.mimeType.startsWith("image/") -> {
                previewFile = file
            }
            else -> {
                docFileToView = file
            }
        }
    }

    // Internal Audio Player Dialog
    if (audioFileToPlay != null) {
        AppInternalAudioPlayerDialog(
            audioUri = audioFileToPlay!!.uri,
            title = audioFileToPlay!!.name,
            subtitle = getCategoryLabel(audioFileToPlay!!.category),
            onDismiss = { audioFileToPlay = null },
            onShare = { shareFile(context, audioFileToPlay!!, cannotShareMsg) },
            onDelete = {
                val item = audioFileToPlay
                audioFileToPlay = null
                fileToDelete = item
            }
        )
    }

    // Internal Sticker / GIF Viewer Dialog
    if (stickerFileToView != null) {
        AppInternalStickerViewerDialog(
            mediaUri = stickerFileToView!!.uri,
            title = stickerFileToView!!.name,
            mimeType = stickerFileToView!!.mimeType,
            onDismiss = { stickerFileToView = null },
            onShare = { shareFile(context, stickerFileToView!!, cannotShareMsg) },
            onDelete = {
                val item = stickerFileToView
                stickerFileToView = null
                fileToDelete = item
            }
        )
    }

    // Internal Document Viewer Dialog
    if (docFileToView != null) {
        AppInternalDocumentViewerDialog(
            docUri = docFileToView!!.uri,
            title = docFileToView!!.name,
            filePath = docFileToView!!.filePath,
            fileSizeFormatted = formatSize(docFileToView!!.size),
            mimeType = docFileToView!!.mimeType,
            onDismiss = { docFileToView = null },
            onShare = { shareFile(context, docFileToView!!, cannotShareMsg) },
            onDelete = {
                val item = docFileToView
                docFileToView = null
                fileToDelete = item
            }
        )
    }

    // Delete dialog with prominent warning
    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    t("Faylı həmişəlik sil?", "Permanently delete file?", "Удалить файл навсегда?", "Dosyayı kalıcı olarak sil?"),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = fileToDelete!!.name,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        t(
                            "XƏBƏRDARLIQ: Bu fayl tətbiqdən və cihazın daxili yaddaşından (bütün yerlərdən) həmişəlik silinəcək. Bu əməliyyat geri qaytarıla bilməz!",
                            "WARNING: This file will be permanently deleted from the app and internal device storage (everywhere). This operation cannot be undone!",
                            "ПРЕДУПРЕЖДЕНИЕ: Этот файл будет навсегда удален из приложения и памяти устройства (отовсюду). Действие нельзя отменить!",
                            "UYARI: Bu dosya uygulamadan ve cihaz hafızasından (her yerden) kalıcı olarak silinecektir. Bu işlem geri alınamaz!"
                        ),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val item = fileToDelete!!
                        fileManagerViewModel.deleteFile(item)
                        fileToDelete = null
                        Toast.makeText(context, fileDeletedMsg, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(t("Həmişəlik Sil", "Permanently Delete", "Удалить навсегда", "Kalıcı Sil"), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { fileToDelete = null }) {
                    Text(t("Ləğv et", "Cancel", "Отмена", "İptal"))
                }
            }
        )
    }

    // Media Preview Dialog (Images)
    if (previewFile != null) {
        Dialog(onDismissRequest = { previewFile = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = previewFile!!.name,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { previewFile = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (previewFile!!.mimeType.startsWith("image/")) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = { fullScreenMediaFile = previewFile },
                                    onLongClick = { openFileSafely(context, previewFile!!, cannotOpenMsg) }
                                )
                        ) {
                            AsyncImage(
                                model = previewFile!!.uri,
                                contentDescription = previewFile!!.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Full Screen",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        OutlinedButton(
                            onClick = {
                                openFileSafely(context, previewFile!!, cannotOpenMsg)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(t("Xarici Aç", "External Open", "Открыть во внешнем", "Dışarıda Aç"), fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = {
                                fullScreenMediaFile = previewFile
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(t("Tam Göster", "Full View", "На весь экран", "Tam Göster"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                shareFile(context, previewFile!!, cannotShareMsg)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        }

                        Button(
                            onClick = {
                                val item = previewFile
                                previewFile = null
                                fileToDelete = item
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(t("Sil", "Delete", "Удалить", "Sil"), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Full Screen Media Overlay (Videos / Full Screen Photos)
    if (fullScreenMediaFile != null) {
        Dialog(
            onDismissRequest = { fullScreenMediaFile = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (fullScreenMediaFile!!.mimeType.startsWith("video/")) {
                    AppInternalVideoPlayer(
                        videoUri = fullScreenMediaFile!!.uri,
                        title = fullScreenMediaFile!!.name,
                        onClose = { fullScreenMediaFile = null },
                        onShare = { shareFile(context, fullScreenMediaFile!!, cannotShareMsg) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    if (fullScreenMediaFile!!.mimeType.startsWith("image/")) {
                        AsyncImage(
                            model = fullScreenMediaFile!!.uri,
                            contentDescription = fullScreenMediaFile!!.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(fullScreenMediaFile!!.name, color = Color.White)
                        }
                    }

                    // Full Screen Top Controls Bar for Images & Non-video files
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { fullScreenMediaFile = null },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        Row {
                            IconButton(
                                onClick = { openFileSafely(context, fullScreenMediaFile!!, cannotOpenMsg) },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = "Open", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { shareFile(context, fullScreenMediaFile!!, cannotShareMsg) },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    val item = fullScreenMediaFile
                                    fullScreenMediaFile = null
                                    fileToDelete = item
                                },
                                modifier = Modifier.background(Color.Red.copy(alpha = 0.85f), CircleShape)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            t("WhatsApp Fayl Meneceri", "WhatsApp File Manager", "Файловый менеджер WhatsApp", "WhatsApp Dosya Yöneticisi"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "${filteredFiles.size} " + t("fayl", "files", "файлов", "dosya"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // View Mode Toggle Button (List vs Gallery Grid)
                    IconButton(onClick = { isGalleryGridMode = !isGalleryGridMode }) {
                        Icon(
                            imageVector = if (isGalleryGridMode) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View Mode"
                        )
                    }
                    IconButton(onClick = { fileManagerViewModel.loadFiles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(t("Fayl adını axtar...", "Search file name...", "Поиск файла...", "Dosya adı ara...")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Direction Filter Bar (All, Received, Sent)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val allCount = files.size
                val receivedCount = files.count { !it.isSent }
                val sentCount = files.count { it.isSent }

                item {
                    FilterChip(
                        selected = selectedDirection == "ALL",
                        onClick = { selectedDirection = "ALL" },
                        label = { Text(t("Hamısı", "All", "Все", "Tümü") + " ($allCount)", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.AllInclusive,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = selectedDirection == "RECEIVED",
                        onClick = { selectedDirection = "RECEIVED" },
                        label = { Text(t("Qəbul", "Received", "Получено", "Alınan") + " ($receivedCount)", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.SouthWest,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (selectedDirection == "RECEIVED") Color.White else Color(0xFF2E7D32)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2E7D32),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = selectedDirection == "SENT",
                        onClick = { selectedDirection = "SENT" },
                        label = { Text(t("Göndərilən", "Sent", "Отправлено", "Gönderilen") + " ($sentCount)", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.NorthEast,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (selectedDirection == "SENT") Color.White else Color(0xFFE65100)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE65100),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Categories Filter Chips with Icons
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { categoryKey ->
                    val isSelected = selectedCategory == categoryKey
                    val count = if (categoryKey == "All") files.size else files.count { it.category == categoryKey }
                    val labelText = "${getCategoryLabel(categoryKey)} ($count)"

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = categoryKey },
                        label = { Text(labelText, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            Icon(
                                imageVector = getCategoryIcon(categoryKey),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (filteredFiles.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            t("Fayl tapılmadı", "No files found", "Файлы не найдены", "Dosya bulunamadı"),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (isGalleryGridMode) {
                    // QALEREYA / GRID MODE
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredFiles, key = { "${it.filePath}_${it.uri}_${it.name}" }) { file ->
                            FileItemGridCard(
                                file = file,
                                categoryLabel = getCategoryLabel(file.category),
                                onClick = { openFileInternal(file) },
                                onShareClick = { shareFile(context, file, cannotShareMsg) },
                                onDeleteClick = { fileToDelete = file }
                            )
                        }
                    }
                } else {
                    // SIYAHI / LIST MODE
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredFiles, key = { "${it.filePath}_${it.uri}_${it.name}" }) { file ->
                            FileItemRow(
                                file = file,
                                categoryLabel = getCategoryLabel(file.category),
                                onClick = { openFileInternal(file) },
                                onShareClick = { shareFile(context, file, cannotShareMsg) },
                                onDeleteClick = { fileToDelete = file }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemGridCard(
    file: FileItem,
    categoryLabel: String,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDeleteClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image / Thumbnail / Icon Box
            if (file.mimeType.startsWith("image/") || file.category == "Stickers" || file.category == "GIFs") {
                AsyncImage(
                    model = file.uri,
                    contentDescription = file.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            when {
                                file.mimeType.startsWith("video/") -> MaterialTheme.colorScheme.secondaryContainer
                                file.mimeType.startsWith("audio/") || file.category == "Voice Notes" -> MaterialTheme.colorScheme.tertiaryContainer
                                file.mimeType == "application/pdf" -> Color(0xFFFFEBEE)
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            file.mimeType.startsWith("video/") -> Icons.Default.Videocam
                            file.category == "Voice Notes" -> Icons.Default.Mic
                            file.mimeType.startsWith("audio/") -> Icons.Default.Audiotrack
                            file.mimeType == "application/pdf" -> Icons.Default.PictureAsPdf
                            else -> Icons.Default.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Overlay Badge for Videos or Voice Notes
            if (file.mimeType.startsWith("video/")) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else if (file.mimeType.startsWith("audio/") || file.category == "Voice Notes") {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Audio",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Top Badges (Category & Direction)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip Badge
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = categoryLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Direction Badge (Sent vs Received)
                Surface(
                    color = if (file.isSent) Color(0xFFE65100).copy(alpha = 0.9f) else Color(0xFF2E7D32).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (file.isSent) Icons.Default.NorthEast else Icons.Default.SouthWest,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (file.isSent) t("Göndərildi", "Sent", "Отправлено", "Gönderildi") else t("Qəbul edildi", "Received", "Получено", "Alındı"),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Bottom Info Bar (File Name, Size, Quick Share)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatSize(file.size),
                        color = Color.LightGray,
                        fontSize = 10.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileItemRow(
    file: FileItem,
    categoryLabel: String,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDeleteClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail or Category Icon
            if (file.mimeType.startsWith("image/") || file.category == "Stickers" || file.category == "GIFs") {
                AsyncImage(
                    model = file.uri,
                    contentDescription = file.name,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(
                        when {
                            file.mimeType.startsWith("video/") -> MaterialTheme.colorScheme.secondaryContainer
                            file.mimeType.startsWith("audio/") || file.category == "Voice Notes" -> MaterialTheme.colorScheme.tertiaryContainer
                            file.mimeType == "application/pdf" -> Color(0xFFFFEBEE)
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            file.mimeType.startsWith("video/") -> Icons.Default.Videocam
                            file.category == "Voice Notes" -> Icons.Default.Mic
                            file.mimeType.startsWith("audio/") -> Icons.Default.Audiotrack
                            file.mimeType == "application/pdf" -> Icons.Default.PictureAsPdf
                            file.category == "Stickers" -> Icons.Default.EmojiEmotions
                            file.category == "GIFs" -> Icons.Default.Gif
                            else -> Icons.Default.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = categoryLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = if (file.isSent) Color(0xFFFFE0B2) else Color(0xFFC8E6C9),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (file.isSent) Icons.Default.NorthEast else Icons.Default.SouthWest,
                                contentDescription = null,
                                tint = if (file.isSent) Color(0xFFE65100) else Color(0xFF2E7D32),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (file.isSent) t("Göndərildi", "Sent", "Отправлено", "Gönderildi") else t("Qəbul edildi", "Received", "Получено", "Alındı"),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (file.isSent) Color(0xFFE65100) else Color(0xFF2E7D32)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SdStorage,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = formatSize(file.size),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = formatDate(file.lastModified),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(onClick = onShareClick) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun openFileSafely(context: android.content.Context, file: FileItem, errorMsg: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        if (file.uri.scheme == "content") {
            intent.setDataAndType(file.uri, file.mimeType)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else if (file.filePath.startsWith("/")) {
            val localFile = File(file.filePath)
            if (localFile.exists()) {
                val contentUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", localFile)
                intent.setDataAndType(contentUri, file.mimeType)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                intent.setDataAndType(file.uri, file.mimeType)
            }
        } else {
            intent.setDataAndType(file.uri, file.mimeType)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
    }
}

private fun shareFile(context: android.content.Context, file: FileItem, errorMsg: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = file.mimeType
            putExtra(Intent.EXTRA_STREAM, file.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    } catch (e: Exception) {
        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
    }
}

private fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
