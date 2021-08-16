/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2019, Google and/or its affiliates. All rights reserved.
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

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "jvmti.h"
#include "ExceptionCheckingJniEnv.hpp"

// A few static global variables required due to the callback nature of JNI
// methods.
static bool is_error_called;
static const char* const null_return_expected_message_start =
    "JNI method GetFieldID : Return is NULL from exceptionjni001.cpp : ";
static const char* const null_file_expected_message_start =
    "JNI method GetFieldID : Return is NULL from Unknown File : ";

// Used by the ErrorCheckerMessage and the tests to determine test success.
static long expected_line_number;
static bool error_message_ok;
static const char* expected_message_start;

static bool CheckMessage(JNIEnv* env, const char* message, const char* expected_message,
                         long expected_line) {
  if (strstr(message, expected_message) != message) {
    fprintf(stderr, "Message does not start as expected:\n\t%s\n\t%s\n",
            message, expected_message);
    return false;
  }

  size_t len = strlen(expected_message);

  char* end_ptr = NULL;
  long actual_line = strtol(message + len, &end_ptr, 0);

  if (end_ptr == NULL || *end_ptr != '\0') {
    fprintf(stderr, "end_ptr == NULL or *end_ptr terminating from %s\n", message);
    return false;
  }

  if (actual_line != expected_line) {
    fprintf(stderr, "Actual line does not match expected:\n");
    fprintf(stderr, "\tActual: %ld\n\tExpected: %ld\n\tfrom: %s (%s)\n",
            actual_line, expected_line, message, message + len);
    return false;
  }

  // Clear the exception if everything lines up.
  env->ExceptionClear();
  return true;
}

static void ErrorCheckerMessage(JNIEnv* env, const char* error_message) {
  is_error_called = true;
  error_message_ok = CheckMessage(env, error_message, expected_message_start,
                                  expected_line_number);
}

static bool checkSuccess(JNIEnv* env, jclass cls) {
  ExceptionCheckingJniEnvPtr ec_jni(env, ErrorCheckerMessage);
  is_error_called = false;

  ec_jni->GetFieldID(cls, "anInteger", "I", TRACE_JNI_CALL);
  return !is_error_called;
}

static bool checkFailureMessageReturnNull(JNIEnv* env, jclass cls) {
  ExceptionCheckingJniEnvPtr ec_jni(env, ErrorCheckerMessage);

  expected_message_start = null_return_expected_message_start;
  expected_line_number = __LINE__ + 1;
  ec_jni->GetFieldID(cls, "whatever", "does not matter", TRACE_JNI_CALL);

  return is_error_called && error_message_ok;
}

static bool checkFailureMessageEmptyFile(JNIEnv* env, jclass cls) {
  ExceptionCheckingJniEnvPtr ec_jni(env, ErrorCheckerMessage);

  expected_message_start = null_file_expected_message_start;
  expected_line_number = __LINE__ + 1;
  ec_jni->GetFieldID(cls, "whatever", "does not matter", __LINE__, NULL);

  return is_error_called && error_message_ok;
}

static bool checkFailureMessageNilLine(JNIEnv* env, jclass cls) {
  ExceptionCheckingJniEnvPtr ec_jni(env, ErrorCheckerMessage);

  expected_message_start = null_return_expected_message_start;
  expected_line_number = 0;
  ec_jni->GetFieldID(cls, "whatever", "does not matter", 0, __FILE__);

  return is_error_called && error_message_ok;
}

static bool checkFailureMessageNegativeLine(JNIEnv* env, jclass cls) {
  ExceptionCheckingJniEnvPtr ec_jni(env, ErrorCheckerMessage);

  expected_message_start = null_return_expected_message_start;
  expected_line_number = -1;
  ec_jni->GetFieldID(cls, "whatever", "does not matter", -1, __FILE__);

  return is_error_called && error_message_ok;
}

static bool checkFailureMessageMinLine(JNIEnv* env, jclass cls) {
  ExceptionCheckingJniEnvPtr ec_jni(env, ErrorCheckerMessage);

  expected_message_start = null_return_expected_message_start;
  expected_line_number = INT32_MIN;
  ec_jni->GetFieldID(cls, "whatever", "does not matter", INT32_MIN, __FILE__);

  return is_error_called && error_message_ok;
}

static bool checkFailureMessageMaxLine(JNIEnv* env, jclass cls) {
  ExceptionCheckingJniEnvPtr ec_jni(env, ErrorCheckerMessage);

  expected_message_start = null_return_expected_message_start;
  expected_line_number = INT32_MAX;
  ec_jni->GetFieldID(cls, "whatever", "does not matter", INT32_MAX, __FILE__);

  return is_error_called && error_message_ok;
}

static bool CheckExceptionJni(JNIEnv* env, jclass cls) {
  typedef bool (*TestExceptionJniWrapper)(JNIEnv* env, jclass cls);

  TestExceptionJniWrapper tests[] = {
    checkSuccess,
    checkFailureMessageReturnNull,
    checkFailureMessageEmptyFile,
    checkFailureMessageNilLine,
    checkFailureMessageNegativeLine,
    checkFailureMessageMinLine,
    checkFailureMessageMaxLine,
  };

  size_t max_tests = sizeof(tests) / sizeof(tests[0]);
  for (size_t i = 0; i < max_tests; i++) {
    is_error_called = false;
    error_message_ok = false;
    if (!tests[i](env, cls)) {
      return false;
    }
  }
  return true;
}

extern "C" {

jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
  return JNI_OK;
}

JNIEXPORT jboolean JNICALL
Java_nsk_share_ExceptionCheckingJniEnv_exceptionjni001_check(JNIEnv *env, jclass cls) {
  return CheckExceptionJni(env, cls);
}

}
