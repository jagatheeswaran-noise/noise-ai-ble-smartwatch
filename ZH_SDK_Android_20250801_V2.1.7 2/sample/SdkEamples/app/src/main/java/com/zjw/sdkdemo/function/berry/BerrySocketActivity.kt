package com.zjw.sdkdemo.function.berry

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.NetworkUtils
import com.blankj.utilcode.util.ToastUtils
import com.zhapp.ble.ControlBleTools
import com.zhapp.ble.bean.berry.WearSocketConfig
import com.zhapp.ble.bean.berry.WearSocketMessageData
import com.zhapp.ble.bean.berry.WearSocketResp
import com.zhapp.ble.callback.CallBackUtils
import com.zhapp.ble.callback.WearSocketCallBack
import com.zjw.sdkdemo.R
import com.zjw.sdkdemo.databinding.LayoutSocketBinding
import com.zjw.sdkdemo.function.language.BaseActivity
import java.io.ByteArrayOutputStream

/**
 * Created by Android on 2025/6/12.
 */
class BerrySocketActivity : BaseActivity() {
    private val TAG = "BerrySocketActivity"
    private val binding: LayoutSocketBinding by lazy { LayoutSocketBinding.inflate(layoutInflater) }

    // 数据缓冲区
    private val dataBuffer = ByteArrayOutputStream()
    private var lastDataReceivedTime: Long = 0
    private val DATA_TIMEOUT: Long = 200 // 超时

    // Handler用于处理超时
    private val alipayDataHandler = Handler(Looper.getMainLooper())
    private val processDataRunnable = Runnable {
        processTimedOutData()
    }


