package com.example.myapplication

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val subText = extras.getCharSequence("android.subText")?.toString() ?: ""

        val sharedPref = applicationContext.getSharedPreferences("SmsPrefs", Context.MODE_PRIVATE)
        val appWhitelistStr = sharedPref.getString("app_whitelist", "com.kakao.talk") ?: "com.kakao.talk"
        val whiteList = appWhitelistStr.split(",").map { it.trim() }
        
        if (whiteList.contains(packageName)) {
            Log.d("NotificationService", "알림 수신: [$packageName] $title : $text ($subText)")
            ForwardingEngine.process(applicationContext, "알림($title)", text)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // 알림이 제거될 때 필요한 로직이 있다면 작성
    }
}