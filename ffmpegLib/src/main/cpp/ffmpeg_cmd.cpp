
#include "ffmpeg_cmd.h"

FFmpegCmdJni *ffmpegCmdJni;

extern "C" {
FFMPEG_CMD_FUNC(void, executeFFCallback, jobjectArray cmd_str) {
    ffmpegCmdJni = new FFmpegCmdJni();
    jclass ffmpegCmdClass = env->GetObjectClass(thiz);
    if (!ffmpegCmdClass) {
        LOGE("找不到类:%s", "commandModeCallback");
        return;
    }
    ffmpegCmdJni->g_obj = env->NewGlobalRef(thiz);
    ffmpegCmdJni->onFinishCallback = env->GetMethodID(ffmpegCmdClass, "onFinish", "()V");
    ffmpegCmdJni->onErrorCallback = env->GetMethodID(ffmpegCmdClass, "onError",
                                                     "(Ljava/lang/String;)V");
    if (!ffmpegCmdJni->onFinishCallback) {
        LOGE("找不到方法:onFinish()");
        return;
    }
    if (!ffmpegCmdJni->onErrorCallback) {
        LOGE("找不到方法:onError(String)");
        return;
    }
    av_log_set_level(AV_LOG_INFO);
    av_log_set_callback(log_callback);
    int length = env->GetArrayLength(cmd_str);
    char **argv = (char **) malloc(length * sizeof(char *));
    int i;
    for (i = 0; i < length; i++) {
        jstring jStr = (jstring) env->GetObjectArrayElement(cmd_str, i);
        char *temp = (char *) env->GetStringUTFChars(jStr, 0);
        argv[i] = static_cast<char *>(malloc(1024));
        strcpy(argv[i], temp);
        env->ReleaseStringUTFChars(jStr, temp);
    }
    int result;
    //execute ffmpeg command
    result = ffmpeg_run(length, argv);
    if (result == 0) {
        env->CallVoidMethod(ffmpegCmdJni->g_obj, ffmpegCmdJni->onFinishCallback);
    } else {
        //需要将ffmpeg.c中的日志重写才能实现错误信息的返回 todo
        jstring jStr = (jstring) env->GetObjectArrayElement(cmd_str, 0);
        env->CallVoidMethod(thiz, ffmpegCmdJni->onErrorCallback, jStr);
    }
    //release memory
    for (i = 0; i < length; i++) {
        free(argv[i]);
    }
    free(argv);
}
}