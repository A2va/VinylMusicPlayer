//
// Created by yanxx on 2019/1/23.
//
#include "AudioPlayer.h"
#include <jni.h>
#include <binder_parcel.h>
//#include <binder/Parcel.h>
#ifndef NELEM
# define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
#endif


static AudioPlayer *player;

// Native code of MediaPlayer : https://android.googlesource.com/platform/frameworks/base/+/56a2301/media/jni/android_media_MediaPlayer.cpp

/*
 * Register several native methods for one class.
 */
// Use Jni Load for register method

static int registerNativeMethods(JNIEnv* env, const char* className,
								 JNINativeMethod* gMethods, int numMethods)
{
  jclass clazz;
  clazz = env->FindClass(className);
  if (clazz == NULL) {
	LOGE("Native registration unable to find class '%s'", className);
	return JNI_FALSE;
  }
  if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
	LOGE("RegisterNatives failed for '%s'", className);
	return JNI_FALSE;
  }
  return JNI_TRUE;
}

extern "C" JNIEXPORT void
JNICALL
Java_com_poupa_vinylmusicplayer_service_MediaPlayerFFmpeg_alloc(
    JNIEnv *env,
    jobject thiz)
{
    player = new AudioPlayer();
    player->setEnv(env);
    player->setObject(thiz);
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
    delete player;
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

extern "C" JNIEXPORT void
JNICALL
Java_com_poupa_vinylmusicplayer_service_MediaPlayerFFmpeg_nativereset(
    JNIEnv *env,
    jobject /* this */)
{
    player->release();

}


