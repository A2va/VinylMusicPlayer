
#ifndef FFMPEGAUDIOPLAYER_H
#define FFMPEGAUDIOPLAYER_H

#include <vector>
#include <SLES/OpenSLES_Android.h>
#include <android/log.h>
#include <jni.h>

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libswresample/swresample.h>
#include <pthread.h>
#define LOGI(FORMAT, ...) __android_log_print(ANDROID_LOG_INFO,"FFmpegAudioPlayer",FORMAT,##__VA_ARGS__);
#define LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR,"FFmpegAudioPlayer",FORMAT,##__VA_ARGS__);

class AudioPlayer {
public:
    AudioPlayer(const char *path);

    jobject jobj;
    JNIEnv *env;
//Decoding
    AVFormatContext *fmt_ctx = nullptr;  //FFmpeg context
    AVCodecContext *codec_ctx = nullptr; //Decoder context
    AVStream  *stream = nullptr;
    AVCodec *codec = nullptr;
    int stream_index;          //Audio stream index


//AVFrame queue
    std::vector<AVFrame *> queue;   //Queue, used to save the AVFrame after decoding and filtering

//Input and output format
    SwrContext *swr_ctx;            //Re-sampling，Used to convert AVFrame into pcm data
    uint64_t in_ch_layout;
    int in_sample_rate;            //Sampling Rate
    int in_ch_layout_nb;           //Number of input channels, used with swr_ctx
    enum AVSampleFormat in_sample_fmt; //Input audio sample format

    uint64_t out_ch_layout;
    int out_sample_rate;            //Sampling Rate
    int out_ch_layout_nb;           //Number of output channels, used with swr_ctx
    int max_audio_frame_size;       //Maximum buffer data size
    enum AVSampleFormat out_sample_fmt; //Output audio sample format

// Progress related
    AVRational time_base;           //Scale, used to calculate progress
    double total_time;              //Total time (seconds)
    double current_time = 0;          //Current progress
    int isPlay = 0;                 //Playing status 1: Playing

//Multithreading
    pthread_t decodeId;             //Decoding thread id
    pthread_t playId;               //Play thread id
    pthread_mutex_t mutex;          //Sync lock
    pthread_cond_t not_full;        //Not a full condition, used when producing AVFrame
    pthread_cond_t not_empty;       //Not empty condition, used when consuming AVFrame
//Open SL ES
    SLObjectItf engineObject;       //Engine object
    SLEngineItf engineItf;          //Engine interface
    SLObjectItf mixObject;          //Output mixing object
    SLObjectItf playerObject;       //player object
    SLPlayItf playItf;              //Player interface
    SLVolumeItf volumeItf;          //Volume interface
    SLAndroidSimpleBufferQueueItf bufferQueueItf;   //Buffer interface

    void play();

    void pause();

    void setPlaying();

    void decodeAudio();                     //Decode audio

    int createPlayer();                     //Create player
    int initCodecs(const char *path);         //Initialize the decoder
    int initSwrContext();                   //Initialize SwrContext

    AVFrame *get();

    int put(AVFrame *frame);

    void seek(double secs);

    void release();

    void setVolume(float volume);

};

}

#endif //FFMPEGAUDIOPLAYER_H
