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

#define TEST_OBJECT_TAG 0x8000
#define EXPECTED_NON_PRIMITIVES_COUNT 1
#define EXPECTED_PRIMITIVE_VALUE 0xC1A55F1E1DLL

static int timeout = 0;

static int field_found = 0;
static int object_unloaded = 0;
static int non_primitive_reported = 0;


#define className "nsk/jvmti/IterateThroughHeap/concrete_klass_filter/ConcreteKlassFilter"
#define fieldName "testObject"
#define fieldSig "Ljava/lang/Object;"
#define testClassName "nsk/jvmti/IterateThroughHeap/concrete_klass_filter/TestClass"


jint JNICALL field_callback(jvmtiHeapReferenceKind kind,
                            const jvmtiHeapReferenceInfo* info,
                            jlong object_class_tag,
                            jlong* object_tag_ptr,
                            jvalue value,
                            jvmtiPrimitiveType value_type,
                            void* user_data) {
  //only field of our test object are expected
  if (*object_tag_ptr != TEST_OBJECT_TAG) {
    NSK_COMPLAIN2("jvmtiPrimitiveFieldCallback was invoked for primitive "
                  "field with unexpected class tag 0x%lX and object tag 0x%lX.\n",
                  object_class_tag, *object_tag_ptr);
    nsk_jvmti_setFailStatus();
    return 0;
  }
  //expected field is long
  if (value_type != JVMTI_PRIMITIVE_TYPE_LONG) {
    NSK_COMPLAIN0("jvmtiPrimitiveFieldCallback was invoked for non-long field.\n");
    nsk_jvmti_setFailStatus();
    return 0;
  }
  //check value
  if (value.j != EXPECTED_PRIMITIVE_VALUE) {
    NSK_COMPLAIN0("Unexpected value was passed to jvmtiPrimitiveFieldCallback.\n");
    NSK_COMPLAIN1("Expected value: 0x%lX.\n", EXPECTED_PRIMITIVE_VALUE);
    NSK_COMPLAIN1("Passed value: 0x%lX.\n", value.j);
    nsk_jvmti_setFailStatus();
  } else {
    field_found++;
  }
  return 0;
}

