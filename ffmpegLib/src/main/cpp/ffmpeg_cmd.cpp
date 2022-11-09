
#include "ffmpeg_cmd.h"

extern "C"{
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
}