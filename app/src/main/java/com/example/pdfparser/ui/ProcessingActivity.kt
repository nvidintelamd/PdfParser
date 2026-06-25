package com.example.pdfparser.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.pdfparser.databinding.ActivityProcessingBinding

class ProcessingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProcessingBinding
    private val viewModel: ProgressViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProcessingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()

        val fileUriString = intent.getStringExtra("file_uri")
        val fileName = intent.getStringExtra("file_name")
        val token = intent.getStringExtra("token")

        if (fileUriString == null || fileName == null || token == null) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val fileUri = Uri.parse(fileUriString)
        binding.tvTitle.text = "正在处理 $fileName..."

        // Start processing and observe work
        viewModel.startProcessing(this, fileUri, token, fileName)
        viewModel.observeWork(this, this)

        binding.btnCancel.setOnClickListener {
            viewModel.cancelProcessing(this)
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.progressPercent.observe(this) { percent ->
            binding.progressBar.progress = percent
            binding.tvProgressPercent.text = "$percent%"
        }

        viewModel.progressMessage.observe(this) { message ->
            binding.tvCurrentStage.text = message
        }

        viewModel.progressDetail.observe(this) { detail ->
            binding.tvDetail.text = detail
            binding.tvDetail.visibility = if (detail.isBlank()) View.GONE else View.VISIBLE
        }

        viewModel.stageStatus.observe(this) { stages ->
            stages[1]?.let { binding.tvStage1.text = it }
            stages[2]?.let { binding.tvStage2.text = it }
            stages[3]?.let { binding.tvStage3.text = it }
            stages[4]?.let { binding.tvStage4.text = it }
            stages[5]?.let { binding.tvStage5.text = it }
            stages[6]?.let { binding.tvStage6.text = it }
        }

        viewModel.outputPath.observe(this) { path ->
            if (path != null) {
                binding.btnCancel.text = "完成"
                binding.btnCancel.setOnClickListener {
                    finish()
                }
            }
        }

        viewModel.error.observe(this) { error ->
            if (error != null) {
                binding.btnCancel.text = "返回"
                binding.btnCancel.setOnClickListener {
                    finish()
                }
            }
        }
    }
}
