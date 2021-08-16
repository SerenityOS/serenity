#include <jni.h>

#ifndef RandomGen
#define RandomGen
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Java_RandomGen_getRandom
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
