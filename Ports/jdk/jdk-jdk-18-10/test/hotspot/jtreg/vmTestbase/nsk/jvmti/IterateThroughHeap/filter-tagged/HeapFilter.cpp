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

#define STATIC_FIELD 0x0008

/*
  For this test tags have following format:
  |63       48|      32|     16|      0|
  |<not used> |tag type|obj idx|fld idx|
*/

#define FIELD_TAG 1
#define OBJECT_TAG 2
#define CLASS_TAG 4

#define ENCODE_TAG(type, obj, fld) (((jlong)type)<<32 | ((jlong)obj)<<16 | (jlong)fld)
#define DECODE_TYPE(tag) tag>>32
#define DECODE_OBJECT(tag) ((tag>>16)&0xFFFF)
#define DECODE_FIELD(tag) (tag&0xFFFF)

#define TEST_OBJECTS_COUNT 2
#define TAGGED_OBJECTS 1

static long timeout = 0;
static int filter_type = -1;

// by default at least one object will be reported regardless to filter type
static int expected_object_count = 1;
static int reported_objects = 0;

//expected values
#define INT_ARRAY_LENGTH 2

static jint POISON = 0x1234;
static jint TAGGED_STATIC_INT_VALUE = 0xC0DE01 + POISON;
static jint TAGGED_INT_VALUE = 0xC0DE02 + POISON;
static jint UNTAGGED_STATIC_INT_VALUE = 0xC0DE03 + POISON;
static jint UNTAGGED_INT_VALUE = 0xC0DE04 + POISON;
static jint TAGGED_INT_ARRAY_VALUE[] = { 0xC0DE01, 0xC0DE01 + 1 };
static jint UNTAGGED_INT_ARRAY_VALUE[] = { 0xC0DE03, 0xC0DE03 + 1 };
static const wchar_t *TAGGED_STRING_VALUE = L"I'm a tagged string";
static const wchar_t *UNTAGGED_STRING_VALUE = L"I'm an untagged string";

//kind of field
#define TYPE_FIELD 1
#define TYPE_ARRAY 2
#define TYPE_STRING 4

//field info
typedef struct {
  char *name;
  char *signature;
  int found;
  int collected;
  int primitive;
  int expected;
  int type;
  void *value;
  int size;
} field_info_t;

//object info
typedef struct {
  char *name;
  jint fields_count;
  field_info_t *fields;
  int collected;
} object_info_t;

static object_info_t objects_info[TEST_OBJECTS_COUNT];

#define className "nsk/jvmti/IterateThroughHeap/filter_tagged/HeapFilter"
#define fieldName "testObjects"
#define fieldSig "[Ljava/lang/Object;"
#define STRING_SIGNATURE "Ljava/lang/String;"
#define INT_ARRAY_SIGNATURE "[I"

// Check if the signature is signature of primitive type.
jboolean is_primitive_type(const char *signature) {
  if (!strcmp(signature,"C")
      || !strcmp(signature, "B")
      || !strcmp(signature, "S")
      || !strcmp(signature, "I")
      || !strcmp(signature, "J")
      || !strcmp(signature, "F")
      || !strcmp(signature, "D")
      || !strcmp(signature, "Z"))
    return JNI_TRUE;
  return JNI_FALSE;
}

//check tag values accoring to heap filter choosed for test
jboolean verify_tag(jlong class_tag, jlong object_tag) {
  switch (filter_type) {
  case JVMTI_HEAP_FILTER_TAGGED:
    return object_tag == 0;
  case JVMTI_HEAP_FILTER_UNTAGGED:
    return object_tag != 0;
  case JVMTI_HEAP_FILTER_CLASS_TAGGED:
    return class_tag == 0;
  case JVMTI_HEAP_FILTER_CLASS_UNTAGGED:
    return class_tag != 0;
  default:
    return JNI_FALSE;
  }
}

//check whether or not field expected to be reported
jboolean occurance_expected(int tagged, int is_static, int is_primitive) {
  switch (filter_type) {
  case JVMTI_HEAP_FILTER_TAGGED:
    return !tagged;
  case JVMTI_HEAP_FILTER_UNTAGGED:
    return tagged;
  case JVMTI_HEAP_FILTER_CLASS_TAGGED:
    return (is_static && is_primitive) || !is_primitive || !tagged;
  case JVMTI_HEAP_FILTER_CLASS_UNTAGGED:
    return !is_static && is_primitive && tagged;
  default:
    return JNI_ERR;
  }
}

