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

//region play status
//-1=未知状态
#define PLAY_STATUS_UNKNOWN_STATUS -1
//0=准备
#define PLAY_STATUS_PREPARED 0
//1=播放中
#define PLAY_STATUS_PREPARING 1
//2=暂停中
#define PLAY_STATUS_STOP 2
//3=播放完成
#define PLAY_STATUS_COMPLETE 3
//4=播放取消
#define PLAY_STATUS_CANCEL 4
//5=释放资源
#define PLAY_STATUS_RELEASE 5
//6=更新滤镜
#define PLAY_STATUS_UPDATE_FILTER 6
//endregion

//inline void checkUtf8Bytes(char const*bytes);
void throwError(char const*msg);

#endif //FFMPEGLIB_DEFAULT_CODE_H
