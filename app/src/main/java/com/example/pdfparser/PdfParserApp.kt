package com.example.pdfparser

import android.app.Application
import androidx.work.Configuration

class PdfParserApp : Application(), Configuration.Provider {
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }
}
