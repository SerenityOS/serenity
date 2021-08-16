/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include <string.h>
#include "jvmti.h"

#ifdef __cplusplus
extern "C" {
#endif

#define STATUS_PASSED 0
#define STATUS_FAILED 2
#define TranslateError(err) "JVMTI error"

static jint result = STATUS_PASSED;
static jvmtiEnv *jvmti = NULL;

#define DECL_TEST_FUNC(type, Type) \
static void \
test_##type(jthread thr, int depth, int slot, const char* exp_type) { \
  j##type val; \
  jvmtiError err = jvmti->GetLocal##Type(thr, depth, slot, &val); \
  \
  printf(" GetLocal%s: %s (%d)\n", #Type, TranslateError(err), err); \
  if (err != JVMTI_ERROR_NONE) { \
    printf(" FAIL: GetLocal%s failed to get value from a local %s\n", #Type, exp_type); \
    result = STATUS_FAILED; \
  } else { \
    printf(" GetLocal%s got value from a local %s as expected\n", #Type, exp_type); \
  } \
}

#define DECL_TEST_INV_SLOT_FUNC(type, Type) \
static void \
test_##type##_inv_slot(jthread thr, int depth, int slot, const char* exp_type) { \
  j##type val; \
  jvmtiError err = jvmti->GetLocal##Type(thr, depth, slot, &val); \
  \
  printf(" GetLocal%s: %s (%d)\n", #Type, TranslateError(err), err); \
  if (err != JVMTI_ERROR_INVALID_SLOT) { \
    printf(" FAIL: GetLocal%s failed to return JVMTI_ERROR_INVALID_SLOT for local %s\n", #Type, exp_type); \
    result = STATUS_FAILED; \
  } else { \
    printf(" GetLocal%s returned JVMTI_ERROR_INVALID_SLOT for local %s as expected\n", #Type, exp_type); \
  } \
}

#define DECL_TEST_TYPE_MISMATCH_FUNC(type, Type) \
static void \
test_##type##_type_mismatch(jthread thr, int depth, int slot, const char* exp_type) { \
  j##type val; \
  jvmtiError err = jvmti->GetLocal##Type(thr, depth, slot, &val); \
  \
  printf(" GetLocal%s: %s (%d)\n", #Type, TranslateError(err), err); \
  if (err != JVMTI_ERROR_TYPE_MISMATCH) { \
    printf(" FAIL: GetLocal%s failed to return JVMTI_ERROR_TYPE_MISMATCH for local %s\n", #Type, exp_type); \
    result = STATUS_FAILED; \
  } else { \
    printf(" GetLocal%s returned JVMTI_ERROR_TYPE_MISMATCH for local %s as expected\n", #Type, exp_type); \
  } \
}

DECL_TEST_FUNC(int, Int);
DECL_TEST_FUNC(float, Float);
DECL_TEST_FUNC(long, Long);
DECL_TEST_FUNC(double, Double);
DECL_TEST_FUNC(object, Object);

DECL_TEST_INV_SLOT_FUNC(int, Int);
DECL_TEST_INV_SLOT_FUNC(float, Float);
DECL_TEST_INV_SLOT_FUNC(long, Long);
DECL_TEST_INV_SLOT_FUNC(double, Double);
DECL_TEST_INV_SLOT_FUNC(object, Object);

DECL_TEST_TYPE_MISMATCH_FUNC(int, Int);
DECL_TEST_TYPE_MISMATCH_FUNC(float, Float);
DECL_TEST_TYPE_MISMATCH_FUNC(long, Long);
DECL_TEST_TYPE_MISMATCH_FUNC(double, Double);
DECL_TEST_TYPE_MISMATCH_FUNC(object, Object);

static void
test_local_byte(jthread thr, int depth, int slot) {
  printf("\n test_local_byte: BEGIN\n\n");

  test_int(thr, depth, slot, "byte");
  test_long_inv_slot(thr, depth, slot, "byte");
  test_float(thr, depth, slot, "byte");
  test_double_inv_slot(thr, depth, slot, "byte");
  test_object_type_mismatch(thr, depth, slot, "byte");

  printf("\n test_local_byte: END\n\n");
}

static void
test_local_object(jthread thr, int depth, int slot) {
  printf("\n test_local_object: BEGIN\n\n");

  test_int_type_mismatch(thr, depth, slot, "object");
  test_long_type_mismatch(thr, depth, slot, "object");
  test_float_type_mismatch(thr, depth, slot, "object");
  test_double_type_mismatch(thr, depth, slot, "object");
  test_object(thr, depth, slot, "object");

  printf("\n test_local_object: END\n\n");
}

static void
test_local_double(jthread thr, int depth, int slot) {
  printf("\n test_local_double: BEGIN\n\n");

  test_int(thr, depth, slot, "double");
  test_long(thr, depth, slot, "double");
  test_float(thr, depth, slot, "double");
  test_double(thr, depth, slot, "double");
  test_object_type_mismatch(thr, depth, slot, "double");

  printf("\n test_local_double: END\n\n");
}

static void
test_local_integer(jthread thr, int depth, int slot) {
  printf("\n test_local_integer: BEGIN\n\n");

  test_int(thr, depth, slot, "int");
  test_float(thr, depth, slot, "int");
  test_object_type_mismatch(thr, depth, slot, "double");

  printf("\n test_local_integer: END\n\n");
}

static void
test_local_invalid(jthread thr, int depth, int slot) {
  printf("\n test_local_invalid: BEGIN\n\n");

  test_int_inv_slot(thr, depth, slot, "invalid");
  test_long_inv_slot(thr, depth, slot, "invalid");
  test_float_inv_slot(thr, depth, slot, "invalid");
  test_double_inv_slot(thr, depth, slot, "invalid");

  printf("\n test_local_invalid: END\n\n");
}

jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
  jint res;
  jvmtiError err;
  static jvmtiCapabilities caps;

  res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_9);
  if (res != JNI_OK || jvmti == NULL) {
    printf("Wrong result of a valid call to GetEnv!\n");
    return JNI_ERR;
  }
  caps.can_access_local_variables = 1;

  err = jvmti->AddCapabilities(&caps);
  if (err != JVMTI_ERROR_NONE) {
    printf("AddCapabilities: unexpected error: %s (%d)\n", TranslateError(err), err);
    return JNI_ERR;
  }
  err = jvmti->GetCapabilities(&caps);
  if (err != JVMTI_ERROR_NONE) {
    printf("GetCapabilities: unexpected error: %s (%d)\n", TranslateError(err), err);
    return JNI_ERR;
  }
  if (!caps.can_access_local_variables) {
    printf("Warning: Access to local variables is not implemented\n");
    return JNI_ERR;
  }
  return JNI_OK;
}

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
  return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT jint JNICALL
Agent_OnAttach(JavaVM *jvm, char *options, void *reserved) {
  return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT void JNICALL
Java_GetLocalVars_testLocals(JNIEnv *env, jclass cls, jobject thread) {
  /*
   * We test the JVMTI GetLocal<Type> for locals of the method:
   *
   *  int staticMeth(byte byteArg, Object objArg, double dblArg, int intArg) {
   *      testLocals(Thread.currentThread());
   *      {
   *          int intLoc = 9999;
   *          intArg = intLoc;
   *      }
   *      return intArg;
   *  }
   */
  static const char* METHOD_NAME = "staticMeth";
  static const char* METHOD_SIGN = "(BLjava/lang/Object;DI)I";
  static const int Depth = 1;
  static const int ByteSlot = 0;
  static const int ObjSlot = 1;
  static const int DblSlot = 2;
  static const int IntSlot = 4;
  static const int InvalidSlot = 5;

  jmethodID mid = NULL;

  if (jvmti == NULL) {
    printf("JVMTI client was not properly loaded!\n");
    result = STATUS_FAILED;
    return;
  }

  mid = env->GetStaticMethodID(cls, METHOD_NAME, METHOD_SIGN);
  if (mid == NULL) {
    printf("Cannot find Method ID for %s%s\n", METHOD_NAME, METHOD_SIGN);
    result = STATUS_FAILED;
    return;
  }

  test_local_byte(thread, Depth, ByteSlot);
  test_local_object(thread, Depth, ObjSlot);
  test_local_double(thread, Depth, DblSlot);
  test_local_integer(thread, Depth, IntSlot);
  test_local_invalid(thread, Depth, InvalidSlot);
}

JNIEXPORT jint JNICALL
Java_GetLocalVars_getStatus(JNIEnv *env, jclass cls) {
    return result;
}

#ifdef __cplusplus
}
#endif
