
#include "AudioPlayer.h"
#include <unistd.h>

void _playCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
    AudioPlayer *player = (AudioPlayer *)context;
    AVFrame *frame = player->get();
    if (frame)
    {
        int size = av_samples_get_buffer_size(NULL, player->out_ch_layout_nb, frame->nb_samples,
                                              player->out_sample_fmt, 1);
        if (size > 0)
        {
            uint8_t *outBuffer = (uint8_t *)av_malloc(player->max_audio_frame_size);
            swr_convert(player->swr_ctx, &outBuffer, player->max_audio_frame_size,
                        (const uint8_t **)frame->data, frame->nb_samples);
            (*bq)->Enqueue(bq, outBuffer, size);
        }
    }
}

void *_decodeAudio(void *args)
{
    AudioPlayer *p = (AudioPlayer *)args;
    p->decodeAudio();
    pthread_exit(0);
}

void *_play(void *args)
{
    AudioPlayer *p = (AudioPlayer *)args;
    p->setPlaying();
    pthread_exit(0);
}

AudioPlayer::init(const char *path)
{
    initCodecs(path);
}

AudioPlayer::AudioPlayer()
{
    pthread_mutex_init(&mutex, NULL);
    pthread_cond_init(&not_full, NULL);
    pthread_cond_init(&not_empty, NULL);

    initSwrContext();
    createPlayer();
}

/**
 * 将AVFrame Join the queue, when the queue length is 5, block waiting
 * @param frame
 * @return
 */
int AudioPlayer::put(AVFrame *frame)
{
    AVFrame *out = av_frame_alloc();
    if (av_frame_ref(out, frame) < 0)
        return -1;
    pthread_mutex_lock(&mutex);
    if (queue.size() == 5)
    {
        LOGI("Queue is full,wait for put frame:%d", queue.size());
        pthread_cond_wait(&not_full, &mutex);
    }
    queue.push_back(out);
    pthread_cond_signal(&not_empty);
    pthread_mutex_unlock(&mutex);
    return 1;
}

/**
 * Take out AVFrame，When the queue is empty, block waiting
 * @return
 */
AVFrame *AudioPlayer::get()
{
    AVFrame *out = av_frame_alloc();
    pthread_mutex_lock(&mutex);
    while (isPlay)
    {
        if (queue.empty())
        {
            pthread_cond_wait(&not_empty, &mutex);
        }
        else
        {
            AVFrame *src = queue.front();
            if (av_frame_ref(out, src) < 0)
                return NULL;
            queue.erase(queue.begin()); //Delete the removed element
            av_free(src);
            if (queue.size() < 5)
                pthread_cond_signal(&not_full);
            pthread_mutex_unlock(&mutex);
            // Get the current time in msec
            current_time = av_rescale(out->pts,time_base.num,time_base.den);
            current_time *= 1000;
            LOGI("Get frame:%d,time:%d", queue.size(), current_time);
            return out;
        }
    }
    pthread_mutex_unlock(&mutex);
    return NULL;
}

void AudioPlayer::decodeAudio()
{
    LOGI("Start decode...");
    AVFrame *frame = av_frame_alloc();
    AVPacket *packet = av_packet_alloc();
    int ret;

    while (isPlay)
    {

        if (av_read_frame(fmt_ctx, packet) < 0)
        {
            break;
        }

        // If the packet is on another stream continue
        if (packet->stream_index != stream_index)
        {
            continue;
        }
        // Send packet to the decoder
        avcodec_send_packet(codec_ctx, packet);
        // Reveive the frame
        ret = avcodec_receive_frame(codec_ctx, frame);
        // A frame can be split across several packets, so continue reading in this case
        if (ret == AVERROR(EAGAIN))
        {
            continue;
        }

        // And if there is any error
        if (ret >= 0)
        {
            put(frame);
        }
    }

    LOGE("Error with decoding or end of file");
    av_packet_unref(packet);
    av_frame_unref(frame);
    isPlay = 0;

    //release();
}

