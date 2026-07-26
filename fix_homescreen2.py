import os

content = """package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.StatusItem
import com.example.viewmodel.StatusViewModel
import com.example.util.t
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
    
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedStatuses = remember { mutableStateListOf<StatusItem>() }
    
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
                    title = { Text("${selectedStatuses.size} Seçildi", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedStatuses.clear()
                        }) { Icon(Icons.Default.Close, contentDescription = "Cancel") }
                    },
                    actions = {
                        IconButton(onClick = {
                            selectedStatuses.forEach { viewModel.saveStatus(it) }
                            isSelectionMode = false
                            selectedStatuses.clear()
                        }) { Icon(Icons.Default.Save, contentDescription = "Save All") }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("StatusSaver Pro", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { viewModel.refreshStatuses() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = onFileManagerClick) {
                            Icon(Icons.Default.Folder, contentDescription = "Files")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedFilter,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                val tabs = listOf("Hamısı", "Şəkillər", "Videolar", "Saxlanılanlar")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedFilter == index,
                        onClick = { viewModel.setFilter(index) },
                        text = { Text(title) }
                    )
                }
            }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Avtomatik Yadda Saxla", modifier = Modifier.weight(1f))
                            Switch(checked = autoSave, onCheckedChange = { viewModel.toggleAutoSave() })
                        }
                    }
                }
                
                if (activeStatuses.isEmpty() && expiredStatuses.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Text("Hələ ki heç bir status yoxdur", style = MaterialTheme.typography.titleMedium)
                            if (uiState.hasFolderPermission) {
                                Button(onClick = {
                                    try { folderPickerLauncher.launch(Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fmedia")) } catch (e: Exception) { folderPickerLauncher.launch(null) }
                                }) { Text("Qovluğu dəyiş") }
                            }
                        }
                    }
                } else {
                    items(activeStatuses, key = { it.id }) { status ->
                        StatusGridItem(
                            status = status,
                            isSelected = selectedStatuses.contains(status),
                            onClick = {
                                if (isSelectionMode) {
                                    if (selectedStatuses.contains(status)) selectedStatuses.remove(status) else selectedStatuses.add(status)
                                    if (selectedStatuses.isEmpty()) isSelectionMode = false
                                } else { onStatusClick(status) }
                            },
                            onLongClick = { if (!isSelectionMode) { isSelectionMode = true; selectedStatuses.add(status) } },
                            onSaveClick = { viewModel.saveStatus(status) },
                            showSaveIndicator = autoSave
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
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSaveClick: () -> Unit,
    showSaveIndicator: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.aspectRatio(0.75f).combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .border(if (isSelected) 3.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(model = status.uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (isSelected) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.4f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(48.dp))
                }
            } else if (status.isVideo) {
                Icon(Icons.Default.PlayCircleOutline, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(48.dp))
            }
        }
    }
}

@Composable
fun TopUpDialog(viewModel: StatusViewModel, onDismiss: () -> Unit) {}

@Composable
fun UnityAdsSimulatedOverlay(onAdCompleted: () -> Unit, onAdDismissed: () -> Unit) {}
"""
with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(content)

