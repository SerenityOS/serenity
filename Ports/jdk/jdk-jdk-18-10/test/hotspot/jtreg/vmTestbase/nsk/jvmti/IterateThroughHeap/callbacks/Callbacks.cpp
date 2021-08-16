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

///amount of objects to be tagged
#define TEST_OBJECTS_COUNT 4

///expected amount of times object will bre ported by callbacks
#define PRIMITIVE_OCCURANCE_COUNT 1
#define STRING_OCCURANCE_COUNT 2
#define PRIMITIVE_ARRAY_OCCURANCE_COUNT 2
#define NONPRIMITIVE_OCCURANCE_COUNT 1


/**
  tag format

  63      35   32      16         0
  |1       |type|obj idx|field idx|

*/

#define TAG_TYPE_PRIMITIVE 0
#define TAG_TYPE_STRING 1
#define TAG_TYPE_ARRAY 2
#define TAG_TYPE_OBJECT 3

#define ENCODE_TAG(type,obj,fld) (0x8000000000000000ULL|(((jlong)type)<<32)|(((jlong)obj)<<16)|fld)

#define DECODE_TYPE(tag) ((tag>>32)&0x0FFFF)
#define DECODE_OBJECT(tag) ((tag>>16)&0x0FFFF)
#define DECODE_FIELD(tag) (tag&0x0FFFF)

///expected values
#define BOOLEAN JNI_FALSE
#define BYTE 0xB
#define CHAR 'z'
#define SHORT 0xB00
#define INT ((int)0xDEADBEEF)
#define LONG 0xDEADBEEFDEADLL
#define FLOAT 3.1416f
#define DOUBLE 3.14159265

#define ARRAY_LENGTH 5

static const wchar_t *STRING = L"I hope you'll find me in the heap!";
static jboolean BOOLEAN_ARRAY[] = { JNI_TRUE, JNI_TRUE, JNI_FALSE, JNI_TRUE, JNI_FALSE };
static jbyte BYTE_ARRAY[] = { BYTE, BYTE+1, BYTE+2, BYTE+3, BYTE+4 };
static jchar CHAR_ARRAY[] = { CHAR, CHAR+1, CHAR+2, CHAR+3, CHAR+4 };
static jshort SHORT_ARRAY[] = { SHORT, SHORT+1, SHORT+2, SHORT+3, SHORT+4 };
static jint INT_ARRAY[] = { INT, INT+1, INT+2, INT+3, INT+4 };
static jlong LONG_ARRAY[] = { LONG, LONG+1, LONG+2, LONG+3, LONG+4 };
static jfloat FLOAT_ARRAY[] = { FLOAT, FLOAT+1, FLOAT+2, FLOAT+3, FLOAT+4 };
static jdouble DOUBLE_ARRAY[] = { DOUBLE, DOUBLE+1, DOUBLE+2, DOUBLE+3, DOUBLE+4 };

static long timeout = 0;

///information about field
typedef struct {
  char *name;
  char *signature;
  int found;
  int collected;
  int primitive;
} field_info_t;

///information about object
typedef struct {
  char *name;
  jint fields_count;
  field_info_t *fields;
  int collected;
} object_info_t;

static object_info_t objects_info[TEST_OBJECTS_COUNT];

#define className "nsk/jvmti/IterateThroughHeap/callbacks/Callbacks"
#define fieldName "testObjects"
#define fieldSig "[Ljava/lang/Object;"

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

// For given signature find expected tag type
int get_tag_type(const char *signature) {
  if (is_primitive_type(signature)) {
    return TAG_TYPE_PRIMITIVE;
  } else if (signature[0] == '[' && is_primitive_type(signature+1)) {
    return TAG_TYPE_ARRAY;
  } else if (!strcmp(signature, "Ljava/lang/String;")) {
    return TAG_TYPE_STRING;
  } else {
    return TAG_TYPE_OBJECT;
  }
}

/**
  * Check value corectness accordning to it's type.
  * Returns 0 if value matched with expected.
  */