jint JNICALL field_callback(jvmtiHeapReferenceKind kind,
                            const jvmtiHeapReferenceInfo* info,
                            jlong object_class_tag,
                            jlong* object_tag_ptr,
                            jvalue value,
                            jvmtiPrimitiveType value_type,
                            void* user_data) {
  int object;
  int field;
  if (!NSK_VERIFY(verify_tag(object_class_tag, *object_tag_ptr))) {
    nsk_jvmti_setFailStatus();
  }

  //iterate over all fields found during tagging and compare reported value
  //with their values.
  if (value_type != JVMTI_PRIMITIVE_TYPE_INT)
    return 0;
  for (object = 0; object < TEST_OBJECTS_COUNT; object++) {
    for (field = 0; field < objects_info[object].fields_count; field++) {
      if (objects_info[object].fields[field].type == TYPE_FIELD &&
         *(jint*)(objects_info[object].fields[field].value) == value.i) {
        objects_info[object].fields[field].found++;
      }
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
  int object;
  int field;
  if (!NSK_VERIFY(verify_tag(class_tag, *tag_ptr))) {
    nsk_jvmti_setFailStatus();
  }
  for (object = 0; object < TEST_OBJECTS_COUNT; object++) {
    for (field = 0; field < objects_info[object].fields_count; field++) {
      if (objects_info[object].fields[field].type == TYPE_STRING &&
         value_length == objects_info[object].fields[field].size) {
        int matched = 1;
        int i;
        wchar_t *str = (wchar_t*)objects_info[object].fields[field].value;
        for (i = 0; i < value_length && matched; i++) {
          matched = (str[i] == value[i]);
        }
        if (matched)
          objects_info[object].fields[field].found++;
      }
    }
  }
  return 0;
}


jint JNICALL array_callback(jlong class_tag,
                            jlong size,
                            jlong* tag_ptr,
                            jint element_count,
                            jvmtiPrimitiveType element_type,
                            const void* elements,
                            void* user_data) {
  int object;
  int field;
  if (!NSK_VERIFY(verify_tag(class_tag, *tag_ptr))) {
    nsk_jvmti_setFailStatus();
  }
  for (object = 0; object < TEST_OBJECTS_COUNT; object++) {
    for (field = 0; field < objects_info[object].fields_count; field++) {
      if (objects_info[object].fields[field].type == TYPE_ARRAY &&
         element_count == objects_info[object].fields[field].size) {
        int matched = 1;
        int i;
        for (i = 0; i < element_count && matched; i++) {
          matched = ((jint*)objects_info[object].fields[field].value)[i] ==
            ((jint*)elements)[i];
        }
        if (matched)
          objects_info[object].fields[field].found++;
      }
    }
  }
  return 0;
}

jint JNICALL heap_callback(jlong class_tag,
                           jlong size,
                           jlong* tag_ptr,
                           jint length,
                           void* user_data) {
  if (!NSK_VERIFY(verify_tag(class_tag, *tag_ptr))) {
    NSK_COMPLAIN0("Tag values invalid for selected heap filter were passed "
                  "to jvmtiHeapIterationCallback.\n");
    NSK_COMPLAIN2("\tClass tag: 0x%lX;\n\tObject tag: 0x%lX.\n", class_tag, *tag_ptr);
    nsk_jvmti_setFailStatus();
  }
  reported_objects++;
  return 0;
}

JNIEXPORT void JNICALL
object_free_callback(jvmtiEnv* jvmti, jlong tag) {
  if (DECODE_TYPE(tag) == OBJECT_TAG) {
    objects_info[DECODE_OBJECT(tag)].collected = 1;
  } else if (DECODE_TYPE(tag) == FIELD_TAG) {
    objects_info[DECODE_OBJECT(tag)].fields[DECODE_FIELD(tag)].collected = 1;
  }
}

//set expected fields value according to it's type
void set_expected_value(field_info_t *field, int tagged, int is_static) {
  if (field->primitive) {
    field->size = (int) sizeof(jint);
    if (is_static) {
      field->value = (void*)(tagged ? &TAGGED_STATIC_INT_VALUE : &UNTAGGED_STATIC_INT_VALUE);
    } else {
      field->value = (void*)(tagged ? &TAGGED_INT_VALUE : &UNTAGGED_INT_VALUE);
    }
    field->type = TYPE_FIELD;
  } else if (0 == strcmp(field->signature,STRING_SIGNATURE)) {
    field->value = (void*)(tagged ? TAGGED_STRING_VALUE : UNTAGGED_STRING_VALUE);
    field->size = (int) wcslen((wchar_t*)field->value);
    field->type = TYPE_STRING;
  } else if (0 == strcmp(field->signature,INT_ARRAY_SIGNATURE)) {
    field->size = INT_ARRAY_LENGTH;
    field->value = (void*)(tagged ? TAGGED_INT_ARRAY_VALUE : UNTAGGED_INT_ARRAY_VALUE);
    field->type = TYPE_ARRAY;
  }
}

/**
 * Read array of test objects.
 * Tag each of these objects, their classes, non-primitive fields and non-primitive fields classes.
 */
int tag_objects(jvmtiEnv *jvmti, JNIEnv *jni) {
  jclass debugee;
  jfieldID testObjectsField;
  jobjectArray testObjects;
  int object;

  if (!NSK_VERIFY(NULL != (debugee = jni->FindClass(className))))
    return JNI_ERR;

  if (!NSK_VERIFY(NULL != (testObjectsField = jni->GetStaticFieldID(debugee, fieldName, fieldSig))))
    return JNI_ERR;

  if (!NSK_VERIFY(NULL != (testObjects = (jobjectArray)(jni->GetStaticObjectField(
          debugee, testObjectsField)))))
    return JNI_ERR;

  // For each of test objects tag every field
  for (object = 0; object<TEST_OBJECTS_COUNT; object++) {
    jobject target;
    jclass targetClass;
    jfieldID *targetFields;
    jint field;
    int tagged = object == 0;

    memset(&objects_info[object],0,sizeof(object_info_t));
    if (!NSK_VERIFY(NULL != (target = jni->GetObjectArrayElement(testObjects, object))))
      return JNI_ERR;

    if (!NSK_VERIFY(NULL != (targetClass = jni->GetObjectClass(target))))
      return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetClassSignature(targetClass, &(objects_info[object].name), NULL)))
      return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetClassFields(
            targetClass, &(objects_info[object].fields_count), &targetFields)))
      return JNI_ERR;

    objects_info[object].fields = (field_info_t*)calloc(objects_info[object].fields_count,sizeof(field_info_t));

    // Iterate over fields, collect info about it and tag non primitive fields.
    for (field = 0; field < objects_info[object].fields_count; field++) {
      jint modifiers;
      int is_static = 0;
      int is_primitive = 0;
      if (!NSK_JVMTI_VERIFY(jvmti->GetFieldName(targetClass,
                                                targetFields[field],
                                                &objects_info[object].fields[field].name,
                                                &objects_info[object].fields[field].signature,
                                                NULL)))
        return JNI_ERR;
      if (!NSK_JVMTI_VERIFY(jvmti->GetFieldModifiers(
              targetClass, targetFields[field], &modifiers))) {
        return JNI_ERR;
      }
      is_static = (modifiers & STATIC_FIELD) == STATIC_FIELD;
      if (is_primitive_type(objects_info[object].fields[field].signature)) {
        objects_info[object].fields[field].primitive = 1;
        is_primitive = 1;
        // Add POISON to all int fields to make the values opaque to the JIT compiler.
        if (is_static) {
          jint value = jni->GetStaticIntField(targetClass, targetFields[field]);
          jni->SetStaticIntField(targetClass, targetFields[field], value + POISON);
        } else {
          jint value = jni->GetIntField(target, targetFields[field]);
          jni->SetIntField(target, targetFields[field], value + POISON);
        }
      } else {
        jobject value;
        if (!NSK_JVMTI_VERIFY(jvmti->GetFieldModifiers(
                targetClass, targetFields[field], &modifiers))) {
          return JNI_ERR;
        }
        if (is_static) {
          if (!NSK_VERIFY(NULL != (value = jni->GetStaticObjectField(
                  targetClass, targetFields[field])))) {
            return JNI_ERR;
          }
        } else {
          if (!NSK_VERIFY(NULL != (value = jni->GetObjectField(target, targetFields[field])))) {
            return JNI_ERR;
          }
        }
        if (tagged) {
          if (!NSK_JVMTI_VERIFY(jvmti->SetTag(value, ENCODE_TAG(FIELD_TAG,object,field)))) {
            return JNI_ERR;
          }
        }
        jni->DeleteLocalRef(value);
      }

      objects_info[object].fields[field].expected =
        occurance_expected(tagged,is_static,is_primitive);
      expected_object_count +=
        objects_info[object].fields[field].expected && !is_primitive;
      set_expected_value(&objects_info[object].fields[field], tagged, is_static);
    }

    // tag class and it's instance to pass this tag into primitive field callback
    if (tagged) {
      if (!NSK_JVMTI_VERIFY(jvmti->SetTag(target, ENCODE_TAG(OBJECT_TAG,object,0))))
        return JNI_ERR;
      if (!NSK_JVMTI_VERIFY(jvmti->SetTag(targetClass, ENCODE_TAG(CLASS_TAG,object,0))))
        return JNI_ERR;
    }

    NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)targetFields));
    jni->DeleteLocalRef(target);
    jni->DeleteLocalRef(targetClass);
  }

  jni->DeleteLocalRef(testObjects);

  return JNI_OK;
}


