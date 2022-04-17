//
// Created by Void on 2022/3/28.
//
#ifndef VIDEOPROJECT_FFMPEG_DECODER_JNI_H
#define VIDEOPROJECT_FFMPEG_DECODER_JNI_H

#include <jni.h>
#include <iostream>
#include <cstdlib>
#include <unistd.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include "NativePlayer.h"
#include "define/default_code.h"

#define decoderClassName "Java_com_voidcom_ffmpeglib_FFmpegDecoderJni_"

typedef struct JniBeanNode {
  BaseNode node;
  int code;
  const char *msg;

  JniBeanNode(int code) {
    this->code = code;
    this->msg = "-";
  }

  JniBeanNode(int code, const char *msg) {
    this->code = code;
//    checkUtf8Bytes(msg);
    if (strlen(msg) == 0) {
      msg = "-";
    }
    this->msg = msg;
  }
} JniBean;

class FFmpegDecoderJni {
 public:
  LinkedList *stateCallbackList;
  LinkedList *errorCallbackList;
  //全局释放标识
  bool isRelease = false;
  //线程指针数组
  pthread_t pt[10]{};

  JavaVM *g_jvm = nullptr;
  jobject g_obj = nullptr;
  jmethodID playStatusCallback = nullptr;
  jmethodID errorCallback = nullptr;
  jmethodID createAudioTrack = nullptr;
  jmethodID playAudioMethod = nullptr;
  jmethodID stopAudioMethod = nullptr;
  jmethodID releaseJniMethod = nullptr;
  jmethodID writeAudioDataMethod = nullptr;

  FFmpegDecoderJni();

  void jniPlayStatusCallback(int status);

  void jniErrorCallback(int errorCode, char const*msg);

  void onRelease();

  JNIEnv *get_env() {
    if (g_jvm == nullptr)return nullptr;
    JNIEnv *env = nullptr;
    int status = g_jvm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (status == JNI_EDETACHED || env == nullptr) {
      status = g_jvm->AttachCurrentThread(&env, nullptr);
      if (status < 0) env = nullptr;
    }
    return env;
  }

  void del_env() {
    g_jvm->DetachCurrentThread();
  }
};

#endif //VIDEOPROJECT_FFMPEG_DECODER_JNI_H