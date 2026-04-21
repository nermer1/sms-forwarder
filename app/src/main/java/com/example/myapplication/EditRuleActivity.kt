package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.models.ForwardTarget
import com.example.myapplication.models.ForwardingRule
import com.example.myapplication.models.TargetType
import com.google.android.material.button.MaterialButton

class EditRuleActivity : AppCompatActivity() {

    private lateinit var targetContainer: LinearLayout
    private var currentRuleId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_rule)

        targetContainer = findViewById(R.id.targetContainer)
        val etRuleName = findViewById<EditText>(R.id.etRuleName)
        val etSenderFilter = findViewById<EditText>(R.id.etSenderFilter)
        val etKeywordFilter = findViewById<EditText>(R.id.etKeywordFilter)
        val etAppWhitelist = findViewById<EditText>(R.id.etAppWhitelist)
        
        val rgSource = findViewById<android.widget.RadioGroup>(R.id.rgSource)
        val rbSms = findViewById<android.widget.RadioButton>(R.id.rbSms)
        val rbNotification = findViewById<android.widget.RadioButton>(R.id.rbNotification)
        
        val layoutAppFilter = findViewById<View>(R.id.layoutAppFilter)

        val layoutSenderFilter = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.layoutSenderFilter)

        rgSource.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbSms -> {
                    layoutAppFilter.visibility = View.GONE
                    layoutSenderFilter.hint = "발신 번호 필터 (쉼표 구분)"
                }
                R.id.rbNotification -> {
                    layoutAppFilter.visibility = View.VISIBLE
                    layoutSenderFilter.hint = "발신자(제목) 필터 (쉼표 구분)"
                }
            }
        }

        currentRuleId = intent.getStringExtra("RULE_ID")
        if (currentRuleId != null) {
            val rules = RuleManager.getRules(this)
            rules.find { it.id == currentRuleId }?.let { rule ->
                etRuleName.setText(rule.name ?: "")
                etSenderFilter.setText(rule.senderFilter ?: "")
                etKeywordFilter.setText(rule.keywordFilter ?: "")
                etAppWhitelist.setText(rule.targetAppPackages ?: "")
                
                if (rule.isNotificationEnabled && !rule.isSmsEnabled) {
                    rbNotification.isChecked = true
                } else {
                    rbSms.isChecked = true
                }
                rule.targets?.forEach { addTargetView(it) }
                
                findViewById<View>(R.id.btnDeleteRule).apply {
                    visibility = View.VISIBLE
                    setOnClickListener { deleteRule() }
                }
            }
        }

        findViewById<MaterialButton>(R.id.btnAddWebhook).setOnClickListener {
            addTargetView(ForwardTarget(type = TargetType.SLACK))
        }

        findViewById<MaterialButton>(R.id.btnAddApi).setOnClickListener {
            addTargetView(ForwardTarget(type = TargetType.API))
        }

        findViewById<MaterialButton>(R.id.btnAddSms).setOnClickListener {
            addTargetView(ForwardTarget(type = TargetType.SMS))
        }

        findViewById<MaterialButton>(R.id.btnSaveRule).setOnClickListener { saveRule() }
        findViewById<MaterialButton>(R.id.btnTestRule).setOnClickListener { testCurrentSettings() }
        
        findViewById<View>(R.id.btnSelectApps).setOnClickListener {
            showAppSelectionDialog()
        }
    }

    private fun showAppSelectionDialog() {
        val pm = packageManager
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
        mainIntent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        
        val resolvedInfos = pm.queryIntentActivities(mainIntent, 0)
        val appList = resolvedInfos.map { 
            val pkg = it.activityInfo.packageName
            com.example.myapplication.models.AppInfo(
                appName = it.loadLabel(pm).toString(),
                packageName = pkg,
                icon = it.loadIcon(pm),
                isSelected = findViewById<EditText>(R.id.etAppWhitelist).text.toString().contains(pkg)
            )
        }.sortedBy { it.appName }

        val adapter = AppListAdapter(appList)
        val recyclerView = androidx.recyclerview.widget.RecyclerView(this).apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@EditRuleActivity)
            this.adapter = adapter
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("대상 앱 선택")
            .setView(recyclerView)
            .setPositiveButton("선택 완료") { _, _ ->
                findViewById<EditText>(R.id.etAppWhitelist).setText(adapter.getSelectedPackages())
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun addTargetView(target: ForwardTarget) {
        val layoutRes = when(target.type) {
            TargetType.SLACK -> R.layout.item_target_webhook // 기존꺼 재활용 (URL만 있음)
            TargetType.API -> R.layout.item_target_api
            TargetType.SMS -> R.layout.item_target_sms
        }
        val view = LayoutInflater.from(this).inflate(layoutRes, targetContainer, false)
        view.tag = target.type

        when(target.type) {
            TargetType.SLACK -> {
                view.findViewById<EditText>(R.id.etTargetUrl).setText(target.destination)
            }
            TargetType.SMS -> {
                view.findViewById<EditText>(R.id.etTargetSms).setText(target.destination)
            }
            TargetType.API -> {
                view.findViewById<EditText>(R.id.etTargetUrl).setText(target.destination)
                val hContainer = view.findViewById<LinearLayout>(R.id.headerContainer)
                val bContainer = view.findViewById<LinearLayout>(R.id.bodyContainer)
                
                target.headers.forEach { (k, v) -> addKvView(hContainer, k, v) }
                target.bodyMap.forEach { (k, v) -> addKvView(bContainer, k, v) }

                view.findViewById<View>(R.id.btnAddHeader).setOnClickListener { addKvView(hContainer, "", "") }
                view.findViewById<View>(R.id.btnAddBody).setOnClickListener { addKvView(bContainer, "", "") }

                // 프리셋 설정
                val spinner = view.findViewById<android.widget.Spinner>(R.id.spinnerPreset)
                val presets = listOf("직접 입력", "슬랙 헬퍼 프리셋")
                val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, presets)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
                
                spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, position: Int, id: Long) {
                        if (position == 1) { //슬랙 헬퍼
                            view.findViewById<EditText>(R.id.etTargetUrl).setText("https://unidocu.unipost.co.kr/helper/api/v1/integrations/slack/notify")
                            hContainer.removeAllViews()
                            addKvView(hContainer, "x-api-key", "헬퍼에서 발급 받은 api 토큰 입력")
                            bContainer.removeAllViews()
                            addKvView(bContainer, "message", "{{content}}")
                            addKvView(bContainer, "from", "{{sender}}")
                        }
                    }
                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                }
            }
        }

        view.findViewById<View>(R.id.btnRemoveTarget).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("타겟 삭제")
                .setMessage("이 전달 타겟을 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    targetContainer.removeView(view)
                }
                .setNegativeButton("취소", null)
                .show()
        }
        targetContainer.addView(view)
    }

    private fun addKvView(container: LinearLayout, key: String, value: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_key_value_input, container, false)
        view.findViewById<EditText>(R.id.etKey).setText(key)
        view.findViewById<EditText>(R.id.etValue).setText(value)
        view.findViewById<View>(R.id.btnRemoveKv).setOnClickListener { container.removeView(view) }
        container.addView(view)
    }

    private fun saveRule() {
        val rule = collectRuleData() ?: return
        if (currentRuleId == null) RuleManager.addRule(this, rule)
        else RuleManager.updateRule(this, rule)
        finish()
    }

    private fun testCurrentSettings() {
        val rule = collectRuleData() ?: return
        ForwardingEngine.testRule(this, rule, "010-1234-5678", "테스트 메시지입니다.") { 
            runOnUiThread { android.widget.Toast.makeText(this, it, android.widget.Toast.LENGTH_SHORT).show() }
        }
    }

    private fun collectRuleData(): ForwardingRule? {
        val name = findViewById<EditText>(R.id.etRuleName).text.toString()
        if (name.isEmpty()) return null

        val targets = mutableListOf<ForwardTarget>()
        for (i in 0 until targetContainer.childCount) {
            val view = targetContainer.getChildAt(i)
            val type = view.tag as TargetType
            val target = ForwardTarget(type = type)

            when(type) {
                TargetType.SLACK -> target.destination = view.findViewById<EditText>(R.id.etTargetUrl).text.toString()
                TargetType.SMS -> target.destination = view.findViewById<EditText>(R.id.etTargetSms).text.toString()
                TargetType.API -> {
                    target.destination = view.findViewById<EditText>(R.id.etTargetUrl).text.toString()
                    target.headers = collectKvData(view.findViewById(R.id.headerContainer))
                    target.bodyMap = collectKvData(view.findViewById(R.id.bodyContainer))
                }
            }
            targets.add(target)
        }

        val rbNotification = findViewById<android.widget.RadioButton>(R.id.rbNotification)

        return ForwardingRule(
            id = currentRuleId ?: java.util.UUID.randomUUID().toString(),
            name = name,
            isSmsEnabled = !rbNotification.isChecked,
            isNotificationEnabled = rbNotification.isChecked,
            senderFilter = findViewById<EditText>(R.id.etSenderFilter).text.toString(),
            keywordFilter = findViewById<EditText>(R.id.etKeywordFilter).text.toString(),
            targetAppPackages = findViewById<EditText>(R.id.etAppWhitelist).text.toString(),
            targets = targets
        )
    }

    private fun deleteRule() {
        currentRuleId?.let { id ->
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("규칙 삭제")
                .setMessage("이 규칙을 삭제하시겠습니까?")
                .setPositiveButton("삭제") { _, _ ->
                    RuleManager.deleteRule(this, id)
                    finish()
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private fun collectKvData(container: LinearLayout): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        for (i in 0 until container.childCount) {
            val v = container.getChildAt(i)
            val key = v.findViewById<EditText>(R.id.etKey).text.toString()
            val value = v.findViewById<EditText>(R.id.etValue).text.toString()
            if (key.isNotEmpty()) map[key] = value
        }
        return map
    }
}