//release resources allocated in tag_objects
void release_object_info(jvmtiEnv *jvmti, JNIEnv *jni) {
  int object;
  int field;
  for (object = 0; object < TEST_OBJECTS_COUNT; object++) {
    for (field = 0; field < objects_info[object].fields_count; field++) {
      jvmti->Deallocate((unsigned char*)objects_info[object].fields[field].name);
      jvmti->Deallocate((unsigned char*)objects_info[object].fields[field].signature);
    }
    jvmti->Deallocate((unsigned char*)objects_info[object].name);
    free(objects_info[object].fields);
  }
}

// Check that every field was found expected amount of times
void verify_objects(int reachable) {
  int object;
  int field;
  for (object = 0; object < (reachable ? TEST_OBJECTS_COUNT : TAGGED_OBJECTS); object++) {
    for (field = 0; field < objects_info[object].fields_count; field++) {
      // If primitive field of object that was not collected or
      // non primitive field that was not collected was not found
      // expected amount of times, than test failed.
      if ((objects_info[object].fields[field].primitive &&
           !objects_info[object].collected)
          ||
          (!objects_info[object].fields[field].primitive &&
           !objects_info[object].fields[field].collected)) {
        if (objects_info[object].fields[field].expected !=
            objects_info[object].fields[field].found) {
          NSK_COMPLAIN4("Field %s::%s expected to be found %d times, "
                        "but it was found %d times.\n",
                        objects_info[object].name,
                        objects_info[object].fields[field].name,
                        objects_info[object].fields[field].expected,
                        objects_info[object].fields[field].found);
          nsk_jvmti_setFailStatus();
        }
      }
      objects_info[object].fields[field].found = 0;
    }
  }
}

