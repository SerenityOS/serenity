/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "jlong.h"

typedef struct {
    jclass holder;
    jmethodID mid;
} *JNICB;

#define CHECK_NULL(thing, message) \
    if (thing == NULL) { \
        jclass cls = (*env)->FindClass(env, "java/lang/Exception"); \
        (*env)->ThrowNew(env, cls, message); \
        return 0; \
    }

JNIEXPORT jlong JNICALL Java_org_openjdk_bench_jdk_incubator_foreign_Upcalls_makeCB
  (JNIEnv *env, jclass cls, jstring holderName, jstring methodName, jstring descriptor) {

  const char* holderNameC = (*env)->GetStringUTFChars(env, holderName, NULL);
  const char* methodNameC = (*env)->GetStringUTFChars(env, methodName, NULL);
  const char* descriptorC = (*env)->GetStringUTFChars(env, descriptor, NULL);

  JNICB cb = malloc(sizeof *cb);
  CHECK_NULL(cb, "Can not allocate cb");

  jclass holder = (*env)->FindClass(env, holderNameC);
  CHECK_NULL(holder, "Can not find class");
  holder = (jclass) (*env)->NewGlobalRef(env, holder);
  cb->holder = holder;

  jmethodID methodID = (*env)->GetStaticMethodID(env, holder, methodNameC, descriptorC);
  CHECK_NULL(methodID, "Can not find method");
  //methodID = (jmethodID) (*env)->NewGlobalRef(env, methodID); // DON'T DO THIS! -> Crashes GC
  cb->mid = methodID;

  (*env)->ReleaseStringUTFChars(env, holderName, holderNameC);
  (*env)->ReleaseStringUTFChars(env, methodName, methodNameC);
  (*env)->ReleaseStringUTFChars(env, descriptor, descriptorC);

  return ptr_to_jlong(cb);
}

JNIEXPORT void JNICALL Java_org_openjdk_bench_jdk_incubator_foreign_Upcalls_blank
  (JNIEnv *env, jclass cls, jlong cb) {
    JNICB jniCb = jlong_to_ptr(cb);
    (*env)->CallStaticVoidMethod(env, jniCb->holder, jniCb->mid);
}

JNIEXPORT jint JNICALL Java_org_openjdk_bench_jdk_incubator_foreign_Upcalls_identity
  (JNIEnv *env, jclass cls, jint x, jlong cb) {
    JNICB jniCb = jlong_to_ptr(cb);
    return (*env)->CallStaticIntMethod(env, jniCb->holder, jniCb->mid, x);
}

JNIEXPORT jint JNICALL Java_org_openjdk_bench_jdk_incubator_foreign_Upcalls_args5
  (JNIEnv *env, jclass cls,
      jlong a0, jdouble a1, jlong a2, jdouble a3, jlong a4,
      jlong cb) {
    JNICB jniCb = jlong_to_ptr(cb);
    return (*env)->CallStaticIntMethod(env, jniCb->holder, jniCb->mid, a0, a1, a2, a3, a4);
}

JNIEXPORT jint JNICALL Java_org_openjdk_bench_jdk_incubator_foreign_Upcalls_args10
  (JNIEnv *env, jclass cls,
      jlong a0, jdouble a1, jlong a2, jdouble a3, jlong a4,
      jdouble a5, jlong a6, jdouble a7, jlong a8, jdouble a9,
      jlong cb) {
    JNICB jniCb = jlong_to_ptr(cb);
    return (*env)->CallStaticIntMethod(env, jniCb->holder, jniCb->mid, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9);
}
