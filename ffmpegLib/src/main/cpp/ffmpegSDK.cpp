#include <jni.h>
#include <string>

#ifdef __cplusplus
extern "C" {
#endif
#include "ffmpeg/ffmpeg.h"
#include "ffmpeg_cmd.h"
#ifdef __cplusplus
}
#endif

VIDEO_PLAYER_FUNC(jint, executeFFmpeg, jobjectArray cmd_str) {
    av_log_set_level(AV_LOG_INFO);
    av_log_set_callback(log_callback);
    int argc = env->GetArrayLength(cmd_str);
    char **argv = (char **) malloc(argc * sizeof(char *));
    int i;
    for (i = 0; i < argc; i++) {
        jstring jstr = (jstring) env->GetObjectArrayElement(cmd_str, i);
        char *temp = (char *) env->GetStringUTFChars(jstr, 0);
        argv[i] = static_cast<char *>(malloc(1024));
        strcpy(argv[i], temp);
        env->ReleaseStringUTFChars(jstr, temp);
    }
    int result;
    //execute ffprobe command
    result = ffmpeg_run(argc, argv);
    //release memory
    for (i = 0; i < argc; i++) {
        free(argv[i]);
    }
    free(argv);
    return result;
}

VIDEO_PLAYER_FUNC(jint, executeFF, jstring cmd_str) {
    char *temp = (char *) env->GetStringUTFChars(cmd_str, 0);

    return 0;
}

void progress_callback(int position, int duration, int state) {
    LOGI("ffmpeg_progress。\n position:%d;duration:%d;state:%d", position, duration, state);
}

void msg_callback(const char *format, va_list args) {
//    if (ff_env && msg_method) {
//        char *ff_msg = (char *) malloc(sizeof(char) * INPUT_SIZE);
//        vsprintf(ff_msg, format, args);
//        jstring jstr = (*ff_env)->NewStringUTF(ff_env, ff_msg);
//        (*ff_env)->CallStaticVoidMethod(ff_env, ff_class, msg_method, jstr);
//        free(ff_msg);
//    }
}

void log_callback(void *ptr, int level, const char *format, va_list args) {
    switch (level) {
        case AV_LOG_INFO:
            LOGI_v(format, args);
            if (format && strncmp("silence", format, 7) == 0) {
                msg_callback(format, args);
            }
            break;
        case AV_LOG_ERROR:
            LOGE_v(format, args);
//            if (err_count < 10) {
//                err_count++;
//                msg_callback(format, args);
//            }
            break;
        default:
            break;
    }
}