#include <jni.h>
#include <android/log.h>
#include "opus.h"
#include <vector>
#include <string>
#include <stdio.h>
#include <stdint.h>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "OpusBridge", __VA_ARGS__)
static const int SR = 16000;
static const int CH = 1;
static const int MAX_SAMPLES = 120 * SR / 1000; // 120ms

extern "C" void wav_write_header(FILE*, uint32_t, uint16_t, uint16_t, uint32_t);

extern "C" {
JNIEXPORT jstring JNICALL
Java_com_noise_ai_OpusBridgeModule_decodeBinToWavNative(
  JNIEnv* env,
  jobject /*thiz*/,
  jstring jIn,
  jstring jOut,
  jint jBytesPerPkt
) {
  const char* inPath  = env->GetStringUTFChars(jIn,  0);
  const char* outPath = env->GetStringUTFChars(jOut, 0);
  const int bytesPerPkt = jBytesPerPkt > 0 ? jBytesPerPkt : 80;

  FILE* fi = fopen(inPath, "rb");
  if(!fi){
    env->ReleaseStringUTFChars(jIn,inPath);
    env->ReleaseStringUTFChars(jOut,outPath);
    return env->NewStringUTF("OPEN_IN_FAIL");
  }
  fseek(fi,0,SEEK_END); long sz=ftell(fi); fseek(fi,0,SEEK_SET);
  if (sz % bytesPerPkt != 0){
    fclose(fi);
    env->ReleaseStringUTFChars(jIn,inPath);
    env->ReleaseStringUTFChars(jOut,outPath);
    return env->NewStringUTF("SIZE_NOT_MULTIPLE_OF_PACKET");
  }

  int err=0;
  OpusDecoder* dec = opus_decoder_create(SR, CH, &err);
  if(err!=OPUS_OK || !dec){
    fclose(fi);
    env->ReleaseStringUTFChars(jIn,inPath);
    env->ReleaseStringUTFChars(jOut,outPath);
    return env->NewStringUTF("OPUS_CREATE_FAIL");
  }

  FILE* fo = fopen(outPath, "wb");
  if(!fo){
    opus_decoder_destroy(dec); fclose(fi);
    env->ReleaseStringUTFChars(jIn,inPath);
    env->ReleaseStringUTFChars(jOut,outPath);
    return env->NewStringUTF("OPEN_OUT_FAIL");
  }

  // placeholder WAV header
  wav_write_header(fo, SR, CH, 16, 0);

  std::vector<unsigned char> pkt(bytesPerPkt);
  std::vector<int16_t> pcm(MAX_SAMPLES);
  uint32_t totalSamples = 0;

  long nPackets = sz / bytesPerPkt;
  for(long i=0; i<nPackets; ++i){
    if(fread(pkt.data(),1,bytesPerPkt,fi) != (size_t)bytesPerPkt) break;
    int got = opus_decode(dec, pkt.data(), bytesPerPkt, pcm.data(), MAX_SAMPLES, 0);
    if(got < 0) continue;
    fwrite(pcm.data(), sizeof(int16_t), got, fo);
    totalSamples += got;
  }

  // finalize WAV header
  fflush(fo);
  fseek(fo, 0, SEEK_SET);
  wav_write_header(fo, SR, CH, 16, totalSamples);

  fclose(fo); fclose(fi);
  opus_decoder_destroy(dec);

  env->ReleaseStringUTFChars(jIn,inPath);
  env->ReleaseStringUTFChars(jOut,outPath);
  return env->NewStringUTF("OK");
}
}
