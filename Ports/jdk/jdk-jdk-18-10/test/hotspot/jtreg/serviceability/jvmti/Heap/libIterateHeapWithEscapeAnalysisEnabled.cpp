/*
 * Copyright (c) 2020 SAP SE. All rights reserved.
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
#include "jni.h"

extern "C" {

#define FAILED -1
#define OK      0

static jvmtiEnv *jvmti;

static jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved);

static void ShowErrorMessage(jvmtiEnv *jvmti, jvmtiError errCode, const char *message) {
  char *errMsg;
  jvmtiError result;

  result = jvmti->GetErrorName(errCode, &errMsg);
  if (result == JVMTI_ERROR_NONE) {
    fprintf(stderr, "%s: %s (%d)\n", message, errMsg, errCode);
    jvmti->Deallocate((unsigned char *)errMsg);
  } else {
    fprintf(stderr, "%s (%d)\n", message, errCode);
  }
}

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
  return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT jint JNICALL
Agent_OnAttach(JavaVM *jvm, char *options, void *reserved) {
  return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *jvm, void *reserved) {
  jint res;
  JNIEnv *env;

  res = jvm->GetEnv((void **) &env, JNI_VERSION_9);
  if (res != JNI_OK || env == NULL) {
    fprintf(stderr, "Error: GetEnv call failed(%d)!\n", res);
    return JNI_ERR;
  }

  return JNI_VERSION_9;
}

static jint
Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
  jint res;

  printf("Agent_OnLoad started\n");

  res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_9);
  if (res != JNI_OK || jvmti == NULL) {
    fprintf(stderr, "Error: wrong result of a valid call to GetEnv!\n");
    return JNI_ERR;
  }

  printf("Agent_OnLoad finished\n");
  return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_IterateHeapWithEscapeAnalysisEnabled_acquireCanTagObjectsCapability(JNIEnv *env, jclass cls) {
  jvmtiError err;
  jvmtiCapabilities caps;

  memset(&caps, 0, sizeof(caps));

  caps.can_tag_objects = 1;

  err = jvmti->AddCapabilities(&caps);
  if (err != JVMTI_ERROR_NONE) {
    ShowErrorMessage(jvmti, err,
                     "acquireCanTagObjectsCapability: error in JVMTI AddCapabilities");
    return JNI_ERR;
  }

  err = jvmti->GetCapabilities(&caps);
  if (err != JVMTI_ERROR_NONE) {
    ShowErrorMessage(jvmti, err,
                     "acquireCanTagObjectsCapability: error in JVMTI GetCapabilities");
    return JNI_ERR;
  }

  if (!caps.can_tag_objects) {
    fprintf(stderr, "Warning: didn't get the capability can_tag_objects\n");
    return JNI_ERR;
  }

  return JNI_OK;
}

static jobject method_IterateOverReachableObjects;
static jobject method_IterateOverHeap;
static jobject method_IterateOverInstancesOfClass;
static jobject method_FollowReferences;
static jobject method_IterateThroughHeap;

JNIEXPORT jint JNICALL
Java_IterateHeapWithEscapeAnalysisEnabled_registerMethod(JNIEnv *env, jclass cls, jobject method, jstring name) {
  const char *name_chars = env->GetStringUTFChars(name, 0);
  int rc = FAILED;
  if (rc != OK && strcmp(name_chars, "IterateOverReachableObjects") == 0) {
    method_IterateOverReachableObjects = env->NewGlobalRef(method);
    rc = OK;
  }
  if (rc != OK && strcmp(name_chars, "IterateOverHeap") == 0) {
    method_IterateOverHeap = env->NewGlobalRef(method);
    rc = OK;
  }
  if (rc != OK && strcmp(name_chars, "IterateOverInstancesOfClass") == 0) {
    method_IterateOverInstancesOfClass = env->NewGlobalRef(method);
    rc = OK;
  }
  if (rc != OK && strcmp(name_chars, "IterateThroughHeap") == 0) {
    method_IterateThroughHeap = env->NewGlobalRef(method);
    rc = OK;
  }
  if (rc != OK && strcmp(name_chars, "FollowReferences") == 0) {
    method_FollowReferences = env->NewGlobalRef(method);
    rc = OK;
  }
  env->ReleaseStringUTFChars(name, name_chars);
  return rc;
}

JNIEXPORT void JNICALL
Java_IterateHeapWithEscapeAnalysisEnabled_agentTearDown(JNIEnv *env, jclass cls) {
  env->DeleteGlobalRef(method_IterateOverReachableObjects);
  env->DeleteGlobalRef(method_IterateOverHeap);
  env->DeleteGlobalRef(method_IterateOverInstancesOfClass);
  env->DeleteGlobalRef(method_FollowReferences);
  env->DeleteGlobalRef(method_IterateThroughHeap);
}

JNIEXPORT jint JNICALL
Java_IterateHeapWithEscapeAnalysisEnabled_jvmtiTagClass(JNIEnv *env, jclass cls, jclass clsToTag, jlong tag) {
  jvmtiError err;
  err = jvmti->SetTag(clsToTag, tag);
  if (err != JVMTI_ERROR_NONE) {
    ShowErrorMessage(jvmti, err,
                     "jvmtiTagClass: error in JVMTI SetTag");
    return FAILED;
  }
  return OK;
}

typedef struct Tag_And_Counter {
  jlong instance_counter;
  jlong class_tag;
  jlong instance_tag;
} Tag_And_Counter;

static jvmtiIterationControl JNICALL
__stackReferenceCallback(jvmtiHeapRootKind root_kind,
                       jlong class_tag,
                       jlong size,
                       jlong* tag_ptr,
                       jlong thread_tag,
                       jint depth,
                       jmethodID method,
                       jint slot,
                         void* d) {
  Tag_And_Counter* data = (Tag_And_Counter*) d;
  if (class_tag == data->class_tag && *tag_ptr == 0) {
    data->instance_counter++;
    *tag_ptr = data->instance_tag;
  }
  return JVMTI_ITERATION_CONTINUE;
}

static jvmtiIterationControl JNICALL
__jvmtiHeapObjectCallback(jlong class_tag, jlong size, jlong* tag_ptr, void* d) {
  Tag_And_Counter* data = (Tag_And_Counter*) d;
  if (class_tag == data->class_tag && *tag_ptr == 0) {
    data->instance_counter++;
    *tag_ptr = data->instance_tag;
  }
  return JVMTI_ITERATION_CONTINUE;
}

static jint JNICALL
__jvmtiHeapReferenceCallback(jvmtiHeapReferenceKind reference_kind,
                             const jvmtiHeapReferenceInfo* reference_info,
                             jlong class_tag,
                             jlong referrer_class_tag,
                             jlong size,
                             jlong* tag_ptr,
                             jlong* referrer_tag_ptr,
                             jint length,
                             void* d) {
  Tag_And_Counter* data = (Tag_And_Counter*) d;
  if (class_tag == data->class_tag && *tag_ptr == 0) {
    data->instance_counter++;
    *tag_ptr = data->instance_tag;
  }
  return JVMTI_VISIT_OBJECTS;
}

static jint JNICALL
__jvmtiHeapIterationCallback(jlong class_tag,
                             jlong size,
                             jlong* tag_ptr,
                             jint length,
                             void* d) {
  Tag_And_Counter* data = (Tag_And_Counter*) d;
  if (class_tag == data->class_tag && *tag_ptr == 0) {
    data->instance_counter++;
    *tag_ptr = data->instance_tag;
  }
  return JVMTI_VISIT_OBJECTS;
}


JNIEXPORT jlong JNICALL
Java_IterateHeapWithEscapeAnalysisEnabled_countAndTagInstancesOfClass(JNIEnv *env,
                                                                      jclass cls,
                                                                      jclass tagged_class,
                                                                      jlong cls_tag,
                                                                      jlong instance_tag,
                                                                      jobject method) {
  jvmtiError err;
  jvmtiHeapCallbacks callbacks = {};
  Tag_And_Counter data = {0, cls_tag, instance_tag};
  jboolean method_found = JNI_FALSE;

  jint idx = 0;

  if (env->IsSameObject(method, method_IterateOverReachableObjects)) {
    method_found = JNI_TRUE;
    err = jvmti->IterateOverReachableObjects(NULL /*jvmtiHeapRootCallback*/,
                                             __stackReferenceCallback,
                                             NULL /* jvmtiObjectReferenceCallback */,
                                             &data);
    if (err != JVMTI_ERROR_NONE) {
      ShowErrorMessage(jvmti, err,
                       "countAndTagInstancesOfClass: error in JVMTI IterateOverReachableObjects");
      return FAILED;
    }
  }
  if (env->IsSameObject(method, method_IterateOverHeap)) {
    method_found = JNI_TRUE;
    err = jvmti->IterateOverHeap(JVMTI_HEAP_OBJECT_EITHER,
                                 __jvmtiHeapObjectCallback,
                                 &data);
    if (err != JVMTI_ERROR_NONE) {
      ShowErrorMessage(jvmti, err,
                       "countAndTagInstancesOfClass: error in JVMTI IterateOverHeap");
      return FAILED;
    }
  }
  if (env->IsSameObject(method, method_IterateOverInstancesOfClass)) {
    method_found = JNI_TRUE;
    err = jvmti->IterateOverInstancesOfClass(tagged_class,
                                             JVMTI_HEAP_OBJECT_EITHER,
                                             __jvmtiHeapObjectCallback,
                                             &data);
    if (err != JVMTI_ERROR_NONE) {
      ShowErrorMessage(jvmti, err,
                       "countAndTagInstancesOfClass: error in JVMTI IterateOverHeap");
      return FAILED;
    }
  }
  if (env->IsSameObject(method, method_FollowReferences)) {
    method_found = JNI_TRUE;
    callbacks.heap_reference_callback = __jvmtiHeapReferenceCallback;
    err = jvmti->FollowReferences(0 /* filter nothing */,
                                  NULL /* no class filter */,
                                  NULL /* no initial object, follow roots */,
                                  &callbacks,
                                  &data);
    if (err != JVMTI_ERROR_NONE) {
      ShowErrorMessage(jvmti, err,
                       "countAndTagInstancesOfClass: error in JVMTI FollowReferences");
      return FAILED;
    }
  }
  if (env->IsSameObject(method, method_IterateThroughHeap)) {
    method_found = JNI_TRUE;
    callbacks.heap_iteration_callback = __jvmtiHeapIterationCallback;
    err = jvmti->IterateThroughHeap(0 /* filter nothing */,
                                    NULL /* no class filter */,
                                    &callbacks,
                                    &data);
    if (err != JVMTI_ERROR_NONE) {
      ShowErrorMessage(jvmti, err,
                       "countAndTagInstancesOfClass: error in JVMTI IterateThroughHeap");
      return FAILED;
    }
  }

  if (!method_found) {
    fprintf(stderr, "countAndTagInstancesOfClass: unknown method\n");
    return FAILED;
  }

  return data.instance_counter;
}

