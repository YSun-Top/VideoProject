//
// Created by Void on 2023/5/5.
//

#ifndef VIDEOPROJECT_VIDEOSTREAM_H
#define VIDEOPROJECT_VIDEOSTREAM_H

#include <inttypes.h>
#include <mutex>
#include "rtmp/rtmp.h"
#include "include/x264/x264.h"

class VideoStream{
    typedef void (*VideoCallback)(RTMPPacket *packet);

private:
    std::mutex m_mutex;

    int m_frameLen;
    x264_t *videoCodec = 0;
    x264_picture_t *pic_in = 0;

    VideoCallback videoCallback;

    void sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len);

    void sendFrame(int type, uint8_t *payload, int i_payload);

public:
    VideoStream();

    ~VideoStream();

    int setVideoEncInfo(int width, int height, int fps, int bitrate);

    void encodeVideo(int8_t *data, int camera_type);

    void setVideoCallback(VideoCallback callback);

};

#endif //VIDEOPROJECT_VIDEOSTREAM_H
