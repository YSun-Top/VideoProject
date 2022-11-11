
#include "ffmpeg_cmd.h"

extern "C" {
FFMPEG_CMD_FUNC(jint, executeFFmpeg, jobjectArray cmd_str) {
    av_log_set_level(AV_LOG_INFO);
    av_log_set_callback(log_callback);
    int length = env->GetArrayLength(cmd_str);
    char **argv = (char **) malloc(length * sizeof(char *));
    int i;
    for (i = 0; i < length; i++) {
        jstring jstr = (jstring) env->GetObjectArrayElement(cmd_str, i);
        char *temp = (char *) env->GetStringUTFChars(jstr, 0);
        argv[i] = static_cast<char *>(malloc(1024));
        strcpy(argv[i], temp);
        env->ReleaseStringUTFChars(jstr, temp);
    }
    int result;
    //execute ffmpeg command
    result = ffmpeg_run(length, argv);
    //release memory
    for (i = 0; i < length; i++) {
        free(argv[i]);
    }
    free(argv);
    return result;
}

FFMPEG_CMD_FUNC(jint, executeFF, jstring cmd_str) {
    char *temp = (char *) env->GetStringUTFChars(cmd_str, 0);
    int length = env->GetStringLength(cmd_str);
    char **argv = (char **) malloc(length);
    strcpy(argv[1], temp);
    int result = ffmpeg_run(length, argv);
    //release memory
    free(argv[1]);
    free(argv);
    env->ReleaseStringUTFChars(cmd_str, temp);
    return result;
}
}