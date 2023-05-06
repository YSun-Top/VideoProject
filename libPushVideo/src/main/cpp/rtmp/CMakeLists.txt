cmake_minimum_required(VERSION 3.18.1)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO")

add_library(rtmp

        STATIC

        amf.c
        log.c
        rtmp.c)
