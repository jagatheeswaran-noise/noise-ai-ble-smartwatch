package com.zjw.sdkdemo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import com.blankj.utilcode.util.LogUtils

/**
 * Created by Android on 2023/3/2.
 */
class CallAndMessageReceiver : BroadcastReceiver() {

    private val TAG = "CallAndMessageReceiver"

    override fun onReceive(context: Context?, intent: Intent) {
        try {
            when (intent.action) {
                Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                    if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                        for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                            val messageBody = smsMessage.messageBody
                            val sender = smsMessage.originatingAddress
                            val senderName = getSenderName(sender, context)
                            LogUtils.e("接收到短信：senderName:$senderName,sender:$sender,messageBody:$messageBody")
                            //接收到短信：senderName:null,sender:10694522198517104760,messageBody:【爱奇艺】您正在登录，您的验证码是174543，转发给他人可能导致账号被盗，请勿泄漏，谨防被骗。
                        }
                    }
                }
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSenderName(number: String?, context: Context?): String? {
        try {
            number?.let {
                val uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(number)
                )
                val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
                var contactName: String? = null
                val cursor = context?.contentResolver?.query(uri, projection, null, null, null)
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        contactName = cursor.getString(0)
                    }
                    cursor.close()
                }
                return contactName
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return null
    }
}
