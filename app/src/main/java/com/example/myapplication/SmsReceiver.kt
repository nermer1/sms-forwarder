package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SmsReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages != null && messages.isNotEmpty()) {
                val sender = messages[0].originatingAddress ?: "Unknown"
                val fullBody = StringBuilder()
                messages.forEach { sms ->
                    fullBody.append(sms.messageBody ?: "")
                }
                ForwardingEngine.process(context, sender, fullBody.toString())
            }
        }
    }
}