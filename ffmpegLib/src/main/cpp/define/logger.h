//
// Created by Void on 2020/11/30.
//

#ifndef TESTEXAMPLE_LOGGER_H
#define TESTEXAMPLE_LOGGER_H

#ifdef ANDROID

#include <android/log.h>

#define LOG_TAG    "FFmpeg_LIB"
#define LOGD(format, ...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, format, ##__VA_ARGS__)
#define LOGI(format, ...)  __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, format, ##__VA_ARGS__)
#define LOGW(format, ...)  __android_log_print(ANDROID_LOG_WARN, LOG_TAG, format, ##__VA_ARGS__)
#define LOGE(format, ...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, format, ##__VA_ARGS__)

#define IsCloseLog_v true
#define LOGD_v(format, ...)  __android_log_vprint(ANDROID_LOG_DEBUG, LOG_TAG, format, ##__VA_ARGS__)
#define LOGI_v(format, ...)  __android_log_vprint(ANDROID_LOG_INFO,  LOG_TAG, format, ##__VA_ARGS__)
#define LOGW_v(format, ...)  __android_log_vprint(ANDROID_LOG_WARN, LOG_TAG, format, ##__VA_ARGS__)
#define LOGE_v(format, ...)  __android_log_vprint(ANDROID_LOG_ERROR, LOG_TAG, format, ##__VA_ARGS__)
#else
#define LOGE(format, ...)  println2(LOG_TAG format, ##__VA_ARGS__)
#define LOGI(format, ...)  println2(LOG_TAG format, ##__VA_ARGS__)
#endif

#endif //TESTEXAMPLE_LOGGER_H
