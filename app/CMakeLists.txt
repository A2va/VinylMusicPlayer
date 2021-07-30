cmake_minimum_required(VERSION 3.4.1)

set(ffmpeg_dir ${CMAKE_SOURCE_DIR}/../ffmpeg-android-maker/output)
set(ffmpeg_libs ${ffmpeg_dir}/lib/${ANDROID_ABI})

include_directories(${ffmpeg_dir}/include/${ANDROID_ABI})

set(
        # List variable name
        ffmpeg_libs_names
        # Values in the list
        avutil avformat avcodec swscale swresample avfilter)

foreach (ffmpeg_lib_name ${ffmpeg_libs_names})
    add_library(
            ${ffmpeg_lib_name}
            SHARED
            IMPORTED)
    set_target_properties(
            ${ffmpeg_lib_name}
            PROPERTIES
            IMPORTED_LOCATION
            ${ffmpeg_libs}/lib${ffmpeg_lib_name}.so)
endforeach ()

add_library(
        # Name for a library to build
        media-player
        # Type of a library
        SHARED
        # All cpp files to compile   
		src/main/cpp/AudioPlayer.cpp
		src/main/cpp/AudioPlayer.h
        src/main/cpp/player_interface.cpp)

target_link_libraries(
        # Library to link
        media-player
        # List of libraries to link against:
        # Library for writing messages in LogCat
        log
        OpenSLES
        # FFmpeg libraries
        ${ffmpeg_libs_names})