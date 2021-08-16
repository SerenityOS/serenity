#include "jni.h"
JNIEXPORT void JNICALL JavaCritical_compiler_runtime_criticalnatives_argumentcorruption_CheckLongArgs_m1
  (jlong a1, jlong a2, jlong a3, jlong a4, jlong a5, jlong a6, jlong a7, jlong a8,jint result_length,jbyte* result) {

  if (a1 != 1111111122222222LL || a2 != 3333333344444444LL || a3 != 5555555566666666LL || a4 != 7777777788888888LL ||
      a5 != 9999999900000000LL || a6 != 1212121234343434LL || a7 != 5656565678787878LL || a8 != 9090909012121212LL ||
      result_length != 1 || result[0] != -1) {
    result[0] = 1;
  } else {
    result[0] = 2;
  }
}

JNIEXPORT void JNICALL JavaCritical_compiler_runtime_criticalnatives_argumentcorruption_CheckLongArgs_m2
  (jlong a1, jint a2_length, jint* a2, jlong a3, jint a4_length, jint* a4, jlong a5, jint a6_length, jint* a6, jlong a7,
   jint a8_length, jint* a8, jlong a9, jint result_length, jbyte* result) {
  if (a1 != 1111111122222222LL || a2_length != 3 || a2[0] != 1111 || a3 != 3333333344444444LL || a4_length != 3 || a4[0] != 4444 ||
      a5 != 5555555566666666LL || a6_length != 3 || a6[0] != 7777 || a7 != 7777777788888888LL || a8_length != 3 || a8[0] != 1010 || a9 != 9999999900000000LL ||
      result_length != 1 || result[0] != -1) {
    result[0] = 1;
  } else {
    result[0] = 2;
  }
}

JNIEXPORT void JNICALL Java_compiler_runtime_criticalnatives_argumentcorruption_CheckLongArgs_m1
  (JNIEnv * env, jclass jclazz, jlong a3, jlong a4, jlong a5, jlong a6, jlong a7, jlong a8, jlong a9, jlong a10, jbyteArray result) {}

JNIEXPORT void JNICALL Java_compiler_runtime_criticalnatives_argumentcorruption_CheckLongArgs_m2
  (JNIEnv * env, jclass jclazz, jlong a3, jintArray a4, jlong a5, jintArray a6, jlong a7, jintArray a8, jlong a9, jintArray a10, jlong a11, jbyteArray result) {}
