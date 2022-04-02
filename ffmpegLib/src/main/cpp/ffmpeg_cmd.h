

#ifndef _Included_FFmpeg_Cmd
#define _Included_FFmpeg_Cmd
#include <jni.h>
#include "define/default_code.h"

#define FFMPEG_TAG "FFmpegCmd"
enum ProgressState{
    STATE_INIT,
    STATE_RUNNING,
    STATE_FINISH,
    STATE_ERROR
};

void progress_callback(int position, int duration, int state);

void log_callback(void *, int, const char *, va_list);
#endif