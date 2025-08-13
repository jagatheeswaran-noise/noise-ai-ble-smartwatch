package com.zjw.sdkdemo.function.diydial

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.*
import com.bumptech.glide.Glide
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.DiyWatchFaceConfigBean
import com.zhapp.ble.bean.WatchFaceInstallResultBean
import com.zhapp.ble.bean.diydial.OldDiyParamsBean
import com.zhapp.ble.callback.*
import com.zhapp.ble.custom.DiyDialUtils
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivityDiyDialBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.utils.AssetUtils
import com.zjw.sdkdemo.utils.LoadingDialog
import java.util.*

/**
 * Created by Android on 2022/12/19.
 */
class OldDiyDialActivity : BaseActivity() {
    //diy表盘描述对象
    private var dataJson = ""

    private val photoData = mutableListOf<PhotoBean>()
    private var photoSelectBitmap: Bitmap? = null

    private val styles = mutableListOf<StyleBean>()
    private var styleSelect: StyleBean? = null


    private var diyWatchFaceConfig: DiyWatchFaceConfigBean? = null


    //功能选择跳转返回
    private lateinit var functionSelectResultLauncher: ActivityResultLauncher<Intent>

    private val binding by lazy { ActivityDiyDialBinding.inflate(layoutInflater) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "DIY"
        setContentView(binding.root)
        inits()
        initCallBack()
        event()
        activityResultRegister()
    }

