package com.udacity

import android.app.DownloadManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.databinding.DataBindingUtil
import com.udacity.databinding.ActivityMainBinding
import com.udacity.utils.createChannel
import com.udacity.utils.sendNotification
import kotlinx.coroutines.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var downloadID: Long = 0

    private val notificationManager: NotificationManager by lazy {
        ContextCompat.getSystemService(
            applicationContext,
            NotificationManager::class.java
        ) as NotificationManager
    }

    private val downloadManager: DownloadManager by lazy {
        getSystemService(DOWNLOAD_SERVICE) as DownloadManager
    }

    private fun setSelectionEnabled(enabled: Boolean) {
        binding.contentMain.radioGroup.run {
            isEnabled = enabled
            children.forEach {
                it.isEnabled = enabled
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setSupportActionBar(binding.toolbar)

        createChannel(
            getString(R.string.download_notification_channel_id),
            getString(R.string.download_notification_channel_name),
            getString(R.string.download_notification_channel_desc)
        )

        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        binding.contentMain.radioGroup.setOnCheckedChangeListener { group, checkedId ->
            fileTitle = group.findViewById<RadioButton>(checkedId).text.toString()
        }

        binding.contentMain.customButton.setOnClickListener {
            setSelectionEnabled(false)
            download(
                when (binding.contentMain.radioGroup.checkedRadioButtonId) {
                    R.id.radio_100mb -> URL_100MB_FILE
                    R.id.radio_glide -> URL_GLIDE
                    R.id.radio_current_repo -> URL_CURRENT_REPO
                    R.id.radio_retrofit -> URL_RETROFIT
                    else -> {
                        setSelectionEnabled(true)
                        binding.contentMain.customButton.done()
                        Toast.makeText(
                            this,
                            "Please choose one of the repository",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                }
            )
        }
    }

    private fun isDownloadCanceled(cursor: Cursor?): Boolean {
        return cursor == null || !cursor.moveToFirst()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID == id) {
                val cursor =
                    downloadManager.query(DownloadManager.Query().setFilterById(downloadID))
                if (!isDownloadCanceled(cursor)) {
                    try {
                        val status =
                            cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                        val filePath: String
                        val filename: String
                        Log.d(TAG, "download: Status: $status")
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL, DownloadManager.STATUS_FAILED -> {
                                filePath =
                                    cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                                filename = filePath.substring(
                                    filePath.lastIndexOf('/') + 1,
                                    filePath.length
                                )

                                when (status) {
                                    DownloadManager.STATUS_SUCCESSFUL -> {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Download Completed",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        notificationManager.sendNotification(
                                            applicationContext,
                                            fileTitle ?: filename,
                                            filePath,
                                            success = true
                                        )
                                    }
                                    DownloadManager.STATUS_FAILED -> {
                                        val reason =
                                            cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                                        Log.d(
                                            TAG,
                                            "Download failed with status->$status and reason->$reason"
                                        )
                                        downloadFailedOrCanceled(
                                            fileTitle ?: filename,
                                            filePath
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "onReceive: $e")
                    }
                } else {
                    downloadFailedOrCanceled(fileTitle ?: "Unknown", "")
                }
            }
        }
    }

    private fun downloadFailedOrCanceled(
        filename: String,
        filePath: String,
    ) {
        Toast.makeText(
            this@MainActivity,
            "Download Failed",
            Toast.LENGTH_SHORT
        ).show()

        notificationManager.sendNotification(
            applicationContext,
            fileTitle ?: filename,
            filePath,
            success = false
        )
    }

    private fun download(url: String) {
        val request =
            DownloadManager.Request(Uri.parse(url))
                .setTitle(getString(R.string.app_name))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDescription(getString(R.string.app_description))
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        setRequiresCharging(false)
                    }
                }
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

        downloadID =
            downloadManager.download(request)// enqueue puts the download request in the queue.
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private fun DownloadManager.download(request: DownloadManager.Request): Long {
        val downloadIdTemp = enqueue(request)
        coroutineScope.launch {
            var status: Int = -1
            try {
                loop@ while (isActive) {
                    val q = DownloadManager.Query().apply {
                        setFilterById(downloadID)
                    }
                    val cursor: Cursor = query(q)
                    cursor.moveToFirst()
                    val bytesDownloaded =
                        cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal =
                        cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val newStatus =
                        cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    withContext(Dispatchers.Main) {
                        if (bytesTotal >= 0) {
                            binding.contentMain.customButton.setProgress((bytesDownloaded.toFloat() / bytesTotal.toFloat()))
                        } else {
                            binding.contentMain.customButton.setIndeterminateProgress()
                        }
                    }
                    Log.d(TAG, "repeat status: $newStatus")
                    cursor.close()

                    if (newStatus != status) {
                        status = newStatus
                        when (status) {
                            DownloadManager.STATUS_PAUSED -> {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Download paused.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            DownloadManager.STATUS_SUCCESSFUL, DownloadManager.STATUS_FAILED -> {
                                break@loop
                            }
                        }
                    }
                    delay(500)
                }
            } catch (e: Exception) {
            }

            withContext(Dispatchers.Main) {
                setSelectionEnabled(true)
                binding.contentMain.run {
                    customButton.done()
                }
            }
        }
        return downloadIdTemp
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        coroutineScope.cancel()
        super.onDestroy()
    }

    companion object {
        private var fileTitle: String? = null

        private const val URL_100MB_FILE =
            "https://speed.hetzner.de/100MB.bin"
        private const val URL_GLIDE = "https://github.com/bumptech/glide/archive/master.zip"
        private const val URL_RETROFIT = "https://github.com/square/retrofit/archive/master.zip"
        private const val URL_CURRENT_REPO =
            "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip"
    }

}
