package com.example.myapplication.log

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LogDbHelper(context: Context) : SQLiteOpenHelper(context, "ForwardLogs.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        // 테이블 생성: ID, 시간, 타입(슬랙/SMS 등), 대상, 내용, 성공여부
        val sql = """
            CREATE TABLE logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER,
                target_type TEXT,
                target_dest TEXT,
                message TEXT,
                is_success INTEGER
            )
        """.trimIndent()
        db.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS logs")
        onCreate(db)
    }

    // 로그 추가
    fun addLog(type: String, dest: String, msg: String, isSuccess: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("timestamp", System.currentTimeMillis())
            put("target_type", type)
            put("target_dest", dest)
            put("message", msg)
            put("is_success", if (isSuccess) 1 else 0)
        }
        db.insert("logs", null, values)
        db.close()
    }

    // ★ 자동 삭제 기능 (days일 지난 로그 삭제)
    fun deleteOldLogs(days: Int) {
        val db = writableDatabase
        // 현재 시간 - (일 * 24시간 * 60분 * 60초 * 1000밀리초)
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        db.delete("logs", "timestamp < ?", arrayOf(cutoffTime.toString()))
        db.close()
    }

    // 로그 전체 삭제 (수동)
    fun clearAllLogs() {
        val db = writableDatabase
        db.delete("logs", null, null)
        db.close()
    }

    fun getAllLogs(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        val db = readableDatabase
        val cursor = db.query("logs", null, null, null, null, null, "timestamp DESC")
        
        while (cursor.moveToNext()) {
            val map = mutableMapOf<String, Any>()
            map["id"] = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            map["timestamp"] = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
            map["target_type"] = cursor.getString(cursor.getColumnIndexOrThrow("target_type"))
            map["target_dest"] = cursor.getString(cursor.getColumnIndexOrThrow("target_dest"))
            map["message"] = cursor.getString(cursor.getColumnIndexOrThrow("message"))
            map["is_success"] = cursor.getInt(cursor.getColumnIndexOrThrow("is_success"))
            list.add(map)
        }
        cursor.close()
        db.close()
        return list
    }
}