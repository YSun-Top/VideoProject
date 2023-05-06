#include "AudioStream.h"

//
// Created by Void on 2023/5/5.
//
AudioStream::AudioStream() {

}

void AudioStream::setAudioCallback(AudioStream::AudioCallback callback) {
    this->audioCallback = callback;
}
