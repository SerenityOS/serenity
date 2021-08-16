/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#include <string.h>
#include "jvmti.h"

extern "C" {

#define AGENT_NAME "agent2"

static JavaVM *java_vm = NULL;
static jthread exp_thread = NULL;
static jvmtiEnv *jvmti2 = NULL;
static jint agent2_event_count = 0;
static bool fail_status = false;

static void
check_jvmti_status(JNIEnv* env, jvmtiError err, const char* msg) {
  if (err != JVMTI_ERROR_NONE) {
    printf("check_jvmti_status: JVMTI function returned error: %d\n", err);
    fail_status = true;
    env->FatalError(msg);
  }
}

static void JNICALL
CompiledMethodLoad(jvmtiEnv* jvmti, jmethodID method,
                   jint code_size, const void* code_addr,
                   jint map_length, const jvmtiAddrLocationMap* map,
                   const void* compile_info) {
  JNIEnv* env = NULL;
  jthread thread = NULL;
  char* name = NULL;
  char* sign = NULL;
  jvmtiError err;

  // Posted on JavaThread's, so it is legal to obtain JNIEnv*
  if (java_vm->GetEnv((void **) (&env), JNI_VERSION_9) != JNI_OK) {
    fail_status = true;
    return;
  }

  err = jvmti->GetCurrentThread(&thread);
  check_jvmti_status(env, err, "CompiledMethodLoad: Error in JVMTI GetCurrentThread");
  if (!env->IsSameObject(thread, exp_thread)) {
    return; // skip events from unexpected threads
  }
  agent2_event_count++;

  err = jvmti->GetMethodName(method, &name, &sign, NULL);
  check_jvmti_status(env, err, "CompiledMethodLoad: Error in JVMTI GetMethodName");

  printf("%s: CompiledMethodLoad: %s%s\n", AGENT_NAME, name, sign);
  fflush(0);
}

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
  jvmtiEventCallbacks callbacks;
  jvmtiCapabilities caps;
  jvmtiError err;

  java_vm = jvm;
  if (jvm->GetEnv((void **) (&jvmti2), JVMTI_VERSION_9) != JNI_OK) {
    return JNI_ERR;
  }

  memset(&callbacks, 0, sizeof(callbacks));
  callbacks.CompiledMethodLoad = &CompiledMethodLoad;

  err = jvmti2->SetEventCallbacks(&callbacks, sizeof(jvmtiEventCallbacks));
  if (err != JVMTI_ERROR_NONE) {
    printf("Agent_OnLoad: Error in JVMTI SetEventCallbacks: %d\n", err);
    fail_status = true;
    return JNI_ERR;
  }

  memset(&caps, 0, sizeof(caps));
  caps.can_generate_compiled_method_load_events = 1;

  err = jvmti2->AddCapabilities(&caps);
  if (err != JVMTI_ERROR_NONE) {
    printf("Agent_OnLoad: Error in JVMTI AddCapabilities: %d\n", err);
    fail_status = true;
    return JNI_ERR;
  }
  return JNI_OK;
}

JNIEXPORT void JNICALL
Java_MyPackage_GenerateEventsTest_agent2SetThread(JNIEnv *env, jclass cls, jthread thread) {
  jvmtiError err;

  exp_thread = (jthread)env->NewGlobalRef(thread);

  err = jvmti2->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_LOAD, NULL);
  check_jvmti_status(env, err, "setThread2: Error in JVMTI SetEventNotificationMode: JVMTI_ENABLE");
}

JNIEXPORT jboolean JNICALL
Java_MyPackage_GenerateEventsTest_agent2FailStatus(JNIEnv *env, jclass cls) {
  jvmtiError err;

  err = jvmti2->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_COMPILED_METHOD_LOAD, NULL);
  check_jvmti_status(env, err, "check2: Error in JVMTI SetEventNotificationMode: JVMTI_DISABLE");

  printf("\n");
  if (agent2_event_count == 0) {
    printf("check2: Zero events in agent2 as expected\n");
  } else {
    fail_status = true;
    printf("check2: Unexpected non-zero event count in agent2: %d\n", agent2_event_count);
  }
  printf("\n");
  fflush(0);

  return fail_status;
}

} // extern "C"
