package com.howie.toolforfather

import android.Manifest
import android.content.*
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val KEY_FILTER_WORDS = "filterWords"
    private val KEY_FILTER_PHONES = "filterPhones"
    private val SPLIT_FILTER = Regex(";|；")

    private lateinit var sp: SharedPreferences
    private var debug = false
    private var switch: Boolean = false
    private val receiver: BroadcastReceiver by lazy { SMSBroadcastReceiver() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermissions()
        sp = getSharedPreferences("data", Context.MODE_PRIVATE)

        initView()
        initSave()
        initSwitch()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }
        requestPermissions(
            arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS),
            11111
        )
    }

    private fun initSwitch() {
        enableListener.setOnClickListener {
            if (switch) {
                enableListener.text = "开启监听短信"
                switch = false
                unregisterReceiver(receiver)
            } else {
                RingtonePlayer.init(this)
                enableListener.text = "关闭监听短信"
                switch = true
                registerReceiver(receiver, IntentFilter("android.provider.Telephony.SMS_RECEIVED"))
            }
        }
    }

    private fun initSave() {
        save.setOnClickListener {
            val filterNumber = filterNumbers.text.toString()
            if (filterNumber.isNotBlank()) {
                val split = filterNumber.split(SPLIT_FILTER)
                for (s in split) {
                    if (s.isBlank()) {
                        Toast.makeText(this, "过滤的号码中有空号码，请删除", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                    if (!s.isDigitsOnly()) {
                        Toast.makeText(this, "过滤的号码中只能是数字，请更正", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                }
            }

            val filterWords = filter.text.toString()
            if (filterWords.isNotBlank()) {
                val split = filterWords.split(SPLIT_FILTER)
                for (s in split) {
                    if (s.isBlank()) {
                        Toast.makeText(this, "过滤的词中有空，请删除", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                }
            }

            sp.edit()
                .putString(KEY_FILTER_WORDS, filter.text.toString())
                .putString(KEY_FILTER_PHONES, filterNumber)
                .apply()
            Toast.makeText(this, "配置保存成功", Toast.LENGTH_SHORT).show()
        }
        save.setOnLongClickListener {
            if (debug) {
                debug = false
                test.visibility = View.GONE
            } else {
                debug = true
                test.visibility = View.VISIBLE
            }
            true
        }
    }

    private fun initView() {
        filter.setText(sp.getString(KEY_FILTER_WORDS, ""))
        filterNumbers.setText(sp.getString(KEY_FILTER_PHONES, ""))
    }

    private fun handleMsg(from: String, content: String) {
        test.append("收到信息：phone:$from,content:$content\n")
        // 有过滤号码才会过滤
        val filterPhones = sp.getString(KEY_FILTER_PHONES, "") ?: ""
        if (filterPhones.isNotBlank()) {
            if (from.isBlank()) {
                test.append("来电号码为空，被过滤掉\n")
                return
            }
            val phones = filterPhones.split(SPLIT_FILTER)
            var contain = false
            for (p in phones) {
                if (from == p) {
                    contain = true
                    break
                }
            }
            // 不包含号码
            if (!contain) {
                test.append("来电号码被过滤掉\n")
                return
            }
        }

        // 有过滤词才会过滤
        val filterWords = sp.getString(KEY_FILTER_WORDS, "") ?: ""
        if (filterWords != "") {
            if (content.isBlank()) {
                test.append("短信内容为空，被过滤掉\n")
                return
            }
            val words = filterWords.split(SPLIT_FILTER)
            var contain = false
            for (w in words) {
                if (content.contains(w)) {
                    contain = true
                    break
                }
            }
            // 不包含关键词
            if (!contain) {
                test.append("短信内容被过滤掉\n")
                return
            }
        }
        // 响铃提醒
        val intent = Intent(this, AlertActivity::class.java)
        startActivity(intent)
        RingtonePlayer.start()
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        RingtonePlayer.destroy()
    }

    internal inner class SMSBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(arg0: Context, intent: Intent) {

            val data = intent.extras!!.get("pdus") as Array<Any>? ?: return
            for (pdus in data) {
                val pdusMsg = pdus as ByteArray
                val sms = SmsMessage.createFromPdu(pdusMsg)
                val mobile = sms.originatingAddress//发送短信的手机号
                val content = sms.messageBody//短信内容
                handleMsg(mobile ?: "", content)
            }
        }
    }

}
