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

        val fullContent = if (subText.isNotEmpty()) "[$subText] $text" else text

        Log.d("NotificationService", "알림 감지: [$packageName] $title : $fullContent")
        
        // 규칙 기반 프로세싱 호출
        ForwardingEngine.process(
            context = applicationContext,
            sender = title,
            body = fullContent,
            packageName = packageName
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // 알림이 제거될 때 필요한 로직이 있다면 작성
    }
}