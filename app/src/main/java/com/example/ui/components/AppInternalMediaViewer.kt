package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.util.t
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sticker & GIF Internal Viewer Dialog
 */
@Composable
fun AppInternalStickerViewerDialog(
    mediaUri: Uri,
    title: String,
    mimeType: String,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var backgroundBgType by remember { mutableIntStateOf(0) } // 0: Transparent/Dark, 1: Chat Green, 2: Light White

    val copiedMsg = t("Mətn kopyalandı", "Text copied", "Текст скопирован", "Metin kopyalandı")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (mimeType.contains("gif")) Icons.Default.Gif else Icons.Default.StickyNote2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (mimeType.contains("gif")) t("Daxili GIF Baxıcısı", "Internal GIF Viewer", "Встроенный просмотр GIF", "Dahili GIF Görüntüleyici")
                                else t("Daxili Stiker Baxıcısı", "Internal Sticker Viewer", "Встроенный просмотр стикеров", "Dahili Çıkartma Görüntüleyici"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Preview Container with Background switcher
                val previewBgColor = when (backgroundBgType) {
                    1 -> Color(0xFF075E54) // WhatsApp Dark Green
                    2 -> Color(0xFFF0F2F5) // Chat Light Gray
                    else -> Color(0xFF1F2937) // Dark Canvas
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(previewBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = mediaUri,
                        contentDescription = title,
                        modifier = Modifier
                            .size(200.dp)
                            .padding(12.dp),
                        contentScale = ContentScale.Fit
                    )

                    // Background switch indicator top right
                    IconButton(
                        onClick = { backgroundBgType = (backgroundBgType + 1) % 3 },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Switch Preview Canvas",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            backgroundBgType = (backgroundBgType + 1) % 3
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(t("Fon Değiş", "Bg Theme", "Фон", "Arkaplan"), fontSize = 12.sp)
                    }

                    if (onShare != null) {
                        Button(
                            onClick = onShare,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        }
                    }

                    if (onDelete != null) {
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
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
}

/**
 * Document & Text File Internal Reader / Viewer Dialog
 */
@Composable
fun AppInternalDocumentViewerDialog(
    docUri: Uri,
    title: String,
    filePath: String,
    fileSizeFormatted: String,
    mimeType: String,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var textContent by remember { mutableStateOf<String?>(null) }
    var isLoadingText by remember { mutableStateOf(false) }
    var isTextFile by remember { mutableStateOf(false) }

    val copiedMsg = t("Mətn buferə kopyalandı", "Text copied to clipboard", "Текст скопирован", "Metin kopyalandı")
    val truncatedNotice = t("Fayl çox böyükdür, ilk 200KB göstərilir", "File too large, showing first 200KB", "Файл слишком большой", "Dosya çok büyük")

    // Check if text or code format
    LaunchedEffect(docUri, mimeType, filePath) {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        val isText = mimeType.startsWith("text/") ||
                extension in listOf("txt", "json", "xml", "csv", "html", "log", "md", "vcf")

        isTextFile = isText

        if (isText) {
            isLoadingText = true
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(docUri)?.use { inputStream ->
                        val bytes = inputStream.readBytes()
                        textContent = if (bytes.size > 200000) {
                            // Trim large files for UI safety
                            String(bytes, 0, 200000) + "\n\n...[$truncatedNotice]..."
                        } else {
                            String(bytes)
                        }
                    }
                } catch (e: Exception) {
                    textContent = null
                } finally {
                    isLoadingText = false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(if (isTextFile) 0.85f else 0.55f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isTextFile) Icons.Default.Description else Icons.Default.Article,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$fileSizeFormatted • $mimeType",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isTextFile) {
                    if (isLoadingText) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (!textContent.isNullOrEmpty()) {
                        // Scrollable Code / Text Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            val scrollState = rememberScrollState()
                            Text(
                                text = textContent!!,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t("Mətn oxuna bilmədi", "Could not read text content", "Не удалось прочитать текст", "Metin okunamadı"),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    // Binary Document Inspector Card (PDF / Word / ZIP / etc.)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = t("Sənəd tətbiq daxilində qorunur və təhlükəsizdir", "Document stored safely inside app", "Документ защищен внутри приложения", "Belge uygulama içinde güvenle korundu"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isTextFile && !textContent.isNullOrEmpty()) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Document Text", textContent)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, copiedMsg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(t("Kopyala", "Copy", "Копировать", "Kopyala"), fontSize = 12.sp)
                        }
                    }

                    if (onShare != null) {
                        Button(
                            onClick = onShare,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        }
                    }

                    if (onDelete != null) {
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
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
}
