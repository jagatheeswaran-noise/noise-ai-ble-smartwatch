package com.noise_ai

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class OpusBridgePackage : ReactPackage {
    override fun createNativeModules(reactCtx: ReactApplicationContext): List<NativeModule> {
        android.util.Log.d("OpusBridge", "OpusBridgePackage.createNativeModules called")
        try {
            val module = OpusBridgeModule(reactCtx)
            android.util.Log.d("OpusBridge", "OpusBridgeModule created successfully")
            return listOf(module)
        } catch (e: Exception) {
            android.util.Log.e("OpusBridge", "Failed to create OpusBridgeModule: ${e.message}")
            android.util.Log.e("OpusBridge", "Stack trace: ${e.stackTraceToString()}")
            throw e
        }
    }

    override fun createViewManagers(reactCtx: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }
}
