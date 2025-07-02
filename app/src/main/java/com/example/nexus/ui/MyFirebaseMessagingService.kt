package com.example.nexus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SimpleFCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")

        // Lưu token để gửi lên server sau
        getSharedPreferences("nexus_prefs", MODE_PRIVATE).edit()
            .putString("fcm_token", token)
            .apply()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Lấy dữ liệu từ notification
        val title = message.notification?.title ?: message.data["title"] ?: "Thông báo"
        val body = message.notification?.body ?: message.data["body"] ?: ""

        // Hiển thị notification
        showNotification(title, body)
    }

    @OptIn(UnstableApi::class)
    private fun showNotification(title: String, body: String) {
        val channelId = "nexus_channel"
        val notificationId = System.currentTimeMillis().toInt()

        // Tạo intent để mở app
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tạo notification manager
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Tạo channel cho Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nexus Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Build notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Dùng icon default
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Show notification
        notificationManager.notify(notificationId, notification)
    }
}