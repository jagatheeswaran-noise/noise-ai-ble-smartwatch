package com.zjw.sdkdemo.function

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ClickUtils
import com.blankj.utilcode.util.ScreenUtils
import com.skydoves.colorpickerview.ColorEnvelope
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.flag.BubbleFlag
import com.skydoves.colorpickerview.flag.FlagMode
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.skydoves.colorpickerview.listeners.ColorPickerViewListener
import com.skydoves.colorpickerview.preference.ColorPickerPreferenceManager
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.BreathingLightSettingsBean
import com.zhapp.ble.callback.SettingMenuCallBack
import com.zhapp.ble.custom.DiyDialUtils
import com.zhapp.ble.parsing.ParsingStateManager.SendCmdStateListener
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivityLightSetBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.livedata.DeviceSettingLiveData
import com.zjw.sdkdemo.utils.customdialog.CustomDialog
import com.zjw.sdkdemo.view.CustomMaterialSwitch

/**
 * Created by Android on 2023/12/13.
 */
@SuppressLint("NotifyDataSetChanged")
class LightSetActivity : BaseActivity() {
    private var settingsBean: BreathingLightSettingsBean? = null
    private var data: MutableList<BreathingLightSettingsBean.LightItem> = mutableListOf()

    companion object {
        const val COLOR_PICKER_NAME = "LightColorPickerDialog"
    }

    private val binding by lazy { ActivityLightSetBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s528)
        setContentView(binding.root)

        binding.rvData.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.rvData.adapter = LightItemAdapter(this, data) {

        }

