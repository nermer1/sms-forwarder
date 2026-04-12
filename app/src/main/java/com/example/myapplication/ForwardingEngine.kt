package com.example.myapplication

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import com.example.myapplication.log.LogDbHelper
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

enum class TargetType { SLACK, WEBHOOK, SMS }

data class ForwardTarget(
    val type: TargetType,
    var destination: String
)
object ForwardingEngine {
    // ★ 설정된 타겟 리스트 (나중에는 SharedPreferences에서 불러오게 구현 필요)
    // 지금은 하드코딩으로 예시를 듭니다.
    private fun getTargetList(): List<ForwardTarget> {
        return listOf(
            ForwardTarget(TargetType.SLACK, "https://hooks.slack.com/services/AAAA/BBBB/CCCC"),
            ForwardTarget(TargetType.WEBHOOK, "https://my-company-server.com/api/sms"),
            ForwardTarget(TargetType.SMS, "010-9999-8888") // 세컨폰 번호
        )
    }

    fun process(context: Context, sender: String, body: String) {
        val dbHelper = LogDbHelper(context)
        val targets = getTargetList() // 저장된 타겟 불러오기

        // 1. 오래된 로그 정리 (예: 7일 지난거 삭제) - 실행될 때마다 체크
        dbHelper.deleteOldLogs(7)

        // 2. 타겟별 전송 시작
        targets.forEach { target ->
            Thread {
                var success = false
                try {
                    when (target.type) {
                        TargetType.SLACK -> success = sendToSlack(target.destination, sender, body)
                        TargetType.WEBHOOK -> success = sendToWebhook(target.destination, sender, body)
                        TargetType.SMS -> success = sendSms(target.destination, body)
                    }
                } catch (e: Exception) {
                    Log.e("ForwardEngine", "전송 실패: ${e.message}")
                    success = false
                } finally {
                    // 3. 결과 DB 저장
                    dbHelper.addLog(target.type.name, target.destination, body, success)
                    Log.d("ForwardEngine", "${target.type} 전송 결과: $success")
                }
            }.start()
        }
    }

    // --- 개별 전송 로직 ---

    // 1. 슬랙 전송
    private fun sendToSlack(urlStr: String, sender: String, msg: String): Boolean {
        return try {
            val json = JSONObject().apply {
                put("text", "[문자 수신] $sender\n$msg")
            }
            postJson(urlStr, json.toString())
        } catch (e: Exception) { false }
    }

    // 2. 일반 웹훅 전송 (JSON 포맷은 서버 스펙에 맞게 수정)
    private fun sendToWebhook(urlStr: String, sender: String, msg: String): Boolean {
        return try {
            val json = JSONObject().apply {
                put("sender_phone", sender)
                put("content", msg)
                put("received_at", System.currentTimeMillis())
            }
            postJson(urlStr, json.toString())
        } catch (e: Exception) { false }
    }

    // 3. SMS 재전송 (다른 폰으로)
    private fun sendSms(phoneNumber: String, msg: String): Boolean {
        return try {
            val smsManager = SmsManager.getDefault()
            // 긴 문자는 쪼개서 보냄
            val parts = smsManager.divideMessage("[$msg]") // [전달] 표시
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // HTTP POST 공통 함수
    private fun postJson(urlStr: String, jsonBody: String): Boolean {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        OutputStreamWriter(conn.outputStream).use { it.write(jsonBody) }

        return conn.responseCode in 200..299
    }
}