//
// Created by Void on 2022/3/28.
//

#include "ffmpeg_decoder_jni.h"

JavaVM *g_jvm;

FFmpegDecoderJni *libDefine;
NativePlayer nativePlayer;
//初始化读写锁
pthread_rwlock_t stateCallbackRWLock = PTHREAD_RWLOCK_INITIALIZER;
pthread_rwlock_t errorCallbackRWLock = PTHREAD_RWLOCK_INITIALIZER;

FFmpegDecoderJni::FFmpegDecoderJni() {
    libDefine = this;
    nativePlayer = NativePlayer();
    stateCallbackList = new LinkedList();
    errorCallbackList = new LinkedList();
}

void FFmpegDecoderJni::jniPlayStatusCallback(int status) const {
    if (isRelease || stateCallbackList == nullptr)return;
    LOGD("addCallback-playStatus--status:%d", status);
    //写加锁
    pthread_rwlock_wrlock(&stateCallbackRWLock);
    stateCallbackList->add((BaseNode *) new JniBean(status));
    //写解锁
    pthread_rwlock_unlock(&stateCallbackRWLock);
}

void FFmpegDecoderJni::jniErrorCallback(int errorCode, char const *msg) const {
    if (isRelease || errorCallbackList == nullptr)return;
    LOGE("addCallback-error--status:%d, msg:%s", errorCode, msg);
    //写加锁
    pthread_rwlock_wrlock(&errorCallbackRWLock);

    errorCallbackList->add((BaseNode *) new JniBean(errorCode, msg));
    //写解锁
    pthread_rwlock_unlock(&errorCallbackRWLock);
}

/**
 * 回调程序
 * 专门处理回调内容的的线程
 */
void *onStateCallbackThread(void *arg) {
    JNIEnv *env = libDefine->get_env();
    JniBean *bean;
    jstring jmsg = nullptr;

    if (env == nullptr) goto delEnv;

    while (!libDefine->isRelease) {
        if (libDefine->isRelease)break;
        pthread_rwlock_rdlock(&stateCallbackRWLock);
        if (libDefine->stateCallbackList != nullptr && libDefine->stateCallbackList->Size() > 0) {
            bean = (JniBean *) libDefine->stateCallbackList->get(0);
//            LOGD("回调线程运行中--playState--:code:%d", bean->code);
            env->CallVoidMethod(libDefine->g_obj, libDefine->playStatusCallback,
                                bean->code);
            libDefine->stateCallbackList->removeAt(0);
        }
        pthread_rwlock_unlock(&stateCallbackRWLock);
        usleep(200 * 1000);
    }
    env->DeleteLocalRef(jmsg);
    delEnv:
    libDefine->del_env();
    pthread_exit(nullptr);
}

void *onErrorCallbackThread(void *arg) {
    JNIEnv *env = libDefine->get_env();
    JniBean *bean;
    jstring jmsg = nullptr;

    if (env == nullptr) goto delEnv;
    while (!libDefine->isRelease) {
        pthread_rwlock_rdlock(&errorCallbackRWLock);
        if (libDefine->errorCallbackList != nullptr && libDefine->errorCallbackList->Size() > 0) {
            bean = (JniBean *) libDefine->errorCallbackList->get(0);
            LOGE("回调线程运行中--error--:code:%d ;msg:%s", bean->code, bean->msg);

            jmsg = env->NewStringUTF(bean->msg);
            env->CallVoidMethod(libDefine->g_obj, libDefine->errorCallback, bean->code,
                                jmsg);
            libDefine->errorCallbackList->removeAt(0);
        }
        pthread_rwlock_unlock(&errorCallbackRWLock);
        usleep(200 * 1000);
    }
    env->DeleteLocalRef(jmsg);
    delEnv:
    libDefine->del_env();
    pthread_exit(nullptr);
}

void FFmpegDecoderJni::onRelease() {
    stateCallbackList->release();
    stateCallbackList = nullptr;
    errorCallbackList->release();
    errorCallbackList = nullptr;
}

