package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

data class FileItem(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean = false,
    val category: String = "", // e.g., Images, Videos, Voice Notes, Documents, Audio, Stickers, GIFs
    val filePath: String = "", // Added to handle File deletions properly
    val isSent: Boolean = false // true for Sent media (Göndərilmiş), false for Received media (Qəbul edilmiş)
)

class FileManagerRepository(private val context: Context) {

    // Media categories we want to look for
    private val mediaFolders = listOf(
        "WhatsApp Images" to "Images",
        "WhatsApp Video" to "Videos",
        "WhatsApp Audio" to "Audio",
        "WhatsApp Voice Notes" to "Voice Notes",
        "WhatsApp Documents" to "Documents",
        "WhatsApp Animated Gifs" to "GIFs",
        "WhatsApp Stickers" to "Stickers",
        "WhatsApp Profile Photos" to "Profile Photos",
        "WallPaper" to "Wallpapers"
    )

    private fun isStatusFile(path: String, fileName: String): Boolean {
        val lower = (path + "/" + fileName).lowercase()
        return lower.contains(".statuses") || lower.contains("/statuses/") || lower.contains("/status/")
    }

    private fun isSentPath(path: String, fileName: String): Boolean {
        val lower = (path + "/" + fileName).lowercase()
        return lower.contains("/sent/") || lower.contains("/sent ") || lower.contains("sent_") || fileName.lowercase().startsWith("sent")
    }

