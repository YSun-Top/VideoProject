#include <cstring>
#include "VideoStream.h"

//
// Created by Void on 2023/5/5.
//
VideoStream::VideoStream():m_frameLen(0),
                           videoCodec(nullptr),
                           pic_in(nullptr),
                           videoCallback(nullptr) {

}

void VideoStream::setVideoCallback(VideoCallback callback) {
    this->videoCallback = callback;
}

void VideoStream::encodeVideo(int8_t *data, int camera_type) {
    std::lock_guard<std::mutex> l(m_mutex);
    if (!pic_in)
        return;

    if (camera_type == 1) {
        memcpy(pic_in->img.plane[0], data, m_frameLen); // y
        for (int i = 0; i < m_frameLen/4; ++i) {
            *(pic_in->img.plane[1] + i) = *(data + m_frameLen + i * 2 + 1);  // u
            *(pic_in->img.plane[2] + i) = *(data + m_frameLen + i * 2); // v
        }
    } else if (camera_type == 2) {
        int offset = 0;
        memcpy(pic_in->img.plane[0], data, (size_t) m_frameLen); // y
        offset += m_frameLen;
        memcpy(pic_in->img.plane[1], data + offset, (size_t) m_frameLen / 4); // u
        offset += m_frameLen / 4;
        memcpy(pic_in->img.plane[2], data + offset, (size_t) m_frameLen / 4); // v
    } else {
        return;
    }

    x264_nal_t *pp_nal;
    int pi_nal;
    x264_picture_t pic_out;
    x264_encoder_encode(videoCodec, &pp_nal, &pi_nal, pic_in, &pic_out);
    int pps_len, sps_len = 0;
    uint8_t sps[100];
    uint8_t pps[100];
    for (int i = 0; i < pi_nal; ++i) {
        x264_nal_t nal = pp_nal[i];
        if (nal.i_type == NAL_SPS) {
            sps_len = nal.i_payload - 4;
            memcpy(sps, nal.p_payload + 4, static_cast<size_t>(sps_len));
        } else if (nal.i_type == NAL_PPS) {
            pps_len = nal.i_payload - 4;
            memcpy(pps, nal.p_payload + 4, static_cast<size_t>(pps_len));
            sendSpsPps(sps, pps, sps_len, pps_len);
        } else {
            sendFrame(nal.i_type, nal.p_payload, nal.i_payload);
        }
    }
}
