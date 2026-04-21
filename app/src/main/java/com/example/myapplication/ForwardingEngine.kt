package com.example.myapplication

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import com.example.myapplication.log.LogDbHelper
import com.example.myapplication.models.ForwardTarget
import com.example.myapplication.models.ForwardingRule
import com.example.myapplication.models.TargetType
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ForwardingEngine {

    fun process(
        context: Context, 
        sender: String, 
        body: String, 
        packageName: String? = null,
        onResult: ((String) -> Unit)? = null
    ) {
        val rules = RuleManager.getRules(context)
        // dbHelper.deleteOldLogs(7) 호출이 process 시작할 때마다 있으면 
        // 의도치 않게 로그가 삭제될 가능성이 있으므로 주석 처리하거나 로직 확인 필요
        // val dbHelper = LogDbHelper(context)
        // dbHelper.deleteOldLogs(7)

        rules.filter { it.isEnabled }.forEach { rule ->
            if (isMatched(rule, sender, body, packageName)) {
                testRule(context, rule, sender, body, onResult)
            }
        }
    }

    fun testRule(
        context: Context,
        rule: ForwardingRule,
        sender: String,
        body: String,
        onResult: ((String) -> Unit)? = null
    ) {
        val dbHelper = LogDbHelper(context)
        rule.targets.forEach { target ->
            executeTarget(context, rule.id, target, sender, body, dbHelper, onResult)
        }
    }

    private fun isMatched(rule: ForwardingRule, sender: String, body: String, packageName: String?): Boolean {
        // 소스 체크 (SMS/Notification 선택 여부)
        if (packageName == null) {
            if (!rule.isSmsEnabled) return false
        } else {
            if (!rule.isNotificationEnabled) return false
            // 알림인 경우 앱 패키지 필터 추가 체크
            if (rule.targetAppPackages.isNotEmpty() && !rule.targetAppPackages.split(",").any { packageName.contains(it.trim()) }) return false
        }

        // 공통 발신자 필터 체크 (SMS 번호 또는 알림 제목)
        if (rule.senderFilter.isNotEmpty()) {
            val filterParts = rule.senderFilter.split(",")
            val matched = if (packageName == null) {
                // SMS인 경우: 숫자만 추출하여 비교 (하이픈 등 무시)
                val normalizedSender = sender.filter { it.isDigit() }
                filterParts.any { part ->
                    val normalizedPart = part.trim().filter { it.isDigit() }
                    normalizedPart.isNotEmpty() && normalizedSender.contains(normalizedPart)
                }
            } else {
                // 알림인 경우: 제목 텍스트 포함 여부 체크
                filterParts.any { sender.contains(it.trim()) }
            }
            if (!matched) return false
        }

        // 공통 키워드 필터 체크 (내용)
        if (rule.keywordFilter.isNotEmpty() && !rule.keywordFilter.split(",").any { body.contains(it.trim()) }) return false

        return true
    }

    private fun executeTarget(
        context: Context,
        ruleId: String,
        target: ForwardTarget,
        sender: String,
        body: String,
        dbHelper: LogDbHelper,
        onResult: ((String) -> Unit)?
    ) {
        Thread {
            try {
                when (target.type) {
                    TargetType.SLACK -> {
                        val result = sendToSlack(target.destination, sender, body)
                        dbHelper.addLog(ruleId, "SLACK", target.destination, body, result.first)
                        onResult?.invoke("💬 [SLACK] ${if(result.first) "✅" else "❌"} ${result.second}")
                    }
                    TargetType.API -> {
                        val result = sendToApi(target, sender, body)
                        dbHelper.addLog(ruleId, "API", target.destination, body, result.first)
                        onResult?.invoke("🔌 [API] ${if(result.first) "✅" else "❌"} ${result.second}")
                    }
                    TargetType.SMS -> {
                        Log.d("ForwardingEngine", "Sending SMS to: ${target.destination}")
                        val success = sendSms(context, target.destination, "[$sender]\n$body")
                        dbHelper.addLog(ruleId, "SMS", target.destination, body, success)
                        onResult?.invoke("📱 [SMS] to ${target.destination}: ${if(success) "✅" else "❌"}")
                    }
                }
            } catch (e: Exception) {
                onResult?.invoke("⚠️ Error: ${e.message}")
            }
        }.start()
    }

    private fun sendToSlack(urlStr: String, sender: String, msg: String): Pair<Boolean, String> {
        return try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            
            val json = JSONObject().apply {
                put("text", "[SMS/알림 전달]\n*보낸이*: $sender\n*내용*: $msg")
            }

            OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
            val code = conn.responseCode
            if (code in 200..299) Pair(true, "OK") else Pair(false, "HTTP $code")
        } catch (e: Exception) {
            Pair(false, e.message ?: "Error")
        }
    }

    private fun sendToApi(target: ForwardTarget, sender: String, msg: String): Pair<Boolean, String> {
        return try {
            val conn = URL(target.destination).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            conn.doOutput = true
            
            // 1. Headers 설정
            target.headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (!target.headers.containsKey("Content-Type")) {
                conn.setRequestProperty("Content-Type", "application/json")
            }

            // 2. Body 데이터 치환 및 JSON 생성
            val json = JSONObject()
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            
            target.bodyMap.forEach { (k, v) ->
                val replacedValue = v.replace("{{sender}}", sender)
                    .replace("{{content}}", msg)
                    .replace("{{title}}", "SMS Forwarder") // 제목 필드 (추후 확장 가능)
                    .replace("{{date}}", dateStr)
                json.put(k, replacedValue)
            }

            OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
            val code = conn.responseCode
            if (code in 200..299) Pair(true, "OK") else Pair(false, "HTTP $code")
        } catch (e: Exception) {
            Pair(false, e.message ?: "Error")
        }
    }

    private fun sendSms(context: Context, phoneNumber: String, msg: String): Boolean {
        return try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)!!
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(msg)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }
}