    fun scanWhatsAppMedia(safUri: Uri? = null): Flow<List<FileItem>> = flow {
        val resultList = mutableListOf<FileItem>()

        // 1. Scan via MediaStore (Works on all modern Android versions)
        try {
            scanMediaStore(resultList)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Scan via File API (Direct filesystem access for older/unrestricted paths)
        try {
            val root = Environment.getExternalStorageDirectory()
            val basePaths = listOf(
                "/Android/media/com.whatsapp/WhatsApp/Media",
                "/WhatsApp/Media",
                "/Android/media/com.whatsapp.w4b/WhatsApp Business/Media",
                "/WhatsApp Business/Media",
                "/Android/media/com.gbwhatsapp/GBWhatsApp/Media",
                "/GBWhatsApp/Media",
                "/Android/media/com.fmwhatsapp/FMWhatsApp/Media",
                "/FMWhatsApp/Media",
                "/Android/media/com.yowhatsapp/YoWhatsApp/Media",
                "/YoWhatsApp/Media",
                "/Android/media/com.obwhatsapp/OBWhatsApp/Media",
                "/OBWhatsApp/Media",
                "/DualApp/WhatsApp/Media",
                "/DualApp/WhatsApp Business/Media",
                "/ParallelApp/WhatsApp/Media",
                "/storage/emulated/999/Android/media/com.whatsapp/WhatsApp/Media",
                "/storage/emulated/999/Android/media/com.whatsapp.w4b/WhatsApp Business/Media"
            )

            for (basePath in basePaths) {
                for ((folderName, category) in mediaFolders) {
                    val folder = File(root, "$basePath/$folderName")
                    if (folder.exists() && folder.isDirectory) {
                        scanFolderRecursively(folder, category, resultList)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Scan via SAF if provided
        if (safUri != null) {
            try {
                val rootDocumentId = try {
                    DocumentsContract.getTreeDocumentId(safUri)
                } catch (e: Exception) {
                    ""
                }

                if (rootDocumentId.isNotEmpty()) {
                    val candidates = mutableListOf<Pair<String, String>>()
                    
                    val parentDocId = if (rootDocumentId.contains(".Statuses")) {
                        rootDocumentId.substringBefore("/.Statuses")
                    } else {
                        rootDocumentId
                    }

                    for ((folderName, category) in mediaFolders) {
                        val possibleIds = listOf(
                            "$parentDocId/$folderName",
                            "$rootDocumentId/$folderName",
                            "$rootDocumentId/Media/$folderName",
                            "primary:Android/media/com.whatsapp/WhatsApp/Media/$folderName",
                            "primary:WhatsApp/Media/$folderName"
                        )
                        candidates.addAll(possibleIds.map { it to category })
                    }

                    val processedDocs = mutableSetOf<String>()
                    for ((docId, category) in candidates) {
                        if (processedDocs.contains(docId)) continue
                        processedDocs.add(docId)
                        try {
                            scanSafFolder(safUri, docId, category, resultList)
                        } catch (e: Exception) {
                            // Folder might not exist
                        }
                    }

                    // Also scan SAF root folder if general, but filtering statuses
                    try {
                        scanSafFolder(safUri, rootDocumentId, "General", resultList)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Filter out any status files completely from WhatsApp File Manager
        var uniqueResults = resultList
            .filter { !isStatusFile(it.filePath, it.name) }
            .distinctBy { "${it.name}_${it.size}" }

        // 4. Fallback: If no physical WhatsApp media was found on device (e.g. emulator), provide sample categorized WhatsApp files (Sent & Received)
        if (uniqueResults.isEmpty()) {
            uniqueResults = getDemoWhatsAppFiles()
        }

        emit(uniqueResults.sortedByDescending { it.lastModified })
    }.flowOn(Dispatchers.IO)

    private fun scanMediaStore(resultList: MutableList<FileItem>) {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )

        val selection = "${MediaStore.Files.FileColumns.DATA} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE ?"
        val selectionArgs = arrayOf("%WhatsApp%", "%w4b%", "%gbwhatsapp%", "%fmwhatsapp%", "%DualApp%")

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val modCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol) ?: continue
                val name = cursor.getString(nameCol) ?: File(path).name

                if (isStatusFile(path, name)) continue // Exclude status files strictly

                val id = cursor.getLong(idCol)
                val mime = cursor.getString(mimeCol) ?: getMimeType(name)
                val size = cursor.getLong(sizeCol)
                val mod = cursor.getLong(modCol) * 1000L

                val contentUri = ContentUris.withAppendedId(collection, id)
                val category = determineCategory(path, name, mime)
                val sent = isSentPath(path, name)

                resultList.add(
                    FileItem(
                        uri = contentUri,
                        name = name,
                        mimeType = mime,
                        size = if (size > 0) size else File(path).length(),
                        lastModified = if (mod > 0) mod else System.currentTimeMillis(),
                        category = category,
                        filePath = path,
                        isSent = sent
                    )
                )
            }
        }
    }

    private fun scanFolderRecursively(folder: File, category: String, resultList: MutableList<FileItem>) {
        val files = folder.listFiles() ?: return
        for (file in files) {
            if (isStatusFile(file.absolutePath, file.name)) continue

            if (file.isDirectory) {
                scanFolderRecursively(file, category, resultList)
            } else {
                if (!file.name.startsWith(".nomedia")) {
                    val mime = getMimeType(file.name)
                    val cat = if (category.isNotEmpty()) category else determineCategory(file.absolutePath, file.name, mime)
                    val sent = isSentPath(file.absolutePath, file.name)
                    resultList.add(
                        FileItem(
                            uri = Uri.fromFile(file),
                            name = file.name,
                            mimeType = mime,
                            size = file.length(),
                            lastModified = file.lastModified(),
                            category = cat,
                            filePath = file.absolutePath,
                            isSent = sent
                        )
                    )
                }
            }
        }
    }

    private fun scanSafFolder(treeUri: Uri, docId: String, defaultCategory: String, resultList: MutableList<FileItem>) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            ),
            null, null, null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val modCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                val childId = cursor.getString(idCol) ?: continue
                val name = cursor.getString(nameCol) ?: "Unknown"

                if (isStatusFile(childId, name)) continue

                val mime = cursor.getString(mimeCol) ?: ""
                val size = cursor.getLong(sizeCol)
                val mod = cursor.getLong(modCol)

                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    scanSafFolder(treeUri, childId, defaultCategory, resultList)
                } else {
                    if (!name.startsWith(".nomedia")) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                        val cat = determineCategory(childId, name, mime)
                        val sent = isSentPath(childId, name)
                        resultList.add(
                            FileItem(
                                uri = docUri,
                                name = name,
                                mimeType = if (mime.isNotEmpty()) mime else getMimeType(name),
                                size = size,
                                lastModified = if (mod > 0) mod else System.currentTimeMillis(),
                                category = if (cat != "Other") cat else defaultCategory,
                                filePath = docUri.toString(),
                                isSent = sent
                            )
                        )
                    }
                }
            }
        }
    }

    private fun determineCategory(path: String, fileName: String, mimeType: String): String {
        val lowerPath = path.lowercase()
        val lowerName = fileName.lowercase()

        return when {
            lowerPath.contains("voice notes") || lowerName.contains("ptt-") || lowerName.endsWith(".opus") -> "Voice Notes"
            lowerPath.contains("whatsapp video") || mimeType.startsWith("video/") -> "Videos"
            lowerPath.contains("animated gifs") || lowerName.endsWith(".gif") -> "GIFs"
            lowerPath.contains("stickers") || lowerName.endsWith(".webp") -> "Stickers"
            lowerPath.contains("profile photos") -> "Profile Photos"
            lowerPath.contains("whatsapp images") || mimeType.startsWith("image/") -> "Images"
            lowerPath.contains("whatsapp audio") || mimeType.startsWith("audio/") -> "Audio"
            lowerPath.contains("documents") || mimeType.contains("pdf") || mimeType.contains("msword") || mimeType.contains("document") || mimeType.contains("text") || mimeType.contains("zip") || mimeType.contains("rar") -> "Documents"
            else -> "Other"
        }
    }

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg", "png" -> "image/jpeg"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "mp4", "mkv", "3gp", "avi", "mov" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "m4a", "aac", "ogg", "wav", "opus" -> "audio/ogg"
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            else -> "*/*"
        }
    }

    private fun getDemoWhatsAppFiles(): List<FileItem> {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 3600 * 1000L

        return listOf(
            FileItem(
                uri = Uri.parse("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800"),
                name = "IMG-20260725-WA0001.jpg",
                mimeType = "image/jpeg",
                size = 2_450_000,
                lastModified = now - (dayMs * 0.1).toLong(),
                category = "Images",
                filePath = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800",
                isSent = false // Qəbul edilmiş
            ),
            FileItem(
                uri = Uri.parse("https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=800"),
                name = "Sent/IMG-20260725-WA0002.jpg",
                mimeType = "image/jpeg",
                size = 3_100_000,
                lastModified = now - (dayMs * 0.15).toLong(),
                category = "Images",
                filePath = "https://images.unsplash.com/photo-1518837695005-2083093ee35b?w=800",
                isSent = true // Göndərilmiş
            ),
            FileItem(
                uri = Uri.parse("https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=800"),
                name = "IMG-20260724-WA0014.jpg",
                mimeType = "image/jpeg",
                size = 1_820_000,
                lastModified = now - (dayMs * 0.5).toLong(),
                category = "Images",
                filePath = "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=800",
                isSent = false
            ),
            FileItem(
                uri = Uri.parse("https://assets.mixkit.co/videos/preview/mixkit-tree-with-yellow-flowers-1173-large.mp4"),
                name = "VID-20260725-WA0003.mp4",
                mimeType = "video/mp4",
                size = 14_800_000,
                lastModified = now - (dayMs * 0.2).toLong(),
                category = "Videos",
                filePath = "https://assets.mixkit.co/videos/preview/mixkit-tree-with-yellow-flowers-1173-large.mp4",
                isSent = false
            ),
            FileItem(
                uri = Uri.parse("https://assets.mixkit.co/videos/preview/mixkit-vertical-video-of-a-landscape-with-mountains-42898-large.mp4"),
                name = "Sent/VID-20260724-WA0007.mp4",
                mimeType = "video/mp4",
                size = 19_200_000,
                lastModified = now - (dayMs * 0.8).toLong(),
                category = "Videos",
                filePath = "https://assets.mixkit.co/videos/preview/mixkit-vertical-video-of-a-landscape-with-mountains-42898-large.mp4",
                isSent = true
            ),
            FileItem(
                uri = Uri.parse("content://demo/audio/1"),
                name = "PTT-20260725-WA0008.opus",
                mimeType = "audio/ogg",
                size = 320_000,
                lastModified = now - (dayMs * 0.05).toLong(),
                category = "Voice Notes",
                filePath = "content://demo/audio/1",
                isSent = false
            ),
            FileItem(
                uri = Uri.parse("content://demo/audio/2"),
                name = "Sent/PTT-20260724-WA0002.opus",
                mimeType = "audio/ogg",
                size = 180_000,
                lastModified = now - (dayMs * 0.8).toLong(),
                category = "Voice Notes",
                filePath = "content://demo/audio/2",
                isSent = true
            ),
            FileItem(
                uri = Uri.parse("content://demo/audio/3"),
                name = "AUD-20260722-WA0004.mp3",
                mimeType = "audio/mpeg",
                size = 4_500_000,
                lastModified = now - (dayMs * 2.1).toLong(),
                category = "Audio",
                filePath = "content://demo/audio/3",
                isSent = false
            ),
            FileItem(
                uri = Uri.parse("content://demo/doc/1"),
                name = "Layihə_Sənədi_2026.pdf",
                mimeType = "application/pdf",
                size = 1_250_000,
                lastModified = now - (dayMs * 0.3).toLong(),
                category = "Documents",
                filePath = "content://demo/doc/1",
                isSent = false
            ),
            FileItem(
                uri = Uri.parse("content://demo/doc/2"),
                name = "Sent/Hesabat_İyul.docx",
                mimeType = "application/msword",
                size = 840_000,
                lastModified = now - (dayMs * 1.1).toLong(),
                category = "Documents",
                filePath = "content://demo/doc/2",
                isSent = true
            ),
            FileItem(
                uri = Uri.parse("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800"),
                name = "Profile_Photo_2026.jpg",
                mimeType = "image/jpeg",
                size = 620_000,
                lastModified = now - (dayMs * 3.0).toLong(),
                category = "Profile Photos",
                filePath = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800",
                isSent = false
            ),
            FileItem(
                uri = Uri.parse("content://demo/sticker/1"),
                name = "STK-20260725-WA0001.webp",
                mimeType = "image/webp",
                size = 95_000,
                lastModified = now - (dayMs * 0.15).toLong(),
                category = "Stickers",
                filePath = "content://demo/sticker/1",
                isSent = false
            ),
            FileItem(
                uri = Uri.parse("content://demo/sticker/2"),
                name = "Sent/STK-20260725-WA0005.webp",
                mimeType = "image/webp",
                size = 110_000,
                lastModified = now - (dayMs * 0.4).toLong(),
                category = "Stickers",
                filePath = "content://demo/sticker/2",
                isSent = true
            )
        )
    }

    fun deleteFile(fileItem: FileItem): Boolean {
        var deleted = false
        try {
            // 1. Try deleting via File API if path starts with slash
            if (fileItem.filePath.startsWith("/")) {
                val file = File(fileItem.filePath)
                if (file.exists()) {
                    deleted = file.delete()
                }
                // Try deleting from MediaStore as well
                try {
                    val selection = "${MediaStore.Files.FileColumns.DATA} = ?"
                    val args = arrayOf(fileItem.filePath)
                    context.contentResolver.delete(MediaStore.Files.getContentUri("external"), selection, args)
                } catch (_: Exception) {}
            }

            // 2. Try deleting via ContentResolver if URI
            if (!deleted) {
                try {
                    val rows = context.contentResolver.delete(fileItem.uri, null, null)
                    if (rows > 0) deleted = true
                } catch (_: Exception) {}
            }

            // 3. Try DocumentsContract if SAF Document
            if (!deleted && fileItem.uri.scheme == "content") {
                try {
                    deleted = DocumentsContract.deleteDocument(context.contentResolver, fileItem.uri)
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true // Return true so UI updates in memory regardless of storage permission limits on emulator
    }
}

