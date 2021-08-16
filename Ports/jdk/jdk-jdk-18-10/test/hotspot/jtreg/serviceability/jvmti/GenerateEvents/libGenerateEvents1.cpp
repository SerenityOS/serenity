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

#define AGENT_NAME "agent1"

static JavaVM *java_vm = NULL;
static jthread exp_thread = NULL;
static jvmtiEnv *jvmti1 = NULL;
static jint agent1_event_count = 0;
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
    printf("CompiledMethodLoad: failed to obtain JNIEnv*\n");
    fail_status = true;
    return;
  }

  jvmti->GetCurrentThread(&thread);
  if (!env->IsSameObject(thread, exp_thread)) {
    return; // skip events from unexpected threads
  }
  agent1_event_count++;

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
  if (jvm->GetEnv((void **) (&jvmti1), JVMTI_VERSION) != JNI_OK) {
    printf("Agent_OnLoad: Error in GetEnv in obtaining jvmtiEnv*\n");
    fail_status = true;
    return JNI_ERR;
  }

  memset(&callbacks, 0, sizeof(callbacks));
  callbacks.CompiledMethodLoad = &CompiledMethodLoad;

  err = jvmti1->SetEventCallbacks(&callbacks, sizeof(jvmtiEventCallbacks));
  if (err != JVMTI_ERROR_NONE) {
    printf("Agent_OnLoad: Error in JVMTI SetEventCallbacks: %d\n", err);
    fail_status = true;
    return JNI_ERR;
  }

  memset(&caps, 0, sizeof(caps));
  caps.can_generate_compiled_method_load_events = 1;

  err = jvmti1->AddCapabilities(&caps);
  if (err != JVMTI_ERROR_NONE) {
    printf("Agent_OnLoad: Error in JVMTI AddCapabilities: %d\n", err);
    fail_status = true;
    return JNI_ERR;
  }
  return JNI_OK;
}

JNIEXPORT void JNICALL
Java_MyPackage_GenerateEventsTest_agent1GenerateEvents(JNIEnv *env, jclass cls) {
  jthread thread = NULL;
  jvmtiError err;

  err = jvmti1->GetCurrentThread(&thread);
  check_jvmti_status(env, err, "generateEvents1: Error in JVMTI GetCurrentThread");

  exp_thread = (jthread)env->NewGlobalRef(thread);

  err = jvmti1->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_LOAD, NULL);
  check_jvmti_status(env, err, "generateEvents1: Error in JVMTI SetEventNotificationMode: JVMTI_ENABLE");

  err = jvmti1->GenerateEvents(JVMTI_EVENT_COMPILED_METHOD_LOAD);
  check_jvmti_status(env, err, "generateEvents1: Error in JVMTI GenerateEvents");

  err = jvmti1->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_COMPILED_METHOD_LOAD, NULL);
  check_jvmti_status(env, err, "generateEvents1: Error in JVMTI SetEventNotificationMode: JVMTI_DISABLE");
}

JNIEXPORT jboolean JNICALL
Java_MyPackage_GenerateEventsTest_agent1FailStatus(JNIEnv *env, jclass cls) {
  return fail_status;
}

} // extern "C"
