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

    public static final String TAG = MediaPlayerVLC.class.getSimpleName();
    /**
     * Interface definition of a callback to be invoked when there
     * has been an error during decoding.
     */
    public interface OnErrorListener{
        /**
         * Called to indicate an error.
         *
         * @param mp the MediaPlayer who cause the error
         * @param what the type of error has occurred
         * @param extra an extra error code
         * @return True if the method handled error
         * */
        boolean onError(MediaPlayerVLC mp, int what, int extra);
    }
    /**
     * Interface definition for a callback to be invoked when playback of
     * a media source has completed.
     */
    public interface OnCompletionListener{
        /**
         * Called when the end of a media source is reached during playback.
         *
         * @param mp the MediaPlayer that reached the end of the file
         * */
        void onCompletion(MediaPlayerVLC mp);
    }

    // VLC lib object
    private LibVLC mLibVLC = null; // LibVLC context
    private MediaPlayer mMediaPlayer = null; // VLC MediaPlayer
    private Media mMedia; // A media

    // Listener
    private OnErrorListener mOnErrorListener;
    private OnCompletionListener mOnCompletionListener;


    private Context mContext; // The context of the app
    private boolean mIsInitialized;

    private String mfilePath = ""; // Actual path file

     //TODO setNextMediaPlayer

    /**
     * Pass the application context to the player.
     * Needed to play file.
     *
     * @param context
     * */
    void setContext(Context context){
        mContext = context;
    }

    /**
     * Called when event occurred in MediaPlayer.
     *
     * @param event
     * */
    private void onEvent(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaPlayer.Event.EncounteredError: // Error event
                if(mOnErrorListener != null){
                    // Call the listener
                    // what and extra are now unused
                    mOnErrorListener.onError(this,0,0);
                }
                break;
            case MediaPlayer.Event.EndReached: // End of file event
                // Call the listener
                if(mOnCompletionListener != null){
                    mOnCompletionListener.onCompletion(this);
                }
                break;
            default:
                break;
        }

    }
    /**
     * If the player has not starting, start from the beginning
     * else start at the current position.
     * */
    public void start(){
        // TODO
        if(mIsInitialized){
            /*if(mfilePath != null && mfile == null){
                setDataSource(mfilePath);
            }
            else if(mfile !=null && mfilePath == null){
                setDataSource(mContext,mfile);
            }*/

            //mMediaPlayer.setMedia(mMedia);
            //mMediaPlayer.setTime(mlast_position);
            mMediaPlayer.play();
        }
    }

    /**
     * Play the current file.
     * */
    public void play(){
        mMediaPlayer.play();
    }

    /**
     * Pause the current file.
     * */
    public void pause(){
        mMediaPlayer.pause();
    }

    /**
     * Seeks to specified time position.
     *
     * @param msec the offset in milliseconds from the start to seek to
     * @throws IllegalStateException if the internal player engine has not been
     * initialized
     * */
    public void seekTo(long msec) throws IllegalStateException{
         if(!mIsInitialized){
             final String msg = "No context has been setted, MediaPlayer is null";
             throw new IllegalStateException(msg);
         }
         if(mMediaPlayer.setTime(msec) == -1){
             final String msg = "No media has been setted";
             throw new IllegalStateException(msg);
         }
    }

    /**
     * Resets the MediaPlayer to its uninitialized state. After calling
     * this method, you will have to initialize it again by setting the
     * data source and calling prepare().
     * */
    public void reset(){
        if(mMedia != null) {
            mMedia.release();
            mMedia = null;
            mfilePath = null;
            mMediaPlayer.stop();
        }
    }
    /**
     * Releases resources associated with this MediaPlayer object.
     * It is considered good practice to call this method when you're
     * done using the MediaPlayer.
     * */
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

    /**
     * Prepare the MediaPlayer. Needed to call setDataSource before.
     * */
    public void prepare(){
        // Argument for the LibVLC
        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv"); // Verbose log LibVLC
        mLibVLC = new LibVLC(mContext,args);
        mMediaPlayer = new MediaPlayer(mLibVLC);
        mMediaPlayer.setEventListener(this::onEvent);

        // Get the media from the file path
        mMedia = getMedia(mfilePath);
        if(mMedia != null){
            // Set the media to the media player
            mMediaPlayer.setMedia(mMedia);
            mIsInitialized = true;
        }


    }

    /**
     * Register a callback to be invoked when an error has happened
     * during decoding.
     *
     * @param listener the callback that will be run
     * */
    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    /**
     * Register a callback to be invoked when playback of
     * a media source has completed.
     *
     * @param listener the callback that will be run
     * */
    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    /**
     * Set file path can be uri string as well.
     *
     * @param path
     * */
    public void setDataSource(String path){
        mfilePath = path;
    }

    /**
     * Set the file with an uri.
     * The context are not necessary it's here for the compatibility because the
     * {@link android.media.MediaPlayer} have it.
     * @param context the application context
     * @param uri the file uri
     * */
    public void setDataSource(Context context, Uri uri){
        // Convert the uri to a string
        setDataSource(uri.toString());
    }

    /*
    * Get a media object from a path.
    *
    * @return media from file path
    * */
    private Media getMedia(String path){
        if (path.startsWith("content://")) {
            ContentResolver contentResolver = mContext.getContentResolver();
            try {
                AssetFileDescriptor file = contentResolver.openAssetFileDescriptor(Uri.parse(path), "r");
                Media media = new Media(mLibVLC, file);
                return media;
            }catch (Exception ex){
                Log.e("t",ex.getMessage());
            }
        } else {
            Media media = new Media(mLibVLC, path);
            return media;
        }
        return  null;
    }

    /**
     * Returns the duration of the media.
     *
     * @return the duration of the media in ms. -1 if there is no media.
     * */
    public int getDuration(){
        return (int)mMediaPlayer.getLength();
    }

    /**
     * Returns the current position of the media.
     *
     * @return the urrent position of the media in ms. -1 if there is no media.
     * */
    public int getCurrentPosition(){
        return (int)mMediaPlayer.getTime();
    }

    /**
     * Sets the volume on this player.
     *
     * Again for the compatibility with {@link android.media.MediaPlayer} the left and right volume
     * are float but only the left are used.
     *
     * @param leftVolume
     * @param rightVolume
     * */
    public void setVolume(float leftVolume, float rightVolume){
        mMediaPlayer.setVolume((int)(leftVolume*100));
    }

    /**
     * Checks whether the MediaPlayer is playing.
     *
     * @return true if currently playing, false otherwise
     */
    public boolean isPlaying(){
        return mMediaPlayer.isPlaying();
    }
}
