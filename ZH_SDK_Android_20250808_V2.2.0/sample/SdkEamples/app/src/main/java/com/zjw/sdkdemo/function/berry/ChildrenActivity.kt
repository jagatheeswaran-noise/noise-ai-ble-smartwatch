package com.zjw.sdkdemo.function.berry

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.Observer
import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.ToastUtils
import com.google.gson.GsonBuilder
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.DeviceInfoBean
import com.zhapp.ble.bean.ExamSettingsBean
import com.zhapp.ble.bean.SchedulerBean
import com.zhapp.ble.bean.SchedulerBean.AlertBean
import com.zhapp.ble.bean.SchedulerBean.HabitBean
import com.zhapp.ble.bean.SchedulerBean.ReminderBean
import com.zhapp.ble.bean.SchoolBean
import com.zhapp.ble.bean.SettingTimeBean
import com.zhapp.ble.bean.TimeBean
import com.zhapp.ble.bean.berry.BerryBigFileResultBean
import com.zhapp.ble.bean.berry.children.AddParentInfoBean
import com.zhapp.ble.bean.berry.children.ChallengeInfoBean
import com.zhapp.ble.bean.berry.children.ChallengeResultBean
import com.zhapp.ble.bean.berry.children.ChildrenInfoBean
import com.zhapp.ble.bean.berry.children.EarningsExchangeBean
import com.zhapp.ble.bean.berry.children.EarningsInfoBean
import com.zhapp.ble.bean.berry.children.EarningsPriceBean
import com.zhapp.ble.bean.berry.children.FlashCardIdsBean
import com.zhapp.ble.bean.berry.children.FlashCardProgressBean
import com.zhapp.ble.bean.berry.children.FlashCardThemeBean
import com.zhapp.ble.bean.berry.children.MedalInfoBean
import com.zhapp.ble.bean.berry.children.ParentInfoBean
import com.zhapp.ble.bean.berry.children.SchedulerResultBean
import com.zhapp.ble.callback.BerryBigFileResultCallBack
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.ChildrenCallBack
import com.zhapp.ble.callback.DeviceInfoCallBack
import com.zhapp.ble.callback.ExamModeDetailCallback
import com.zhapp.ble.callback.UploadBigDataListener
import com.zhapp.ble.parsing.ParsingStateManager
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.ActivityChildrenBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import com.zjw.sdkdemo.livedata.DeviceSettingLiveData
import com.zjw.sdkdemo.utils.AssetUtils.getAssetBitmap
import com.zjw.sdkdemo.utils.ToastDialog
import com.zjw.sdkdemo.utils.customdialog.CustomDialog
import com.zjw.sdkdemo.utils.customdialog.MyDialog
import java.io.File
import kotlin.collections.arrayListOf

/**
 * Created by Android on 2025/2/11.
 */
class ChildrenActivity : BaseActivity() {

    val binding: ActivityChildrenBinding by lazy { ActivityChildrenBinding.inflate(layoutInflater) }

    private val gson by lazy { GsonBuilder().setPrettyPrinting().create() }

    private val mFilePath = PathUtils.getExternalAppCachePath() + "/parentHead"
    private lateinit var parentHeadfile: File

