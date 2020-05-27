package com.example.cameraxbeta.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

class MyBroadcaseReceiver(val listener: (Int) -> Unit) : BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent!!.action.equals("android.intent.action.ACTION_POWER_CONNECTED")) {
            val extras = intent.extras
            val state = extras!!.getBoolean("state")
            Log.d("AppLog", "Charge")
            listener.invoke(1)
        }
    }

}