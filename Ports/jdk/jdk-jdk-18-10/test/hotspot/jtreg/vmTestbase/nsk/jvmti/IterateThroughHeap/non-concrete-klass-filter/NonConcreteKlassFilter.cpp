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

//expected values
#define EXPECTED_PRIMITIVES 2
#define EXPECTED_OCCURANCE_COUNT 1
static jlong expected_values[] = { 0xF1E1D01L, 0xF1E1D02L };
static int occurancies[] = { 0, 0 };

//unexpected fields have value 0xDEADF1E1Dxx
#define IS_FIELD_UNEXPECTED(x) (0xDEADF1E1D00LL == ((x>>8)<<8))

//in that phase no fields are expected to be reported
#define ZERO_INVOCATIONS_PHASE 0
//in this phase fields should be reported
#define STATIC_FIELDS_FINDING_PHASE 1

static int phase;

static int timeout = 0;

//klass-filters used in this test
#define FILTER_COUNT 2
static const char *types[] = { "nsk/jvmti/IterateThroughHeap/non_concrete_klass_filter/Interface",
                               "nsk/jvmti/IterateThroughHeap/non_concrete_klass_filter/AbstractClass" };

jint JNICALL field_callback(jvmtiHeapReferenceKind kind,
                            const jvmtiHeapReferenceInfo* info,
                            jlong object_class_tag,
                            jlong* object_tag_ptr,
                            jvalue value,
                            jvmtiPrimitiveType value_type,
                            void* user_data) {
  //nothing should be reported in ZERO_INVOCATIONS_PHASE
  if (phase == ZERO_INVOCATIONS_PHASE) {
    NSK_COMPLAIN2("jvmtiPrimitiveFieldCallback was invoked for a field with "
                  "class tag 0x%lX and object tag 0x%lX during iteration with "
                  "interface or abstract class as a filter.\n",
                  object_class_tag,*object_tag_ptr);
    nsk_jvmti_setFailStatus();
  } else {
    int i;
    if (value_type != JVMTI_PRIMITIVE_TYPE_LONG)
      return 0;

    if (IS_FIELD_UNEXPECTED(value.j)) {
      NSK_COMPLAIN3("Unexpected value 0x%lX was repotrted by "
                    "jvmtiPrimitiveFieldCallback for an object with "
                    "class tag 0x%lX and object tag 0x%lX.\n",
                    value.j, object_class_tag, *object_tag_ptr);
      nsk_jvmti_setFailStatus();
      return 0;
    }

    //find reported field in expected values
    for (i = 0; i < EXPECTED_PRIMITIVES; i++) {
      if (expected_values[i] == value.j)
        occurancies[i]++;
    }

  }
  return 0;
}

jint JNICALL string_callback(jlong class_tag,
                             jlong size,
                             jlong* tag_ptr,
                             const jchar* value,
                             jint value_length,
                             void* user_data) {
  NSK_COMPLAIN2("jvmtiStringPrimitiveValueCallback was invoked for an object "
                "with class tag 0x%lX and object tag 0x%lX.\n",class_tag,*tag_ptr);
  nsk_jvmti_setFailStatus();
  return 0;
}

jint JNICALL array_callback(jlong class_tag,
                            jlong size,
                            jlong* tag_ptr,
                            jint element_count,
                            jvmtiPrimitiveType element_type,
                            const void* elements,
                            void* user_data) {
   NSK_COMPLAIN2("jvmtiArrayPrimitiveValueCallback was invoked for an object "
                 "with class tag 0x%lX and object tag 0x%lX.\n",class_tag,*tag_ptr);
   nsk_jvmti_setFailStatus();
   return 0;
}

jint JNICALL heap_callback(jlong class_tag,
                           jlong size,
                           jlong* tag_ptr,
                           jint length,
                           void* user_data) {
  //nothing should be reported in ZERO_INVOCATIONS_PHASE
  if (phase == ZERO_INVOCATIONS_PHASE) {
    NSK_COMPLAIN2("jvmtiHeapIterationCallback was invoked for an object with "
                  "class tag 0x%lX and object tag 0x%lX during iteration with "
                  "interface or abstract class as a klass-filter.\n",
                  class_tag, *tag_ptr);
    nsk_jvmti_setFailStatus();
  }
  return 0;
}

static void JNICALL
agent(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
  jvmtiEvent event = JVMTI_EVENT_OBJECT_FREE;
  jvmtiHeapCallbacks primitive_callbacks;
  jclass klass;
  int i;

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

  phase = ZERO_INVOCATIONS_PHASE;
  for (i = 0; i < FILTER_COUNT; i++) {
    if (!NSK_VERIFY(NULL != (klass = jni->FindClass(types[i])))) {
      NSK_COMPLAIN1("Can't find class %s.\n",types[i]);
      nsk_jvmti_setFailStatus();
      return;
    }
    NSK_DISPLAY1("Iterating through heap with klass-filter '%s'.\n",types[i]);
    if (!NSK_JVMTI_VERIFY(jvmti->IterateThroughHeap(0, klass, &primitive_callbacks, NULL))) {
      nsk_jvmti_setFailStatus();
      return;
    }
  }

  phase = STATIC_FIELDS_FINDING_PHASE;
  NSK_DISPLAY0("Iterating through heap with klass-filter 'java/lang/Class'.\n");
  if (!NSK_VERIFY(NULL != (klass = jni->FindClass("java/lang/Class")))) {
    NSK_COMPLAIN0("Can't find class java/lang/Class.\n");
    nsk_jvmti_setFailStatus();
    return;
  }
  if (!NSK_JVMTI_VERIFY(jvmti->IterateThroughHeap(0, klass, &primitive_callbacks, NULL))) {
    nsk_jvmti_setFailStatus();
    return;
  }
  for (i = 0; i < EXPECTED_PRIMITIVES; i++) {
    if (occurancies[i] != EXPECTED_OCCURANCE_COUNT) {
      NSK_COMPLAIN3("Primitive static field with value 0x%lX was reported "
                    "%d times while expected to be reported %d times.\n",
                    expected_values[i], occurancies[i], EXPECTED_OCCURANCE_COUNT);
      nsk_jvmti_setFailStatus();
    }
  }

  if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
    return;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_NonConcreteKlassFilter(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_NonConcreteKlassFilter(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_NonConcreteKlassFilter(JavaVM *jvm, char *options, void *reserved) {
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
