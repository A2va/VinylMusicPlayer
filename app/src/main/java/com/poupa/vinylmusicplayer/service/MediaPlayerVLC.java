package com.poupa.vinylmusicplayer.service;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.LibVLC;


import android.content.Context;

import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnCompletionListener;

public class MediaPlayerVLC extends android.media.MediaPlayer{
    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;
    private Media mMedia;

    private OnErrorListener mOnErrorListener;
    private OnCompletionListener mOnCompletionListener;


    private Context mContext;
    private boolean mIsInitialized;

    /*
     TODO
     Method to create:
     release() : ok
     setWakeMode(): normally ok because extends
     reset(): Todo
     setOnPreparedListener(): normally ok
     setDataSource(String path): todo
     setDataSource(context and uri): todo
     setNextMediaPlayer(MediaPlayer): todo
     start(): todo
     reset(): todo
     setAudioStreamType(): no
     prepare(): normally no
     setOnCompletionListener(): ok
     setOnErrorListener(): ok

    */

    // work like a init method
    void setContext(Context context){
        mContext = context;
        // create libvlc and a mediaplayer object from context
        mLibVLC = new LibVLC(context);
        mMediaPlayer = new MediaPlayer(mLibVLC);
        mMediaPlayer.setEventListener(new MediaPlayer.EventListener() {
            @Override
            public void onEvent(MediaPlayer.Event event) {
                switch (event.type) {

                    case MediaPlayer.Event.EncounteredError:
                        onError();
                    case MediaPlayer.Event.EndReached:
                        onComplete();
                        break;
                }

            }
        });
        mIsInitialized = true;
    }

    private void onError(MediaPlayer.Event event){
        if(mOnErrorListener != null){
         mOnErrorListener.onError(this,0,0);
        }
    }

    private void onComplete(){
        if(mOnCompletionListener != null){
            mOnCompletionListener.onCompletion(this);
        }
        // Code from android MediaPlayeer
        //mOnCompletionInternalListener.onCompletion(mMediaPlayer);
        OnCompletionListener onCompletionListener = mOnCompletionListener;
        if (onCompletionListener != null)
            onCompletionListener.onCompletion(this);

    }

    public void seekTo(long msec) throws IllegalStateException{
         if(mIsInitialized){
             final String msg = "No context has been setted, MediaPlayer is null";
             throw new IllegalStateException(msg);
         }
         if(mMediaPlayer.setTime(msec) == -1){
             final String msg = "No media has been setted";
             throw new IllegalStateException(msg);
         }
    }

    public void release() {

        if(mIsInitialized) {
            mMediaPlayer.release();
            mMedia.release();
            mLibVLC.release();
        }

        mMediaPlayer = null;
        mMedia = null;

        mOnCompletionListener = null;
        mOnErrorListener = null;

    }

    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    public void setDataSource(String path){
        mMedia = new Media(mLibVLC, path);
        mMediaPlayer.setMedia(mMedia);

        mMediaPlayer.setEventListener();

    }
}