extern "C" {
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

VIDEO_LIB_FUNC(void, initJni) {
    libDefine = new FFmpegDecoderJni();
    jclass ffmpegDecoder = env->GetObjectClass(thiz);
    if (!ffmpegDecoder) {
        LOGE("找不到类:%s", decoderClassName);
        return;
    }
    libDefine->playStatusCallback = env->GetMethodID(ffmpegDecoder,
                                                     "jniPlayStatusCallback", "(I)V");
    if (!libDefine->playStatusCallback) {
        LOGE("找不到方法:jniPlayStatusCallback(int)");
        return;
    }
    libDefine->errorCallback = env->GetMethodID(ffmpegDecoder, "jniErrorCallback",
                                                "(ILjava/lang/String;)V");
    if (!libDefine->errorCallback) {
        LOGE("找不到方法:jniErrorCallback(int,String)");
        return;
    }
    libDefine->createAudioTrack = env->GetMethodID(ffmpegDecoder, "createAudioTrack", "(II)V");
    if (!libDefine->createAudioTrack) {
        LOGE("找不到方法:createAudioTrack(int,int)");
        return;
    }
    libDefine->playAudioMethod = env->GetMethodID(ffmpegDecoder, "playAudio", "()V");
    if (!libDefine->playAudioMethod) {
        LOGE("找不到方法:playAudio()");
        return;
    }
    libDefine->stopAudioMethod = env->GetMethodID(ffmpegDecoder, "stopAudio", "()V");
    if (!libDefine->stopAudioMethod) {
        LOGE("找不到方法:stopAudio()");
        return;
    }
    libDefine->releaseJniMethod = env->GetMethodID(ffmpegDecoder, "releaseJni", "()V");
    if (!libDefine->releaseJniMethod) {
        LOGE("找不到方法:releaseJni()");
        return;
    }
    libDefine->writeAudioDataMethod = env->GetMethodID(ffmpegDecoder, "writeAudioData", "([BII)I");
    if (!libDefine->writeAudioDataMethod) {
        LOGE("找不到方法:releaseJni()");
        return;
    }

    libDefine->isRelease = false;
    libDefine->g_obj = env->NewGlobalRef(thiz);
    //执行消息回调线程
    pthread_create(&libDefine->pt[0], nullptr, &onStateCallbackThread, nullptr);
    pthread_create(&libDefine->pt[1], nullptr, &onErrorCallbackThread, nullptr);
}

VIDEO_LIB_FUNC(void, setDisplay, jobject surface) {
    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, surface);
    if (nativeWindow == nullptr) {
        LOGE("Could not get native window from surface");
        libDefine->jniErrorCallback(INIT_FAIL, "播放异常，surface无效");
        return;
    }
    nativePlayer.setPlayInfo(nativeWindow);
}

VIDEO_LIB_FUNC(void, setDataSource, jstring vPath) {
  if (libDefine->isRelease)return;
    nativePlayer.file_name = env->GetStringUTFChars(vPath, nullptr);
    nativePlayer.setPlayStatus(0);
    nativePlayer.init_player();
    libDefine->jniPlayStatusCallback(0);
}

VIDEO_LIB_FUNC(long long, getCurrentPosition) {
    if (libDefine->isRelease)return 0;
    return nativePlayer.getPlayProgress(0);
}

VIDEO_LIB_FUNC(long long, getDuration) {
    if (libDefine->isRelease)return 0;
    return nativePlayer.getPlayProgress(1);
}

VIDEO_LIB_FUNC(void, goSelectedTime, jint t) {
    if (libDefine->isRelease)return;
    return nativePlayer.seekTo(t);
}

VIDEO_LIB_FUNC(bool, isPlaying) {
    if (libDefine->isRelease)return false;
    return nativePlayer.getPlayStatus() == 1;
}

VIDEO_LIB_FUNC(void, setPlayState, jint status) {
    if (libDefine->isRelease)return;
    nativePlayer.setPlayStatus(status);
    libDefine->isRelease = status == 5;
    if (status != 5)return;
    libDefine->onRelease();
}

VIDEO_LIB_FUNC(void, setFilter, jstring value) {
    nativePlayer.filter_descr = env->GetStringUTFChars(value, nullptr);
    LOGD("setFilter:%d", nativePlayer.getPlayStatus());
    nativePlayer.setPlayStatus(2);
    usleep(50 * 1000);
    nativePlayer.setPlayStatus(1);
}

VIDEO_LIB_FUNC(void, isPlayAudio, jboolean flag) {
    nativePlayer.isPlayAudio = flag;
}
}