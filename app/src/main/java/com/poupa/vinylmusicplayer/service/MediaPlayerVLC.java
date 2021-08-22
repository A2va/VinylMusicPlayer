package com.poupa.vinylmusicplayer.service;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.util.HWDecoderUtil;
import org.videolan.libvlc.util.HWDecoderUtil.AudioOutput;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;

public class MediaPlayerVLC {

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

    private AudioOutput mAout = AudioOutput.AUDIOTRACK;

    // Listener
    private OnErrorListener mOnErrorListener;
    private OnCompletionListener mOnCompletionListener;


    private Context mContext; // The context of the app

    private String mfilePath = ""; // Actual path file

    private int mCurrentpos; // in msec

    private MediaPlayerVLC mNextMediaPlayer = null;

    /**
     * Pass the application context to the player.
     * Needed to play file.
     *
     * @param context
     * */
    void setContext(Context context){
        mContext = context;
    }

    public void setAudioOutput(AudioOutput aout){
        mAout = aout;
        if(mAout == AudioOutput.OPENSLES){
            mMediaPlayer.setAudioOutput("opensles_android");
            return;
        }
        mMediaPlayer.setAudioOutput("android_audiotrack");
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
                // Start the next media player
                if(mNextMediaPlayer != null){
                    mNextMediaPlayer.start();
                }
                // Call the listener
                if(mOnCompletionListener != null){
                    mOnCompletionListener.onCompletion(this);
                }
                break;
            case MediaPlayer.Event.PositionChanged:
                mCurrentpos = (int)mMediaPlayer.getTime(); // Track the current position
                break;
            default:
                break;
        }

    }

    /**
     * If the player has not starting, start from the beginning
     * else start at the current position.
     * */
    public void start() throws IllegalStateException {
        if(mMediaPlayer == null){
            final String msg = "Need to call prepare, MediaPlayer is null";
            throw new IllegalStateException(msg);
        }
        if(!mMediaPlayer.isPlaying()){
            mMediaPlayer.play();
        }
    }

    /**
     * Play the current file.
     * */
    public void play() throws IllegalStateException {
        if(mMediaPlayer == null){
            final String msg = "Need to call prepare, MediaPlayer is null";
            throw new IllegalStateException(msg);
        }
        mMediaPlayer.play();
    }

    /**
     * Pause the current file.
     * */
    public void pause() throws IllegalStateException {
        if(mMediaPlayer == null){
            final String msg = "Need to call prepare, MediaPlayer is null";
            throw new IllegalStateException(msg);
        }
        mMediaPlayer.pause();
    }

    /**
     * Resets the MediaPlayer to its uninitialized state. After calling
     * this method, you will have to initialize it again by setting the
     * data source and calling prepare().
     * */
    public void reset() {
        if(mMedia != null) {
            mMediaPlayer.stop();
            mMedia.release();
            mMedia = null;
            mfilePath = ""; // reset the filename
        }
    }
    /**
     * Releases resources associated with this MediaPlayer object.
     * It is considered good practice to call this method when you're
     * done using the MediaPlayer.
     * */
    public void release() {

        if(mMedia != null) {
            mMedia.release();
            mMedia = null;
        }

        if(mLibVLC != null) {
            mLibVLC.release();
            mLibVLC = null;
        }

        if(mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    /**
     * Prepare the MediaPlayer. Needed to call setDataSource before.
     * */
    public void prepare() {

        // Create LibVLC object if doesn't exist
        if(mLibVLC == null) {
            // Argument for the LibVLC
            final ArrayList<String> args = new ArrayList<>();
            // All args are documented here:
            // https://wiki.videolan.org/VLC_command-line_help/
            args.add("-vvv"); // Verbose log LibVLC
            mLibVLC = new LibVLC(mContext, args);
        }

        if(mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer(mLibVLC);
            mMediaPlayer.setEventListener(this::onEvent);
        }

        // Get the media from the file path
        mMedia = getMedia(mfilePath);
        if(mMedia != null) {
            mMediaPlayer.stop();
            // Set the media to the media player
            mMediaPlayer.setMedia(mMedia);
            // Very hacky to enable seeking
            // Apparently seeking is not functional before playing
            // TODO Maybe post an issue on vlc android gitlab or find an other way
            mMediaPlayer.play();
            mMediaPlayer.pause();
            seekTo(0);
            mCurrentpos = 0;
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
    public void setDataSource(String path) {
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

    /**
     * Set the next media player you want to play.
     * @param mp the next media player
     * */
    public void setNextMediaPlayer(MediaPlayerVLC mp){
        mNextMediaPlayer = mp;
    }

    /*
    * Get a media object from a path.
    *
    * @return media from file path
    * */
    private Media getMedia(String path) {
        if (path.startsWith("content://")) {
            Uri uri = Uri.parse(path);
            ContentResolver contentResolver = mContext.getContentResolver();
            try {
                // Old code
                // When the app is opened for the first time, player is unable to resume the actual
                // music but if the music is changed or resume second time it worked.
                /*AssetFileDescriptor file = contentResolver.openAssetFileDescriptor(Uri.parse(path), "r");
                return  new Media(mLibVLC, file);*/

                // From the VLC android code
                Cursor cursor = contentResolver.query(uri, new String[] {MediaStore.Audio.Media.DATA}, null, null, null);
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);

                // If cursor is not empty
                if (cursor.moveToFirst()){
                    return new Media(mLibVLC, cursor.getString(columnIndex));
                }
                else{
                    // Try the uri with AssetFileDescriptor
                    AssetFileDescriptor file = contentResolver.openAssetFileDescriptor(uri, "r");
                    return new Media(mLibVLC, file);
                }

            }catch (Exception ex){
                ex.printStackTrace();
            }

        } else {
            return new Media(mLibVLC, path);
        }

        return null;
    }

    /**
     * Seeks to specified time position.
     *
     * @param msec the offset in milliseconds from the start to seek to
     * @throws IllegalStateException if the internal player engine has not been
     * initialized
     * */
    public void seekTo(int msec) throws IllegalStateException {
        if(mLibVLC == null || mMediaPlayer == null){
            final String msg = "Need to call prepare, MediaPlayer is null";
            throw new IllegalStateException(msg);
        }
        if(mMediaPlayer.setTime(msec) == -1){
            final String msg = "No media has been setted";
            throw new IllegalStateException(msg);
        }
        mCurrentpos = msec;
    }

    /**
     * Returns the duration of the media.
     *
     * @return the duration of the media in ms. -1 if there is no media.
     * */
    public int getDuration() throws IllegalStateException {
        if(mMediaPlayer == null){
            final String msg = "Need to call prepare, MediaPlayer is null";
            throw new IllegalStateException(msg);
        }
        return (int)mMediaPlayer.getLength();
    }

    /**
     * Returns the current position of the media.
     *
     * @return the urrent position of the media in ms. -1 if there is no media.
     * */
    public int getCurrentPosition() throws IllegalStateException {
        if(mMediaPlayer == null){
            final String msg = "Need to call prepare, MediaPlayer is null";
            throw new IllegalStateException(msg);
        }
        //return (int)mMediaPlayer.getTime();
        return  mCurrentpos;
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
    public void setVolume(float leftVolume, float rightVolume) {
        if(mMediaPlayer == null){
            final String msg = "Need to call prepare, MediaPlayer is null";
            throw new IllegalStateException(msg);
        }
        mMediaPlayer.setVolume((int)(leftVolume*100));
    }

    /**
     * Checks whether the MediaPlayer is playing.
     *
     * @return true if currently playing, false otherwise
     */
    public boolean isPlaying() {
        if(mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }
}
