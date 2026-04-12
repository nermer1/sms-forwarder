package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        checkPermission()
        /*ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/
        // UI 요소 연결
        val etUrl = findViewById<EditText>(R.id.etWebhookUrl)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnTest = findViewById<Button>(R.id.btnTest)
        val tvLog = findViewById<TextView>(R.id.tvLog)
        // 1. 저장된 URL 불러오기 (SharedPreferences)
        val sharedPref = getSharedPreferences("SmsPrefs", Context.MODE_PRIVATE)
        val savedUrl = sharedPref.getString("webhook_url", "")
        etUrl.setText(savedUrl)

        // 2. 저장 버튼 클릭 이벤트
        btnSave.setOnClickListener {
            val url = etUrl.text.toString()
            if (url.isNotEmpty()) {
                sharedPref.edit().putString("webhook_url", url).apply()
                Toast.makeText(this, "URL이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                tvLog.text = "설정 저장됨.\n" + tvLog.text
            }
        }

        btnTest.setOnClickListener {
            val targetUrl = etUrl.text.toString()

            if (targetUrl.isEmpty()) {
                Toast.makeText(this, "URL을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            tvLog.append("\n[테스트] 발송 시도 중...")

            // ★ 수정된 부분: 유틸 클래스 사용
            // 마지막 인자로 람다식(Callback)을 넘겨서 성공/실패 여부를 받음
            SendUtil.send(targetUrl, "테스트맨", "이것은 테스트입니다.") { isSuccess ->

                // UI 변경은 반드시 메인 스레드(runOnUiThread)에서
                runOnUiThread {
                    if (isSuccess) {
                        tvLog.append("\n[성공] 전송 완료!")
                        Toast.makeText(this, "성공!", Toast.LENGTH_SHORT).show()
                    } else {
                        tvLog.append("\n[실패] 로그캣을 확인하세요.")
                        Toast.makeText(this, "실패...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun checkPermission() {
        // 1. 필요한 권한 목록 정의 (수신 + 발신)
        val requiredPermissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS
        )

        // 2. 권한 중 하나라도 허용되지 않은 게 있는지 확인
        val isAnyPermissionDenied = requiredPermissions.any { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        // 3. 거절된 게 있다면 권한 요청 팝업 띄움
        if (isAnyPermissionDenied) {
            ActivityCompat.requestPermissions(this, requiredPermissions, 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "권한 허용됨. 이제 문자를 전달합니다.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }
}