package com.zjw.sdkdemo.function.language

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.zhapp.ble.BleCommonAttributes
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.LanguageListBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.LanguageCallBack
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivityLanguageBinding
import com.zjw.sdkdemo.livedata.BleConnectState
import java.lang.ref.WeakReference
import kotlin.Exception

/**
 * Created by Android on 2022/1/19.
 */
class LanguageActivity : BaseActivity() {

    //ALBANIAN = 0X01; //阿尔巴尼亚语
    //	ARABIC = 0X02; //阿拉伯语
    //	AM_HARIC= 0X03; //阿姆哈拉语
    //	IRISH = 0X04; //爱尔兰语
    //	ORIYA = 0X05; //奥利亚语
    //	BASQUE = 0X06; //巴斯克语
    //	BELARUSIAN = 0X07; //白俄罗斯语
    //	BULGARIAN = 0X08; //保加利亚语
    //	POLISH = 0X09; //波兰语
    //	PERSIAN = 0X10; //波斯语
    //	BOOLEAN = 0X11; //布尔语
    //	DANISH = 0X12; //丹麦语
    //	GERMAN = 0X13; //德语
    //	RUSSIAN = 0X14; //俄语
    //	FRENCH = 0X15; //法语
    //	FILIPINO = 0X16; //菲律宾语
    //	FINNISH = 0X17; //芬兰语
    //	CAMBODIAN = 0X18; //高棉语
    //	GEORGIAN = 0X19; //格鲁吉亚语
    //	GUJARATI = 0X20; //古吉拉特语
    //	KAZAKH = 0X21; //哈萨克语
    //	JAITAN_CREOLE = 0X22; //海地克里奥尔语
    //	KOREAN = 0X23; //韩语
    //	DUTCH = 0X24; //荷兰语
    //	GALICIAN = 0X25; //加利西亚语
    //	CATALAN = 0X26; //加泰罗尼亚语
    //	CZECH = 0X27; //捷克语
    //	KANNADA = 0X28; //卡纳达语
    //	CROATIAN = 0X29; //克罗地亚语
    //	KURDISH = 0X30; //库尔德语
    //	LATIN = 0X31; //拉丁语
    //	LAO = 0X32; //老挝语
    //	KINYARWANDA = 0X33; //卢旺达语
    //	ROMANIAN = 0X34; //罗马尼亚语
    //	MALAGASY = 0X35; //马尔加什语
    //	MARATHI = 0X36; //马拉地语
    //	MALAYALAM = 0X37; //马拉雅拉姆语
    //	MALAY = 0X38; //马来语
    //	MONGOLIAN = 0X39; //蒙古语
    //	BENGALI = 0X40; //孟加拉语
    //	BURMESE = 0X41; //缅甸语
    //	HMONG = 0X42; //苗语
    //	ZULU_SOYTH_AFRICA = 0X43; //南非祖鲁语
    //	NEPALI = 0X44; //尼泊尔语
    //	NORWEGIAN = 0X45; //挪威语
    //	PORTUGUESE = 0X46; //葡萄牙语
    //	JAPANESE = 0X47; //日语
    //	SWEDISH = 0X48; //瑞典语
    //	SERBIAN = 0X49; //塞尔维亚语
    //	SINHALA = 0X50; //僧伽罗语
    //	SLOVAK= 0X51; //斯洛伐克语
    //	SOMALI = 0X52; //索马里语
    //	TAJIK = 0X53; //塔吉克语
    //	TELUGU = 0X54; //泰卢固语
    //	TAMIL = 0X55; //泰米尔语
    //	THAI = 0X56; //泰语
    //	TURKISH = 0X57; //土耳其语
    //	URDU = 0X58; //乌尔都语
    //	UKRAINIAN = 0X59; //乌克兰语
    //	UZBEK = 0X60; //乌兹别克语
    //	SPANISH = 0X61; //西班牙语
    //	GREEK = 0X62; //希腊语
    //	HUNGARIAN = 0X63; //匈牙利语
    //	IGBO = 0X64; //伊博语
    //	ITALIAN = 0X65; //意大利语
    //	HINDI= 0X66; //印地语
    //	INDONESIAN = 0X67; //印尼语
    //	ENGLISH = 0X68; //英语
    //	VIETNAMESE = 0X69; //越南语
    //	TRADITIONAL_CHINESE = 0X70; //繁体中文
    //	SIMPLIFIED_CHINESE = 0X71; //简体中文
    //  HEBREW = 0X72; //希伯来语
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
        "简体中文","希伯来语"
    )

    private var languageList: MutableList<String> = mutableListOf()

    private var devList: MutableList<String> = mutableListOf()

    private var languageListBean: LanguageListBean? = null

    private val binding by lazy { ActivityLanguageBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s254)
        setContentView(binding.root)
        inits()
        clicks()
    }

    private fun clicks() {
        click(binding.btnGet) {
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().getLanguageList(null)
            }
        }

        click(binding.btnSet) {
            if (ControlBleTools.getInstance().isConnect) {
                if (languageListBean != null) {
                    val sId = languageListBean!!.languageList.get(binding.spinnerLan.selectedItemPosition)
                    ControlBleTools.getInstance().setLanguage(sId, null)
                }
            }
        }

        click(binding.btnSet2){
            if (ControlBleTools.getInstance().isConnect) {
                try {
                    var languageId = binding.etLanguageId.text.toString().trim().toInt()
                    ControlBleTools.getInstance().setLanguage(languageId, null)
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun inits() {
        BleConnectState.getInstance().observe(this, Observer<Int?> { integer ->
            when (integer) {
                BleCommonAttributes.STATE_CONNECTED -> {
                    binding.tvStatus.text = getString(R.string.s255) + getString(R.string.ble_connected_tips)
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
        })

        languageArr.forEach {
            languageList.add(it)
        }

        CallBackUtils.languageCallback = MyLanguageCallBack(this)
    }

    class MyLanguageCallBack(var activity: LanguageActivity) : LanguageCallBack {
        private lateinit var wkAct: WeakReference<LanguageActivity>

        init {
            wkAct = WeakReference(activity)
        }

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
                        if(!languageListBean!!.languageList.isNullOrEmpty()) {
                            devList.clear()
                            for (i in languageListBean!!.languageList){
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
                        binding.btnSet.isEnabled = true
                    } catch (e: Exception) {
                        ToastUtils.showShort(getString(R.string.s256))
                    }
                }

            }
        }
    }
}