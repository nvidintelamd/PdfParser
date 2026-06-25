package com.example.pdfparser.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.pdfparser.data.local.TokenStorage
import com.example.pdfparser.data.model.FileEntry
import com.example.pdfparser.data.repository.MineruRepository
import com.example.pdfparser.pdf.PdfRepository
import com.example.pdfparser.util.FileUtils
import com.example.pdfparser.util.MarkdownUtils
import net.lingala.zip4j.ZipFile
import java.io.File

class ParseWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "ParseWorker"
    }

    override suspend fun doWork(): Result {
        val token = inputData.getString("token") ?: return Result.failure(
            workDataOf("error" to "No token provided")
        )
        val fileUriString = inputData.getString("file_uri") ?: return Result.failure(
            workDataOf("error" to "No file URI")
        )
        val fileName = inputData.getString("file_name") ?: return Result.failure(
            workDataOf("error" to "No file name")
        )
        val pdfName = inputData.getString("pdf_name") ?: return Result.failure(
            workDataOf("error" to "No PDF name")
        )
        val needsSplit = inputData.getBoolean("needs_split", false)

        return try {
            val context = applicationContext
            val mineruRepo = MineruRepository()
            val fileUri = android.net.Uri.parse(fileUriString)

            val outputDir = FileUtils.getOutputDir(context, pdfName)
            outputDir.mkdirs()

            if (!needsSplit) {
                uploadSingleFile(context, mineruRepo, token, fileUri, fileName, outputDir)
            } else {
                uploadSplitFiles(context, mineruRepo, token, fileUri, fileName, outputDir)
            }

            val outputData = workDataOf("output_path" to outputDir.absolutePath)
            setProgress(workDataOf("message" to "完成!", "percent" to 100))
            Result.success(outputData)

        } catch (e: Exception) {
            Log.e(TAG, "Parse failed", e)
            Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
        }
    }

    private suspend fun uploadSingleFile(
        context: Context,
        mineruRepo: MineruRepository,
        token: String,
        fileUri: android.net.Uri,
        fileName: String,
        outputDir: File
    ) {
        val fileBytes = FileUtils.readFileBytes(context, fileUri)

        setProgress("上传文件中...", 20, "准备上传 $fileName")
        val files = listOf(FileEntry(name = fileName, is_ocr = true))
        val uploadResponse = mineruRepo.requestUploadUrls(token, files)

        if (uploadResponse.code != 0 || uploadResponse.data == null) {
            throw Exception("申请上传URL失败: ${uploadResponse.msg}")
        }

        val uploadUrl = uploadResponse.data.file_urls.first()
        val batchId = uploadResponse.data.batch_id

        setProgress("上传文件中...", 30, "上传 ${FileUtils.formatFileSize(fileBytes.size.toLong())}")
        mineruRepo.uploadFile(uploadUrl, fileBytes)

        val zipUrl = pollResult(mineruRepo, token, batchId)

        setProgress("下载结果...", 70)
        downloadAndExtract(mineruRepo, zipUrl, outputDir)
    }

    private suspend fun uploadSplitFiles(
        context: Context,
        mineruRepo: MineruRepository,
        token: String,
        fileUri: android.net.Uri,
        fileName: String,
        outputDir: File
    ) {
        // TODO: 实现 PDF 分割逻辑 (Phase 2)
        // 当前 MVP 版本不分割，直接上传
        setProgress("分割功能尚未实现，使用单文件模式...", 5)
        uploadSingleFile(context, mineruRepo, token, fileUri, fileName, outputDir)
    }

    private suspend fun pollResult(
        mineruRepo: MineruRepository,
        token: String,
        batchId: String
    ): String {
        val maxRetries = 200
        var retryCount = 0

        while (retryCount < maxRetries) {
            val response = mineruRepo.getBatchResult(token, batchId)
            val results = response.data?.extract_result

            if (results != null && results.isNotEmpty()) {
                val firstResult = results[0]

                when (firstResult.state) {
                    "done" -> {
                        return firstResult.full_zip_url
                            ?: throw Exception("解析完成但无下载链接")
                    }
                    "failed" -> {
                        throw Exception(firstResult.err_msg ?: "解析失败")
                    }
                    "running" -> {
                        val progress = firstResult.extract_progress
                        if (progress != null) {
                            val percent = if (progress.total_pages > 0) {
                                40 + (progress.extracted_pages * 20 / progress.total_pages)
                            } else 45
                            setProgress(
                                "等待解析...",
                                percent,
                                "已解析 ${progress.extracted_pages}/${progress.total_pages} 页"
                            )
                        }
                    }
                    "pending" -> {
                        setProgress("排队中...", 40)
                    }
                }
            }

            retryCount++
            kotlinx.coroutines.delay(3000)
        }

        throw Exception("轮询超时，请稍后重试")
    }

    private fun downloadAndExtract(
        mineruRepo: MineruRepository,
        zipUrl: String,
        outputDir: File
    ) {
        setProgress("下载结果...", 70)
        val zipBytes = mineruRepo.downloadZip(zipUrl)

        setProgress("解压文件...", 80)
        val zipFile = File(outputDir, "result.zip")
        zipFile.writeBytes(zipBytes)

        val extractDir = File(outputDir, "_temp_extract")
        extractDir.mkdirs()
        ZipFile(zipFile).extractAll(extractDir.absolutePath)
        zipFile.delete()

        setProgress("后处理...", 90)
        val extractedMd = File(extractDir, "full.md")
        val imagesDir = File(extractDir, "images")

        if (extractedMd.exists()) {
            val mdContent = extractedMd.readText()
            val fixedMd = MarkdownUtils.fixImagePaths(mdContent, imagesDir)
            File(outputDir, "full.md").writeText(fixedMd)
        }

        if (imagesDir.exists()) {
            val outImages = File(outputDir, "images")
            outImages.mkdirs()
            imagesDir.copyRecursively(outImages, overwrite = true)
        }

        extractDir.deleteRecursively()
    }

    private suspend fun setProgress(message: String, percent: Int, detail: String = "") {
        setProgress(workDataOf(
            "message" to message,
            "percent" to percent,
            "detail" to detail
        ))
    }
}
