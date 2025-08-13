package com.zjw.sdkdemo.function.diydial

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivityDiySelectBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.utils.customdialog.CustomDialog
import com.zjw.sdkdemo.utils.customdialog.MyDialog
import java.io.File

/**
 * Created by Android on 2023/9/20.
 */
class DiySelectActivity : BaseActivity(), View.OnClickListener {
    private val binding by lazy { ActivityDiySelectBinding.inflate(layoutInflater) }
    private val mFilePath = PathUtils.getAppDataPathExternalFirst() + "/diy"

    private var mFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(com.zjw.sdkdemo.R.string.s361)
        setContentView(binding.root)
        FileUtils.createOrExistsDir(mFilePath)
        initView()
    }

    private fun initView() {


    }

    override fun onClick(v: View) {
        when (v.id) {
            binding.btnFile.id -> {
                val files = FileUtils.listFilesInDir(mFilePath)
                if (files.isNullOrEmpty()) {
                    ToastUtils.showShort("$mFilePath 目录文件为空")
                    return
                }
                showListDialog(files)
            }
        }
    }

    private var dialog: MyDialog? = null
    private fun showListDialog(files: List<File>) {
        val rootView = layoutInflater.inflate(R.layout.dialog_debug_bin_list, null)
        dialog = CustomDialog.builder(this)
            .setContentView(rootView)
            .setWidth(ScreenUtils.getScreenWidth())
            .setHeight((ScreenUtils.getScreenHeight() * 0.8f).toInt())
            .setGravity(Gravity.CENTER)
            .build()
        for (file in files) {
            val view = Button(this)
            view.isAllCaps = false
            view.text = file.name
            view.setTextColor(ContextCompat.getColor(this, R.color.theme_color2))
            if (FileUtils.isDir(file)) {
                for (f in FileUtils.listFilesInDir(file)) {
                    //LogUtils.d("name = " + f.name)
                    if (FileUtils.isDir(f)) {
                        for (f2 in FileUtils.listFilesInDir(f)) {
                            //LogUtils.d("name = " + f2.name)
                            if (TextUtils.equals(f2.name, "designsketch.png")) {
                                Glide.with(this).load(f2).into(object : CustomTarget<Drawable>() {
                                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                        val h: Int = resource.getIntrinsicHeight()
                                        val w: Int = resource.getIntrinsicWidth()
                                        resource.setBounds(0, 0, w, h)
                                        view.setCompoundDrawables(null, resource, null, null)
                                    }

                                    override fun onLoadCleared(placeholder: Drawable?) {
                                        LogUtils.d("获取表盘图片失败")
                                    }

                                })
                                break
                            }
                        }
                    }
                }
            }

            view.setOnClickListener {
                var isLegal = false
                if (FileUtils.isDir(file)) {
                    var isJson = false
                    var isBackground = false
                    var isComplex = false
                    var isOverlay = false
                    var isPointer = false
                    var isTime = false
                    //background、complex、overlay、pointer、time、watch.json
                    for (f in FileUtils.listFilesInDir(file)) {
                        //LogUtils.d("name = " + f.name)
                        if (TextUtils.equals(f.name, "background")) isBackground = true
                        if (TextUtils.equals(f.name, "complex")) isComplex = true
                        if (TextUtils.equals(f.name, "overlay")) isOverlay = true
                        if (TextUtils.equals(f.name, "pointer")) isPointer = true
                        if (TextUtils.equals(f.name, "time")) isTime = true
                        if (TextUtils.equals(f.name, "watch.json")) isJson = true
                    }
                    isLegal = isJson && isBackground && isComplex && isOverlay && isPointer && isTime
                }
                if (!isLegal) {
                    ToastUtils.showShort(getString(R.string.s512))
                    dialog?.dismiss()
                    return@setOnClickListener
                }
                binding.tvName.text = "${getString(R.string.s513)} ${file.name}"
                dialog?.dismiss()
                mFile = file
                if (mFile != null) {
                    LogUtils.d("选择的文件夹：" + mFile!!.absolutePath)
                    startActivity(
                        Intent(this, NewDiyDialActivity::class.java)
                            .putExtra(NewDiyDialActivity.EX_FILE, mFile!!.absolutePath)
                    )
                } else {
                    ToastUtils.showShort(getString(R.string.s512))
                    dialog?.dismiss()
                    return@setOnClickListener
                }
            }
            rootView.findViewById<LinearLayout>(R.id.listLayout).addView(view)
        }
        dialog?.show()
    }


}