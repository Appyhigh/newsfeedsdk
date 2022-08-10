#include <jni.h>
#include <string.h>
#include <stdio.h>

JNIEXPORT jstring JNICALL
Java_com_appyhigh_newsfeedsdk_encryption_AuthSocket_nativeKey1(JNIEnv *env, jobject thiz) {

    char value[500];
    char end[10];
    strcpy(value,
           "83b0b108b0b69b0b47b0b96b0b87b0b89b0b107b0b77e0e86c0c85c0c105c0c98c0c108d0c85d0c47d0c84d0c68d0d65d0d121e0e98e0f50e0f98e0f118a0a98a0a108a0a119e0e");
    strcpy(end, "80e0e");
    return (*env)->NewStringUTF(env, strncat(value, end, strlen(value) - 6));
}