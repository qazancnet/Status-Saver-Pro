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
