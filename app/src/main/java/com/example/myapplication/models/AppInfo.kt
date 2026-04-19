package com.example.myapplication.models

import android.graphics.drawable.Drawable

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    var isSelected: Boolean = false
)