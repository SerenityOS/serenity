/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include <jni.h>
#include <stdlib.h>
#include "jnihelper.h"

/*
  basic routine: provide critical sections and calculations
    enter array CS
    enter first string CS
    leave first string CS
    enter second string CS
    leave array CS
    leave second string CS
*/
#define BODY(type)                                                          \
  int hash = 0;                                                             \
  jsize i, arraySize, stringSize;                                           \
  jchar *nativeStr = NULL;                                                  \
  type *nativeArray = NULL;                                                 \
                                                                            \
  arraySize = env->GetArrayLength(array); CE                                \
  stringSize = env->GetStringLength(str); CE                                \
                                                                            \
  nativeArray = (type *)env->GetPrimitiveArrayCritical(array, NULL); CE     \
  qsort(nativeArray, arraySize, sizeof(type), *type##comp);                 \
                                                                            \
  nativeStr = (jchar *)env->GetStringCritical(str, NULL); CE                \
                                                                            \
  for (i = 0; i < stringSize; ++i)                                          \
    hash += (int)nativeStr[i];                                              \
  env->ReleaseStringCritical(str, nativeStr); CE                            \
                                                                            \
  nativeStr = (jchar *)env->GetStringCritical(str, NULL); CE                \
                                                                            \
  env->ReleasePrimitiveArrayCritical(array, nativeArray, 0); CE             \
                                                                            \
  for (i = 0; i < stringSize; ++i)                                          \
    hash += (int)nativeStr[i];                                              \
  env->ReleaseStringCritical(str, nativeStr); CE                            \
                                                                            \
  return hash;

// compare most java primitive value types
#define COMP(type)                                                          \
int type##comp(const void *s1, const void *s2)                              \
{                                                                           \
  type st1 = *((type *)s1);                                                 \
  type st2 = *((type *)s2);                                                 \
  if (st1 < st2)                                                            \
    return -1;                                                              \
  else if (st1 > st2)                                                       \
    return 1;                                                               \
  else                                                                      \
    return 0;                                                               \
}

COMP(jint)
COMP(jboolean)
COMP(jchar)
COMP(jshort)
COMP(jbyte)
COMP(jdouble)
COMP(jfloat)
COMP(jlong)

extern "C" {

/*
 * Class:     JNIWorker
 * Method:    NativeCall
 * Signature: ([ZLjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_nsk_stress_jni_gclocker_JNIWorker_NativeCall___3ZLjava_lang_String_2
  (JNIEnv * env, jobject obj, jbooleanArray array, jstring str)
{
    BODY(jboolean)
}

/*
 * Class:     JNIWorker
 * Method:    NativeCall
 * Signature: ([BLjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_nsk_stress_jni_gclocker_JNIWorker_NativeCall___3BLjava_lang_String_2
  (JNIEnv * env, jobject obj, jbyteArray array, jstring str)
{
    BODY(jbyte)
}

/*
 * Class:     JNIWorker
 * Method:    NativeCall
 * Signature: ([CLjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_nsk_stress_jni_gclocker_JNIWorker_NativeCall___3CLjava_lang_String_2
  (JNIEnv *env, jobject obj, jcharArray array, jstring str)
{
    BODY(jchar)
}

/*
 * Class:     JNIWorker
 * Method:    NativeCall
 * Signature: ([SLjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_nsk_stress_jni_gclocker_JNIWorker_NativeCall___3SLjava_lang_String_2
  (JNIEnv *env, jobject obj, jshortArray array, jstring str)
{
    BODY(jshort)
}

/*
 * Class:     JNIWorker
 * Method:    NativeCall
 * Signature: ([ILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_nsk_stress_jni_gclocker_JNIWorker_NativeCall___3ILjava_lang_String_2
  (JNIEnv *env, jobject obj, jintArray array, jstring str)
{
    BODY(jint)
}

/*
 * Class:     JNIWorker
 * Method:    NativeCall
 * Signature: ([JLjava/lang/String;)I
 */

JNIEXPORT jint JNICALL Java_nsk_stress_jni_gclocker_JNIWorker_NativeCall___3JLjava_lang_String_2
  (JNIEnv *env, jobject obj, jlongArray array, jstring str)
{
    BODY(jlong)
}

/*
 * Class:     JNIWorker
 * Method:    NativeCall
 * Signature: ([FLjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_nsk_stress_jni_gclocker_JNIWorker_NativeCall___3FLjava_lang_String_2
  (JNIEnv *env, jobject obj, jfloatArray array, jstring str)
{
    BODY(jfloat)
}

/*
 * Class:     JNIWorker
 * Method:    NativeCall
 * Signature: ([DLjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_nsk_stress_jni_gclocker_JNIWorker_NativeCall___3DLjava_lang_String_2
  (JNIEnv *env, jobject obj, jdoubleArray array, jstring str)
{
    BODY(jdouble)
}

}
