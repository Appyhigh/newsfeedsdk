#include <jni.h>

JNIEXPORT jstring JNICALL
Java_com_appyhigh_newsfeedsdk_encryption_AuthSocket_nativeKey1(JNIEnv *env, jobject thiz) {
    return (*env)->NewStringUTF(env, "TmF0aXZlNWVjcmV0UEBzc3cwcmQx");
}