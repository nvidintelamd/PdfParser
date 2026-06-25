package com.example.pdfparser.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import androidx.work.*
import com.example.pdfparser.data.model.PdfInfo
import com.example.pdfparser.pdf.PdfRepository
import com.example.pdfparser.worker.ParseWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val _pdfInfo = MutableLiveData<PdfInfo?>()
    val pdfInfo: LiveData<PdfInfo?> = _pdfInfo

    private val _fileName = MutableLiveData<String?>()
    val fileName: LiveData<String?> = _fileName

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _progressMessage = MutableLiveData("")
    val progressMessage: LiveData<String> = _progressMessage

    private val _progressPercent = MutableLiveData(0)
    val progressPercent: LiveData<Int> = _progressPercent

    private val _progressDetail = MutableLiveData("")
    val progressDetail: LiveData<String> = _progressDetail

    private val _stageStatus = MutableLiveData<Map<Int, String>>()
    val stageStatus: LiveData<Map<Int, String>> = _stageStatus

    private val _outputPath = MutableLiveData<String?>()
    val outputPath: LiveData<String?> = _outputPath

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _currentWorkId = MutableLiveData<String?>()
    val currentWorkId: LiveData<String?> = _currentWorkId

    init {
        initStageStatus()
    }

    private fun initStageStatus() {
        _stageStatus.value = mapOf(
            1 to "\u23F3 检查 PDF",
            2 to "\u23F3 分割 PDF",
            3 to "\u23F3 上传到 MinerU",
            4 to "\u23F3 等待解析",
            5 to "\u23F3 下载结果",
            6 to "\u23F3 后处理"
        )
    }

    fun checkPdf(context: android.content.Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pdfRepo = PdfRepository(context)
                val info = pdfRepo.checkPdf(uri)
                _pdfInfo.postValue(info)
            } catch (e: Exception) {
                _error.postValue("检查 PDF 失败: ${e.message}")
            }
        }
    }

    fun setFileName(name: String) {
        _fileName.value = name
    }

    fun startProcessing(context: android.content.Context, uri: Uri, token: String, fileName: String) {
        val pdfInfo = _pdfInfo.value ?: return
        val pdfName = fileName.removeSuffix(".pdf").removeSuffix(".PDF")

        _isProcessing.value = true
        _error.value = null
        _outputPath.value = null
        initStageStatus()
        updateStage(1, "\u2705 检查 PDF")

        val inputData = workDataOf(
            "token" to token,
            "file_uri" to uri.toString(),
            "file_name" to fileName,
            "pdf_name" to pdfName,
            "needs_split" to pdfInfo.needsSplit
        )

        val workRequest = OneTimeWorkRequestBuilder<ParseWorker>()
            .setInputData(inputData)
            .build()

        _currentWorkId.value = workRequest.id

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    fun observeWork(context: android.content.Context, lifecycleOwner: LifecycleOwner) {
        val workId = _currentWorkId.value ?: return

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(workId)
            .observe(lifecycleOwner) { info ->
                if (info == null) return@observe

                val message = info.progress.getString("message") ?: ""
                val percent = info.progress.getInt("percent", 0)
                val detail = info.progress.getString("detail") ?: ""

                _progressMessage.value = message
                _progressPercent.value = percent
                _progressDetail.value = detail

                updateStagesFromProgress(percent)

                when (info.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        _isProcessing.value = false
                        _outputPath.value = info.outputData.getString("output_path")
                        markAllComplete()
                    }
                    WorkInfo.State.FAILED -> {
                        _isProcessing.value = false
                        _error.value = info.outputData.getString("error") ?: "处理失败"
                    }
                    WorkInfo.State.CANCELLED -> {
                        _isProcessing.value = false
                        _error.value = "已取消"
                    }
                    else -> {}
                }
            }
    }

    private fun updateStagesFromProgress(percent: Int) {
        val stages = mutableMapOf<Int, String>()
        stages[1] = if (percent >= 5) "\u2705 检查 PDF" else "\u23F3 检查 PDF"
        stages[2] = if (percent >= 10) "\u2705 分割 PDF (跳过)" else "\u23F3 分割 PDF"
        stages[3] = when {
            percent >= 40 -> "\u2705 上传到 MinerU"
            percent >= 20 -> "\u1F504 上传到 MinerU"
            else -> "\u23F3 上传到 MinerU"
        }
        stages[4] = when {
            percent >= 70 -> "\u2705 等待解析"
            percent >= 40 -> "\u1F504 等待解析"
            else -> "\u23F3 等待解析"
        }
        stages[5] = when {
            percent >= 90 -> "\u2705 下载结果"
            percent >= 70 -> "\u1F504 下载结果"
            else -> "\u23F3 下载结果"
        }
        stages[6] = when {
            percent >= 100 -> "\u2705 后处理"
            percent >= 90 -> "\u1F504 后处理"
            else -> "\u23F3 后处理"
        }
        _stageStatus.value = stages
    }

    private fun updateStage(stage: Int, status: String) {
        val current = _stageStatus.value?.toMutableMap() ?: mutableMapOf()
        current[stage] = status
        _stageStatus.value = current
    }

    private fun markAllComplete() {
        _stageStatus.value = mapOf(
            1 to "\u2705 检查 PDF",
            2 to "\u2705 分割 PDF (跳过)",
            3 to "\u2705 上传到 MinerU",
            4 to "\u2705 等待解析",
            5 to "\u2705 下载结果",
            6 to "\u2705 后处理"
        )
    }

    fun cancelProcessing(context: android.content.Context) {
        _currentWorkId.value?.let { id ->
            WorkManager.getInstance(context).cancelWorkById(java.util.UUID.fromString(id))
        }
        _isProcessing.value = false
    }

    fun clearError() {
        _error.value = null
    }
}
