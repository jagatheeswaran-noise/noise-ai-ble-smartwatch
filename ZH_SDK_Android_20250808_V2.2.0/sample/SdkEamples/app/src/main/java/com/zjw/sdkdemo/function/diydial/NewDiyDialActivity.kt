package com.zjw.sdkdemo.function.diydial

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.flag.BubbleFlag
import com.skydoves.colorpickerview.flag.FlagMode
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.skydoves.colorpickerview.preference.ColorPickerPreferenceManager
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.DiyWatchFaceConfigBean
import com.zhapp.ble.bean.WatchFaceInstallResultBean
import com.zhapp.ble.bean.diydial.NewDiyParamsBean
import com.zhapp.ble.bean.diydial.NewZhDiyDialBean
import com.zhapp.ble.callback.*
import com.zhapp.ble.custom.DiyDialUtils
import com.zhapp.ble.custom.DiyDialUtils.getColorByRGBValue
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivityDiyDialBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.utils.AssetUtils
import com.zjw.sdkdemo.utils.LoadingDialog
import java.io.File

/**
 * Created by Android on 2022/12/19.
 */
class NewDiyDialActivity : BaseActivity() {
    companion object {
        const val TAG = "DIY"

        const val EX_FILE = "file"

        const val COLOR_PICKER_NAME = "MyColorPickerDialog"
    }

    private var diyFilePath: String? = ""

    //diy 表盘样式 ： 1：数字 ， 2：指针
    private var diyType = 1

    //diy表盘描述对象
    private var dataJson = ""

    //背景
    private val photoData = mutableListOf<PhotoBean>()
    private var photoSelectBitmap = mutableListOf<Bitmap>()

    //背景覆盖图
    private val overlayData = mutableListOf<OverlayBean>()
    private var overlaySelectBitmap: Bitmap? = null

    //指针
    private val styles = mutableListOf<StyleBean>()
    private var styleSelect: StyleBean? = null

    //数字样式 数字位置
    private val numberFonts = mutableListOf<NumberFontBean>()
    private val numberLocationBeans = mutableListOf<NumberLocationBean>()
    private var numberSelect: NumberLocationBean? = null
    private var numberSelectPosition = 0

    //选中的颜色 默认白色
    private var selectedColors: MutableList<IntArray> = mutableListOf<IntArray>().apply {
        add(intArrayOf(255, 255, 255))
    }

    private var diyWatchFaceConfig: DiyWatchFaceConfigBean? = null

    //功能选择跳转返回
    private lateinit var functionSelectResultLauncher: ActivityResultLauncher<Intent>

