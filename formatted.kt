package com.example.ui.screens

import android.net.Uri
import android.app.Activity
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Translate
import com.example.util.LanguageManager
import com.example.util.t
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import coil.compose.AsyncImage
import com.example.model.StatusItem
import com.example.viewmodel.StatusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
viewModel: StatusViewModel,
onStatusClick: (StatusItem) -> Unit,
modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val autoSave by viewModel.autoSaveEnabled.collectAsState()
    val coins by viewModel.coinsBalance.collectAsState()
    val context = LocalContext.current
    
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedStatuses = remember { mutableStateListOf<StatusItem>() }
    
    var showLanguageMenu by remember { mutableStateOf(false) }
    
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
    val activeStatuses = remember(uiState.statuses, uiState.selectedFilter, currentTime) {
        uiState.statuses.filter {
            uiState.selectedFilter == 3 || (currentTime - it.dateModified <= 24 * 60 * 60 * 1000L)
        }
    }
    val expiredStatuses = remember(uiState.statuses, uiState.selectedFilter, currentTime) {
        uiState.statuses.filter {
            uiState.selectedFilter != 3 && (currentTime - it.dateModified > 24 * 60 * 60 * 1000L)
        }
    }
    
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    val tabs = listOf(
    t("Hamısı", "All", "Все", "Hepsi"),
    t("Şəkillər", "Images", "Изображения", "Resimler"),
    t("Videolar", "Videos", "Видео", "Videolar"),
    t("Saxlanılanlar", "Saved", "Сохраненные", "Kaydedilenler")
    )
    
    // DocumentTree picker launcher for SAF WhatsApp folder
    val folderPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
    if (uri != null) {
        // Persist permission
        context.contentResolver.takePersistableUriPermission(
        uri,
        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
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
            if (selectedStatuses.isNotEmpty()) {
                IconButton(onClick = {
                    selectedStatuses.forEach { status ->
                    viewModel.saveStatus(status)
                }
                isSelectionMode = false
                selectedStatuses.clear()
            }) {
                Icon(Icons.Default.Download, contentDescription = "Download All")
            }
        }
    },
    colors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
    )
} else {
    TopAppBar(
    title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
            modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
            ) {
                Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
            text = "StatusSaver Pro",
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground
            )
        }
    },
    actions = {
        // Golden Coin Balance Chip
        Surface(
        onClick = { viewModel.showTopUpDialog = true },
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
        contentColor = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(100),
        modifier = Modifier
        .padding(end = 8.dp)
        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(100))
        ) {
            Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Balans",
                tint = Color(0xFFFFB300), // Beautiful Golden Color
                modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                text = "$coins Coin",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        
        Box {
            IconButton(
            onClick = { showLanguageMenu = true },
            modifier = Modifier.testTag("language_button")
            ) {
                Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = "Language / Dil",
                tint = MaterialTheme.colorScheme.onBackground
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
                text = { Text("🇺🇸 English") },
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
            }
        }
        IconButton(
        onClick = { viewModel.refreshStatuses() },
        modifier = Modifier.testTag("refresh_button")
        ) {
            Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = t(
            az = "Yenilə",
            en = "Refresh",
            ru = "Обновить",
            tr = "Yenile"
            ),
            tint = MaterialTheme.colorScheme.onBackground
            )
        }
    },
    scrollBehavior = scrollBehavior,
    colors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.background
    )
    )
}
},
modifier = modifier
.fillMaxSize()
.nestedScroll(scrollBehavior.nestedScrollConnection)
) { innerPadding ->
LazyVerticalGrid(
columns = GridCells.Fixed(2),
contentPadding = PaddingValues(bottom = 16.dp),
verticalArrangement = Arrangement.spacedBy(8.dp),
horizontalArrangement = Arrangement.spacedBy(8.dp),
modifier = Modifier
.fillMaxSize()
.padding(innerPadding)
.background(MaterialTheme.colorScheme.background)
) {

// Auto Save Control Card
item(span = { GridItemSpan(maxLineSpan) }) {
    Card(
    modifier = Modifier
    .fillMaxWidth()
    .padding(12.dp),
    shape = RoundedCornerShape(16.dp),
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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                    text = t(
                    az = "Avtomatik Yadda Saxla",
                    en = "Auto Save",
                    ru = "Автосохранение",
                    tr = "Otomatik Kaydet"
                    ),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                text = t(
                az = "Yeni statuslar görünən kimi dərhal qalereyanıza avtomatik köçürülsün.",
                en = "Automatically save new statuses to your gallery as soon as they appear.",
                ru = "Автоматически сохранять новые статусы в галерею, как только они появятся.",
                tr = "Yeni durumlar görünür görünmez galerinizde otomatik olarak kaydedilsin."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
            checked = autoSave,
            onCheckedChange = { viewModel.toggleAutoSave() },
            colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.primary,
            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            ),
            modifier = Modifier.testTag("auto_save_switch")
            )
        }
    }
}

