package com.zjw.sdkdemo.function.berry

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.zhapp.ble.bean.berry.WearSocketConfig
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object BerrySocketManager {
    private const val TAG = "BerrySocketManager"
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var executorService: ExecutorService = Executors.newCachedThreadPool()
    private val handler = Handler(Looper.getMainLooper())
    private val isRunning = AtomicBoolean(false)
    private var reconnectRunnable: Runnable? = null
    private var reconnectDelay = 5000L // 5秒重连间隔

    // 添加重连次数相关变量
    private var maxReconnectAttempts = 3 // 最大重连次数
    private var currentReconnectAttempts = 0 // 当前重连次数
    private var wearSocketConfig: WearSocketConfig? = null // 保存配置用于重连

    // Socket配置信息
    private var host: String = ""
    private var port: Int = 0
    private var connectTimeout: Int = 10000
    private var sendTimeout: Int = 10000
    private var recvTimeout: Int = 10000

    // 连接会话ID (使用AtomicInteger保证线程安全)
    private var sessionId: Int = 0
    private val sessionIdGenerator = AtomicInteger(10000) //生成会话ID

    private var socketListener: BerrySocketListener? = null

    fun setSocketListener(listener: BerrySocketListener) {
        this.socketListener = listener
    }

    /**
     * Socket回调监听器
     */
    interface BerrySocketListener {
        fun onConnected(sessionId: Int)
        fun onConnectTimeout()
        fun onDisconnected(sessionId: Int)
        fun onDataReceived(data: ByteArray)
    }

    /**
     * 初始化线程池，确保线程池可用
     */
    private fun initExecutorService() {
        if (executorService.isShutdown || executorService.isTerminated) {
            Log.d(TAG, "线程池已关闭，重新初始化")
            executorService = Executors.newCachedThreadPool()
        }
    }

    /**
     * 根据设备返回的配置建立Socket连接
     */
    fun connect(config: WearSocketConfig) {
        // 确保线程池可用
        initExecutorService()
        // 保存配置用于重连
        this.wearSocketConfig = config
        this.host = config.host
        this.port = config.port
        this.connectTimeout = config.connectTimeout
        this.sendTimeout = config.sendTimeout
        this.recvTimeout = config.recvTimeout

        // 重置重连次数
        currentReconnectAttempts = 0

        executorService.execute {
            try {
                if (isConnected()) {
                    Log.d(TAG, "检测到已有连接，先断开现有连接")
                    disconnect()
                }
                socket = Socket()
                socket?.soTimeout = recvTimeout
                socket?.connect(InetSocketAddress(host, port), connectTimeout)
                if (socket?.isConnected == true) {
                    inputStream = socket?.getInputStream()
                    outputStream = socket?.getOutputStream()
                    isRunning.set(true)

                    Log.d(TAG, "Socket连接成功: $host:$port")
                    onConnected()

                    // 开始接收数据
                    startReceiveData()
                } else {
                    Log.e(TAG, "Socket连接失败")
                    onConnectFailed("连接失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Socket连接异常: ${e.message}")
                onConnectFailed(e.message ?: "连接异常")
            }
        }
    }

    /**
     * 接收数据
     */
    private fun startReceiveData() {
        executorService.execute {
            val buffer = ByteArray(1024)
            while (isRunning.get()) {
                try {
                    val count = inputStream?.read(buffer)
                    if (count != null && count > 0) {
                        val data = ByteArray(count)
                        System.arraycopy(buffer, 0, data, 0, count)
                        onDataReceived(data)
                    } else if (count == -1) {
                        // 连接已断开
                        Log.d(TAG, "Socket连接已断开")
                        onDisconnected()
                        break
                    }
                } catch (e: IOException) {
                    if (isRunning.get()) {
                        Log.e(TAG, "接收数据异常: ${e.message}")
                        onDisconnected()
                    }
                    break
                }
            }
        }
    }

    /**
     * 发送数据
     */
    fun sendData(data: ByteArray, callback: ((Boolean, String?) -> Unit)? = null) {
        executorService.execute {
            try {
                if (isConnected()) {
                    outputStream?.write(data)
                    outputStream?.flush()
                    Log.d(TAG, "数据发送成功")
                    handler.post {
                        callback?.invoke(true, null)
                    }
                } else {
                    Log.e(TAG, "Socket未连接，无法发送数据")
                    handler.post {
                        callback?.invoke(false, "Socket未连接")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "发送数据异常: ${e.message}")
                handler.post {
                    callback?.invoke(false, e.message)
                }
            }
        }
    }


    /**
     * 主动断开连接
     */
    fun disconnect() {
        isRunning.set(false)
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "断开连接时发生异常: ${e.message}")
        } finally {
            inputStream = null
            outputStream = null
            socket = null
        }
        Log.d(TAG, "Socket连接已断开")
        // 当主动断开连接时也通知监听器
        onDisconnected()
    }

    /**
     * 连接成功回调
     */
    private fun onConnected() {
        // 连接成功时重置重连次数
        currentReconnectAttempts = 0
        // 生成新的会话ID
        sessionId = sessionIdGenerator.getAndIncrement()
        val id = sessionId
        Log.d(TAG, "连接会话ID: $id")
        handler.post {
            socketListener?.onConnected(id)
        }
    }

    /**
     * 连接失败回调
     */
    private fun onConnectFailed(error: String) {
        // 自动重连
        scheduleReconnect()
    }

    /**
     * 断开连接回调
     */
    private fun onDisconnected() {
        isRunning.set(false)
        val id = sessionId
        handler.post {
            socketListener?.onDisconnected(id)
        }
        // 在断开连接时尝试重连
        scheduleReconnect()
    }

    /**
     * 接收到数据回调
     */
    private fun onDataReceived(data: ByteArray) {
        handler.post {
            Log.d(TAG, "接收到数据: ${String(data)} ${data.size}")
            socketListener?.onDataReceived(data)
        }
    }

    /**
     * 定时重连
     */
    private fun scheduleReconnect() {
        // 检查是否达到最大重连次数
        if (currentReconnectAttempts >= maxReconnectAttempts) {
            Log.d(TAG, "已达到最大重连次数，连接失败")
            val id = sessionId
            handler.post {
                // 通知连接最终失败
                socketListener?.onConnectTimeout()
            }
            destroy()
            return
        }

        reconnectRunnable?.let { handler.removeCallbacks(it) }
        reconnectRunnable = Runnable {
            if (!isConnected()) {
                // 增加重连次数
                currentReconnectAttempts++
                Log.d(TAG, "尝试重新连接... (第${currentReconnectAttempts}次)")

                // 使用保存的配置重新连接
                wearSocketConfig?.let { config ->
                    connect(config)
                }
            }
        }
        handler.postDelayed(reconnectRunnable!!, reconnectDelay)
    }

    /**
     * 检查是否已连接
     */
    fun isConnected(): Boolean {
        val s = socket ?: return false
        return s.isConnected && !s.isClosed && !s.isInputShutdown
    }


    /**
     * 清理资源
     */
    fun destroy() {
        disconnect()
        try {
            executorService.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "关闭线程池时发生异常: ${e.message}")
        }
        reconnectRunnable?.let { handler.removeCallbacks(it) }
        // 重置重连相关变量
        currentReconnectAttempts = 0
        wearSocketConfig = null
        sessionId = 0
    }

    /**
     * 获取当前会话ID
     */
    fun getSessionId(): Int? {
        return sessionId
    }
}
