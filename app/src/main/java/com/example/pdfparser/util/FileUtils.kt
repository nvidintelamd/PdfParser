package com.example.pdfparser.util

import android.content.Context
import android.net.Uri
import java.io.File

object FileUtils {

    fun getFileSize(context: Context, uri: Uri): Long {
        return context.contentResolver.openFileDescriptor(uri, "r")?.use {
            it.statSize
        } ?: 0L
    }

    fun readFileBytes(context: Context, uri: Uri): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use {
            it.readBytes()
        } ?: throw Exception("Failed to read file")
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun getOutputDir(context: Context, pdfNameWithoutExt: String): File {
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        return File(downloadsDir, "PDFParser/$pdfNameWithoutExt")
    }
}
