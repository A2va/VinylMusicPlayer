package com.poupa.vinylmusicplayer.service;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;

public class MediaPlayerFFmpeg extends android.media.MediaPlayer {
    private native void init();

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

    private native void nativereset();

    @Override
    public void setDataSource(String path){}

    @Override
    public void prepare(){

    }

    private native void alloc();

    @Override
    public void setDataSource(FileDescriptor fd, long offset, long length)
            throws IOException, IllegalArgumentException, IllegalStateException {
        //_setDataSource(fd, offset, length);
        // Set data source file descriptor
        // See: https://github.com/wseemann/FFmpegMediaMetadataRetriever/blob/6e3cb99b638205698139ea8f0267023935c9b4b0/native/src/main/jni/metadata/wseemann_media_MediaMetadataRetriever.cpp#L218
        // adn: https://github.com/wseemann/FFmpegMediaMetadataRetriever/blob/6e3cb99b638205698139ea8f0267023935c9b4b0/native/src/main/jni/metadata/ffmpeg_mediametadataretriever.c#L304
    }

    static {
        // Load ffmpeg and native library
        System.loadLibrary("avutil");
        System.loadLibrary("avcodec");
        System.loadLibrary("avformat");
        System.loadLibrary("swscale");
        System.loadLibrary("swresample");
        System.loadLibrary("media-player");

        alloc(); // TODO: see how Android MediaPlayer alloc the memory class in C++
    }
}
