package com.poupa.vinylmusicplayer.service;

public class MediaPlayer {
    native  void init(String path);

    native void play();

    native void pause();

    native void release();

    native void setVolume(float leftVolume, float rightVolume);


    native int duration();

    native int position();

    native double seekTo(int msec);

    native void callback();

    static {
        // Load ffmpeg and native library
        System.loadLibrary("avutil");
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("swscale");
        System.loadLibrary("swresample");
        System.loadLibrary("media-player");
    }
}
