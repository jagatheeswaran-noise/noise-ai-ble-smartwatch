package com.zjw.sdkdemo.function.berry

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import com.zh.ble.wear.protobuf.MusicProtos
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.MusicInfoBean
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.MusicCallBack
import com.zhapp.ble.parsing.ParsingStateManager.SendCmdStateListener
import com.zhapp.ble.parsing.SendCmdState
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.app.MyApplication
import com.zjw.sdkdemo.databinding.ActivityBerryMusicBinding
import com.zjw.sdkdemo.function.RemindActivity
import com.zjw.sdkdemo.function.language.BaseActivity

/**
 * Created by Android on 2024/10/25.
 */
class BerryMusicActivity : BaseActivity() {
    private val binding: ActivityBerryMusicBinding by lazy { ActivityBerryMusicBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s416)
        setContentView(binding.root)
        initListener()
        initMusicCallBack()
    }


    private fun initListener() {
        click(binding.btnSyncMusic) {
            syncMusic()
        }
    }

    //region 音乐
    private var audioManager: AudioManager? = null

    /**
     * 同步音乐信息
     */
    private fun syncMusic() {
        if (audioManager == null) {
            audioManager = this.getSystemService(AUDIO_SERVICE) as AudioManager
        }
        val mMusicTitle: String = binding.etMusicTitle.getText().toString().trim { it <= ' ' }
        val strState: String = binding.etMusicState.getText().toString().trim { it <= ' ' }
        val sMusicNew: String = binding.etMusicNew.getText().toString().trim { it <= ' ' }

        var mMusicState = 0
        try {
            mMusicState = strState.toInt()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var mCurrent = 0
        if (audioManager != null) {
            mCurrent = audioManager!!.getStreamVolume(AudioManager.STREAM_MUSIC)
        }

        var mMaxVolume = 0
        if (audioManager != null) {
            mMaxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        }

        val isNewPermissionType = sMusicNew.contains("1")

        //当应用通知访问权限未开启时，收到设备获取音乐信息请求MusicCallBack.onRequestMusic()后APP应下发音乐无权限指令通知设备，以便设备显示无权限页面。ControlBleTools.getInstance().syncMusicInfo(new MusicInfoBean(1, "", 0, 0), null)
        val musiceInfoBean = MusicInfoBean(mMusicState, mMusicTitle, mCurrent, mMaxVolume, isNewPermissionType)
        ControlBleTools.getInstance().syncMusicInfo(musiceInfoBean, object : SendCmdStateListener() {
            override fun onState(state: SendCmdState) {
                when (state) {
                    SendCmdState.SUCCEED -> MyApplication.showToast("同步音乐成功")
                    else -> MyApplication.showToast("同步音乐超时")
                }
            }
        })
    }

    /**
     * 音乐控制指令
     *
     * @param context
     * @param keyCode
     */
    fun controlMusic(context: Context, keyCode: Int) {
        val eventTime = SystemClock.uptimeMillis()
        val key = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
        dispatchMediaKeyToAudioService(context, key)
        dispatchMediaKeyToAudioService(context, KeyEvent.changeAction(key, KeyEvent.ACTION_UP))
    }

    private fun dispatchMediaKeyToAudioService(context: Context, event: KeyEvent) {
        val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        if (audioManager != null) {
            try {
                audioManager.dispatchMediaKeyEvent(event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun initMusicCallBack() {
        /**
         * 处理音乐相关回调
         */
        CallBackUtils.musicCallBack = object : MusicCallBack {
            override fun onRequestMusic() { //设备进入音乐界面，请求音乐信息，此时发送音乐信息设备才有反应
                Log.d("MUSIC", "RequestMusic")
                syncMusic()
            }

            override fun onSyncMusic(errorCode: Int) { //syncMusic结果
                Log.d("MUSIC", "onSyncMusic$errorCode")
            }

            override fun onQuitMusic() {
                Log.d("MUSIC", getString(R.string.s240))
            }

            override fun onSendMusicCmd(command: Int) { //设备控制指令
                when (command) {
                    MusicProtos.SEPlayerControlCommand.PLAYING_VALUE, MusicProtos.SEPlayerControlCommand.PAUSE_VALUE -> RemindActivity.controlMusic(
                        this@BerryMusicActivity, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    )

                    MusicProtos.SEPlayerControlCommand.PREV_VALUE -> RemindActivity.controlMusic(this@BerryMusicActivity, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                    MusicProtos.SEPlayerControlCommand.NEXT_VALUE -> RemindActivity.controlMusic(this@BerryMusicActivity, KeyEvent.KEYCODE_MEDIA_NEXT)
                    MusicProtos.SEPlayerControlCommand.ADJUST_VOLUME_UP_VALUE -> {
                        //调用系统提高音量
                        if (audioManager != null) {
                            audioManager!!.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
                        }
                        //发送音乐信息
                        syncMusic()
                    }

                    MusicProtos.SEPlayerControlCommand.ADJUST_VOLUME_DOWN_VALUE -> {
                        if (audioManager != null) {
                            audioManager!!.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND)
                        }
                        //发送音乐信息
                        syncMusic()
                    }
                }
            }
        }
    }


}