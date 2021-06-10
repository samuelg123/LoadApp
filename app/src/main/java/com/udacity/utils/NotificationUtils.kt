package com.udacity.utils
/*
 * Copyright (C) 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import android.graphics.Color
import android.os.Build
import com.udacity.DetailActivity
import com.udacity.MainActivity
import com.udacity.R

// Notification ID.
private const val NOTIFICATION_ID = 0

/**
 * Builds and delivers the notification.
 *
 * @param messageBody, notification text.
 * @param context, activity context.
 */
fun NotificationManager.sendNotification(
    applicationContext: Context,
    downloadedTitle: String,
    downloadedFilePath: String,
    success: Boolean
) {

    //To Home / MainActivity
    val homeIntent = Intent(applicationContext, MainActivity::class.java)
    val homePendingIntent = PendingIntent.getActivity(
        applicationContext,
        NOTIFICATION_ID,
        homeIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    //To Content / DetailActivity
    val contentIntent = Intent(applicationContext, DetailActivity::class.java).apply {
        putExtra(DetailActivity.DOWNLOADED_TITLE, downloadedTitle)
        putExtra(DetailActivity.DOWNLOADED_FILEPATH, downloadedFilePath)
        putExtra(DetailActivity.DOWNLOAD_SUCCESS, success)
    }
    val contentPendingIntent = PendingIntent.getActivity(
        applicationContext,
        NOTIFICATION_ID,
        contentIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    val downloadDoneImage =
        ResourceUtils.getBitmap(
            applicationContext,
            if (success) R.drawable.ic_baseline_cloud_done_24 else R.drawable.ic_baseline_cloud_done_red_24
        )
    // BitmapFactory will not work on vector drawable
//        BitmapFactory.decodeResource(
//        applicationContext.resources,
//        R.drawable.ic_baseline_cloud_done_24
//    )

    // Build the notification
    val builder = NotificationCompat.Builder(
        applicationContext,
        applicationContext.getString(R.string.download_notification_channel_id)
    )
        .setSmallIcon(R.drawable.ic_assistant_black_24dp)
        .setLargeIcon(downloadDoneImage)
        .setContentTitle(applicationContext.getString(R.string.notification_title))
        .setContentText(
            if (success)
                applicationContext.getString(R.string.notification_description)
            else
                applicationContext.getString(R.string.download_failed)
        )
        .setContentIntent(homePendingIntent)
        .addAction(
            R.drawable.ic_baseline_folder_open_24,
            applicationContext.getString(R.string.notification_button),
            contentPendingIntent
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    // Deliver the notification
    notify(NOTIFICATION_ID, builder.build())
}

fun NotificationManager.cancelNotifications() {
    cancelAll()
}

fun Context.createChannel(channelId: String, channelName: String, channelDesc: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Create channel to show notifications.
        val notificationChannel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        )
            .apply {
                setShowBadge(false)
            }

        notificationChannel.enableLights(true)
        notificationChannel.lightColor = Color.RED
        notificationChannel.enableVibration(true)
        notificationChannel.description = channelDesc

        val notificationManager = getSystemService(
            NotificationManager::class.java
        )

        notificationManager.createNotificationChannel(notificationChannel)

    }
}
