/*
 * Copyright (c) 2019 SAP SE and/or its affiliates. All rights reserved.
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

static jvmtiEnv *jvmti = NULL;

static const char* fields[] = { "Z", "B", "C", "S", "I", "J", "F", "D" };
#define NUM_FIELDS (sizeof fields / sizeof fields[0])
static jfieldID fieldIDs[NUM_FIELDS];
static jlong fieldAccessCount = 0;


JNIEXPORT jboolean JNICALL Java_FastGetField_initFieldIDs(JNIEnv *env, jobject this, jclass c) {
  for (int i = 0; i < (int)NUM_FIELDS; ++i) {
    fieldIDs[i] = (*env)->GetFieldID(env, c, fields[i], fields[i]);
    if (fieldIDs[i] == NULL) {
      printf("field %d not found\n", i);
      return JNI_FALSE;
    }
  }
  return JNI_TRUE;
}


JNIEXPORT jboolean JNICALL Java_FastGetField_initWatchers(JNIEnv *env, jobject this, jclass c) {
  if (jvmti == NULL) {
    printf("jvmti is NULL\n");
    return JNI_FALSE;
  }

  for (int i = 0; i < (int)NUM_FIELDS; ++i) {
    jvmtiError err = (*jvmti)->SetFieldAccessWatch(jvmti, c, fieldIDs[i]);
    if (err != JVMTI_ERROR_NONE) {
      printf("SetFieldAccessWatch failed with error %d\n", err);
      return JNI_FALSE;
    }
  }

  return JNI_TRUE;
}


JNIEXPORT jlong JNICALL Java_FastGetField_accessFields(JNIEnv *env, jobject this, jobject obj) {
  return
      (*env)->GetBooleanField(env, obj, fieldIDs[0]) +
      (*env)->GetByteField(env, obj, fieldIDs[1]) +
      (*env)->GetCharField(env, obj, fieldIDs[2]) +
      (*env)->GetShortField(env, obj, fieldIDs[3]) +
      (*env)->GetIntField(env, obj, fieldIDs[4]) +
      (*env)->GetLongField(env, obj, fieldIDs[5]) +
      (jlong)((*env)->GetFloatField(env, obj, fieldIDs[6])) +
      (jlong)((*env)->GetDoubleField(env, obj, fieldIDs[7]));
}


JNIEXPORT jlong JNICALL Java_FastGetField_getFieldAccessCount(JNIEnv *env, jclass c) {
  return fieldAccessCount;
}


static void JNICALL onFieldAccess(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                                  jmethodID method, jlocation location, jclass field_klass,
                                  jobject object, jfieldID field) {
  char *fname = NULL, *mname = NULL;

  jvmtiError err = (*jvmti)->GetFieldName(jvmti, field_klass, field, &fname, NULL, NULL);
  if (err != JVMTI_ERROR_NONE) {
    printf("GetFieldName failed with error %d\n", err);
    return;
  }

  err = (*jvmti)->GetMethodName(jvmti, method, &mname, NULL, NULL);
  if (err != JVMTI_ERROR_NONE) {
    printf("GetMethodName failed with error %d\n", err);
    return;
  }

  printf("%s accessed field %s\n", mname, fname);

  err = (*jvmti)->Deallocate(jvmti, (unsigned char*)fname);
  if (err != JVMTI_ERROR_NONE) {
    printf("Deallocate failed with error %d\n", err);
    return;
  }

  err = (*jvmti)->Deallocate(jvmti, (unsigned char*)mname);
  if (err != JVMTI_ERROR_NONE) {
    printf("Deallocate failed with error %d\n", err);
    return;
  }

  fieldAccessCount++;
}


JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM* vm, char* options, void* reserved) {
  jvmtiCapabilities capa;
  jvmtiEventCallbacks cbs = {0};

  (*vm)->GetEnv(vm, (void**)&jvmti, JVMTI_VERSION_1_0);

  memset(&capa, 0, sizeof(capa));
  capa.can_generate_field_access_events = 1;
  (*jvmti)->AddCapabilities(jvmti, &capa);

  cbs.FieldAccess = &onFieldAccess;
  (*jvmti)->SetEventCallbacks(jvmti, &cbs, sizeof(cbs));
  (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_FIELD_ACCESS, NULL);
  printf("Loaded agent\n");
  fflush(stdout);

  return 0;
}
