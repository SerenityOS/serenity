/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

static jmethodID _callable_method_id;
static jmethodID _callable_nested_method_id;

static void check_exceptions(JNIEnv *env) {
  if ((*env)->ExceptionCheck(env)) {
    (*env)->ExceptionDescribe(env);
    (*env)->FatalError(env, "Unexpected Exception");
  }
}

static jmethodID get_method_id(JNIEnv *env, jclass clz, jstring jname, jstring jsig) {
  jmethodID mid;
  const char *name, *sig;

  name = (*env)->GetStringUTFChars(env, jname, NULL);
  check_exceptions(env);

  sig  = (*env)->GetStringUTFChars(env, jsig, NULL);
  check_exceptions(env);

  mid  = (*env)->GetMethodID(env, clz, name, sig);
  check_exceptions(env);

  (*env)->ReleaseStringUTFChars(env, jname, name);
  (*env)->ReleaseStringUTFChars(env, jsig, sig);
  return mid;
}

JNIEXPORT void JNICALL
Java_TestCheckedJniExceptionCheck_initMethodIds(JNIEnv *env,
                                                jobject obj,
                                                jstring callable_method_name,
                                                jstring callable_method_sig,
                                                jstring callable_nested_method_name,
                                                jstring callable_nested_method_sig) {
  jclass clz = (*env)->GetObjectClass(env, obj);

  _callable_method_id = get_method_id(env, clz,
                                      callable_method_name,
                                      callable_method_sig);

  _callable_nested_method_id = get_method_id(env, clz,
                                             callable_nested_method_name,
                                             callable_nested_method_sig);
}

JNIEXPORT void JNICALL
Java_TestCheckedJniExceptionCheck_callJavaFromNative(JNIEnv *env,
                                                     jobject obj,
                                                     jint nofCalls,
                                                     jboolean checkExceptions) {
  int i;
  for (i = 0; i < nofCalls; i++) {
    (*env)->CallVoidMethod(env, obj, _callable_method_id);
    if (checkExceptions == JNI_TRUE) {
      check_exceptions(env);
    }
  }
}

JNIEXPORT void JNICALL
Java_TestCheckedJniExceptionCheck_callNestedJavaFromNative(JNIEnv *env,
                                                           jobject obj,
                                                           jint nofCalls,
                                                           jboolean checkExceptions) {
  int i;
  for (i = 0; i < nofCalls; i++) {
    (*env)->CallVoidMethod(env, obj, _callable_nested_method_id, nofCalls, checkExceptions);
    if (checkExceptions == JNI_TRUE) {
      check_exceptions(env);
    }
  }
}
