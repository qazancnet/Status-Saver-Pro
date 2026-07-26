package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ColorLens as FilterIcon
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.collectAsState
import com.example.util.LanguageManager
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import com.example.viewmodel.StatusViewModel
import coil.compose.AsyncImage
import com.example.model.StatusItem
import com.example.util.t
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.roundToInt

// Data classes for photo editing layers
data class DoodlePath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    // Store original points to render on the final high-res Bitmap
    val points: List<Offset> = emptyList()
)

data class TextOverlay(
    val id: Long = System.currentTimeMillis() + (0..1000).random(),
    var text: String,
    var color: Color,
    var sizeSp: Float = 28f,
    var offset: Offset = Offset(200f, 400f),
    var fontFamily: String = "Default",
    var isItalic: Boolean = false,
    var scale: Float = 1f,
    var rotation: Float = 0f
)

data class PhotoFilter(
    val name: String,
    val colorMatrix: ComposeColorMatrix?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    status: StatusItem,
    viewModel: StatusViewModel,
    onBack: () -> Unit,
    onSaveEdits: (StatusItem, Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Loading active bitmap on background thread
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingBitmap by remember { mutableStateOf(true) }

    LaunchedEffect(status.id) {
        withContext(Dispatchers.IO) {
            try {
                val input = context.contentResolver.openInputStream(status.uri)
                if (input != null) {
                    val decoded = BitmapFactory.decodeStream(input)
                    sourceBitmap = decoded
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // local placeholder block
                sourceBitmap = Bitmap.createBitmap(800, 1200, Bitmap.Config.ARGB_8888)
            } finally {
                isLoadingBitmap = false
            }
        }
    }

    // 2. Editor Transformation States
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var activeFilterIndex by remember { mutableStateOf(0) }
    
    var containerWidth by remember { mutableFloatStateOf(1080f) }
    var containerHeight by remember { mutableFloatStateOf(1600f) }
    
    // Premium Effects lists & session unlock state
    val premiumFilters = remember { setOf("Neon", "Retro", "Canlı", "Parlaq") }
    val premiumEmojis = remember { setOf("👑", "🚀", "💀", "🤡", "💯") }
    val unlockedEffects = remember { mutableStateListOf<String>() }
    
    var pendingPremiumEffect by remember { mutableStateOf<String?>(null) }
    var premiumConfirmType by remember { mutableStateOf<String?>(null) } // "filter" or "sticker"
    
    // Premium Color Adjustments
    var brightnessAdjustment by remember { mutableFloatStateOf(0f) }
    var contrastAdjustment by remember { mutableFloatStateOf(1f) }
    var saturationAdjustment by remember { mutableFloatStateOf(1f) }
    
    // Brush drawing curves
    val doodlePaths = remember { mutableStateListOf<DoodlePath>() }
    var currentDrawColor by remember { mutableStateOf(Color.Red) }
    var currentStrokeWidth by remember { mutableFloatStateOf(10f) }
    val currentPathPoints = remember { mutableStateListOf<Offset>() }
    val currentPath = remember { mutableStateOf<Path?>(null) }
    var drawTrigger by remember { mutableStateOf(0) }

    // Text Overlay values
    val textOverlays = remember { mutableStateListOf<TextOverlay>() }
    var editingTextOverlay by remember { mutableStateOf<TextOverlay?>(null) }
    var currentInputText by remember { mutableStateOf("") }
    var currentFontFamily by remember { mutableStateOf("Default") }
    var currentIsItalic by remember { mutableStateOf(false) }

    // Brush vs Filter vs Text tool selectors
    var activeTool by remember { mutableStateOf("none") } // "brush", "filter", "text", "none"

    val currentLangState by LanguageManager.currentLanguage.collectAsState()
    // Set of filters
    val filterList = remember(currentLangState) {
        listOf(
            PhotoFilter(LanguageManager.translate("Orijinal", "Original", "Оригинал", "Orijinal"), null),
            PhotoFilter(LanguageManager.translate("B&W", "B&W", "Ч/Б", "S&B"), ComposeColorMatrix(floatArrayOf(
                0.33f, 0.59f, 0.11f, 0f, 0f,
                0.33f, 0.59f, 0.11f, 0f, 0f,
                0.33f, 0.59f, 0.11f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))),
            PhotoFilter(LanguageManager.translate("Sepiya", "Sepia", "Сепия", "Sepya"), ComposeColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))),
            PhotoFilter(LanguageManager.translate("Neon", "Neon", "Неон", "Neon"), ComposeColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))),
            PhotoFilter(LanguageManager.translate("Retro", "Retro", "Ретро", "Retro"), ComposeColorMatrix(floatArrayOf(
                1f, 0.2f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 0.8f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))),
            PhotoFilter(LanguageManager.translate("Soyuq", "Cool", "Холодный", "Soğuk"), ComposeColorMatrix(floatArrayOf(
                0.8f, 0f, 0f, 0f, 0f,
                0f, 0.9f, 0f, 0f, 0f,
                0f, 0f, 1.2f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))),
            PhotoFilter(LanguageManager.translate("İsti", "Warm", "Теплый", "Sıcak"), ComposeColorMatrix(floatArrayOf(
                1.2f, 0f, 0f, 0f, 10f,
                0f, 1.0f, 0f, 0f, 5f,
                0f, 0f, 0.8f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))),
            PhotoFilter(LanguageManager.translate("Köhnə", "Old", "Старый", "Eski"), ComposeColorMatrix(floatArrayOf(
                0.9f, 0.1f, 0.1f, 0f, 15f,
                0.1f, 0.8f, 0.1f, 0f, 15f,
                0.1f, 0.1f, 0.7f, 0f, 15f,
                0f, 0f, 0f, 1f, 0f
            ))),
            PhotoFilter(LanguageManager.translate("Mavi", "Blue", "Синий", "Mavi"), ComposeColorMatrix(floatArrayOf(
                0.6f, 0f, 0f, 0f, 0f,
                0f, 0.7f, 0f, 0f, 0f,
                0f, 0f, 1.4f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            ))),
            PhotoFilter(LanguageManager.translate("Canlı", "Vivid", "Яркий", "Canlı"), ComposeColorMatrix(floatArrayOf(
                1.3f, -0.15f, -0.15f, 0f, 0f,
                -0.15f, 1.3f, -0.15f, 0f, 0f,
                -0.15f, -0.15f, 1.3f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))),
            PhotoFilter(LanguageManager.translate("Parlaq", "Bright", "Светлый", "Parlak"), ComposeColorMatrix(floatArrayOf(
                1.15f, 0f, 0f, 0f, 20f,
                0f, 1.15f, 0f, 0f, 20f,
                0f, 0f, 1.15f, 0f, 20f,
                0f, 0f, 0f, 1f, 0f
            ))),
            PhotoFilter(LanguageManager.translate("Yaşıl", "Green", "Зеленый", "Yeşil"), ComposeColorMatrix(floatArrayOf(
                0.8f, 0f, 0f, 0f, 0f,
                0f, 1.25f, 0f, 0f, 10f,
                0f, 0f, 0.8f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )))
        )
    }

    // Colors available for Draw and Text
    val editorColors = listOf(
        Color.Red, Color.Green, Color(0xFF25D366), Color.Blue, 
        Color.Yellow, Color.Cyan, Color.Magenta, Color.White, Color.Black
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(t("Şəkli Redaktə Et", "Edit Image", "Редактировать фото", "Resmi Düzenle"), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        
                        // Coin Balance Chip
                        val coins by viewModel.coinsBalance.collectAsState()
                        Card(
                            onClick = { viewModel.showTopUpDialog = true },
                            shape = RoundedCornerShape(100),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2E2E38)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f)),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Coin",
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "$coins Coin",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
                    }
                },
                actions = {
                    // Save modified image trigger (costs 5 coins)
                    IconButton(
                        onClick = {
                            if (sourceBitmap == null) return@IconButton
                            isLoadingBitmap = true
                            scope.launch {
                                val finalBitmap = withContext(Dispatchers.Default) {
                                    renderEditedBitmap(
                                        source = sourceBitmap!!,
                                        rotation = rotationDegrees,
                                        filterName = filterList[activeFilterIndex].name,
                                        brightness = brightnessAdjustment,
                                        contrast = contrastAdjustment,
                                        saturation = saturationAdjustment,
                                        doodles = doodlePaths,
                                        texts = textOverlays,
                                        viewportWidth = containerWidth,
                                        viewportHeight = containerHeight
                                    )
                                }
                                onSaveEdits(status, finalBitmap)
                                isLoadingBitmap = false
                            }
                        },
                        modifier = Modifier.testTag("editor_save_trigger")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = t("Yadda Saxla", "Save", "Сохранить", "Kaydet"),
                            tint = Color(0xFFFFB300)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E24)
                )
            )
        }
    ) { innerPadding ->
        if (isLoadingBitmap) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(t("Şəkil yüklənir və emal olunur...", "Loading and processing image...", "Загрузка и обработка фото...", "Resim yükleniyor ve işleniyor..."), fontWeight = FontWeight.Bold)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFF1E1E1E)),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                
                // MAIN CANVAS PANEL (Displays image, drawings, text overlays)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0F0F13)), // Dark cinema background
                    contentAlignment = Alignment.Center
                ) {
                    val bitmapWidth = sourceBitmap?.width?.toFloat() ?: 1f
                    val bitmapHeight = sourceBitmap?.height?.toFloat() ?: 1f
                    
                    val isSwapped = (rotationDegrees / 90f).roundToInt() % 2 != 0
                    val currentAspectRatio = if (isSwapped) {
                        bitmapHeight / bitmapWidth
                    } else {
                        bitmapWidth / bitmapHeight
                    }

                    Box(
                        modifier = Modifier
                            .aspectRatio(currentAspectRatio)
                            .fillMaxSize()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .onSizeChanged { size ->
                                containerWidth = size.width.toFloat()
                                containerHeight = size.height.toFloat()
                            }
                            .pointerInput(activeTool) {
                                if (activeTool != "brush") return@pointerInput
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val newPath = Path().apply { moveTo(offset.x, offset.y) }
                                        currentPath.value = newPath
                                        currentPathPoints.clear()
                                        currentPathPoints.add(offset)
                                        drawTrigger++
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val currentOffset = change.position
                                        currentPathPoints.add(currentOffset)
                                        currentPath.value?.lineTo(currentOffset.x, currentOffset.y)
                                        drawTrigger++
                                    },
                                    onDragEnd = {
                                        val completedPath = currentPath.value
                                        if (completedPath != null) {
                                            doodlePaths.add(
                                                DoodlePath(
                                                    path = completedPath,
                                                    color = currentDrawColor,
                                                    strokeWidth = currentStrokeWidth,
                                                    points = currentPathPoints.toList()
                                                )
                                            )
                                        }
                                        currentPath.value = null
                                        currentPathPoints.clear()
                                        drawTrigger++
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    // Tap outside text overlay can close submenus
                                }
                            }
                    ) {
                        
                        // Base Image with Active Rotation and Color Filter
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            sourceBitmap?.let { bmp ->
                                val filterMatrix = getAdjustedMatrix(
                                    filterName = filterList[activeFilterIndex].name,
                                    brightness = brightnessAdjustment,
                                    contrast = contrastAdjustment,
                                    saturation = saturationAdjustment
                                )
                                ComposeCanvas(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    rotate(rotationDegrees) {
                                        val dstWidth = if (isSwapped) size.height.roundToInt() else size.width.roundToInt()
                                        val dstHeight = if (isSwapped) size.width.roundToInt() else size.height.roundToInt()
                                        val offsetX = (size.width - dstWidth) / 2f
                                        val offsetY = (size.height - dstHeight) / 2f
                                        drawImage(
                                            image = bmp.asImageBitmap(),
                                            dstOffset = androidx.compose.ui.unit.IntOffset(offsetX.roundToInt(), offsetY.roundToInt()),
                                            dstSize = androidx.compose.ui.unit.IntSize(dstWidth, dstHeight),
                                            colorFilter = ColorFilter.colorMatrix(filterMatrix)
                                        )
                                    }
                                }
                            }
                        }

                    // DRAWING (DOODLE) LAYER
                    ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                        val _trigger = drawTrigger
                        // Historical paths
                        doodlePaths.forEach { doodle ->
                            drawPath(
                                path = doodle.path,
                                color = doodle.color,
                                style = Stroke(width = doodle.strokeWidth)
                            )
                        }
                        // Active drawing path
                        currentPath.value?.let { activePath ->
                            drawPath(
                                path = activePath,
                                color = currentDrawColor,
                                style = Stroke(width = currentStrokeWidth)
                            )
                        }
                    }

                    // DRAGGABLE TEXT OVERLAYS LAYER
                    textOverlays.forEach { textOverlay ->
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(textOverlay.offset.x.roundToInt(), textOverlay.offset.y.roundToInt()) }
                                .graphicsLayer {
                                    scaleX = textOverlay.scale
                                    scaleY = textOverlay.scale
                                    rotationZ = textOverlay.rotation
                                }
                                .pointerInput(textOverlay.id) {
                                    detectTransformGestures { _, pan, zoom, rotation ->
                                        val idx = textOverlays.indexOfFirst { it.id == textOverlay.id }
                                        if (idx >= 0) {
                                            val current = textOverlays[idx]
                                            val newOffset = Offset(
                                                x = current.offset.x + pan.x,
                                                y = current.offset.y + pan.y
                                            )
                                            val newScale = (current.scale * zoom).coerceIn(0.3f, 5.0f)
                                            val newRotation = (current.rotation + rotation) % 360f
                                            textOverlays[idx] = current.copy(
                                                offset = newOffset,
                                                scale = newScale,
                                                rotation = newRotation
                                            )
                                        }
                                    }
                                }
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .clickable {
                                    editingTextOverlay = textOverlay
                                    currentInputText = textOverlay.text
                                    currentFontFamily = textOverlay.fontFamily
                                    currentIsItalic = textOverlay.isItalic
                                    currentDrawColor = textOverlay.color
                                    activeTool = "text"
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = textOverlay.text,
                                    color = textOverlay.color,
                                    fontSize = textOverlay.sizeSp.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = when (textOverlay.fontFamily) {
                                        "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                                        "SansSerif" -> androidx.compose.ui.text.font.FontFamily.SansSerif
                                        "Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
                                        else -> androidx.compose.ui.text.font.FontFamily.Default
                                    },
                                    fontStyle = if (textOverlay.isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { textOverlays.remove(textOverlay) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Sil",
                                        tint = Color.Red,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                    }
                }

                // SUB-TOOL OPTIONS INTERFACE (Brush settings, filter lists, etc.)
                AnimatedVisibility(visible = activeTool != "none") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (activeTool) {
                                    "brush" -> t("Fırça Alətləri", "Brush Tools", "Инструменты кисти", "Fırça Araçları")
                                    "filter" -> t("Rəng Filtrləri", "Color Filters", "Цветовые фильтры", "Renk Filtreleri")
                                    "text" -> t("Mətn Alətləri", "Text Tools", "Текстовые инструменты", "Metin Araçları")
                                    "adjust" -> t("Düzəliş Alətləri", "Adjustment Tools", "Инструменты настройки", "Ayarlama Araçları")
                                    "sticker" -> t("Stiker & Emoji", "Sticker & Emoji", "Стикеры и Эмодзи", "Etiket & Emoji")
                                    else -> ""
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { activeTool = "none" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = t("Bağla", "Close", "Закрыть", "Kapat"))
                            }
                        }

                        // Tool Contents
                        when (activeTool) {
                            "brush" -> {
                                // Color selection
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    items(editorColors) { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    width = if (currentDrawColor == color) 3.dp else 1.dp,
                                                    color = if (currentDrawColor == color) MaterialTheme.colorScheme.primary else Color.Gray,
                                                    shape = CircleShape
                                                )
                                                .clickable { currentDrawColor = color }
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                // Brush Size Selector
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(t("Fırça ölçüsü:", "Brush size:", "Размер кисти:", "Fırça boyutu:"), fontSize = 12.sp, modifier = Modifier.width(80.dp))
                                    Slider(
                                        value = currentStrokeWidth,
                                        onValueChange = { currentStrokeWidth = it },
                                        valueRange = 2f..40f,
                                        modifier = Modifier.weight(1f).testTag("brush_size_slider")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { doodlePaths.clear() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text(t("Təmizlə", "Clear", "Очистить", "Temizle"), fontSize = 11.sp)
                                    }
                                }
                            }
                            
                            "filter" -> {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                ) {
                                    items(filterList.size) { idx ->
                                        val filter = filterList[idx]
                                        val isPremium = premiumFilters.contains(filter.name)
                                        val isUnlocked = unlockedEffects.contains(filter.name)
                                        
                                        Box(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, end = 2.dp)) {
                                            Card(
                                                modifier = Modifier
                                                    .width(85.dp)
                                                    .clickable {
                                                        if (isPremium && !isUnlocked) {
                                                            pendingPremiumEffect = filter.name
                                                            premiumConfirmType = "filter"
                                                        } else {
                                                            activeFilterIndex = idx
                                                        }
                                                    }
                                                    .border(
                                                        width = if (activeFilterIndex == idx) 2.dp else 1.dp,
                                                        color = if (activeFilterIndex == idx) Color(0xFFFFB300) else Color.Gray.copy(alpha = 0.3f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (activeFilterIndex == idx) Color(0xFF2E2E38) else Color(0xFF1E1E24)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ColorLens,
                                                        contentDescription = null,
                                                        tint = if (activeFilterIndex == idx) Color(0xFFFFB300) else Color.White.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = filter.name,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (activeFilterIndex == idx) Color.White else Color.LightGray
                                                    )
                                                }
                                            }
                                            
                                            if (isPremium) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .offset(x = 4.dp, y = (-4).dp)
                                                        .background(
                                                            if (isUnlocked) Color(0xFF25D366) else Color(0xFFFFB300),
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Star,
                                                            contentDescription = null,
                                                            tint = Color.Black,
                                                            modifier = Modifier.size(8.dp)
                                                        )
                                                        Text(
                                                            text = if (isUnlocked) t("Açıq", "Free", "Откр.", "Açık") else t("5C", "5C", "5М", "5C"),
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color.Black
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            "text" -> {
                                val fontFamilies = listOf(
                                    "Default" to t("Standart", "Standard", "Стандарт", "Standart"),
                                    "Serif" to t("Zərif", "Elegant", "Элегантный", "Şık"),
                                    "SansSerif" to t("Sadə", "Simple", "Простой", "Sade"),
                                    "Monospace" to t("Teleqraf", "Telegraph", "Телеграф", "Telgraf")
                                )
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = currentInputText,
                                        onValueChange = { currentInputText = it },
                                        placeholder = { Text(t("Mətni daxil edin...", "Enter text...", "Введите текст...", "Metin girin...")) },
                                        modifier = Modifier.fillMaxWidth().testTag("text_input_field"),
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                if (currentInputText.isNotEmpty()) {
                                                    if (editingTextOverlay != null) {
                                                        val idx = textOverlays.indexOfFirst { it.id == editingTextOverlay!!.id }
                                                        if (idx >= 0) {
                                                            textOverlays[idx] = textOverlays[idx].copy(
                                                                text = currentInputText,
                                                                color = currentDrawColor,
                                                                fontFamily = currentFontFamily,
                                                                isItalic = currentIsItalic
                                                            )
                                                        }
                                                    } else {
                                                        textOverlays.add(
                                                            TextOverlay(
                                                                text = currentInputText,
                                                                color = currentDrawColor,
                                                                offset = Offset(300f, 500f),
                                                                fontFamily = currentFontFamily,
                                                                isItalic = currentIsItalic
                                                            )
                                                        )
                                                    }
                                                    currentInputText = ""
                                                    editingTextOverlay = null
                                                }
                                            }) {
                                                Icon(imageVector = Icons.Default.Check, contentDescription = t("Əlavə et", "Add", "Добавить", "Ekle"))
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Font Family Selection and Italic button row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(t("Şrift və Maililik:", "Font & Italic:", "Шрифт и Наклон:", "Yazı Tipi & İtalik:"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                        
                                        // Italic style toggle
                                        IconButton(
                                            onClick = { currentIsItalic = !currentIsItalic },
                                            modifier = Modifier
                                                .background(
                                                    if (currentIsItalic) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (currentIsItalic) MaterialTheme.colorScheme.primary else Color.Gray,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.TextFields,
                                                contentDescription = "Maili Yaz",
                                                tint = if (currentIsItalic) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(18.dp).graphicsLayer(rotationZ = -15f)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    // List of font styles as selectable cards
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        items(fontFamilies) { (familyKey, familyLabel) ->
                                            val isSelected = currentFontFamily == familyKey
                                            Card(
                                                modifier = Modifier
                                                    .clickable { currentFontFamily = familyKey }
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                                )
                                            ) {
                                                Text(
                                                    text = familyLabel,
                                                    fontFamily = when (familyKey) {
                                                        "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                                                        "SansSerif" -> androidx.compose.ui.text.font.FontFamily.SansSerif
                                                        "Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
                                                        else -> androidx.compose.ui.text.font.FontFamily.Default
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    fontStyle = if (currentIsItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    Text(t("Rəng:", "Color:", "Цвет:", "Renk:"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Text Color picker
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(editorColors) { color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .border(
                                                        width = if (currentDrawColor == color) 2.dp else 1.dp,
                                                        color = Color.White,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { currentDrawColor = color }
                                            )
                                        }
                                    }
                                }
                            }

                            "adjust" -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Brightness Slider
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(t("Parlaqlıq:", "Brightness:", "Яркость:", "Parlaklık:"), fontSize = 11.sp, modifier = Modifier.width(75.dp), color = MaterialTheme.colorScheme.onSurface)
                                        Slider(
                                            value = brightnessAdjustment,
                                            onValueChange = { brightnessAdjustment = it },
                                            valueRange = -0.5f..0.5f,
                                            modifier = Modifier.weight(1f).testTag("brightness_slider")
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${(brightnessAdjustment * 100).toInt()}%", fontSize = 10.sp, modifier = Modifier.width(30.dp), color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Contrast Slider
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(t("Kontrast:", "Contrast:", "Контраст:", "Kontrast:"), fontSize = 11.sp, modifier = Modifier.width(75.dp), color = MaterialTheme.colorScheme.onSurface)
                                        Slider(
                                            value = contrastAdjustment,
                                            onValueChange = { contrastAdjustment = it },
                                            valueRange = 0.5f..1.5f,
                                            modifier = Modifier.weight(1f).testTag("contrast_slider")
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${(contrastAdjustment * 100).toInt()}%", fontSize = 10.sp, modifier = Modifier.width(30.dp), color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Saturation Slider
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(t("Doyğunluq:", "Saturation:", "Насыщенность:", "Doygunluk:"), fontSize = 11.sp, modifier = Modifier.width(75.dp), color = MaterialTheme.colorScheme.onSurface)
                                        Slider(
                                            value = saturationAdjustment,
                                            onValueChange = { saturationAdjustment = it },
                                            valueRange = 0.0f..2.0f,
                                            modifier = Modifier.weight(1f).testTag("saturation_slider")
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${(saturationAdjustment * 100).toInt()}%", fontSize = 10.sp, modifier = Modifier.width(30.dp), color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Reset Button
                                    Button(
                                        onClick = {
                                            brightnessAdjustment = 0f
                                            contrastAdjustment = 1f
                                            saturationAdjustment = 1f
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text(t("Sıfırla", "Reset", "Сбросить", "Sıfırla"), fontSize = 11.sp)
                                    }
                                }
                            }

                            "sticker" -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(t("Şəklə əlavə etmək üçün toxunun (★ Premium - 5 Coin):", "Tap to add to image (★ Premium - 5 Coins):", "Нажмите для добавления на фото (★ Премиум - 5 монет):", "Resme eklemek için dokunun (★ Premium - 5 Coin):"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val emojis = listOf("😂", "❤️", "🔥", "👍", "😍", "🎉", "🚀", "👏", "🌟", "💀", "🤡", "💩", "💯", "🎂", "👑", "🍕", "🎈")
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        items(emojis) { emoji ->
                                            val isPremium = premiumEmojis.contains(emoji)
                                            val isUnlocked = unlockedEffects.contains(emoji)
                                            Box(
                                                modifier = Modifier.padding(end = 6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(46.dp)
                                                        .background(Color(0xFF2E2E38), CircleShape)
                                                        .border(
                                                            1.dp,
                                                            if (isPremium) Color(0xFFFFB300).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                                                            CircleShape
                                                        )
                                                        .clickable {
                                                            if (isPremium && !isUnlocked) {
                                                                pendingPremiumEffect = emoji
                                                                premiumConfirmType = "sticker"
                                                            } else {
                                                                textOverlays.add(
                                                                    TextOverlay(
                                                                        text = emoji,
                                                                        color = Color.White,
                                                                        sizeSp = 48f,
                                                                        offset = Offset(300f, 500f)
                                                                    )
                                                                )
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(emoji, fontSize = 24.sp)
                                                }
                                                
                                                if (isPremium) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .background(
                                                                if (isUnlocked) Color(0xFF25D366) else Color(0xFFFFB300),
                                                                CircleShape
                                                            )
                                                            .size(16.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = if (isUnlocked) "✓" else "★",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.Black
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // BOTTOM MAIN TOOLBAR PANEL (Selecting core tools: brush, rotate, filter, text)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rotate
                    ToolbarIconButton(
                        icon = Icons.Default.RotateRight,
                        label = t("Fırlat", "Rotate", "Поворот", "Döndür"),
                        isSelected = false,
                        onClick = {
                            rotationDegrees = (rotationDegrees + 90f) % 360f
                        },
                        modifier = Modifier.testTag("tool_rotate")
                    )

                    // Draw Brush
                    ToolbarIconButton(
                        icon = Icons.Default.Brush,
                        label = t("Fırça", "Brush", "Кисть", "Fırça"),
                        isSelected = activeTool == "brush",
                        onClick = { activeTool = if (activeTool == "brush") "none" else "brush" },
                        modifier = Modifier.testTag("tool_brush")
                    )

                    // Filter
                    ToolbarIconButton(
                        icon = Icons.Default.ColorLens,
                        label = t("Filtrlər", "Filters", "Фильтры", "Filtreler"),
                        isSelected = activeTool == "filter",
                        onClick = { activeTool = if (activeTool == "filter") "none" else "filter" },
                        modifier = Modifier.testTag("tool_filters")
                    )

                    // Add Text
                    ToolbarIconButton(
                        icon = Icons.Default.TextFields,
                        label = t("Yazı", "Text", "Текст", "Yazı"),
                        isSelected = activeTool == "text",
                        onClick = { activeTool = if (activeTool == "text") "none" else "text" },
                        modifier = Modifier.testTag("tool_text")
                    )

                    // Adjust Color Matrix
                    ToolbarIconButton(
                        icon = Icons.Default.Tune,
                        label = t("Düzəliş", "Adjust", "Настройка", "Ayarla"),
                        isSelected = activeTool == "adjust",
                        onClick = { activeTool = if (activeTool == "adjust") "none" else "adjust" },
                        modifier = Modifier.testTag("tool_adjust")
                    )

                    // Add Stickers / Emojis
                    ToolbarIconButton(
                        icon = Icons.Default.Mood,
                        label = t("Stiker", "Sticker", "Стикер", "Etiket"),
                        isSelected = activeTool == "sticker",
                        onClick = { activeTool = if (activeTool == "sticker") "none" else "sticker" },
                        modifier = Modifier.testTag("tool_sticker")
                    )
                }
            }
        }
        
        // Premium Effect Purchase/Unlock Dialog
        if (pendingPremiumEffect != null) {
            val currentCoins by viewModel.coinsBalance.collectAsState()
            AlertDialog(
                onDismissRequest = {
                    pendingPremiumEffect = null
                    premiumConfirmType = null
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(t("Premium Effekt", "Premium Effect", "Премиум Эффект", "Premium Efekt"), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                text = {
                    Column {
                        Text(
                            text = t(
                                az = "Bu premium effekti ('$pendingPremiumEffect') aktivləşdirmək balansınızdan 5 Coin çıxacaq. Davam edilsin?",
                                en = "Activating this premium effect ('$pendingPremiumEffect') will cost 5 Coins from your balance. Continue?",
                                ru = "Активация этого премиум-эффекта ('$pendingPremiumEffect') спишет 5 монет с вашего баланса. Продолжить?",
                                tr = "Bu premium efekti ('$pendingPremiumEffect') etkinleştirmek bakiyenizden 5 Coin düşecektir. Devam edilsin mi?"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = t(
                                az = "Cari balansınız: $currentCoins Coin",
                                en = "Your current balance: $currentCoins Coins",
                                ru = "Ваш текущий баланс: $currentCoins монет",
                                tr = "Mevcut bakiyeniz: $currentCoins Coin"
                            ),
                            fontWeight = FontWeight.Bold,
                            color = if (currentCoins >= 5) Color(0xFFFFB300) else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val effectName = pendingPremiumEffect!!
                            if (viewModel.spendCoins(5)) {
                                unlockedEffects.add(effectName)
                                // Apply effect
                                if (premiumConfirmType == "filter") {
                                    val filterIdx = filterList.indexOfFirst { it.name == effectName }
                                    if (filterIdx >= 0) {
                                        activeFilterIndex = filterIdx
                                    }
                                } else if (premiumConfirmType == "sticker") {
                                    textOverlays.add(
                                        TextOverlay(
                                            text = effectName,
                                            color = Color.White,
                                            sizeSp = 48f,
                                            offset = Offset(300f, 500f)
                                        )
                                    )
                                }
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    LanguageManager.translate(
                                        az = "Kifayət qədər coin yoxdur! Zəhmət olmasa balansı artırın.",
                                        en = "Not enough coins! Please top up your balance.",
                                        ru = "Недостаточно монет! Пожалуйста, пополните баланс.",
                                        tr = "Yetersiz coin! Lütfen bakiyenizi artırın."
                                    ),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                            pendingPremiumEffect = null
                            premiumConfirmType = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB300),
                            contentColor = Color.Black
                        )
                    ) {
                        Text(t("Kilidi Aç (5 Coin)", "Unlock (5 Coins)", "Разблокировать (5 монет)", "Kilidi Aç (5 Coin)"), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            pendingPremiumEffect = null
                            premiumConfirmType = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            contentColor = Color.White
                        )
                    ) {
                        Text(t("Ləğv Et", "Cancel", "Отмена", "İptal"))
                    }
                },
                containerColor = Color(0xFF1E1E24)
            )
        }
    }
}

@Composable
fun ToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 28.dp)
                .clip(RoundedCornerShape(100))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// THE BACKGROUND HARD-CORE PHOTO RENDER ENGINE
fun renderEditedBitmap(
    source: Bitmap,
    rotation: Float,
    filterName: String,
    brightness: Float,
    contrast: Float,
    saturation: Float,
    doodles: List<DoodlePath>,
    texts: List<TextOverlay>,
    viewportWidth: Float = 1080f,
    viewportHeight: Float = 1600f
): Bitmap {
    // 1. Calculate final dimensions and rotation of bitmap copy
    val matrix = android.graphics.Matrix().apply {
        postRotate(rotation)
    }
    var workingBitmap = Bitmap.createBitmap(
        source, 0, 0, source.width, source.height, matrix, true
    ).copy(Bitmap.Config.ARGB_8888, true)

    val canvas = Canvas(workingBitmap)

    // 2. Render Color Filter & Adjustments onto Bitmap pixels
    val adjustedMatrix = getAdjustedMatrix(filterName, brightness, contrast, saturation)
    val paint = Paint()
    val floatVals = adjustedMatrix.values
    val filter = ColorMatrixColorFilter(ColorMatrix(floatVals))
    paint.colorFilter = filter
    
    // Redraw bitmap with combined adjustments applied
    val tempBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true)
    canvas.drawBitmap(tempBitmap, 0f, 0f, paint)

    // Since our user interaction layout size (e.g. 1080x1920 viewport) differs from
    // the source image density (e.g. 4000x3000), we calculate high-fidelity scale ratios
    // to map drawings and text coordinate spaces onto source bitmap pixels perfectly!
    val scaleX = workingBitmap.width / viewportWidth
    val scaleY = workingBitmap.height / viewportHeight

    // 3. Draw finger drawings
    val drawPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    doodles.forEach { doodle ->
        drawPaint.color = doodle.color.value.toInt()
        drawPaint.strokeWidth = doodle.strokeWidth * scaleX
        
        if (doodle.points.isNotEmpty()) {
            val path = android.graphics.Path()
            doodle.points.forEachIndexed { i, pt ->
                val realX = pt.x * scaleX
                val realY = pt.y * scaleY
                if (i == 0) {
                    path.moveTo(realX, realY)
                } else {
                    path.lineTo(realX, realY)
                }
            }
            canvas.drawPath(path, drawPaint)
        }
    }

    // 4. Draw texts onto bitmap
    val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }

    texts.forEach { overlay ->
        val tf = when (overlay.fontFamily) {
            "Serif" -> android.graphics.Typeface.SERIF
            "SansSerif" -> android.graphics.Typeface.SANS_SERIF
            "Monospace" -> android.graphics.Typeface.MONOSPACE
            else -> android.graphics.Typeface.DEFAULT
        }
        val style = if (overlay.isItalic) android.graphics.Typeface.BOLD_ITALIC else android.graphics.Typeface.BOLD
        textPaint.typeface = android.graphics.Typeface.create(tf, style)
        
        textPaint.color = overlay.color.value.toInt()
        textPaint.textSize = overlay.sizeSp * scaleX * 1.5f * overlay.scale
        
        val realX = overlay.offset.x * scaleX
        val realY = (overlay.offset.y + overlay.sizeSp) * scaleY // offset correction

        val bounds = android.graphics.Rect()
        textPaint.getTextBounds(overlay.text, 0, overlay.text.length, bounds)
        val textWidth = bounds.width().toFloat()
        val textHeight = bounds.height().toFloat()

        canvas.save()
        val centerX = realX + textWidth / 2f
        val centerY = realY - textHeight / 2f
        canvas.rotate(overlay.rotation, centerX, centerY)
        canvas.drawText(overlay.text, realX, realY, textPaint)
        canvas.restore()
    }

    return workingBitmap
}

fun getAdjustedMatrix(
    filterName: String,
    brightness: Float,
    contrast: Float,
    saturation: Float
): ComposeColorMatrix {
    val base = when (filterName) {
        "B&W" -> floatArrayOf(
            0.33f, 0.59f, 0.11f, 0f, 0f,
            0.33f, 0.59f, 0.11f, 0f, 0f,
            0.33f, 0.59f, 0.11f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        "Sepiya" -> floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        "Neon" -> floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
        "Retro" -> floatArrayOf(
            1f, 0.2f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 0.8f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        "Soyuq" -> floatArrayOf(
            0.8f, 0f, 0f, 0f, 0f,
            0f, 0.9f, 0f, 0f, 0f,
            0f, 0f, 1.2f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        "İsti" -> floatArrayOf(
            1.2f, 0f, 0f, 0f, 10f,
            0f, 1.0f, 0f, 0f, 5f,
            0f, 0f, 0.8f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        "Köhnə" -> floatArrayOf(
            0.9f, 0.1f, 0.1f, 0f, 15f,
            0.1f, 0.8f, 0.1f, 0f, 15f,
            0.1f, 0.1f, 0.7f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
        )
        "Mavi" -> floatArrayOf(
            0.6f, 0f, 0f, 0f, 0f,
            0f, 0.7f, 0f, 0f, 0f,
            0f, 0f, 1.4f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        )
        "Canlı" -> floatArrayOf(
            1.3f, -0.15f, -0.15f, 0f, 0f,
            -0.15f, 1.3f, -0.15f, 0f, 0f,
            -0.15f, -0.15f, 1.3f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        "Parlaq" -> floatArrayOf(
            1.15f, 0f, 0f, 0f, 20f,
            0f, 1.15f, 0f, 0f, 20f,
            0f, 0f, 1.15f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        )
        "Yaşıl" -> floatArrayOf(
            0.8f, 0f, 0f, 0f, 0f,
            0f, 1.25f, 0f, 0f, 10f,
            0f, 0f, 0.8f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        else -> floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    val result = FloatArray(20)
    val c = contrast
    val bOffset = brightness * 255f
    
    val lr = 0.213f
    val lg = 0.715f
    val lb = 0.072f
    val s = saturation

    result[0] = (base[0] * ((1f - s) * lr + s)) * c
    result[1] = (base[1] * ((1f - s) * lg)) * c
    result[2] = (base[2] * ((1f - s) * lb)) * c
    result[3] = base[3]
    result[4] = base[4] * c + bOffset

    result[5] = (base[5] * ((1f - s) * lr)) * c
    result[6] = (base[6] * ((1f - s) * lg + s)) * c
    result[7] = (base[7] * ((1f - s) * lb)) * c
    result[8] = base[8]
    result[9] = base[9] * c + bOffset

    result[10] = (base[10] * ((1f - s) * lr)) * c
    result[11] = (base[11] * ((1f - s) * lg)) * c
    result[12] = (base[12] * ((1f - s) * lb + s)) * c
    result[13] = base[13]
    result[14] = base[14] * c + bOffset

    result[15] = base[15]
    result[16] = base[16]
    result[17] = base[17]
    result[18] = base[18]
    result[19] = base[19]

    return ComposeColorMatrix(result)
}
