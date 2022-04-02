//
// Created by Void on 2020/12/10.
// 错误代码定义
//

#ifndef TESTEXAMPLE_ERRORCODEDEFINE_H
#define TESTEXAMPLE_ERRORCODEDEFINE_H

#define INIT_FAIL 0x01
#define FILTER_CHANGE_FAIL 0x02
#define OPEN_CODEC_FAIL 0x03
#define CODEC_NOT_FOUNT 0x04
#define VIDEO_STREAM_NOT_FOUNT 0x05


/**
 * 错误消息结构
 */
struct ErrorInfoObj {
    int errorCode;
    char const *errorMessage;
};


#endif //TESTEXAMPLE_ERRORCODEDEFINE_H
