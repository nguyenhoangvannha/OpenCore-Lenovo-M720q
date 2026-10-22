package com.lenovo.m720q.efitool

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SafEfiManager {

    suspend fun copyEfiNonRoot(
        context: Context,
        sourceUri: Uri?,
        sourceFallbackPath: String,
        targetUsbUri: Uri,
        onLog: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            onLog("[*] [Non-Root SAF] Kiểm tra quyền và thư mục đích USB...")
            val targetUsbDir = DocumentFile.fromTreeUri(context, targetUsbUri)
            if (targetUsbDir == null || !targetUsbDir.exists() || !targetUsbDir.isDirectory) {
                onLog("[ERROR] Không thể truy cập thư mục USB đã chọn. Vui lòng chọn lại USB.")
                return@withContext false
            }

            onLog("[+] Đã kết nối phân vùng USB: ${targetUsbDir.name ?: "USB Drive"}")

            // Handle backup of existing EFI if present on USB
            val existingEfi = targetUsbDir.findFile("EFI")
            if (existingEfi != null && existingEfi.exists()) {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val backupName = "EFI_backup_$timestamp"
                onLog("[*] Phát hiện thư mục EFI cũ trên USB. Đang đổi tên thành $backupName...")
                try {
                    existingEfi.renameTo(backupName)
                } catch (e: Exception) {
                    onLog("[!] Không thể đổi tên EFI cũ, tiến hành dọn dẹp các tệp cũ...")
                }
            }

            // Create target "EFI" directory on USB
            val destEfiDir = targetUsbDir.findFile("EFI") ?: targetUsbDir.createDirectory("EFI")
            if (destEfiDir == null) {
                onLog("[ERROR] Không thể tạo thư mục 'EFI' trên USB. Kiểm tra quyền ghi của USB.")
                return@withContext false
            }

            var copiedCount = 0

            // Check if source is a SAF DocumentTree Uri
            val sourceDocDir = if (sourceUri != null) DocumentFile.fromTreeUri(context, sourceUri) else null

            if (sourceDocDir != null && sourceDocDir.exists()) {
                onLog("[*] Nguồn EFI từ DocumentTree: ${sourceDocDir.name}")
                // If source folder itself is named "EFI", copy its children into destEfiDir
                val rootToCopy = if (sourceDocDir.name.equals("EFI", ignoreCase = true)) {
                    sourceDocDir
                } else {
                    sourceDocDir.findFile("EFI") ?: sourceDocDir
                }

                copiedCount = copyDocumentDirectory(context, rootToCopy, destEfiDir, onLog)
            } else {
                // Fallback to java.io.File from file path
                val srcFile = File(sourceFallbackPath)
                if (!srcFile.exists() || !srcFile.isDirectory) {
                    onLog("[ERROR] Không tìm thấy thư mục EFI nguồn tại: $sourceFallbackPath")
                    return@withContext false
                }
                onLog("[*] Nguồn EFI từ đường dẫn bộ nhớ: ${srcFile.absolutePath}")
                val rootToCopy = if (srcFile.name.equals("EFI", ignoreCase = true)) {
                    srcFile
                } else {
                    val childEfi = File(srcFile, "EFI")
                    if (childEfi.exists() && childEfi.isDirectory) childEfi else srcFile
                }

                copiedCount = copyFileDirectoryToDocument(context, rootToCopy, destEfiDir, onLog)
            }

            onLog("============================================================")
            onLog("[✓] [Non-Root SAF] THÀNH CÔNG! Đã copy $copiedCount tệp vào USB/EFI.")
            onLog("[✓] Tất cả tệp đã được flush ghi an toàn vào USB.")
            onLog("============================================================")
            true
        } catch (e: Exception) {
            onLog("[EXCEPTION Non-Root]: ${e.localizedMessage}")
            false
        }
    }

    private fun copyDocumentDirectory(
        context: Context,
        srcDir: DocumentFile,
        destDir: DocumentFile,
        onLog: (String) -> Unit
    ): Int {
        var count = 0
        val files = srcDir.listFiles()
        for (item in files) {
            if (item.isDirectory) {
                val subDestDir = destDir.findFile(item.name ?: "") ?: destDir.createDirectory(item.name ?: "dir")
                if (subDestDir != null) {
                    count += copyDocumentDirectory(context, item, subDestDir, onLog)
                }
            } else if (item.isFile) {
                val existingFile = destDir.findFile(item.name ?: "")
                existingFile?.delete()

                val mimeType = item.type ?: "application/octet-stream"
                val newDestFile = destDir.createFile(mimeType, item.name ?: "file")
                if (newDestFile != null) {
                    context.contentResolver.openInputStream(item.uri)?.use { input ->
                        context.contentResolver.openOutputStream(newDestFile.uri)?.use { output ->
                            input.copyTo(output, bufferSize = 64 * 1024)
                            output.flush()
                        }
                    }
                    count++
                    onLog("  + [File] ${item.name}")
                }
            }
        }
        return count
    }

    private fun copyFileDirectoryToDocument(
        context: Context,
        srcDir: File,
        destDir: DocumentFile,
        onLog: (String) -> Unit
    ): Int {
        var count = 0
        val files = srcDir.listFiles() ?: return 0
        for (file in files) {
            if (file.isDirectory) {
                val subDestDir = destDir.findFile(file.name) ?: destDir.createDirectory(file.name)
                if (subDestDir != null) {
                    count += copyFileDirectoryToDocument(context, file, subDestDir, onLog)
                }
            } else if (file.isFile) {
                val existingFile = destDir.findFile(file.name)
                existingFile?.delete()

                val newDestFile = destDir.createFile("application/octet-stream", file.name)
                if (newDestFile != null) {
                    FileInputStream(file).use { input ->
                        context.contentResolver.openOutputStream(newDestFile.uri)?.use { output ->
                            input.copyTo(output, bufferSize = 64 * 1024)
                            output.flush()
                        }
                    }
                    count++
                    onLog("  + [File] ${file.name}")
                }
            }
        }
        return count
    }
}
