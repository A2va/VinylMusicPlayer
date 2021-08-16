package com.poupa.vinylmusicplayer.service;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.LibVLC;

import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnCompletionListener;

public class MediaPlayerVLC extends android.media.MediaPlayer {
    private MediaPlayer mMediaPlayer;
    private Media mMedia;
    private LibVLC mLibVLC;

    private OnErrorListener mOnErrorListener;
    private OnCompletionListener mOnCompletionListener;

    /*
     TODO
     Method to create:
     release() : ok
     onTrackWentToNext()
     setWakeMode(): normally ok because extends
     reset(): Todo
     setOnPreparedListener(): normally ok
     setDataSource(String path): todo
     setDataSource(context and uri): todo
     setNextMediaPlayer(MediaPlayer): todo
     start(): maybe
     reset(): maybe
     setAudioStreamType(): no
     prepare(): normally no
     setOnCompletionListener(): todo
     setOnErrorListener(): todo

    */
}