jboolean verify_value(jvalue value, jvmtiPrimitiveType type) {
  switch (type) {
  case JVMTI_PRIMITIVE_TYPE_BOOLEAN:
    return value.z == BOOLEAN;
  case JVMTI_PRIMITIVE_TYPE_BYTE:
    return value.b == BYTE;
  case JVMTI_PRIMITIVE_TYPE_CHAR:
    return value.c == CHAR;
  case JVMTI_PRIMITIVE_TYPE_SHORT:
    return value.s == SHORT;
  case JVMTI_PRIMITIVE_TYPE_INT:
    return value.i == INT;
  case JVMTI_PRIMITIVE_TYPE_LONG:
    return value.j == LONG;
  case JVMTI_PRIMITIVE_TYPE_FLOAT:
    return value.f == FLOAT;
  case JVMTI_PRIMITIVE_TYPE_DOUBLE:
    return value.d == DOUBLE;
  default:
    NSK_COMPLAIN1("Unknown type: %X.",type);
    return JNI_FALSE;
  }
}

// Check that array values are correct depending on type of elements
jboolean verify_array(const void *array, jvmtiPrimitiveType type, jint length) {
  void *expected_array;
  switch (type) {
  case JVMTI_PRIMITIVE_TYPE_BOOLEAN:
    expected_array = (void*)BOOLEAN_ARRAY;
    break;
  case JVMTI_PRIMITIVE_TYPE_CHAR:
    expected_array = (void*)CHAR_ARRAY;
    break;
  case JVMTI_PRIMITIVE_TYPE_BYTE:
    expected_array = (void*)BYTE_ARRAY;
    break;
  case JVMTI_PRIMITIVE_TYPE_SHORT:
    expected_array = (void*)SHORT_ARRAY;
    break;
  case JVMTI_PRIMITIVE_TYPE_INT:
    expected_array = (void*)INT_ARRAY;
    break;
  case JVMTI_PRIMITIVE_TYPE_LONG:
    expected_array = (void*)LONG_ARRAY;
    break;
  case JVMTI_PRIMITIVE_TYPE_FLOAT:
    expected_array = (void*)FLOAT_ARRAY;
    break;
  case JVMTI_PRIMITIVE_TYPE_DOUBLE:
    expected_array = (void*)DOUBLE_ARRAY;
    break;
  default:
    NSK_COMPLAIN0("Unexpected type of array's elements.\n");
    return JNI_FALSE;
  }
  return memcmp(expected_array,array,length) == 0;
}

jint JNICALL field_callback(jvmtiHeapReferenceKind kind,
                            const jvmtiHeapReferenceInfo* info,
                            jlong object_class_tag,
                            jlong* object_tag_ptr,
                            jvalue value,
                            jvmtiPrimitiveType value_type,
                            void* user_data) {
  jlong tag;
  tag = *object_tag_ptr;

  // skip all non-tagged fields as well as fields of tagged objects
  if (tag == 0 ||
      DECODE_TYPE(tag) == TAG_TYPE_OBJECT ||
      DECODE_TYPE(tag) == TAG_TYPE_STRING) {
    return 0;
  } else if (DECODE_TYPE(tag) != TAG_TYPE_PRIMITIVE) {
    NSK_COMPLAIN3("jvmtiPrimitiveFieldCallback was invoked for an object with "
                  "non-primitive field tag (0x%lX) corresponging to %s::%s.\n",
                  DECODE_TYPE(tag),
                  objects_info[DECODE_OBJECT(tag)].name,
                  objects_info[DECODE_OBJECT(tag)].fields[DECODE_FIELD(tag)].name);
    nsk_jvmti_setFailStatus();
    return 0;
  }

  objects_info[DECODE_OBJECT(tag)].fields[info->field.index].found++;

  if (!verify_value(value, value_type)) {
    NSK_COMPLAIN2("Field %s::%s has unexpected value.\n",
                  objects_info[DECODE_OBJECT(tag)].name,
                  objects_info[DECODE_OBJECT(tag)].fields[info->field.index].name);
    nsk_jvmti_setFailStatus();
  }
  return 0;
}

