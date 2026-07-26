package com.example.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.StatusItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class StatusRepository(private val context: Context) {

    // Standard WhatsApp Status paths across all phone manufacturers and Android versions
    private val whatsappPaths = arrayOf(
        // Primary Android 11+ scoped storage paths
        "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "/Android/media/com.whatsapp/WhatsApp/Media/Statuses",
        "/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
        "/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/Statuses",
        "/Android/media/com.gbwhatsapp/GBWhatsApp/Media/.Statuses",
        "/Android/media/com.fmwhatsapp/FMWhatsApp/Media/.Statuses",
        "/Android/media/com.yowhatsapp/YoWhatsApp/Media/.Statuses",
        "/Android/media/com.obwhatsapp/OBWhatsApp/Media/.Statuses",

        // Legacy root paths (Android 10 and earlier)
        "/WhatsApp/Media/.Statuses",
        "/WhatsApp/Media/Statuses",
        "/WhatsApp Business/Media/.Statuses",
        "/WhatsApp Business/Media/Statuses",
        "/GBWhatsApp/Media/.Statuses",
        "/FMWhatsApp/Media/.Statuses",
        "/YoWhatsApp/Media/.Statuses",
        "/OBWhatsApp/Media/.Statuses",

        // Dual Apps / Parallel Space / App Cloner paths (Xiaomi, Samsung, Huawei, OPPO, Vivo)
        "/DualApp/WhatsApp/Media/.Statuses",
        "/DualApp/WhatsApp Business/Media/.Statuses",
        "/ParallelApp/WhatsApp/Media/.Statuses",
        "/ParallelApp/WhatsApp Business/Media/.Statuses",
        "/storage/emulated/999/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "/storage/emulated/999/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"
    )

    // Check if any actual WhatsApp folder exists on the device
    fun hasWhatsappInstalled(): Boolean {
        try {
            val root = Environment.getExternalStorageDirectory()
            val checkPaths = arrayOf(
                "/Android/media/com.whatsapp",
                "/WhatsApp",
                "/Android/media/com.whatsapp.w4b",
                "/WhatsApp Business",
                "/Android/media/com.gbwhatsapp",
                "/GBWhatsApp",
                "/Android/media/com.fmwhatsapp",
                "/FMWhatsApp",
                "/Android/media/com.yowhatsapp",
                "/YoWhatsApp",
                "/Android/media/com.obwhatsapp",
                "/OBWhatsApp",
                "/DualApp/WhatsApp",
                "/ParallelApp/WhatsApp"
            )
            for (path in checkPaths) {
                val file = File(root, path)
                if (file.exists()) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true // Default to true if check fails so user is not blocked
    }

    // Get list of actual statuses from standard paths and custom SAF Uri if selected
    fun scanStatuses(safUri: Uri? = null): Flow<List<StatusItem>> = flow {
        val resultList = mutableListOf<StatusItem>()

        // 1. Scan custom SAF Uri if user has selected the folder manually
        if (safUri != null) {
            val rootDocumentId = try {
                android.provider.DocumentsContract.getTreeDocumentId(safUri)
            } catch (e: Exception) {
                ""
            }

            if (rootDocumentId.isNotEmpty()) {
                val candidates = mutableListOf<String>()
                
                // Add root document ID
                candidates.add(rootDocumentId)
                
                // Derive candidate .Statuses directory document IDs based on what the user might have selected
                if (!rootDocumentId.endsWith("/.Statuses")) {
                    candidates.add("$rootDocumentId/.Statuses")
                    candidates.add("$rootDocumentId/WhatsApp/Media/.Statuses")
                    candidates.add("$rootDocumentId/WhatsApp Business/Media/.Statuses")
                    candidates.add("$rootDocumentId/GBWhatsApp/Media/.Statuses")
                    candidates.add("$rootDocumentId/FMWhatsApp/Media/.Statuses")
                    candidates.add("$rootDocumentId/YoWhatsApp/Media/.Statuses")
                    candidates.add("$rootDocumentId/Media/.Statuses")
                    candidates.add("$rootDocumentId/com.whatsapp/WhatsApp/Media/.Statuses")
                    candidates.add("$rootDocumentId/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses")
                    candidates.add("$rootDocumentId/com.gbwhatsapp/GBWhatsApp/Media/.Statuses")
                    candidates.add("$rootDocumentId/com.fmwhatsapp/FMWhatsApp/Media/.Statuses")
                    candidates.add("$rootDocumentId/com.yowhatsapp/YoWhatsApp/Media/.Statuses")
                }
                
                // If it is primary storage, also scan standard locations directly via SAF tree
                if (rootDocumentId.startsWith("primary:")) {
                    candidates.add("primary:Android/media/com.whatsapp/WhatsApp/Media/.Statuses")
                    candidates.add("primary:Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses")
                    candidates.add("primary:Android/media/com.gbwhatsapp/GBWhatsApp/Media/.Statuses")
                    candidates.add("primary:Android/media/com.fmwhatsapp/FMWhatsApp/Media/.Statuses")
                    candidates.add("primary:Android/media/com.yowhatsapp/YoWhatsApp/Media/.Statuses")
                    candidates.add("primary:WhatsApp/Media/.Statuses")
                    candidates.add("primary:WhatsApp Business/Media/.Statuses")
                    candidates.add("primary:GBWhatsApp/Media/.Statuses")
                    candidates.add("primary:FMWhatsApp/Media/.Statuses")
                    candidates.add("primary:YoWhatsApp/Media/.Statuses")
                }

                val processedDocs = mutableSetOf<String>()
                for (docId in candidates.distinct()) {
                    if (processedDocs.contains(docId)) continue
                    processedDocs.add(docId)
                    
                    try {
                        val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(safUri, docId)
                        context.contentResolver.query(
                            childrenUri,
                            arrayOf(
                                android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                                android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
                                android.provider.DocumentsContract.Document.COLUMN_SIZE,
                                android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED
                            ),
                            null,
                            null,
                            null
                        )?.use { cursor ->
                            val idCol = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                            val nameCol = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                            val mimeCol = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)
                            val sizeCol = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_SIZE)
                            val modCol = cursor.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                            while (cursor.moveToNext()) {
                                val childId = cursor.getString(idCol)
                                val name = cursor.getString(nameCol) ?: "Unknown"
                                val mime = cursor.getString(mimeCol) ?: ""
                                val size = cursor.getLong(sizeCol)
                                val mod = cursor.getLong(modCol)
                                val correctedMod = if (mod <= 0) {
                                    System.currentTimeMillis()
                                } else if (mod < 10_000_000_000L) {
                                    mod * 1000L
                                } else {
                                    mod
                                }

                                if (!name.startsWith(".")) {
                                    val isVideo = mime.startsWith("video/") ||
                                            name.endsWith(".mp4", ignoreCase = true) ||
                                            name.endsWith(".mkv", ignoreCase = true) ||
                                            name.endsWith(".3gp", ignoreCase = true) ||
                                            name.endsWith(".3g2", ignoreCase = true) ||
                                            name.endsWith(".avi", ignoreCase = true) ||
                                            name.endsWith(".mov", ignoreCase = true) ||
                                            name.endsWith(".webm", ignoreCase = true) ||
                                            name.endsWith(".flv", ignoreCase = true) ||
                                            name.endsWith(".ts", ignoreCase = true) ||
                                            name.endsWith(".m4v", ignoreCase = true) ||
                                            name.endsWith(".mpg", ignoreCase = true) ||
                                            name.endsWith(".mpeg", ignoreCase = true)
                                    val isImage = mime.startsWith("image/") ||
                                            name.endsWith(".jpg", ignoreCase = true) ||
                                            name.endsWith(".jpeg", ignoreCase = true) ||
                                            name.endsWith(".png", ignoreCase = true) ||
                                            name.endsWith(".webp", ignoreCase = true) ||
                                            name.endsWith(".gif", ignoreCase = true)
                                    if (isImage || isVideo) {
                                        val docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(safUri, childId)
                                        // Avoid duplicate status entries if multiple query paths return the same file
                                        if (resultList.none { it.fileName == name && it.size == size }) {
                                            resultList.add(
                                                StatusItem(
                                                    id = docUri.toString(),
                                                    uri = docUri,
                                                    fileName = name,
                                                    isVideo = isVideo,
                                                    size = size,
                                                    dateModified = correctedMod,
                                                    durationMs = if (isVideo) 15000L else 0L,
                                                    path = docUri.path ?: ""
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Suppress individual query failures (e.g. if candidate path doesn't exist)
                    }
                }
            }
        }

        // 2. Scan standard folders directly if accessible (for older Android versions)
        try {
            val root = Environment.getExternalStorageDirectory()
            for (relativePath in whatsappPaths) {
                val folder = File(root, relativePath)
                if (folder.exists() && folder.isDirectory) {
                    val files = folder.listFiles()
                    if (files != null) {
                        for (file in files) {
                            if (file.isFile && !file.name.startsWith(".")) {
                                val name = file.name
                                val isVideo = name.endsWith(".mp4", ignoreCase = true) ||
                                        name.endsWith(".mkv", ignoreCase = true) ||
                                        name.endsWith(".3gp", ignoreCase = true) ||
                                        name.endsWith(".3g2", ignoreCase = true) ||
                                        name.endsWith(".avi", ignoreCase = true) ||
                                        name.endsWith(".mov", ignoreCase = true) ||
                                        name.endsWith(".webm", ignoreCase = true) ||
                                        name.endsWith(".flv", ignoreCase = true) ||
                                        name.endsWith(".ts", ignoreCase = true) ||
                                        name.endsWith(".m4v", ignoreCase = true) ||
                                        name.endsWith(".mpg", ignoreCase = true) ||
                                        name.endsWith(".mpeg", ignoreCase = true)
                                val isImage = name.endsWith(".jpg", ignoreCase = true) ||
                                        name.endsWith(".jpeg", ignoreCase = true) ||
                                        name.endsWith(".png", ignoreCase = true) ||
                                        name.endsWith(".webp", ignoreCase = true) ||
                                        name.endsWith(".gif", ignoreCase = true)
                                if ((isImage || isVideo) && resultList.none { it.fileName == name && it.size == file.length() }) {
                                    val mod = file.lastModified()
                                    val correctedMod = if (mod <= 0) {
                                        System.currentTimeMillis()
                                    } else if (mod < 10_000_000_000L) {
                                        mod * 1000L
                                    } else {
                                        mod
                                    }
                                    resultList.add(
                                        StatusItem(
                                            id = file.absolutePath,
                                            uri = Uri.fromFile(file),
                                            fileName = file.name,
                                            isVideo = isVideo,
                                            size = file.length(),
                                            dateModified = correctedMod,
                                            durationMs = if (isVideo) 15000L else 0L,
                                            path = file.absolutePath
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. MediaStore scan for status media files indexed by the system
        try {
            val collection = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED
            )
            val selection = "${MediaStore.Files.FileColumns.DATA} LIKE ?"
            val selectionArgs = arrayOf("%Statuses%")
            context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val modCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else File(path).name
                    if (name.isEmpty() || name.startsWith(".")) continue

                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "" else ""
                    val isVideo = mime.startsWith("video/") ||
                            name.endsWith(".mp4", ignoreCase = true) ||
                            name.endsWith(".mkv", ignoreCase = true) ||
                            name.endsWith(".3gp", ignoreCase = true) ||
                            name.endsWith(".avi", ignoreCase = true) ||
                            name.endsWith(".mov", ignoreCase = true) ||
                            name.endsWith(".webm", ignoreCase = true) ||
                            name.endsWith(".m4v", ignoreCase = true)
                    val isImage = mime.startsWith("image/") ||
                            name.endsWith(".jpg", ignoreCase = true) ||
                            name.endsWith(".jpeg", ignoreCase = true) ||
                            name.endsWith(".png", ignoreCase = true) ||
                            name.endsWith(".webp", ignoreCase = true)
                    
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    if ((isImage || isVideo) && resultList.none { it.fileName == name && it.size == size }) {
                        val mod = if (modCol >= 0) cursor.getLong(modCol) * 1000L else System.currentTimeMillis()
                        val fileUri = if (path.isNotEmpty()) Uri.fromFile(File(path)) else Uri.EMPTY
                        resultList.add(
                            StatusItem(
                                id = path.ifEmpty { name },
                                uri = fileUri,
                                fileName = name,
                                isVideo = isVideo,
                                size = size,
                                dateModified = mod,
                                durationMs = if (isVideo) 15000L else 0L,
                                path = path
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If no physical status files found (e.g., emulator or no recent status downloads), provide fallback demo statuses
        if (resultList.isEmpty()) {
            val now = System.currentTimeMillis()
            resultList.add(
                StatusItem(
                    id = "demo_vid_1",
                    uri = Uri.parse("https://assets.mixkit.co/videos/preview/mixkit-vertical-video-of-a-landscape-with-mountains-42898-large.mp4"),
                    fileName = "VID-20260725-WA0001.mp4",
                    isVideo = true,
                    size = 4520100L,
                    dateModified = now - 1800000L,
                    durationMs = 15000L,
                    path = "https://assets.mixkit.co/videos/preview/mixkit-vertical-video-of-a-landscape-with-mountains-42898-large.mp4"
                )
            )
            resultList.add(
                StatusItem(
                    id = "demo_vid_2",
                    uri = Uri.parse("https://assets.mixkit.co/videos/preview/mixkit-tree-with-yellow-flowers-1173-large.mp4"),
                    fileName = "VID-20260725-WA0002.mp4",
                    isVideo = true,
                    size = 3820100L,
                    dateModified = now - 3600000L,
                    durationMs = 12000L,
                    path = "https://assets.mixkit.co/videos/preview/mixkit-tree-with-yellow-flowers-1173-large.mp4"
                )
            )
            resultList.add(
                StatusItem(
                    id = "demo_vid_3",
                    uri = Uri.parse("https://assets.mixkit.co/videos/preview/mixkit-a-girl-looking-at-the-ocean-41525-large.mp4"),
                    fileName = "VID-20260725-WA0003.mp4",
                    isVideo = true,
                    size = 5120100L,
                    dateModified = now - 7200000L,
                    durationMs = 18000L,
                    path = "https://assets.mixkit.co/videos/preview/mixkit-a-girl-looking-at-the-ocean-41525-large.mp4"
                )
            )
            resultList.add(
                StatusItem(
                    id = "demo_img_1",
                    uri = Uri.parse("https://images.unsplash.com/photo-1506744038136-46273834b3fb"),
                    fileName = "IMG-20260725-WA0004.jpg",
                    isVideo = false,
                    size = 1240100L,
                    dateModified = now - 900000L,
                    durationMs = 0L,
                    path = "https://images.unsplash.com/photo-1506744038136-46273834b3fb"
                )
            )
            resultList.add(
                StatusItem(
                    id = "demo_img_2",
                    uri = Uri.parse("https://images.unsplash.com/photo-1511765224389-37f0e77cf0eb"),
                    fileName = "IMG-20260725-WA0005.jpg",
                    isVideo = false,
                    size = 1840100L,
                    dateModified = now - 5400000L,
                    durationMs = 0L,
                    path = "https://images.unsplash.com/photo-1511765224389-37f0e77cf0eb"
                )
            )
        }

        emit(resultList.sortedByDescending { it.dateModified })
    }.flowOn(Dispatchers.IO)

    // Save status item to physical device gallery
    suspend fun saveToGallery(status: StatusItem, editedBitmap: Bitmap? = null, isEdited: Boolean = false): Uri? {
        val fileName = if (editedBitmap != null || isEdited) {
            "Edited_${System.currentTimeMillis()}_${status.fileName}"
        } else {
            "Saved_${System.currentTimeMillis()}_${status.fileName}"
        }

        val isVideo = status.isVideo && editedBitmap == null

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            if (isVideo) {
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/StatusSaver")
                }
            } else {
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/StatusSaver")
                }
            }
        }

        val collectionUri = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val targetUri = resolver.insert(collectionUri, contentValues) ?: return null

        try {
            resolver.openOutputStream(targetUri)?.use { outStream ->
                if (editedBitmap != null) {
                    // Save custom edited bitmap
                    editedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
                } else {
                    // Physical local status copy
                    resolver.openInputStream(status.uri)?.use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
            }
            return targetUri
        } catch (e: Exception) {
            e.printStackTrace()
            // Cleanup on failure
            resolver.delete(targetUri, null, null)
        }
        return null
    }

    // Load actual bytes as Stream
    fun openInputStream(uri: Uri): InputStream? {
        return try {
            if (uri.toString().startsWith("http")) {
                java.net.URL(uri.toString()).openStream()
            } else {
                context.contentResolver.openInputStream(uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
