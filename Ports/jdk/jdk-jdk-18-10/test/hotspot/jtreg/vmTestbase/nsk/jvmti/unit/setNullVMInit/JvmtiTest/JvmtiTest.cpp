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

/*
 This test case to test the following:

           VMInit initial thread arg.
           SetThreadLocalStorage and SetEnvironmentLocalStorage should allow
           value to be set to NULL.
 */

#include <stdio.h>
#include <string.h>
#include <inttypes.h>
#include "jvmti.h"
#include "jni_tools.h"
#include "agent_common.h"

extern "C" {

#define JVMTI_ERROR_CHECK(str,res) if (res != JVMTI_ERROR_NONE) { printf(str); printf("%d\n",res); return res; }
#define JVMTI_ERROR_CHECK_EXPECTED_ERROR(str,res,err) if (res != err) { printf(str); printf("unexpected error %d\n",res); return res; }

#define JVMTI_ERROR_CHECK_VOID(str,res) if (res != JVMTI_ERROR_NONE) { printf(str); printf("%d\n",res); iGlobalStatus = 2; }

#define JVMTI_ERROR_CHECK_EXPECTED_ERROR_VOID(str,res,err) if (res != err) { printf(str); printf("unexpected error %d\n",res); iGlobalStatus = 2; }

jvmtiEnv *jvmti;
jint iGlobalStatus = 0;
static jvmtiEventCallbacks callbacks;

int printdump = 0;


void debug_printf(char *fmt, ...) {
    va_list args;

    va_start(args, fmt);
    if (printdump) {
        vprintf(fmt, args);
    }
    va_end(args);
}


intptr_t get_env_local() {
  jvmtiError res;
  void *val;
  res = jvmti->GetEnvironmentLocalStorage(&val);
  JVMTI_ERROR_CHECK("GetEnvironmentLocalStorage returned error", res);
  return (intptr_t)val;
}

void set_env_local(intptr_t x) {
  jvmtiError res;
  void *val = (void*)x;
  res = jvmti->SetEnvironmentLocalStorage(val);
  JVMTI_ERROR_CHECK_VOID("SetEnvironmentLocalStorage returned error", res);
}

intptr_t get_thread_local(jthread thread) {
  jvmtiError res;
  void *val;
  res = jvmti->GetThreadLocalStorage(thread, &val);
  JVMTI_ERROR_CHECK("GetThreadLocalStorage returned error", res);
  return (intptr_t)val;
}

void set_thread_local(jthread thread, intptr_t x) {
  jvmtiError res;
  void *val = (void*)x;
  res = jvmti->SetThreadLocalStorage(thread, val);
  JVMTI_ERROR_CHECK_VOID("SetThreadLocalStorage returned error", res);
}

void check_val(intptr_t x, intptr_t y, const char* msg) {
  if (x != y) {
    printf("Error in %s: expected %" PRIdPTR " to be %" PRIdPTR "\n", msg, x, y);
    iGlobalStatus = 2;
  } else if (printdump) {
    printf("Correct in %s: expected %" PRIdPTR " to be %" PRIdPTR "\n", msg, x, y);
  }
}


void JNICALL vmInit(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread) {
  check_val(get_thread_local(thread), 0, "thread initial");
  check_val(get_thread_local(NULL), 0, "thread initial");
  set_thread_local(thread, 35);
  check_val(get_thread_local(thread), 35, "thread set non-zero");
  check_val(get_thread_local(NULL), 35, "thread set non-zero");
  set_thread_local(NULL, 0);
  check_val(get_thread_local(thread), 0, "thread set zero");
  check_val(get_thread_local(NULL), 0, "thread set zero");

  check_val(get_env_local(), 14, "env set non-zero");
  set_env_local(77);
  check_val(get_env_local(), 77, "env set non-zero");
}


void init_callbacks() {
    memset((void *)&callbacks, 0, sizeof(jvmtiEventCallbacks));
    callbacks.VMInit = vmInit;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_JvmtiTest(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_JvmtiTest(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_JvmtiTest(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM * jvm, char *options, void *reserved) {
    jint res;

    if (options && strlen(options) > 0) {
        if (strstr(options, "printdump")) {
            printdump = 1;
        }
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res < 0) {
        printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    check_val(get_env_local(), 0, "env initial");
    set_env_local(0);
    check_val(get_env_local(), 0, "env set zero");
    set_env_local(14);
    check_val(get_env_local(), 14, "env set non-zero");

    /* Enable events */
    init_callbacks();
    res = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    JVMTI_ERROR_CHECK("SetEventCallbacks returned error", res);

    res = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT,NULL);
    JVMTI_ERROR_CHECK("SetEventNotificationMode for VM_INIT returned error", res);

    return JNI_OK;
}



JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_setNullVMInit_JvmtiTest_check(JNIEnv *env, jclass cls) {
  check_val(get_env_local(), 77, "env lasts");
  set_env_local(0);
  check_val(get_env_local(), 0, "env reset to zero");

  check_val(get_thread_local(NULL), 0, "thread check");

  return iGlobalStatus;
}

}
