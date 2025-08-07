package com.zjw.sdkdemo.function

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.GlodFriendContactsBean
import com.zhapp.ble.bean.GlodFriendEmojiBean
import com.zhapp.ble.bean.TimeBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.GlodFriendContactsCallBack
import com.zhapp.ble.callback.GlodFriendEmojiCallBack
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.function.language.BaseActivity
import java.util.Calendar

class EmojiActivity : BaseActivity() {
    var glodFriendEmojiBean: GlodFriendEmojiBean? = null

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s548)
        setContentView(R.layout.activity_emoji)
        val etContacts = findViewById<EditText>(R.id.etContacts)
        val etEmoji = findViewById<EditText>(R.id.etEmoji)

        findViewById<Button>(R.id.btnGetContacts).setOnClickListener {
            CallBackUtils.glodFriendContactsCallBack = GlodFriendContactsCallBack { it ->
                var text = ""
                it.forEach {
                    text += it.callContactsId.toString() + ","+it.callContactsName + ","
                }
                text = text.removeRange(text.length - 1, text.length)
                etContacts.setText(text)
            }
            ControlBleTools.getInstance().getGlodFriendContactsList(null)
        }
        findViewById<Button>(R.id.btnSetContacts).setOnClickListener {
            val array = etContacts.text.split(",")
            val list = mutableListOf<GlodFriendContactsBean>()
            for (a in array.indices step 2) {
                var bean = GlodFriendContactsBean()
                bean.callContactsId = array[a].toInt()
                bean.callContactsName = array[a + 1]
                list.add(bean)
            }
            ControlBleTools.getInstance().setGlodFriendContactsList(list, null)

        }

        findViewById<Button>(R.id.btnSetEmoji).setOnClickListener {
            val array = etEmoji.text.split(",")
            val cal = Calendar.getInstance()
            cal.timeInMillis = System.currentTimeMillis()
            val bean = GlodFriendEmojiBean(
                array[0].toInt(),
                array[1],
                array[2].toInt(),
                array[3].toInt(),
                TimeBean(cal[Calendar.YEAR], cal[Calendar.MONTH] + 1, cal[Calendar.DAY_OF_MONTH], cal[Calendar.HOUR_OF_DAY], cal[Calendar.MINUTE], cal[Calendar.SECOND])
            )
            ControlBleTools.getInstance().setGlodFriendEmoji(bean, null)
        }
        findViewById<Button>(R.id.btnSuc).setOnClickListener {
            if (glodFriendEmojiBean == null) return@setOnClickListener
            glodFriendEmojiBean?.friendEmojiState = 1
            ControlBleTools.getInstance().setGlodFriendEmojiRequest(glodFriendEmojiBean, null)
        }
        findViewById<Button>(R.id.btnFail).setOnClickListener {
            if (glodFriendEmojiBean == null) return@setOnClickListener
            glodFriendEmojiBean?.friendEmojiState = 0
            ControlBleTools.getInstance().setGlodFriendEmojiRequest(glodFriendEmojiBean, null)
        }

        CallBackUtils.glodFriendEmojiCallBack = GlodFriendEmojiCallBack {
            glodFriendEmojiBean = it
            etEmoji.setText("${it.callContactsId},${it.callContactsName},${it.emoji},${it.color}")
        }
    }
}