static void JNICALL
agent(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
  jvmtiEvent event = JVMTI_EVENT_OBJECT_FREE;
  jvmtiHeapCallbacks primitive_callbacks;
  jvmtiEventCallbacks event_callbacks;

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
  if (!NSK_JVMTI_VERIFY(jvmti->IterateThroughHeap(filter_type, NULL, &primitive_callbacks, NULL))) {
    nsk_jvmti_setFailStatus();
    return;
  }

  NSK_DISPLAY0("Verifying that all fields were found.\n");
  verify_objects(1);

  if (!NSK_VERIFY(nsk_jvmti_resumeSync())) {
    return;
  }

  if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout))) {
    return;
  }

  NSK_DISPLAY0("Iterating over unreachable objects.\n");
  if (!NSK_JVMTI_VERIFY(jvmti->IterateThroughHeap(filter_type, NULL, &primitive_callbacks, NULL))) {
    nsk_jvmti_setFailStatus();
    return;
  }

  NSK_DISPLAY0("Verifying that all fields were found.\n");
  verify_objects(0);

  /*
   * This is done to clear event_callbacks.ObjectFree before we call release_object_info(),
   * since it will free some memory that the callback will access.
   */
  memset(&event_callbacks, 0, sizeof(jvmtiEventCallbacks));
  if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&event_callbacks, sizeof(jvmtiEventCallbacks)))) {
    return;
  }

  release_object_info(jvmti, jni);

  if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
    return;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_HeapFilter(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_HeapFilter(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_HeapFilter(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
  jvmtiEnv *jvmti;
  jvmtiCapabilities caps;
  jvmtiEventCallbacks event_callbacks;
  const char *type;

  jvmti = nsk_jvmti_createJVMTIEnv(jvm, reserved);
  if (!NSK_VERIFY(jvmti != NULL)) {
    return JNI_ERR;
  }

  nsk_jvmti_parseOptions(options);

  type = nsk_jvmti_findOptionValue("filter");
  if (type != NULL) {
    if (0 == strcmp(type, "JVMTI_HEAP_FILTER_TAGGED")) {
      filter_type = JVMTI_HEAP_FILTER_TAGGED;
    } else if (0 == strcmp(type, "JVMTI_HEAP_FILTER_UNTAGGED")) {
      filter_type = JVMTI_HEAP_FILTER_UNTAGGED;
    } else if (0 == strcmp(type, "JVMTI_HEAP_FILTER_CLASS_TAGGED")) {
      filter_type = JVMTI_HEAP_FILTER_CLASS_TAGGED;
    } else if (0 == strcmp(type, "JVMTI_HEAP_FILTER_CLASS_UNTAGGED")) {
      filter_type = JVMTI_HEAP_FILTER_CLASS_UNTAGGED;
    } else {
      NSK_COMPLAIN1("unknown filter value '%s'.\n",type);
      return JNI_ERR;
    }
  } else {
    NSK_COMPLAIN0("filter option shound be presented.\n");
    return JNI_ERR;
  }

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
