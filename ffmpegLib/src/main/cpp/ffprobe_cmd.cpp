//
// Created by Voidcom on 2022/11/10.
//

#include "ffprobe_cmd.h"

extern "C" {
FFPROBE_CMD_FUNC(jstring, executeFFprobeArray, jobjectArray commands) {
    av_log_set_level(AV_LOG_INFO);
    av_log_set_callback(log_callback);
    int argc = env->GetArrayLength(commands);
    char **argv = (char **) malloc(argc * sizeof(char *));
    int i;
    for (i = 0; i < argc; i++) {
        jstring jstr = (jstring) env->GetObjectArrayElement(commands, i);
        char *temp = (char *) env->GetStringUTFChars(jstr, 0);
        argv[i] = static_cast<char *>(malloc(1024));
        strcpy(argv[i], temp);
        env->ReleaseStringUTFChars(jstr, temp);
    }
    char *result = ffprobe_run(argc, argv);
    //release memory
    for (i = 0; i < argc; i++) {
        free(argv[i]);
    }
    free(argv);
    if (result == NULL)return nullptr;
    return env->NewStringUTF(result);
}
}
