cmake_minimum_required(VERSION 3.18.1)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")
project("ffmpegLib")

# 添加头文件路径
include_directories(ffmpeg)
include_directories(include)
# 定义源码所在目录
aux_source_directory(. SRC_CPP)
aux_source_directory(./define SRC_DEFINE)
aux_source_directory(./ffmpeg SRC_FFMPEG)
list(APPEND SRC_CPP ${SRC_DEFINE})
list(APPEND SRC_CPP ${SRC_FFMPEG})
message("output ${SRC_CPP}")
if (${CMAKE_ANDROID_ARCH_ABI} MATCHES "armeabi-v7a")
    message("This is armeabi-v7a")
    include_directories(include/armeabi-v7a)
elseif (${CMAKE_ANDROID_ARCH_ABI} MATCHES "arm64-v8a")
    message("This is arm64-v8a")
    include_directories(include/arm64-v8a)
elseif (${CMAKE_ANDROID_ARCH_ABI} MATCHES "x86_64")
    message("This is x86_64")
    include_directories(include/x86_64)
endif ()

add_library(
        ffmpegSDK
        SHARED
        ${SRC_CPP})

# 定义so库所在位置
set(SO_DIR ${CMAKE_SOURCE_DIR}/../../../libs/${CMAKE_ANDROID_ARCH_ABI})
# 第三方SO库
add_library(ffmpeg
        SHARED
        IMPORTED)
set_target_properties(ffmpeg
        PROPERTIES IMPORTED_LOCATION
        ${SO_DIR}/libffmpeg.so)
#add_library(mp3lame
#        SHARED
#        IMPORTED)
#set_target_properties(mp3lame
#        PROPERTIES IMPORTED_LOCATION
#        ${SO_DIR}/libmp3lame.so)
#add_library(x264
#        SHARED
#        IMPORTED)
#set_target_properties(x264
#        PROPERTIES IMPORTED_LOCATION
#        ${SO_DIR}/libx264.so)
# 日志库
find_library(
        log-lib
        log)
# 将其他库链接到目标库ffmpegSDK
target_link_libraries(
        ffmpegSDK
        ffmpeg
#        mp3lame
#        x264
        -landroid #native_window
        -ljnigraphics #bitmap
        -lOpenSLES #openSLES
        ${log-lib})