    private fun activityResultRegister() {
        functionSelectResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            LogUtils.d("functionSelectResultLauncher: ${result.resultCode}")
            if (result.resultCode == Activity.RESULT_OK) {
                val json = result.data?.getStringExtra(FunctionSelectActivity.RESULT_DATA_TEXT)
                if (json != null) {
                    val configBean = GsonUtils.fromJson(json, DiyWatchFaceConfigBean.FunctionsConfig::class.java)
                    for (i in 0..diyWatchFaceConfig!!.functionsConfigs!!.size) {
                        val info = diyWatchFaceConfig!!.functionsConfigs!!.get(i)
                        if (info.position == configBean.position) {
                            val configs = diyWatchFaceConfig!!.functionsConfigs as MutableList
                            configs[i] = configBean
                            diyWatchFaceConfig!!.functionsConfigs = configs
                            break
                        }
                    }
                    refFunctionsAdapter()
                    refPreView()
                }
            }
        }
    }

    private fun inits() {
        dataJson = ResourceUtils.readAssets2String("diy_dial/watch.json")
        //自定义背景
        photoData.apply {
            //TODO 图片资源高宽必须和表盘高宽一致 demo中为 410*502
            add(PhotoBean(AssetUtils.getAssetBitmap(this@OldDiyDialActivity, "diy_dial/background/background.png"), true))
            add(PhotoBean(AssetUtils.getAssetBitmap(this@OldDiyDialActivity, "diy_photo/photo_1.png"), false))
            add(PhotoBean(AssetUtils.getAssetBitmap(this@OldDiyDialActivity, "diy_photo/photo_2.png"), false))
            add(PhotoBean(AssetUtils.getAssetBitmap(this@OldDiyDialActivity, "diy_photo/photo_3.png"), false))
            add(PhotoBean(AssetUtils.getAssetBitmap(this@OldDiyDialActivity, "diy_photo/photo_4.png"), false))
            add(PhotoBean(AssetUtils.getAssetBitmap(this@OldDiyDialActivity, "diy_photo/photo_5.png"), false))
            add(PhotoBean(AssetUtils.getAssetBitmap(this@OldDiyDialActivity, "diy_photo/photo_6.png"), false))
            //这是个错误资源 ！！！
            add(PhotoBean(ConvertUtils.drawable2Bitmap(ContextCompat.getDrawable(this@OldDiyDialActivity, R.mipmap.ic_launcher)), false))
        }
        binding.rvPhoto.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPhoto.adapter = PhotoAdapter(this@OldDiyDialActivity, photoData) { selectedP ->
            photoSelectBitmap = photoData.get(selectedP).imgBitmap
            refPreView()
        }

        try {
            //指针
            styles.add(
                StyleBean(
                    OldDiyParamsBean.StyleResBean.StyleType.POINTER.type,
                    ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/pointer/3101_IMG.png"))),
                    ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/pointer/3101_Overlay.png"))),
                    ConvertUtils.inputStream2Bytes(assets.open("diy_dial/pointer/3101_Data.bin")), false
                )
            )
            styles.add(
                StyleBean(
                    OldDiyParamsBean.StyleResBean.StyleType.POINTER.type,
                    ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/pointer/3102_IMG.png"))),
                    ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/pointer/3102_Overlay.png"))),
                    ConvertUtils.inputStream2Bytes(assets.open("diy_dial/pointer/3102_Data.bin")), false
                )
            )
            styles.add(
                StyleBean(
                    OldDiyParamsBean.StyleResBean.StyleType.POINTER.type,
                    ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/pointer/3103_IMG.png"))),
                    ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/pointer/3103_Overlay.png"))),
                    ConvertUtils.inputStream2Bytes(assets.open("diy_dial/pointer/3103_Data.bin")), false
                )
            )
            //数字
            styles.add(
                StyleBean(
                    OldDiyParamsBean.StyleResBean.StyleType.NUMBER.type,
                    ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/time/3001_IMG.png"))),
                    ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/time/3001_Overlay.png"))),
                    ConvertUtils.inputStream2Bytes(assets.open("diy_dial/time/3001_Data.bin")), false
                )
            )
            styles.add(
                StyleBean(
                    OldDiyParamsBean.StyleResBean.StyleType.NUMBER.type,
                    ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/time/3002_IMG.png"))),
                    ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/time/3002_Overlay.png"))),
                    ConvertUtils.inputStream2Bytes(assets.open("diy_dial/time/3002_Data.bin")), false
                )
            )
            styles.add(
                StyleBean(
                    OldDiyParamsBean.StyleResBean.StyleType.NUMBER.type,
                    ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/time/3003_IMG.png"))),
                    ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/time/3003_Overlay.png"))),
                    ConvertUtils.inputStream2Bytes(assets.open("diy_dial/time/3003_Data.bin")), false
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //指针默认选中
        styles.get(0).isSelected = true
        styleSelect = styles.get(0)

        binding.rvStyle.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvStyle.adapter = StyleAdapter(this@OldDiyDialActivity, styles) { selectedP ->
            styleSelect = styles.get(selectedP)
            refPreView()
        }

        //复杂功能
        diyWatchFaceConfig = ControlBleTools.getInstance().getDefDiyWatchFaceConfig(dataJson)

        if (diyWatchFaceConfig != null && diyWatchFaceConfig!!.functionsConfigs != null) {
            binding.rvComplex.apply {
                layoutManager = LinearLayoutManager(this@OldDiyDialActivity, LinearLayoutManager.VERTICAL, false)
                addItemDecoration(DividerItemDecoration(this.context, LinearLayoutManager.VERTICAL))
                adapter = FunctionsAdapter(this@OldDiyDialActivity, diyWatchFaceConfig!!.functionsConfigs!!) { clickPosition ->
                    val info = diyWatchFaceConfig!!.functionsConfigs!!.get(clickPosition)
                    val details = info.functionsConfigTypes
                    if (details.isNullOrEmpty()) {
                        return@FunctionsAdapter
                    }
                    val intent = Intent(this@OldDiyDialActivity, FunctionSelectActivity::class.java)
                    intent.putExtra(FunctionSelectActivity.ACTIVITY_DATA_TEXT, GsonUtils.toJson(info))
                    functionSelectResultLauncher.launch(intent)
                }
            }
        }
        refPreView()

        //获取设备表盘功能状态
        ControlBleTools.getInstance().getDiyWatchFaceConfig(diyWatchFaceConfig?.id, object : ParsingStateManager.SendCmdStateListener() {
            override fun onState(state: SendCmdState?) {
                when (state) {
                    SendCmdState.SUCCEED -> MyApplication.showToast(R.string.s220)
                    else -> MyApplication.showToast(R.string.s221)
                }
            }
        })
    }

    private fun initCallBack() {
        /**
         * 获取diy表盘配置相关回调
         */
        CallBackUtils.diyWatchFaceCallBack = object : DiyWatchFaceCallBack {
            override fun onDiyWatchFaceStatus(config: DiyWatchFaceConfigBean?) {
                //根据 DiyWatchFaceConfigBean更新UI
                if (config != null) {
                    LogUtils.d("config:" + GsonUtils.toJson(config))
                    //复杂功能为空时，取默认配置类的复杂功能设置
                    if (config.functionsConfigs == null || config.functionsConfigs.isEmpty()) {
                        val cfConfig = diyWatchFaceConfig?.functionsConfigs
                        diyWatchFaceConfig = config
                        diyWatchFaceConfig?.functionsConfigs = cfConfig
                    } else {
                        diyWatchFaceConfig = config
                    }
                    //刷新复杂功能
                    refFunctionsAdapter()
                    //刷新背景
                    if (diyWatchFaceConfig?.backgroundFileConfig != null && !diyWatchFaceConfig!!.backgroundFileConfig.watchFaceFiles.isNullOrEmpty()) {
                        var selectedMd5 = ""
                        for (faceFile in diyWatchFaceConfig!!.backgroundFileConfig!!.watchFaceFiles) {
                            if (faceFile.fileNumber == diyWatchFaceConfig!!.backgroundFileConfig!!.usedFileNumber) {
                                selectedMd5 = faceFile.fileMd5
                                Log.d("DDQ", "background selectedMd5:$selectedMd5")
                            }
                        }
                        for (photo in photoData) {
                            Log.d("DDQ", "background Md5:${DiyDialUtils.getDiyBitmapMd5String(photo.imgBitmap)}")
                            photo.isSelected = TextUtils.equals(selectedMd5, DiyDialUtils.getDiyBitmapMd5String(photo.imgBitmap))
                            if (photo.isSelected) photoSelectBitmap = photo.imgBitmap
                        }
                    }
                    refPhotoAdapter()
                    //刷新指针或者数字
                    if (diyWatchFaceConfig?.pointerFileConfig != null && !diyWatchFaceConfig!!.pointerFileConfig.watchFaceFiles.isNullOrEmpty()) {
                        var selectedMd5 = ""
                        for (faceFile in diyWatchFaceConfig!!.pointerFileConfig!!.watchFaceFiles) {
                            if (faceFile.fileNumber == diyWatchFaceConfig!!.pointerFileConfig!!.usedFileNumber) {
                                selectedMd5 = faceFile.fileMd5
                                Log.d("DDQ", "pointer selectedMd5:$selectedMd5")
                            }
                        }
                        for (style in styles) {
                            style.isSelected = TextUtils.equals(selectedMd5, DiyDialUtils.getDiyBinBytesMd5(style.binData))
                            if (style.isSelected) styleSelect = style
                            Log.d("DDQ", "pointer md5:${DiyDialUtils.getDiyBinBytesMd5(style.binData)}")
                        }
                    }
                    if (diyWatchFaceConfig?.numberFileConfig != null && !diyWatchFaceConfig!!.numberFileConfig.watchFaceFiles.isNullOrEmpty()) {
                        var selectedMd5 = ""
                        for (faceFile in diyWatchFaceConfig!!.numberFileConfig!!.watchFaceFiles) {
                            if (faceFile.fileNumber == diyWatchFaceConfig!!.numberFileConfig!!.usedFileNumber) {
                                selectedMd5 = faceFile.fileMd5
                                Log.d("DDQ", "number selectedMd5:$selectedMd5")
                            }
                        }
                        for (style in styles) {
                            style.isSelected = TextUtils.equals(selectedMd5, DiyDialUtils.getDiyBinBytesMd5(style.binData))
                            if (style.isSelected) styleSelect = style
                            Log.d("DDQ", "number md5:${DiyDialUtils.getDiyBinBytesMd5(style.binData)}")
                        }

                    }
                    refStyleAdapter()

                    refPreView()
                }
            }
        }

        /**
         * 表盘文件安装结果回调
         */
        CallBackUtils.watchFaceInstallCallBack = object : WatchFaceInstallCallBack {
            override fun onresult(result: WatchFaceInstallResultBean?) {
                if (progressDialog != null && progressDialog!!.isShowing) {
                    progressDialog!!.dismiss()
                }
                if (result != null) {
                    if (result.code == WatchFaceInstallCallBack.WatchFaceInstallCode.INSTALL_SUCCESS.state) {
                        if (result?.diyWatchFaceConfigBean != null) {
                            diyWatchFaceConfig = result.diyWatchFaceConfigBean
                        }
                    }
                }

            }
        }
    }

    private fun event() {

        binding.btnSync.setOnClickListener {
            loadingDialog = LoadingDialog.show(this)
            ControlBleTools.getInstance().getOldDiyDialData(getDiyParamsBean(), object : DiyDialDataCallBack {
                override fun onDialData(diyDialId: String?, data: ByteArray?, configBean: DiyWatchFaceConfigBean?) {
                    if (diyDialId == null || data == null || configBean == null) {
                        MyApplication.showToast(R.string.s221)
                        if (loadingDialog != null && loadingDialog!!.isShowing) loadingDialog!!.dismiss()
                        return
                    }

                    LogUtils.d("diyDialId:" + diyDialId + ",Data size = ${data.size}" + "configBean:" + GsonUtils.toJson(configBean))
                    //需要更新文件和配置类
                    uploadWatch(diyDialId, data, configBean)
                }

                override fun onChangeConfig(configBean: DiyWatchFaceConfigBean?) {
                    if (configBean == null) {
                        MyApplication.showToast(R.string.s221)
                        if (loadingDialog != null && loadingDialog!!.isShowing) loadingDialog!!.dismiss()
                        return
                    }

                    //只需要更新配置类
                    ControlBleTools.getInstance().setDiyWatchFaceConfig(configBean, object : ParsingStateManager.SendCmdStateListener() {
                        override fun onState(state: SendCmdState?) {
                            if (loadingDialog != null && loadingDialog!!.isShowing) loadingDialog!!.dismiss()
                            when (state) {
                                SendCmdState.SUCCEED -> {
                                    MyApplication.showToast(R.string.s220)

                                    diyWatchFaceConfig = configBean
                                }
                                else -> MyApplication.showToast(R.string.s221)
                            }
                        }
                    })
                }

                override fun onError(errMsg: String?) {
                    errMsg?.let { MyApplication.showToast(it) }
                    if (loadingDialog != null && loadingDialog!!.isShowing) {
                        loadingDialog!!.dismiss()
                    }
                }
            })
        }

    }

    //region 传输表盘
    private var loadingDialog: Dialog? = null
    private fun uploadWatch(watchId: String, data: ByteArray, configBean: DiyWatchFaceConfigBean) {
        if (!ControlBleTools.getInstance().isConnect) {
            if (loadingDialog != null && loadingDialog!!.isShowing) {
                loadingDialog!!.dismiss()
            }
            return
        }
        ControlBleTools.getInstance().getDeviceDiyWatchFace(watchId, data.size, true,configBean, object : DeviceWatchFaceFileStatusListener {
            override fun onSuccess(statusValue: Int, statusName: String) {
                if (loadingDialog != null && loadingDialog!!.isShowing) {
                    loadingDialog!!.dismiss()
                }
                if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.READY.state) {
                    sendWatchData(data,configBean)
                } else if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.BUSY.state) {
                    MyApplication.showToast(getString(R.string.s223))
                } else if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.DUPLICATED.state) {
                    MyApplication.showToast(getString(R.string.s224))
                } else if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.LOW_STORAGE.state) {
                    MyApplication.showToast(getString(R.string.s224))
                } else if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.LOW_BATTERY.state) {
                    MyApplication.showToast(getString(R.string.s225))
                } else if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.DOWNGRADE.state) {
                    MyApplication.showToast(getString(R.string.s224))
                }
            }

            override fun timeOut() {
                MyApplication.showToast("timeOut")
                if (loadingDialog != null && loadingDialog!!.isShowing) {
                    loadingDialog!!.dismiss()
                }
            }
        })
    }

    private fun sendWatchData(data: ByteArray, configBean: DiyWatchFaceConfigBean) {
        showDialog()
        ControlBleTools.getInstance().startUploadBigData(
            BleCommonAttributes.UPLOAD_BIG_DATA_WATCH,
            data, true, object : UploadBigDataListener {
                override fun onSuccess() {
                    //TODO 监听安装回调
                    if (progressDialog != null && progressDialog!!.isShowing) {
                        progressDialog!!.dismiss()
                    }
                    diyWatchFaceConfig = configBean
                }

                override fun onProgress(curPiece: Int, dataPackTotalPieceLength: Int) {
                    val progress = curPiece * 100 / dataPackTotalPieceLength
                    progressBar?.progress = progress
                    tvDeviceUpdateProgress?.setText("${progress}%")
                }

                override fun onTimeout(msg:String?) {
                    Log.e("PhotoDialActivity", "startUploadBigData timeOut")
                    if (progressDialog != null && progressDialog!!.isShowing) {
                        progressDialog!!.dismiss()
                    }
                }
            })
    }

    private var progressDialog: Dialog? = null
    var msg: TextView? = null
    var tvDeviceUpdateProgress: TextView? = null
    private var progressBar: ProgressBar? = null

    private fun showDialog() {
        if (progressDialog == null) {
            progressDialog = Dialog(this, R.style.progress_dialog)
            progressDialog!!.setContentView(R.layout.update_layout)
            progressDialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        }
        msg = progressDialog!!.findViewById<View>(R.id.tvLoading) as TextView
        progressBar = progressDialog!!.findViewById(R.id.progressBar)
        tvDeviceUpdateProgress = progressDialog!!.findViewById<TextView>(R.id.tvDeviceUpdateProgress)
        progressBar?.setVisibility(View.VISIBLE)
        progressDialog!!.show()
        progressDialog!!.setCanceledOnTouchOutside(false)
        progressDialog!!.setOnDismissListener { dialog: DialogInterface? -> }
    }
    //endregion

    //region 图片选择适配器
    data class PhotoBean(var imgBitmap: Bitmap, var isSelected: Boolean)

    class PhotoAdapter(private val context: Context, private val data: List<PhotoBean>, var selected: (postion: Int) -> Unit) :
        RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            return PhotoViewHolder(
                LayoutInflater.from(context).inflate(R.layout.item_diy_photo, parent, false)
            )
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val photoBean = data.get(position)
            if (photoBean.imgBitmap != null) {
                Glide.with(context).load(photoBean.imgBitmap).into(holder.ivIcon)
            } else {
                Glide.with(context).load(R.mipmap.sport_share_custom_camera).into(holder.ivIcon)
            }
            holder.ivSelected.visibility = if (photoBean.isSelected) View.VISIBLE else View.GONE
            holder.rootlayout.setOnClickListener {

                if (!photoBean.isSelected) {
                    data.forEach { it.isSelected = false }
                    data.get(position).isSelected = true
                    notifyDataSetChanged()
                    selected(position)
                }

            }
        }

        override fun getItemCount(): Int = data.size


        inner class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var ivIcon: AppCompatImageView = view.findViewById(R.id.iv_icon)
            var ivSelected: AppCompatImageView = view.findViewById(R.id.iv_selected)
            val rootlayout: ConstraintLayout = view.findViewById(R.id.root_layout)
        }
    }
    //endregion

    //region 指针、数字适配器
    data class StyleBean(var type: Int, var img: Bitmap, var imgData: Bitmap, var binData: ByteArray, var isSelected: Boolean)

    class StyleAdapter(private val context: Context, private val data: List<StyleBean>, var selected: (postion: Int) -> Unit) :
        RecyclerView.Adapter<StyleAdapter.PointerViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PointerViewHolder {
            return PointerViewHolder(
                LayoutInflater.from(context).inflate(R.layout.item_diy_photo, parent, false)
            )
        }

        override fun onBindViewHolder(holder: PointerViewHolder, position: Int) {
            val photoBean = data.get(position)
            if (photoBean.img != null) {
                Glide.with(context).load(photoBean.img).into(holder.ivIcon)
            } else {
                Glide.with(context).load(R.mipmap.sport_share_custom_camera).into(holder.ivIcon)
            }
            holder.ivSelected.visibility = if (photoBean.isSelected) View.VISIBLE else View.GONE
            holder.rootlayout.setOnClickListener {
                if (!photoBean.isSelected) {
                    data.forEach { it.isSelected = false }
                    data.get(position).isSelected = true
                    notifyDataSetChanged()
                    selected(position)
                }
            }
        }

        override fun getItemCount(): Int = data.size


        inner class PointerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var ivIcon: AppCompatImageView = view.findViewById(R.id.iv_icon)
            var ivSelected: AppCompatImageView = view.findViewById(R.id.iv_selected)
            val rootlayout: ConstraintLayout = view.findViewById(R.id.root_layout)
        }
    }
    //endregion

    //region 复杂功能

    class FunctionsAdapter(private val context: Context, var data: List<DiyWatchFaceConfigBean.FunctionsConfig>, var click: (postion: Int) -> Unit) :
        RecyclerView.Adapter<FunctionsAdapter.FunctionsViewHolder>() {


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FunctionsViewHolder {
            return FunctionsViewHolder(
                LayoutInflater.from(context).inflate(R.layout.item_complex, parent, false)
            )
        }

        override fun onBindViewHolder(holder: FunctionsViewHolder, position: Int) {
            val info = data.get(position)
            holder.tvLocation.text = MyDiyDialUtils.getFunctionsLocationNameByType(context, info.position)

            holder.tvContext.text = MyDiyDialUtils.getFunctionsDetailNameByType(context, info.typeChoose)

            ClickUtils.applySingleDebouncing(holder.rootLayout) {
                click(position)
            }
        }

        override fun getItemCount(): Int = data.size


        inner class FunctionsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var rootLayout: LinearLayout = view.findViewById(R.id.rootLayout)
            var tvLocation: TextView = view.findViewById(R.id.tvLocation)
            var tvContext: TextView = view.findViewById(R.id.tvContext)
        }
    }

    //endregion

    @SuppressLint("NotifyDataSetChanged")
    private fun refFunctionsAdapter() {
        (binding.rvComplex.adapter as FunctionsAdapter?)?.let {
            it.data = diyWatchFaceConfig!!.functionsConfigs
            it.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refPhotoAdapter() {
        (binding.rvPhoto.adapter as PhotoAdapter?)?.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refStyleAdapter() {
        (binding.rvStyle.adapter as StyleAdapter?)?.notifyDataSetChanged()
    }

    //region 预览
    private fun refPreView() {
        val diyRenderingBean = getDiyParamsBean()
        ControlBleTools.getInstance().getOldPreviewBitmap(diyRenderingBean, object : DiyDialPreviewCallBack {
            override fun onDialPreview(preview: Bitmap?) {
                if (preview != null) {
                    Glide.with(this@OldDiyDialActivity).load(preview).into(binding.ivPreview)
                }
            }

            override fun onError(errMsg: String?) {
                errMsg?.let { MyApplication.showToast(it) }
            }
        })
    }
    //endregion

    /**
     * 获取diy表盘请求参数
     */
    private fun getDiyParamsBean(): OldDiyParamsBean {
        val diyParamsBean = OldDiyParamsBean()

        //json
        diyParamsBean.jsonStr = dataJson

        //背景资源
        val backgroundResBean = OldDiyParamsBean.BackgroundResBean()
        val background: Bitmap =
            if (photoSelectBitmap == null) {
                //默认背景
                ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/background/background.png")))
            } else {
                //本地资源
                photoSelectBitmap!!
                //TODO 相册/相机
            }
        val backgroundOverlay = ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/background/overlay.png")))
        backgroundResBean.background = background
        backgroundResBean.backgroundOverlay = backgroundOverlay
        diyParamsBean.backgroundResBean = backgroundResBean

        //指针
        if (styleSelect != null) {
            val styleResBean = OldDiyParamsBean.StyleResBean()
            styleResBean.type = styleSelect!!.type
            styleResBean.styleBm = styleSelect!!.imgData
            styleResBean.styleBin = styleSelect!!.binData
            diyParamsBean.styleResBean = styleResBean
        }

        //复杂功能
        val functionsResBean = OldDiyParamsBean.FunctionsResBean()
        functionsResBean.functionsBin = ConvertUtils.inputStream2Bytes(assets.open("diy_dial/complex/complex.bin"))
        val functionsBitmapBeans = mutableListOf<OldDiyParamsBean.FunctionsResBean.FunctionsBitmapBean>()
        var functionsBitmapBean = OldDiyParamsBean.FunctionsResBean.FunctionsBitmapBean()
        functionsBitmapBean.bitmap = ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/complex/calorie.png")))
        functionsBitmapBean.function = DiyWatchFaceCallBack.DiyWatchFaceFunction.CALORIE.function
        functionsBitmapBeans.add(functionsBitmapBean)
        functionsBitmapBean = OldDiyParamsBean.FunctionsResBean.FunctionsBitmapBean()
        functionsBitmapBean.bitmap = ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/complex/generaldate.png")))
        functionsBitmapBean.function = DiyWatchFaceCallBack.DiyWatchFaceFunction.GENERAL_DATE.function
        functionsBitmapBeans.add(functionsBitmapBean)
        functionsBitmapBean = OldDiyParamsBean.FunctionsResBean.FunctionsBitmapBean()
        functionsBitmapBean.bitmap = ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/complex/kwh.png")))
        functionsBitmapBean.function = DiyWatchFaceCallBack.DiyWatchFaceFunction.BATTERY.function
        functionsBitmapBeans.add(functionsBitmapBean)
        functionsBitmapBean = OldDiyParamsBean.FunctionsResBean.FunctionsBitmapBean()
        functionsBitmapBean.bitmap = ConvertUtils.bytes2Bitmap(ConvertUtils.inputStream2Bytes(assets.open("diy_dial/complex/step.png")))
        functionsBitmapBean.function = DiyWatchFaceCallBack.DiyWatchFaceFunction.STEP.function
        functionsBitmapBeans.add(functionsBitmapBean)
        functionsResBean.functionsBitmaps = functionsBitmapBeans
        //复杂功能资源
        diyParamsBean.functionsResBean = functionsResBean
        //复杂功能设置
        diyParamsBean.diyWatchFaceConfigBean = diyWatchFaceConfig
        return diyParamsBean
    }

}