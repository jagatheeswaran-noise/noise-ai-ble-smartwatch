package com.zjw.sdkdemo.function.berry

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.LanguageListBean
import com.zhapp.ble.bean.SettingTimeBean
import com.zhapp.ble.bean.UserInfo
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.LanguageCallBack
import com.zhapp.ble.callback.UserInfoCallBack
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivityBerryUserSetBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.utils.ToastDialog
import java.lang.ref.WeakReference
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Created by Android on 2024/10/24.
 */
class BerryUserSetActivity : BaseActivity() {
    private val binding: ActivityBerryUserSetBinding by lazy { ActivityBerryUserSetBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s606)
        setContentView(binding.root)
        initListener()
        initView()
    }

    private fun initView() {
        initUserInfoData()
        initLanguage()
        //语言

    }


    private fun initListener() {
        click(binding.btnGetLanguage) {
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().getLanguageList(baseSendCmdStateListener)
            }
        }

        click(binding.btnSetLanguage) {
            if (ControlBleTools.getInstance().isConnect) {
                if (languageListBean != null) {
                    val sId = languageListBean!!.languageList.get(binding.spinnerLan.selectedItemPosition)
                    ControlBleTools.getInstance().setLanguage(sId, baseSendCmdStateListener)
                }
            }
        }

        click(binding.btnSetLanguage2) {
            if (ControlBleTools.getInstance().isConnect) {
                try {
                    var languageId = binding.etLanguageId.text.toString().trim().toInt()
                    ControlBleTools.getInstance().setLanguage(languageId, baseSendCmdStateListener)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        binding.chbDistanceUnit.setOnCheckedChangeListener { buttonView, isChecked ->
            if (ControlBleTools.getInstance().isConnect) {
                if (buttonView.isChecked) {
                    ControlBleTools.getInstance().setDistanceUnit(0, baseSendCmdStateListener)
                } else {
                    ControlBleTools.getInstance().setDistanceUnit(1, baseSendCmdStateListener)
                }
            }
        }

        binding.chbTemperatureUnit.setOnCheckedChangeListener { buttonView, isChecked ->
            if (ControlBleTools.getInstance().isConnect) {
                if (buttonView.isChecked) {
                    ControlBleTools.getInstance().setTemperatureUnit(0, baseSendCmdStateListener)
                } else {
                    ControlBleTools.getInstance().setTemperatureUnit(1, baseSendCmdStateListener)
                }
            }
        }

        click(binding.btnSetUserProfile) {
            setUserProfile()
        }

        click(binding.btnGetUserProfile) {
            getUserProfile()
        }

    }

    //region 语言
    private var languageArr = arrayOf(
        "阿尔巴尼亚语", "阿拉伯语", "阿姆哈拉语", "爱尔兰语", "奥利亚语",
        "巴斯克语", "白俄罗斯语", "保加利亚语", "波兰语", "波斯语",
        "布尔语", "丹麦语", "德语", "俄语", "法语",
        "菲律宾语", "芬兰语", "高棉语", "格鲁吉亚语", "古吉拉特语",
        "哈萨克语", "海地克里奥尔语", "韩语", "荷兰语", "加利西亚语",
        "加泰罗尼亚语", "捷克语", "卡纳达语", "克罗地亚语", "库尔德语",
        "拉丁语", "老挝语", "卢旺达语", "罗马尼亚语", "马尔加什语",
        "马拉地语", "马拉雅拉姆语", "马来语", "蒙古语", "孟加拉语",
        "缅甸语", "苗语", "南非祖鲁语", "尼泊尔语", "挪威语",
        "葡萄牙语", "日语", "瑞典语", "塞尔维亚语", "僧伽罗语",
        "斯洛伐克语", "索马里语", "塔吉克语", "泰卢固语", "泰米尔语",
        "泰语", "土耳其语", "乌尔都语", "乌克兰语", "乌兹别克语",
        "西班牙语", "希腊语", "匈牙利语", "伊博语", "意大利语",
        "印地语", "印尼语", "英语", "越南语", "繁体中文",
        "简体中文", "希伯来语"
    )

    private var languageList: MutableList<String> = mutableListOf()

    private var devList: MutableList<String> = mutableListOf()

    private var languageListBean: LanguageListBean? = null

    private fun initLanguage() {
        languageArr.forEach {
            languageList.add(it)
        }
        CallBackUtils.languageCallback = MyLanguageCallBack(this)
    }

    class MyLanguageCallBack(var activity: BerryUserSetActivity) : LanguageCallBack {
        private var wkAct: WeakReference<BerryUserSetActivity> = WeakReference(activity)

        override fun onResult(bean: LanguageListBean?) {
            if (bean != null) {
                //{defaultLanguageId=104, selectLanguageId=104, list=[104, 71, 21, 19, 101, 97]}
                LogUtils.d(bean.toString())
                wkAct.get()?.apply {
                    try {
                        languageListBean = bean
                        val sIndex = ConvertUtils.int2HexString(languageListBean!!.selectLanguageId).toInt()
                        val sValue = languageList.get(sIndex - 1)
                        LogUtils.d("默认语言 $sValue")
                        if (!languageListBean!!.languageList.isNullOrEmpty()) {
                            devList.clear()
                            for (i in languageListBean!!.languageList) {
                                val index = ConvertUtils.int2HexString(i).toInt()
                                val value = languageList.get(index - 1)
                                devList.add(value)
                            }
                        }
                        LogUtils.d("语言列表：${GsonUtils.toJson(devList)}")
                        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, devList)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        //绑定 Adapter到控件
                        binding.spinnerLan.adapter = adapter
                        binding.spinnerLan.setSelection(devList.indexOf(sValue))
                        binding.btnSetLanguage.isEnabled = true
                    } catch (e: Exception) {
                        ToastUtils.showShort(getString(R.string.s256))
                    }
                }

            }
        }
    }
    //endregion

    fun setUserProfile() {
        if (!ControlBleTools.getInstance().isConnect) return
        val bean = testUserInfo()
        ControlBleTools.getInstance().setUserProfile(bean, baseSendCmdStateListener)
    }

    fun getUserProfile() {
        if (!ControlBleTools.getInstance().isConnect) return
        ControlBleTools.getInstance().getUserProfile(baseSendCmdStateListener)
    }

    private fun initUserInfoData() {
        CallBackUtils.setUserInfoCallBack(object : UserInfoCallBack {
            override fun onUserInfo(userInfo: UserInfo) {
                ToastDialog.showToast(this@BerryUserSetActivity, "用户信息：$userInfo")
            }

            override fun onDayTimeSleep(isDayTime: Boolean) {
                LogUtils.d("是否白天睡眠：$isDayTime")
            }

            override fun onAppWeatherSwitch(isSwitch: Boolean) {
                LogUtils.d("app天气开关：$isSwitch")
            }
        })
    }

    private fun testUserInfo(): UserInfo {
        var date = Date()
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        try {
            date = simpleDateFormat.parse("2021-10-01")
        } catch (e: ParseException) {
            e.printStackTrace()
            date.time = 0
        }
        val bean = UserInfo()
        bean.userName = "test_user_name" // 最大支持90字节=30中文字符  Maximum support 90 bytes = 30 Chinese characters
        bean.age = 18
        bean.height = 170 //cm
        bean.weight = 60.0f //KG
        bean.birthday = date.time.toInt()
        bean.sex = 2 //1=male，2=female
        bean.maxHr = 80 //最大心率（次/分）  Maximum heart rate (beats/minute)
        bean.calGoal = 180 //卡路里目标 千卡  Calorie goals kcal
        bean.stepGoal = 18000 //步数目标 步  step goal steps
        bean.distanceGoal = 18 // 距离目标 米  Meters from target
        bean.standingTimesGoal = 18 //有效站立目标 次  Effective standing target times
        bean.goalSleepMinute = 7 * 60
        //天气开关是否打开  Whether the weather switch is on
        //bean.appWeatherSwitch = Utils.getSharedPreferences(this).getBoolean(SP_WEATHER_SWITCH,false);
        bean.appWeatherSwitch = false
        bean.userSleepStartTime = SettingTimeBean(20, 1)
        bean.userSleepEndTime = SettingTimeBean(8, 2)
        return bean
    }

}