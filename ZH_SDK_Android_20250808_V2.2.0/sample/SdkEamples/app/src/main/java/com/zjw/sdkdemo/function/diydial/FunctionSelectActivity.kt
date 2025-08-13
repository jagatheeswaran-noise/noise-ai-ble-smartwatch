package com.zjw.sdkdemo.function.diydial

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.zhapp.ble.bean.DiyWatchFaceConfigBean
import com.zhapp.ble.callback.DiyWatchFaceCallBack
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivityFunctionSelectBinding
import com.zjw.sdkdemo.function.language.BaseActivity

/**
 * Created by Android on 2022/12/20.
 */
class FunctionSelectActivity : BaseActivity() {
    private val binding by lazy { ActivityFunctionSelectBinding.inflate(layoutInflater) }
    companion object {
        const val ACTIVITY_DATA_TEXT = "type"
        const val RESULT_DATA_TEXT = "data"
    }

    private lateinit var functionsConfig: DiyWatchFaceConfigBean.FunctionsConfig
    private lateinit var functions: MutableList<DiyWatchFaceConfigBean.FunctionsConfig.FunctionsConfigType>
    private var typeChoose = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s368)
        setContentView(binding.root)
        inits()
    }

    private fun inits() {
        val json = intent.getStringExtra(ACTIVITY_DATA_TEXT)
        functionsConfig = GsonUtils.fromJson(json, DiyWatchFaceConfigBean.FunctionsConfig::class.java)
        if(functionsConfig!= null && functionsConfig.functionsConfigTypes!=null) {
            functionsConfig.functionsConfigTypes.add(0, DiyWatchFaceConfigBean.FunctionsConfig.FunctionsConfigType(DiyWatchFaceCallBack.DiyWatchFaceFunction.OFF.function))
        }
        functions = functionsConfig.functionsConfigTypes
        typeChoose = functionsConfig.typeChoose
        if(functions.isNullOrEmpty()){
            finish()
            return
        }
        LogUtils.json(functions)
        binding.rvFunction.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.rvFunction.adapter = FunctionAdapter(this, functions) {
            ToastUtils.showShort("function:" + functions.get(it))
            functionsConfig.typeChoose = functions.get(it).type
            typeChoose = functions.get(it).type
        }
    }

    inner class FunctionAdapter(private val context: Context,
                                private val data: List<DiyWatchFaceConfigBean.FunctionsConfig.FunctionsConfigType>,
                                var selected: (postion: Int) -> Unit) :
        RecyclerView.Adapter<FunctionAdapter.FunctionViewHolder>() {

        inner class FunctionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var rootLayout: ConstraintLayout = view.findViewById(R.id.root_layout)
            var tvTitle: TextView = view.findViewById(R.id.tv_title)
            var ivSelected: ImageView = view.findViewById(R.id.iv_selected)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FunctionViewHolder {
            return FunctionViewHolder(
                LayoutInflater.from(context).inflate(R.layout.item_diy_function, parent, false)
            )
        }

        override fun onBindViewHolder(holder: FunctionViewHolder, position: Int) {
            val function = data.get(position)
            holder.tvTitle.text = MyDiyDialUtils.getFunctionsDetailNameByType(context, function.type)
            holder.ivSelected.visibility = if (function.type == typeChoose) View.VISIBLE else View.GONE
            holder.rootLayout.setOnClickListener {
                selected(position)
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int = data.size
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val intent = intent
            functionsConfig.functionsConfigTypes?.removeAt(0)
            intent.putExtra(RESULT_DATA_TEXT, GsonUtils.toJson(functionsConfig))
            setResult(Activity.RESULT_OK, intent)
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                val intent = intent
                functionsConfig.functionsConfigTypes?.removeAt(0)
                intent.putExtra(RESULT_DATA_TEXT, GsonUtils.toJson(functionsConfig))
                setResult(Activity.RESULT_OK, intent)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}