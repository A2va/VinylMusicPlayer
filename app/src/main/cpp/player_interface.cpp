//
// Created by yanxx on 2019/1/23.
//
#include "AudioPlayer.h"
#include <jni.h>

static AudioPlayer *player;

extern "C" JNIEXPORT void
    JNICALL
    Java_com_poupa_vinylmusicplayer_service_MediaPlayerFFmpeg_init(
        JNIEnv *env,
        jobject thiz, jstring path)
{

    const char *cFilePath = env->GetStringUTFChars(path, nullptr);

    player = new AudioPlayer(cFilePath);
    player->env = env;
    player->jobj = thiz;

    env->ReleaseStringUTFChars(path, cFilePath);
}

/*extern "C" JNIEXPORT void
JNICALL
Java_io_github_iamyours_ffmpegaudioplayer_FFmpegAudioPlayer_callback(
        JNIEnv *env,
        jobject thiz, jobject callback) {
    jclass callbackClass = env->GetObjectClass(callbackClass);
    jmethodID method = env->GetMethodID(callbackClass, "test", "()V");
    env->CallVoidMethod(callback, method);
}*/

extern "C" JNIEXPORT void
    JNICALL
    Java_com_poupa_vinylmusicplayer_service_MediaPlayerFFmpeg_setVolume(
        JNIEnv *env,
        jobject /* this */, jfloat leftVolume, jfloat rightVolume)
{
    player->setVolume(leftVolume);
}

extern "C" JNIEXPORT void
    JNICALL
    Java_com_poupa_vinylmusicplayer_service_MediaPlayerFFmpeg_play(
        JNIEnv *env,
        jobject /* this */)
{
    player->play();
}

extern "C" JNIEXPORT void
    JNICALL
    Java_com_poupa_vinylmusicplayer_service_MediaPlayerFFmpeg_pause(
        JNIEnv *env,
        jobject /* this */)
{
    player->pause();
}

extern "C" JNIEXPORT void
    JNICALL
    Java_com_poupa_vinylmusicplayer_service_MediaPlayerFFmpeg_release(
        JNIEnv *env,
        jobject /* this */)
{
    player->release();
}
extern "C" JNIEXPORT void
    JNICALL
    Java_com_poupa_vinylmusicplayer_service_MediaPlayerFFmpeg_seekTo(
        JNIEnv *env,
        jobject /* this */, jint msecs)
{
    //TODO Return seek as int in ms
    //player->seek(msecs);
}

extern "C" JNIEXPORT jint
    JNICALL
    Java_com_poupa_vinylmusicplayer_service_MediaPlayerFFmpeg_duration(
        JNIEnv *env,
        jobject /* this */)
{
    return player->duration();
}

extern "C" JNIEXPORT jint
    JNICALL
    Java_com_poupa_vinylmusicplayer_service_MediaPlayerFFmpeg_position(
        JNIEnv *env,
        jobject /* this */)
{
    return player->position();
}
