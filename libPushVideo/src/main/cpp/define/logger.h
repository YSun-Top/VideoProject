//
// Created by Void on 2020/11/30.
//

#ifndef TESTEXAMPLE_LOGGER_H
#define TESTEXAMPLE_LOGGER_H

#ifdef ANDROID

#include <android/log.h>
#include <stdarg.h>

#define LOG_TAG    "lib_push_video"
#define LOGD(format, ...)  LOGD_TAG( LOG_TAG, format, ##__VA_ARGS__)
#define LOGI(format, ...)  LOGI_TAG( LOG_TAG, format, ##__VA_ARGS__)
#define LOGW(format, ...)  LOGW_TAG(LOG_TAG, format, ##__VA_ARGS__)
#define LOGE(format, ...)  LOGE_TAG(LOG_TAG, format, ##__VA_ARGS__)

#define IsCloseLog_v true
#define LOGD_v(format, ...)  LOGD_v_TAG(LOG_TAG,format, ##__VA_ARGS__)
#define LOGI_v(format, ...)  LOGI_v_TAG( LOG_TAG, format, ##__VA_ARGS__)
#define LOGW_v(format, ...)  LOGW_v_TAG(LOG_TAG, format, ##__VA_ARGS__)
#define LOGE_v(format, ...)  LOGE_v_TAG( LOG_TAG, format, ##__VA_ARGS__)

#define LOGD_TAG(tag, format, ...)  __android_log_print(ANDROID_LOG_DEBUG, tag, format, ##__VA_ARGS__)
#define LOGI_TAG(tag, format, ...)  __android_log_print(ANDROID_LOG_INFO,  tag, format, ##__VA_ARGS__)
#define LOGW_TAG(tag, format, ...)  __android_log_print(ANDROID_LOG_WARN, tag, format, ##__VA_ARGS__)
#define LOGE_TAG(tag, format, ...)  __android_log_print(ANDROID_LOG_ERROR, tag, format, ##__VA_ARGS__)

#define IsCloseLog_v true
#define LOGD_v_TAG(tag, format, ...)  __android_log_vprint(ANDROID_LOG_DEBUG, tag, format, ##__VA_ARGS__)
#define LOGI_v_TAG(tag, format, ...)  __android_log_vprint(ANDROID_LOG_INFO,  tag, format, ##__VA_ARGS__)
#define LOGW_v_TAG(tag, format, ...)  __android_log_vprint(ANDROID_LOG_WARN, tag, format, ##__VA_ARGS__)
#define LOGE_v_TAG(tag, format, ...)  __android_log_vprint(ANDROID_LOG_ERROR, tag, format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  println2(LOG_TAG format, ##__VA_ARGS__)
#define LOGI(format, ...)  println2(LOG_TAG format, ##__VA_ARGS__)
#endif

#define LOGE_v_TAG(tag, format, ...)  __android_log_vprint(ANDROID_LOG_ERROR, tag, format, ##__VA_ARGS__)
/***************relative to Java**************/
//error code for opening video encoder
const int ERROR_VIDEO_ENCODER_OPEN = 0x01;
//error code for video encoding
const int ERROR_VIDEO_ENCODE = 0x02;
//error code for opening audio encoder
const int ERROR_AUDIO_ENCODER_OPEN = 0x03;
//error code for audio encoding
const int ERROR_AUDIO_ENCODE = 0x04;
//error code for RTMP connecting
const int ERROR_RTMP_CONNECT = 0x05;
//error code for connecting stream
const int ERROR_RTMP_CONNECT_STREAM = 0x06;
//error code for sending packet
const int ERROR_RTMP_SEND_PACKET = 0x07;

/***************relative to Java**************/
#endif //TESTEXAMPLE_LOGGER_H
