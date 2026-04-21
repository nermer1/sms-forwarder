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
            val pendingResult = goAsync() // 비동기 작업 시작 알림
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages != null && messages.isNotEmpty()) {
                val sender = messages[0].originatingAddress ?: "Unknown"
                val fullBody = StringBuilder()
                messages.forEach { sms ->
                    fullBody.append(sms.messageBody ?: "")
                }
                
                // 별도 스레드에서 처리 후 finish() 호출
                Thread {
                    try {
                        ForwardingEngine.process(context, sender, fullBody.toString())
                    } finally {
                        pendingResult.finish()
                    }
                }.start()
            } else {
                pendingResult.finish()
            }
        }
    }
}