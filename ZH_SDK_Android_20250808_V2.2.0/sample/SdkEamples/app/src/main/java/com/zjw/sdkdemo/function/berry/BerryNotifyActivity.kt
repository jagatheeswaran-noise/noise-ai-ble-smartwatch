package com.zjw.sdkdemo.function.berry

import android.R.attr.type
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.ToastUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.ContactBean
import com.zhapp.ble.bean.ContactLotBean
import com.zhapp.ble.bean.DeviceInfoBean
import com.zhapp.ble.bean.EmergencyContactBean
import com.zhapp.ble.bean.SuperNotificationBean
import com.zhapp.ble.bean.berry.AddFavoriteContactBean
import com.zhapp.ble.bean.berry.FavoriteContactsBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.ContactCallBack
import com.zhapp.ble.callback.ContactLotCallBack
import com.zhapp.ble.callback.DeviceInfoCallBack
import com.zhapp.ble.callback.FavoriteContactsCallBack
import com.zhapp.ble.callback.UploadBigDataListener
import com.zhapp.ble.parsing.ParsingStateManager.SendCmdStateListener
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivityBerryNotifyBinding
import com.zjw.sdkdemo.function.berry.ChildrenActivity
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.livedata.DeviceSettingLiveData
import com.zjw.sdkdemo.utils.AssetUtils.getAssetBitmap
import com.zjw.sdkdemo.utils.ToastDialog
import com.zjw.sdkdemo.utils.customdialog.CustomDialog
import com.zjw.sdkdemo.utils.customdialog.MyDialog
import java.io.File
import java.util.Arrays


/**
 * Created by Android on 2024/10/24.
 */
class BerryNotifyActivity : BaseActivity() {

    private val binding: ActivityBerryNotifyBinding by lazy { ActivityBerryNotifyBinding.inflate(layoutInflater) }