void AudioPlayer::setPlaying()
{
    //Set playback status
    (*playItf)->SetPlayState(playItf, SL_PLAYSTATE_PLAYING);
    _playCallback(bufferQueueItf, this);
}

void AudioPlayer::play()
{
    LOGI("Play...");
    if (isPlay)
    {
        (*playItf)->SetPlayState(playItf, SL_PLAYSTATE_PLAYING);
        return;
    }
    isPlay = 1;
    seek(0);
    pthread_create(&decodeId, NULL, _decodeAudio, this);
    pthread_create(&playId, NULL, _play, this);
}

void AudioPlayer::pause()
{
    (*playItf)->SetPlayState(playItf, SL_PLAYSTATE_PAUSED);
}

int AudioPlayer::createPlayer()
{
    //Create player
    //Create and initialize the engine object
    //    SLObjectItf engineObject;
    slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    //获取引擎接口
    //    SLEngineItf engineItf;
    (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineItf);
    //Get output mix through engine interface
    //    SLObjectItf mixObject;
    (*engineItf)->CreateOutputMix(engineItf, &mixObject, 0, 0, 0);
    (*mixObject)->Realize(mixObject, SL_BOOLEAN_FALSE);

    //Set player parameters
    SLDataLocator_AndroidSimpleBufferQueue
        android_queue = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    SLuint32 samplesPerSec = (SLuint32)out_sample_rate * 1000;
    //pcm format
    SLDataFormat_PCM pcm = {SL_DATAFORMAT_PCM,
                            2, //两声道
                            samplesPerSec,
                            SL_PCMSAMPLEFORMAT_FIXED_16,
                            SL_PCMSAMPLEFORMAT_FIXED_16,
                            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT, //
                            SL_BYTEORDER_LITTLEENDIAN};

    SLDataSource slDataSource = {&android_queue, &pcm};

    //Output pipeline
    SLDataLocator_OutputMix outputMix = {SL_DATALOCATOR_OUTPUTMIX, mixObject};
    SLDataSink audioSnk = {&outputMix, NULL};
    const SLInterfaceID ids[3] = {SL_IID_BUFFERQUEUE, SL_IID_EFFECTSEND, SL_IID_VOLUME};
    const SLboolean req[3] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
    //Through the engine interface, create and initialize the player object
    //    SLObjectItf playerObject;
    (*engineItf)->CreateAudioPlayer(engineItf, &playerObject, &slDataSource, &audioSnk, 1, ids, req);
    (*playerObject)->Realize(playerObject, SL_BOOLEAN_FALSE);

    //Get the playback interface
    //    SLPlayItf playItf;
    (*playerObject)->GetInterface(playerObject, SL_IID_PLAY, &playItf);
    //Get buffer interface
    //    SLAndroidSimpleBufferQueueItf bufferQueueItf;
    (*playerObject)->GetInterface(playerObject, SL_IID_BUFFERQUEUE, &bufferQueueItf);

    //Register buffer callback
    (*bufferQueueItf)->RegisterCallback(bufferQueueItf, _playCallback, this);

    // Get Volume interface
    (*playerObject)->GetInterface(playerObject,SL_IID_VOLUME,&volumeItf);

    return 1;
}

int AudioPlayer::initSwrContext()
{
    LOGI("Init swr context");
    swr_ctx = swr_alloc();
    out_sample_fmt = AV_SAMPLE_FMT_S16;
    out_ch_layout = AV_CH_LAYOUT_STEREO;
    out_ch_layout_nb = 2;
    out_sample_rate = in_sample_rate;
    max_audio_frame_size = out_sample_rate * 2;

    swr_alloc_set_opts(swr_ctx, out_ch_layout, out_sample_fmt, out_sample_rate, in_ch_layout,
                       in_sample_fmt, in_sample_rate, 0, NULL);
    if (swr_init(swr_ctx) < 0)
    {
        LOGE("Error init SwrContext");
        return -1;
    }
    return 1;
}

