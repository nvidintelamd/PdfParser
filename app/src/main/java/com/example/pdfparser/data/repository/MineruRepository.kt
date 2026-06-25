package com.example.pdfparser.data.repository

import com.example.pdfparser.data.model.*
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class MineruRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonType = "application/json".toMediaType()

    /**
     * Step 1: 申请预签名上传 URL
     */
    fun requestUploadUrls(
        token: String,
        files: List<FileEntry>,
        modelVersion: String = "vlm"
    ): BatchUploadResponse {
        val request = BatchUploadRequest(files = files, model_version = modelVersion)
        val body = gson.toJson(request).toRequestBody(jsonType)

        val httpRequest = Request.Builder()
            .url("https://mineru.net/api/v4/file-urls/batch")
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()

        val response = client.newCall(httpRequest).execute()
        val responseBody = response.body?.string()
            ?: throw IOException("Empty response body")

        return gson.fromJson(responseBody, BatchUploadResponse::class.java)
    }

    /**
     * Step 2: PUT 上传文件到预签名 URL
     */
    fun uploadFile(fileUrl: String, fileBytes: ByteArray) {
        val body = fileBytes.toRequestBody(null)
        val request = Request.Builder()
            .url(fileUrl)
            .put(body)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Upload failed: HTTP ${response.code}")
        }
    }

    /**
     * Step 3: 查询批量解析结果
     */
    fun getBatchResult(token: String, batchId: String): BatchResultResponse {
        val request = Request.Builder()
            .url("https://mineru.net/api/v4/extract-results/batch/$batchId")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw IOException("Empty response body")

        return gson.fromJson(responseBody, BatchResultResponse::class.java)
    }

    /**
     * 下载 ZIP 文件并返回字节数组
     */
    fun downloadZip(url: String): ByteArray {
        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("Download failed: HTTP ${response.code}")
        }

        return response.body?.bytes() ?: throw IOException("Empty download body")
    }
}
