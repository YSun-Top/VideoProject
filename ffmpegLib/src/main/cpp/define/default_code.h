//
// Created by Void on 2020/9/8.
//

#ifndef FFMPEGLIB_DEFAULT_CODE_H
#define FFMPEGLIB_DEFAULT_CODE_H

#include <jni.h>
#include "logger.h"
#include "ErrorCodeDefine.h"

#define VIDEO_PLAYER_FUNC(RETURN_TYPE, FUNC_NAME, ...) \
    JNIEXPORT RETURN_TYPE JNICALL Java_com_voidcom_ffmpeglib_FFmpegCmd_ ## FUNC_NAME \
    (JNIEnv *env, jclass thiz, ##__VA_ARGS__)\

//#define VIDEO_LIB_FUNC(RETURN_TYPE, FUNC_NAME, ...) \
//extern "C" { \
//    JNIEXPORT RETURN_TYPE JNICALL Java_com_voidcom_ffmpeglib_FFMPEGDecoderJni_ ## FUNC_NAME \
//    (JNIEnv *env, jclass thiz, ##__VA_ARGS__);\
//}\
//    JNIEXPORT RETURN_TYPE JNICALL Java_com_voidcom_ffmpeglib_FFMPEGDecoderJni_ ## FUNC_NAME \
//    (JNIEnv *env, jclass thiz, ##__VA_ARGS__)

#define VIDEO_LIB_FUNC(RETURN_TYPE, FUNC_NAME, ...) \
    JNIEXPORT RETURN_TYPE JNICALL Java_com_voidcom_ffmpeglib_FFmpegDecoderJni_ ## FUNC_NAME \
    (JNIEnv *env, jclass thiz, ##__VA_ARGS__)

//inline void checkUtf8Bytes(char const*bytes);
void throwError(char const*msg);

#endif //FFMPEGLIB_DEFAULT_CODE_H
