package com.zjw.sdkdemo.function.sifli

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivitySifliTestBinding
import com.zjw.sdkdemo.function.language.BaseActivity

/**
 * Created by Android on 2023/8/2.
 */
class SifliTestActivity : BaseActivity() {

    private var loadingDialog: Dialog? = null //loadingDialog = LoadingDialog.show(this)

    private val binding by lazy { ActivitySifliTestBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s488)
        setContentView(binding.root)
    }

    public fun ota(v: View?) {
        startActivity(Intent(this, SifliOtaActivity::class.java))
    }

    public fun dial(v: View?) {
        startActivity(Intent(this, SifliDialActivity::class.java))
    }

    public fun photoDial(v: View?) {
        startActivity(Intent(this, SifliPhotoDialActivity::class.java))
    }
}