    private var parentInfos: ParentInfoBean? = null
    private var flashCardInfos: FlashCardIdsBean? = null
    private var challengeInfoBean: ChallengeInfoBean? = null
    private var earningsPriceBean: EarningsInfoBean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s649)
        setContentView(binding.root)
        initView()
        initEvent()
        initCallBack()

    }

    private fun initView() {
        FileUtils.createOrExistsDir(mFilePath)
        binding.tvTip1.setText("操作方法\n\n1.将[头像文件(图片资源)]，放到[${mFilePath}]目录下\n\n2.点击[选择文件]按钮，选择文件\n\n3.点击[添加家长信息]\n\n4.不选择将使用默认资源")
        binding.cbFc1.setText(getString(R.string.s688) + " 1")
        binding.cbFc2.setText(getString(R.string.s688) + " 2")
        binding.cbFc3.setText(getString(R.string.s688) + " 3")
        binding.cbFc4.setText(getString(R.string.s688) + " 4")
        binding.cbFc5.setText(getString(R.string.s688) + " 5")
    }

    private fun initEvent() {
        click(binding.btnSetChildrenInfo) {
            val info = ChildrenInfoBean()
            info.name = binding.etChildrenName.text.toString()
            info.bloodGroup = binding.etBloodGroup.text.toString()
            info.allergy = binding.etAllergy.text.toString()
            info.medication = binding.etMedication.text.toString()
            info.address = binding.etAddress.text.toString()
            ControlBleTools.getInstance().setChildrenInfoByBerry(info, baseSendCmdStateListener)
        }

        click(binding.btnGetChildrenInfo) {
            ControlBleTools.getInstance().getChildrenInfoByBerry(baseSendCmdStateListener)
        }

        click(binding.btnGetParentInfo) {
            ControlBleTools.getInstance().getParentInfoByBerry(baseSendCmdStateListener)
        }

        click(binding.btnGetHeadFile) {
            val files = FileUtils.listFilesInDir(mFilePath)
            if (files.isNullOrEmpty()) {
                ToastUtils.showShort("$mFilePath 目录文件为空")
                return@click
            }
            showListDialog(files)
        }

        click(binding.btnSetParentInfo) {
            sendAddParentInfo()
        }

        click(binding.btnDelParentInfo) {
            if (parentInfos == null || parentInfos!!.parentBaseInfos == null) {
                ToastUtils.showShort(R.string.s659)
                return@click
            }
            val ids = mutableListOf<Int>()
            for (item in parentInfos!!.parentBaseInfos) {
                ids.add(item.id)
            }
            ControlBleTools.getInstance().delParentInfoByBerry(ids, baseSendCmdStateListener)
        }

        click(binding.btnGetFcInfo) {
            ControlBleTools.getInstance().getFlashCardIdsByBerry(baseSendCmdStateListener)
        }

        binding.cbFc1.setOnCheckedChangeListener(MyFlashCardCheckListener())
        binding.cbFc2.setOnCheckedChangeListener(MyFlashCardCheckListener())
        binding.cbFc3.setOnCheckedChangeListener(MyFlashCardCheckListener())
        binding.cbFc4.setOnCheckedChangeListener(MyFlashCardCheckListener())
        binding.cbFc5.setOnCheckedChangeListener(MyFlashCardCheckListener())

        click(binding.btnSetFcInfo) {
            setFcInfo()
        }

        click(binding.btnDelFcInfo) {
            if (flashCardInfos == null) {
                ToastUtils.showShort(R.string.s662)
                return@click
            }
            ControlBleTools.getInstance().delFlashCardByBerry(flashCardInfos!!.list, baseSendCmdStateListener)

            ThreadUtils.runOnUiThreadDelayed({
                binding.btnGetFcInfo.callOnClick()
            }, 1000)
        }

        click(binding.btnGetChallenge) {
            ControlBleTools.getInstance().getChallengeInfoByBerry(baseSendCmdStateListener)
        }

        click(binding.btnSetChallenge) {
            if (challengeInfoBean == null) {
                ToastUtils.showShort(R.string.s677)
                return@click
            }
            for (item in challengeInfoBean!!.detailList) {
                for (i in 0..<item.content.size) {
                    item.content[i] = "new ${item.content[i]}"
                }
            }
            ControlBleTools.getInstance().modifyChallengeInfoByBerry(challengeInfoBean, baseSendCmdStateListener)
        }

        click(binding.btnSetEarnings) {
            try {
                val price = EarningsPriceBean()
                price.stepsGoal = binding.etEStep.text.toString().trim().toInt()
                price.sleepGoal = binding.etESleep.text.toString().trim().toInt()
                price.calGoal = binding.etECal.text.toString().trim().toInt()
                price.distanceGoal = binding.etEDis.text.toString().trim().toInt()
                price.flashCard = binding.etEFc.text.toString().trim().toInt()
                price.task = binding.etETask.text.toString().trim().toInt()
                price.challenge = binding.etEChallenge.text.toString().trim().toInt()
                price.medals = binding.etEZMedals.text.toString().trim().toInt()
                ControlBleTools.getInstance().setEarningsPriceByBerry(price, baseSendCmdStateListener)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                ToastDialog.showToast(this@ChildrenActivity, getString(R.string.s238))
                return@click
            }
        }

        click(binding.btnGetEarnings) {
            ControlBleTools.getInstance().getEarningsInfoByBerry(baseSendCmdStateListener)
        }

        click(binding.btnSetEarningsInfo) {
            setEarningsInfo()
        }

        click(binding.btnGetScheduler) {
            ControlBleTools.getInstance().getScheduleReminder(baseSendCmdStateListener)
        }

        click(binding.btnSetScheduler) {
            sendScheduler()
        }

        click(binding.btnGetSchoolMode) {
            ControlBleTools.getInstance().getSchoolMode(baseSendCmdStateListener)
        }

        click(binding.btnSetSchoolMode) {
            sendSchoolMode()
        }

        click(binding.btnGetExMode) {
            ControlBleTools.getInstance().getExamReminderSettings(baseSendCmdStateListener)
        }

        click(binding.btnSetExMode) {
            sendExamReminder()
        }

        click(binding.btnGetMedal) {
            ControlBleTools.getInstance().getMedalInfoByBerry(baseSendCmdStateListener)
        }

        click(binding.btnSetMedal) {
            setMedalInfo()
        }
    }

    //region 家长信息
    private fun sendAddParentInfo() {
        FileUtils.deleteAllInDir(getExternalFilesDir("agps"))
        if (deviceInfo == null) {
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().getDeviceInfo(baseSendCmdStateListener)
            }
            return
        }
        if (parentInfos == null || parentInfos!!.parentBaseInfos == null) {
            ToastUtils.showShort(R.string.s659)
            return
        }
        if (parentInfos!!.headImgWidth == 0 || parentInfos!!.headImgHeight == 0 || parentInfos!!.headImgRadius == 0) {
            ToastUtils.showShort("Error ! headImgWidth == ${parentInfos!!.headImgWidth},headImgWidth == ${parentInfos!!.headImgHeight},headImgWidth == ${parentInfos!!.headImgRadius} ")
            return
        }
        val adp = AddParentInfoBean()
        adp.id = binding.etPID.text.toString().trim().toInt()
        adp.name = binding.etPName.text.toString()
        adp.number = binding.etPPhone.text.toString()
        if (::parentHeadfile.isInitialized) {
            var headImg = ConvertUtils.bytes2Bitmap(FileIOUtils.readFile2BytesByStream(parentHeadfile))
            headImg = ImageUtils.scale(headImg, parentInfos!!.headImgWidth, parentInfos!!.headImgHeight)
            headImg = ImageUtils.toRoundCorner(headImg, parentInfos!!.headImgRadius * 1.0f)
            adp.avatarImg = headImg
        } else {
            var headImg = getAssetBitmap(this, "children" + File.separator + "p_head.png")
            headImg = ImageUtils.scale(headImg, parentInfos!!.headImgWidth, parentInfos!!.headImgHeight)
            headImg = ImageUtils.toRoundCorner(headImg, parentInfos!!.headImgRadius * 1.0f)
            adp.avatarImg = headImg
        }
        ControlBleTools.getInstance().addParentInfoByBerry(adp, deviceInfo!!.equipmentNumber, MyUploadBigDataListener(1))
    }
    //endregion

    //region 闪存卡
    private var fcTheme1: FlashCardThemeBean.Theme? = null
    private var fcTheme2: FlashCardThemeBean.Theme? = null
    private var fcTheme3: FlashCardThemeBean.Theme? = null
    private var fcTheme4: FlashCardThemeBean.Theme? = null
    private var fcTheme5: FlashCardThemeBean.Theme? = null
    private var fcDataIndex = 0

    inner class MyFlashCardCheckListener : CompoundButton.OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            if (isChecked) {
                fcDataIndex += 1
            }
            when (buttonView?.id) {
                R.id.cbFc1 -> {
                    if (isChecked) {
                        fcTheme1 = FlashCardThemeBean.Theme().apply {
                            themeId = 1
                            typeId = 1
                            topicId = 1
                            topicTitle = "$fcDataIndex 1111111111"
                            promptContent = "$fcDataIndex 111"
                            topics = mutableListOf<FlashCardThemeBean.BaseTopicType>().apply {
                                //getMaxNum() == 10 //--------------Up to 10 can be set, and there must be 10, otherwise it is invalid--------------
                                add(FlashCardThemeBean.TopicType1().apply {
                                    questionTextByEN = "$fcDataIndex because"
                                    questionTextByHI = "$fcDataIndex क्योंकि"
                                    answerTextByEN = "$fcDataIndex Gives a reason"
                                    answerTextByHI = "$fcDataIndex कारण बताता है"
                                })
                                add(FlashCardThemeBean.TopicType1().apply {
                                    questionTextByEN = "$fcDataIndex people"
                                    questionTextByHI = "$fcDataIndex लोग"
                                    answerTextByEN = "$fcDataIndex Human beings"
                                    answerTextByHI = "$fcDataIndex मानव समुदाय"
                                })
                                add(FlashCardThemeBean.TopicType1().apply {
                                    questionTextByEN = "$fcDataIndex country"
                                    questionTextByHI = "$fcDataIndex देश"
                                    answerTextByEN = "$fcDataIndex Nation/land"
                                    answerTextByHI = "$fcDataIndex राष्ट्र/भूमि"
                                })
                                add(FlashCardThemeBean.TopicType1().apply {
                                    questionTextByEN = "$fcDataIndex morning"
                                    questionTextByHI = "$fcDataIndex सुबह"
                                    answerTextByEN = "$fcDataIndex Early part of day"
                                    answerTextByHI = "$fcDataIndex दिन का आरंभिक भाग"
                                })
                                add(FlashCardThemeBean.TopicType1().apply {
                                    questionTextByEN = "$fcDataIndex evening"
                                    questionTextByHI = "$fcDataIndex शाम"
                                    answerTextByEN = "$fcDataIndex Late part of day"
                                    answerTextByHI = "$fcDataIndex दिन का अंतिम भाग"
                                })
                                add(FlashCardThemeBean.TopicType1().apply {
                                    questionTextByEN = "$fcDataIndex because"
                                    questionTextByHI = "$fcDataIndex क्योंकि"
                                    answerTextByEN = "$fcDataIndex Gives a reason"
                                    answerTextByHI = "$fcDataIndex कारण बताता है"
                                })
                                add(FlashCardThemeBean.TopicType1().apply {
                                    questionTextByEN = "$fcDataIndex people"
                                    questionTextByHI = "$fcDataIndex लोग"
                                    answerTextByEN = "$fcDataIndex Human beings"
                                    answerTextByHI = "$fcDataIndex मानव समुदाय"
                                })
                                add(FlashCardThemeBean.TopicType1().apply {
                                    questionTextByEN = "$fcDataIndex country"
                                    questionTextByHI = "$fcDataIndex देश"
                                    answerTextByEN = "$fcDataIndex Nation/land"
                                    answerTextByHI = "$fcDataIndex राष्ट्र/भूमि"
                                })
                                add(FlashCardThemeBean.TopicType1().apply {
                                    questionTextByEN = "$fcDataIndex morning"
                                    questionTextByHI = "$fcDataIndex सुबह"
                                    answerTextByEN = "$fcDataIndex Early part of day"
                                    answerTextByHI = "$fcDataIndex दिन का आरंभिक भाग"
                                })
                                add(FlashCardThemeBean.TopicType1().apply {
                                    questionTextByEN = "$fcDataIndex evening"
                                    questionTextByHI = "$fcDataIndex शाम"
                                    answerTextByEN = "$fcDataIndex Late part of day"
                                    answerTextByHI = "$fcDataIndex दिन का अंतिम भाग"
                                })
                            }
                        }
                        ToastDialog.showToast(this@ChildrenActivity, gson.toJson(fcTheme1))
                    } else {
                        fcTheme1 = null
                    }
                }

                R.id.cbFc2 -> {
                    if (isChecked) {
                        fcTheme2 = FlashCardThemeBean.Theme().apply {
                            themeId = 2
                            typeId = 2
                            topicId = 2
                            topicTitle = "$fcDataIndex 2222222222"
                            promptContent = "$fcDataIndex 222"
                            topics = mutableListOf<FlashCardThemeBean.BaseTopicType>().apply {
                                //getMaxNum() == 10 //--------------Up to 10 can be set, and there must be 10, otherwise it is invalid--------------
                                add(FlashCardThemeBean.TopicType2().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_q1.png")
                                    answerImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_a1.png")
                                    answerText = "$fcDataIndex Bahrain"
                                })
                                add(FlashCardThemeBean.TopicType2().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_q2.png")
                                    answerImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_a2.png")
                                    answerText = "$fcDataIndex Bangladesh"
                                })
                                add(FlashCardThemeBean.TopicType2().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_q3.png")
                                    answerImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_a3.png")
                                    answerText = "$fcDataIndex Bhutan"
                                })
                                add(FlashCardThemeBean.TopicType2().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_q4.png")
                                    answerImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_a4.png")
                                    answerText = "$fcDataIndex Hong Kong"
                                })
                                add(FlashCardThemeBean.TopicType2().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_q5.png")
                                    answerImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_a5.png")
                                    answerText = "$fcDataIndex South Africa"
                                })
                                add(FlashCardThemeBean.TopicType2().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_q6.png")
                                    answerImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_a6.png")
                                    answerText = "$fcDataIndex Switzerland"
                                })
                                add(FlashCardThemeBean.TopicType2().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_q7.png")
                                    answerImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_a7.png")
                                    answerText = "$fcDataIndex Turkey"
                                })
                                add(FlashCardThemeBean.TopicType2().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_q8.png")
                                    answerImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_a8.png")
                                    answerText = "$fcDataIndex Ukraine"
                                })
                                add(FlashCardThemeBean.TopicType2().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_q9.png")
                                    answerImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_a9.png")
                                    answerText = "$fcDataIndex Ukraine2"
                                })
                                add(FlashCardThemeBean.TopicType2().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_q10.png")
                                    answerImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t2_a10.png")
                                    answerText = "$fcDataIndex Ukraine3"
                                })
                            }
                        }
                        ToastDialog.showToast(this@ChildrenActivity, gson.toJson(fcTheme2))
                    } else {
                        fcTheme2 = null
                    }
                }

                R.id.cbFc3 -> {
                    if (isChecked) {
                        fcTheme3 = FlashCardThemeBean.Theme().apply {
                            themeId = 3
                            typeId = 3
                            topicId = 3
                            topicTitle = "$fcDataIndex 3333333333"
                            promptContent = "$fcDataIndex 333"
                            topics = mutableListOf<FlashCardThemeBean.BaseTopicType>().apply {
                                //getMaxNum() == 10 //--------------Up to 10 can be set, and there must be 10, otherwise it is invalid --------------
                                add(FlashCardThemeBean.TopicType3().apply {
                                    questionText = "$fcDataIndex Guess the macros of the food 1 "
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t3.png")
                                    answerText1 = "$fcDataIndex Protein: 11g 1"
                                    answerText2 = "$fcDataIndex Carbs: 22.8g 1"
                                    answerText3 = "$fcDataIndex Fats: 0.3g 2"
                                    answerText4 = "$fcDataIndex Fibre: 2.6g 3"
                                })
                                add(FlashCardThemeBean.TopicType3().apply {
                                    questionText = "$fcDataIndex Guess the macros of the food 2"
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t3.png")
                                    answerText1 = "$fcDataIndex Protein: 11g 2"
                                    answerText2 = "$fcDataIndex Carbs: 22.8g 2"
                                    answerText3 = "$fcDataIndex Fats: 0.3g 2"
                                    answerText4 = "$fcDataIndex Fibre: 2.6g 2"
                                })
                                add(FlashCardThemeBean.TopicType3().apply {
                                    questionText = "$fcDataIndex Guess the macros of the food 3"
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t3.png")
                                    answerText1 = "$fcDataIndex Protein: 11g 3"
                                    answerText2 = "$fcDataIndex Carbs: 22.8g 3"
                                    answerText3 = "$fcDataIndex Fats: 0.3g 3"
                                    answerText4 = "$fcDataIndex Fibre: 2.6g 3"
                                })
                                add(FlashCardThemeBean.TopicType3().apply {
                                    questionText = "$fcDataIndex Guess the macros of the food 4"
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t3.png")
                                    answerText1 = "$fcDataIndex Protein: 11g 4"
                                    answerText2 = "$fcDataIndex Carbs: 22.8g 4"
                                    answerText3 = "$fcDataIndex Fats: 0.3g 4"
                                    answerText4 = "$fcDataIndex Fibre: 2.6g 4"
                                })
                                add(FlashCardThemeBean.TopicType3().apply {
                                    questionText = "$fcDataIndex Guess the macros of the food 5"
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t3.png")
                                    answerText1 = "$fcDataIndex Protein: 11g 5"
                                    answerText2 = "$fcDataIndex Carbs: 22.8g 5"
                                    answerText3 = "$fcDataIndex Fats: 0.3g 5"
                                    answerText4 = "$fcDataIndex Fibre: 2.6g 5"
                                })
                                add(FlashCardThemeBean.TopicType3().apply {
                                    questionText = "$fcDataIndex Guess the macros of the food 6"
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t3.png")
                                    answerText1 = "$fcDataIndex Protein: 11g 6"
                                    answerText2 = "$fcDataIndex Carbs: 22.8g 6"
                                    answerText3 = "$fcDataIndex Fats: 0.3g 6"
                                    answerText4 = "$fcDataIndex Fibre: 2.6g 6"
                                })
                                add(FlashCardThemeBean.TopicType3().apply {
                                    questionText = "$fcDataIndex Guess the macros of the food 7"
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t3.png")
                                    answerText1 = "$fcDataIndex Protein: 11g 7"
                                    answerText2 = "$fcDataIndex Carbs: 22.8g 7"
                                    answerText3 = "$fcDataIndex Fats: 0.3g 7"
                                    answerText4 = "$fcDataIndex Fibre: 2.6g 7"
                                })
                                add(FlashCardThemeBean.TopicType3().apply {
                                    questionText = "$fcDataIndex Guess the macros of the food 8"
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t3.png")
                                    answerText1 = "$fcDataIndex Protein: 11g 8"
                                    answerText2 = "$fcDataIndex Carbs: 22.8g 8"
                                    answerText3 = "$fcDataIndex Fats: 0.3g 8"
                                    answerText4 = "$fcDataIndex Fibre: 2.6g 8"
                                })
                                add(FlashCardThemeBean.TopicType3().apply {
                                    questionText = "$fcDataIndex Guess the macros of the food 9"
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t3.png")
                                    answerText1 = "$fcDataIndex Protein: 11g 9"
                                    answerText2 = "$fcDataIndex Carbs: 22.8g 9"
                                    answerText3 = "$fcDataIndex Fats: 0.3g 9"
                                    answerText4 = "$fcDataIndex Fibre: 2.6g 9"
                                })
                                add(FlashCardThemeBean.TopicType3().apply {
                                    questionText = "$fcDataIndex Guess the macros of the food 0"
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t3.png")
                                    answerText1 = "$fcDataIndex Protein: 11g 0"
                                    answerText2 = "$fcDataIndex Carbs: 22.8g 0"
                                    answerText3 = "$fcDataIndex Fats: 0.3g 0"
                                    answerText4 = "$fcDataIndex Fibre: 2.6g 0"
                                })
                            }
                        }
                        ToastDialog.showToast(this@ChildrenActivity, gson.toJson(fcTheme3))
                    } else {
                        fcTheme3 = null
                    }
                }

                R.id.cbFc4 -> {
                    if (isChecked) {
                        fcTheme4 = FlashCardThemeBean.Theme().apply {
                            themeId = 4
                            typeId = 4
                            topicId = 4
                            topicTitle = "$fcDataIndex 4444444444"
                            promptContent = "$fcDataIndex 444"
                            topics = mutableListOf<FlashCardThemeBean.BaseTopicType>().apply {
                                //getMaxNum() == 10 //--------------Up to 10 can be set, and there must be 10, otherwise it is invalid --------------
                                add(FlashCardThemeBean.TopicType4().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t4.png")
                                    answerNumber = 1
                                })
                                add(FlashCardThemeBean.TopicType4().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t4.png")
                                    answerNumber = 2
                                })
                                add(FlashCardThemeBean.TopicType4().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t4.png")
                                    answerNumber = 3
                                })
                                add(FlashCardThemeBean.TopicType4().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t4.png")
                                    answerNumber = 4
                                })
                                add(FlashCardThemeBean.TopicType4().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t4.png")
                                    answerNumber = 1
                                })
                                add(FlashCardThemeBean.TopicType4().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t4.png")
                                    answerNumber = 2
                                })
                                add(FlashCardThemeBean.TopicType4().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t4.png")
                                    answerNumber = 3
                                })
                                add(FlashCardThemeBean.TopicType4().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t4.png")
                                    answerNumber = 4
                                })
                                add(FlashCardThemeBean.TopicType4().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t4.png")
                                    answerNumber = 1
                                })
                                add(FlashCardThemeBean.TopicType4().apply {
                                    questionImg = getAssetBitmap(this@ChildrenActivity, "children" + File.separator + "fc_t4.png")
                                    answerNumber = 2
                                })
                            }
                        }
                        ToastDialog.showToast(this@ChildrenActivity, gson.toJson(fcTheme4))
                    } else {
                        fcTheme4 = null
                    }
                }

                R.id.cbFc5 -> {
                    if (isChecked) {
                        fcTheme5 = FlashCardThemeBean.Theme().apply {
                            themeId = 5
                            typeId = 5
                            topicId = 5
                            topicTitle = "$fcDataIndex 5555555555"
                            promptContent = "$fcDataIndex 555"
                            topics = mutableListOf<FlashCardThemeBean.BaseTopicType>().apply {
                                //getMaxNum() == 10 //--------------Up to 10 can be set, and there must be 10, otherwise it is invalid --------------
                                add(FlashCardThemeBean.TopicType5().apply {
                                    questionText = "$fcDataIndex Which planet is closest to the Sun?"
                                    answerNumber = 4
                                    answerText1 = "$fcDataIndex Earth"
                                    answerText2 = "$fcDataIndex Venus"
                                    answerText3 = "$fcDataIndex Mars"
                                    answerText4 = "$fcDataIndex Mercury"
                                })
                                add(FlashCardThemeBean.TopicType5().apply {
                                    questionText = "$fcDataIndex What is the capital of India?"
                                    answerNumber = 3
                                    answerText1 = "$fcDataIndex Mumbai"
                                    answerText2 = "$fcDataIndex Chennai"
                                    answerText3 = "$fcDataIndex New Delhi"
                                    answerText4 = "$fcDataIndex Kolkata"
                                })
                                add(FlashCardThemeBean.TopicType5().apply {
                                    questionText = "$fcDataIndex Which animal is known as the \"King of the Jungle\"?"
                                    answerNumber = 3
                                    answerText1 = "$fcDataIndex Elephant"
                                    answerText2 = "$fcDataIndex Tiger"
                                    answerText3 = "$fcDataIndex Lion"
                                    answerText4 = "$fcDataIndex Zebra"
                                })
                                add(FlashCardThemeBean.TopicType5().apply {
                                    questionText = "$fcDataIndex Which country is famous for the Eiffel Tower?"
                                    answerNumber = 2
                                    answerText1 = "$fcDataIndex Italy"
                                    answerText2 = "$fcDataIndex France"
                                    answerText3 = "$fcDataIndex Spain"
                                    answerText4 = "$fcDataIndex Germany"
                                })
                                add(FlashCardThemeBean.TopicType5().apply {
                                    questionText = "$fcDataIndex Which fruit is known for having many seeds and is red or pink in color?"
                                    answerNumber = 2
                                    answerText1 = "$fcDataIndex Apple"
                                    answerText2 = "$fcDataIndex Watermelon"
                                    answerText3 = "$fcDataIndex Mango"
                                    answerText4 = "$fcDataIndex Pear"
                                })
                                add(FlashCardThemeBean.TopicType5().apply {
                                    questionText = "$fcDataIndex Which planet is closest to the Sun?"
                                    answerNumber = 4
                                    answerText1 = "$fcDataIndex Earth"
                                    answerText2 = "$fcDataIndex Venus"
                                    answerText3 = "$fcDataIndex Mars"
                                    answerText4 = "$fcDataIndex Mercury"
                                })
                                add(FlashCardThemeBean.TopicType5().apply {
                                    questionText = "$fcDataIndex What is the capital of India?"
                                    answerNumber = 3
                                    answerText1 = "$fcDataIndex Mumbai"
                                    answerText2 = "$fcDataIndex Chennai"
                                    answerText3 = "$fcDataIndex New Delhi"
                                    answerText4 = "$fcDataIndex Kolkata"
                                })
                                add(FlashCardThemeBean.TopicType5().apply {
                                    questionText = "$fcDataIndex Which animal is known as the \"King of the Jungle\"?"
                                    answerNumber = 3
                                    answerText1 = "$fcDataIndex Elephant"
                                    answerText2 = "$fcDataIndex Tiger"
                                    answerText3 = "$fcDataIndex Lion"
                                    answerText4 = "$fcDataIndex Zebra"
                                })
                                add(FlashCardThemeBean.TopicType5().apply {
                                    questionText = "$fcDataIndex Which country is famous for the Eiffel Tower?"
                                    answerNumber = 2
                                    answerText1 = "$fcDataIndex Italy"
                                    answerText2 = "$fcDataIndex France"
                                    answerText3 = "$fcDataIndex Spain"
                                    answerText4 = "$fcDataIndex Germany"
                                })
                                add(FlashCardThemeBean.TopicType5().apply {
                                    questionText = "$fcDataIndex Which fruit is known for having many seeds and is red or pink in color?"
                                    answerNumber = 2
                                    answerText1 = "$fcDataIndex Apple"
                                    answerText2 = "$fcDataIndex Watermelon"
                                    answerText3 = "$fcDataIndex Mango"
                                    answerText4 = "$fcDataIndex Pear"
                                })
                            }
                        }
                        ToastDialog.showToast(this@ChildrenActivity, gson.toJson(fcTheme5))
                    } else {
                        fcTheme5 = null
                    }
                }
            }
        }

    }

    private fun setFcInfo() {
        FileUtils.deleteAllInDir(getExternalFilesDir("agps"))

        if (deviceInfo == null) {
            if (ControlBleTools.getInstance().isConnect) {
                ControlBleTools.getInstance().getDeviceInfo(null)
            }
            ToastUtils.showShort(R.string.s8)
            return
        }
        if (flashCardInfos == null) {
            ToastUtils.showShort(R.string.s662)
            return
        }

        val ftb = FlashCardThemeBean().apply {
            themes = mutableListOf<FlashCardThemeBean.Theme>().apply {
                if (fcTheme1 != null) {
                    //每种typeId仅支持存在一个
                    if (themes == null || themes.firstOrNull { it.typeId == fcTheme1!!.typeId } == null) add(fcTheme1!!)
                }
                if (fcTheme2 != null) {
                    //每种typeId仅支持存在一个
                    if (themes == null || themes.firstOrNull { it.typeId == fcTheme2!!.typeId } == null) add(fcTheme2!!)
                }
                if (fcTheme3 != null) {
                    //每种typeId仅支持存在一个
                    if (themes == null || themes.firstOrNull { it.typeId == fcTheme3!!.typeId } == null) add(fcTheme3!!)
                }
                if (fcTheme4 != null) {
                    //每种typeId仅支持存在一个
                    if (themes == null || themes.firstOrNull { it.typeId == fcTheme4!!.typeId } == null) add(fcTheme4!!)
                }
                if (fcTheme5 != null) {
                    //每种typeId仅支持存在一个
                    if (themes == null || themes.firstOrNull { it.typeId == fcTheme5!!.typeId } == null) add(fcTheme5!!)
                }
            }
            //最多支持数量
            while (themes.size > flashCardInfos!!.maxCount) {
                themes.removeAt(themes.size - 1)
            }
        }
        if (ftb.themes.isEmpty()) {
            ToastUtils.showShort(R.string.s238)
            return
        }

        //已有的闪卡为空
        if (flashCardInfos!!.list.isNullOrEmpty()) {
            //直接发送
            ControlBleTools.getInstance().setupFlashCardByBerry(ftb, deviceInfo!!.equipmentNumber, MyUploadBigDataListener(2))
        } else {
            //需要删除旧的
            //删除已有数据
            ControlBleTools.getInstance().delFlashCardByBerry(flashCardInfos!!.list, object : ParsingStateManager.SendCmdStateListener() {
                override fun onState(state: SendCmdState?) {
                    if (state == SendCmdState.SUCCEED) {
                        //重新设置
                        ControlBleTools.getInstance().setupFlashCardByBerry(ftb, deviceInfo!!.equipmentNumber, MyUploadBigDataListener(2))
                    } else {
                        ToastDialog.showToast(this@ChildrenActivity, getString(R.string.s221))
                    }
                }
            })
        }

    }
    //endregion

    //region 儿童手表相关获取回调
    private fun initCallBack() {
        if (ControlBleTools.getInstance().isConnect) {
            ControlBleTools.getInstance().getDeviceInfo(baseSendCmdStateListener)
        }
        initDeviceInfoCallBack()

        CallBackUtils.childrenCallBack = object : ChildrenCallBack {
            override fun onChildrenInfo(bean: ChildrenInfoBean?) {
                ToastDialog.showToast(this@ChildrenActivity, "${getString(R.string.s656)}：${gson.toJson(bean)}")
            }

            override fun onParentInfos(info: ParentInfoBean?) {
                parentInfos = info
                ToastDialog.showToast(this@ChildrenActivity, "${getString(R.string.s657)}：${gson.toJson(parentInfos)}")
            }

            override fun onFlashCardInfos(infos: FlashCardIdsBean?) {
                flashCardInfos = infos
                ToastDialog.showToast(this@ChildrenActivity, "${getString(R.string.s660)}：${gson.toJson(flashCardInfos)}")
            }

            override fun onFlashCardProgress(bean: FlashCardProgressBean?) {
                ToastDialog.showToast(this@ChildrenActivity, "${getString(R.string.s693)}：${gson.toJson(bean)}")
            }

            override fun onChallengeInfos(bean: ChallengeInfoBean?) {
                ToastDialog.showToast(this@ChildrenActivity, "${getString(R.string.s664)}：${gson.toJson(bean)}")
                challengeInfoBean = bean
            }

            override fun onChallengeResult(bean: ChallengeResultBean?) {
                ToastDialog.showToast(this@ChildrenActivity, "${getString(R.string.s692)}：${gson.toJson(bean)}")
            }

            override fun onEarningsInfo(bean: EarningsInfoBean?) {
                ToastDialog.showToast(this@ChildrenActivity, "${getString(R.string.s665)}：${gson.toJson(bean)}")
                earningsPriceBean = bean
            }

            override fun onChangePocketMoney(bean: EarningsExchangeBean?) {
                ToastDialog.showToast(this@ChildrenActivity, "${getString(R.string.s667)}：${gson.toJson(bean)}")
            }

            override fun onSchedulerResult(list: MutableList<SchedulerResultBean>?) {
                ToastDialog.showToast(this@ChildrenActivity, "${getString(R.string.s694)}：${gson.toJson(list)}")
            }

            override fun onMedalInfo(bean: MutableList<MedalInfoBean>?) {
                ToastDialog.showToast(this@ChildrenActivity, "${getString(R.string.s701)}：${gson.toJson(bean)}")
            }

        }

        //获取调度器回调
        DeviceSettingLiveData.getInstance().getmScheduler().observe(this, object : Observer<SchedulerBean?> {
            override fun onChanged(bean: SchedulerBean?) {
                if (bean == null) return
                ToastDialog.showToast(this@ChildrenActivity, "${getString(R.string.s323)}：$${gson.toJson(bean)}")
            }
        })

        //获取学校模式
        DeviceSettingLiveData.getInstance().getmSchoolBean().observe(this, object : Observer<SchoolBean?> {
            override fun onChanged(bean: SchoolBean?) {
                if (bean == null) return
                ToastDialog.showToast(this@ChildrenActivity, "${getString(R.string.s320)}：${gson.toJson(bean)}")
            }
        })

        //获取考试模式
        CallBackUtils.examModeDetailCallback = object : ExamModeDetailCallback {
            override fun onExamDetail(bean: MutableList<ExamSettingsBean>?, supportCount: Int) {
                this@ChildrenActivity.supportCount = supportCount
                ToastDialog.showToast(this@ChildrenActivity, "${getString(R.string.s686)}：${gson.toJson(bean)}")
            }
        }

        // 家长信息或 闪存卡设备接收结果
        CallBackUtils.berryBigFileResultCallBack = object : BerryBigFileResultCallBack {
            override fun onBigFileResult(bean: BerryBigFileResultBean?) {
                if (bean != null) {
                    if (bean.type == BerryBigFileResultCallBack.BigFileType.PARENTS_INFO.type) {
                        if (bean.statue == 1) { //成功
                            ToastDialog.showToast(this@ChildrenActivity, "${getString(R.string.s684)}：success")
                        } else {
                            ToastDialog.showToast(this@ChildrenActivity, "${getString(R.string.s684)}：Failed：${bean.errorType}")
                            //@see BerryBigFileResultCallBack.ErrorType
                        }
                    } else if (bean.type == BerryBigFileResultCallBack.BigFileType.FLASH_CARD.type) {
                        if (bean.statue == 1) { //成功
                            ToastDialog.showToast(this@ChildrenActivity, "${getString(R.string.s683)}：success")
                        } else {
                            ToastDialog.showToast(this@ChildrenActivity, "${getString(R.string.s683)}：Failed：${bean.errorType}")
                            //@see BerryBigFileResultCallBack.ErrorType
                        }
                    }
                }
            }

        }
    }

    private var deviceInfo: DeviceInfoBean? = null
    private fun initDeviceInfoCallBack() {
        CallBackUtils.deviceInfoCallBack = object : DeviceInfoCallBack {
            override fun onDeviceInfo(deviceInfoBean: DeviceInfoBean?) {
                //ToastDialog.showToast(this@BerryDeviceActivity, gson.toJson(deviceInfoBean))
                deviceInfo = deviceInfoBean
            }

            override fun onBatteryInfo(capacity: Int, chargeStatus: Int) {
                var state = getString(R.string.s208)
                if (chargeStatus == DeviceInfoCallBack.ChargeStatus.UNKNOWN.state) {
                    state = getString(R.string.s208)
                } else if (chargeStatus == DeviceInfoCallBack.ChargeStatus.CHARGING.state) {
                    state = getString(R.string.s209)
                } else if (chargeStatus == DeviceInfoCallBack.ChargeStatus.NOT_CHARGING.state) {
                    state = getString(R.string.s210)
                } else if (chargeStatus == DeviceInfoCallBack.ChargeStatus.FULL.state) {
                    state = getString(R.string.s211)
                }
                val tmp = """${getString(R.string.s212)}$capacity ${getString(R.string.s213)}$chargeStatus $state"""
                //ToastDialog.showToast(this@BerryDeviceActivity, tmp)
            }

        }
    }
    //endregion

    //region 选择家长头像
    private var headDialog: MyDialog? = null
    private fun showListDialog(files: List<File>) {
        headDialog = CustomDialog.builder(this)
            .setContentView(R.layout.dialog_debug_bin_list)
            .setWidth(ScreenUtils.getScreenWidth())
            .setHeight((ScreenUtils.getScreenHeight() * 0.8f).toInt())
            .build()
        for (file in files) {
            if (file.name.endsWith(".jpg") || file.name.endsWith(".jpeg") || file.name.endsWith(".png")) {
                val view = Button(this)
                view.isAllCaps = false
                view.text = file.name
                view.setOnClickListener {
                    this.parentHeadfile = file
                    binding.tvHeadName.text = "${getString(R.string.s513)} ${file.name}"
                    headDialog?.dismiss()
                }
                headDialog?.findViewById<LinearLayout>(R.id.listLayout)?.addView(view)
            }
        }
        headDialog?.show()
    }
    //endregion

    //region 大文件传输回调
    private var dialog: Dialog? = null

    inner class MyUploadBigDataListener(var type: Int) : UploadBigDataListener {
        override fun onSuccess() {
            if (dialog?.isShowing == true) dialog?.dismiss()
            ToastUtils.showShort(getString(R.string.s220))
            if (type == 1) {
                binding.btnGetParentInfo.callOnClick()
            } else {
                binding.btnGetFcInfo.callOnClick()
            }
        }

        override fun onProgress(curPiece: Int, dataPackTotalPieceLength: Int) {
            if (curPiece == 0) {
                if (dialog?.isShowing == true) dialog?.dismiss()
                dialog = ToastDialog.showToast(this@ChildrenActivity, getString(R.string.s648))
            }
        }

        override fun onTimeout(msg: String?) {
            if (dialog?.isShowing == true) dialog?.dismiss()
            ToastUtils.showShort(getString(R.string.s221) + " : " + msg)
        }

    }
    //endregion

    //region 设置收益
    //test Data
    private var testEarning = 20
    private fun setEarningsInfo() {
        ControlBleTools.getInstance().setEarningsInfoByBerry(EarningsInfoBean().apply {
            steps = testEarning - 2
            sleep = testEarning - 3
            calories = testEarning - 4
            distance = testEarning - 5
            flashcard = testEarning - 6
            task = testEarning - 7
            challenge = testEarning - 8
            medals = testEarning - 9
            activity = steps + sleep + calories + distance
            today = steps + sleep + calories + distance + flashcard + task + challenge + medals
            total = steps + sleep + calories + distance + flashcard + task + challenge + medals + 100
        }, baseSendCmdStateListener)

        testEarning += 10
    }
    //endregion

    //region 设置勋章
    //test data
    private fun setMedalInfo() {
        var modal1 = 0
        var modal2 = 0
        var modal3 = 0
        var modal4 = 0
        try {
            modal1 = binding.etMedal1.text.toString().toInt()
            modal2 = binding.etMedal2.text.toString().toInt()
            modal3 = binding.etMedal3.text.toString().toInt()
            modal4 = binding.etMedal4.text.toString().toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            ToastDialog.showToast(this@ChildrenActivity, getString(R.string.s238))
        }
        val medalInfoBeans = arrayListOf<MedalInfoBean>()
        medalInfoBeans.apply {
            add(MedalInfoBean().apply {
                medalId = ChildrenCallBack.MedalId.STEPS_ID.id
                completedProgress = modal1
            })
            add(MedalInfoBean().apply {
                medalId = ChildrenCallBack.MedalId.CHALLENGE_ID.id
                completedProgress = modal2
            })
            add(MedalInfoBean().apply {
                medalId = ChildrenCallBack.MedalId.FLASH_CARD_ID.id
                completedProgress = modal3
            })
            add(MedalInfoBean().apply {
                medalId = ChildrenCallBack.MedalId.SCHEDULER_ID.id
                completedProgress = modal4
            })
        }

        ControlBleTools.getInstance().setMedalInfoByBerry(medalInfoBeans, baseSendCmdStateListener)
    }
    //endregion

    //region 调度器
    private fun sendScheduler() {
        if (DeviceSettingLiveData.getInstance().getmScheduler().value == null) {
            Toast.makeText(this@ChildrenActivity, getString(R.string.s219), Toast.LENGTH_LONG).show()
            return
        }
        val schedulerBean = DeviceSettingLiveData.getInstance().getmScheduler().value!!

        try {
            schedulerBean.alertList = arrayListOf<AlertBean>().apply {
                add(AlertBean().apply {
                    alertBerryId = binding.etSchedulerAlertBerryId.text.toString().trim().toInt()
                    alertName = binding.etSchedulerAlertName.text.toString()
                    notifyText = binding.etSchedulerNotifyText.text.toString()
                    alertTime = SettingTimeBean(binding.etAlertSH.text.toString().trim().toInt(), binding.etAlertSM.text.toString().trim().toInt())
                    isMonday = binding.cbAlertC1.isChecked
                    isTuesday = binding.cbAlertC2.isChecked
                    isWednesday = binding.cbAlertC3.isChecked
                    isThursday = binding.cbAlertC4.isChecked
                    isFriday = binding.cbAlertC5.isChecked
                    isSaturday = binding.cbAlertC6.isChecked
                    isSunday = binding.cbAlertC7.isChecked
                })
            }
            schedulerBean.habitBeanList = arrayListOf<HabitBean>().apply {
                add(HabitBean().apply {
                    habitBerryId = binding.etSchedulerHId.text.toString().trim().toInt()
                    habitName = binding.etSchedulerHName.text.toString()
                    habitType = binding.etSchedulerHType.text.toString().trim().toInt()
                    habitTimeList = arrayListOf<SettingTimeBean>().apply {
                        add(SettingTimeBean(binding.etHabitSH.text.toString().trim().toInt(), binding.etHabitSM.text.toString().trim().toInt()))
                    }
                    isMonday = binding.cbHabitC1.isChecked
                    isTuesday = binding.cbHabitC2.isChecked
                    isWednesday = binding.cbHabitC3.isChecked
                    isThursday = binding.cbHabitC4.isChecked
                    isFriday = binding.cbHabitC5.isChecked
                    isSaturday = binding.cbHabitC6.isChecked
                    isSunday = binding.cbHabitC7.isChecked
                })
            }
            schedulerBean.reminderBeanList = arrayListOf<ReminderBean>().apply {
                add(ReminderBean().apply {
                    reminderBerryId = binding.etSchedulerRId.text.toString().trim().toInt()
                    reminderName = binding.etSchedulerRName.text.toString()
                    reminderType = binding.etSchedulerRType.text.toString().trim().toInt()
                    startTime = SettingTimeBean(binding.etReminderSH.text.toString().trim().toInt(), binding.etReminderSM.text.toString().trim().toInt())
                    endTime = SettingTimeBean(binding.etReminderEH.text.toString().trim().toInt(), binding.etReminderEM.text.toString().trim().toInt())
                    isMonday = binding.cbReminderC1.isChecked
                    isTuesday = binding.cbReminderC2.isChecked
                    isWednesday = binding.cbReminderC3.isChecked
                    isThursday = binding.cbReminderC4.isChecked
                    isFriday = binding.cbReminderC5.isChecked
                    isSaturday = binding.cbReminderC6.isChecked
                    isSunday = binding.cbReminderC7.isChecked
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ToastDialog.showToast(this@ChildrenActivity, getString(R.string.s238))
            return
        }
        ToastDialog.showToast(this, gson.toJson(schedulerBean))
        ControlBleTools.getInstance().setScheduleReminder(schedulerBean, baseSendCmdStateListener)
    }
    //endregion

    //region 学校模式
    private var testSchoolModeTimeValue = 0
    private fun sendSchoolMode() {
        testSchoolModeTimeValue++
        if (testSchoolModeTimeValue + 10 > 24) {
            testSchoolModeTimeValue = 0
        }
        val schoolBean = SchoolBean(
            true,
            SettingTimeBean(testSchoolModeTimeValue, 0),
            SettingTimeBean(testSchoolModeTimeValue + 10, 30),
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            testSchoolModeTimeValue,
            true,
            true
        )
        ToastDialog.showToast(this, gson.toJson(schoolBean))
        ControlBleTools.getInstance().setSchoolMode(schoolBean, baseSendCmdStateListener)
    }
    //endregion

    //region 设置考试模式
    private var examValue = 0
    private var supportCount = 0
    private fun sendExamReminder() {
        examValue++
        if (examValue + 10 > 24) {
            examValue = 0
        }
        if (supportCount == 0) return
        val list: MutableList<ExamSettingsBean> = mutableListOf()
        for (i in 0..<supportCount) {
            list.add(ExamSettingsBean().apply {
                name = "name $examValue"
                duration = 120 + examValue
                nudges = 60 + examValue
                time = TimeBean(2025, 4, 1, examValue, examValue + 30, examValue + 40)
                status = (examValue + 1) % 2 == 0
            })
        }
        ToastDialog.showToast(this, gson.toJson(list))
        ControlBleTools.getInstance().setExamReminderSettings(list, baseSendCmdStateListener)
    }
    //endregion

}