    private var serverData: ByteArray? = null
    private var sentDataIndex: Int = 0 // 已发送数据的索引位置

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.s708)
        setContentView(binding.root)
        initEvent()
        initCallBack()
    }

    private fun initEvent() {

        click(binding.btnNet, {
            ToastUtils.showShort(getString(R.string.s709) + " true")
            ControlBleTools.getInstance().sendSocketNetworkEnable(true, baseSendCmdStateListener)
        })

        click(binding.btnNet2, {
            ToastUtils.showShort(getString(R.string.s709) + " false")
            ControlBleTools.getInstance().sendSocketNetworkEnable(false, baseSendCmdStateListener)
        })

    }

    private fun initCallBack() {
        CallBackUtils.wearSocketCallBack = object : WearSocketCallBack {
            override fun onSocketConfig(config: WearSocketConfig?) {
                LogUtils.e(TAG, "onSocketConfig: " + config?.toString())
                NetworkUtils.isAvailableAsync {
                    if(it){
                        // 使用配置建立Socket连接
                        config?.let {
                            BerrySocketManager.connect(it)
                        }
                    }else{
                        ControlBleTools.getInstance().sendSocketNetworkEnable(false, baseSendCmdStateListener)
                    }
                }
            }

            override fun onWearSendSocketMessage(data: WearSocketMessageData?) {
                LogUtils.e(TAG, "onWearSendSocketMessage: " + data?.toString())
                data?.let {
                    ControlBleTools.getInstance().replyWearSocketMessage(WearSocketMessageData().apply {
                        this.uuid = data.uuid
                        this.messageData = data.messageData
                    }, baseSendCmdStateListener)
                    if (data.messageData != null) {
                        // 发送数据
                        BerrySocketManager.sendData(data.messageData, { isSuc, msg ->
                            LogUtils.e(TAG, "sendData: $isSuc $msg")
                        })
                    }
                }
            }

            override fun onWearRequestSocketMessage(data: WearSocketMessageData?) {
                LogUtils.e(TAG, "onWearRequestSocketMessage: " + data?.toString() + "serverData: " + serverData?.size)

                if (data != null && serverData != null) {
                    // 分段发送数据，每次最多发送1024字节
                    val dataSize = serverData!!.size
                    val startIndex = sentDataIndex
                    val endIndex = Math.min(startIndex + 1024, dataSize)
                    // 提取要发送的数据段
                    val chunk = serverData!!.sliceArray(startIndex until endIndex)
                    // 发送数据段
                    ControlBleTools.getInstance().sendServerSocketMessage(
                        WearSocketMessageData().apply {
                            this.uuid = data.uuid
                            this.messageData = chunk
                        }, baseSendCmdStateListener
                    )
                    // 更新已发送的索引
                    sentDataIndex = endIndex
                    // 如果还有剩余数据，等待下一次请求再发送
                    // 如果已经发送完所有数据，重置索引
                    if (sentDataIndex >= dataSize) {
                        sentDataIndex = 0
                    }
                }
            }

            override fun onSocketDisconnect(resp: WearSocketResp?) {
                LogUtils.e(TAG, "onSocketDisconnect: " + resp?.toString())
                BerrySocketManager.destroy()
                // 清理超时任务
                alipayDataHandler.removeCallbacks(processDataRunnable)
                // 清空数据缓冲区
                clearDataBuffer()
                ControlBleTools.getInstance().replyWearSocketDisconnect(WearSocketResp().apply {
                    this.uuid = uuid
                    this.respType = 1
                }, baseSendCmdStateListener)
                // 重置发送索引
                sentDataIndex = 0
            }

            override fun onRequestNetworkStatus() {
                LogUtils.e(TAG, "onRequestNetworkStatus")
                NetworkUtils.isAvailableAsync {
                    ControlBleTools.getInstance().replyWearSocketNetworkEnable(it, baseSendCmdStateListener)
                }
            }

        }

        BerrySocketManager.setSocketListener(object : BerrySocketManager.BerrySocketListener {
            override fun onConnected(sessionId: Int) {
                LogUtils.e(TAG, "onConnected: " + sessionId)
                ControlBleTools.getInstance().sendSocketNetworkEnable(true, baseSendCmdStateListener)
                // 连接成功时清空缓冲区
                clearDataBuffer()
                ControlBleTools.getInstance().replyWearSocketResp(WearSocketResp().apply {
                    this.uuid = sessionId
                    this.respType = 1
                }, baseSendCmdStateListener)
            }

            override fun onConnectTimeout() {
                // 连接超时时清空缓冲区
                clearDataBuffer()
                ControlBleTools.getInstance().replyWearSocketResp(WearSocketResp().apply {
                    this.uuid = 0
                    this.respType = 3
                }, baseSendCmdStateListener)
            }

            override fun onDisconnected(sessionId: Int) {
                // 连接断开时清空缓冲区
                clearDataBuffer()
                ControlBleTools.getInstance().replyWearSocketResp(WearSocketResp().apply {
                    this.uuid = sessionId
                    this.respType = 2
                }, baseSendCmdStateListener)
                // 重置发送索引
                sentDataIndex = 0
            }

            override fun onDataReceived(data: ByteArray) {
                // 将接收到的数据添加到缓冲区
                dataBuffer.write(data)
                lastDataReceivedTime = System.currentTimeMillis()
                // 移除之前的超时任务
                alipayDataHandler.removeCallbacks(processDataRunnable)
                // 设置新的超时任务
                alipayDataHandler.postDelayed(processDataRunnable, DATA_TIMEOUT)
                LogUtils.d("BerrySocketActivity", "接收到数据片段，长度: ${data.size}，缓冲区总长度: ${dataBuffer.size()}")
            }

        })
    }

    /**
     * 处理超时数据
     */
    private fun processTimedOutData() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDataReceivedTime >= DATA_TIMEOUT) {
            // 超时，认为数据接收完成
            val completeData = dataBuffer.toByteArray()
            if (completeData.isNotEmpty()) {
                handleCompletePacket(completeData)
                dataBuffer.reset()
            }
        }
    }

    /**
     * 处理完整数据包
     */
    private fun handleCompletePacket(data: ByteArray) {
        LogUtils.d("BerrySocketActivity", "接收到完整数据包，总长度: ${data.size}")
        serverData = data
        // 在这里处理你的完整数据
    }

    /**
     * 清空数据缓冲区
     */
    private fun clearDataBuffer() {
        dataBuffer.reset()
        alipayDataHandler.removeCallbacks(processDataRunnable)
        lastDataReceivedTime = 0
    }

    // 记住在适当的时候清理资源
    override fun onDestroy() {
        super.onDestroy()
        alipayDataHandler.removeCallbacks(processDataRunnable)
        clearDataBuffer()
    }

}
