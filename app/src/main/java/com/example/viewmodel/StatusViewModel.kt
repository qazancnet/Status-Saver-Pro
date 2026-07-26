package com.example.viewmodel

import android.app.Application
import android.app.Activity
import com.unity3d.ads.UnityAds
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAdsShowOptions
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SavedStatusEntity
import com.example.data.StatusRepository
import com.example.model.StatusItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StatusRepository(application)
    private val database = AppDatabase.getDatabase(application)
    private val savedDao = database.savedStatusDao()
    private val sharedPrefs = application.getSharedPreferences("StatusSaverPrefs", android.content.Context.MODE_PRIVATE)

    // Folder access state (SAF Tree Uri)
    private val _safFolderUri = MutableStateFlow<Uri?>(null)
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    val safFolderUri = _safFolderUri.asStateFlow()

    // Auto-Save configuration toggle
    private val _autoSaveEnabled = MutableStateFlow(false)
    val autoSaveEnabled = _autoSaveEnabled.asStateFlow()

    // Active status filter: 0 = Hamısı (All), 1 = Şəkillər (Images), 2 = Videolar (Videos), 3 = Saxlanılanlar (Saved)
    private val _selectedFilter = MutableStateFlow(0)
    val selectedFilter = _selectedFilter.asStateFlow()

    // Raw scanned statuses
    private val _rawStatuses = MutableStateFlow<List<StatusItem>>(emptyList())

    // Deleted/hidden status IDs list (to support instant & persistent local deletions of any status, including demo ones)
    private val _deletedStatusIds = MutableStateFlow<Set<String>>(emptySet())

    // Coins/Balance and daily limits
    private val _coinsBalance = MutableStateFlow(20) // default 20 coins
    val coinsBalance = _coinsBalance.asStateFlow()

    private val _lastFreeDownloadDate = MutableStateFlow("")
    val lastFreeDownloadDate = _lastFreeDownloadDate.asStateFlow()

    // Active auto-saves cache to prevent simultaneous parallel duplicate writes
    private val activeAutoSaves = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    // UI Dialog State Management
    var showTopUpDialog by mutableStateOf(false)
    var pendingStatusToSave by mutableStateOf<StatusItem?>(null)
    var isWatchingAd by mutableStateOf(false)
    var isPurchasing by mutableStateOf(false)

    var showCoinAnimation by mutableStateOf(false)
    var coinAnimationAmount by mutableStateOf(0)

    fun triggerCoinAnimation(amount: Int) {
        viewModelScope.launch(Dispatchers.Main) {
            coinAnimationAmount = amount
            showCoinAnimation = true
        }
    }

    // Filtered raw statuses that are not deleted/hidden
    private val _nonDeletedRawStatuses = combine(_rawStatuses, _deletedStatusIds) { raw, deletedIds ->
        raw.filter { it.id !in deletedIds }
    }

    // Room's saved statuses database flow
    val dbSavedStatuses: StateFlow<List<SavedStatusEntity>> = savedDao.getAllSaved()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined filtered statuses that combines scanner list and room's saved database list
    val uiState: StateFlow<StatusState> = combine(
        _nonDeletedRawStatuses,
        dbSavedStatuses,
        _selectedFilter,
        _autoSaveEnabled,
        _safFolderUri
    ) { activeRaw, saved, filterIndex, autoSave, safUri ->
        
        // Map raw items and check if they are already saved in DB
        val mappedRaw = activeRaw.map { item ->
            val isSavedInDb = saved.any { it.id == item.id || it.fileName == item.fileName }
            item.copy(isSaved = isSavedInDb)
        }

        // Automatic saving trigger (with strict thread-safe duplicate checks)
        if (autoSave && mappedRaw.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                mappedRaw.forEach { item ->
                    val isAlreadySaved = saved.any { it.id == item.id || it.fileName == item.fileName || it.originalPath == item.path }
                    if (!isAlreadySaved && !activeAutoSaves.contains(item.id)) {
                        activeAutoSaves.add(item.id)
                        attemptAutoSaveStatus(item, saved)
                    }
                }
            }
        }

        // Apply filters
        val deletedIds = _deletedStatusIds.value
        val filteredList = when (filterIndex) {
            1 -> mappedRaw.filter { !it.isVideo } // Images only
            2 -> mappedRaw.filter { it.isVideo }  // Videos only
            3 -> {
                // Map DB Saved entities to StatusItems for visual display (and filter out if hidden/deleted)
                saved.filter { it.id !in deletedIds }.map { entity ->
                    StatusItem(
                        id = entity.id,
                        uri = Uri.parse(entity.uriString),
                        fileName = entity.fileName,
                        isVideo = entity.isVideo,
                        size = entity.size,
                        dateModified = entity.dateSaved,
                        isSaved = true,
                        path = entity.editedPath ?: entity.originalPath
                    )
                }
            }
            else -> mappedRaw // All
        }

        StatusState(
            statuses = filteredList,
            allStatuses = mappedRaw,
            selectedFilter = filterIndex,
            isAutoSaveActive = autoSave,
            isWhatsappDetected = repository.hasWhatsappInstalled(),
            hasFolderPermission = safUri != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatusState())

    private var refreshJob: kotlinx.coroutines.Job? = null

    init {
        val savedUriStr = sharedPrefs.getString("saf_folder_uri", null)
        if (savedUriStr != null) {
            try {
                _safFolderUri.value = Uri.parse(savedUriStr)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _autoSaveEnabled.value = sharedPrefs.getBoolean("auto_save_enabled", false)
        
        // Load persistent deleted status IDs (using extremely robust comma-separated string to avoid Android SharedPreferences StringSet bugs)
        val deletedStr = sharedPrefs.getString("deleted_status_ids_str", "") ?: ""
        val deletedSet = if (deletedStr.isNotEmpty()) deletedStr.split(",").toSet() else emptySet()
        _deletedStatusIds.value = deletedSet

        // Load balance and limits
        _coinsBalance.value = sharedPrefs.getInt("coins_balance", 20)
        _lastFreeDownloadDate.value = sharedPrefs.getString("last_free_download_date", "") ?: ""

        loadStatuses()
    }

    // Manually refresh/scan WhatsApp statuses
    fun refreshStatuses() {
        loadStatuses()
    }

    // Load/Scan WhatsApp directories
    fun loadStatuses() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.scanStatuses(_safFolderUri.value).collect { list ->
                _rawStatuses.value = list
                _isLoading.value = false
            }
        }
    }

    // Set custom selected WhatsApp SAF Folder
    fun setSafFolderUri(uri: Uri) {
        _safFolderUri.value = uri
        sharedPrefs.edit().putString("saf_folder_uri", uri.toString()).apply()
        loadStatuses()
    }

    // Set filter category
    fun setFilter(index: Int) {
        _selectedFilter.value = index
    }

    // Toggle Auto-Save status
    fun toggleAutoSave() {
        _autoSaveEnabled.value = !_autoSaveEnabled.value
        sharedPrefs.edit().putBoolean("auto_save_enabled", _autoSaveEnabled.value).apply()
        val stateText = if (_autoSaveEnabled.value) "Aktivləşdirildi" else "Deaktivləşdirildi"
        Toast.makeText(getApplication(), "Avtomatik yadda saxlama $stateText", Toast.LENGTH_SHORT).show()
    }

    // Manual Save to Gallery with Coins / Limit Check
    fun saveStatus(status: StatusItem) {
        viewModelScope.launch {
            if (status.isSaved) {
                // Already saved, just download/refresh safely without cost
                saveStatusToGallery(status, showToast = true)
                return@launch
            }

            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val hasFreeDownload = _lastFreeDownloadDate.value != todayStr

            if (hasFreeDownload) {
                val success = saveStatusToGallery(status, showToast = false)
                if (success) {
                    _lastFreeDownloadDate.value = todayStr
                    sharedPrefs.edit().putString("last_free_download_date", todayStr).apply()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Bugünkü pulsuz yükləmə hüququnuzdan istifadə etdiniz!", Toast.LENGTH_LONG).show()
                    }
                }
            } else if (_coinsBalance.value >= 10) {
                val success = saveStatusToGallery(status, showToast = false)
                if (success) {
                    _coinsBalance.value -= 10
                    sharedPrefs.edit().putInt("coins_balance", _coinsBalance.value).apply()
                    triggerCoinAnimation(-10)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Status yükləndi! Balansınızdan 10 coin çıxıldı.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                // Insufficient coins! Show dialog to top up balance
                pendingStatusToSave = status
                showTopUpDialog = true
            }
        }
    }

    // Auto-Save helper under same daily free limits or coin cost
    private suspend fun attemptAutoSaveStatus(item: StatusItem, savedList: List<SavedStatusEntity>): Boolean {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val hasFreeDownload = _lastFreeDownloadDate.value != todayStr

        if (hasFreeDownload) {
            val success = saveStatusToGallery(item, showToast = false)
            if (success) {
                _lastFreeDownloadDate.value = todayStr
                sharedPrefs.edit().putString("last_free_download_date", todayStr).apply()
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Avtomatik yadda saxlanıldı (Gündəlik pulsuz)", Toast.LENGTH_SHORT).show()
                }
                return true
            }
        } else if (_coinsBalance.value >= 10) {
            val success = saveStatusToGallery(item, showToast = false)
            if (success) {
                _coinsBalance.value -= 10
                sharedPrefs.edit().putInt("coins_balance", _coinsBalance.value).apply()
                triggerCoinAnimation(-10)
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Avtomatik yadda saxlanıldı (10 Coin çıxıldı)", Toast.LENGTH_SHORT).show()
                }
                return true
            }
        }
        return false
    }

    private var isUnityAdsInitialized = false
    val unityGameId = "5595316" // Unity Ads Game ID
    val interstitialPlacementId = "Interstitial_Android"
    val rewardedPlacementId = "Rewarded_Android"
    val bannerPlacementId = "Banner_Android"

    fun initUnityAds(activity: Activity) {
        if (isUnityAdsInitialized) return
        try {
            UnityAds.initialize(activity.applicationContext, unityGameId, false, object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    isUnityAdsInitialized = true
                    android.util.Log.d("UnityAdsStatus", "Unity Ads Initialized Successfully with Game ID: $unityGameId")
                }

                override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                    android.util.Log.e("UnityAdsStatus", "Unity Ads Initialization Failed: $message")
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Show Interstitial Ad (Unity Ads with fallback)
    fun showInterstitialAd(activity: Activity, onComplete: () -> Unit) {
        initUnityAds(activity)
        try {
            UnityAds.load(interstitialPlacementId, object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String?) {
                    UnityAds.show(activity, interstitialPlacementId, UnityAdsShowOptions(), object : IUnityAdsShowListener {
                        override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                            android.util.Log.e("UnityAdsStatus", "Interstitial Show Failed: $message")
                            onComplete()
                        }

                        override fun onUnityAdsShowStart(placementId: String?) {
                            android.util.Log.d("UnityAdsStatus", "Interstitial Started")
                        }

                        override fun onUnityAdsShowClick(placementId: String?) {
                            android.util.Log.d("UnityAdsStatus", "Interstitial Clicked")
                        }

                        override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                            onComplete()
                        }
                    })
                }

                override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                    android.util.Log.e("UnityAdsStatus", "Interstitial Failed to Load: $message")
                    onComplete()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete()
        }
    }

    // Watch a rewarded ad to earn 5 coins (Unity Ads with Simulated fallback)
    fun watchAdToEarnCoins(activity: Activity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isWatchingAd = true
            initUnityAds(activity)

            try {
                UnityAds.load(rewardedPlacementId, object : IUnityAdsLoadListener {
                    override fun onUnityAdsAdLoaded(placementId: String?) {
                        if (!isWatchingAd) return
                        UnityAds.show(activity, rewardedPlacementId, UnityAdsShowOptions(), object : IUnityAdsShowListener {
                            override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                                android.util.Log.e("UnityAdsStatus", "Unity Ads Show Failed: $message")
                            }

                            override fun onUnityAdsShowStart(placementId: String?) {
                                android.util.Log.d("UnityAdsStatus", "Unity Ads Show Started")
                            }

                            override fun onUnityAdsShowClick(placementId: String?) {
                                android.util.Log.d("UnityAdsStatus", "Unity Ads Clicked")
                            }

                            override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                                viewModelScope.launch(Dispatchers.Main) {
                                    if (isWatchingAd) {
                                        isWatchingAd = false
                                        _coinsBalance.value += 5
                                        sharedPrefs.edit().putInt("coins_balance", _coinsBalance.value).apply()
                                        triggerCoinAnimation(5)
                                        Toast.makeText(getApplication(), "Təbriklər! Unity Ads (Game ID: 5595316) izlədiyiniz üçün +5 Coin qazandınız!", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                    }
                                }
                            }
                        })
                    }

                    override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                        android.util.Log.e("UnityAdsStatus", "Unity Ads Failed to Load: $message")
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Complete simulated ad view from Compose overlay
    fun completeSimulatedAd(onSuccess: () -> Unit) {
        if (isWatchingAd) {
            isWatchingAd = false
            _coinsBalance.value += 5
            sharedPrefs.edit().putInt("coins_balance", _coinsBalance.value).apply()
            triggerCoinAnimation(5)
            Toast.makeText(getApplication(), "Təbriklər! Reklam izlədiyiniz üçün +5 Coin qazandınız!", Toast.LENGTH_SHORT).show()
            onSuccess()
        }
    }

    // Purchase coins with money (simulated)
    fun purchaseCoins(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isPurchasing = true
            kotlinx.coroutines.delay(1500L) // Realistic payment getaway simulation
            isPurchasing = false
            _coinsBalance.value += 50
            sharedPrefs.edit().putInt("coins_balance", _coinsBalance.value).apply()
            triggerCoinAnimation(50)
            Toast.makeText(getApplication(), "Ödəniş uğurlu oldu! Balansınıza +50 Coin əlavə edildi.", Toast.LENGTH_SHORT).show()
            onSuccess()
        }
    }

    // Internal helper to perform save task in background
    private suspend fun saveStatusToGallery(status: StatusItem, showToast: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            val savedUri = repository.saveToGallery(status)
            if (savedUri != null) {
                // Register in Room Database
                val entity = SavedStatusEntity(
                    id = status.id,
                    uriString = savedUri.toString(),
                    fileName = status.fileName,
                    isVideo = status.isVideo,
                    size = status.size,
                    dateSaved = System.currentTimeMillis(),
                    originalPath = status.path
                )
                savedDao.insertSaved(entity)

                withContext(Dispatchers.Main) {
                    if (showToast) {
                        Toast.makeText(getApplication(), "Qalereyaya uğurla saxlanıldı!", Toast.LENGTH_SHORT).show()
                    }
                    loadStatuses()
                }
                true
            } else {
                withContext(Dispatchers.Main) {
                    if (showToast) {
                        Toast.makeText(getApplication(), "Yadda saxlanma zamanı xəta baş verdi", Toast.LENGTH_SHORT).show()
                    }
                }
                false
            }
        }
    }

    // Delete a status (remove from database and local UI)
    fun deleteStatus(status: StatusItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // If it is in saved database, delete it
                savedDao.deleteSavedById(status.id)
                
                // Delete physical file (handles both file paths and SAF/MediaStore content:// Uris)
                if (status.id.startsWith("content://")) {
                    try {
                        val uri = Uri.parse(status.id)
                        val deletedBySaf = try {
                            android.provider.DocumentsContract.deleteDocument(getApplication<android.app.Application>().contentResolver, uri)
                        } catch (e: Exception) {
                            false
                        }
                        if (!deletedBySaf) {
                            getApplication<android.app.Application>().contentResolver.delete(uri, null, null)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else if (status.path.isNotEmpty()) {
                    try {
                        val file = java.io.File(status.path)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Add to local deleted status IDs list persistently (using comma-separated string for 100% reliability)
                val currentDeleted = _deletedStatusIds.value.toMutableSet()
                currentDeleted.add(status.id)
                _deletedStatusIds.value = currentDeleted
                sharedPrefs.edit().putString("deleted_status_ids_str", currentDeleted.joinToString(",")).apply()

                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Uğurla silindi!", Toast.LENGTH_SHORT).show()
                    loadStatuses()
                }
            }
        }
    }

    // Helper method to spend coins. Returns true if successful, false if insufficient.
    fun spendCoins(amount: Int): Boolean {
        if (_coinsBalance.value >= amount) {
            _coinsBalance.value -= amount
            sharedPrefs.edit().putInt("coins_balance", _coinsBalance.value).apply()
            triggerCoinAnimation(-amount)
            return true
        } else {
            showTopUpDialog = true
            return false
        }
    }

    // Save custom image edits to Gallery and Room DB (costs 5 coins)
    fun saveImageEdits(status: StatusItem, bitmap: Bitmap, onSaved: () -> Unit) {
        if (_coinsBalance.value < 5) {
            viewModelScope.launch {
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Şəkili redaktə edib yadda saxlamaq üçün 5 Coin lazımdır! Zəhmət olmasa balansı artırın.", Toast.LENGTH_LONG).show()
                    showTopUpDialog = true
                }
            }
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val savedUri = repository.saveToGallery(status, editedBitmap = bitmap, isEdited = true)
                if (savedUri != null) {
                    val entity = SavedStatusEntity(
                        id = "edited_img_" + System.currentTimeMillis() + "_" + status.id,
                        uriString = savedUri.toString(),
                        fileName = "Redaktə olunmuş - " + status.fileName,
                        isVideo = false,
                        size = bitmap.byteCount.toLong(),
                        dateSaved = System.currentTimeMillis(),
                        originalPath = status.path,
                        editedPath = savedUri.toString(),
                        hasEdits = true
                    )
                    savedDao.insertSaved(entity)

                    // Deduct 5 coins
                    _coinsBalance.value -= 5
                    sharedPrefs.edit().putInt("coins_balance", _coinsBalance.value).apply()
                    triggerCoinAnimation(-5)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Redaktə edilmiş şəkil ayrı nüsxə olaraq qalereyaya yazıldı! Balansınızdan 5 Coin çıxıldı.", Toast.LENGTH_LONG).show()
                        loadStatuses()
                        onSaved()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Redaktəni yadda saxlayarkən xəta baş verdi", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Save custom video edits (Trim/Mute/Speed simulation)
    fun saveVideoEdits(
        status: StatusItem, 
        startTimeSec: Float, 
        endTimeSec: Float, 
        playbackSpeed: Float, 
        isMuted: Boolean
    ) {
        viewModelScope.launch {
            // Simulate video compression and processing (with standard progress delay)
            withContext(Dispatchers.IO) {
                val savedUri = repository.saveToGallery(status, isEdited = true)
                if (savedUri != null) {
                    val entity = SavedStatusEntity(
                        id = "edited_vid_" + System.currentTimeMillis() + "_" + status.id,
                        uriString = savedUri.toString(),
                        fileName = "Redaktə olunmuş - " + status.fileName,
                        isVideo = true,
                        size = (status.size * ((endTimeSec - startTimeSec) / (status.durationMs / 1000f).coerceAtLeast(1f))).toLong(),
                        dateSaved = System.currentTimeMillis(),
                        originalPath = status.path,
                        editedPath = savedUri.toString(),
                        hasEdits = true
                    )
                    savedDao.insertSaved(entity)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Videorolik redaktə edildi və ayrı nüsxə olaraq qalereyaya yazıldı!", Toast.LENGTH_LONG).show()
                        loadStatuses()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Videonun yadda saxlanması uğursuz oldu", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

// UI state declaration
data class StatusState(
    val statuses: List<StatusItem> = emptyList(),
    val allStatuses: List<StatusItem> = emptyList(),
    val selectedFilter: Int = 0,
    val isAutoSaveActive: Boolean = false,
    val isWhatsappDetected: Boolean = false,
    val hasFolderPermission: Boolean = false,
    val isLoading: Boolean = false
)
