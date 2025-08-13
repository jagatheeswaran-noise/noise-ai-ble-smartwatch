package com.zjw.sdkdemo.function.berry

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ClickUtils
import com.blankj.utilcode.util.ClipboardUtils
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.ToastUtils
import com.zhapp.ble.BerryBluetoothService
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.DeviceInfoBean
import com.zhapp.ble.bean.WatchFaceInstallResultBean
import com.zhapp.ble.bean.WatchFaceListBean
import com.zhapp.ble.bean.berry.BerryAlbumWatchFaceEditRequestBean
import com.zhapp.ble.bean.berry.BerryWatchFaceStatusReplyBean
import com.zhapp.ble.callback.BerryDialUploadListener
import com.zhapp.ble.callback.BerryWatchFaceStatusCallBack
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.DeviceInfoCallBack
import com.zhapp.ble.callback.DeviceLargeFileStatusListener
import com.zhapp.ble.callback.WatchFaceCallBack
import com.zhapp.ble.callback.WatchFaceInstallCallBack
import com.zhapp.ble.callback.WatchFaceListCallBack
import com.zhapp.ble.parsing.SendCmdState
import com.zhapp.ble.utils.BleUtils
import com.zhapp.ble.utils.MD5Utils
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.AcitivityBerryDialBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.livedata.BleConnectState
import com.zjw.sdkdemo.utils.customdialog.CustomDialog
import com.zjw.sdkdemo.utils.customdialog.MyDialog
import java.io.File

/**
 * Created by Android on 2024/10/12.
 */
class BerryDialActivity : BaseActivity() {
    private val binding: AcitivityBerryDialBinding by lazy { AcitivityBerryDialBinding.inflate(layoutInflater) }
    private var deviceInfo: DeviceInfoBean? = null

    private val mFilePath = PathUtils.getExternalAppCachePath() + "/dialTest"

    private val mBgFilePath = PathUtils.getExternalAppCachePath() + "/dialBgTest"

    private var watchFaceList: MutableList<WatchFaceListBean> = mutableListOf()

    //是否发送相册表盘
    private var isSendAlbum = false

    var dialBgWidth = 0
    var dailBgHeight = 0
    var dialBgStyle = "1"

    private var dialAlbumId = ""

