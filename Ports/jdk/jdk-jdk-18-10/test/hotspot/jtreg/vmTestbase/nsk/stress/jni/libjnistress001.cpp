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
/* Changed from strings.h to string.h for Windows. */
#include <string.h>
#include <stdlib.h>
#include "jnihelper.h"

extern "C" {

#define DIGESTLENGTH 16

typedef struct {
  const char **str;
  char **checkstr;
} CHAR_ARRAY;

typedef struct {
  const jchar **str;
  char **checkstr;
  int *size;
} JCHAR_ARRAY;

JNIEXPORT jstring JNICALL
Java_nsk_stress_jni_JNIter001_jnistress (JNIEnv *env, jobject jobj, jstring jstr,
                                         jint nstr, jint printperiod) {

  int i, j;
  size_t k;
  static CHAR_ARRAY *element;
  unsigned char digest[DIGESTLENGTH];
  static int allocs = 0;
  static size_t strsize = 0;
  static unsigned int compared = 1;

  const char *clsName = "nsk/stress/jni/JNIter001";
  const char *name = "setpass";
  const char *sig = "(Z)V";
  const char *halt = "halt";
  const char *haltSig = "()V";
  jstring tmpstr;
  jclass clazz;
  jmethodID methodID;

  CHECK(env->MonitorEnter(jobj));

  if (!allocs) {
    element = (CHAR_ARRAY *)c_malloc(env, sizeof(CHAR_ARRAY));
    element->str = (const char **)c_malloc(env, nstr * sizeof(const char*));
    element->checkstr = (char **)c_malloc(env, nstr * sizeof(char*));
    for (j = 0; j < nstr; j++) {
      element->checkstr[j] = (char *)c_malloc(env, DIGESTLENGTH * sizeof(char));
    }
  }
  for (j = 0; j < DIGESTLENGTH; j++) {
    digest[j] = 0;
  }
  element->str[allocs] = env->GetStringUTFChars(jstr, 0); CE
  if (strlen(element->str[allocs]) != (size_t) env->GetStringUTFLength(jstr)) {
    printf("Length is wrong in string No. %d\n", allocs);
  } else {
    strsize += strlen(element->str[allocs]) + 1;
  }
  for (k = 0; k < strlen(element->str[allocs]); k++) {
    digest[k % DIGESTLENGTH] += element->str[allocs][k];
  }
  memcpy(element->checkstr[allocs], digest, DIGESTLENGTH);
  allocs++;
  if (allocs % printperiod == 0) {
    printf("Check string for thread %s is ", element->str[allocs - 1]);
    for (j = 0; j < DIGESTLENGTH; j++) {
      printf("%02x", digest[j]);
    }
    printf("\n");
  }
  if (allocs == nstr) {
    printf("JNI UTF8 strings memory = %zd\n", strsize);
    tmpstr = env->NewStringUTF(element->str[allocs - 1]); CE
    for (j = 0; j < nstr; j++) {

      for (i = 0; i < DIGESTLENGTH; i++) {
        digest[i] = 0;
      }
      for (k = 0; k < strlen(element->str[j]); k++) {
        digest[k % DIGESTLENGTH] += element->str[j][k];
      }
      if (memcmp(digest, element->checkstr[j], DIGESTLENGTH) == 0) {
        env->ReleaseStringUTFChars(jstr, element->str[j]); CE
        element->str[j] = NULL;
        element->checkstr[j] = NULL;
      } else {
        compared = 0;
        printf("The element No. %d has been corrupted %s vs %s\n", j,
               element->str[j], element->checkstr[j]);
        printf("Digest string  is %s [", element->str[j]);
        for (i = 0; i < DIGESTLENGTH; i++) {
          printf("ch[%d] = %02x", i, digest[i]);
        }
        printf("Digest end\n");
      }
    }
    allocs = 0;
    strsize = 0;
    for (j = 0; j < nstr; j++) {
      free(element->checkstr[j]);
    }
    free(element->checkstr);
    free((void *)(element->str));
    free(element);
    clazz = env->FindClass(clsName); CE
    if (!compared) {
      methodID = env->GetStaticMethodID(clazz, name, sig); CE
      env->CallStaticVoidMethod(clazz, methodID, JNI_FALSE); CE
    }
    //methodID = env->GetStaticMethodID(clazz, halt, haltSig); CE
    //env->CallStaticVoidMethod(clazz, methodID); CE
    return tmpstr;
  }
  CHECK(env->MonitorExit(jobj));
  return (env->NewStringUTF(element->str[allocs - 1]));
}

JNIEXPORT jstring JNICALL
Java_nsk_stress_jni_JNIter001_jnistress1(JNIEnv *env, jobject jobj, jstring jstr,
                                         jint nstr, jint printperiod) {

  int i, j;
  static JCHAR_ARRAY *javachars;
  unsigned char digest[DIGESTLENGTH];
  static int index = 0;
  static long len = 0;
  static unsigned int equal = 1;
  char *elem;
  int elem_len = -1;

  const char *clsName = "nsk/stress/jni/JNIter001";
  const char *name = "setpass";
  const char *sig = "(Z)V";
  const char *halt = "halt";
  const char *haltSig = "()V";
  jstring tmpstr;
  jclass clazz;
  jmethodID methodID;

  CHECK(env->MonitorEnter(jobj));
  if (!index) {
    javachars = (JCHAR_ARRAY *)c_malloc(env, sizeof(JCHAR_ARRAY));
    javachars->str = (const jchar **)c_malloc(env, nstr * sizeof(const jchar*));
    javachars->checkstr = (char **)c_malloc(env, nstr * sizeof(char*));
    javachars->size = (int *)c_malloc(env, nstr * sizeof(int));
    for (j = 0; j < nstr; j++) {
      javachars->checkstr[j] = (char *)c_malloc(env, DIGESTLENGTH * sizeof(char));
    }
  }
  for (j = 0; j < DIGESTLENGTH; j++) {
    digest[j] = 0;
  }
  javachars->str[index] = env->GetStringChars(jstr, 0); CE
  javachars->size[index] = env->GetStringUTFLength(jstr); CE
  elem_len = javachars->size[index];
  len += elem_len;
  elem = (char*) c_malloc(env, elem_len * sizeof(char));
  for (j = 0; j < elem_len; j++) {
    elem[j] = (char) javachars->str[index][j];
  }

  //memcpy(digest, elem, javachars->size[index]);
  for (j = 0; j < elem_len; j++) {
    digest[j % DIGESTLENGTH] += elem[j];
  }
  memcpy(javachars->checkstr[index++], digest, DIGESTLENGTH);
  if (index % printperiod == 0) {
    printf("Check string sum for thread %.*s is ", elem_len, elem);
    for (j = 0; j < DIGESTLENGTH; j++) {
      printf("%02x", digest[j]);
    }
    printf("\n");
  }
  free(elem);
  if (index == nstr) {
    printf("JNI Unicode strings memory = %ld\n", len);
    tmpstr = env->NewString(javachars->str[index - 1], elem_len); CE
    for (j = 0; j < nstr; j++) {
      elem = (char*) c_malloc(env, javachars->size[j] * sizeof(char));
      for (i = 0; i < javachars->size[j]; i++) {
        elem[i] = (char) javachars->str[j][i];
      }
      //memcpy(digest, elem, javachars->size[j]);
      for (i = 0; i < DIGESTLENGTH; i++) {
        digest[i] = 0;
      }
      for (i = 0; i < javachars->size[j]; i++) {
        digest[i % DIGESTLENGTH] += elem[i];
      }
      free(elem);
      if (memcmp(digest, javachars->checkstr[j], javachars->size[j]) == 0) {
        env->ReleaseStringChars(jstr, javachars->str[j]); CE
        javachars->str[j] = NULL;
        javachars->checkstr[j] = NULL;
        javachars->size[j] = 0;
      } else {
        equal = 0;
        printf("The Unicode element No. %d has been corrupted\n", j);
        for (i = 0; i < DIGESTLENGTH; i++) {
          printf("digest[%d] = %02x checkstr[%d] = %02x\n", i, digest[i], i, javachars->checkstr[j][i]);
        }
      }
    }
    index = 0;
    len = 0;
    for (j = 0; j < nstr; j++) {
      free(javachars->checkstr[j]);
    }
    free(javachars->checkstr);
    free((void *)(javachars->str));
    free(javachars->size);
    free(javachars);
    clazz = env->FindClass(clsName); CE
    if (!equal) {
      methodID = env->GetStaticMethodID(clazz, name, sig); CE
      env->CallStaticVoidMethod(clazz, methodID, JNI_FALSE); CE
    }
    //methodID = env->GetStaticMethodID(clazz, halt, haltSig); CE
    //env->CallStaticVoidMethod(clazz, methodID); CE
    return tmpstr;
  }
  CHECK(env->MonitorExit(jobj));
  return env->NewString(javachars->str[index - 1], elem_len);
}

}
