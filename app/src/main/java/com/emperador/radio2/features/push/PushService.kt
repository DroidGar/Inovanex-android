package com.emperador.radio2.features.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.emperador.radio2.MainActivity
import com.emperador.radio2.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class PushService : FirebaseMessagingService() {

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Log.e("onNewToken", "Refreshed token: $token")


    }

    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)

        val title = p0.data["title"]
        val detail = p0.data["detail"]
        val longDetail = p0.data["longDetail"]
        val link = p0.data["link"]
        val image = p0.data["image"]
        //0 abre normal, 1 a la encuesta 2 a los resultados
        val type = p0.data["type"]

        Log.e("image", image)
        if (image != "") {
            val bitmap = Glide.with(this)
                .asBitmap()
                .load(image)
                .submit().get()

            showPush(title, detail, longDetail, bitmap, link)
        } else {
            showPush(title, detail, longDetail, null, link)
        }
    }



    private fun showPush(
        title: String?,
        msg1: String?,
        msg2: String?,
        bitmap: Bitmap?,
        link: String?
    ) {


        createNotificationChannel()
        val notificationIntent: Intent

        if (link != "") {
            notificationIntent = Intent(Intent.ACTION_VIEW)
            notificationIntent.data = Uri.parse(link)
        } else {

            notificationIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)


        val builder = NotificationCompat.Builder(this, "channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(msg1)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (bitmap != null) {
            builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))

        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(msg2))

        }

        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(123, builder.build())
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "channel"
            val descriptionText = "channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("channel", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}