int AudioPlayer::initCodecs(const char *path)
{
    LOGI("Init codecs");

    int ret = 0;
    fmt_ctx = avformat_alloc_context();

    ret = avformat_open_input(&fmt_ctx, path, NULL, NULL);
    if (ret < 0)
    { //Open a file
        LOGE("Could not open file:%s", path);
        char err[128];
        av_strerror(ret, err, 128);
        LOGE("Error %s", err);
        return -1;
    }

    if (avformat_find_stream_info(fmt_ctx, NULL) < 0)
    {
        LOGE("Find stream info error");
        return -1;
    }
    for (int i = 0; i < fmt_ctx->nb_streams; i++)
    {
        if (fmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO)
        {
            stream_index = i;
            LOGI("Find audio stream index:%d", stream_index);
            break;
        }
    }

    if (stream_index < 0)
    {
        LOGE("Error find stream index");
        return -1;
    }

    codec_ctx = avcodec_alloc_context3(NULL);

    stream = fmt_ctx->streams[stream_index];
    avcodec_parameters_to_context(codec_ctx, fmt_ctx->streams[stream_index]->codecpar);
    codec = avcodec_find_decoder(codec_ctx->codec_id);

    in_sample_fmt = codec_ctx->sample_fmt;
    in_ch_layout = codec_ctx->channel_layout;
    in_sample_rate = codec_ctx->sample_rate;
    in_ch_layout_nb = av_get_channel_layout_nb_channels(in_ch_layout);
    max_audio_frame_size = in_sample_rate * in_ch_layout_nb;
    time_base = fmt_ctx->streams[stream_index]->time_base;
    int64_t duration = stream->duration;

    // Get the total time in msec
    total_time = av_rescale(duration,time_base.num,time_base.den);
    total_time *= 1000;
    LOGI("Total time:%d", total_time);

    if (avcodec_open2(codec_ctx, codec, NULL) < 0)
    {
        LOGE("Could not open codec");
        return -1;
    }
    return 1;
}

void AudioPlayer::seek(jint msecs)
{
    pthread_mutex_lock(&mutex);
    int64_t timestamp = av_rescale(msecs,time_base.num,time_base.den);
    timestamp /= 1000;
    av_seek_frame(fmt_ctx, stream_index, timestamp, AVSEEK_FLAG_ANY);

    current_time = msecs;
    queue.clear();
    pthread_cond_signal(&not_full);
    pthread_mutex_unlock(&mutex);
}

void AudioPlayer::release()
{
    pthread_mutex_lock(&mutex);
    isPlay = 0;
    pthread_cond_signal(&not_full);
    pthread_mutex_unlock(&mutex);
    if (playItf)
        (*playItf)->SetPlayState(playItf, SL_PLAYSTATE_STOPPED);
    if (playerObject)
    {
        (*playerObject)->Destroy(playerObject);
        playerObject = 0;
        bufferQueueItf = 0;
    }
    if (mixObject)
    {
        (*mixObject)->Destroy(mixObject);
        mixObject = 0;
    }
    if (engineObject)
    {
        (*engineObject)->Destroy(engineObject);
        engineItf = 0;
    }
    if (swr_ctx)
    {
        swr_free(&swr_ctx);
    }

    avcodec_close(codec_ctx);
    avformat_close_input(&fmt_ctx);
    LOGI("Release...");
}
void AudioPlayer::setVolume(float volume)
{
    // volume in float are 0.0 to 1.0 value
    SLmillibel  maxVolume;
    (*volumeItf)->GetMaxVolumeLevel(volumeItf, &maxVolume);
    (*volumeItf)->SetVolumeLevel(volumeItf, static_cast<SLmillibel>(volume*maxVolume));

}

AudioPlayer::setDataSource(jstring jpath)
{
    const char *path = env->GetStringUTFChars(jpath, nullptr);
    initCodecs(path);
    env->ReleaseStringUTFChars(path, jpath);

}

//TODO
// Headers implementation https://android.googlesource.com/platform/frameworks/base/+/56a2301/media/jni/android_media_MediaPlayer.cpp#179
AudioPlayer::setDataSource(const char *srcUrl, const char *headers)
{


}
AudioPlayer::setDataSource(int fd, int64_t offset, int64_t length)
{

}
