//
// Created by Void on 2022/4/7.
//
#include "run_callback.h"

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