    private var isDialLoop = false
    private var isAlbumDialLoop = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s23)
        setContentView(binding.root)
        inits()
        clicks()
    }

    override fun onResume() {
        super.onResume()
        initSppLogCallBack()
    }

    //region log
    private fun initSppLogCallBack() {

        click(binding.llLog) {
            val log = StringBuilder()
            for (i in 0 until binding.llLog.childCount) {
                log.append((binding.llLog.getChildAt(i) as TextView).text.toString().trim()).append("\n")
            }
            ClipboardUtils.copyText(log)
            ToastUtils.showShort("LOG COPY")
        }

        binding.llLog.setOnLongClickListener {
            binding.llLog.removeAllViews()
            ToastUtils.showShort("LOG CLEAR")
            return@setOnLongClickListener true
        }

        BerryBluetoothService.getInstance().setSppDataListener(object : BerryBluetoothService.SppDataListener {
            @SuppressLint("SetTextI18n")
            override fun onWLog(data: ByteArray?) {
                ThreadUtils.runOnUiThread {
                    if (data != null) {
                        binding.llLog.addView(TextView(this@BerryDialActivity).apply {
                            text = "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} Spp write --------> : ${BleUtils.bytes2HexString(data)}"
                            textSize = 10.0f
                        }, 0)
                    }
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onRLog(data: ByteArray?) {
                ThreadUtils.runOnUiThread {
                    if (data != null) {
                        binding.llLog.addView(TextView(this@BerryDialActivity).apply {
                            text = "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} Spp received --> : ${BleUtils.bytes2HexString(data)}"
                            textSize = 10.0f
                        }, 0)
                    }
                }
            }

            override fun onString(data: String?) {
                ThreadUtils.runOnUiThread {
                    if (data != null) {
                        binding.llLog.addView(TextView(this@BerryDialActivity).apply {
                            text = "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} Parsing --> : ${data}"
                            textSize = 10.0f
                            if (data.contains("Exception")) {
                                setTextColor(Color.RED)
                            }
                        }, 0)
                    }
                }
            }
        })
    }
    //endregion


    private fun inits() {
        initView()
        initObserve()
        initCallBack()
    }

    private fun initView() {
        if (ControlBleTools.getInstance().isConnect) {
            ControlBleTools.getInstance().getDeviceInfo(baseSendCmdStateListener)
            ControlBleTools.getInstance().getWatchFaceList(MyWatchFaceListCallBack())
        }

        FileUtils.createOrExistsDir(mFilePath)
        //Operation method
        //
        //1. Place the [watch face file (.bin)] in the [${mFilePath}] directory
        //
        //2. Click the [Select watch face file] button and select the file
        //
        //3. Click [Send online watch face] to start the transfer
        binding.tvTip1.setText("操作方法\n\n1.将[表盘文件(.bin)]，放到[${mFilePath}]目录下\n\n2.点击[选择表盘文件]按钮，选择文件\n\n3.点击[发送在线表盘]开始传输")
        FileUtils.createOrExistsDir(mBgFilePath)
        //Operation method
        //
        //1. Put [watch face file (.bin)] in the [${mFilePath}] directory
        //
        //2. Click the [Select watch face file] button and select the file
        //
        //3. Put [background file (image)] in the [${mBgFilePath}] directory
        //
        //4. Click the [Select watch face background file] button and select the file
        //
        //5. Click [Send album watch face] to start the transfer
        binding.tvTip2.setText("操作方法\n\n1.将[表盘文件(.bin)]，放到[${mFilePath}]目录下\n\n2.点击[选择表盘文件]按钮，选择文件\n\n3.将[背景文件(图片)]，放到[${mBgFilePath}]目录下\n\n4.点击[选择表盘背景文件]按钮，选择文件\n\n5.点击[发送相册表盘]开始传输")

        binding.rvList.layoutManager = LinearLayoutManager(this)
        binding.rvList.adapter = MyAdapter(watchFaceList)

    }

    private fun initCallBack() {
        CallBackUtils.deviceInfoCallBack = object : DeviceInfoCallBack {
            override fun onDeviceInfo(deviceInfoBean: DeviceInfoBean?) {
                deviceInfo = deviceInfoBean
            }

            override fun onBatteryInfo(capacity: Int, chargeStatus: Int) {}
        }

        //APP 或 设备 设置、删除表盘回调 APP or device settings, delete watch face callbacks
        CallBackUtils.watchFaceCallBack = object : WatchFaceCallBack {
            override fun setWatchFace(isSet: Boolean) {
                if (isSet) {
                    ToastUtils.showShort("设置为当前成功") //Set to current success
                    binding.btnGetList.callOnClick()
                } else {
                    ToastUtils.showShort("设置为当前失败") //Set to current failure
                }
            }

            override fun removeWatchFace(isRemove: Boolean) {
                if (isRemove) {
                    ToastUtils.showShort("删除成功")  //Deleted successfully
                    binding.btnGetList.callOnClick()
                } else {
                    ToastUtils.showShort("删除失败")  //Deletion failed
                }
            }

        }

        CallBackUtils.berryWatchFaceStatusCallBack = object : BerryWatchFaceStatusCallBack {
            override fun onPrepareStatus(bean: BerryWatchFaceStatusReplyBean?) {
                if (bean != null) {
                    if (bean.statusValue == BerryWatchFaceStatusCallBack.PrepareStatus.READY.getState()) {
                        if (ControlBleTools.getInstance().isConnect) {
                            ControlBleTools.getInstance().getDeviceLargeFileStateByBerry(
                                FileIOUtils.readFile2BytesByStream(if (!isSendAlbum) dialFile else albumDialFile),
                                BleCommonAttributes.UPLOAD_BIG_DATA_WATCH,
                                deviceInfo!!.equipmentNumber, deviceInfo!!.firmwareVersion, dialLargeFileStatusListener
                            )
                        }
                    } else if (bean.statusValue == BerryWatchFaceStatusCallBack.PrepareStatus.BUSY.getState()) {
                        MyApplication.showToast(getString(R.string.s223))
                    } else if (bean.statusValue == BerryWatchFaceStatusCallBack.PrepareStatus.DUPLICATED.getState()) {
                        MyApplication.showToast(getString(R.string.s224))
                    } else if (bean.statusValue == BerryWatchFaceStatusCallBack.PrepareStatus.LOW_STORAGE.getState()) {
                        MyApplication.showToast(getString(R.string.s224))
                    } else if (bean.statusValue == BerryWatchFaceStatusCallBack.PrepareStatus.LOW_BATTERY.getState()) {
                        MyApplication.showToast(getString(R.string.s225))
                    } else if (bean.statusValue == BerryWatchFaceStatusCallBack.PrepareStatus.DOWNGRADE.getState()) {
                        MyApplication.showToast(getString(R.string.s224))
                    }
                }
            }
        }

        CallBackUtils.watchFaceInstallCallBack = object : WatchFaceInstallCallBack {
            override fun onresult(result: WatchFaceInstallResultBean?) {
                if (result != null) {
                    when (result?.code) {
                        WatchFaceInstallCallBack.WatchFaceInstallCode.INSTALL_SUCCESS.state -> {
                            //安装成功 Installation Successful
                            MyApplication.showToast(R.string.s497)
                            binding.btnGetList.callOnClick()
                            testLoop(true)
                        }

                        WatchFaceInstallCallBack.WatchFaceInstallCode.INSTALL_FAILED.state -> {
                            //安装失败 Installation failed
                            MyApplication.showToast(R.string.s498)
                        }

                        WatchFaceInstallCallBack.WatchFaceInstallCode.VERIFY_FAILED.state,
                        WatchFaceInstallCallBack.WatchFaceInstallCode.INSTALL_USED.state -> {
                            //验证失败 Authentication failed
                            MyApplication.showToast(R.string.s499)
                        }
                    }
                }
            }

        }

    }

    private fun initObserve() {
        BleConnectState.getInstance().observe(this) { integer ->
            when (integer) {
                BleCommonAttributes.STATE_CONNECTED -> {
                    binding.tvStatus.text = getString(R.string.s255) + getString(R.string.ble_connected_tips)

                    if (ControlBleTools.getInstance().isConnect) {
                        ControlBleTools.getInstance().getDeviceInfo(baseSendCmdStateListener)
                        ControlBleTools.getInstance().getWatchFaceList(MyWatchFaceListCallBack())
                    }
                }

                BleCommonAttributes.STATE_CONNECTING -> {
                    binding.tvStatus.text = getString(R.string.s255) + getString(R.string.ble_connecting_tips)
                }

                BleCommonAttributes.STATE_DISCONNECTED -> {
                    binding.tvStatus.text = getString(R.string.s255) + getString(R.string.ble_disconnect_tips)
                }

                BleCommonAttributes.STATE_TIME_OUT -> {
                    binding.tvStatus.text = getString(R.string.s255) + getString(R.string.ble_connect_time_out_tips)
                }
            }
        }
    }


    private fun clicks() {
        click(binding.btnGetList) {
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().getWatchFaceList(MyWatchFaceListCallBack())
            }
        }

        click(binding.btnGetDialFile) {
            val files = FileUtils.listFilesInDir(mFilePath)
            if (files.isNullOrEmpty()) {
                ToastUtils.showShort("$mFilePath 目录文件为空 The directory file is empty")
                return@click
            }
            showListDialog(files)
        }

        click(binding.btnGetAlbumDialFile) {
            val files = FileUtils.listFilesInDir(mFilePath)
            if (files.isNullOrEmpty()) {
                ToastUtils.showShort("$mFilePath 目录文件为空 The directory file is empty")
                return@click
            }
            showBgListDialog(files)
        }

        click(binding.btnGetDialBgFile) {
            try {
                dialBgWidth = binding.etW.text.toString().toInt()
                dailBgHeight = binding.etH.text.toString().toInt()
                if (dialBgWidth <= 0 || dailBgHeight <= 0) {
                    ToastUtils.showShort("请先输入表盘高宽 Please enter the height and width of the dial first")
                    return@click
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ToastUtils.showShort("请先输入表盘高宽 Please enter the height and width of the dial first")
                return@click
            }

            val files = FileUtils.listFilesInDir(mBgFilePath)
            if (files.isNullOrEmpty()) {
                ToastUtils.showShort("$mBgFilePath 目录文件为空 The directory file is empty")
                return@click
            }
            showBgImgListDialog(files)
        }

        click(binding.btnDial) {
            FileUtils.deleteAllInDir(getExternalFilesDir("agps"))
            dialBgStyle = binding.etStyle.text.toString()
            if (!::dialFile.isInitialized) {
                ToastUtils.showShort("请先选择表盘文件 Please select the watch face file first")
                return@click
            }
            isSendAlbum = false
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().getWatchFaceStatusByBerry(
                    getTestDialIDByFile(dialFile), dialBgStyle,
                    FileIOUtils.readFile2BytesByStream(dialFile).size,
                    null,
                    baseSendCmdStateListener
                )
            }
        }

        click(binding.btnDialLoop) {
            isDialLoop = true
            isAlbumDialLoop = false
            binding.btnDial.callOnClick()
        }

        click(binding.btnAlbumDial) {
            FileUtils.deleteAllInDir(getExternalFilesDir("agps"))
            if (!::albumDialFile.isInitialized) {
                ToastUtils.showShort("请先选择表盘文件 Please select the watch face file first")
                return@click
            }
            if (watchFaceList.isNullOrEmpty()) {
                ToastUtils.showShort("请先选择获取表盘列表 Please select Get watch face list first")
                return@click
            }
            dialAlbumId = binding.etCDid.text.toString().trim()
            if (dialAlbumId.isNullOrEmpty()) {
                ToastUtils.showShort("请输入表盘ID Please enter the dial ID")
                return@click
            }
            dialBgStyle = binding.etStyle.text.toString()
            isSendAlbum = true
            if (ControlBleTools.getInstance().isConnect) {
                //发送相册表盘
                var isNeedGetState = true
                for (item in watchFaceList) {
                    if (TextUtils.equals(item.id, dialAlbumId)) {
                        isNeedGetState = false;
                    }
                }
                //发送相册表盘
                if (isNeedGetState) {
                    //未安装 - 需查询状态后再发送
                    var bgBitmap: Bitmap? = null
                    try {
                        dialBgWidth = binding.etW.text.toString().toInt()
                        dailBgHeight = binding.etH.text.toString().toInt()
                    }catch (e: Exception){
                        dialBgWidth = 0
                        dailBgHeight = 0
                    }
                    if (dialBgWidth <= 0 || dailBgHeight <= 0) {
                        ToastUtils.showShort("请先输入表盘高宽 Please enter the height and width of the dial first")
                        return@click
                    }
                    if (::dialBgFile.isInitialized) bgBitmap =
                        ImageUtils.scale(ConvertUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(dialBgFile)), dialBgWidth, dailBgHeight)
                    ControlBleTools.getInstance().getWatchFaceStatusByBerry(
                        dialAlbumId, dialBgStyle,
                        FileIOUtils.readFile2BytesByStream(albumDialFile).size,
                        bgBitmap,
                        baseSendCmdStateListener
                    )
                } else {
                    //已安装 - 直接发送
                    val albumRequest = BerryAlbumWatchFaceEditRequestBean()
                    albumRequest.id = dialAlbumId
                    albumRequest.isSetCurrent = true
                    albumRequest.style = dialBgStyle //样式后台约定 1，2，3 ...
                    var bgBitmap: Bitmap? = null
                    dialBgWidth = binding.etW.text.toString().toInt()
                    dailBgHeight = binding.etH.text.toString().toInt()
                    if (dialBgWidth <= 0 || dailBgHeight <= 0) {
                        ToastUtils.showShort("请先输入表盘高宽 Please enter the height and width of the dial first")
                        return@click
                    }
                    if (::dialBgFile.isInitialized) bgBitmap =
                        ImageUtils.scale(ConvertUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(dialBgFile)), dialBgWidth, dailBgHeight)
                    ControlBleTools.getInstance().startUploadDialBigDataByBerry(
                        true,
                        FileIOUtils.readFile2BytesByStream(albumDialFile),
                        bgBitmap,
                        albumRequest,
                        dialUploadListener
                    )
                }
            }
        }

        click(binding.btnAlbumDialLoop) {
            isAlbumDialLoop = true
            isDialLoop = false
            binding.btnAlbumDial.callOnClick()
        }
    }

    private fun getTestDialIDByFile(file: File): String {
        return Math.abs(BleUtils.byte2Int(MD5Utils.getMD5bytes(FileIOUtils.readFile2BytesByStream(file)), false)).toString()
    }

    //region 表盘列表
    inner class MyWatchFaceListCallBack : WatchFaceListCallBack {
        @SuppressLint("NotifyDataSetChanged")
        override fun onResponse(list: MutableList<WatchFaceListBean>?) {
            if (list != null) {
                watchFaceList.clear()
                watchFaceList.addAll(list)
                binding.rvList.adapter?.notifyDataSetChanged()
            }
        }

        override fun timeOut(errorStart: SendCmdState) {
            ToastUtils.showShort("getWatchFaceList error = $errorStart")
        }
    }

    inner class MyAdapter(private val dataList: List<WatchFaceListBean>) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivImg: AppCompatImageView = itemView.findViewById(R.id.ivImg)
            var tvDialId: AppCompatTextView = itemView.findViewById(R.id.tvDialId)
            var tvDialName: AppCompatTextView = itemView.findViewById(R.id.tvDialName)
            var tvDialType: AppCompatTextView = itemView.findViewById(R.id.tvDialType)
            var btnCur: AppCompatButton = itemView.findViewById(R.id.btnCur)
            var btnDel: AppCompatButton = itemView.findViewById(R.id.btnDel)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_berry_dial, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvDialId.text = dataList[position].id
            holder.tvDialName.text = dataList[position].name
            holder.tvDialType.text = dataList[position].style
            holder.btnCur.isEnabled = !dataList[position].isCurrent
            holder.btnDel.visibility = if (dataList[position].isRemove) View.VISIBLE else View.GONE
            ClickUtils.applySingleDebouncing(holder.btnCur) {
                onCur(dataList[position])
            }
            ClickUtils.applySingleDebouncing(holder.btnDel) {
                onDel(dataList[position])
            }
        }

        override fun getItemCount(): Int {
            return dataList.size
        }
    }

    private fun onCur(watchFace: WatchFaceListBean) {
        if (ControlBleTools.getInstance().isConnect) {
            ControlBleTools.getInstance().setDeviceWatchFromId(watchFace.id, baseSendCmdStateListener)
        }
    }

    private fun onDel(watchFace: WatchFaceListBean) {
        if (ControlBleTools.getInstance().isConnect) {
            ControlBleTools.getInstance().deleteDeviceWatchFromId(watchFace.id, baseSendCmdStateListener)
        }
    }
    //endregion

    //region 选择dialFile
    private lateinit var dialFile: File
    private var dailDialog: MyDialog? = null
    private fun showListDialog(files: List<File>) {
        dailDialog = CustomDialog.builder(this)
            .setContentView(R.layout.dialog_debug_bin_list)
            .setWidth(ScreenUtils.getScreenWidth())
            .setHeight((ScreenUtils.getScreenHeight() * 0.8f).toInt())
            .build()
        for (file in files) {
            if (file.name.endsWith(".bin")) {
                val view = Button(this)
                view.isAllCaps = false
                view.text = file.name
                view.setOnClickListener {
                    this.dialFile = file
                    binding.tvDialName.text = "${getString(R.string.s513)} ${file.name}"
                    dailDialog?.dismiss()
                }
                dailDialog?.findViewById<LinearLayout>(R.id.listLayout)?.addView(view)
            }
        }
        dailDialog?.show()
    }
    //endregion

    //region 选择相册dialFile
    private lateinit var albumDialFile: File
    private var albumDailDialog: MyDialog? = null
    private fun showBgListDialog(files: List<File>) {
        albumDailDialog = CustomDialog.builder(this)
            .setContentView(R.layout.dialog_debug_bin_list)
            .setWidth(ScreenUtils.getScreenWidth())
            .setHeight((ScreenUtils.getScreenHeight() * 0.8f).toInt())
            .build()
        for (file in files) {
            if (file.name.endsWith(".bin")) {
                val view = Button(this)
                view.isAllCaps = false
                view.text = file.name
                view.setOnClickListener {
                    this.albumDialFile = file
                    binding.tvDialName.text = "${getString(R.string.s513)} ${file.name}"
                    albumDailDialog?.dismiss()
                }
                albumDailDialog?.findViewById<LinearLayout>(R.id.listLayout)?.addView(view)
            }
        }
        albumDailDialog?.show()
    }
    //endregion

    //region 选择dialBgFile
    private lateinit var dialBgFile: File
    private var dialBgDialog: MyDialog? = null
    private fun showBgImgListDialog(files: List<File>) {
        dialBgDialog = CustomDialog.builder(this)
            .setContentView(R.layout.dialog_debug_bin_list)
            .setWidth(ScreenUtils.getScreenWidth())
            .setHeight((ScreenUtils.getScreenHeight() * 0.8f).toInt())
            .build()
        for (file in files) {
            if (ImageUtils.isImage(file)) {
                val view = Button(this)
                view.isAllCaps = false
                view.text = file.name
                view.setOnClickListener {
                    this.dialBgFile = file
                    binding.tvDialBgName.text = "${getString(R.string.s513)} ${file.name}"
                    dialBgDialog?.dismiss()
                }
                dialBgDialog?.findViewById<LinearLayout>(R.id.listLayout)?.addView(view)
            }
        }
        dialBgDialog?.show()
    }
    //endregion

    //region 在线表盘大文件传输状态
    private val dialLargeFileStatusListener: DialLargeFileStatusListener by lazy { DialLargeFileStatusListener() }

    inner class DialLargeFileStatusListener : DeviceLargeFileStatusListener {
        override fun onSuccess(statusValue: Int, statusName: String?) {
            if (statusValue == DeviceLargeFileStatusListener.PrepareStatus.READY.state || statusName == "READY") {
                if (!isSendAlbum) {
                    //发送在线
                    ControlBleTools.getInstance().startUploadDialBigDataByBerry(
                        false,
                        FileIOUtils.readFile2BytesByStream(dialFile),
                        null,
                        null,
                        dialUploadListener
                    )
                } else {
                    //发送相册
                    var isNeedGetState = true
                    for (item in watchFaceList) {
                        if (TextUtils.equals(item.id, dialAlbumId)) {
                            isNeedGetState = false;
                        }
                    }
                    if (isNeedGetState) {
                        val albumRequest = BerryAlbumWatchFaceEditRequestBean()
                        albumRequest.id = dialAlbumId
                        albumRequest.isSetCurrent = true
                        albumRequest.style = dialBgStyle //样式后台约定 1，2，3 ...
                        var bgBitmap: Bitmap? = null
                        dialBgWidth = binding.etW.text.toString().toInt()
                        dailBgHeight = binding.etH.text.toString().toInt()
                        if (dialBgWidth <= 0 || dailBgHeight <= 0) {
                            ToastUtils.showShort("请先输入表盘高宽 Please enter the height and width of the dial first")
                            return
                        }
                        if (::dialBgFile.isInitialized){
                            bgBitmap = ImageUtils.scale(ConvertUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(dialBgFile)), dialBgWidth, dailBgHeight)
                            //处理圆角
                            bgBitmap = ImageUtils.toRoundCorner(bgBitmap, ControlBleTools.getInstance().berryAlbumRadius * 1.0f)
                        }
                        ControlBleTools.getInstance().startUploadDialBigDataByBerry(
                            true,
                            FileIOUtils.readFile2BytesByStream(albumDialFile),
                            bgBitmap,
                            albumRequest,
                            dialUploadListener
                        )
                    } else {
                        //已安装 - 直接发送
                        val albumRequest = BerryAlbumWatchFaceEditRequestBean()
                        albumRequest.id = dialAlbumId
                        albumRequest.isSetCurrent = true
                        albumRequest.style = dialBgStyle //样式后台约定 1，2，3 ...
                        var bgBitmap: Bitmap? = null
                        dialBgWidth = binding.etW.text.toString().toInt()
                        dailBgHeight = binding.etH.text.toString().toInt()
                        if (dialBgWidth <= 0 || dailBgHeight <= 0) {
                            ToastUtils.showShort("请先输入表盘高宽 Please enter the height and width of the dial first")
                            return
                        }
                        if (::dialBgFile.isInitialized){
                            bgBitmap = ImageUtils.scale(ConvertUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(dialBgFile)), dialBgWidth, dailBgHeight)
                            //处理圆角
                            bgBitmap = ImageUtils.toRoundCorner(bgBitmap, ControlBleTools.getInstance().berryAlbumRadius * 1.0f)
                        }
                        ControlBleTools.getInstance().startUploadDialBigDataByBerry(
                            true,
                            FileIOUtils.readFile2BytesByStream(albumDialFile),
                            bgBitmap,
                            albumRequest,
                            dialUploadListener
                        )
                    }
                }
            } else {
                MyApplication.showToast(getString(R.string.s221) + ": " + statusName)
            }
        }

        override fun timeOut() {
            MyApplication.showToast(getString(R.string.s221) + ": timeOut !!! ")
        }

    }
    //endregion

    //region 表盘传输进度
    private val dialUploadListener: MyBerryDialUploadListener by lazy { MyBerryDialUploadListener() }

    inner class MyBerryDialUploadListener : BerryDialUploadListener {
        override fun onSuccess(code: Int) {
            //参考 BerryAlbumDialUploadListener.SucCode
            if (code == 1) {
                ToastUtils.showShort("背景图发送失败 Failed to send background image")
            }
            binding.llLog.addView(TextView(this@BerryDialActivity).apply {
                text = "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} :LargeFile --> onSuccess() ${code}"
                textSize = 10.0f
                setTextColor(Color.RED)
            }, 0)

        }

        override fun onProgress(curPiece: Int, dataPackTotalPieceLength: Int) {
            ThreadUtils.runOnUiThread {
                val percentage = curPiece * 100 / dataPackTotalPieceLength
                binding.llLog.addView(TextView(this@BerryDialActivity).apply {
                    text =
                        "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} :LargeFile --> cur = $curPiece, all = $dataPackTotalPieceLength, progress = $percentage%"
                    textSize = 10.0f
                    setTextColor(Color.RED)
                }, 0)
            }
        }

        override fun onTimeout(msg: String?) {
            binding.llLog.addView(TextView(this@BerryDialActivity).apply {
                text = "${TimeUtils.getNowString(TimeUtils.getSafeDateFormat("HH:mm:ss:SSS"))} :LargeFile --> onTimeout --> $msg"
                textSize = 10.0f
                setTextColor(Color.RED)
            }, 0)

            testLoop()
        }

    }

    private fun testLoop(isDel: Boolean = false) {
        if(isDialLoop || isAlbumDialLoop) {
            ThreadUtils.runOnUiThreadDelayed({
                try {
                    if (isDel && ControlBleTools.getInstance().isConnect) {
                        ControlBleTools.getInstance().deleteDeviceWatchFromId(watchFaceList.last().id, baseSendCmdStateListener)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                ThreadUtils.runOnUiThreadDelayed({
                    if (isDialLoop) {
                        binding.btnDial.callOnClick()
                    } else if (isAlbumDialLoop) {
                        binding.btnAlbumDial.callOnClick()
                    }
                }, 2000)

            }, 3000)
        }
    }

    //endregion
}