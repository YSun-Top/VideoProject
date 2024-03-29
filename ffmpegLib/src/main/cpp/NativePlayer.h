//
// Created by Void on 2020/11/30.
//

#ifndef TESTEXAMPLE_NATIVEPLAYER_H
#define TESTEXAMPLE_NATIVEPLAYER_H

#include <android/native_window.h>
#include "define/default_code.h"
#include "define/linked_list_define.h"

#ifdef __cplusplus
extern "C" {
#endif

#include "libavformat/avformat.h"
#include "libavutil/imgutils.h"
#include "libswscale/swscale.h"
#include "libavfilter/avfilter.h"
#include "libavutil/opt.h"
#include <libavfilter/buffersrc.h>
#include <libavfilter/buffersink.h>
#include "libswresample/swresample.h"
#include "run_callback.h"

#define MAX_AUDIO_FRAME_SIZE 48000 * 4
#ifdef __cplusplus
}
#endif

class NativePlayer {
private:
    //详细设定请看：src/main/cpp/define/default_code.h
    int playStatus = -1;
public:
    //播放进度(ms)
    long jniCurrentTime = 0L;

    const char *file_name;
    const char *filter_descr = "lutyuv='u=128:v=128'";
    int findFileInfo_Ok = 1;
    int videoIndex = -1;
    int audioIndex = -1;
    long jniMaxTime = 0;
    bool isPlayAudio = true;

    //视频编解码器
    const AVCodec *vCodec = NULL;
    //音频编解码器
    const AVCodec *aCodec = NULL;

    int init_player();

    int open_file(const char *file_name);

    int change_filter() const;

    int init_audio();

    void setPlayInfo(ANativeWindow *aNWindow);

    /**
     * 获取播放进度
     * @param type 0当前进度 1总时长
     * @return 返回对应时间单位毫秒(ms)
     */
    long long getPlayProgress(int type) const;

    /**
     * 设置播放进度
     * @param t 目标时间戳单位：ms
     */
    void seekTo(int t);

    /**
     * 设置播放状态
     * 注：设置状态5后，该引用将失效，需要重新初始化。
     * //详细设定请看：src/main/cpp/define/default_code.h
     * @param status
     */
    void setPlayStatus(int status);

    int getPlayStatus() const;

    static int writeAudioData(AVPacket *packet, AVFrame *frame);
};
#endif //TESTEXAMPLE_NATIVEPLAYER_H