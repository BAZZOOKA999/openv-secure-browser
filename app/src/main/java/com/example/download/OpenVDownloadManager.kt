package com.example.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import android.widget.Toast
import com.example.data.model.DownloadItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OpenVDownloadManager(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    fun enqueueDownload(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String
    ): Long {
        try {
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("OpenV: $fileName")
                setDescription("Baixando arquivo com segurança...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                addRequestHeader("User-Agent", userAgent)
                setMimeType(mimeType)
            }

            val downloadId = downloadManager.enqueue(request)
            val item = DownloadItem(
                id = downloadId,
                fileName = fileName,
                url = url,
                mimeType = mimeType,
                status = "Iniciado"
            )
            _downloads.update { listOf(item) + it }
            Toast.makeText(context, "Download iniciado: $fileName", Toast.LENGTH_SHORT).show()
            return downloadId
        } catch (e: Exception) {
            Toast.makeText(context, "Falha ao iniciar download: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            return -1L
        }
    }
}
