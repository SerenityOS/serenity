/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "jnihelper.h"

extern "C" {

#define DIGESTLENGTH 16

JNIEXPORT jcharArray JNICALL
Java_nsk_stress_jni_JNIter004_CheckSum (JNIEnv *env, jobject jobj, jstring jstr) {

  unsigned char digest[DIGESTLENGTH];
  jchar *tmp;
  static jint upper = 0;
  jcharArray jArr;
  int i;
  const jchar *critstr;
  char *str;
  jint len = env->GetStringUTFLength(jstr); CE

  for (i = 0; i < DIGESTLENGTH; i++) {
    digest[i] = 0;
  }
  str = (char *)c_malloc(env, len * sizeof(char));
  /*     const char *threadName = env->GetStringUTFChars(jstr, 0); */

  CHECK(env->MonitorEnter(jobj));
  if (upper == 0) {
    tmp = (jchar *) c_malloc(env, DIGESTLENGTH * sizeof(char));
  }
  critstr = env->GetStringCritical(jstr, 0); CE
  for (i = 0; i < len; i++) {
    str[i] = (char) critstr[i];
  }
  env->ReleaseStringCritical(jstr, critstr); CE
  for (i = 0; i < len; i++) {
    digest[i % DIGESTLENGTH] += str[i];
  }
  free(str);
  memcpy(tmp, digest, DIGESTLENGTH);
  jArr = env->NewCharArray(DIGESTLENGTH / sizeof(jchar)); CE
  len = env->GetArrayLength(jArr); CE
  env->SetCharArrayRegion(jArr, 0, len, tmp); CE
  /*     ++upper; */
  CHECK(env->MonitorExit(jobj));
  return jArr;
}

JNIEXPORT jboolean JNICALL
Java_nsk_stress_jni_JNIter004_CheckCompare (JNIEnv *env, jobject jobj, jstring jstr,
                                            jcharArray cArr, jint limit) {

  unsigned char digest[DIGESTLENGTH];
  jchar *tmp;
  /*     jcharArray jArr; */
  const jchar *critstr;
  jint strlen;
  char *str;
  jboolean ret = JNI_TRUE;
  int i;
  static jint upper = 0;
  jint len;
  jchar *ch;

  for (i = 0; i < DIGESTLENGTH; i++) {
    digest[i] = 0;
  }
  strlen =  env->GetStringUTFLength(jstr); CE
  str = (char *)c_malloc(env, strlen * sizeof(char));

  len =env->GetArrayLength(cArr); CE

  CHECK(env->MonitorEnter(jobj));
  if (upper > limit) {
    CHECK(env->MonitorExit(jobj));
    return JNI_FALSE;
  }
  tmp = (jchar *)c_malloc(env, DIGESTLENGTH * sizeof(char));
  critstr = env->GetStringCritical(jstr, 0); CE
  for (i = 0; i < strlen; i++) {
    str[i] = (char) critstr[i];
  }
  env->ReleaseStringCritical(jstr, critstr); CE
  for (i = 0; i < strlen; i++) {
    digest[i % DIGESTLENGTH] += str[i % DIGESTLENGTH];
  }
  free(str);
  memcpy(tmp, digest, DIGESTLENGTH);

  /*     jArr = env->NewCharArray(DIGESTLENGTH/sizeof(jchar)); */
  /*     len = env->GetArrayLength(jArr); */
  /*     env->SetCharArrayRegion(jArr, 0, len, tmp); */
  /*     ++upper; */
  /*     env->MonitorExit(jobj); */

  /* Compare  */
  /*     env->MonitorEnter(jobj); */

  ch = (jchar *)env->GetPrimitiveArrayCritical(cArr, 0); CE

  printf("Comparing: ");
  for (i = 0; i < len; i++) {
    if (ch[i] != tmp[i]) {
      printf("Error in %d\n", i);
      printf("ch[%d] = %02x tmp[%d] = %02x\n", i, ch[i], i, tmp[i]);
      ret = JNI_FALSE;
    } else {
      printf("ch[%d] = %02x tmp[%d] = %02x\n", i, ch[i], i, tmp[i]);
    }
  }
  printf("\n");
  env->ReleasePrimitiveArrayCritical(cArr, ch, 0); CE
  ++upper;
  if ((upper % 500) == 0) {
    fprintf(stderr, "There are %d elements now.\n", upper);
  }
  if (upper == limit) {
    jclass clazz;
    jmethodID methodID;
    const char* name = "halt";
    const char* sig = "()V";

    clazz = env->GetObjectClass(jobj); CE
    methodID = env->GetStaticMethodID(clazz, name, sig); CE
    env->CallStaticVoidMethod(clazz, methodID); CE
    free(tmp);
    ret = JNI_TRUE;
  }
  CHECK(env->MonitorExit(jobj));
  return ret;
}

}
