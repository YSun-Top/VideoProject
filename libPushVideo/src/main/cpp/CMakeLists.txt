cmake_minimum_required(VERSION 3.18.1)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")
project("libPushVideo")

# 添加头文件路径
include_directories(./include)
# 定义源码所在目录
aux_source_directory(. SRC_CPP)
list(APPEND SRC_CPP ${SRC_RTMP})

message("output ${SRC_CPP}")
add_library(${CMAKE_PROJECT_NAME}
        SHARED
        ${SRC_CPP})

add_subdirectory(./rtmp)

set(distribution_DIR ${CMAKE_SOURCE_DIR}/../../../../libs)

add_library(faac
        STATIC
        IMPORTED)
set_target_properties(faac
        PROPERTIES IMPORTED_LOCATION
        ../../../../libs/${CMAKE_ANDROID_ARCH_ABI}/libfaac.a)

add_library(x264
        STATIC
        IMPORTED)
set_target_properties(x264
        PROPERTIES IMPORTED_LOCATION
        ../../../../libs/${CMAKE_ANDROID_ARCH_ABI}/libx264.a)

target_link_libraries(${CMAKE_PROJECT_NAME}
        x264
        faac
        rtmp
        android
        jnigraphics
        log)