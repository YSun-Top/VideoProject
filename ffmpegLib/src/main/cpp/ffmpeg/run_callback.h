//
// Created by Void on 2022/4/7.
//

#ifndef VIDEOPROJECT_FFMPEGLIB_SRC_MAIN_CPP_RUN_CALLBACK_H_
#define VIDEOPROJECT_FFMPEGLIB_SRC_MAIN_CPP_RUN_CALLBACK_H_

#include <jni.h>
#include "../define/logger.h"
#ifdef __cplusplus
extern "C" {
#endif
#include <libavutil/log.h>
#ifdef __cplusplus
}
#endif
enum ProgressState{
  STATE_INIT,
  STATE_RUNNING,
  STATE_FINISH,
  STATE_ERROR
};

void progress_callback(int position, int duration, int state);

void log_callback(void *, int, const char *, va_list);

#endif //VIDEOPROJECT_FFMPEGLIB_SRC_MAIN_CPP_RUN_CALLBACK_H_
