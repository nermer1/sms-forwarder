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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: RuleAdapter
    private lateinit var tvLastLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        checkPermission()
        checkNotificationServicePermission()
        requestIgnoreBatteryOptimizations()
        
        ForegroundService.startService(this)

        tvLastLog = findViewById(R.id.tvLastLog)
        val rvRules = findViewById<RecyclerView>(R.id.rvRules)
        rvRules.layoutManager = LinearLayoutManager(this)
        
        adapter = RuleAdapter(
            rules = RuleManager.getRules(this),
            onRuleClick = { rule ->
                val intent = Intent(this, EditRuleActivity::class.java)
                intent.putExtra("RULE_ID", rule.id)
                startActivity(intent)
            },
            onToggleRule = { rule, isEnabled ->
                rule.isEnabled = isEnabled
                RuleManager.updateRule(this, rule)
                Toast.makeText(this, "${rule.name} ${if(isEnabled) "활성화" else "비활성화"}", Toast.LENGTH_SHORT).show()
            }
        )
        rvRules.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            startActivity(Intent(this, EditRuleActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // 규칙 목록 갱신
        adapter.updateRules(RuleManager.getRules(this))
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

    private fun checkNotificationServicePermission() {
        if (!isNotificationServiceEnabled()) {
            try {
                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                Toast.makeText(this, "알림 접근 권한을 허용해주세요.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                // 일부 기기에서 인텐트 오류 대비
            }
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