package com.example.pdfparser.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pdfparser.data.local.TokenStorage
import com.example.pdfparser.data.model.FileEntry
import com.example.pdfparser.data.repository.MineruRepository
import com.example.pdfparser.databinding.ActivitySettingsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val savedToken = TokenStorage.getToken(this)
        if (!savedToken.isNullOrBlank()) {
            binding.etToken.setText(savedToken)
        }

        binding.btnTestConnection.setOnClickListener {
            testConnection()
        }

        binding.btnSave.setOnClickListener {
            saveToken()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun testConnection() {
        val token = binding.etToken.text?.toString()?.trim() ?: ""
        if (token.isBlank()) {
            Toast.makeText(this, "请输入 Token", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnTestConnection.isEnabled = false
        binding.tvTestStatus.text = "测试中..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val repo = MineruRepository()
                val files = listOf(FileEntry(name = "test.pdf"))
                val response = repo.requestUploadUrls(token, files)
                val success = response.code == 0

                withContext(Dispatchers.Main) {
                    binding.btnTestConnection.isEnabled = true
                    if (success) {
                        binding.tvTestStatus.text = "✅ 连接正常"
                        binding.tvTestStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                    } else {
                        binding.tvTestStatus.text = "❌ ${response.msg}"
                        binding.tvTestStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnTestConnection.isEnabled = true
                    binding.tvTestStatus.text = "❌ 连接失败"
                    binding.tvTestStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                }
            }
        }
    }

    private fun saveToken() {
        val token = binding.etToken.text?.toString()?.trim() ?: ""
        if (token.isBlank()) {
            Toast.makeText(this, "Token 不能为空", Toast.LENGTH_SHORT).show()
            return
        }

        TokenStorage.saveToken(this, token)
        Toast.makeText(this, "Token 已保存", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }
}
