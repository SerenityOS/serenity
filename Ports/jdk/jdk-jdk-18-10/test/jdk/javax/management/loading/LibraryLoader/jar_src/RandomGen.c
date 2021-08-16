#include <jni.h>
#include <stdio.h>
#include <stdlib.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_UseNativeLib_getRandom(JNIEnv *env, jobject obj) {
        return rand();
}

#ifdef __cplusplus
}
#endif