jint JNICALL string_callback(jlong class_tag,
                             jlong size,
                             jlong* tag_ptr,
                             const jchar* value,
                             jint value_length,
                             void* user_data) {
  //strings are not expected
  NSK_COMPLAIN2("jvmtiStringPrimitiveValueCallback was invoked for object "
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
  //arrays are not expected
  NSK_COMPLAIN2("jvmtiArrayPrimitiveValueCallback was invoked for object "
                "with class tag 0x%lX and object tag 0x%lX.\n",class_tag,*tag_ptr);
  nsk_jvmti_setFailStatus();
  return 0;
}

jint JNICALL heap_callback(jlong class_tag,
                           jlong size,
                           jlong* tag_ptr,
                           jint length,
                           void* user_data) {
  //test object have to be reported by this callback
  if (*tag_ptr != TEST_OBJECT_TAG) {
    NSK_COMPLAIN2("Object with unexpected class tag 0x%lX and object tag 0x%lX "
                  "was passed to jvmtiHeapIterationCallback.\n", class_tag, *tag_ptr);
    nsk_jvmti_setFailStatus();
    return 0;
  }

  non_primitive_reported++;

  if (non_primitive_reported>EXPECTED_NON_PRIMITIVES_COUNT) {
    NSK_COMPLAIN1("Test object was reported more than %d times.\n",
                  EXPECTED_NON_PRIMITIVES_COUNT);
    nsk_jvmti_setFailStatus();
  }

  return 0;
}

JNIEXPORT void JNICALL
object_free_callback(jvmtiEnv* jvmti, jlong tag) {
  if (tag != TEST_OBJECT_TAG) {
    NSK_COMPLAIN1("object free callback was invoked for an object with "
                  "unexpected tag 0x%lX.\n",tag);
    nsk_jvmti_setFailStatus();
  } else {
    object_unloaded = 1;
  }
}

/**
 * Tag test object and it's class.
 */
int tag_objects(jvmtiEnv *jvmti, JNIEnv *jni) {
  jclass debugee;
  jfieldID testObjectField;
  jobject testObject;
  jclass testObjectClass;

  if (!NSK_VERIFY(NULL != (debugee = jni->FindClass(className))))
    return JNI_ERR;

  if (!NSK_VERIFY(NULL != (testObjectField = jni->GetStaticFieldID(debugee, fieldName, fieldSig))))
    return JNI_ERR;

  if (!NSK_VERIFY(NULL != (testObject = (jni->GetStaticObjectField(debugee, testObjectField)))))
    return JNI_ERR;

  if (!NSK_VERIFY(NULL != (testObjectClass = (jni->GetObjectClass(testObject)))))
    return JNI_ERR;

  // tag class and it's instance to pass this tag into primitive field callback
  if (!NSK_JVMTI_VERIFY(jvmti->SetTag(testObject, TEST_OBJECT_TAG)))
    return JNI_ERR;
  if (!NSK_JVMTI_VERIFY(jvmti->SetTag(testObjectClass, TEST_OBJECT_TAG)))
    return JNI_ERR;

  jni->DeleteLocalRef(testObjectClass);
  jni->DeleteLocalRef(testObject);

  return JNI_OK;
}

void verify_objects() {
  //if test object was not unloaded then it's field expected to be found once.
  if (object_unloaded) return;
  if (field_found == 0) {
    NSK_COMPLAIN0("TestClass instance field was not found.\n");
    nsk_jvmti_setFailStatus();
  } if (field_found > 1) {
    NSK_COMPLAIN1("TestClass instance field was reported more than once: %d times.\n",
                  field_found);
    nsk_jvmti_setFailStatus();
  }
  field_found = 0;
  non_primitive_reported = 0;
}

static void JNICALL
agent(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
  jvmtiEvent event = JVMTI_EVENT_OBJECT_FREE;
  jvmtiHeapCallbacks primitive_callbacks;
  jclass klass;

  if (!NSK_VERIFY(NULL != (klass = jni->FindClass(testClassName)))) {
    NSK_COMPLAIN1("Can't find class %s.\n",testClassName);
    nsk_jvmti_setFailStatus();
    return;
  }

  NSK_DISPLAY0("Waiting debugee.\n");
  if (!NSK_VERIFY(nsk_jvmti_enableEvents(JVMTI_ENABLE, 1, &event, NULL))) {
    return;
  }
  if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout))) {
    return;
  }

  NSK_DISPLAY0("Tagging fields.\n");
  if (!NSK_VERIFY(JNI_OK == tag_objects(jvmti, jni))) {
    return;
  }

  memset(&primitive_callbacks, 0, sizeof(jvmtiHeapCallbacks));
  primitive_callbacks.primitive_field_callback = &field_callback;
  primitive_callbacks.array_primitive_value_callback = &array_callback;
  primitive_callbacks.string_primitive_value_callback = &string_callback;
  primitive_callbacks.heap_iteration_callback = &heap_callback;

  NSK_DISPLAY0("Iterating over reachable objects.\n");
  if (!NSK_JVMTI_VERIFY(jvmti->IterateThroughHeap(0, klass, &primitive_callbacks, NULL))) {
    nsk_jvmti_setFailStatus();
    return;
  }

  NSK_DISPLAY0("Verifying that all filds were found.\n");
  verify_objects();

  if (!NSK_VERIFY(nsk_jvmti_resumeSync())) {
    return;
  }

  if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout))) {
    return;
  }

  NSK_DISPLAY0("Iterating over unreachable objects.\n");
  if (!NSK_JVMTI_VERIFY(jvmti->IterateThroughHeap(0, klass, &primitive_callbacks, NULL))) {
    nsk_jvmti_setFailStatus();
    return;
  }

  NSK_DISPLAY0("Verifying that all filds were found.\n");
  verify_objects();

  if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
    return;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ConcreteKlassFilter(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ConcreteKlassFilter(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ConcreteKlassFilter(JavaVM *jvm, char *options, void *reserved) {
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
  event_callbacks.ObjectFree = &object_free_callback;
  if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&event_callbacks, sizeof(jvmtiEventCallbacks)))) {
    return JNI_ERR;
  }

  if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agent, NULL))) {
    return JNI_ERR;
  }

  return JNI_OK;
}

}
