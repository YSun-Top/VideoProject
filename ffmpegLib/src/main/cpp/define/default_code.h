//
// Created by Void on 2020/9/8.
//

#ifndef TESTEXAMPLE_DEFAULT_CODE_H
#define TESTEXAMPLE_DEFAULT_CODE_H

//#include <cstring>
//#include <stdlib>
#include "logger.h"
#include "jni.h"

#define VIDEO_PLAYER_FUNC(RETURN_TYPE, FUNC_NAME, ...) \
extern "C" { \
    JNIEXPORT RETURN_TYPE JNICALL Java_com_voidcom_ffmpeglib_FFmpegCmd_ ## FUNC_NAME \
    (JNIEnv *env, jclass thiz, ##__VA_ARGS__);\
}\
    JNIEXPORT RETURN_TYPE JNICALL Java_com_voidcom_ffmpeglib_FFmpegCmd_ ## FUNC_NAME \
    (JNIEnv *env, jclass thiz, ##__VA_ARGS__)\

#endif //TESTEXAMPLE_DEFAULT_CODE_H
