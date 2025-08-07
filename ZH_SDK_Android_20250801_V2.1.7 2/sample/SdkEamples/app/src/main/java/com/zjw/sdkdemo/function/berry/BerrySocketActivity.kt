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

    // ���ݻ�����
    private val dataBuffer = ByteArrayOutputStream()
    private var lastDataReceivedTime: Long = 0
    private val DATA_TIMEOUT: Long = 200 // ��ʱ

    // Handler���ڴ���ʱ
    private val alipayDataHandler = Handler(Looper.getMainLooper())
    private val processDataRunnable = Runnable {
        processTimedOutData()
    }


    private var serverData: ByteArray? = null
    private var sentDataIndex: Int = 0 // �ѷ������ݵ�����λ��

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
                        // ʹ�����ý���Socket����
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
                        // ��������
                        BerrySocketManager.sendData(data.messageData, { isSuc, msg ->
                            LogUtils.e(TAG, "sendData: $isSuc $msg")
                        })
                    }
                }
            }

            override fun onWearRequestSocketMessage(data: WearSocketMessageData?) {
                LogUtils.e(TAG, "onWearRequestSocketMessage: " + data?.toString() + "serverData: " + serverData?.size)

                if (data != null && serverData != null) {
                    // �ֶη������ݣ�ÿ����෢��1024�ֽ�
                    val dataSize = serverData!!.size
                    val startIndex = sentDataIndex
                    val endIndex = Math.min(startIndex + 1024, dataSize)
                    // ��ȡҪ���͵����ݶ�
                    val chunk = serverData!!.sliceArray(startIndex until endIndex)
                    // �������ݶ�
                    ControlBleTools.getInstance().sendServerSocketMessage(
                        WearSocketMessageData().apply {
                            this.uuid = data.uuid
                            this.messageData = chunk
                        }, baseSendCmdStateListener
                    )
                    // �����ѷ��͵�����
                    sentDataIndex = endIndex
                    // �������ʣ�����ݣ��ȴ���һ�������ٷ���
                    // ����Ѿ��������������ݣ���������
                    if (sentDataIndex >= dataSize) {
                        sentDataIndex = 0
                    }
                }
            }

            override fun onSocketDisconnect(resp: WearSocketResp?) {
                LogUtils.e(TAG, "onSocketDisconnect: " + resp?.toString())
                BerrySocketManager.destroy()
                // ����ʱ����
                alipayDataHandler.removeCallbacks(processDataRunnable)
                // ������ݻ�����
                clearDataBuffer()
                ControlBleTools.getInstance().replyWearSocketDisconnect(WearSocketResp().apply {
                    this.uuid = uuid
                    this.respType = 1
                }, baseSendCmdStateListener)
                // ���÷�������
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
                // ���ӳɹ�ʱ��ջ�����
                clearDataBuffer()
                ControlBleTools.getInstance().replyWearSocketResp(WearSocketResp().apply {
                    this.uuid = sessionId
                    this.respType = 1
                }, baseSendCmdStateListener)
            }

            override fun onConnectTimeout() {
                // ���ӳ�ʱʱ��ջ�����
                clearDataBuffer()
                ControlBleTools.getInstance().replyWearSocketResp(WearSocketResp().apply {
                    this.uuid = 0
                    this.respType = 3
                }, baseSendCmdStateListener)
            }

            override fun onDisconnected(sessionId: Int) {
                // ���ӶϿ�ʱ��ջ�����
                clearDataBuffer()
                ControlBleTools.getInstance().replyWearSocketResp(WearSocketResp().apply {
                    this.uuid = sessionId
                    this.respType = 2
                }, baseSendCmdStateListener)
                // ���÷�������
                sentDataIndex = 0
            }

            override fun onDataReceived(data: ByteArray) {
                // �����յ���������ӵ�������
                dataBuffer.write(data)
                lastDataReceivedTime = System.currentTimeMillis()
                // �Ƴ�֮ǰ�ĳ�ʱ����
                alipayDataHandler.removeCallbacks(processDataRunnable)
                // �����µĳ�ʱ����
                alipayDataHandler.postDelayed(processDataRunnable, DATA_TIMEOUT)
                LogUtils.d("BerrySocketActivity", "���յ�����Ƭ�Σ�����: ${data.size}���������ܳ���: ${dataBuffer.size()}")
            }

        })
    }

    /**
     * ����ʱ����
     */
    private fun processTimedOutData() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDataReceivedTime >= DATA_TIMEOUT) {
            // ��ʱ����Ϊ���ݽ������
            val completeData = dataBuffer.toByteArray()
            if (completeData.isNotEmpty()) {
                handleCompletePacket(completeData)
                dataBuffer.reset()
            }
        }
    }

    /**
     * �����������ݰ�
     */
    private fun handleCompletePacket(data: ByteArray) {
        LogUtils.d("BerrySocketActivity", "���յ��������ݰ����ܳ���: ${data.size}")
        serverData = data
        // �����ﴦ�������������
    }

    /**
     * ������ݻ�����
     */
    private fun clearDataBuffer() {
        dataBuffer.reset()
        alipayDataHandler.removeCallbacks(processDataRunnable)
        lastDataReceivedTime = 0
    }

    // ��ס���ʵ���ʱ��������Դ
    override fun onDestroy() {
        super.onDestroy()
        alipayDataHandler.removeCallbacks(processDataRunnable)
        clearDataBuffer()
    }

}
