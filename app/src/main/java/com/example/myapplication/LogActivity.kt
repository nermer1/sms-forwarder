package com.example.myapplication

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.log.LogDbHelper

class LogActivity : AppCompatActivity() {

    private lateinit var logAdapter: LogAdapter
    private lateinit var dbHelper: LogDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "전송 로그"

        dbHelper = LogDbHelper(this)
        val rvLogs = findViewById<RecyclerView>(R.id.rvLogs)
        rvLogs.layoutManager = LinearLayoutManager(this)
        
        logAdapter = LogAdapter(dbHelper.getAllLogs())
        rvLogs.adapter = logAdapter

        findViewById<Button>(R.id.btnClearLogs).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("로그 삭제")
                .setMessage("모든 전송 로그를 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    dbHelper.clearAllLogs()
                    logAdapter.updateLogs(emptyList())
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
