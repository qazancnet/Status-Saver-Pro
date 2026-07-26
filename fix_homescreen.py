import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    lines = f.readlines()

# Find the empty state block
start_idx = -1
for i, line in enumerate(lines):
    if "Hələ ki heç bir status yoxdur" in line:
        start_idx = i
        break

# Go up to the start of that item block
while start_idx > 0 and "item(span = { GridItemSpan(maxLineSpan) }) {" not in lines[start_idx]:
    start_idx -= 1

# Keep the file up to this point
new_lines = lines[:start_idx]

# Now append the rest of the file correctly
rest = """                item(span = { GridItemSpan(maxLineSpan) }) {
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
                                    az = "WhatsApp (və digər) statuslarına baxdıqdan sonra onlar bura avtomatik yüklənəcəkdir.",
                                    en = "WhatsApp (and other variants) statuses will appear here automatically after you view them.",
                                    ru = "Статусы WhatsApp (и других версий) появятся здесь автоматически после их просмотра.",
                                    tr = "WhatsApp (ve diğer) durumlarını izledikten sonra burada otomatik olarak görünecektir."
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                            if (uiState.hasFolderPermission) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = {
                                    try {
                                        val initialUri = android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fmedia")
                                        folderPickerLauncher.launch(initialUri)
                                    } catch (e: Exception) {
                                        try { folderPickerLauncher.launch(null) } catch (e2: Exception) {}
                                    }
                                }) {
                                    Text(t("Qovluğu dəyiş (Səhv seçmisinizsə)", "Change Folder", "Изменить папку", "Klasörü Değiştir"))
                                }
                            }
                        }
                    }
                }
            } else {
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
                        showSaveIndicator = autoSave
                    )
                }
                
                if (expiredStatuses.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = t("Köhnə Statuslar (24 saatdan çox)", "Old Statuses (>24h)", "Старые статусы (>24ч)", "Eski Durumlar (>24s)"),
                            style = MaterialTheme.typography.titleMedium,
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
                            showSaveIndicator = false
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StatusGridItem(
    status: com.example.model.StatusItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    showSaveIndicator: Boolean = false,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            coil.compose.AsyncImage(
                model = status.uri,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isSelected) {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(48.dp))
                }
            } else if (status.isVideo) {
                Icon(
                    Icons.Default.Videocam, 
                    contentDescription = null, 
                    tint = Color.White, 
                    modifier = Modifier.align(Alignment.Center).size(48.dp)
                )
            }
        }
    }
}

@Composable
fun TopUpDialog(viewModel: com.example.viewmodel.StatusViewModel, onDismiss: () -> Unit) {}

@Composable
fun UnityAdsSimulatedOverlay(onAdCompleted: () -> Unit, onAdDismissed: () -> Unit) {}
"""

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.writelines(new_lines)
    f.write(rest)
