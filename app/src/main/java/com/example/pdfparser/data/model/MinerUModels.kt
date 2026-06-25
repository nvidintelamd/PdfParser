package com.example.pdfparser.data.model

data class BatchUploadRequest(
    val files: List<FileEntry>,
    val model_version: String = "vlm",
    val enable_formula: Boolean = true,
    val enable_table: Boolean = true,
    val language: String = "ch",
    val extra_formats: List<String>? = null
)

data class FileEntry(
    val name: String,
    val data_id: String? = null,
    val is_ocr: Boolean = true,
    val page_ranges: String? = null
)

data class BatchUploadResponse(
    val code: Int,
    val msg: String,
    val data: BatchUploadData?
)

data class BatchUploadData(
    val batch_id: String,
    val file_urls: List<String>
)

data class BatchResultResponse(
    val code: Int,
    val msg: String,
    val data: BatchResultData?
)

data class BatchResultData(
    val batch_id: String,
    val extract_result: List<ExtractResult>?
)

data class ExtractResult(
    val file_name: String,
    val state: String,
    val full_zip_url: String?,
    val err_msg: String?,
    val extract_progress: ExtractProgress?,
    val data_id: String?
)

data class ExtractProgress(
    val extracted_pages: Int,
    val total_pages: Int,
    val start_time: String
)

data class PdfInfo(
    val fileSize: Long,
    val pageCount: Int,
    val needsSplit: Boolean
)
