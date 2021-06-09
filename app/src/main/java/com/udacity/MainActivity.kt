package com.udacity

import android.app.DownloadManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.udacity.databinding.ActivityMainBinding
import com.udacity.notification.createChannel
import com.udacity.notification.sendNotification

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

        binding.contentMain.customButton.setOnClickListener {
            download(
                when (binding.contentMain.radioGroup.checkedRadioButtonId) {
                    R.id.radio_glide -> URL_GLIDE
                    R.id.radio_current_repo -> URL_CURRENT_REPO
                    R.id.radio_retrofit -> URL_RETROFIT
                    else -> {
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

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID == id) {
                Toast.makeText(
                    this@MainActivity,
                    "Download Completed",
                    Toast.LENGTH_SHORT
                ).show()

                // Query filename
                val extras = intent.extras
                val q = DownloadManager.Query()
                q.setFilterById(extras!!.getLong(DownloadManager.EXTRA_DOWNLOAD_ID))
                val c: Cursor = downloadManager.query(q)
                var filename = ""
                var filePath = ""
                if (c.moveToFirst()) {
                    val status: Int = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        filePath =
                            c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                        filename =
                            filePath.substring(filePath.lastIndexOf('/') + 1, filePath.length)
                    }
                }
                c.close()

                notificationManager.sendNotification(
                    applicationContext,
                    "File $filename is downloaded.",
                    filename,
                    filePath,
                )

                binding.contentMain.customButton.done()
            }
        }
    }

    private fun download(url: String) {
        val request =
            DownloadManager.Request(Uri.parse(url))
                .setTitle(getString(R.string.app_name))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDescription(getString(R.string.app_description))
                .setRequiresCharging(false)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

        downloadID =
            downloadManager.enqueue(request)// enqueue puts the download request in the queue.

        DownloadProgress(downloadID, downloadManager) { progress ->
            runOnUiThread {
                binding.contentMain.customButton.setProgress(progress.toFloat())
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    companion object {
        private const val URL_GLIDE = "https://github.com/bumptech/glide/archive/master.zip"
        private const val URL_RETROFIT = "https://github.com/square/retrofit/archive/master.zip"
        private const val URL_CURRENT_REPO =
            "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip"
    }

}