        binding.mainSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (settingsBean != null) {
                var isSetNew = false
                if (settingsBean!!.mainSwitch != buttonView.isChecked) {
                    isSetNew = true
                }
                settingsBean!!.mainSwitch = buttonView.isChecked
                if (isSetNew) {
                    // 设置给设备
                    setBreathingLightSettings()
                    refUiByData()
                }
            }
        }

        binding.aSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (settingsBean != null) {
                var isSetNew = false
                if (settingsBean!!.lighttingSwitch != buttonView.isChecked) {
                    isSetNew = true
                }
                settingsBean!!.lighttingSwitch = buttonView.isChecked
                if (isSetNew) {
                    // 设置给设备
                    setBreathingLightSettings()
                    refUiByData()
                }
            }
        }

        binding.view.setOnClickListener {
            if (settingsBean != null) {
                showSelectColor(settingsBean!!.lighttingColor, object : ColorEnvelopeListener {
                    override fun onColorSelected(envelope: ColorEnvelope?, fromUser: Boolean) {
                        if (envelope != null) {
                            settingsBean!!.lighttingColor = DiyDialUtils.getRGBValueByColor(envelope.argb[1], envelope.argb[2], envelope.argb[3])
                            // 设置给设备
                            setBreathingLightSettings()
                            refUiByData()
                        }
                    }
                })
            }
        }
        initCallBack()
        getBreathingLightSettings()

        //添加
        ClickUtils.applySingleDebouncing(binding.btnAdd) {
            showAddDialog()
        }
    }

    private fun getBreathingLightSettings() {
        if (ControlBleTools.getInstance().isConnect) {
            ControlBleTools.getInstance().getBreathingLightSettings(object : SendCmdStateListener(lifecycle) {
                override fun onState(state: SendCmdState) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(getString(R.string.s220))
                        else -> MyApplication.showToast(getString(R.string.s221))
                    }
                }
            })
        }
    }

    private fun setBreathingLightSettings() {
        if (ControlBleTools.getInstance().isConnect) {
            ControlBleTools.getInstance().setBreathingLightSettings(settingsBean, object : SendCmdStateListener(lifecycle) {
                override fun onState(state: SendCmdState) {
                    when (state) {
                        SendCmdState.SUCCEED -> MyApplication.showToast(getString(R.string.s220))
                        else -> MyApplication.showToast(getString(R.string.s221))
                    }
                }
            })
        }
    }

    private fun showSelectColor(colorRGB: Int, colorListener: ColorPickerViewListener) {
        //https://github.com/skydoves/ColorPickerView
        val colorPockerBuilder = ColorPickerDialog.Builder(this)
            .setTitle("")
            .setPreferenceName(COLOR_PICKER_NAME)
            .setNegativeButton(getString(R.string.s483)) { dialogInterface, i -> dialogInterface.dismiss() }
            .attachAlphaSlideBar(false) // the default value is true.
            .attachBrightnessSlideBar(true) // the default value is true.
            .setBottomSpace(12) // set a bottom space between the last slidebar and buttons.
            .setPositiveButton(getString(R.string.s484), colorListener)
        colorPockerBuilder.colorPickerView.apply {
            val bubbleFlag = BubbleFlag(this@LightSetActivity)
            bubbleFlag.flagMode = FlagMode.FADE
            setFlagView(bubbleFlag)
        }

        val colors = DiyDialUtils.getColorByRGBValue(colorRGB)
        ColorPickerPreferenceManager.getInstance(this)
            .clearSavedAllData() // clears all of the states.
            .setColor(COLOR_PICKER_NAME, Color.argb(255, colors[0], colors[1], colors[2])) // manipulates the saved color data.

        colorPockerBuilder.show()
    }

    private fun initCallBack() {
        DeviceSettingLiveData.getInstance().getmBreathingLightSettingsBean().observe(this) { bean ->
            settingsBean = bean
            refUiByData()
        }
    }

    private fun refUiByData() {
        if (settingsBean != null) {
            binding.mainSwitch.isChecked = settingsBean!!.mainSwitch
            binding.llData.visibility = if (settingsBean!!.mainSwitch) View.VISIBLE else View.GONE
            binding.aSwitch.isChecked = settingsBean!!.lighttingSwitch
            val color = DiyDialUtils.getColorByRGBValue(settingsBean!!.lighttingColor)
            binding.view.setBackgroundColor(Color.rgb(color[0], color[1], color[2]))
            if (settingsBean!!.lightItems != null) {
                data.clear()
                data.addAll(settingsBean!!.lightItems)
                binding.rvData.post {
                    binding.rvData.adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    inner class LightItemAdapter(
        private val context: Context,
        private val data: MutableList<BreathingLightSettingsBean.LightItem>,
        var selected: (postion: Int) -> Unit
    ) :
        RecyclerView.Adapter<LightItemAdapter.LightItemViewHolder>() {

        inner class LightItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var tvTitle: AppCompatTextView = view.findViewById(R.id.tv1)
            val colorView: View = view.findViewById(R.id.view)
            val mSwitch: CustomMaterialSwitch = view.findViewById(R.id.mSwitch)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LightItemViewHolder {
            return LightItemViewHolder(
                LayoutInflater.from(context).inflate(R.layout.item_light, parent, false)
            )
        }

        override fun onBindViewHolder(holder: LightItemViewHolder, position: Int) {
            val light = data.get(position)
            holder.tvTitle.setText(getNameByLightType(light.lightType))
            holder.mSwitch.isChecked = light.lightSwitch
            val color = DiyDialUtils.getColorByRGBValue(light.lightColor)
            holder.colorView.setBackgroundColor(Color.rgb(color[0], color[1], color[2]))

            holder.mSwitch.setOnClickListener {
                if (holder.mSwitch.isChecked != light.lightSwitch) {
                    light.lightSwitch = holder.mSwitch.isChecked
                    setBreathingLightSettings()
                    refUiByData()
                }
            }

            holder.colorView.setOnClickListener {
                showSelectColor(light.lightColor, object : ColorEnvelopeListener {
                    override fun onColorSelected(envelope: ColorEnvelope?, fromUser: Boolean) {
                        if (envelope != null) {
                            light.lightColor = DiyDialUtils.getRGBValueByColor(envelope.argb[1], envelope.argb[2], envelope.argb[3])
                            // 设置给设备
                            setBreathingLightSettings()
                            refUiByData()
                        }
                    }
                })
            }

            holder.colorView.setOnLongClickListener {
                data.removeAt(position)
                settingsBean?.lightItems = data
                // 设置给设备
                setBreathingLightSettings()
                refUiByData()
                return@setOnLongClickListener true
            }

        }

        override fun getItemCount(): Int = data.size
    }


    @SuppressLint("MissingInflatedId")
    private fun showAddDialog() {
        val rootView = layoutInflater.inflate(R.layout.dialog_add_light, null)
        val dialog = CustomDialog.builder(this)
            .setContentView(rootView)
            .setWidth(ScreenUtils.getScreenWidth())
            .setHeight((ScreenUtils.getScreenHeight() * 0.8f).toInt())
            .setGravity(Gravity.CENTER)
            .build()
        val btnOk: AppCompatButton = rootView.findViewById(R.id.btnOk)
        val rvAdd: RecyclerView = rootView.findViewById(R.id.rvAdd)

        rvAdd.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        val addData = mutableListOf<LightAddItem>()
        SettingMenuCallBack.BreathingLightType.values().forEach { value ->
            addData.add(LightAddItem(value.type, data.lastOrNull { it.lightType == value.type } != null))
        }
        rvAdd.adapter = LightAddAdapter(this, addData)
        btnOk.setOnClickListener {
            if (settingsBean != null) {
                for (additem in addData) {
                    val item = settingsBean!!.lightItems.lastOrNull { it.lightType == additem.type }
                    if (item == null && additem.isCheck) {
                        settingsBean!!.lightItems.add(BreathingLightSettingsBean.LightItem().apply {
                            lightType = additem.type
                            lightSwitch = false
                            lightColor = DiyDialUtils.getRGBValueByColor(255,255,255)
                        })
                    }
                    if (item != null && !additem.isCheck) {
                        settingsBean!!.lightItems.remove(item)
                    }
                }
            }
            // 设置给设备
            setBreathingLightSettings()
            refUiByData()
            dialog.dismiss()
        }
        dialog.show()
    }

    data class LightAddItem(var type: Int, var isCheck: Boolean)

    inner class LightAddAdapter(
        private val context: Context,
        private val data: MutableList<LightAddItem>,
    ) : RecyclerView.Adapter<LightAddAdapter.LightAddViewHolder>() {

        inner class LightAddViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var tvTitle: AppCompatTextView = view.findViewById(R.id.tv1)
            val mSwitch: CheckBox = view.findViewById(R.id.mSwitch)

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LightAddViewHolder {
            return LightAddViewHolder(
                LayoutInflater.from(context).inflate(R.layout.item_add_light, parent, false)
            )
        }

        override fun onBindViewHolder(holder: LightAddViewHolder, position: Int) {
            holder.tvTitle.setText(getNameByLightType(data.get(position).type))
            holder.mSwitch.isChecked = data.get(position).isCheck

            holder.mSwitch.setOnClickListener {
                data.get(position).isCheck = holder.mSwitch.isChecked
            }
        }

        override fun getItemCount(): Int = data.size
    }


    private fun getNameByLightType(lightType: Int): String {
        return when (lightType) {
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_CHARGE_INDEX.type -> getString(R.string.s534)
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_LOW_POWER_INDEX.type -> getString(R.string.s535)
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_CALL_INDEX.type -> getString(R.string.s536)
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_MISS_CALL_INDEX.type -> "MissCall"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_SMS_INDEX.type -> "SMS"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_QQ_INDEX.type -> "QQ"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_WECHAT_INDEX.type -> "WECHAT"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_SKYPE_INDEX.type -> "SKYPE"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_WHATSAPP_INDEX.type -> "WHATSAPP"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_FACEBOOK_INDEX.type -> "FACEBOOK"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_MESSENGER_INDEX.type -> "MESSENGER"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_LINKEDLN_INDEX.type -> "LINKEDLN"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_TWITTER_INDEX.type -> "TWITTER"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_VIBER_INDEX.type -> "VIBER"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_LINE_INDEX.type -> "LINE"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_GMAIL_INDEX.type -> "GMAIL"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_OUTLOOK_INDEX.type -> "OUTLOOK"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_INSTAGRAM_INDEX.type -> "INSTAGRAM"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_SNAPCHAT_INDEX.type -> "SNAPCHAT"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_MAIL_INDEX.type -> "MAIL"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_CALENDAR_INDEX.type -> "CALENDAR"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_ZALO_INDEX.type -> "ZALO"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_TELEGRAM_INDEX.type -> "TELEGRAM"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_KAKAOTALK_INDEX.type -> "KAKAOTALK"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_VK_INDEX.type -> "VK"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_OK_INDEX.type -> "OK"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_ICQ_INDEX.type -> "ICQ"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_YOUTUBE_INDEX.type -> "YOUTUBE"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_PINTEREST_INDEX.type -> "PINTEREST"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_HANGOUT_INDEX.type -> "HANGOUT"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_PHONRPE_INDEX.type -> "PHONRP"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_GOOGLE_PLAY_INDEX.type -> "GOOGLE PLAY"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_PAYTM_INDEX.type -> "PAYTM"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_NAUKRI_INDEX.type -> "NAUKRI"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_INSHOT_INDEX.type -> "INSHOT"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_GOOGLE_NEWS_INDEX.type -> "GOOGLE NEWS"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_OLA_INDEX.type -> "OLA"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_UBER_INDEX.type -> "UBER"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_FLIPKART_INDEX.type -> "FLIPKART"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_AMAZON_INDEX.type -> "AMAZON"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_WHATSAPP_BUSINESS_INDEX.type -> "WHATSAPP BUSINESS"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_NOISEFIT_INDEX.type -> "NOISEFIT"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_GOOGLE_CLASSROOM_INDEX.type -> "GOOGLE CLASSROOM"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_TIKTOK_INDEX.type -> "TIKTOK"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_TEXTNOW_INDEX.type -> "TEXTNOW"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_DISCORD_INDEX.type -> "DISCORD"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_AIRTEL_THANKS_INDEX.type -> "AIRTEL THANKS"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_CHARGE_INDEX.type -> "CHARGE"
            SettingMenuCallBack.BreathingLightType.BREATHING_LIGHT_OTHER_INDEX.type -> "other"
            else -> "unknown"
        }
    }
}