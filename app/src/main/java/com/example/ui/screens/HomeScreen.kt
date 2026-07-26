package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.StatusItem
import com.example.util.LanguageManager
import com.example.util.t
import com.example.viewmodel.StatusViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: StatusViewModel,
    onStatusClick: (StatusItem) -> Unit,
    onFileManagerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val autoSave by viewModel.autoSaveEnabled.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val coins by viewModel.coinsBalance.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        (context as? Activity)?.let { act ->
            viewModel.initUnityAds(act)
        }
    }

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedStatuses = remember { mutableStateListOf<StatusItem>() }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "About",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = t("Tətbiq Haqqında", "About App", "О приложении", "Uygulama Hakkında"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "StatusSaver Pro",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = t(
                            "Bu proqram təminatı Ucoin Software Technologies tərəfindən yüksək keyfiyyət və təhlükəsizlik standartlarına uyğun olaraq layihələndirilmiş və işlənib hazırlanmışdır.",
                            "This software was engineered and developed by Ucoin Software Technologies adhering to high quality and security standards.",
                            "Данное программное обеспечение разработано и спроектировано Ucoin Software Technologies в соответствии с высокими стандартами качества.",
                            "Bu yazılım Ucoin Software Technologies tarafından yüksek kalite ve güvenlik standartlarına uygun olarak geliştirilmiştir."
                        ),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "🚀 Powered by Ucoin Technologies",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text(t("Aydın", "Got it", "Понятно", "Anlaşıldı"))
                }
            }
        )
    }

    // Top up & Ad overlay triggers
    if (viewModel.showTopUpDialog) {
        TopUpDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showTopUpDialog = false }
        )
    }

    if (viewModel.isWatchingAd) {
        UnityAdsSimulatedOverlay(
            onAdCompleted = {
                viewModel.completeSimulatedAd {
                    val pending = viewModel.pendingStatusToSave
                    if (pending != null && (coins + 5) >= 10) {
                        viewModel.saveStatus(pending)
                        viewModel.pendingStatusToSave = null
                        viewModel.showTopUpDialog = false
                    }
                }
            },
            onAdDismissed = {
                viewModel.isWatchingAd = false
            }
        )
    }

    val currentTime = System.currentTimeMillis()
    val activeStatuses = remember(uiState.statuses) {
        uiState.statuses
    }

    val expiredStatuses = remember { emptyList<StatusItem>() }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setSafFolderUri(uri)
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedStatuses.size} ${t("Seçildi", "Selected", "Выбрано", "Seçildi")}",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedStatuses.clear()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val allList = activeStatuses + expiredStatuses
                            if (selectedStatuses.size == allList.size) {
                                selectedStatuses.clear()
                            } else {
                                selectedStatuses.clear()
                                selectedStatuses.addAll(allList)
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        if (selectedStatuses.isNotEmpty()) {
                            IconButton(onClick = {
                                selectedStatuses.forEach { status ->
                                    viewModel.saveStatus(status)
                                }
                                Toast.makeText(context, LanguageManager.translate("Seçilənlər saxlanıldı!", "Selected saved!", "Выбранные сохранены!", "Seçilenler kaydedildi!"), Toast.LENGTH_SHORT).show()
                                isSelectionMode = false
                                selectedStatuses.clear()
                            }) {
                                Icon(Icons.Default.Download, contentDescription = "Save Selected", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                selectedStatuses.forEach { status ->
                                    viewModel.deleteStatus(status)
                                }
                                isSelectionMode = false
                                selectedStatuses.clear()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "StatusSaver Pro",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    actions = {
                        // Coin Balance Chip
                        Surface(
                            onClick = { viewModel.showTopUpDialog = true },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("coin_balance_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = "Coins",
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "$coins",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Refresh Statuses Button
                        IconButton(onClick = { viewModel.refreshStatuses() }, modifier = Modifier.testTag("refresh_button")) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = t("Yenilə", "Refresh", "Обновить", "Yenile")
                            )
                        }

                        // File Manager Quick Access Button
                        IconButton(onClick = onFileManagerClick, modifier = Modifier.testTag("file_manager_button")) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = t("Fayl Meneceri", "File Manager", "Файловый менеджер", "Dosya Yöneticisi"),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Language Selection & Info Menu
                        Box {
                            IconButton(onClick = { showLanguageMenu = true }, modifier = Modifier.testTag("language_button")) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = t("Dil və Haqqında", "Language & About", "Язык и Инфо", "Dil ve Hakkında")
                                )
                            }
                            DropdownMenu(
                                expanded = showLanguageMenu,
                                onDismissRequest = { showLanguageMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("🇦🇿 Azərbaycan") },
                                    onClick = {
                                        LanguageManager.setLanguage("az")
                                        showLanguageMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🇬🇧 English") },
                                    onClick = {
                                        LanguageManager.setLanguage("en")
                                        showLanguageMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🇷🇺 Русский") },
                                    onClick = {
                                        LanguageManager.setLanguage("ru")
                                        showLanguageMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🇹🇷 Türkçe") },
                                    onClick = {
                                        LanguageManager.setLanguage("tr")
                                        showLanguageMenu = false
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = t("Haqqında / Ucoin", "About / Ucoin", "О приложении / Ucoin", "Hakkında / Ucoin"),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    },
                                    onClick = {
                                        showLanguageMenu = false
                                        showAboutDialog = true
                                    }
                                )
                            }
                        }
                    }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Loading Progress Bar
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().testTag("loading_indicator"))
            }

            // Category Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedFilter,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val imageCount = uiState.allStatuses.count { !it.isVideo }
                val videoCount = uiState.allStatuses.count { it.isVideo }
                val savedCount = uiState.allStatuses.count { it.isSaved }
                val totalCount = uiState.allStatuses.size

                val tabItems = listOf(
                    Triple(t("Hamısı ($totalCount)", "All ($totalCount)", "Все ($totalCount)", "Hepsi ($totalCount)"), Icons.Default.AllInclusive, "All"),
                    Triple(t("Şəkillər ($imageCount)", "Images ($imageCount)", "Картинки ($imageCount)", "Resimler ($imageCount)"), Icons.Default.Image, "Images"),
                    Triple(t("Videolar ($videoCount)", "Videos ($videoCount)", "Видео ($videoCount)", "Videolar ($videoCount)"), Icons.Default.Videocam, "Videos"),
                    Triple(t("Saxlanılan ($savedCount)", "Saved ($savedCount)", "Сохраненные ($savedCount)", "Kaydedilen ($savedCount)"), Icons.Default.Bookmark, "Saved")
                )

                tabItems.forEachIndexed { index, item ->
                    Tab(
                        selected = uiState.selectedFilter == index,
                        onClick = { viewModel.setFilter(index) },
                        icon = {
                            Icon(
                                imageVector = item.second,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        text = {
                            Text(
                                text = item.first,
                                fontWeight = if (uiState.selectedFilter == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Auto-Save Controls Card & Storage Access Banner
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Autorenew,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = t("Avtomatik Yadda Saxla", "Auto Save Statuses", "Автосохранение", "Otomatik Kaydet"),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = t("Baxdığınız statuslar avtomatik qalereyaya yazılır", "Auto-saves viewed statuses to gallery", "Сохраняет просмотры в галерею", "İzlediğiniz durumlar otomatik kaydedilir"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                Switch(
                                    checked = autoSave,
                                    onCheckedChange = { viewModel.toggleAutoSave() },
                                    modifier = Modifier.testTag("auto_save_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // WhatsApp SAF Folder Selection
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val initialUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fmedia")
                                        folderPickerLauncher.launch(initialUri)
                                    } catch (e: Exception) {
                                        try { folderPickerLauncher.launch(null) } catch (e2: Exception) {}
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("change_folder_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.hasFolderPermission)
                                        t("WhatsApp Qovluğunu Dəyiş", "Change WhatsApp Folder", "Изменить папку WhatsApp", "WhatsApp Klasörünü Değiştir")
                                    else
                                        t("WhatsApp Status Qovluğunu Seç", "Select WhatsApp Status Folder", "Выбрать папку WhatsApp", "WhatsApp Durum Klasörünü Seç"),
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Open Categorized WhatsApp File Manager Button
                            FilledTonalButton(
                                onClick = onFileManagerClick,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("open_file_manager_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = t("WhatsApp Fayl Menecerini Aç (Kateqoriyalarla)", "Open WhatsApp File Manager (Categorized)", "Открыть файловый менеджер WhatsApp (По категориям)", "WhatsApp Dosya Yöneticisini Aç (Kategorili)"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Empty State if no status items exist
                if (activeStatuses.isEmpty() && expiredStatuses.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = t("Hələ ki heç bir status tapılmadı", "No statuses found yet", "Статусы пока не найдены", "Henüz durum bulunamadı"),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = t(
                                        "WhatsApp tətbiqində statusları izlədikdən sonra onlar bura avtomatik düşəcəkdir.",
                                        "Statuses will appear here automatically after you view them in WhatsApp.",
                                        "Статусы появятся здесь автоматически после просмотра в WhatsApp.",
                                        "WhatsApp'ta durumları izledikten sonra burada otomatik görünecektir."
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Active Statuses
                    items(activeStatuses, key = { it.id }) { status ->
                        val isSelected = selectedStatuses.contains(status)
                        StatusGridItem(
                            status = status,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelectionMode) {
                                    if (isSelected) selectedStatuses.remove(status) else selectedStatuses.add(status)
                                    if (selectedStatuses.isEmpty()) isSelectionMode = false
                                } else {
                                    onStatusClick(status)
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedStatuses.add(status)
                                }
                            },
                            onSaveClick = { viewModel.saveStatus(status) },
                            onDeleteClick = { viewModel.deleteStatus(status) },
                            onShareClick = {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_STREAM, status.uri)
                                    type = if (status.isVideo) "video/*" else "image/*"
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, LanguageManager.translate("Statusu Paylaş", "Share Status", "Поделиться", "Durumu Paylaş")))
                            }
                        )
                    }

                    // Expired (>24 Hours) Statuses
                    if (expiredStatuses.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = t("Köhnə Statuslar (>24 saat)", "Older Statuses (>24h)", "Старые статусы (>24ч)", "Eski Durumlar (>24s)"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(expiredStatuses, key = { "exp_${it.id}" }) { status ->
                            val isSelected = selectedStatuses.contains(status)
                            StatusGridItem(
                                status = status,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelectionMode) {
                                        if (isSelected) selectedStatuses.remove(status) else selectedStatuses.add(status)
                                        if (selectedStatuses.isEmpty()) isSelectionMode = false
                                    } else {
                                        onStatusClick(status)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedStatuses.add(status)
                                    }
                                },
                                onSaveClick = { viewModel.saveStatus(status) },
                                onDeleteClick = { viewModel.deleteStatus(status) },
                                onShareClick = {
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_STREAM, status.uri)
                                        type = if (status.isVideo) "video/*" else "image/*"
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, LanguageManager.translate("Statusu Paylaş", "Share Status", "Поделиться", "Durumu Paylaş")))
                                }
                            )
                        }
                    }

                    // Developer credit footer badge inside LazyVerticalGrid item scope
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t(
                                    "© Ucoin Software Technologies | Bütün hüquqlar qorunur",
                                    "© Ucoin Software Technologies | All rights reserved",
                                    "© Ucoin Software Technologies | Все права защищены",
                                    "© Ucoin Software Technologies | Tüm hakları saklıdır"
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Status Item Card with explicit, legible Download, Share, and Delete action buttons directly on the card!
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatusGridItem(
    status: StatusItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("status_item_${status.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Media Thumbnail
            AsyncImage(
                model = status.uri,
                contentDescription = status.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Video Play Indicator Badge
            if (status.isVideo) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Video",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Is Saved Indicator Badge (Top-Left)
            if (status.isSaved) {
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 12.dp, topStart = 16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = t("Saxlanıldı", "Saved", "Сохранено", "Kaydedildi"),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Multi-selection Overlay
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // CRITICAL ACTION OVERLAY BUTTONS (YÜKLƏ, SİL, PAYLAŞ) AT THE BOTTOM OF CARD
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // YÜKLƏ (Save / Download) Button
                    IconButton(
                        onClick = onSaveClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("download_button_${status.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = t("Yüklə", "Download", "Скачать", "Yükle"),
                            tint = if (status.isSaved) Color(0xFF4CAF50) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // PAYLAŞ (Share) Button
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("share_button_${status.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = t("Paylaş", "Share", "Поделиться", "Paylaş"),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // SİL (Delete) Button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_button_${status.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = t("Sil", "Delete", "Удалить", "Sil"),
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * TopUp Dialog for earning / purchasing Coins
 */
@Composable
fun TopUpDialog(
    viewModel: StatusViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFB300))
                Spacer(modifier = Modifier.width(8.dp))
                Text(t("Coin Balansı Artır", "Top Up Coins", "Пополнить монеты", "Coin Bakiyesini Artır"))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    t(
                        "Statusları yukləmək və redaktə etmək üçün Coin tələb olunur.",
                        "Coins are required for downloading and editing statuses.",
                        "Монеты необходимы для скачивания и редактирования статусов.",
                        "Durumları indirmek ve düzenlemek için Coin gereklidir."
                    ),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Unity Ads ID: 5595316 | Rewarded & Interstitial",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp)
                    )
                }

                // Option 1: Watch Rewarded Ad (+5 Coins)
                Button(
                    onClick = {
                        if (activity != null) {
                            viewModel.watchAdToEarnCoins(activity) {
                                viewModel.triggerCoinAnimation(5)
                                onDismiss()
                            }
                        } else {
                            viewModel.isWatchingAd = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("watch_ad_button")
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(t("Unity Rewarded Ad İzlə (+5 Coin)", "Watch Unity Rewarded Ad (+5 Coins)", "Смотреть рекламу Unity (+5)", "Unity Rewarded Ad İzle (+5 Coin)"), fontWeight = FontWeight.Bold)
                }

                // Option 2: Watch Interstitial Ad (+3 Coins)
                OutlinedButton(
                    onClick = {
                        if (activity != null) {
                            viewModel.showInterstitialAd(activity) {
                                viewModel.completeSimulatedAd {
                                    viewModel.triggerCoinAnimation(3)
                                    onDismiss()
                                }
                            }
                        } else {
                            viewModel.isWatchingAd = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("watch_interstitial_ad_button")
                ) {
                    Icon(Icons.Default.FeaturedPlayList, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(t("Unity Interstitial Ad İzlə (+3 Coin)", "Watch Unity Interstitial Ad (+3 Coins)", "Смотреть Interstitial рекламу (+3)", "Unity Interstitial Ad İzle (+3 Coin)"))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(t("Bağla", "Close", "Закрыть", "Kapat"))
            }
        }
    )
}

/**
 * Simulated Unity Ads Overlay (Fallback if live Unity Ads fails)
 */
@Composable
fun UnityAdsSimulatedOverlay(
    onAdCompleted: () -> Unit,
    onAdDismissed: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(5) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
    }

    Dialog(
        onDismissRequest = { if (timeLeft <= 0) onAdDismissed() },
        properties = DialogProperties(dismissOnBackPress = (timeLeft <= 0), dismissOnClickOutside = (timeLeft <= 0))
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(56.dp)
                )

                Text(
                    text = t("Sponsorlu Reklam", "Sponsored Ad", "Спонсорская реклама", "Sponsorlu Reklam"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                LinearProgressIndicator(
                    progress = { (5 - timeLeft) / 5f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFFFB300),
                    trackColor = Color.White.copy(alpha = 0.2f)
                )

                Text(
                    text = if (timeLeft > 0)
                        t("Reklam gedir: $timeLeft saniyə...", "Ad playing: $timeLeft seconds...", "Реклама идет: $timeLeft сек...", "Reklam izleniyor: $timeLeft saniye...")
                    else
                        t("Reklam tamamlandı!", "Ad completed!", "Реклама завершена!", "Reklam tamamlandı!"),
                    color = Color.LightGray,
                    fontSize = 13.sp
                )

                if (timeLeft <= 0) {
                    Button(
                        onClick = onAdCompleted,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(t("REKLAMI BAĞLA VƏ +5 COIN QAZAN", "CLOSE AD & EARN +5 COINS", "ЗАКРЫТЬ И ПОЛУЧИТЬ +5 МОНЕТ", "KAPAT VE +5 COIN KAZAN"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
