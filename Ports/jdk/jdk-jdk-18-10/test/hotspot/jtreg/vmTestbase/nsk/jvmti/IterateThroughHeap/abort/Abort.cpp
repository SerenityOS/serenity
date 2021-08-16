/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include <wchar.h>
#include <string.h>
#include <stdlib.h>

#include "jvmti.h"
#include "jni_tools.h"
#include "jvmti_tools.h"
#include "agent_common.h"

extern "C" {

static int timeout = 0;

jint JNICALL field_callback(jvmtiHeapReferenceKind kind,
                            const jvmtiHeapReferenceInfo* info,
                            jlong object_class_tag,
                            jlong* object_tag_ptr,
                            jvalue value,
                            jvmtiPrimitiveType value_type,
                            void* user_data) {
  (*(int*)user_data)++;
  return JVMTI_VISIT_ABORT;
}

jint JNICALL string_callback(jlong class_tag,
                             jlong size,
                             jlong* tag_ptr,
                             const jchar* value,
                             jint value_length,
                             void* user_data) {
  (*(int*)user_data)++;
  return JVMTI_VISIT_ABORT;
}

jint JNICALL array_callback(jlong class_tag,
                            jlong size,
                            jlong* tag_ptr,
                            jint element_count,
                            jvmtiPrimitiveType element_type,
                            const void* elements,
                            void* user_data) {
  (*(int*)user_data)++;
  return JVMTI_VISIT_ABORT;
}

jint JNICALL heap_callback(jlong class_tag,
                           jlong size,
                           jlong* tag_ptr,
                           jint length,
                           void* user_data) {
  (*(int*)user_data)++;
  return JVMTI_VISIT_ABORT;
}

static void JNICALL
agent(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
  jvmtiEvent event = JVMTI_EVENT_OBJECT_FREE;
  jvmtiHeapCallbacks primitive_callbacks;
  int invocations = 0;

  NSK_DISPLAY0("Waiting debugee.\n");
  if (!NSK_VERIFY(nsk_jvmti_enableEvents(JVMTI_ENABLE, 1, &event, NULL))) {
    return;
  }
  if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout))) {
    return;
  }

  memset(&primitive_callbacks, 0, sizeof(jvmtiHeapCallbacks));
  primitive_callbacks.primitive_field_callback = &field_callback;
  primitive_callbacks.array_primitive_value_callback = &array_callback;
  primitive_callbacks.string_primitive_value_callback = &string_callback;
  primitive_callbacks.heap_iteration_callback = &heap_callback;

  NSK_DISPLAY0("Iterating over reachable objects.\n");
  if (!NSK_JVMTI_VERIFY(jvmti->IterateThroughHeap(0, NULL, &primitive_callbacks, &invocations))) {
    nsk_jvmti_setFailStatus();
    return;
  }

  if (invocations != 1) {
    NSK_COMPLAIN1("Primitive callbacks were invoked more than once: "
                  "%d invocations registered.\n",invocations);
    nsk_jvmti_setFailStatus();
  }

  if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
    return;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_Abort(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_Abort(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_Abort(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
  jvmtiEnv *jvmti;
  jvmtiCapabilities caps;
  jvmtiEventCallbacks event_callbacks;

  jvmti = nsk_jvmti_createJVMTIEnv(jvm, reserved);
  if (!NSK_VERIFY(jvmti != NULL)) {
    return JNI_ERR;
  }

  nsk_jvmti_parseOptions(options);

  timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

  memset(&caps, 0, sizeof(caps));
  caps.can_tag_objects = 1;
  caps.can_generate_object_free_events = 1;

  if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
    return JNI_ERR;
  }

  memset(&event_callbacks, 0, sizeof(jvmtiEventCallbacks));
  if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&event_callbacks, sizeof(jvmtiEventCallbacks)))) {
    return JNI_ERR;
  }

  if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agent, NULL))) {
    return JNI_ERR;
  }

  return JNI_OK;
}

}
