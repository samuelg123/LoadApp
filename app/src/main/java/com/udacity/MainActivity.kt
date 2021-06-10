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
import com.udacity.notification.createChannel
import com.udacity.notification.sendNotification
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

    private fun setSelectionEnabled(enabled: Boolean){
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

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID == id) {
                // Query filename
                val q = DownloadManager.Query().apply {
                    setFilterById(id)
                }
                val c: Cursor = downloadManager.query(q)
                var filename = ""
                var filePath = ""
                try {
                    val res = c.moveToFirst()
                    if (res) {
                        val status: Int = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            filePath =
                                c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                            filename =
                                filePath.substring(filePath.lastIndexOf('/') + 1, filePath.length)

                            Toast.makeText(
                                this@MainActivity,
                                "Download Completed",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON))
                            Log.d(TAG, "Download not correct, status [$status] reason [$reason]")
                            Toast.makeText(
                                this@MainActivity,
                                "Download Failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        notificationManager.sendNotification(
                            applicationContext,
                            fileTitle ?: filename,
                            filePath,
                            true
                        )
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Exception!! $e")
                }
                c.close()

                binding.contentMain.run {
                    radioGroup.isEnabled = true
                    customButton.done()
                }
            }
        }
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
            var status: Int?
            try {
                while (isActive) {
                    val q = DownloadManager.Query().apply {
                        setFilterById(downloadID)
                    }
                    val cursor: Cursor = query(q)
                    cursor.moveToFirst()
                    val bytesDownloaded =
                        cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal =
                        cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))

                    Log.d(TAG, "download: Status: $status")
                    withContext(Dispatchers.Main) {
                        if (bytesTotal >= 0) {
                            binding.contentMain.customButton.setProgress((bytesDownloaded.toFloat() / bytesTotal.toFloat()))
                        } else {
                            binding.contentMain.customButton.setIndeterminateProgress()
                        }
                    }
//                    var filePath: String
//                    var filename: String
//                    when (status) {
//                        DownloadManager.STATUS_SUCCESSFUL or DownloadManager.STATUS_FAILED -> {
//                            filePath =
//                                cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
//                            filename =
//                                filePath.substring(filePath.lastIndexOf('/') + 1, filePath.length)
//
//
//                            when (status) {
//                                DownloadManager.STATUS_SUCCESSFUL -> {
//                                    Toast.makeText(
//                                        this@MainActivity,
//                                        "Download Completed",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
//
//                                    notificationManager.sendNotification(
//                                        applicationContext,
//                                        fileTitle ?: filename,
//                                        filePath,
//                                        success = true
//                                    )
//                                }
//                                DownloadManager.STATUS_FAILED -> {
//                                    val reason =
//                                        cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
//                                    Log.d(TAG, "Download not correct, status [$status] reason [$reason]")
//                                    Toast.makeText(
//                                        this@MainActivity,
//                                        "Download Failed",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
//
//                                    notificationManager.sendNotification(
//                                        applicationContext,
//                                        fileTitle ?: filename,
//                                        filePath,
//                                        success = false
//                                    )
//                                }
//                            }
//                        }
//                    }
                    cursor.close()

                    if (status == DownloadManager.STATUS_SUCCESSFUL ||
                        status == DownloadManager.STATUS_FAILED
                    ) {
                        break
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
        super.onDestroy()
        unregisterReceiver(receiver)
        coroutineScope.cancel()
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
