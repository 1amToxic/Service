package com.example.cameraxbeta.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.cameraxbeta.MainActivity
import com.example.cameraxbeta.R


const val CHANNEL_ID = "123"
const val NOTIFI_ID = 1

class NotificationActivity : AppCompatActivity() {

    lateinit var fabNotification: ToggleButton
    lateinit var broadcast: MyBroadcaseReceiver
    lateinit var filter: IntentFilter

    companion object {
        private val REQUIRED_PERMISSIONSS = arrayOf(Manifest.permission.WRITE_SETTINGS)
        private val REQUIRED_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)
        if (allPermissionsGranted()) {

        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONSS, REQUIRED_CODE)
        }

        init()
        createNotificationChannel()
        broadcast = MyBroadcaseReceiver(listener = { createNotification(1) })
        filter = IntentFilter("android.intent.action.ACTION_POWER_CONNECTED")
        registerReceiver(broadcast, filter)
        Log.d("AppLog", "ok")
        fabNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startQuickCharge(true)
            } else {
                startQuickCharge(false)
            }
        }

    }


    private fun init() {
        fabNotification = findViewById(R.id.fab_notification)
    }

    private fun startQuickCharge(status: Boolean) {
        turnOffBrightness()
        toggleBluetoothMode(status)
    }

    private fun toggleAirPlaneMode(status: Boolean) {

    }

    private fun turnOffBrightness() {
        Settings.System.putInt(
            this.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS, 10
        );
    }

    private fun toggleOrientation(status: Boolean) {
//        if(status){
//            this.requestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
//        }
//        else{
//            this.requestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR)
//        }
    }

    private fun toggleBluetoothMode(status: Boolean) {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (status) {
            mBluetoothAdapter.disable()
        } else mBluetoothAdapter.enable()

    }

    private fun allPermissionsGranted() = NotificationActivity.REQUIRED_PERMISSIONSS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotification(id: Int) {
        val notifyIntent = Intent(this, NotificationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val notifyPendingIntent = PendingIntent.getActivity(
            this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_camera)
            .setContentTitle("Charging")
            .setContentIntent(notifyPendingIntent)
            .setContentText("This is a nice notification!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFI_ID, builder.build())
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (allPermissionsGranted()) {

        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(this)) {

                } else {
                    startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
                }
            } else {
                TODO(
                    "VERSION.SDK_INT < M")
            }
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcast)
    }
}
