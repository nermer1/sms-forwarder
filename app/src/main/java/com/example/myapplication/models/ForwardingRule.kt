package com.example.myapplication.models

import java.util.UUID

enum class TargetType { SLACK, API, SMS }

data class ForwardTarget(
    val id: String = UUID.randomUUID().toString(),
    var type: TargetType,
    var destination: String = "",
    var headers: MutableMap<String, String> = mutableMapOf(),
    var bodyMap: MutableMap<String, String> = mutableMapOf()
)

data class ForwardingRule(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var isEnabled: Boolean = true,
    
    // 메시지 소스 설정
    var isSmsEnabled: Boolean = true,
    var isNotificationEnabled: Boolean = false,
    
    // 필터 조건
    var senderFilter: String = "",
    var keywordFilter: String = "",
    var targetAppPackages: String = "", // 알림용 패키지명 (쉼표 구분)
    
    // 전달 대상
    var targets: MutableList<ForwardTarget> = mutableListOf()
)