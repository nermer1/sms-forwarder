package com.example.myapplication

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object SendUtil {
    fun send(webhookUrl: String, sender: String, message: String, onResult: (Boolean) -> Unit = {}) {
        Thread {
            try {
                // 1. URL 공백 제거 (실수 방지)
                val cleanUrl = webhookUrl.trim()

                val url = URL(cleanUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                // 2. ★ JSON 생성 (JSONObject 사용으로 특수문자/줄바꿈 자동 처리)
                val jsonObject = JSONObject()
                // 슬랙 포맷에 맞게 텍스트 조합
                val finalMessage = "[문자 수신]\nFrom: $sender\nContent: $message"
                jsonObject.put("text", finalMessage)

                val jsonPayload = jsonObject.toString()

                // 전송
                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(jsonPayload)
                writer.flush()
                writer.close()

                // 3. ★ 서버 응답 내용(Body) 확인하기
                val responseCode = conn.responseCode

                // 응답 본문 읽기 (성공이든 실패든 이유를 들어보자)
                val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
                val reader = BufferedReader(InputStreamReader(stream))
                val responseBody = reader.readText()
                reader.close()

                Log.d("SlackUtil", "전송 코드: $responseCode / 응답 본문: $responseBody")

                // 슬랙은 성공하면 body에 "ok"라고 줍니다.
                if (responseCode == 200 && responseBody == "ok") {
                    onResult(true)
                } else {
                    // 200인데 내용이 이상하거나, 400/500 에러인 경우
                    Log.e("SlackUtil", "실패 원인: $responseBody")
                    onResult(false)
                }

            } catch (e: Exception) {
                Log.e("SlackUtil", "에러 발생: ${e.message}")
                e.printStackTrace()
                onResult(false)
            }
        }.start()
    }
}