// Folder Access / Demo Mode Banner
if (!uiState.hasFolderPermission && uiState.selectedFilter != 3) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Card(
        modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
        containerColor = if (uiState.isWhatsappDetected) 
        MaterialTheme.colorScheme.primaryContainer 
        else 
        MaterialTheme.colorScheme.secondaryContainer
        )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = if (uiState.isWhatsappDetected) 
                    MaterialTheme.colorScheme.primary 
                    else 
                    MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                    text = if (uiState.isWhatsappDetected) 
                    t(
                    az = "Real Statusları Aktivləşdirin",
                    en = "Enable Real Statuses",
                    ru = "Включить реальные статусы",
                    tr = "Gerçek Durumları Etkinleştir"
                    )
                    else 
                    t(
                    az = "Simulyasiya Rejimi Aktivdir",
                    en = "Simulation Mode Active",
                    ru = "Режим симуляции активен",
                    tr = "Simülasyon Modu Aktif"
                    ),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (uiState.isWhatsappDetected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                    MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                text = if (uiState.isWhatsappDetected) 
                t(
                az = "Real WhatsApp statuslarınızı oxumaq və yükləmək üçün tətbiqə icazə verməlisiniz. Aşağıdakı düyməyə klikləyin, açılan pəncərədə aşağıda yerləşən göy rəngli 'BU QOVLUQDAN İSTİFADƏ ET' (USE THIS FOLDER) düyməsini seçin və 'İcazə ver' (Allow) klikləyin.",
                en = "You need to grant permission to access your WhatsApp folder to load real statuses. Click the button below, select 'USE THIS FOLDER' and click 'Allow'.",
                ru = "Вам необходимо предоставить разрешение для доступа к папке WhatsApp, чтобы загружать реальные статусы. Нажмите кнопку ниже, выберите «ИСПЛЬЗОВАТЬ ЭТУ ПАПКУ» и нажмите «Разрешить».",
                tr = "Gerçek WhatsApp durumlarını yüklemek için klasör erişimine izin vermelisiniz. Aşağıdaki butona tıklayın, açılan pencerede 'BU KLASÖRÜ KULLAN' seçeneğini seçin ve 'İzin Ver'e tıklayın."
                )
                else 
                t(
                az = "Cihazda aktiv WhatsApp qovluğu tapılmadı. Tətbiqin şəkil/video redaktə və yadda saxlama funksiyalarını aşağıdakı test statusları üzərində sınaqdan keçirə bilərsiniz. Əgər WhatsApp quraşdırılıbsa, qovluğu əllə seçin.",
                en = "No active WhatsApp folder found on the device. You can test the app's image/video editing and saving features on the test statuses below. If WhatsApp is installed, choose the folder manually.",
                ru = "Активная папка WhatsApp на устройстве не найдена. Вы можете протестировать функции редактирования и сохранения фото/видео на тестовых статусах ниже. Если WhatsApp установлен, выберите папку вручную.",
                tr = "Cihazda etkin WhatsApp klasörü bulunamadı. Uygulamanın resim/video düzenleme ve kaydetme özelliklerini aşağıdaki test durumları üzerinde deneyebilirsiniz. Eğer WhatsApp yüklüyse, klasörü manuel seçin."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (uiState.isWhatsappDetected) 
                MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                onClick = {
                    try {
                        val initialUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fmedia")
                        folderPickerLauncher.launch(initialUri)
                    } catch (e: Exception) {
                        try {
                            folderPickerLauncher.launch(null)
                        } catch (e2: Exception) {
                            e2.printStackTrace()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.isWhatsappDetected) 
                MaterialTheme.colorScheme.primary 
                else 
                MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier.align(Alignment.End).testTag("select_folder_button")
                ) {
                    Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                    if (uiState.isWhatsappDetected) t(
                    az = "İcazə Ver və Aktivləşdir",
                    en = "Grant Permission & Enable",
                    ru = "Разрешить и включить",
                    tr = "İzin Ver ve Etkinleştir"
                    ) else t(
                    az = "Qovluğu Əllə Seç",
                    en = "Choose Folder Manually",
                    ru = "Выбрать папку вручную",
                    tr = "Klasörü Manuel Seç"
                    ), 
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Beautiful custom rounded pill-tab selector matching Vibrant Palette HTML
item(span = { GridItemSpan(maxLineSpan) }) {
    Row(
    modifier = Modifier
    .fillMaxWidth()
    .padding(horizontal = 12.dp, vertical = 8.dp)
    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(100))
    .padding(4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { idx, title ->
        val isSelected = uiState.selectedFilter == idx
        val tabBgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 250),
        label = "tabBg"
        )
        val tabContentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 250),
        label = "tabContent"
        )
        Box(
        modifier = Modifier
        .weight(1f)
        .clip(RoundedCornerShape(100))
        .background(tabBgColor)
        .clickable { viewModel.setFilter(idx) }
        .padding(vertical = 10.dp)
        .testTag("tab_$idx"),
        contentAlignment = Alignment.Center
        ) {
            Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                imageVector = when (idx) {
                    1 -> Icons.Default.Image
                    2 -> Icons.Default.Videocam
                    3 -> Icons.Default.Save
                    else -> Icons.Default.AutoAwesome
                },
                contentDescription = null,
                tint = tabContentColor,
                modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                text = title,
                color = tabContentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
}

// Status List Grid
if (uiState.statuses.isEmpty()) {
item(span = { GridItemSpan(maxLineSpan) }) {
    Box(
    modifier = Modifier
    .fillMaxWidth()
    .padding(vertical = 64.dp),
    contentAlignment = Alignment.Center
    ) {
        Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
        ) {
            Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
            text = t(
            az = "Hələ ki heç bir status yoxdur",
            en = "No statuses yet",
            ru = "Нет статусов",
            tr = "Henüz durum yok"
            ),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
            text = t(
            az = "WhatsApp statuslarına baxdıqdan sonra onlar bura avtomatik yüklənəcəkdir.",
            en = "WhatsApp statuses will appear here automatically after you view them.",
            ru = "Статусы WhatsApp появятся здесь автоматически после их просмотра.",
            tr = "WhatsApp durumlarını izledikten sonra burada otomatik olarak görünecektir."
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
            )
        }
    }
}
} else {
// Promo Banner for the Editor matching Vibrant Palette HTML
item(span = { GridItemSpan(maxLineSpan) }) {
    Card(
    modifier = Modifier
    .fillMaxWidth()
    .padding(horizontal = 12.dp, vertical = 6.dp),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
    ) {
        Row(
        modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
            modifier = Modifier
            .size(44.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
            ) {
                Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                text = t(
                az = "Status Redaktoru",
                en = "Status Editor",
                ru = "Редактор статусов",
                tr = "Durum Editörü"
                ),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
                )
                Text(
                text = t(
                az = "Filtr, Mətn və Kəsim alətləri",
                en = "Filter, Text & Crop tools",
                ru = "Инструменты фильтра, текста и обрезки",
                tr = "Filtre, Metin ve Kırpma araçları"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
                )
            }
            Button(
            onClick = {
                if (uiState.statuses.isNotEmpty()) {
                    onStatusClick(uiState.statuses.first())
                }
            },
            colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = RoundedCornerShape(100),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(t("BAŞLA", "START", "СТАРТ", "BAŞLA"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

if (activeStatuses.isNotEmpty()) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Row(
        modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
            modifier = Modifier
            .size(8.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
            text = if (uiState.selectedFilter == 3) 
            t("Saxlanılan Statuslar", "Saved Statuses", "Сохраненные статусы", "Kaydedilen Durumlar") 
            else 
            t("Aktiv Statuslar", "Active Statuses", "Активные статусы", "Aktif Durumlar"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
            text = t(
            az = "${activeStatuses.size} ədəd",
            en = "${activeStatuses.size} items",
            ru = "${activeStatuses.size} шт.",
            tr = "${activeStatuses.size} adet"
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
            )
        }
    }
    
    items(activeStatuses, key = { it.id }) { status ->
    val isSelected = selectedStatuses.contains(status)
    StatusGridItem(
    status = status,
    isSelected = isSelected,
    isSelectionMode = isSelectionMode,
    onClick = {
        if (isSelectionMode) {
            if (isSelected) selectedStatuses.remove(status)
            else selectedStatuses.add(status)
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
    onDeleteClick = { viewModel.deleteStatus(status) }
    )
}
}

if (expiredStatuses.isNotEmpty()) {
item(span = { GridItemSpan(maxLineSpan) }) {
    Row(
    modifier = Modifier
    .fillMaxWidth()
    .padding(horizontal = 12.dp, vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
        modifier = Modifier
        .size(8.dp)
        .background(MaterialTheme.colorScheme.error, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
        text = t("Vaxtı Bitmiş Statuslar", "Expired Statuses", "Истекшие статусы", "Süresi Dolan Durumlar"),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
        text = t(
        az = "${expiredStatuses.size} ədəd",
        en = "${expiredStatuses.size} items",
        ru = "${expiredStatuses.size} шт.",
        tr = "${expiredStatuses.size} adet"
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline
        )
    }
}

items(expiredStatuses, key = { it.id }) { status ->
val isSelected = selectedStatuses.contains(status)
StatusGridItem(
status = status,
isSelected = isSelected,
isSelectionMode = isSelectionMode,
onClick = {
    if (isSelectionMode) {
        if (isSelected) selectedStatuses.remove(status)
        else selectedStatuses.add(status)
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
onDeleteClick = { viewModel.deleteStatus(status) }
)
}
}
}
}
}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatusGridItem(
status: StatusItem,
onClick: () -> Unit,
onLongClick: () -> Unit = {},
onSaveClick: () -> Unit,
onDeleteClick: () -> Unit,
isSelected: Boolean = false,
isSelectionMode: Boolean = false,
modifier: Modifier = Modifier
) {
val context = LocalContext.current

var isVisible by remember { mutableStateOf(false) }
LaunchedEffect(Unit) {
isVisible = true
}

val scale by animateFloatAsState(
targetValue = if (isVisible) (if (isSelected) 0.9f else 1f) else 0.88f,
animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
label = "itemScale"
)

val alpha by animateFloatAsState(
targetValue = if (isVisible) 1f else 0f,
animationSpec = tween(durationMillis = 300),
label = "itemAlpha"
)

Card(
modifier = modifier
.fillMaxWidth()
.aspectRatio(0.75f) // Elegant 3:4 Aspect Ratio from Vibrant Palette HTML
.graphicsLayer {
scaleX = scale
scaleY = scale
this.alpha = alpha
}
.clip(RoundedCornerShape(24.dp))
.border(
width = if (isSelected) 3.dp else 1.dp,
color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
shape = RoundedCornerShape(24.dp)
)
.background(MaterialTheme.colorScheme.surfaceVariant)
.combinedClickable(
onClick = onClick,
onLongClick = onLongClick
)
.testTag("status_item_${status.id}"),
shape = RoundedCornerShape(24.dp),
colors = CardDefaults.cardColors(
containerColor = MaterialTheme.colorScheme.surfaceVariant
)
) {
Box(modifier = Modifier.fillMaxSize()) {

// Status Media Thumbnail
AsyncImage(
model = status.uri,
contentDescription = status.fileName,
contentScale = ContentScale.Crop,
modifier = Modifier.fillMaxSize()
)

if (isSelected) {
Box(
modifier = Modifier
.fillMaxSize()
.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
contentAlignment = Alignment.Center
) {
Icon(
imageVector = Icons.Default.CheckCircle,
contentDescription = "Selected",
tint = Color.White,
modifier = Modifier.size(48.dp)
)
}
}

// Dynamic high-contrast gradient overlay for readable text & controls
Box(
modifier = Modifier
.fillMaxSize()
.background(
Brush.verticalGradient(
0.4f to Color.Transparent,
1.0f to Color.Black.copy(alpha = 0.8f)
)
)
)

// Top action status indicators (Dynamic Badges & Quick Action Buttons)
Row(
modifier = Modifier
.fillMaxWidth()
.padding(12.dp),
horizontalArrangement = Arrangement.SpaceBetween,
verticalAlignment = Alignment.CenterVertically
) {
// Left-aligned: Saved Badge or Media Type Badge
if (status.isSaved) {
// Saved Badge from HTML (with check_circle and primary-like vibrant accent)
Box(
modifier = Modifier
.background(
color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
shape = RoundedCornerShape(100)
)
.padding(horizontal = 8.dp, vertical = 4.dp)
) {
Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
    imageVector = Icons.Default.Save,
    contentDescription = t("Yadda saxlanılıb", "Saved", "Сохранено", "Kaydedildi"),
    tint = Color.White,
    modifier = Modifier.size(10.dp)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
    text = t("YADDA SAXLANILIB", "SAVED", "СОХРАНЕНО", "KAYDEDİLDİ"),
    color = Color.White,
    fontSize = 9.sp,
    fontWeight = FontWeight.Bold
    )
}
}
} else {
// New/Unsaved Status type Badge
Box(
modifier = Modifier
.background(
color = Color.Black.copy(alpha = 0.6f),
shape = RoundedCornerShape(100)
)
.padding(horizontal = 8.dp, vertical = 4.dp)
) {
Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
    imageVector = if (status.isVideo) Icons.Default.PlayCircle else Icons.Default.Image,
    contentDescription = null,
    tint = Color.White,
    modifier = Modifier.size(10.dp)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
    text = if (status.isVideo) t("VİDEO", "VIDEO", "ВИДЕО", "VİDEO") else t("FOTO", "PHOTO", "ФОТО", "FOTO"),
    color = Color.White,
    fontSize = 9.sp,
    fontWeight = FontWeight.Bold
    )
}
}
}

// Right-aligned: Quick Actions (Save/Delete and Share) placed beautifully over the media in a unified capsule to avoid overlapping shadows
Row(
modifier = Modifier
.background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(100))
.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(100))
.padding(horizontal = 4.dp, vertical = 2.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(4.dp)
) {
if (!status.isSaved) {
IconButton(
onClick = onSaveClick,
modifier = Modifier
.size(24.dp)
.testTag("quick_save_${status.id}")
) {
    Icon(
    imageVector = Icons.Default.Download,
    contentDescription = t("Yadda saxla", "Save", "Сохранить", "Kaydet"),
    tint = Color.White,
    modifier = Modifier.size(13.dp)
    )
}
} else {
IconButton(
onClick = onDeleteClick,
modifier = Modifier
.size(24.dp)
.testTag("quick_delete_${status.id}")
) {
    Icon(
    imageVector = Icons.Default.Delete,
    contentDescription = t("Sil", "Delete", "Удалить", "Sil"),
    tint = Color.Red,
    modifier = Modifier.size(13.dp)
    )
}
}

// Separation line
Box(
modifier = Modifier
.width(1.dp)
.height(12.dp)
.background(Color.White.copy(alpha = 0.3f))
)

// Share action
IconButton(
onClick = {
val shareIntent = android.content.Intent().apply {
    action = android.content.Intent.ACTION_SEND
    putExtra(android.content.Intent.EXTRA_STREAM, status.uri)
    type = if (status.isVideo) "video/*" else "image/*"
    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
context.startActivity(android.content.Intent.createChooser(shareIntent, LanguageManager.translate("Statusu Paylaş", "Share Status", "Поделиться статусом", "Durumu Paylaş")))
},
modifier = Modifier
.size(24.dp)
.testTag("quick_share_${status.id}")
) {
Icon(
imageVector = Icons.Default.Share,
contentDescription = t("Paylaş", "Share", "Поделиться", "Paylaş"),
tint = Color.White,
modifier = Modifier.size(13.dp)
)
}
}
}

// Bottom metadata (File name and modified date)
Column(
modifier = Modifier
.align(Alignment.BottomStart)
.fillMaxWidth()
.padding(12.dp)
) {
Text(
text = status.fileName,
color = Color.White,
fontWeight = FontWeight.Bold,
fontSize = 12.sp,
maxLines = 1,
overflow = TextOverflow.Ellipsis
)

Spacer(modifier = Modifier.height(4.dp))

// Readable Date Label and Expiration badge
Row(
verticalAlignment = Alignment.CenterVertically
) {
val isExpired = System.currentTimeMillis() - status.dateModified > 24 * 60 * 60 * 1000L
if (isExpired) {
Box(
modifier = Modifier
.background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
.padding(horizontal = 4.dp, vertical = 2.dp)
) {
    Text(
    text = t("VAXTI BİTİB", "EXPIRED", "ИСТЕКЛО", "SÜRESİ DOLDU"),
    color = Color.White,
    fontSize = 8.sp,
    fontWeight = FontWeight.Bold
    )
}
Spacer(modifier = Modifier.width(6.dp))
}
Text(
text = android.text.format.DateUtils.getRelativeTimeSpanString(
status.dateModified,
System.currentTimeMillis(),
android.text.format.DateUtils.MINUTE_IN_MILLIS
).toString(),
color = Color.LightGray,
fontSize = 10.sp
)
}
}
}
}
}

@Composable
fun TopUpDialog(
viewModel: StatusViewModel,
onDismiss: () -> Unit
) {
val context = LocalContext.current
val coins by viewModel.coinsBalance.collectAsState()
val lastFreeDate by viewModel.lastFreeDownloadDate.collectAsState()
val isWatching = viewModel.isWatchingAd
val isPurchasing = viewModel.isPurchasing
val todayStr = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) }
val freeRemaining = lastFreeDate != todayStr

AlertDialog(
onDismissRequest = { if (!isWatching && !isPurchasing) onDismiss() },
title = {
Row(
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
Icon(
imageVector = Icons.Default.Star,
contentDescription = null,
tint = Color(0xFFFFB300),
modifier = Modifier.size(28.dp)
)
Text(
text = t(
az = "Balans və Limitlər",
en = "Balance & Limits",
ru = "Баланс и лимиты",
tr = "Bakiye ve Limitler"
),
fontWeight = FontWeight.Bold,
fontSize = 20.sp,
color = MaterialTheme.colorScheme.onSurface
)
}
},
text = {
Column(
modifier = Modifier.fillMaxWidth(),
verticalArrangement = Arrangement.spacedBy(16.dp)
) {
Text(
text = t(
az = "Status yükləmək üçün balansınızda kifayət qədər coin olmalıdır. Hər növbəti yükləmə 10 coindir.",
en = "To save a status, you must have enough coins in your balance. Each save costs 10 coins.",
ru = "Чтобы сохранить статус, на вашем балансе должно быть достаточно монет. Каждое сохранение стоит 10 монет.",
tr = "Durum kaydetmek için bakiyenizde yeterli coin olmalıdır. Her kaydetme 10 coindir."
),
style = MaterialTheme.typography.bodyMedium,
color = MaterialTheme.colorScheme.onSurfaceVariant
)

Card(
colors = CardDefaults.cardColors(
containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
),
shape = RoundedCornerShape(16.dp),
modifier = Modifier.fillMaxWidth()
) {
Column(
modifier = Modifier.padding(16.dp),
verticalArrangement = Arrangement.spacedBy(8.dp)
) {
Row(
modifier = Modifier.fillMaxWidth(),
horizontalArrangement = Arrangement.SpaceBetween,
verticalAlignment = Alignment.CenterVertically
) {
Text(
text = t(
az = "Cari Balansınız:",
en = "Your Current Balance:",
ru = "Ваш текущий баланс:",
tr = "Mevcut Bakiyeniz:"
),
fontWeight = FontWeight.Medium,
fontSize = 14.sp
)
Text("$coins Coin", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
}

Row(
modifier = Modifier.fillMaxWidth(),
horizontalArrangement = Arrangement.SpaceBetween,
verticalAlignment = Alignment.CenterVertically
) {
Text(
text = t(
az = "Gündəlik limit:",
en = "Daily limit:",
ru = "Дневной лимит:",
tr = "Günlük limit:"
),
fontWeight = FontWeight.Medium,
fontSize = 14.sp
)
Box(
modifier = Modifier
.background(
if (freeRemaining) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
RoundedCornerShape(100)
)
.padding(horizontal = 8.dp, vertical = 4.dp)
) {
    Text(
    text = if (freeRemaining) t(
    az = "1 Pulsuz Mövcuddur",
    en = "1 Free Available",
    ru = "Доступно 1 бесплатно",
    tr = "1 Ücretsiz Mevcut"
    ) else t(
    az = "İstifadə edilib",
    en = "Used",
    ru = "Использовано",
    tr = "Kullanıldı"
    ),
    color = if (freeRemaining) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    fontSize = 11.sp,
    fontWeight = FontWeight.Bold
    )
}
}
}
}

if (isWatching || isPurchasing) {
Box(
modifier = Modifier
.fillMaxWidth()
.padding(vertical = 16.dp),
contentAlignment = Alignment.Center
) {
Column(horizontalAlignment = Alignment.CenterHorizontally) {
CircularProgressIndicator(
color = MaterialTheme.colorScheme.primary,
modifier = Modifier.size(36.dp)
)
Spacer(modifier = Modifier.height(12.dp))
Text(
text = if (isWatching) t(
az = "Reklam yüklənir...",
en = "Ad is loading...",
ru = "Реклама загружается...",
tr = "Reklam yükleniyor..."
) else t(
az = "Ödəniş emal edilir...",
en = "Processing payment...",
ru = "Обработка платежа...",
tr = "Ödeme işleniyor..."
),
style = MaterialTheme.typography.bodySmall,
color = MaterialTheme.colorScheme.outline
)
}
}
} else {
Column(
modifier = Modifier.fillMaxWidth(),
verticalArrangement = Arrangement.spacedBy(10.dp)
) {
Text(
text = t(
az = "Coin Qazan / Satın Al",
en = "Earn Coins / Purchase",
ru = "Заработать / Купить",
tr = "Coin Kazan / Satın Al"
),
fontWeight = FontWeight.Bold,
fontSize = 12.sp,
color = MaterialTheme.colorScheme.outline,
modifier = Modifier.padding(bottom = 2.dp)
)

Card(
onClick = {
val activity = context as? Activity
if (activity != null) {
    viewModel.watchAdToEarnCoins(activity) {
        val pending = viewModel.pendingStatusToSave
        if (pending != null && (coins + 5) >= 10) {
            viewModel.saveStatus(pending)
            viewModel.pendingStatusToSave = null
            viewModel.showTopUpDialog = false
        }
    }
}
},
colors = CardDefaults.cardColors(
containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
),
shape = RoundedCornerShape(12.dp),
modifier = Modifier.fillMaxWidth()
) {
Row(
modifier = Modifier
.fillMaxWidth()
.padding(12.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(12.dp)
) {
    Box(
    modifier = Modifier
    .size(40.dp)
    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
    contentAlignment = Alignment.Center
    ) {
        Icon(
        imageVector = Icons.Default.PlayArrow,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(20.dp)
        )
    }
    Column(modifier = Modifier.weight(1f)) {
        Text(
        text = t(
        az = "Reklam İzlə",
        en = "Watch Ad",
        ru = "Смотреть рекламу",
        tr = "Reklam İzle"
        ),
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
        )
        Text(
        text = t(
        az = "Hər reklam izləməyə görə +5 Coin qazanın",
        en = "Earn +5 Coins for watching each ad",
        ru = "Получите +5 монет за каждый просмотр рекламы",
        tr = "Her reklam izleme için +5 Coin kazanın"
        ),
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Text("+5", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
}
}

Card(
onClick = {
viewModel.purchaseCoins {
    val pending = viewModel.pendingStatusToSave
    if (pending != null) {
        viewModel.saveStatus(pending)
        viewModel.pendingStatusToSave = null
        viewModel.showTopUpDialog = false
    }
}
},
colors = CardDefaults.cardColors(
containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
),
shape = RoundedCornerShape(12.dp),
modifier = Modifier.fillMaxWidth()
) {
Row(
modifier = Modifier
.fillMaxWidth()
.padding(12.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(12.dp)
) {
    Box(
    modifier = Modifier
    .size(40.dp)
    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
    contentAlignment = Alignment.Center
    ) {
        Icon(
        imageVector = Icons.Default.ShoppingCart,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp)
        )
    }
    Column(modifier = Modifier.weight(1f)) {
        Text(
        text = t(
        az = "50 Coin Satın Al",
        en = "Purchase 50 Coins",
        ru = "Купить 50 монет",
        tr = "50 Coin Satın Al"
        ),
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
        )
        Text(
        text = t(
        az = "Reklamsız və sürətli artırın — $0.99",
        en = "Ad-free and fast recharge — $0.99",
        ru = "Быстрое пополнение без рекламы — $0.99",
        tr = "Reklamsız ve hızlı yükleme — $0.99"
        ),
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Text("+50", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
}
}
}
}
}
},
confirmButton = {
if (!isWatching && !isPurchasing) {
Button(
onClick = onDismiss,
colors = ButtonDefaults.buttonColors(
containerColor = MaterialTheme.colorScheme.primary
)
) {
Text(
text = t(
az = "Bağla",
en = "Close",
ru = "Закрыть",
tr = "Kapat"
)
)
}
}
}
)
}

@Composable
fun UnityAdsSimulatedOverlay(
onAdCompleted: () -> Unit,
onAdDismissed: () -> Unit
) {
var timeLeft by remember { mutableStateOf(5) }
var canClose by remember { mutableStateOf(false) }

LaunchedEffect(Unit) {
while (timeLeft > 0) {
kotlinx.coroutines.delay(1000L)
timeLeft--
}
canClose = true
}

Dialog(
onDismissRequest = { if (canClose) onAdDismissed() },
properties = DialogProperties(
usePlatformDefaultWidth = false,
dismissOnBackPress = false,
dismissOnClickOutside = false
)
) {
Box(
modifier = Modifier
.fillMaxSize()
.background(Color(0xFF0F0F13)) // Rich dark ad canvas
) {
Column(
modifier = Modifier
.fillMaxSize()
.padding(24.dp),
horizontalAlignment = Alignment.CenterHorizontally,
verticalArrangement = Arrangement.SpaceBetween
) {
// Header
Row(
modifier = Modifier.fillMaxWidth(),
horizontalArrangement = Arrangement.SpaceBetween,
verticalAlignment = Alignment.CenterVertically
) {
Row(
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
Box(
modifier = Modifier
.size(32.dp)
.background(Color(0xFF2E2E38), RoundedCornerShape(8.dp)),
contentAlignment = Alignment.Center
) {
    Icon(
    imageVector = Icons.Default.AutoAwesome,
    contentDescription = null,
    tint = Color(0xFFFFB300),
    modifier = Modifier.size(18.dp)
    )
}
Column {
    Text(
    "Unity Ads",
    color = Color.White,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp
    )
    Text(
    t(
    az = "Yalnız balans artırmaq üçün",
    en = "For topping up balance only",
    ru = "Только для пополнения баланса",
    tr = "Sadece bakiye yüklemek için"
    ),
    color = Color.Gray,
    fontSize = 10.sp
    )
}
}

// Countdown
Box(
modifier = Modifier
.size(36.dp)
.background(Color.White.copy(alpha = 0.12f), CircleShape),
contentAlignment = Alignment.Center
) {
if (timeLeft > 0) {
    Text(
    text = "$timeLeft",
    color = Color.White,
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp
    )
} else {
    IconButton(
    onClick = onAdCompleted,
    modifier = Modifier.size(24.dp)
    ) {
        Icon(
        imageVector = Icons.Default.Close,
        contentDescription = t("Bağla", "Close", "Закрыть", "Kapat"),
        tint = Color.White
        )
    }
}
}
}

// Central high-fidelity visual
Card(
modifier = Modifier
.fillMaxWidth()
.weight(1f)
.padding(vertical = 32.dp),
shape = RoundedCornerShape(24.dp),
colors = CardDefaults.cardColors(
containerColor = Color(0xFF1E1E24)
),
border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
) {
Column(
modifier = Modifier
.fillMaxSize()
.padding(24.dp),
horizontalAlignment = Alignment.CenterHorizontally,
verticalArrangement = Arrangement.Center
) {
Icon(
imageVector = Icons.Default.Star,
contentDescription = null,
tint = Color(0xFFFFB300),
modifier = Modifier.size(72.dp)
)
Spacer(modifier = Modifier.height(24.dp))
Text(
text = t(
az = "StatusSaver Pro Sponsoru",
en = "StatusSaver Pro Sponsor",
ru = "Спонсор StatusSaver Pro",
tr = "StatusSaver Pro Sponsoru"
),
color = Color.White,
fontWeight = FontWeight.Bold,
fontSize = 20.sp,
textAlign = TextAlign.Center
)
Spacer(modifier = Modifier.height(12.dp))
Text(
text = t(
az = "Hər reklam izləməyə görə dərhal balansınıza 5 Coin hədiyyə ediləcəkdir!",
en = "5 Coins will be immediately credited to your balance for each ad view!",
ru = "5 монет будут мгновенно зачислены на ваш баланс за каждый просмотр рекламы!",
tr = "Her reklam izleme için anında bakiyenize 5 Coin hediye edilecektir!"
),
color = Color.LightGray,
fontSize = 13.sp,
textAlign = TextAlign.Center,
lineHeight = 18.sp
)
Spacer(modifier = Modifier.height(24.dp))

LinearProgressIndicator(
progress = { (5 - timeLeft) / 5f },
modifier = Modifier
.fillMaxWidth()
.height(6.dp)
.clip(RoundedCornerShape(100)),
color = Color(0xFFFFB300),
trackColor = Color.White.copy(alpha = 0.1f)
)
}
}

// CTA
Column(
modifier = Modifier.fillMaxWidth(),
horizontalAlignment = Alignment.CenterHorizontally,
verticalArrangement = Arrangement.spacedBy(12.dp)
) {
if (timeLeft > 0) {
Text(
text = t(
az = "Coin qazanmaq üçün reklamı sonadək izləyin...",
en = "Watch the ad to the end to earn coins...",
ru = "Смотрите рекламу до конца, чтобы заработать монеты...",
tr = "Coin kazanmak için reklamı sonuna kadar izleyin..."
),
color = Color.Gray,
fontSize = 11.sp,
textAlign = TextAlign.Center
)
} else {
Button(
onClick = onAdCompleted,
colors = ButtonDefaults.buttonColors(
containerColor = Color(0xFFFFB300),
contentColor = Color.Black
),
shape = RoundedCornerShape(100),
modifier = Modifier
.fillMaxWidth()
.height(50.dp)
) {
    Text(
    text = t(
    az = "REKLAMI BAĞLA VƏ +5 COIN QAZAN",
    en = "CLOSE AD AND EARN +5 COINS",
    ru = "ЗАКРЫТЬ РЕКЛАМУ И ПОЛУЧИТЬ +5 МОНЕТ",
    tr = "REKLAMI KAPAT VE +5 COIN KAZAN"
    ),
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp
    )
}
}
}
}
}
}
}
}
