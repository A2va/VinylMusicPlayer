package com.poupa.vinylmusicplayer.service;

import android.content.Context;
import android.media.MediaPlayer;

public class MediaPlayerFFmpeg extends android.media.MediaPlayer {
    public native  void init(String path);

    public native void play();

    public native void pause();

    public native void release();

    public native void setVolume(float leftVolume, float rightVolume);


    public native int duration();

    public native int position();

    @Override
    public native void seekTo(int msec);

    public native void callback();

    @Override
    public void reset() {
        nativereset();
    }

    //@Override
    private native void nativereset();

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
