#include <jni.h>
#include <string.h>
#include <android/log.h>

#define LOG_TAG "SimpleNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

JNIEXPORT jstring JNICALL
Java_com_noise_1ai_SimpleNativeModule_testNative(
    JNIEnv* env, jobject thiz) {
    LOGI("testNative function called successfully!");
    return (*env)->NewStringUTF(env, "Hello from SimpleNative C code!");
}

JNIEXPORT jint JNICALL
Java_com_noise_1ai_SimpleNativeModule_addNumbers(
    JNIEnv* env, jobject thiz, jint a, jint b) {
    LOGI("addNumbers called with %d + %d", a, b);
    jint result = a + b;
    LOGI("addNumbers result: %d", result);
    return result;
}
