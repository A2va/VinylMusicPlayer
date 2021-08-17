package com.poupa.vinylmusicplayer.service;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.MediaFactory;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;

//import android.media.MediaPlayer.OnErrorListener;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;

public class MediaPlayerVLC{
    public interface OnErrorListener{
        boolean onError(MediaPlayerVLC mp, int what, int extra);
    }
    public interface OnCompletionListener{
        void onCompletion(MediaPlayerVLC mp);
    }

    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;
    private Media mMedia;
    private boolean misPaused = false;

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
        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");

        mLibVLC = new LibVLC(context,args);
        mMediaPlayer = new MediaPlayer(mLibVLC);
        mMediaPlayer.setEventListener(this::onEvent);

        mIsInitialized = true;
    }

    public void onEvent(MediaPlayer.Event event) {
        switch (event.type) {

            case MediaPlayer.Event.EncounteredError:
                onError(this,0,0);
                break;
            case MediaPlayer.Event.EndReached:
                onComplete(this);
                break;
            case MediaPlayer.Event.PausableChanged:
                misPaused = !misPaused;

        }

    }
    public void onError(MediaPlayerVLC mp, int i, int i2){
        if(mOnErrorListener != null){
         mOnErrorListener.onError(mp,i,i2);
        }
    }

    private void onComplete(MediaPlayerVLC mp){
        if(mOnCompletionListener != null){
            mOnCompletionListener.onCompletion(mp);
        }
    }

    public void start(){
        if(mIsInitialized){
            mMediaPlayer.play();
        }
    }

    public void play(){
        mMediaPlayer.play();
    }

    public void pause(){
        mMediaPlayer.pause();
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

    public void reset(){
        if(mMedia != null) {
            mMedia.release();
            mMedia = null;
        }
    }

    public void release() {

        if(mIsInitialized) {
            mMediaPlayer.release();
            if(mMedia != null) {
                mMedia.release();
            }
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
    }

    public void setDataSource(Context context, Uri uri){

        ContentResolver contentResolver = context.getContentResolver();
        try {
            AssetFileDescriptor file = contentResolver.openAssetFileDescriptor(uri, "r");
            mMedia = new Media(mLibVLC, file);
            mMediaPlayer.setMedia(mMedia);
            mMediaPlayer.play();
        }catch (Exception ex){
            Log.e("t",ex.getMessage());
        }
    }

    public int getDuration(){
        return (int)mMediaPlayer.getLength();
    }

    public int getCurrentPosition(){
        return (int)mMediaPlayer.getTime();
    }

    public void setVolume(float leftVolume, float rightVolume){
        mMediaPlayer.setVolume((int)(leftVolume*100));
    }

    public boolean isPlaying(){
        return mMediaPlayer.isPlaying();
    }
}
