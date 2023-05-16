//
// Created by Void on 2023/5/5.
//

#ifndef VIDEOPROJECT_AUDIOSTREAM_H
#define VIDEOPROJECT_AUDIOSTREAM_H

#include <sys/types.h>
#include "include/faac/faac.h"
#include "rtmp/rtmp.h"

class AudioStream {
    typedef void (*AudioCallback)(RTMPPacket *packet);

private:
    AudioCallback audioCallback = nullptr;
    int m_channels = 0;
    faacEncHandle m_audioCodec = 0;
    u_long m_inputSamples = 0;
    u_long m_maxOutputBytes = 0;
    u_char *m_buffer = 0;

public:
    AudioStream();

    ~AudioStream();

    int setAudioEncInfo(int samplesInHZ, int channels);

    void setAudioCallback(AudioCallback callback);

    void encodeData(int8_t *data);

    RTMPPacket *getAudioTag();

};
#endif //VIDEOPROJECT_AUDIOSTREAM_H
