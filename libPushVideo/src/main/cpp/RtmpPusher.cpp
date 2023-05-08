//
// Created by Void on 2023/5/5.
//
#include <jni.h>
#include "VideoStream.h"
#include "AudioStream.h"
#include "PacketQueue.h"
#include "PushInterface.h"

#define RTMP_PUSHER_FUNC(RETURN_TYPE, FUNC_NAME, ...) \
    extern "C" \
    JNIEXPORT RETURN_TYPE JNICALL Java_com_voidcom_videoproject_ui_rtp_LivePusherNew_ ## FUNC_NAME \
    (JNIEnv *env, jobject instance, ##__VA_ARGS__)    \

PacketQueue<RTMPPacket *> packets;
VideoStream *videoStream = nullptr;
AudioStream *audioStream = nullptr;

std::atomic<bool> isPushing;
uint32_t start_time;


JavaVM *javaVM;
jobject jobject_error;

//when calling System.loadLibrary, will callback it
jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    return JNI_VERSION_1_6;
}

//callback error to java
void throwErrToJava(int error_code) {
    JNIEnv *env;
    javaVM->AttachCurrentThread(&env, nullptr);
    jclass classErr = env->GetObjectClass(jobject_error);
    jmethodID methodErr = env->GetMethodID(classErr, "errorFromNative", "(I)V");
    env->CallVoidMethod(jobject_error, methodErr, error_code);
    javaVM->DetachCurrentThread();
}

void callback(RTMPPacket *packet) {
    if (packet) {
        packet->m_nTimeStamp = RTMP_GetTime() - start_time;
        packets.push(packet);
    }
}

void releasePackets(RTMPPacket *&packet) {
    if (packet) {
        RTMPPacket_Free(packet);
        delete packet;
        packet = nullptr;
    }
}

void *start(void *args) {
    char *url = static_cast<char *>(args);
    RTMP *rtmp;
    do {
        rtmp = RTMP_Alloc();
        if (!rtmp) {
            LOGE("RTMP_Alloc fail");
            break;
        }
        RTMP_Init(rtmp);
        int ret = RTMP_SetupURL(rtmp, url);
        if (!ret) {
            LOGE("RTMP_SetupURL:%s", url);
            break;
        }
        //timeout
        rtmp->Link.timeout = 5;
        RTMP_EnableWrite(rtmp);
        ret = RTMP_Connect(rtmp, nullptr);
        if (!ret) {
            LOGE("RTMP_Connect:%s", url);
            throwErrToJava(ERROR_RTMP_CONNECT);
            break;
        }
        ret = RTMP_ConnectStream(rtmp, 0);
        if (!ret) {
            LOGE("RTMP_ConnectStream:%s", url);
            throwErrToJava(ERROR_RTMP_CONNECT_STREAM);
            break;
        }
        //start time
        start_time = RTMP_GetTime();
        //start pushing
        isPushing = true;
        packets.setRunning(true);
        callback(audioStream->getAudioTag());
        RTMPPacket *packet = nullptr;
        while (isPushing) {
            packets.pop(packet);
            if (!isPushing) {
                break;
            }
            if (!packet) {
                continue;
            }

            packet->m_nInfoField2 = rtmp->m_stream_id;
            ret = RTMP_SendPacket(rtmp, packet, 1);
            releasePackets(packet);
            if (!ret) {
                LOGE("RTMP_SendPacket fail...");
                throwErrToJava(ERROR_RTMP_SEND_PACKET);
                break;
            }
        }
        releasePackets(packet);
    } while (0);
    isPushing = false;
    packets.setRunning(false);
    packets.clear();
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
    delete (url);
    return nullptr;
}

RTMP_PUSHER_FUNC(void, nativeInit) {
    LOGI("native init...");
    videoStream = new VideoStream();
    videoStream->setVideoCallback(callback);
    audioStream = new AudioStream();
    audioStream->setAudioCallback(callback);
    packets.setReleaseCallback(releasePackets);
    jobject_error = env->NewGlobalRef(instance);
}

RTMP_PUSHER_FUNC(void, nativePushVideo, jbyteArray yuv, jint cameraType) {
    if (!videoStream || !isPushing) {
        return;
    }
    jbyte *yuv_plane = env->GetByteArrayElements(yuv, JNI_FALSE);
    videoStream->encodeVideo(yuv_plane, cameraType);
    env->ReleaseByteArrayElements(yuv, yuv_plane, 0);
}

RTMP_PUSHER_FUNC(void, nativeStart, jstring path) {
    if (isPushing) {
        return;
    }
    const char *path_ = env->GetStringUTFChars(path, nullptr);
    char *url = new char[strlen(path_) + 1];
    strcpy(url, path_);
    LOGI("native start...: %s", url);

    std::thread pushThread(start, url);
    pushThread.detach();
    env->ReleaseStringUTFChars(path, path_);
}

RTMP_PUSHER_FUNC(void, nativeStop) {
    LOGI("native stop...");
    isPushing = false;
    packets.setRunning(false);
}
RTMP_PUSHER_FUNC(void, nativeRelease) {
    LOGI("native release...");
    env->DeleteGlobalRef(jobject_error);
    delete videoStream;
    videoStream = nullptr;
    delete audioStream;
    audioStream = nullptr;
}

RTMP_PUSHER_FUNC(void, nativeSetAudioCodecInfo, jint sampleRateInHz, jint channels) {
    if (!audioStream)return;
    int ret = audioStream->setAudioEncInfo(sampleRateInHz, channels);
    if (ret >= 0) return;
    throwErrToJava(ERROR_AUDIO_ENCODER_OPEN);
}
