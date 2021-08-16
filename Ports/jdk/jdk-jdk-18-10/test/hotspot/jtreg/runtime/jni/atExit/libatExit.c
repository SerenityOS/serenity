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

#include <stdio.h>
#include <stdlib.h>

#include "jni.h"

static JavaVM *jvm;

static const char* jni_error_code(int ret) {
  switch(ret) {
  case JNI_OK: return "JNI_OK";
  case JNI_ERR: return "JNI_ERR";
  case JNI_EDETACHED: return "JNI_EDETACHED";
  case JNI_EVERSION: return "JNI_EVERSION";
  case JNI_ENOMEM: return "JNI_ENOMEM";
  case JNI_EEXIST: return "JNI_EEXIST";
  case JNI_EINVAL: return "JNI_EINVAL";
  default: return "Invalid JNI error code";
  }
}

static void report(const char* func, int ret_actual, int ret_expected) {
  const char* ret = jni_error_code(ret_actual);
  if (ret_actual == ret_expected) {
    printf("%s returned %s as expected\n", func, ret);
  } else {
    printf("Unexpected JNI return code %s from %s\n", ret, func);
  }
}

static int using_system_exit = 0; // Not System.exit by default

JNIEXPORT
void JNICALL Java_TestAtExit_00024Tester_setUsingSystemExit(JNIEnv* env, jclass c) {
  using_system_exit = 1;
}

void at_exit_handler(void) {
  printf("In at_exit_handler\n");

  // We've saved the JavaVM from OnLoad time so we first try to
  // get a JNIEnv for the current thread.
  JNIEnv *env;
  jint res = (*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_2);
  report("GetEnv", res, JNI_EDETACHED);
  if (res == JNI_EDETACHED) {

    // Test all of the Invocation API functions

    res = (*jvm)->AttachCurrentThreadAsDaemon(jvm, (void **)&env, NULL);
    report("AttachCurrentThreadAsDaemon", res, JNI_ERR);
    res = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL);
    report("AttachCurrentThread", res, JNI_ERR);
    res = (*jvm)->DetachCurrentThread(jvm);
    report("DetachCurrentThread", res, JNI_ERR);

    JavaVMInitArgs args;
    args.version = JNI_VERSION_1_2;
    res = JNI_GetDefaultJavaVMInitArgs(&args);
    report("JNI_GetDefaultJavaVMInitArgs", res, JNI_OK);

    JavaVM* jvm_p[1];
    int nVMs;
    res = JNI_GetCreatedJavaVMs(jvm_p, 1, &nVMs);
    report("JNI_GetCreatedJavaVMs", res, JNI_OK);
    // Whether nVMs is 0 or 1 depends on the termination path
    if (nVMs == 0 && !using_system_exit) {
      printf("Found 0 created VMs as expected\n");
    } else if (nVMs == 1 && using_system_exit) {
      printf("Found 1 created VM as expected\n");
    } else {
      printf("Unexpected number of created VMs: %d\n", nVMs);
    }

    res = (*jvm)->DestroyJavaVM(jvm);
    report("DestroyJavaVM", res, JNI_ERR);

    // Failure mode depends on the termination path
    res = JNI_CreateJavaVM(jvm_p, (void**)&env, &args);
    report("JNI_CreateJavaVM", res, using_system_exit ? JNI_EEXIST : JNI_ERR);
  }
  // else test has already failed
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
  printf("JNI_OnLoad: registering atexit handler\n");
  jvm = vm;
  atexit(at_exit_handler);

  return JNI_VERSION_1_1;
}
