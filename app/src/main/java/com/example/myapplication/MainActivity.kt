package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
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
        checkNotificationServicePermission()
        requestIgnoreBatteryOptimizations()
        
        ForegroundService.startService(this)

        val etUrl = findViewById<EditText>(R.id.etWebhookUrl)
        val etHeaders = findViewById<EditText>(R.id.etHeaders)
        val etTargetSms = findViewById<EditText>(R.id.etTargetSms)
        val etSmsSenderFilter = findViewById<EditText>(R.id.etSmsSenderFilter)
        val etSmsKeywordFilter = findViewById<EditText>(R.id.etSmsKeywordFilter)
        val etAppWhitelist = findViewById<EditText>(R.id.etAppWhitelist)
        
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnTest = findViewById<Button>(R.id.btnTest)
        val tvLog = findViewById<TextView>(R.id.tvLog)

        val sharedPref = getSharedPreferences("SmsPrefs", Context.MODE_PRIVATE)
        
        etUrl.setText(sharedPref.getString("webhook_url", ""))
        etHeaders.setText(sharedPref.getString("webhook_headers", ""))
        etTargetSms.setText(sharedPref.getString("target_sms", ""))
        etSmsSenderFilter.setText(sharedPref.getString("sms_sender_filter", ""))
        etSmsKeywordFilter.setText(sharedPref.getString("sms_keyword_filter", ""))
        etAppWhitelist.setText(sharedPref.getString("app_whitelist", "com.kakao.talk"))

        btnSave.setOnClickListener {
            sharedPref.edit().apply {
                putString("webhook_url", etUrl.text.toString())
                putString("webhook_headers", etHeaders.text.toString())
                putString("target_sms", etTargetSms.text.toString())
                putString("sms_sender_filter", etSmsSenderFilter.text.toString())
                putString("sms_keyword_filter", etSmsKeywordFilter.text.toString())
                putString("app_whitelist", etAppWhitelist.text.toString())
                apply()
            }
            Toast.makeText(this, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            tvLog.text = "설정 저장 완료"
        }

        btnTest.setOnClickListener {
            val targetUrl = etUrl.text.toString()
            val targetSms = etTargetSms.text.toString()
            
            if (targetUrl.isEmpty() && targetSms.isEmpty()) {
                Toast.makeText(this, "URL이나 수신 번호 중 하나는 입력해야 합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            tvLog.text = "[통합 테스트 시작]\n- 웹훅: ${if(targetUrl.isEmpty()) "미설정" else targetUrl}\n- SMS: ${if(targetSms.isEmpty()) "미설정" else targetSms}"
            
            // 엔진 실행 시 결과 콜백 전달
            ForwardingEngine.process(this, "테스트발신자", "이것은 통합 테스트 본문입니다.", true) { resultMsg ->
                // UI 스레드에서 로그 업데이트
                runOnUiThread {
                    tvLog.append("\n$resultMsg")
                }
            }
            
            Toast.makeText(this, "전송 프로세스 시작됨", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermission() {
        val requiredPermissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS
        )
        val isAnyPermissionDenied = requiredPermissions.any { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (isAnyPermissionDenied) {
            ActivityCompat.requestPermissions(this, requiredPermissions, 101)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "권한 허용됨.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkNotificationServicePermission() {
        if (!isNotificationServiceEnabled()) {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            Toast.makeText(this, "알림 접근 권한을 허용해주세요.", Toast.LENGTH_LONG).show()
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (flat != null && flat.isNotEmpty()) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val componentName = android.content.ComponentName.unflattenFromString(name)
                if (componentName != null && pkgName == componentName.packageName) return true
            }
        }
        return false
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}