package com.udacity

import android.app.DownloadManager
import android.database.Cursor


/**
 * Fetches how many bytes have been downloaded so far and updates ProgressBar
 */
internal class DownloadProgress(
    private val downloadId: Long,
    private val manager: DownloadManager,
    private val onProgress: (progress: Int) -> Unit,
) : Thread() {
    private val query: DownloadManager.Query = DownloadManager.Query()
    private var cursor: Cursor = manager.query(query)
    private var lastBytesDownloadedSoFar = 0
    private var totalBytes = 0

    private var progress: Int = 0

    override fun run() {
        while (downloadId > 0) {
            try {
                sleep(500)
                cursor = manager.query(query)
                if (cursor.moveToFirst()) {

                    //get total bytes of the file
                    if (totalBytes <= 0) {
                        totalBytes =
                            cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    }
                    val bytesDownloadedSoFar: Int =
                        cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    if (bytesDownloadedSoFar == totalBytes && totalBytes > 0) {
                        interrupt()
                    } else {
                        progress += (bytesDownloadedSoFar - lastBytesDownloadedSoFar)
                        lastBytesDownloadedSoFar = bytesDownloadedSoFar
                        onProgress(progress)
                    }
                }
                cursor.close()
            } catch (e: Exception) {
                return
            }
        }
    }

    init {
        query.setFilterById(downloadId)
    }
}