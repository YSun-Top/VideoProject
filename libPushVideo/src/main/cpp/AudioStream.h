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
    AudioCallback audioCallback;
    int m_channels;
    faacEncHandle m_audioCodec = 0;
    u_long m_inputSamples;
    u_long m_maxOutputBytes;
    u_char *m_buffer = 0;

public:
    AudioStream();

    ~AudioStream();

    int setAudioEncInfo(int samplesInHZ, int channels);

    void setAudioCallback(AudioCallback callback);

    int getInputSamples() const;

    void encodeData(int8_t *data);

    RTMPPacket *getAudioTag();

};
#endif //VIDEOPROJECT_AUDIOSTREAM_H
