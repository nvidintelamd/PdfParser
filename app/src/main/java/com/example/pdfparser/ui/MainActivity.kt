package com.example.pdfparser.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.pdfparser.data.local.TokenStorage
import com.example.pdfparser.databinding.ActivityMainBinding
import com.example.pdfparser.util.FileUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ProgressViewModel by viewModels()

    private var selectedFileUri: Uri? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            onFileSelected(it)
        }
    }

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateStartButtonState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
        updateStartButtonState()
    }

    private fun setupObservers() {
        viewModel.pdfInfo.observe(this) { info ->
            if (info != null) {
                val fileName = viewModel.fileName.value ?: "unknown"
                val fileSizeStr = FileUtils.formatFileSize(info.fileSize)

                binding.tvFileName.text = fileName
                binding.tvFileSize.text = "大小: $fileSizeStr"
                binding.tvPageCount.text = "页数: ${info.pageCount}"

                if (info.needsSplit) {
                    binding.tvNeedsSplit.visibility = View.VISIBLE
                    binding.tvNeedsSplit.text = "需要分割 (>${if (info.fileSize > 180L * 1024 * 1024) "180MB" else "180页"})"
                } else {
                    binding.tvNeedsSplit.visibility = View.GONE
                }

                binding.cardInfo.visibility = View.VISIBLE
                binding.tvFileInfo.text = "$fileSizeStr | ${info.pageCount} 页"
                binding.tvFileInfo.visibility = View.VISIBLE
                binding.btnStartParse.isEnabled = true
            }
        }

        viewModel.outputPath.observe(this) { path ->
            if (path != null) {
                Toast.makeText(this, "完成! 输出目录: $path", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.error.observe(this) { error ->
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupListeners() {
        binding.btnSelectFile.setOnClickListener {
            filePickerLauncher.launch(arrayOf("application/pdf"))
        }

        binding.btnStartParse.setOnClickListener {
            startProcessing()
        }

        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            settingsLauncher.launch(intent)
        }
    }

    private fun onFileSelected(uri: Uri) {
        selectedFileUri = uri

        val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "unknown.pdf"
        viewModel.setFileName(fileName)

        binding.tvFileInfo.text = "正在检查文件..."
        binding.tvFileInfo.visibility = View.VISIBLE
        binding.cardInfo.visibility = View.GONE
        binding.btnStartParse.isEnabled = false

        viewModel.checkPdf(this, uri)
    }

    private fun startProcessing() {
        val token = TokenStorage.getToken(this)
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "请先在设置中配置 Token", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = selectedFileUri ?: return
        val fileName = viewModel.fileName.value ?: return

        val intent = Intent(this, ProcessingActivity::class.java).apply {
            putExtra("file_uri", uri.toString())
            putExtra("file_name", fileName)
            putExtra("token", token)
        }
        startActivity(intent)
    }

    private fun updateStartButtonState() {
        val hasToken = !TokenStorage.getToken(this).isNullOrBlank()
        val hasFile = selectedFileUri != null
        binding.btnStartParse.isEnabled = hasToken && hasFile
    }

    override fun onResume() {
        super.onResume()
        updateStartButtonState()
    }
}
