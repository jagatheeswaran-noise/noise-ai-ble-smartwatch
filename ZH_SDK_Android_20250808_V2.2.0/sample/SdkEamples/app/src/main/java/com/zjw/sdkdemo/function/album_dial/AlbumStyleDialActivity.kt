//package com.zjw.sdkdemo.function.album_dial
//
//import android.app.Dialog
//import android.content.DialogInterface
//import android.os.Bundle
//import android.text.TextUtils
//import android.view.View
//import android.view.Window
//import android.widget.ProgressBar
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.chad.library.adapter.base.BaseQuickAdapter
//import com.hjq.permissions.Permission
//import com.hjq.permissions.XXPermissions
//import com.zhapp.ble.BleCommonAttributes
//import com.zhapp.ble.ControlBleTools
//import com.zhapp.ble.callback.DeviceWatchFaceFileStatusListener
//import com.zhapp.ble.callback.UploadBigDataListener
//import com.zjw.sdkdemo.R
//import com.zjw.sdkdemo.app.MyApplication
//import com.zjw.sdkdemo.databinding.ActivityAlbumStyleDialBinding
//import com.zjw.sdkdemo.function.album_dial.adapter.BackgroundAdapter
//import com.zjw.sdkdemo.function.album_dial.adapter.PlacementAdapter
//import com.zjw.sdkdemo.function.album_dial.adapter.TextColourAdapter
//import com.zjw.sdkdemo.function.album_dial.adapter.TextFontAdapter
//import com.zjw.sdkdemo.function.album_dial.model.BackgroundSelectModel
//import com.zjw.sdkdemo.function.album_dial.model.ColorSelectModel
//import com.zjw.sdkdemo.function.album_dial.model.FontSelectModel
//import com.zjw.sdkdemo.function.album_dial.model.PlacementSelectModel
//import com.zjw.sdkdemo.function.album_dial.utils.CustomDialUtils.toDialFile
//import com.zjw.sdkdemo.function.album_dial.utils.ImagePickUtils.pickImageCrop
//import com.zjw.sdkdemo.function.album_dial.utils.ImageUtils.composeImage
//import java.io.*
//
//class AlbumStyleDialActivity : AppCompatActivity() {
//    private lateinit var binding: ActivityAlbumStyleDialBinding
//
//    private lateinit var mColors: MutableList<ColorSelectModel>
//    private lateinit var mSelectColor: String
//    private lateinit var mBackgrounds: MutableList<BackgroundSelectModel>
//    private lateinit var mBackgroundSelectModel: BackgroundSelectModel
//
//    private lateinit var mAllPlacement: MutableMap<Int, MutableList<PlacementSelectModel>>
//    private lateinit var mPlacements: MutableList<PlacementSelectModel>
//
//    private lateinit var mFonts: MutableList<FontSelectModel>
//    private lateinit var mPlacementSelectModel: PlacementSelectModel
//    private lateinit var mFontSelectModel: FontSelectModel
//
//    private lateinit var mColourAdapter: TextColourAdapter
//    private lateinit var mBackgroundAdapter: BackgroundAdapter
//    private lateinit var mPlacementAdapter: PlacementAdapter
//    private lateinit var mTextFontAdapter: TextFontAdapter
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
//        binding = ActivityAlbumStyleDialBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        initData()
//        initView()
//    }
//
//    private fun initData() {
//        mColors = ArrayList()
//        val list = arrayOf(
//            "#ffffff", "#000000", "#de4371", "#de4343",
//            "#de7143", "#dba85c", "#dbcf60",
//            "#b7c96b", "#a8e36d", "#85e36d", "#6de379", "#6de39c",
//            "#6de3c0", "#6de3e3", "#6dc0e3", "#6d9ce3", "#6d79e3",
//            "#856de3", "#a86de3", "#cb6de3", "#e36dd7", "#e36db4"
//        )
//        for (s in list) {
//            val colorSelectModel = ColorSelectModel()
//            colorSelectModel.color = s
//            mColors.add(colorSelectModel)
//        }
//        mColors[0].isSelected = true
//        mSelectColor = mColors[0].color
//        mColourAdapter = TextColourAdapter(mColors)
//        mBackgrounds = ArrayList()
//        mBackgrounds.add(BackgroundSelectModel())
//        val backgroundSelectModel = BackgroundSelectModel()
//        backgroundSelectModel.isSelected = true
//        backgroundSelectModel.resId = R.mipmap.background
//        mBackgrounds.add(backgroundSelectModel)
//        mBackgroundSelectModel = backgroundSelectModel
//        mBackgroundAdapter = BackgroundAdapter(mBackgrounds)
//        val placementType = intArrayOf(
//            Constants.PLACEMENT_LEFT_TOP, Constants.PLACEMENT_CENTER_TOP, Constants.PLACEMENT_RIGHT_TOP,
//            Constants.PLACEMENT_LEFT_CENTER, Constants.PLACEMENT_CENTER_CENTER, Constants.PLACEMENT_RIGHT_CENTER,
//            Constants.PLACEMENT_LEFT_BOTTOM, Constants.PLACEMENT_CENTER_BOTTOM, Constants.PLACEMENT_RIGHT_BOTTOM
//        )
//        val fontType = intArrayOf(Constants.FONT_TYPE_ONE, Constants.FONT_TYPE_TWO, Constants.FONT_TYPE_THREE)
//        mAllPlacement = HashMap(3)
//        for (k in fontType) {
//            val placements: MutableList<PlacementSelectModel> = ArrayList(9)
//            for (i in placementType) {
//                val model = PlacementSelectModel()
//                model.backgroundUrl = mBackgroundSelectModel.url
//                model.backgroundResId = mBackgroundSelectModel.resId
//                model.placement = i
//                model.isSelected = false
//                model.font = k
//                model.color = mSelectColor
//                placements.add(model)
//            }
//            mAllPlacement[k] = placements
//        }
//        mPlacements = mAllPlacement.get(Constants.FONT_TYPE_ONE)!!
//        mPlacements[0].isSelected = true
//        mPlacementSelectModel = mPlacements[0]
//        mPlacementAdapter = PlacementAdapter(mPlacements)
//        mFonts = ArrayList(3)
//        for (type in fontType) {
//            val model = FontSelectModel()
//            model.isSelected = false
//            model.font = type
//            model.color = mSelectColor
//            mFonts.add(model)
//        }
//        mFonts[0].isSelected = true
//        mFontSelectModel = mFonts[0]
//        mTextFontAdapter = TextFontAdapter(this, mFonts)
//    }
//
//    private fun initView() {
//        showSelect(true, true, true, true)
//        val colourManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
//        binding.textColourList.layoutManager = colourManager
//        binding.textColourList.adapter = mColourAdapter
//        binding.textColourList.itemAnimator = null
//        mColourAdapter.setOnItemClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
//            for (model in mColors) {
//                model.isSelected = false
//            }
//            mColors[position].isSelected = true
//            mSelectColor = mColors[position].color
//            mColourAdapter.notifyItemRangeChanged(0, mColors.size)
//            showSelect(false, false, false, true)
//        }
//        val backgroundManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
//        binding.backgroundList.layoutManager = backgroundManager
//        binding.backgroundList.adapter = mBackgroundAdapter
//        binding.backgroundList.itemAnimator = null
//        mBackgroundAdapter.setOnItemClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
//            if (position == 0) {
//                XXPermissions.with(this@AlbumStyleDialActivity)
//                    .permission(Permission.WRITE_EXTERNAL_STORAGE)
//                    .permission(Permission.READ_EXTERNAL_STORAGE)
//                    .request { permissions: List<String?>?, all: Boolean ->
//                        if (all) {
//                            pickImageCrop(this@AlbumStyleDialActivity, Constants.WIDTH, Constants.HEIGHT, 0,
//                                { path: String? ->
//                                    for (model in mBackgrounds) {
//                                        model.isSelected = false
//                                    }
//                                    val model = BackgroundSelectModel()
//                                    model.url = path
//                                    model.isSelected = true
//                                    mBackgrounds.add(1, model)
//                                    mBackgroundSelectModel = model
//                                    mBackgroundAdapter.notifyItemRangeChanged(0, mBackgrounds.size)
//                                    showSelect(true, false, false, false)
//                                }) { error: String? ->
//                            }
//                        }
//                    }
//                return@setOnItemClickListener
//            }
//            for (model in mBackgrounds) {
//                model.isSelected = false
//            }
//            mBackgrounds[position].isSelected = true
//            mBackgroundSelectModel = mBackgrounds[position]
//            mBackgroundAdapter.notifyItemRangeChanged(0, mBackgrounds.size)
//            showSelect(true, false, false, false)
//        }
//        val placementManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
//        binding.placementList.layoutManager = placementManager
//        binding.placementList.adapter = mPlacementAdapter
//        binding.placementList.itemAnimator = null
//        mPlacementAdapter.setOnItemClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
//            for (model in mPlacements) {
//                model.isSelected = false
//            }
//            mPlacements[position].isSelected = true
//            mPlacementSelectModel = mPlacements[position]
//            mPlacementAdapter.notifyItemRangeChanged(0, mPlacements.size)
//            showSelect(false, true, false, false)
//        }
//        val fontManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
//        binding.textFontList.layoutManager = fontManager
//        binding.textFontList.adapter = mTextFontAdapter
//        binding.textFontList.itemAnimator = null
//        mTextFontAdapter.setOnItemClickListener { adapter: BaseQuickAdapter<*, *>?, view: View?, position: Int ->
//            for (model in mFonts) {
//                model.isSelected = false
//            }
//            mFonts[position].isSelected = true
//            mFontSelectModel = mFonts[position]
//            mTextFontAdapter.notifyItemRangeChanged(0, mFonts.size)
//            showSelect(false, false, true, false)
//        }
//        binding.generate.setOnClickListener { view: View? ->
//            val file = File("$filesDir/dial")
//            if (!file.exists()) {
//                file.mkdir()
//            }
//            val binPath = file.absolutePath + "/dial_" + System.currentTimeMillis() + ".bin"
//            toDialFile(
//                this@AlbumStyleDialActivity, binPath,
//                mPlacementSelectModel.backgroundUrl, mPlacementSelectModel.placement,
//                mPlacementSelectModel.font, mSelectColor, "dial_base.bin"
//            )
//            uploadWatch(getBytes(binPath)!!)
//        }
//    }
//
//    private fun showSelect(
//        isBackgroundChanged: Boolean, isPlacementChanged: Boolean,
//        isFontChanged: Boolean, isColorChanged: Boolean
//    ) {
//        if (isBackgroundChanged) {
//            val resId = mBackgroundSelectModel.resId
//            val url = mBackgroundSelectModel.url
//            for (model in mPlacements) {
//                model.backgroundResId = resId
//                model.backgroundUrl = url
//            }
//            mPlacementAdapter.notifyItemRangeChanged(0, mPlacements.size)
//        }
//        if (isFontChanged) {
//            val type = mFontSelectModel.font
//            var position = 0
//            for (i in mPlacements.indices) {
//                if (mPlacements[i].isSelected) {
//                    position = i
//                    break
//                }
//            }
//            mPlacements = mAllPlacement[type]!!
//            for (model in mPlacements) {
//                model.isSelected = false
//                model.color = mSelectColor
//            }
//            mPlacementSelectModel = mPlacements[position]
//            mPlacements[position].isSelected = true
//            mPlacementAdapter.setNewInstance(mPlacements)
//        }
//        if (isColorChanged && !TextUtils.isEmpty(mSelectColor)) {
//            for (model in mPlacements) {
//                model.color = mSelectColor
//            }
//            mPlacementAdapter.notifyItemRangeChanged(0, mPlacements.size)
//            for (model in mFonts) {
//                model.color = mSelectColor
//            }
//            mTextFontAdapter.notifyItemRangeChanged(0, mFonts.size)
//        }
//        binding.mainDial.setImageBitmap(
//            composeImage(
//                this@AlbumStyleDialActivity,
//                mPlacementSelectModel.placement, mPlacementSelectModel.font,
//                mPlacementSelectModel.color, mPlacementSelectModel.backgroundUrl
//            )
//        )
//    }
//
//    private fun uploadWatch(data: ByteArray) {
//        ControlBleTools.getInstance().getDeviceWatchFace("180", data.size, true, object : DeviceWatchFaceFileStatusListener {
//            override fun onSuccess(statusValue: Int, statusName: String) {
//                when (statusValue) {
//                    DeviceWatchFaceFileStatusListener.PrepareStatus.READY.state -> {
//                        sendWatchData(data)
//                    }
//                    DeviceWatchFaceFileStatusListener.PrepareStatus.BUSY.state -> {
//                        MyApplication.showToast(getString(R.string.s223))
//                    }
//                    DeviceWatchFaceFileStatusListener.PrepareStatus.DUPLICATED.state -> {
//                        MyApplication.showToast(getString(R.string.s224))
//                    }
//                    DeviceWatchFaceFileStatusListener.PrepareStatus.LOW_STORAGE.state -> {
//                        MyApplication.showToast(getString(R.string.s224))
//                    }
//                    DeviceWatchFaceFileStatusListener.PrepareStatus.LOW_BATTERY.state -> {
//                        MyApplication.showToast(getString(R.string.s225))
//                    }
//                    DeviceWatchFaceFileStatusListener.PrepareStatus.DOWNGRADE.state -> {
//                        MyApplication.showToast(getString(R.string.s224))
//                    }
//                }
//            }
//
//            override fun timeOut() {
//                MyApplication.showToast("timeOut")
//            }
//        })
//    }
//
//    private fun sendWatchData(data: ByteArray) {
//        showDialog()
//        ControlBleTools.getInstance().startUploadBigData(
//            BleCommonAttributes.UPLOAD_BIG_DATA_WATCH,
//            data, true, object : UploadBigDataListener {
//                override fun onSuccess() {
//                    if (progressDialog != null && (progressDialog?.isShowing == true)) {
//                        progressDialog?.dismiss()
//                    }
//                }
//
//                override fun onProgress(curPiece: Int, dataPackTotalPieceLength: Int) {
//                    progressBar?.progress = curPiece * 100 / dataPackTotalPieceLength
//                }
//
//                override fun onTimeout() {
////                    Log.e(tag, "startUploadBigData timeOut")
//                }
//            })
//    }
//
//    private var progressDialog: Dialog? = null
//    var msg: TextView? = null
//    var tvDeviceUpdateProgress: TextView? = null
//    private var progressBar: ProgressBar? = null
//
//    private fun showDialog() {
//        if (progressDialog == null) {
//            progressDialog = Dialog(this, R.style.progress_dialog)
//            progressDialog!!.setContentView(R.layout.update_layout)
//            progressDialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)
//        }
//        msg = progressDialog!!.findViewById<View>(R.id.tvLoading) as TextView
//        progressBar = progressDialog!!.findViewById(R.id.progressBar)
//        tvDeviceUpdateProgress = progressDialog!!.findViewById(R.id.tvDeviceUpdateProgress)
//        progressBar?.visibility = View.VISIBLE
//        progressDialog!!.show()
//        progressDialog!!.setCanceledOnTouchOutside(false)
//        progressDialog!!.setOnDismissListener { dialog: DialogInterface? -> }
//    }
//
//    private fun getBytes(filePath: String): ByteArray? {
//        var buffer: ByteArray? = null
//        try {
//            val file = File(filePath)
//            val fis = FileInputStream(file)
//            val bos = ByteArrayOutputStream(1000)
//            val b = ByteArray(1000)
//            var n: Int
//            while (fis.read(b).also { n = it } != -1) {
//                bos.write(b, 0, n)
//            }
//            fis.close()
//            bos.close()
//            buffer = bos.toByteArray()
//        } catch (e: FileNotFoundException) {
//            e.printStackTrace()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//        return buffer
//    }
//}