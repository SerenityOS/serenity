#include "jni.h"
JNIEXPORT void JNICALL JavaCritical_compiler_runtime_criticalnatives_lookup_LookUp_m1
  (jbyte a1, jlong a2, jchar a3, jint a4, jfloat a5, jdouble a6, jint result_length, jbyte* result) {
  jint l1 = (jint) a5;
  jlong l2 = (jlong) a6;

  if (a1 != 0xA || a2 != 4444444455555555LL || a3 != 0x41 || a4 != 12345678 || l1 != 343434 || l2 != 6666666677777777LL ||
      result_length != 1 || result[0] != -1) {
    result[0] = 1;
  } else {
    result[0] = 2;
  }
}

JNIEXPORT void JNICALL JavaCritical_compiler_runtime_criticalnatives_lookup_LookUp_m2
  (jint a1, jint a2_length, jint* a2, jlong a3, jint a4_length, jlong* a4, jfloat a5, jint a6_length, jfloat* a6, jdouble a7,
   jint a8_length, jdouble* a8, jint result_length, jbyte* result) {
  jint l1 = (jint) a5;
  jlong l2 = (jlong) a7;

  if (a1 != 12345678 || a2_length != 3 || a2[0] != 1111 || a3 != 4444444455555555LL || a4_length != 3 || a4[0] != 4444 ||
      l1 != 343434 ||  a6_length != 3 ||  7777 != (jint)a6[0] || l2 != 6666666677777777LL || a8_length != 3 || 4545 != (jlong)a8[0] ||
      result_length != 1 || result[0] != -1) {
    result[0] = 1;
  } else {
    result[0] = 2;
  }
}

JNIEXPORT void JNICALL Java_compiler_runtime_criticalnatives_lookup_LookUp_m1
  (JNIEnv * env, jclass jclazz, jbyte a3, jlong a4, jchar a5, jint a6, jfloat a7, jdouble a8, jbyteArray result) {}

JNIEXPORT void JNICALL Java_compiler_runtime_criticalnatives_lookup_LookUp_m2
  (JNIEnv * env, jclass jclazz, jint a3, jintArray a4, jlong a5, jlongArray a6, jfloat a7, jfloatArray a8, jdouble a9, jdoubleArray a10, jbyteArray result) {}