jint JNICALL string_callback(jlong class_tag,
                             jlong size,
                             jlong* tag_ptr,
                             const jchar* value,
                             jint value_length,
                             void* user_data) {
  int matched = 1;
  int i;

  //skip all untegged strings
  if (*tag_ptr == 0) {
    return 0;
  } else if (DECODE_TYPE(*tag_ptr) != TAG_TYPE_STRING) {
    NSK_COMPLAIN2("jvmtiStringPrimitiveValueCallback was invoked for an object "
                  "with non-string tag corresponding to %s::%s.\n",
                  objects_info[DECODE_OBJECT(*tag_ptr)].name,
                  objects_info[DECODE_OBJECT(*tag_ptr)].fields[DECODE_FIELD(*tag_ptr)].name);
    return 0;
  }

  objects_info[DECODE_OBJECT(*tag_ptr)].fields[DECODE_FIELD(*tag_ptr)].found++;

  //check that reported length is the same as expected
  if (value_length != (jint) wcslen(STRING)) {
    NSK_COMPLAIN4("Length of reported string %s::%s is %d while %d is expected.\n",
                  objects_info[DECODE_OBJECT(*tag_ptr)].name,
                  objects_info[DECODE_OBJECT(*tag_ptr)].fields[DECODE_FIELD(*tag_ptr)].name,
                  value_length,
                  wcslen(STRING));
    nsk_jvmti_setFailStatus();
    return 0;
  }

  //compare reported value with expected one
  for (i = 0; i<value_length && matched; i++) {
    matched = value[i] == STRING[i];
  }

  if (!matched) {
    NSK_COMPLAIN2("Value of field %s::%s has unexpected value.\n",
                  objects_info[DECODE_OBJECT(*tag_ptr)].name,
                  objects_info[DECODE_OBJECT(*tag_ptr)].fields[DECODE_FIELD(*tag_ptr)].name);

    nsk_jvmti_setFailStatus();
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
  //skip untegged arrays
  if (*tag_ptr == 0) {
    return 0;
  } else if (DECODE_TYPE(*tag_ptr) != TAG_TYPE_ARRAY) {
    NSK_COMPLAIN2("jvmtiArrayPrimitiveValueCallbak was invoked for object "
                  "with non-array tag corresponding to %s::%s.\n",
                  objects_info[DECODE_OBJECT(*tag_ptr)].name,
                  objects_info[DECODE_OBJECT(*tag_ptr)].fields[DECODE_FIELD(*tag_ptr)].name);
    nsk_jvmti_setFailStatus();
    return 0;
  }

  objects_info[DECODE_OBJECT(*tag_ptr)].fields[DECODE_FIELD(*tag_ptr)].found++;

  //check if reported length is the same as expected
  if (element_count != ARRAY_LENGTH) {
    NSK_COMPLAIN4("Length of array %s::%s is %d while %d is expected.\n",
                  objects_info[DECODE_OBJECT(*tag_ptr)].name,
                  objects_info[DECODE_OBJECT(*tag_ptr)].fields[DECODE_FIELD(*tag_ptr)].name,
                  element_count,
                  ARRAY_LENGTH);
    nsk_jvmti_setFailStatus();
  } else if (!verify_array(elements, element_type, element_count)) {
    //compare array with expected one
    NSK_COMPLAIN2("Value of field %s::%s has unexpected value.\n",
                  objects_info[DECODE_OBJECT(*tag_ptr)].name,
                  objects_info[DECODE_OBJECT(*tag_ptr)].fields[DECODE_FIELD(*tag_ptr)].name);
    nsk_jvmti_setFailStatus();
  }
  return 0;
}

jint JNICALL heap_callback(jlong class_tag,
                           jlong size,
                           jlong* tag_ptr,
                           jint length,
                           void* user_data) {
  //skip untagged objects
  if (*tag_ptr == 0) {
    return 0;
  }

  if (DECODE_TYPE(*tag_ptr) != TAG_TYPE_PRIMITIVE) {
    objects_info[DECODE_OBJECT(*tag_ptr)].fields[DECODE_FIELD(*tag_ptr)].found++;
  }

  return 0;
}

JNIEXPORT void JNICALL
object_free_callback(jvmtiEnv* jvmti, jlong tag) {
  if (DECODE_TYPE(tag) == TAG_TYPE_PRIMITIVE) {
    int object = DECODE_OBJECT(tag);
    objects_info[object].collected = 1;
    NSK_DISPLAY1("Object %s collected.\n",
                 objects_info[object].name);
  } else {
    int object = DECODE_OBJECT(tag);
    int field = DECODE_FIELD(tag);
    objects_info[object].fields[field].collected = 1;
    NSK_DISPLAY2("Field %s of intance of %s collected.\n",
                 objects_info[object].fields[field].name,
                 objects_info[object].name);
  }
}

/**
 * Read array of test objects.
 * Tag each of these objjects, their classes, non-primitive fields and non-primitive fields classes.
 * Each tag has following format:
 * In case when some class already tagged old tag is used.
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
      if (!NSK_JVMTI_VERIFY(jvmti->GetFieldName(targetClass,
                                                targetFields[field],
                                                &objects_info[object].fields[field].name,
                                                &objects_info[object].fields[field].signature,
                                                NULL)))
        return JNI_ERR;
      if (is_primitive_type(objects_info[object].fields[field].signature)) {
        objects_info[object].fields[field].primitive = 1;
      } else {
        jint modifiers;
        jobject value;
        int tag_type = get_tag_type(objects_info[object].fields[field].signature);
        if (!NSK_JVMTI_VERIFY(jvmti->GetFieldModifiers(
                targetClass, targetFields[field], &modifiers))) {
          return JNI_ERR;
        }
        if (modifiers & STATIC_FIELD) {
          if (!NSK_VERIFY(NULL != (value = jni->GetStaticObjectField(targetClass,
                                                                     targetFields[field])))) {
            return JNI_ERR;
          }
        } else {
          if (!NSK_VERIFY(NULL != (value = jni->GetObjectField(target, targetFields[field])))) {
            return JNI_ERR;
          }
        }
        //tag field's value
        if (!NSK_JVMTI_VERIFY(jvmti->SetTag(value, ENCODE_TAG(tag_type,object,field)))) {
          return JNI_ERR;
        }
        //remove local reference so object will have a chance to become unreachable
        jni->DeleteLocalRef((jobject)value);
      }
    }

    // tag class and it's instance to pass this tag into primitive field callback
    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(target, ENCODE_TAG(TAG_TYPE_PRIMITIVE,object,0))))
      return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(targetClass, ENCODE_TAG(TAG_TYPE_PRIMITIVE,object,0))))
      return JNI_ERR;

    NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)targetFields));

    jni->DeleteLocalRef((jobject)target);
    jni->DeleteLocalRef((jobject)targetClass);
  }

  jni->DeleteLocalRef((jobject)testObjects);

  return JNI_OK;
}

//free all resources allocated in tag_objects
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

// Check that every field was found expected amount of times and reset information about that.
void verify_objects() {
  int object;
  int field;
  for (object = 0; object < TEST_OBJECTS_COUNT; object++) {
    for (field = 0; field < objects_info[object].fields_count; field++) {
      // If primitive field of object that was not collected or
      // non primitive field that was not collected was not found
      // expected amount of times, than test failed.
      if ((objects_info[object].fields[field].primitive && !objects_info[object].collected)
          || !objects_info[object].fields[field].collected) {
        int expected = 0;
        switch (get_tag_type(objects_info[object].fields[field].signature)) {
        case TAG_TYPE_STRING:
          expected = STRING_OCCURANCE_COUNT;
          break;
        case TAG_TYPE_ARRAY:
          expected = PRIMITIVE_ARRAY_OCCURANCE_COUNT;
          break;
        case TAG_TYPE_PRIMITIVE:
          expected = PRIMITIVE_OCCURANCE_COUNT;
          break;
        default:
          expected = NONPRIMITIVE_OCCURANCE_COUNT;
          break;
        }
        if (expected != objects_info[object].fields[field].found) {
          NSK_COMPLAIN4("Field %s::%s expected to be found %d times, "
                        "but it was found %d times.\n",
                        objects_info[object].name,
                        objects_info[object].fields[field].name,
                        expected,
                        objects_info[object].fields[field].found);
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
  if (!NSK_JVMTI_VERIFY(jvmti->IterateThroughHeap(0, NULL, &primitive_callbacks, NULL))) {
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
  if (!NSK_JVMTI_VERIFY(jvmti->IterateThroughHeap(0, NULL, &primitive_callbacks, NULL))) {
    nsk_jvmti_setFailStatus();
    return;
  }

  NSK_DISPLAY0("Verifying that all filds were found.\n");
  verify_objects();

  if (!NSK_VERIFY(nsk_jvmti_enableEvents(JVMTI_DISABLE, 1, &event, NULL))) {
    return;
  }

  release_object_info(jvmti, jni);

  if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
    return;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_Callbacks(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_Callbacks(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_Callbacks(JavaVM *jvm, char *options, void *reserved) {
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
