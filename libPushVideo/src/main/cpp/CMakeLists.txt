cmake_minimum_required(VERSION 3.18.1)

project("libPushVideo")

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

aux_source_directory(. SRC_CPP)
aux_source_directory(./rtmp SRC_RTMP)
list(APPEND SRC_CPP ${SRC_RTMP})

message("output ${SRC_CPP}")


include_directories(./include)
include_directories(./rtmp)

add_library(${CMAKE_PROJECT_NAME}
        SHARED
        ${SRC_CPP})

target_link_libraries(${CMAKE_PROJECT_NAME}
        x264
        faac
        android
        graphics
        log)