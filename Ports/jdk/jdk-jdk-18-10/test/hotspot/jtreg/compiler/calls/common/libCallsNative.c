/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>

#include "jni.h"

#ifdef __cplusplus
extern "C" {
#endif

#define METHOD_SIGNATURE "(IJFDLjava/lang/String;)Z"
#define STATIC_CALLEE_SIGNATURE "(Lcompiler/calls/common/InvokeStatic;IJFDLjava/lang/String;)Z"
#define BASE_CLASS "compiler/calls/common/CallsBase"

#define CHECK_EXCEPTIONS if ((*env)->ExceptionCheck(env)) return
#define CHECK_EXCEPTIONS_FALSE if ((*env)->ExceptionCheck(env)) return JNI_FALSE

#define IS_STATIC 1
#define NOT_STATIC 0

jboolean doCalleeWork(JNIEnv *env, jobject self, jint param1, jlong param2,
    jfloat param3, jdouble param4, jstring param5) {
  jclass cls = (*env)->GetObjectClass(env, self);
  jfieldID calleeVisitedID = (*env)->GetFieldID(env, cls, "calleeVisited", "Z");
  jclass CheckCallsBaseClass;
  jmethodID checkValuesID;
  CHECK_EXCEPTIONS_FALSE;
  (*env)->SetBooleanField(env, self, calleeVisitedID, JNI_TRUE);
  CHECK_EXCEPTIONS_FALSE;
  CheckCallsBaseClass = (*env)->FindClass(env, BASE_CLASS);
  CHECK_EXCEPTIONS_FALSE;
  checkValuesID = (*env)->GetStaticMethodID(env, CheckCallsBaseClass,
      "checkValues", "(IJFDLjava/lang/String;)V");
  CHECK_EXCEPTIONS_FALSE;
  (*env)->CallStaticVoidMethod(env, CheckCallsBaseClass, checkValuesID,
      param1, param2, param3, param4, param5);
  return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_compiler_calls_common_InvokeDynamic_calleeNative(JNIEnv *env, jobject obj,
    jint param1, jlong param2, jfloat param3, jdouble param4, jstring param5) {
  return doCalleeWork(env, obj, param1, param2, param3, param4, param5);
}

JNIEXPORT jboolean JNICALL Java_compiler_calls_common_InvokeInterface_calleeNative(JNIEnv *env, jobject obj,
    jint param1, jlong param2, jfloat param3, jdouble param4, jstring param5) {
  return doCalleeWork(env, obj, param1, param2, param3, param4, param5);
}

JNIEXPORT jboolean JNICALL Java_compiler_calls_common_InvokeSpecial_calleeNative(JNIEnv *env, jobject obj,
    jint param1, jlong param2, jfloat param3, jdouble param4, jstring param5) {
  return doCalleeWork(env, obj, param1, param2, param3, param4, param5);
}

JNIEXPORT jboolean JNICALL Java_compiler_calls_common_InvokeVirtual_calleeNative(JNIEnv *env, jobject obj,
    jint param1, jlong param2, jfloat param3, jdouble param4, jstring param5) {
  return doCalleeWork(env, obj, param1, param2, param3, param4, param5);
}

JNIEXPORT jboolean JNICALL Java_compiler_calls_common_InvokeStatic_calleeNative(JNIEnv *env, jclass obj,
    jobject self, jint param1, jlong param2, jfloat param3, jdouble param4, jstring param5) {
  return doCalleeWork(env, self, param1, param2, param3, param4, param5);
}

void doCallerWork(JNIEnv *env, jobject obj, int isStatic) {
  jclass cls = (*env)->GetObjectClass(env, obj);
  jmethodID calleeMethodID = 0;
  jfieldID errorMessageID;
  jfieldID nativeCalleeID;
  jobject errorMessage;
  jmethodID assertTrue;
  jboolean callNative;
  jclass assertsClass;
  jclass baseClass;
  jboolean result;
  char* methodName;
  CHECK_EXCEPTIONS;
  nativeCalleeID = (*env)->GetFieldID(env, cls, "nativeCallee", "Z");
  CHECK_EXCEPTIONS;
  callNative = (*env)->GetBooleanField(env, obj, nativeCalleeID);
  CHECK_EXCEPTIONS;
  methodName = (callNative == JNI_TRUE) ? "calleeNative" : "callee";
  if (isStatic) {
    calleeMethodID = (*env)->GetStaticMethodID(env, cls, methodName,
        STATIC_CALLEE_SIGNATURE);
  } else {
    calleeMethodID = (*env)->GetMethodID(env, cls, methodName, METHOD_SIGNATURE);
  }
  CHECK_EXCEPTIONS;
  if (isStatic) {
    result = (*env)->CallStaticBooleanMethod(env, cls, calleeMethodID, obj,
        (jint) 1, (jlong) 2L, (jfloat) 3.0f, (jdouble) 4.0, (*env)->NewStringUTF(env, "5"));
  } else {
    result = (*env)->CallBooleanMethod(env, obj, calleeMethodID,
        (jint) 1, (jlong) 2L, (jfloat) 3.0f, (jdouble) 4.0, (*env)->NewStringUTF(env, "5"));
  }
  CHECK_EXCEPTIONS;
  baseClass = (*env)->FindClass(env, BASE_CLASS);
  CHECK_EXCEPTIONS;
  errorMessageID = (*env)->GetStaticFieldID(env, baseClass,
      "CALL_ERR_MSG", "Ljava/lang/String;");
  CHECK_EXCEPTIONS;
  errorMessage = (*env)->GetStaticObjectField(env, baseClass, errorMessageID);
  CHECK_EXCEPTIONS;
  assertsClass = (*env)->FindClass(env, "jdk/test/lib/Asserts");
  CHECK_EXCEPTIONS;
  assertTrue = (*env)->GetStaticMethodID(env, assertsClass,
      "assertTrue", "(ZLjava/lang/String;)V");
  (*env)->CallStaticVoidMethod(env, assertsClass, assertTrue, result,
      errorMessage);
}

JNIEXPORT void JNICALL Java_compiler_calls_common_InvokeSpecial_callerNative(JNIEnv *env, jobject obj) {
  doCallerWork(env, obj, NOT_STATIC);
}

JNIEXPORT void JNICALL Java_compiler_calls_common_InvokeVirtual_callerNative(JNIEnv *env, jobject obj) {
  doCallerWork(env, obj, NOT_STATIC);
}

JNIEXPORT void JNICALL Java_compiler_calls_common_InvokeStatic_callerNative(JNIEnv *env, jobject obj) {
  doCallerWork(env, obj, IS_STATIC);
}

#ifdef __cplusplus
}
#endif