JNIEXPORT jlong JNICALL
Java_IterateHeapWithEscapeAnalysisEnabled_getObjectsWithTag(JNIEnv *env,
                                                            jclass cls,
                                                            jlong tag,
                                                            jobjectArray res_instances) {
  jvmtiError  err;
  const jlong tags[1] = {tag};
  jint        res_count = -1;
  jobject*    res_instances_raw;
  jlong*      res_tags;
  jint        res_instances_length;
  jint        idx;

  err = jvmti->GetObjectsWithTags(1,
                                  tags,
                                  &res_count,
                                  &res_instances_raw,
                                  &res_tags);
  if (err != JVMTI_ERROR_NONE) {
    ShowErrorMessage(jvmti, err,
                     "getObjectsWithTags: error in JVMTI GetObjectsWithTags");
    return FAILED;
  }

  res_instances_length = env->GetArrayLength(res_instances);
  if (res_count != res_instances_length) {
    fprintf(stderr, "getObjectsWithTags: result array lenght (%d) does not match instance count returned by GetObjectsWithTags (%d) \n",
            res_instances_length, res_count);
    return FAILED;
  }

  for (idx = 0; idx < res_count; idx++) {
    env->SetObjectArrayElement(res_instances, idx, res_instances_raw[idx]);
  }

  jvmti->Deallocate((unsigned char *)res_instances_raw);
  jvmti->Deallocate((unsigned char *)res_tags);

  return OK;
}

}
