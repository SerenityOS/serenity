/*
 * Copyright (c) 2018, Red Hat, Inc. and/or its affiliates.
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
 *
 */

#include "jni.h"

JNIEXPORT jlong JNICALL JavaCritical_gc_CriticalNative_sum1
  (jint length, jlong* a) {
  jlong sum = 0;
  jint index;
  for (index = 0; index < length; index ++) {
    sum += a[index];
  }

  return sum;
}

JNIEXPORT jlong JNICALL  JavaCritical_gc_CriticalNative_sum2
  (jlong a1, jint a2_length, jint* a2, jint a4_length, jint* a4, jint a6_length, jlong* a6, jint a8_length, jint* a8) {
  jlong sum = a1;
  jint index;
  for (index = 0; index < a2_length; index ++) {
    sum += a2[index];
  }

  for (index = 0; index < a4_length; index ++) {
    sum += a4[index];
  }

  for (index = 0; index < a6_length; index ++) {
    sum += a6[index];
  }

  for (index = 0; index < a8_length; index ++) {
    sum += a8[index];
  }
  return sum;
}

JNIEXPORT jlong JNICALL Java_gc_CriticalNative_sum1
  (JNIEnv *env, jclass jclazz, jlongArray a) {
  jlong sum = 0;
  jsize len = (*env)->GetArrayLength(env, a);
  jsize index;
  jlong* arr = (jlong*)(*env)->GetPrimitiveArrayCritical(env, a, 0);
  for (index = 0; index < len; index ++) {
    sum += arr[index];
  }

  (*env)->ReleasePrimitiveArrayCritical(env, a, arr, 0);
  return sum;
}

JNIEXPORT jlong JNICALL Java_gc_CriticalNative_sum2
  (JNIEnv *env, jclass jclazz, jlong a1, jintArray a2, jintArray a3, jlongArray a4, jintArray a5) {
  jlong sum = a1;
  jsize index;
  jsize len;
  jint* a2_arr;
  jint* a3_arr;
  jlong* a4_arr;
  jint* a5_arr;

  len = (*env)->GetArrayLength(env, a2);
  a2_arr = (jint*)(*env)->GetPrimitiveArrayCritical(env, a2, 0);
  for (index = 0; index < len; index ++) {
    sum += a2_arr[index];
  }
  (*env)->ReleasePrimitiveArrayCritical(env, a2, a2_arr, 0);

  len = (*env)->GetArrayLength(env, a3);
  a3_arr = (jint*)(*env)->GetPrimitiveArrayCritical(env, a3, 0);
  for (index = 0; index < len; index ++) {
    sum += a3_arr[index];
  }
  (*env)->ReleasePrimitiveArrayCritical(env, a3, a3_arr, 0);

  len = (*env)->GetArrayLength(env, a4);
  a4_arr = (jlong*)(*env)->GetPrimitiveArrayCritical(env, a4, 0);
  for (index = 0; index < len; index ++) {
    sum += a4_arr[index];
  }
  (*env)->ReleasePrimitiveArrayCritical(env, a4, a4_arr, 0);

  len = (*env)->GetArrayLength(env, a5);
  a5_arr = (jint*)(*env)->GetPrimitiveArrayCritical(env, a5, 0);
  for (index = 0; index < len; index ++) {
    sum += a5_arr[index];
  }
  (*env)->ReleasePrimitiveArrayCritical(env, a5, a5_arr, 0);

  return sum;
}


JNIEXPORT jboolean JNICALL JavaCritical_gc_CriticalNative_isNull
  (jint length, jint* a) {
  return (a == NULL) && (length == 0);
}

JNIEXPORT jboolean JNICALL Java_gc_CriticalNative_isNull
  (JNIEnv *env, jclass jclazz, jintArray a) {
  if (a == NULL) return JNI_TRUE;
  jsize len = (*env)->GetArrayLength(env, a);
  jint* arr = (jint*)(*env)->GetPrimitiveArrayCritical(env, a, 0);
  jboolean is_null = (arr == NULL) && (len == 0);
  (*env)->ReleasePrimitiveArrayCritical(env, a, arr, 0);
  return is_null;
}

