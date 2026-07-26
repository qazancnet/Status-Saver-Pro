package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.model.StatusItem
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.HomeScreen
import com.example.util.LanguageManager
import com.example.util.t
import com.example.ui.screens.ImageEditorScreen
import com.example.ui.screens.VideoEditorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.StatusViewModel

// Simple custom navigation model
sealed class Screen {
    object Home : Screen()
    object FileManager : Screen()
    data class Detail(val status: StatusItem) : Screen()
    data class ImageEditor(val status: StatusItem) : Screen()
    data class VideoEditor(val status: StatusItem) : Screen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: StatusViewModel by viewModels()
    private val fileManagerViewModel: com.example.viewmodel.FileManagerViewModel by viewModels()

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize LanguageManager
        com.example.util.LanguageManager.init(applicationContext)
        
        // Initialize Coil ImageLoader with VideoFrameDecoder
        val imageLoader = coil.ImageLoader.Builder(this)
            .components {
                add(coil.decode.VideoFrameDecoder.Factory())
            }
            .build()
        coil.Coil.setImageLoader(imageLoader)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Main surface background
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

                    // Storage Permission Request Check on Launch
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val isGranted = permissions.values.any { it } || permissions.isEmpty()
                        if (isGranted) {
                            viewModel.loadStatuses()
                        } else {
                            Toast.makeText(
                                this,
                                LanguageManager.translate("Statusları yükləmək üçün yaddaş icazəsi lazımdır.", "Storage permission is required to load statuses.", "Разрешение на память необходимо для загрузки статусов.", "Durumları yüklemek için depolama izni gereklidir."),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    LaunchedEffect(Unit) {
                        val neededPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_AUDIO
                            )
                        } else {
                            arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        }

                        val missingPermissions = neededPermissions.filter {
                            ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                        }.toTypedArray()

                        if (missingPermissions.isNotEmpty()) {
                            permissionLauncher.launch(missingPermissions)
                        } else {
                            viewModel.loadStatuses()
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Slide transitions for premium feel
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                slideInHorizontally(initialOffsetX = { it }) togetherWith
                                        slideOutHorizontally(targetOffsetX = { -it })
                            },
                            label = "screen_navigation"
                        ) { screen ->
                            when (screen) {
                                is Screen.Home -> {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        onStatusClick = { selected ->
                                            currentScreen = Screen.Detail(selected)
                                        },
                                        onFileManagerClick = {
                                            currentScreen = Screen.FileManager
                                        }
                                    )
                                }
                                is Screen.FileManager -> {
                                    BackHandler { currentScreen = Screen.Home }
                                    com.example.ui.screens.FileManagerScreen(
                                        viewModel = viewModel,
                                        fileManagerViewModel = fileManagerViewModel,
                                        onBack = { currentScreen = Screen.Home }
                                    )
                                }
                                is Screen.Detail -> {
                                    BackHandler { currentScreen = Screen.Home }
                                    DetailScreen(
                                        status = screen.status,
                                        onBack = { currentScreen = Screen.Home },
                                        onEditClick = { selected ->
                                            currentScreen = if (selected.isVideo) {
                                                Screen.VideoEditor(selected)
                                            } else {
                                                Screen.ImageEditor(selected)
                                            }
                                        },
                                        onSaveClick = {
                                            viewModel.saveStatus(screen.status)
                                            // Update state to show checked indicator
                                            currentScreen = Screen.Home
                                        },
                                        onDeleteClick = {
                                            viewModel.deleteStatus(screen.status)
                                            currentScreen = Screen.Home
                                        }
                                    )
                                }
                                is Screen.ImageEditor -> {
                                    BackHandler { currentScreen = Screen.Detail(screen.status) }
                                    ImageEditorScreen(
                                        status = screen.status,
                                        viewModel = viewModel,
                                        onBack = { currentScreen = Screen.Detail(screen.status) },
                                        onSaveEdits = { item, bitmap ->
                                            viewModel.saveImageEdits(item, bitmap) {
                                                currentScreen = Screen.Home
                                            }
                                        }
                                    )
                                }
                                is Screen.VideoEditor -> {
                                    BackHandler { currentScreen = Screen.Detail(screen.status) }
                                    VideoEditorScreen(
                                        status = screen.status,
                                        onBack = { currentScreen = Screen.Detail(screen.status) },
                                        onSaveEdits = { item, startSec, endSec, speed, isMuted ->
                                            viewModel.saveVideoEdits(item, startSec, endSec, speed, isMuted)
                                            currentScreen = Screen.Home
                                        }
                                    )
                                }
                            }
                        }

                        // Beautiful animated Coin Event Announcement Overlay
                        CoinEffectOverlay(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun CoinEffectOverlay(viewModel: StatusViewModel) {
    val show = viewModel.showCoinAnimation
    val amount = viewModel.coinAnimationAmount
    if (!show) return

    // Auto-dismiss after 2.3 seconds
    LaunchedEffect(show) {
        kotlinx.coroutines.delay(2300)
        viewModel.showCoinAnimation = false
    }

    var animProgress by remember { mutableStateOf(0f) }
    val progress by animateFloatAsState(
        targetValue = animProgress,
        animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
        label = "coin_anim"
    )

    LaunchedEffect(show) {
        animProgress = 1f
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Dim background slightly as progress grows, then fade out
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = 0.5f * (1f - progress)
                    )
                )
        )

        val isEarned = amount > 0

        // Card Container
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E24)
            ),
            border = BorderStroke(2.dp, if (isEarned) Color(0xFF25D366) else Color(0xFFFFB300)),
            modifier = Modifier
                .graphicsLayer {
                    scaleX = 0.6f + (progress * 0.4f)
                    scaleY = 0.6f + (progress * 0.4f)
                    alpha = if (progress < 0.8f) 1f else (1f - progress) * 5f
                    translationY = -progress * 180f
                }
                .shadow(24.dp, shape = RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Golden Glow Behind Rotating Coin
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                rotationY = progress * 360f * 3f
                            }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isEarned) t("TƏBRİKLƏR!", "CONGRATULATIONS!", "ПОЗДРАВЛЯЕМ!", "TEBRİKLER!") else t("COIN XƏRCLƏNDİ", "COINS SPENT", "МОНЕТЫ ПОТРАЧЕНЫ", "COIN HARCANDI"),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isEarned) "+$amount Coin" else "$amount Coin",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = if (isEarned) Color(0xFF25D366) else Color(0xFFFFB300)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isEarned) t("Balansınız artırıldı", "Your balance has been updated", "Ваш баланс пополнен", "Bakiyeniz güncellendi") else t("İşləm uğurla icra edildi", "Action executed successfully", "Действие успешно выполнено", "İşlem başarıyla gerçekleştirildi"),
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }
        }

        // Particle explosion effect
        val particles = remember {
            List(12) { i ->
                val angle = (i * (360f / 12f)) * (Math.PI / 180f)
                val distance = (120..220).random()
                Offset(
                    x = (Math.cos(angle) * distance).toFloat(),
                    y = (Math.sin(angle) * distance).toFloat()
                )
            }
        }

        particles.forEach { offset ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier
                    .offset(
                        x = (offset.x * progress).dp,
                        y = (offset.y * progress - (progress * 120f)).dp
                    )
                    .graphicsLayer {
                        scaleX = 0.3f + (1f - progress) * 0.7f
                        scaleY = 0.3f + (1f - progress) * 0.7f
                        alpha = 1f - progress
                        rotationZ = progress * 360f
                    }
                    .size(24.dp)
            )
        }
    }
}
