package com.example.myapplication

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import com.example.myapplication.log.LogDbHelper
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ForwardingEngine {

    fun process(
        context: Context, 
        sender: String, 
        body: String, 
        isTest: Boolean = false,
        onResult: ((String) -> Unit)? = null
    ) {
        val sharedPref = context.getSharedPreferences("SmsPrefs", Context.MODE_PRIVATE)
        
        // 1. 필터링 체크
        if (!isTest) {
            val senderFilter = sharedPref.getString("sms_sender_filter", "") ?: ""
            val keywordFilter = sharedPref.getString("sms_keyword_filter", "") ?: ""

            if (senderFilter.isNotEmpty() && !senderFilter.split(",").any { sender.contains(it.trim()) }) return 
            if (keywordFilter.isNotEmpty() && !keywordFilter.split(",").any { body.contains(it.trim()) }) return
        }

        val dbHelper = LogDbHelper(context)
        dbHelper.deleteOldLogs(7)

        // 2. Webhook 전송
        val webhookUrl = sharedPref.getString("webhook_url", "") ?: ""
        val headersStr = sharedPref.getString("webhook_headers", "") ?: ""
        
        if (webhookUrl.isNotEmpty()) {
            Thread {
                val result = sendToWebhook(webhookUrl, headersStr, sender, body)
                val success = result.first
                val message = result.second
                dbHelper.addLog("WEBHOOK", webhookUrl, body, success)
                onResult?.invoke("🌐 웹훅: ${if(success) "✅ 성공" else "❌ 실패 ($message)"}")
            }.start()
        }

        // 3. SMS 릴레이 전송
        val targetSms = sharedPref.getString("target_sms", "") ?: ""
        if (targetSms.isNotEmpty()) {
            Thread {
                val success = sendSms(context, targetSms, "[$sender]\n$body")
                dbHelper.addLog("SMS", targetSms, body, success)
                onResult?.invoke("📱 SMS: ${if(success) "✅ 성공" else "❌ 실패"}")
            }.start()
        }
    }

    private fun sendToWebhook(urlStr: String, headersStr: String, sender: String, msg: String): Pair<Boolean, String> {
        return try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/json")
            
            if (headersStr.isNotEmpty()) {
                headersStr.split("\n").forEach { line ->
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) conn.setRequestProperty(parts[0].trim(), parts[1].trim())
                }
            }

            conn.doOutput = true
            val json = JSONObject().apply {
                // 일반 서버용 데이터
                put("sender", sender)
                put("content", msg)
                // 슬랙 호환용 text 필드 추가
                put("text", "[SMS 전달] $sender\n$msg") 
            }

            OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
            val code = conn.responseCode
            if (code in 200..299) Pair(true, "OK") else Pair(false, "HTTP $code")
        } catch (e: Exception) {
            Log.e("ForwardingEngine", "Webhook Error: ${e.message}")
            Pair(false, e.message ?: "Unknown Error")
        }
    }

    private fun sendSms(context: Context, phoneNumber: String, msg: String): Boolean {
        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(msg)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            true
        } catch (e: Exception) {
            Log.e("ForwardingEngine", "SMS Error: ${e.message}")
            false
        }
    }
}