    private val mFilePath = PathUtils.getExternalAppCachePath() + "/favoriteHead"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s385)
        setContentView(binding.root)
        initViews()
        initListener()
        initCallBack()
    }

    private fun initViews() {
        initFavoriteContactsView()
    }

    private fun initFavoriteContactsView() {
        FileUtils.createOrExistsDir(mFilePath)
        binding.tvTip1.setText("操作方法\n\n1.将[头像文件(图片资源)]，放到[${mFilePath}]目录下\n\n2.点击[选择文件]按钮，选择文件\n\n3.点击[${getString(R.string.s747)}]\n\n4.不选择将使用默认资源")
    }

    private fun initListener() {
        click(binding.btnAPP) {
            showAppDialog(1)
        }
        click(binding.btnAPP2) {
            showAppDialog(2)
        }
        initSuperNotificationTypeChange()
        click(binding.btnSendAppN) {
            sendAppNotice()
        }
        initCallSwitch()
        click(binding.btnSys0) {
            sendSysNotice(0)
        }
        click(binding.btnSys1) {
            sendSysNotice(1)
        }
        click(binding.btnSys2) {
            sendSysNotice(2)
        }
        click(binding.btnSysPhoneHangup) {
            ControlBleTools.getInstance().sendCallState(1, baseSendCmdStateListener)
        }
        click(binding.btnSysPhoneAnswer) {
            ControlBleTools.getInstance().sendCallState(0, baseSendCmdStateListener)
        }
        click(binding.btnSetR) {
            sendShortReply()
        }
        click(binding.btnGetR) {
            ControlBleTools.getInstance().getDevShortReplyData(baseSendCmdStateListener)
        }
        click(binding.btnSetRW) {
            sendWhatsAppReply()
        }
        click(binding.btnGetRW) {
            ControlBleTools.getInstance().getDevWhatsAppShortReplyData(baseSendCmdStateListener)
        }
        click(binding.btnSetContacts) {
            setContacts()
        }
        click(binding.btnGetContacts) {
            getContacts()
        }

        click(binding.setCyContact) {
            sendContactLot()
        }

        click(binding.getCyContact) {
            ControlBleTools.getInstance().getContactLotList(baseSendCmdStateListener)
        }

        click(binding.btnSetSosContacts) {
            sendSosContacts()
        }

        click(binding.btnGetSosContacts) {
            ControlBleTools.getInstance().getEmergencyContacts(baseSendCmdStateListener)
        }

        click(binding.btnSendSuperN) {
            sendSuperNotification()
        }

        click(binding.btnGetFavoriteContacts) {
            getFavoriteContacts()
        }

        click(binding.btnDelFavoriteContacts) {
            delFavoriteContacts()
        }

        click(binding.btnSetFavoriteContacts) {
            setFavoriteContacts()
        }

        click(binding.btnGetHeadFile) {
            val files = FileUtils.listFilesInDir(mFilePath)
            if (files.isNullOrEmpty()) {
                ToastUtils.showShort("$mFilePath 目录文件为空")
                return@click
            }
            showListDialog(files)
        }
    }

    //region 来电开关
    private fun initCallSwitch() {
//        ControlBleTools.getInstance().setBerryIncomingCallNotificationSwitch(binding.cbCallSwitch.isChecked, baseSendCmdStateListener)
//        ControlBleTools.getInstance().setBerryMissCallNotificationSwitch(binding.cbMissCallSwitch.isChecked, baseSendCmdStateListener)
        ControlBleTools.getInstance().setBerryCallNotificationSwitch(binding.cbCallSwitch.isChecked, binding.cbMissCallSwitch.isChecked, baseSendCmdStateListener)

        binding.cbCallSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            ControlBleTools.getInstance().setBerryIncomingCallNotificationSwitch(isChecked, baseSendCmdStateListener)
        }

        binding.cbMissCallSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            ControlBleTools.getInstance().setBerryMissCallNotificationSwitch(isChecked, baseSendCmdStateListener)
        }
    }
    //endregion

    //region 发送第三方app通知
    private var appPackName = ""
    private var appName = ""
    private var appLayout: LinearLayout? = null

    private fun showAppDialog(type: Int) {
        val dialog = Dialog(this)
        dialog.setContentView(layoutInflater.inflate(R.layout.dialog_app, null))
        appLayout = dialog.findViewById(R.id.appLayout)
        dialog.setCancelable(false)
        val params = dialog.window!!.attributes
        params.width = this.windowManager.defaultDisplay.width
        params.height = this.windowManager.defaultDisplay.height
        dialog.window!!.attributes = params
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        for (r in resolveInfos) {
            if (r.activityInfo != null && r.activityInfo.packageName != null) {
                val view = Button(this)
                view.text = r.loadLabel(pm).toString()
                view.setOnClickListener {
                    appPackName = r.activityInfo.packageName
                    appName = r.loadLabel(pm).toString()
                    if (type == 1) {
                        binding.etAppName.setText(appName)
                        binding.etPackName.setText(appPackName)
                    } else if (type == 2) {
                        binding.etAppName2.setText(appName)
                        binding.etPackName2.setText(appPackName)
                    }
                    dialog.dismiss()
                }
                appLayout?.addView(view)
            }
        }
        if (resolveInfos.size > 0) {
            dialog.show()
        } else {
            ToastDialog.showToast(this@BerryNotifyActivity, getString(R.string.s236))
        }
    }

    private fun sendAppNotice() {
        appPackName = binding.etPackName.getText().toString().trim { it <= ' ' }
        appName = binding.etAppName.getText().toString().trim { it <= ' ' }
        if (TextUtils.isEmpty(appPackName) || TextUtils.isEmpty(appName)) {
            Toast.makeText(this@BerryNotifyActivity, getString(R.string.s237), Toast.LENGTH_LONG).show()
            return
        }
        val title: String = binding.etNTitle.getText().toString().trim { it <= ' ' }
        val text: String = binding.etNText.getText().toString().trim { it <= ' ' }
        val t: String = binding.etNTicker.getText().toString().trim { it <= ' ' }
        ControlBleTools.getInstance().sendAppNotification(appName, appPackName, title, text, t, baseSendCmdStateListener)
    }

    //endregion

    //region 发送系统通知
    private fun sendSysNotice(type: Int) {
        val phone = binding.etPhone.text.toString().trim { it <= ' ' }
        val contancts = binding.etContacts.text.toString().trim { it <= ' ' }
        val msg = binding.etMsg.text.toString().trim { it <= ' ' }
        var sysName = ""
        var sysPackageName = ""
        when (type) {
            0, 1 -> {
                val smsInfo = getAppNameInfo(getDefaultSmsPackage(this))
                if (!TextUtils.isEmpty(smsInfo[0]) && !TextUtils.isEmpty(smsInfo[1])) {
                    sysName = smsInfo[1]
                    sysPackageName = smsInfo[0]
                }
            }

            2 -> {
                val smsInfo = getAppNameInfo(getDefaultDialerPackage(this))
                if (!TextUtils.isEmpty(smsInfo[0]) && !TextUtils.isEmpty(smsInfo[1])) {
                    sysName = smsInfo[1]
                    sysPackageName = smsInfo[0]
                }
            }
        }
        ControlBleTools.getInstance().sendSystemNotification(type, sysPackageName, sysName, sysPackageName, phone, contancts, msg, baseSendCmdStateListener)
    }

    fun getDefaultSmsPackage(context: Context): String {
        val packageManager: PackageManager = context.getPackageManager()
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.setData(Uri.parse("smsto:"))
        // 获取所有能处理该Intent的Activity
        val resolveInfo = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolveInfo != null && !resolveInfo.isEmpty()) {
            // 返回第一个匹配的Activity的包名，即默认短信应用的包名
            return resolveInfo[0].activityInfo.packageName
        }
        return ""
    }

    fun getDefaultDialerPackage(context: Context): String {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_DIAL)
        // 获取所有能处理该Intent的Activity
        val resolveInfo = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolveInfo != null && !resolveInfo.isEmpty()) {
            // 返回第一个匹配的Activity的包名，即默认拨号应用的包名
            return resolveInfo[0].activityInfo.packageName
        }
        return ""
    }

    private fun getAppNameInfo(packageName: String): Array<String> {
        val info = arrayOf<String>("", "")
        val defaultSmsPackage = packageName
        if (defaultSmsPackage.isNotEmpty()) {
            info[0] = defaultSmsPackage
            try {
                val appInfo = packageManager.getApplicationInfo(defaultSmsPackage, PackageManager.GET_META_DATA)
                val appName = packageManager.getApplicationLabel(appInfo)
                info[1] = appName.toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return info
    }
    //endregion

    //region 快速回复
    private fun initShortReplyDataObserve() {
        DeviceSettingLiveData.getInstance().getmShortReply().observe(this, Observer<java.util.ArrayList<String?>?> { strings ->
            if (strings == null) return@Observer
            ToastDialog.showToast(this@BerryNotifyActivity, "${getString(R.string.s229)}$strings")
        })
    }

    private fun sendShortReply() {
        val replys = ArrayList<String>()
        val reply: String = binding.etReply.getText().toString().trim { it <= ' ' }
        try {
            if (reply.contains(",")) {
                val rs = reply.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                replys.addAll(Arrays.asList(*rs))
            } else {
                replys.add(reply)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            replys.add(reply)
        }
        //list可设置多个 / 修改 / 删除 ,TODO 最多不能超过5个
        ControlBleTools.getInstance().setDevShortReplyData(replys, baseSendCmdStateListener)
    }
    //endregion

    //region whatsApp快速回复
    private fun initWhatsAppReplyDataObserve() {
        DeviceSettingLiveData.getInstance().getmWhatsAppReply().observe(this, Observer<java.util.ArrayList<String?>?> { strings ->
            if (strings == null) return@Observer
            ToastDialog.showToast(this@BerryNotifyActivity, "${getString(R.string.s229)}$strings")
        })
    }

    private fun sendWhatsAppReply() {
        val replys = ArrayList<String>()
        val reply: String = binding.etReplyW.getText().toString().trim { it <= ' ' }
        try {
            if (reply.contains(",")) {
                val rs = reply.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                replys.addAll(Arrays.asList(*rs))
            } else {
                replys.add(reply)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            replys.add(reply)
        }
        //list可设置多个 / 修改 / 删除 ,TODO 最多不能超过5个
        ControlBleTools.getInstance().setDevWhatsAppShortReplyData(replys, baseSendCmdStateListener)
    }
    //endregion

    //region 常用联系人
    private fun getContacts() {
        ControlBleTools.getInstance().getContactList(baseSendCmdStateListener)
    }

    private fun setContacts() {
        val list: MutableList<ContactBean> = java.util.ArrayList()
        for (i in 0..9) {
            val contactBean = ContactBean()
            contactBean.contacts_name = "name_$i"
            contactBean.contacts_number = "1234567890$i"
            contactBean.contacts_sequence = i
            list.add(contactBean)
        }
        ControlBleTools.getInstance().setContactList(list, baseSendCmdStateListener)
    }

    private fun initContactsData() {
        CallBackUtils.contactCallBack = ContactCallBack { data -> ToastDialog.showToast(this@BerryNotifyActivity, "" + GsonUtils.toJson(data)) }
    }
    //endregion

    //region 大量联系人
    private fun sendContactLot() {
        val list: MutableList<ContactBean> = java.util.ArrayList()
        var name = "contact"
        var phone = "181000000"
        try {
            if (binding.etCyContactName.text.toString().trim { it <= ' ' }.isNotEmpty()) {
                name = binding.etCyContactName.text.toString().trim { it <= ' ' }
            }
            if (binding.etCyContactPh.text.toString().trim { it <= ' ' }.isNotEmpty()) {
                phone = binding.etCyContactPh.text.toString().trim { it <= ' ' }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort(getString(R.string.s238))
            return
        }
        for (i in 0..49) {
            val contactBean = ContactBean()
            contactBean.contacts_name = i.toString() + name
            contactBean.contacts_number = phone + i
            list.add(contactBean)
        }
        val contactLotBean = ContactLotBean()
        contactLotBean.allCount = list.size
        contactLotBean.data = list
        ControlBleTools.getInstance().setContactLotList(contactLotBean, object : SendCmdStateListener(lifecycle) {
            override fun onState(state: SendCmdState) {
                when (state) {
                    SendCmdState.SUCCEED -> MyApplication.showToast(getString(R.string.s220))
                    else -> MyApplication.showToast(getString(R.string.s221))
                }
            }
        })
    }

    private fun initContactsLotData() {
        CallBackUtils.contactLotCallBack = ContactLotCallBack { contactLotBean -> ToastDialog.showToast(this@BerryNotifyActivity, "" + GsonUtils.toJson(contactLotBean)) }
    }
    //endregion

    //region sos联系人
    private fun sendSosContacts() {
        val bean = EmergencyContactBean()
        val sosList: MutableList<ContactBean> = java.util.ArrayList()
        val contactBean = ContactBean()
        var name = "name"
        var phone = "12345678900"
        try {
            if (binding.etSosContactName.text.toString().trim { it <= ' ' }.isNotEmpty()) {
                name = binding.etSosContactName.text.toString().trim { it <= ' ' }
            }
            if (binding.etSosContactPh.text.toString().trim { it <= ' ' }.isNotEmpty()) {
                phone = binding.etSosContactPh.text.toString().trim { it <= ' ' }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ToastUtils.showShort(getString(R.string.s238))
            return
        }
        contactBean.contacts_name = name
        contactBean.contacts_number = phone
        contactBean.contacts_sequence = 2
        sosList.add(contactBean)
        bean.contactList = sosList
        bean.sosSwitch = true
        bean.max = 1
        ControlBleTools.getInstance().setEmergencyContacts(bean, baseSendCmdStateListener)
    }

    private fun initEmergencyContactsData() {
        CallBackUtils.setEmergencyContactsCallBack { bean ->
            LogUtils.d("onEmergencyContacts:$bean")
            ToastDialog.showToast(this@BerryNotifyActivity, "" + GsonUtils.toJson(bean))
        }
    }
    //endregion

    //region 发送超级通知

    private fun initSuperNotificationTypeChange() {
        binding.etSNType.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                try {
                    val typeStr = binding.etSNType.text.toString().trim()
                    if (typeStr.isNotEmpty()) {
                        val type = binding.etSNType.text.toString().trim().toInt()
                        when (type) {
                            // 叫车应用对象:(int)订单状态枚举，(str)0TP字符串，(str)车牌号字符串
                            // 送餐应用对象:(int)订单状态枚举，(str)预计时间
                            // 电子商务应用对象:(int)订单状态枚举，(str)预计送达时间，(str)商品名称，(str)放置地点
                            // OTT和娱乐应用:(str)状态字符串，(str)名称
                            // 日历和会议:(str)标题字符串，(str)时间字符串
                            // 健康与健身应用:(int)状态枚举，(str)状态字符串，(str)内容字符串，(int)完成进度
                            // Messaging :(str)0TP内容
                            // Communication:(NULL)直接显示点赞
                            // 金融:(str)状态字符串，(str)付款人，(str)金额
                            // Dating apps:(int)状态枚举，(str)状态字符串 (str)姓名
                            SuperNotificationBean.NOTIFICATION_TYPE_TAXI -> {
                                binding.etSNContent.hint = getString(R.string.s742) + ":Int,String,String"
                            }

                            SuperNotificationBean.NOTIFICATION_TYPE_TAKEOUT -> {
                                binding.etSNContent.hint = getString(R.string.s742) + ":Int,String"
                            }

                            SuperNotificationBean.NOTIFICATION_TYPE_PARCEL -> {
                                binding.etSNContent.hint = getString(R.string.s742) + ":Int,String,String,String"
                            }

                            SuperNotificationBean.NOTIFICATION_TYPE_OTT -> {
                                binding.etSNContent.hint = getString(R.string.s742) + ":String,String"
                            }

                            SuperNotificationBean.NOTIFICATION_TYPE_CALENDAR -> {
                                binding.etSNContent.hint = getString(R.string.s742) + ":String,String"
                            }

                            SuperNotificationBean.NOTIFICATION_TYPE_HEALTH -> {
                                binding.etSNContent.hint = getString(R.string.s742) + ":Int,String,String,Int"
                            }

                            SuperNotificationBean.NOTIFICATION_TYPE_MESSAGING -> {
                                binding.etSNContent.hint = getString(R.string.s742) + ":String"
                            }

                            SuperNotificationBean.NOTIFICATION_TYPE_COMMUNICATION -> {
                                binding.etSNContent.hint = getString(R.string.s742) + ":String"
                            }

                            SuperNotificationBean.NOTIFICATION_TYPE_FINANCE -> {
                                binding.etSNContent.hint = getString(R.string.s742) + ":String,String,String"
                            }

                            SuperNotificationBean.NOTIFICATION_TYPE_DATING -> {
                                binding.etSNContent.hint = getString(R.string.s742) + ":Int,String,String"
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    ToastUtils.showShort(getString(R.string.s238))
                }
            }

        })
    }

    private fun sendSuperNotification() {
        try {
            val bean = SuperNotificationBean()
            bean.appName = binding.etAppName2.text.toString().trim()
            bean.packageName = binding.etPackName2.text.toString().trim()
            val type = binding.etSNType.text.toString().trim().toInt()
            val context = binding.etSNContent.text.toString().trim().split(",")
            when (type) {
                SuperNotificationBean.NOTIFICATION_TYPE_TAXI -> {
                    bean.notificationContent = SuperNotificationBean.TaxiNotificationContent().apply {
                        orderStatus = context[0].toInt()
                        plateNumber = context[1]
                        otp = context[2]
                    }
                }

                SuperNotificationBean.NOTIFICATION_TYPE_TAKEOUT -> {
                    bean.notificationContent = SuperNotificationBean.TakeoutNotificationContent().apply {
                        orderStatus = context[0].toInt()
                        expectedTime = context[1]
                    }
                }

                SuperNotificationBean.NOTIFICATION_TYPE_PARCEL -> {
                    bean.notificationContent = SuperNotificationBean.ParcelNotificationContent().apply {
                        orderStatus = context[0].toInt()
                        expectedTime = context[1]
                        productName = context[2]
                        productPlace = context[3]
                    }
                }

                SuperNotificationBean.NOTIFICATION_TYPE_OTT -> {
                    bean.notificationContent = SuperNotificationBean.OTTNotificationContent().apply {
                        status = context[0].toInt()
                        name = context[1]
                    }
                }

                SuperNotificationBean.NOTIFICATION_TYPE_CALENDAR -> {
                    bean.notificationContent = SuperNotificationBean.CalendarNotificationContent().apply {
                        title = context[0]
                        time = context[1]
                    }
                }

                SuperNotificationBean.NOTIFICATION_TYPE_HEALTH -> {
                    bean.notificationContent = SuperNotificationBean.HealthNotificationContent().apply {
                        status = context[0].toInt()
                        statusStr = context[1]
                        content = context[2]
                        progress = context[3].toInt()
                    }
                }

                SuperNotificationBean.NOTIFICATION_TYPE_MESSAGING -> {
                    bean.notificationContent = SuperNotificationBean.MessagingNotificationContent().apply {
                        otp = context[0]
                    }
                }

                SuperNotificationBean.NOTIFICATION_TYPE_COMMUNICATION -> {
                    bean.notificationContent = SuperNotificationBean.CommunicationNotificationContent().apply {
                        likeCount = context[0]
                    }
                }

                SuperNotificationBean.NOTIFICATION_TYPE_FINANCE -> {
                    bean.notificationContent = SuperNotificationBean.FinanceNotificationContent().apply {
                        status = context[0].toInt()
                        payer = context[1]
                        amount = context[2]
                    }
                }

                SuperNotificationBean.NOTIFICATION_TYPE_DATING -> {
                    bean.notificationContent = SuperNotificationBean.DatingNotificationContent().apply {
                        status = context[0].toInt()
                        statusStr = context[1]
                        name = context[2]
                    }
                }
            }
            ControlBleTools.getInstance().sendSuperNotification(bean, baseSendCmdStateListener)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort(getString(R.string.s238))
        }
    }
    //endregion

    //region 选择家长头像
    private lateinit var fHeadfile: File
    private var headDialog: MyDialog? = null
    private fun showListDialog(files: List<File>) {
        headDialog = CustomDialog.builder(this)
            .setContentView(R.layout.dialog_debug_bin_list)
            .setWidth(ScreenUtils.getScreenWidth())
            .setHeight((ScreenUtils.getScreenHeight() * 0.8f).toInt())
            .build()
        for (file in files) {
            if (file.name.endsWith(".jpg") || file.name.endsWith(".jpeg") || file.name.endsWith(".png")) {
                val view = Button(this)
                view.isAllCaps = false
                view.text = file.name
                view.setOnClickListener {
                    this.fHeadfile = file
                    binding.tvHeadName.text = "${getString(R.string.s513)} ${file.name}"
                    headDialog?.dismiss()
                }
                headDialog?.findViewById<LinearLayout>(R.id.listLayout)?.addView(view)
            }
        }
        headDialog?.show()
    }
    //endregion

    //region 带头像收藏联系人
    private var favoriteContactsBean: FavoriteContactsBean? = null
    private fun setFavoriteContacts() {
        if (deviceInfo == null) {
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().getDeviceInfo(null)
            }
            ToastUtils.showShort(R.string.s8)
            return
        }
        if (favoriteContactsBean == null) {
            ToastUtils.showShort(R.string.s219)
            return
        }
        val list = mutableListOf<AddFavoriteContactBean>()
        for (i in 0..favoriteContactsBean!!.supportMax - 1) {
            list.add(
                AddFavoriteContactBean().apply {
                    val fName = binding.etFName.text.toString().trim()
                    name = "$fName $i"
                    val fPhone = binding.etFPhone.text.toString().trim()
                    phoneNumber = "$fPhone$i"
                    if (::fHeadfile.isInitialized) {
                        var headImg = ConvertUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(fHeadfile))
                        avatarImg = headImg
                    } else {
                        var headImg = getAssetBitmap(this@BerryNotifyActivity, "children" + File.separator + "p_head.png")
                        avatarImg = headImg
                    }
                }
            )
        }
        ControlBleTools.getInstance().setFavoriteContacts(favoriteContactsBean!!.supportMax, list, deviceInfo!!.equipmentNumber, MyUploadBigDataListener())
        //setFavoriteContacts
    }

    private var dialog: Dialog? = null

    inner class MyUploadBigDataListener() : UploadBigDataListener {
        override fun onSuccess() {
            if (dialog?.isShowing == true) dialog?.dismiss()
            ToastUtils.showShort(getString(R.string.s220))
            binding.btnGetFavoriteContacts.callOnClick()
        }

        override fun onProgress(curPiece: Int, dataPackTotalPieceLength: Int) {
            if (curPiece == 0) {
                if (dialog?.isShowing == true) dialog?.dismiss()
                dialog = ToastDialog.showToast(this@BerryNotifyActivity, getString(R.string.s648))
            }
        }

        override fun onTimeout(msg: String?) {
            if (dialog?.isShowing == true) dialog?.dismiss()
            ToastUtils.showShort(getString(R.string.s221) + " : " + msg)
        }

    }

    private var deviceInfo: DeviceInfoBean? = null
    private fun initDeviceInfoCallBack() {
        CallBackUtils.deviceInfoCallBack = object : DeviceInfoCallBack {
            override fun onDeviceInfo(deviceInfoBean: DeviceInfoBean?) {
                //ToastDialog.showToast(this@BerryDeviceActivity, gson.toJson(deviceInfoBean))
                deviceInfo = deviceInfoBean
            }

            override fun onBatteryInfo(capacity: Int, chargeStatus: Int) {
            }
        }
    }

    private fun getFavoriteContacts() {
        ControlBleTools.getInstance().getFavoriteContacts(baseSendCmdStateListener)
    }

    private fun delFavoriteContacts() {
        if (favoriteContactsBean == null) {
            ToastUtils.showShort(R.string.s219)
            return
        }
        ControlBleTools.getInstance().deleteFavoriteContacts(favoriteContactsBean!!.list, baseSendCmdStateListener)
    }

    private fun initFavoriteContactsDataObserve() {
        CallBackUtils.favoriteContactsCallBack = object : FavoriteContactsCallBack {
            override fun onFavoriteContacts(bean: FavoriteContactsBean?) {
                favoriteContactsBean = bean
                ToastDialog.showToast(this@BerryNotifyActivity, getString(R.string.s745) + ":" + GsonUtils.toJson(bean))
            }
        }
    }
    //endregion

    private fun initCallBack() {
        initDeviceInfoCallBack()
        initContactsData()
        initEmergencyContactsData()
        initContactsLotData()
        initShortReplyDataObserve()
        initWhatsAppReplyDataObserve()
        initFavoriteContactsDataObserve()
    }


}