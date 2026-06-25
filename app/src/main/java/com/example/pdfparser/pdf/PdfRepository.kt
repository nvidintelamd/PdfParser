package com.example.pdfparser.pdf

import android.content.Context
import android.net.Uri
import com.example.pdfparser.data.model.PdfInfo
import com.shockwave.pdfium.PdfiumCore

class PdfRepository(private val context: Context) {

    /**
     * 检查 PDF 大小和页数，判断是否需要分割
     */
    fun checkPdf(uri: Uri): PdfInfo {
        val fileSize = context.contentResolver.openFileDescriptor(uri, "r")?.use {
            it.statSize
        } ?: 0L

        val fd = context.contentResolver.openFileDescriptor(uri, "r")
        val pdfium = PdfiumCore(context)
        val doc = pdfium.newDocument(fd)
        val pageCount = doc.getPagesCount()
        doc.close()

        // 留 10% 安全余量：≤180MB 且 ≤180页 才不分割
        val needsSplit = fileSize > 180L * 1024 * 1024 || pageCount > 180

        return PdfInfo(fileSize, pageCount, needsSplit)
    }

    /**
     * 获取 PDF 页数
     */
    fun getPageCount(uri: Uri): Int {
        val fd = context.contentResolver.openFileDescriptor(uri, "r")
        val pdfium = PdfiumCore(context)
        val doc = pdfium.newDocument(fd)
        val count = doc.getPagesCount()
        doc.close()
        return count
    }
}
