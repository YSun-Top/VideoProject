//
// Created by Void on 2020/11/30.
//

#ifndef TESTEXAMPLE_LOGGER_H
#define TESTEXAMPLE_LOGGER_H

#ifdef ANDROID

#include <android/log.h>
#include <stdarg.h>

#define LOG_TAG    "FFmpeg_LIB"
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
void printLogV(const char *tag, const char *fmt, ...);

void printLogD(const char *tag, const char *fmt, ...);

void printLogI(const char *tag, const char *fmt, ...);

void printLogW(const char *tag, const char *fmt, ...);

void printLogE(const char *tag, const char *fmt, ...);

void printLog(int prio, const char *tag, const char *fmt, ...);

void vPrintLogV(const char *tag, const char *fmt, ...);

void vPrintLogD(const char *tag, const char *fmt, ...);

void vPrintLogI(const char *tag, const char *fmt, ...);

void vPrintLogW(const char *tag, const char *fmt, ...);

void vPrintLogE(const char *tag, const char *fmt, ...);

void vPrintLog(int prio, const char *tag, const char *fmt, ...);

#endif //TESTEXAMPLE_LOGGER_H