    private val binding by lazy { ActivityDiyDialBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s361)
        setContentView(binding.root)
        diyFilePath = intent.getStringExtra(EX_FILE)
        if (diyFilePath == null) {
            ToastUtils.showShort(getString(R.string.s512))
            finish()
            return
        }
        initView()
        initCallBack()
        event()
        activityResultRegister()
    }

    private fun initView() {
        for (f in FileUtils.listFilesInDir(diyFilePath)) {
            if (FileUtils.isDir(f) && TextUtils.equals(f.name, "pointer")) {
                if (!FileUtils.listFilesInDir(f).isNullOrEmpty()) {
                    diyType = 2
                }
            }
            if (FileUtils.isDir(f) && TextUtils.equals(f.name, "time")) {
                if (!FileUtils.listFilesInDir(f).isNullOrEmpty()) {
                    diyType = 1
                }
            }
        }
        if (diyType == 1) {
            initNumbers()  //数字资源
        } else if (diyType == 2) {
            initPointer()  //指针资源
        } else {
            initNumbers()  //数字资源
        }
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

    @SuppressLint("NotifyDataSetChanged")
    private fun initNumbers() {
        try {
            for (f in FileUtils.listFilesInDir(diyFilePath)) {
                if (TextUtils.equals(f.name, "watch.json")) {
                    dataJson = FileIOUtils.readFile2String(f)
                    break
                }
            }

            val diyDialBean: NewZhDiyDialBean? = GsonUtils.fromJson<NewZhDiyDialBean>(dataJson, NewZhDiyDialBean::class.java)
            if (diyDialBean == null) {
                ToastUtils.showShort(getString(R.string.s512))
                finish()
                return
            }

            //region 背景图 可自定义 UI必须存在
            photoData.apply {
                // 图片资源高宽必须和表盘高宽一致
                add(
                    PhotoBean(
                        ConvertUtils.bytes2Bitmap(
                            FileIOUtils.readFile2BytesByStream(
                                diyFilePath + File.separator + diyDialBean.background.backgroundImgPath.replace("\\", File.separator)
                            )
                        ), true
                    )
                )
                add(PhotoBean(AssetUtils.getAssetBitmap(this@NewDiyDialActivity, "diy_photo/photo_1.png"), false))
                add(PhotoBean(AssetUtils.getAssetBitmap(this@NewDiyDialActivity, "diy_photo/photo_2.png"), false))
                add(PhotoBean(AssetUtils.getAssetBitmap(this@NewDiyDialActivity, "diy_photo/photo_3.png"), false))
                add(PhotoBean(AssetUtils.getAssetBitmap(this@NewDiyDialActivity, "diy_photo/photo_4.png"), false))
                add(PhotoBean(AssetUtils.getAssetBitmap(this@NewDiyDialActivity, "diy_photo/photo_5.png"), false))
                add(PhotoBean(AssetUtils.getAssetBitmap(this@NewDiyDialActivity, "diy_photo/photo_6.png"), false))
            }
            photoSelectBitmap.clear()
            photoSelectBitmap.add(photoData[0].imgBitmap)
            binding.rvPhoto.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.rvPhoto.adapter = PhotoAdapter(this@NewDiyDialActivity, photoData) { addP, delP ->
                if (addP != -1) {
                    photoSelectBitmap.add(photoData[addP].imgBitmap)
                    selectedColors.add(intArrayOf(255, 255, 255))
                }
                if (delP != -1) {
                    if (photoSelectBitmap.size > 1) {
                        photoSelectBitmap.remove(photoData[delP].imgBitmap)
                        if (delP < selectedColors.size) {
                            if (delP < selectedColors.size) {
                                selectedColors.removeAt(delP)
                            }
                        }
                    }
                }
                refFixedColorPickerItem()
                refPreView()
            }
            //endregion

            //region 背景覆盖图 读取资源配置，不存在隐藏UI
            if (diyDialBean.overlayImgPaths != null) {
                overlayData.apply {
                    //TODO 更换数据格式
                    for (o in diyDialBean.overlayImgPaths) {
                        add(
                            OverlayBean(
                                ConvertUtils.bytes2Bitmap(
                                    FileIOUtils.readFile2BytesByStream(
                                        diyFilePath + File.separator + o.replace("\\", File.separator)
                                    )
                                ), false
                            )
                        )
                    }
                }
            }
            if (overlayData.size > 0) {
                binding.rvOverlay.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                binding.rvOverlay.adapter = OverlayAdapter(this@NewDiyDialActivity, overlayData) { selectedP ->
                    overlaySelectBitmap = overlayData.get(selectedP).imgBitmap
                    refPreView()
                }
                //背景覆盖图默认选中
                overlayData.get(0).isSelected = true
                overlaySelectBitmap = overlayData.get(0).imgBitmap
            } else {
                binding.tvOverlay.visibility = View.GONE
                binding.rvOverlay.visibility = View.GONE
            }
            //endregion

            //region 数字样式 数字位置 颜色 读取资源配置，不存在隐藏UI
            if (!diyDialBean.time.isNullOrEmpty()) {
                numberFonts.apply {
                    for (time in diyDialBean.time) {
                        var binData: ByteArray = FileIOUtils.readFile2BytesByStream(diyFilePath + File.separator + time.timeDataPath.replace("\\", File.separator))
                        add(
                            NumberFontBean(
                                time.fontsName,
                                ConvertUtils.bytes2Bitmap(
                                    FileIOUtils.readFile2BytesByStream(
                                        diyFilePath + File.separator + time.locationInfos.get(0).timeImgPath.replace("\\", File.separator)
                                    )
                                ),
                                mutableListOf<NumberLocationBean>().apply {
                                    if (!time.locationInfos.isNullOrEmpty()) {
                                        for (location in time.locationInfos) {
                                            add(
                                                NumberLocationBean(
                                                    time.fontsName,
                                                    location.locationName,
                                                    ConvertUtils.bytes2Bitmap(
                                                        FileIOUtils.readFile2BytesByStream(
                                                            diyFilePath + File.separator + location.timeImgPath.replace("\\", File.separator)
                                                        )
                                                    ),
                                                    ConvertUtils.bytes2Bitmap(
                                                        FileIOUtils.readFile2BytesByStream(
                                                            diyFilePath + File.separator + location.timeOverlayPath.replace("\\", File.separator)
                                                        )
                                                    ),
                                                    binData,
                                                    false
                                                )
                                            )
                                        }
                                    }
                                },
                                false
                            )
                        )
                    }

                }
            }
            if (numberFonts.size > 0) {
                //默认选中
                numberFonts.get(0).isSelected = true
                numberFonts.get(0).locations.get(0).isSelected = true
                numberSelect = numberFonts.get(0).locations.get(0)
                numberSelectPosition = 0
                numberLocationBeans.addAll(numberFonts.get(0).locations)

                binding.rvNumberStyle.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                binding.rvNumberStyle.adapter = NumberStylerAdapter(this@NewDiyDialActivity, numberFonts) { selectedP ->
                    numberLocationBeans.clear()
                    numberLocationBeans.addAll(numberFonts.get(selectedP).locations)
                    if (numberSelectPosition < numberLocationBeans.size) {
                        //切换样式更新上次选中的位置
                        numberSelect = numberLocationBeans.get(numberSelectPosition)
                        for (i in 0 until numberLocationBeans.size) {
                            numberLocationBeans.get(i).isSelected = numberSelectPosition == i
                        }
                    } else {
                        //切换样式 上次选中的位置不存在默认选中第一位
                        numberSelectPosition = 0
                        numberSelect = numberLocationBeans.get(0)
                        for (i in 0 until numberLocationBeans.size) {
                            numberLocationBeans.get(i).isSelected = numberSelectPosition == i
                        }
                        //更新样式
                        val selecterLName = numberLocationBeans.get(numberSelectPosition).locationName
                        for (numberfont in numberFonts) {
                            numberfont.locations.firstOrNull { it.locationName == selecterLName }?.locationImg?.let {
                                numberfont.fontImg = it
                            }
                        }
                        binding.rvNumberStyle.adapter?.notifyDataSetChanged()
                    }
                    binding.rvNumberLocation.adapter?.notifyDataSetChanged()

                    refPreView()
                }
                binding.rvNumberLocation.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                binding.rvNumberLocation.adapter = NumberLocationAdapter(this@NewDiyDialActivity, numberLocationBeans) { selectedP ->
                    //切换位置 更新样式图
                    val selecterLName = numberLocationBeans.get(selectedP).locationName
                    for (numberfont in numberFonts) {
                        numberfont.locations.firstOrNull { it.locationName == selecterLName }?.locationImg?.let {
                            numberfont.fontImg = it
                        }
                    }
                    binding.rvNumberStyle.adapter?.notifyDataSetChanged()
                    numberSelect = numberLocationBeans.get(selectedP)
                    numberSelectPosition = selectedP
                    refPreView()
                }
                // 颜色选择器
                refFixedColorPickerItem()
            } else {
                binding.tvNumberStyle.visibility = View.GONE
                binding.rvNumberStyle.visibility = View.GONE
                binding.tvNumberLocation.visibility = View.GONE
                binding.rvNumberLocation.visibility = View.GONE
                binding.layoutCustomize.visibility = View.GONE
            }
            //endregion

            //region 数字颜色(与多背景对应)
            binding.rvbgColors.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            binding.rvbgColors.adapter = BgColorAdapter(this@NewDiyDialActivity, selectedColors)
            //endregion

            binding.tvStyle.visibility = View.GONE
            binding.rvStyle.visibility = View.GONE

            //复杂功能
            diyWatchFaceConfig = ControlBleTools.getInstance().getDefDiyWatchFaceConfig(dataJson)

            if (diyWatchFaceConfig != null && diyWatchFaceConfig!!.functionsConfigs != null) {
                binding.rvComplex.apply {
                    layoutManager = LinearLayoutManager(this@NewDiyDialActivity, LinearLayoutManager.VERTICAL, false)
                    addItemDecoration(DividerItemDecoration(this.context, LinearLayoutManager.VERTICAL))
                    adapter = FunctionsAdapter(this@NewDiyDialActivity, diyWatchFaceConfig!!.functionsConfigs!!) { clickPosition ->
                        val info = diyWatchFaceConfig!!.functionsConfigs!!.get(clickPosition)
                        val details = info.functionsConfigTypes
                        if (details.isNullOrEmpty()) {
                            return@FunctionsAdapter
                        }
                        val intent = Intent(this@NewDiyDialActivity, FunctionSelectActivity::class.java)
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
                        SendCmdState.SUCCEED -> MyApplication.showToast(com.zjw.sdkdemo.R.string.s220)
                        else -> MyApplication.showToast(com.zjw.sdkdemo.R.string.s221)
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort(getString(R.string.s512))
            finish()
            return
        }
    }


    private fun initPointer() {
        try {
            for (f in FileUtils.listFilesInDir(diyFilePath)) {
                if (TextUtils.equals(f.name, "watch.json")) {
                    dataJson = FileIOUtils.readFile2String(f)
                    break
                }
            }

            val diyDialBean: NewZhDiyDialBean? = GsonUtils.fromJson<NewZhDiyDialBean>(dataJson, NewZhDiyDialBean::class.java)
            if (diyDialBean == null) {
                ToastUtils.showShort(getString(R.string.s512))
                finish()
                return
            }


            //region 背景图 可自定义 UI必须存在
            photoData.apply {
                // 图片资源高宽必须和表盘高宽一致
                add(
                    PhotoBean(
                        ConvertUtils.bytes2Bitmap(
                            FileIOUtils.readFile2BytesByStream(
                                diyFilePath + File.separator + diyDialBean.background.backgroundImgPath.replace("\\", File.separator)
                            )
                        ), true
                    )
                )
                add(PhotoBean(AssetUtils.getAssetBitmap(this@NewDiyDialActivity, "diy_photo/photo_1.png"), false))
                add(PhotoBean(AssetUtils.getAssetBitmap(this@NewDiyDialActivity, "diy_photo/photo_2.png"), false))
                add(PhotoBean(AssetUtils.getAssetBitmap(this@NewDiyDialActivity, "diy_photo/photo_3.png"), false))
                add(PhotoBean(AssetUtils.getAssetBitmap(this@NewDiyDialActivity, "diy_photo/photo_4.png"), false))
                add(PhotoBean(AssetUtils.getAssetBitmap(this@NewDiyDialActivity, "diy_photo/photo_5.png"), false))
                add(PhotoBean(AssetUtils.getAssetBitmap(this@NewDiyDialActivity, "diy_photo/photo_6.png"), false))
            }
            photoSelectBitmap.clear()
            photoSelectBitmap.add(photoData[0].imgBitmap)
            binding.rvPhoto.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.rvPhoto.adapter = PhotoAdapter(this@NewDiyDialActivity, photoData) { addP, delP ->
                if (addP != -1) {
                    photoSelectBitmap.add(photoData[addP].imgBitmap)
                    selectedColors.add(intArrayOf(255, 255, 255))
                }
                if (delP != -1) {
                    if (photoSelectBitmap.size > 1) {
                        photoSelectBitmap.remove(photoData[delP].imgBitmap)
                        if (delP < selectedColors.size) {
                            selectedColors.removeAt(delP)
                        }
                    }
                }
                refFixedColorPickerItem()
                refPreView()
            }
            //endregion

            //region 背景覆盖图 读取资源配置，不存在隐藏UI
            if (diyDialBean.overlayImgPaths != null) {
                overlayData.apply {
                    //TODO 更换数据格式
                    for (o in diyDialBean.overlayImgPaths) {
                        add(
                            OverlayBean(
                                ConvertUtils.bytes2Bitmap(
                                    FileIOUtils.readFile2BytesByStream(
                                        diyFilePath + File.separator + o.replace("\\", File.separator)
                                    )
                                ), false
                            )
                        )
                    }
                }
            }
            if (overlayData.size > 0) {
                binding.rvOverlay.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                binding.rvOverlay.adapter = OverlayAdapter(this@NewDiyDialActivity, overlayData) { selectedP ->
                    overlaySelectBitmap = overlayData.get(selectedP).imgBitmap
                    refPreView()
                }
                //背景覆盖图默认选中
                overlayData.get(0).isSelected = true
                overlaySelectBitmap = overlayData.get(0).imgBitmap
            } else {
                binding.tvOverlay.visibility = View.GONE
                binding.rvOverlay.visibility = View.GONE
            }
            //endregion

            //region 数字样式 数字位置 颜色 读取资源配置，不存在隐藏UI
            binding.tvNumberStyle.visibility = View.GONE
            binding.rvNumberStyle.visibility = View.GONE
            binding.tvNumberLocation.visibility = View.GONE
            binding.rvNumberLocation.visibility = View.GONE
            binding.layoutCustomize.visibility = View.GONE
            //endregion

            //region 指针 读取资源配置，不存在隐藏UI
            if (!diyDialBean.pointers.isNullOrEmpty()) {
                styles.apply {
                    for (p in diyDialBean.pointers) {
                        add(
                            StyleBean(
                                ConvertUtils.bytes2Bitmap(
                                    FileIOUtils.readFile2BytesByStream(
                                        diyFilePath + File.separator + p.pointerImgPath.replace("\\", File.separator)
                                    )
                                ),
                                ConvertUtils.bytes2Bitmap(
                                    FileIOUtils.readFile2BytesByStream(
                                        diyFilePath + File.separator + p.pointerOverlayPath.replace("\\", File.separator)
                                    )
                                ),
                                FileIOUtils.readFile2BytesByStream(diyFilePath + File.separator + p.pointerDataPath.replace("\\", File.separator)),
                                false
                            )
                        )
                    }
                }
            }
            if (styles.size > 0) {
                binding.rvStyle.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                binding.rvStyle.adapter = StyleAdapter(this@NewDiyDialActivity, styles) { selectedP ->
                    styleSelect = styles.get(selectedP)
                    refPreView()
                }
                //指针默认选中
                styles.get(0).isSelected = true
                styleSelect = styles.get(0)
            } else {
                binding.tvStyle.visibility = View.GONE
                binding.rvStyle.visibility = View.GONE
            }
            //endregion

            //复杂功能
            diyWatchFaceConfig = ControlBleTools.getInstance().getDefDiyWatchFaceConfig(dataJson)

            if (diyWatchFaceConfig != null && diyWatchFaceConfig!!.functionsConfigs != null) {
                binding.rvComplex.apply {
                    layoutManager = LinearLayoutManager(this@NewDiyDialActivity, LinearLayoutManager.VERTICAL, false)
                    addItemDecoration(DividerItemDecoration(this.context, LinearLayoutManager.VERTICAL))
                    adapter = FunctionsAdapter(this@NewDiyDialActivity, diyWatchFaceConfig!!.functionsConfigs!!) { clickPosition ->
                        val info = diyWatchFaceConfig!!.functionsConfigs!!.get(clickPosition)
                        val details = info.functionsConfigTypes
                        if (details.isNullOrEmpty()) {
                            return@FunctionsAdapter
                        }
                        val intent = Intent(this@NewDiyDialActivity, FunctionSelectActivity::class.java)
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
                        SendCmdState.SUCCEED -> MyApplication.showToast(com.zjw.sdkdemo.R.string.s220)
                        else -> MyApplication.showToast(com.zjw.sdkdemo.R.string.s221)
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort(getString(R.string.s512))
            finish()
            return
        }

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
                    Log.d("config:", GsonUtils.toJson(config))
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
                    //刷新 背景 / 背景覆盖图
                    if (diyWatchFaceConfig?.backgroundFileConfig != null && !diyWatchFaceConfig!!.backgroundFileConfig.watchFaceFiles.isNullOrEmpty()) {
                        //先全部清空背景图选中
                        for (photo in photoData) {
                            photo.isSelected = false
                        }
                        photoSelectBitmap.clear()
                        selectedColors.clear()

                        var selectedOverlayMd5 = ""
                        for (faceFile in diyWatchFaceConfig!!.backgroundFileConfig!!.watchFaceFiles) {
                            Log.d(TAG, "background selectedMd5:${faceFile.fileMd5}")
                            for (photo in photoData) {
                                //Log.d(TAG, "background Md5:${DiyDialUtils.getDiyBitmapMd5String(photo.imgBitmap)}")
                                if (TextUtils.equals(faceFile.fileMd5, DiyDialUtils.getDiyBitmapMd5String(photo.imgBitmap))) {
                                    photo.isSelected = true
                                    photoSelectBitmap.add(photo.imgBitmap)
                                    selectedColors.add(DiyDialUtils.getColorByRGBValue(faceFile.backgroundColorHex))
                                }
                            }
                            if (faceFile.fileNumber == diyWatchFaceConfig!!.backgroundFileConfig!!.usedFileNumber) {
                                selectedOverlayMd5 = faceFile.backgroundOverlayMd5
                                Log.d(TAG, "background selectedOverlayMd5:$selectedOverlayMd5")
                            }
                        }

                        for (overlay in overlayData) {
                            Log.d(TAG, "overlay Md5:${DiyDialUtils.getDiyBitmapMd5String(overlay.imgBitmap)}")
                            overlay.isSelected = TextUtils.equals(selectedOverlayMd5, DiyDialUtils.getDiyBitmapMd5String(overlay.imgBitmap))
                            if (overlay.isSelected) overlaySelectBitmap = overlay.imgBitmap
                        }
                    }
                    refPhotoAdapter()
                    refOverlayAdapter()
                    refFixedColorPickerItem()
                    //刷新指针
                    if (diyWatchFaceConfig?.pointerFileConfig != null && !diyWatchFaceConfig!!.pointerFileConfig.watchFaceFiles.isNullOrEmpty()) {
                        var selectedMd5 = ""
                        for (faceFile in diyWatchFaceConfig!!.pointerFileConfig!!.watchFaceFiles) {
                            if (faceFile.fileNumber == diyWatchFaceConfig!!.pointerFileConfig!!.usedFileNumber) {
                                selectedMd5 = faceFile.fileMd5
                                Log.d(TAG, "pointer selectedMd5:$selectedMd5")
                            }
                        }
                        for (style in styles) {
                            style.isSelected = TextUtils.equals(selectedMd5, DiyDialUtils.getDiyBinBytesMd5(style.binData))
                            if (style.isSelected) styleSelect = style
                            Log.d(TAG, "pointer md5:${DiyDialUtils.getDiyBinBytesMd5(style.binData)}")
                        }
                    }
                    refStyleAdapter()
                    //刷新数字 字体 位置 颜色
                    if (diyWatchFaceConfig?.numberFileConfig != null && !diyWatchFaceConfig!!.numberFileConfig.watchFaceFiles.isNullOrEmpty()) {
                        var selectedNumberFontMd5 = ""
                        var selectedNumberLoncationMd5 = ""
                        var selectedNumberColor: IntArray = intArrayOf(255, 255, 255)
                        for (faceFile in diyWatchFaceConfig!!.numberFileConfig!!.watchFaceFiles) {
                            if (faceFile.fileNumber == diyWatchFaceConfig!!.numberFileConfig!!.usedFileNumber) {
                                selectedNumberFontMd5 = faceFile.numberFontMd5
                                selectedNumberLoncationMd5 = faceFile.numberLocationMd5
                                selectedNumberColor = DiyDialUtils.getColorByRGBValue(faceFile.numberColorHex)
                                Log.d(TAG, "number selectedNumberFontMd5:$selectedNumberFontMd5")
                                Log.d(TAG, "number selectedNumberLoncationMd5:$selectedNumberLoncationMd5")
                                Log.d(TAG, "number selectedNumberColor:${GsonUtils.toJson(selectedNumberColor)}")
                            }
                        }

                        var fontName = ""
                        var locationName = ""
                        for (font in numberFonts) {
                            for (location in font.locations) {
                                if (TextUtils.equals(selectedNumberLoncationMd5, DiyDialUtils.getDiyBitmapMd5String(location.locationImg))) {
                                    fontName = location.fontName
                                    locationName = location.locationName
                                    Log.d(TAG, "number fontName:$fontName,locationName:$locationName")

                                }
                            }
                        }
                        for (numberfont in numberFonts) {
                            numberfont.locations.firstOrNull { it.locationName == locationName }?.locationImg?.let {
                                numberfont.fontImg = it
                            }
                            if (TextUtils.equals(numberfont.fontName, fontName)) {
                                numberfont.isSelected = true
                                numberLocationBeans.clear()
                                numberLocationBeans.addAll(numberfont.locations)
                                for (nl in numberLocationBeans) {
                                    if (TextUtils.equals(nl.locationName, locationName)) {
                                        nl.isSelected = true
                                        numberSelect = nl
                                        numberSelectPosition = numberLocationBeans.indexOf(nl)
                                    } else {
                                        nl.isSelected = false
                                    }

                                }
                            } else {
                                numberfont.isSelected = false
                            }
                        }
                        refNumberFontAndLocation()
                        //赋值给第一个背景对应色
                        selectedColors.get(0)[0] = selectedNumberColor[0]
                        selectedColors.get(0)[1] = selectedNumberColor[1]
                        selectedColors.get(0)[2] = selectedNumberColor[2]
                        refFixedColorPickerItem()
                    }
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
                    when (result?.code) {
                        WatchFaceInstallCallBack.WatchFaceInstallCode.INSTALL_SUCCESS.state -> {
                            //安装成功
                            MyApplication.showToast(R.string.s497)
                        }

                        WatchFaceInstallCallBack.WatchFaceInstallCode.INSTALL_FAILED.state -> {
                            //安装失败
                            MyApplication.showToast(R.string.s498)
                        }

                        WatchFaceInstallCallBack.WatchFaceInstallCode.VERIFY_FAILED.state -> {
                            //验证失败
                            MyApplication.showToast(R.string.s499)
                        }
                    }
                }

            }
        }
    }

    private fun event() {

        binding.btnSync.setOnClickListener {
            loadingDialog = LoadingDialog.show(this)
            ControlBleTools.getInstance().getNewDiyDialData(getDiyParamsBean(), object : DiyDialDataCallBack {
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
        ControlBleTools.getInstance().getDeviceDiyWatchFace(watchId, data.size, true, configBean, object : DeviceWatchFaceFileStatusListener {
            override fun onSuccess(statusValue: Int, statusName: String) {
                if (loadingDialog != null && loadingDialog!!.isShowing) {
                    loadingDialog!!.dismiss()
                }
                if (statusValue == DeviceWatchFaceFileStatusListener.PrepareStatus.READY.state) {
                    sendWatchData(data, configBean)
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
        ControlBleTools.getInstance().startUploadBigData(BleCommonAttributes.UPLOAD_BIG_DATA_WATCH, data, true, object : UploadBigDataListener {
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

            override fun onTimeout(msg: String?) {
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

    class PhotoAdapter(private val context: Context, private val data: List<PhotoBean>, var selected: (addPosition: Int, delPosition: Int) -> Unit) :
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
                    data.get(position).isSelected = true
                    notifyDataSetChanged()
                    selected(position, -1)
                } else {
                    var selectedNum = 0
                    for (item in data) {
                        if (item.isSelected) selectedNum += 1
                    }
                    if (selectedNum > 1) {
                        data.get(position).isSelected = false
                        notifyDataSetChanged()
                        selected(-1, position)
                    }
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

    //region 背景覆盖图选择适配器
    data class OverlayBean(var imgBitmap: Bitmap, var isSelected: Boolean)

    class OverlayAdapter(private val context: Context, private val data: List<OverlayBean>, var selected: (postion: Int) -> Unit) :
        RecyclerView.Adapter<OverlayAdapter.PhotoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            return PhotoViewHolder(
                LayoutInflater.from(context).inflate(R.layout.item_diy_photo, parent, false)
            )
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            val overlayBean = data.get(position)
            if (overlayBean.imgBitmap != null) {
                Glide.with(context).load(overlayBean.imgBitmap).into(holder.ivIcon)
            } else {
                Glide.with(context).load(R.mipmap.sport_share_custom_camera).into(holder.ivIcon)
            }
            holder.ivSelected.visibility = if (overlayBean.isSelected) View.VISIBLE else View.GONE
            holder.rootlayout.setOnClickListener {

                if (!overlayBean.isSelected) {
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

    //region 指针适配器
    data class StyleBean(var img: Bitmap, var imgData: Bitmap, var binData: ByteArray, var isSelected: Boolean)

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

    //region 数字 适配器
    data class NumberFontBean(var fontName: String, var fontImg: Bitmap, var locations: List<NumberLocationBean>, var isSelected: Boolean)
    data class NumberLocationBean(
        var fontName: String, var locationName: String, var locationImg: Bitmap, var imgData: Bitmap, var binData: ByteArray, var isSelected: Boolean
    )

    class NumberStylerAdapter(private val context: Context, private val data: List<NumberFontBean>, var selected: (postion: Int) -> Unit) :
        RecyclerView.Adapter<NumberStylerAdapter.NumberStylerViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberStylerViewHolder {
            return NumberStylerViewHolder(
                LayoutInflater.from(context).inflate(R.layout.item_diy_photo, parent, false)
            )
        }

        override fun onBindViewHolder(holder: NumberStylerViewHolder, position: Int) {
            val photoBean = data.get(position)
            if (photoBean.fontImg != null) {
                Glide.with(context).load(photoBean.fontImg).into(holder.ivIcon)
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
        inner class NumberStylerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var ivIcon: AppCompatImageView = view.findViewById(R.id.iv_icon)
            var ivSelected: AppCompatImageView = view.findViewById(R.id.iv_selected)
            val rootlayout: ConstraintLayout = view.findViewById(R.id.root_layout)
        }
    }

    class NumberLocationAdapter(private val context: Context, private val data: List<NumberLocationBean>, var selected: (postion: Int) -> Unit) :
        RecyclerView.Adapter<NumberLocationAdapter.NumberLocationViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberLocationViewHolder {
            return NumberLocationViewHolder(
                LayoutInflater.from(context).inflate(R.layout.item_diy_photo, parent, false)
            )
        }

        override fun onBindViewHolder(holder: NumberLocationViewHolder, position: Int) {
            val photoBean = data.get(position)
            if (photoBean.locationImg != null) {
                Glide.with(context).load(photoBean.locationImg).into(holder.ivIcon)
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
        inner class NumberLocationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var ivIcon: AppCompatImageView = view.findViewById(R.id.iv_icon)
            var ivSelected: AppCompatImageView = view.findViewById(R.id.iv_selected)
            val rootlayout: ConstraintLayout = view.findViewById(R.id.root_layout)
        }
    }
    //endregion

    //region 背景颜色适配器
    class BgColorAdapter(private val context: NewDiyDialActivity, private val data: List<IntArray>) :
        RecyclerView.Adapter<BgColorAdapter.BgColorViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BgColorViewHolder {
            return BgColorViewHolder(
                LayoutInflater.from(context).inflate(R.layout.item_diy_bg_color, parent, false)
            )
        }

        override fun getItemCount(): Int = data.size

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: BgColorViewHolder, position: Int) {
            val itemData = data.get(position)

            holder.tvName.text = context.getString(R.string.s494) + " " + ((position) + 1)

            val colorList = intArrayOf(
                R.color.theme_color1,
                R.color.theme_color2,
                R.color.theme_color3,
                R.color.theme_color4,
                R.color.theme_color5,
                R.color.theme_color6,
                R.color.theme_color7,
                R.color.theme_color8,
                R.color.theme_color9
            )
            holder.layoutColor.removeAllViews()
            var isFixedValue = false
            for (i in colorList.indices) {
                val mLinearLayout = context.layoutInflater.inflate(R.layout.theme_color_layout, null) as LinearLayout
                val colorRoundView = mLinearLayout.findViewById<ColorRoundView>(R.id.colorRoundView)
                val ivColorBg = mLinearLayout.findViewById<ImageView>(R.id.ivColorBg)
                colorRoundView.setBgColor(colorList[i], colorList[i])
                if (itemData[0] == Color.red(colorRoundView.getcolor())
                    && itemData[1] == Color.green(colorRoundView.getcolor())
                    && itemData[2] == Color.blue(colorRoundView.getcolor())
                ) {
                    ivColorBg.background = ContextCompat.getDrawable(context, R.drawable.theme_select_circle)
                    isFixedValue = true
                }
                //是提供固定的颜色
                if (isFixedValue) {
                    holder.ivCustomizeColor.background = ContextCompat.getDrawable(context, R.color.transparent)
                } else {
                    holder.ivCustomizeColor.background = ContextCompat.getDrawable(context, R.drawable.theme_select_circle)
                }
                colorRoundView.setOnClickListener {
                    for (i in colorList.indices) {
                        val childView = holder.layoutColor.getChildAt(i)
                        val childViewColorBg = childView.findViewById<ImageView>(R.id.ivColorBg)
                        childViewColorBg.background = ContextCompat.getDrawable(context, R.color.transparent)
                    }
                    ivColorBg.background =
                        ContextCompat.getDrawable(context, R.drawable.theme_select_circle)

                    val color: Int = colorRoundView.getcolor()
                    context.setColor(position, Color.red(color), Color.green(color), Color.blue(color))
                    holder.ivCustomizeColor.background = ContextCompat.getDrawable(context, R.color.transparent)
                }
                holder.layoutColor.addView(mLinearLayout)
            }

            holder.layoutCustomizeColor.setOnClickListener {
                showSelectColor(holder, position, context)
            }
        }

        private fun showSelectColor(holder: BgColorViewHolder, position: Int, context: NewDiyDialActivity) {
            //https://github.com/skydoves/ColorPickerView
            val colorPockerBuilder = ColorPickerDialog.Builder(context)
                .setTitle("")
                .setPreferenceName(COLOR_PICKER_NAME)
                .setNegativeButton(context.getString(R.string.s483)) { dialogInterface, i -> dialogInterface.dismiss() }
                .attachAlphaSlideBar(false) // the default value is true.
                .attachBrightnessSlideBar(false) // the default value is true.
                .setBottomSpace(12) // set a bottom space between the last slidebar and buttons.
                .setPositiveButton(context.getString(R.string.s484),
                    ColorEnvelopeListener { envelope, fromUser ->
                        //LogUtils.d("ColorEnvelopeListener:" + GsonUtils.toJson(envelope) + "," + fromUser)
                        context.setColor(position, envelope.argb[1], envelope.argb[2], envelope.argb[3])
                        context.refFixedColorPickerItem()
                        holder.ivCustomizeColor.background = ContextCompat.getDrawable(context, R.drawable.theme_select_circle)
                    }
                )

            colorPockerBuilder.colorPickerView.apply {
                val bubbleFlag = BubbleFlag(context)
                bubbleFlag.flagMode = FlagMode.FADE
                setFlagView(bubbleFlag)
            }

            val item = context.selectedColors.get(position)
            ColorPickerPreferenceManager.getInstance(context)
                .clearSavedAllData() // clears all of the states.
                .setColor(COLOR_PICKER_NAME, Color.rgb(item[0], item[1], item[2])) // manipulates the saved color data.

            colorPockerBuilder.show()
        }

        inner class BgColorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var tvName = view.findViewById<TextView>(R.id.tvName)
            var layoutColor = view.findViewById<LinearLayout>(R.id.layoutColor)
            var layoutCustomizeColor = view.findViewById<ConstraintLayout>(R.id.layoutCustomizeColor)
            var ivCustomizeColor = view.findViewById<ImageView>(R.id.ivCustomizeColor)
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

    @SuppressLint("NotifyDataSetChanged")
    private fun refOverlayAdapter() {
        (binding.rvOverlay.adapter as OverlayAdapter?)?.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun refNumberFontAndLocation() {
        (binding.rvNumberStyle.adapter as NumberStylerAdapter?)?.notifyDataSetChanged()
        (binding.rvNumberLocation.adapter as NumberLocationAdapter?)?.notifyDataSetChanged()
    }

    private fun refFixedColorPickerItem() {
        binding.rvbgColors.adapter?.notifyDataSetChanged()
    }

    //region 颜色选中
    private fun setColor(postion: Int, red: Int, green: Int, blue: Int) {
        LogUtils.d("Color:$red,$green,$blue, p = $postion")
        if (postion < selectedColors.size) {
            val item = selectedColors.get(postion)
            item[0] = red
            item[1] = green
            item[2] = blue
        }
        refPreView()
    }
    //endregion


    //region 预览
    private fun refPreView() {
        val diyRenderingBean = getDiyParamsBean()
        ControlBleTools.getInstance().getNewPreviewBitmap(diyRenderingBean, object : DiyDialPreviewCallBack {
            override fun onDialPreview(preview: Bitmap?) {
                if (preview != null) {
                    Glide.with(this@NewDiyDialActivity).load(preview).into(binding.ivPreview)
                }
            }

            override fun onError(errMsg: String?) {
                LogUtils.e(errMsg)
                errMsg?.let { MyApplication.showToast(it) }
            }
        })
    }
    //endregion

//    /**
//     * 获取diy表盘请求参数
//     */
//    private fun getDiyParamsBean(): DiyParamsBean {
//       return getDiyNumberParamsBean()
//    }

    /**
     * 获取diy表盘请求参数
     */
    private fun getDiyParamsBean(): NewDiyParamsBean {
        val newDiyParamsBean = NewDiyParamsBean()
        try {
            for (f in FileUtils.listFilesInDir(diyFilePath)) {
                if (TextUtils.equals(f.name, "watch.json")) {
                    dataJson = FileIOUtils.readFile2String(f)
                    break
                }
            }

            val diyDialBean: NewZhDiyDialBean? = GsonUtils.fromJson<NewZhDiyDialBean>(dataJson, NewZhDiyDialBean::class.java)
            if (diyDialBean == null) {
                ToastUtils.showShort(getString(R.string.s512))
                finish()
                return newDiyParamsBean
            }

            //json
            newDiyParamsBean.jsonStr = dataJson

            //背景资源
            val backgroundResBean = NewDiyParamsBean.BackgroundResBean()
            val background = photoSelectBitmap
            val backgroundOverlay = overlaySelectBitmap
            backgroundResBean.backgrounds = background
            backgroundResBean.backgroundOverlay = backgroundOverlay
            backgroundResBean.backgroundColors = selectedColors
            newDiyParamsBean.backgroundResBean = backgroundResBean

            //指针
            if (styleSelect != null) {
                val styleResBean = NewDiyParamsBean.StyleResBean()
                styleResBean.styleBm = styleSelect!!.imgData
                styleResBean.styleBin = styleSelect!!.binData
                newDiyParamsBean.styleResBean = styleResBean
            }

            //数字
            if (numberSelect != null) {
                val numberResBean = NewDiyParamsBean.NumberResBean()
                numberResBean.fontName = numberSelect!!.fontName
                for (font in numberFonts) {
                    if (font.fontName == numberResBean.fontName) {
                        numberResBean.fontMD5 = DiyDialUtils.getDiyBitmapMd5String(font.fontImg)
                    }
                }
                numberResBean.locationName = numberSelect!!.locationName
                numberResBean.locationMD5 = DiyDialUtils.getDiyBitmapMd5String(numberSelect!!.locationImg)
                numberResBean.numberBm = numberSelect!!.imgData
                numberResBean.numberBin = numberSelect!!.binData
                //默认使用第一个背景对应色
                numberResBean.red = selectedColors.get(0)[0]
                numberResBean.green = selectedColors.get(0)[1]
                numberResBean.blue = selectedColors.get(0)[2]
                newDiyParamsBean.numberResBean = numberResBean
                //numberResBean.textInfos 由sdk内部根据json检测fontName，locationName补充，不需要赋值
            }

            //复杂功能
            val functionsResBean = NewDiyParamsBean.FunctionsResBean()
            functionsResBean.functionsBin = FileIOUtils.readFile2BytesByStream(diyFilePath + File.separator + diyDialBean.complex.path.replace("\\", File.separator))
            val functionsBitmapBeans = mutableListOf<NewDiyParamsBean.FunctionsResBean.FunctionsBitmapBean>()
            if (!diyDialBean.complex.infos.isNullOrEmpty()) {
                for (info in diyDialBean.complex.infos) {
                    if (!info.detail.isNullOrEmpty()) {
                        for (d in info.detail) {
                            var function = MyDiyDialUtils.getDiyWatchFaceFunctionByTypeName(d.typeName)
                            var isCanAdd = true
                            for (f in functionsBitmapBeans) {
                                if (f.function == function.function) {
                                    isCanAdd = false
                                }
                            }
                            if (isCanAdd) {
                                functionsBitmapBeans.add(
                                    NewDiyParamsBean.FunctionsResBean.FunctionsBitmapBean().apply {
                                        this.bitmap = ConvertUtils.bytes2Bitmap(
                                            FileIOUtils.readFile2BytesByStream(
                                                diyFilePath + File.separator + d.picPath.replace("\\", File.separator)
                                            )
                                        )
                                        this.function = function.function
                                    }
                                )
                            }
                        }
                    }
                }
            }
            functionsResBean.functionsBitmaps = functionsBitmapBeans
            //复杂功能资源
            newDiyParamsBean.functionsResBean = functionsResBean
            //复杂功能设置
            newDiyParamsBean.diyWatchFaceConfigBean = diyWatchFaceConfig
            return newDiyParamsBean
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort(getString(R.string.s512))
            finish()
            return newDiyParamsBean
        }
    }

}