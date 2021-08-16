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

#include <stdio.h>
#include <stdlib.h>
#include <memory.h>

#include "jni.h"
#include "jvmti.h"
#include "agent_common.h"

extern "C" {

static jvmtiEnv *jvmti;
static jint dummy_user_data;
static jboolean user_data_error_flag = JNI_FALSE;

/*
 * Default callbacks
 */

static jvmtiEventObjectFree object_free_callback;

static void JNICALL default_object_free(jvmtiEnv *env, jlong tag) {
    if (object_free_callback != NULL) {
        (*object_free_callback)(env, tag);
    }
}

static jvmtiIterationControl JNICALL default_heap_object_callback
    (jlong class_tag, jlong size, jlong* tag_ptr, void* user_data)
{
    if (user_data != &dummy_user_data && user_data_error_flag == JNI_FALSE) {
        user_data_error_flag = JNI_TRUE;
        fprintf(stderr, "WARNING: (default) unexpected value of user_data\n");
    }
    return JVMTI_ITERATION_ABORT;
}

static jvmtiHeapObjectCallback heap_object_callback = default_heap_object_callback;
static jvmtiHeapRootCallback heap_root_callback = NULL;
static jvmtiStackReferenceCallback stack_ref_callback = NULL;
static jvmtiObjectReferenceCallback object_ref_callback = NULL;

/*
 * Basic tagging functions
 */

JNIEXPORT jint JNICALL Java_nsk_share_jvmti_unit_Heap_setTag0
  (JNIEnv *env, jclass cls, jobject o, jlong tag)
{
    return jvmti->SetTag(o, tag);
}

JNIEXPORT jlong JNICALL Java_nsk_share_jvmti_unit_Heap_getTag0
   (JNIEnv *env, jclass cls, jobject o)
{
    jlong tag;
    jvmtiError err = jvmti->GetTag(o, &tag);
    if (err != JVMTI_ERROR_NONE) {
        fprintf(stderr, "ERORR: GetTag failed: JVMTI error=%d\n", err);
        return 0;
    }
    return tag;
}

JNIEXPORT jlong JNICALL Java_nsk_share_jvmti_unit_Heap_getObjectSize
    (JNIEnv *env, jclass cls, jobject o)
{
    jlong size;
    jvmtiError err = jvmti->GetObjectSize(o, &size);
    if (err != JVMTI_ERROR_NONE) {
        fprintf(stderr, "ERORR: GetObjectSize failed: JVMTI error=%d\n", err);
        return 0;
    }
    return size;
}

/*
 * Iterations functions
 */

JNIEXPORT jint JNICALL Java_nsk_share_jvmti_unit_Heap_iterateOverHeap0
   (JNIEnv *env, jclass cls, jint filter_kind)
{
    if (heap_object_callback == default_heap_object_callback) {
        fprintf(stderr, "WARNING: default heap_object_callback set\n");
    }
    user_data_error_flag = JNI_FALSE;
    return jvmti->IterateOverHeap(
        (jvmtiHeapObjectFilter) filter_kind, heap_object_callback, &dummy_user_data);
}

JNIEXPORT jint JNICALL Java_nsk_share_jvmti_unit_Heap_iterateOverInstancesOfClass0
   (JNIEnv *env, jclass this_cls, jclass target_cls, jint filter_kind)
{
    if (heap_object_callback == default_heap_object_callback) {
        fprintf(stderr, "WARNING: default heap_object_callback set\n");
    }
    user_data_error_flag = JNI_FALSE;
    return jvmti->IterateOverInstancesOfClass(target_cls,
        (jvmtiHeapObjectFilter) filter_kind, heap_object_callback, &dummy_user_data);
}

JNIEXPORT jint JNICALL Java_nsk_share_jvmti_unit_Heap_iterateOverReachableObjects0
    (JNIEnv *env, jclass this_cls)
{
    jvmtiError err;
    user_data_error_flag = JNI_FALSE;
    err = jvmti->IterateOverReachableObjects(heap_root_callback,
        stack_ref_callback, object_ref_callback, &dummy_user_data);
    if (err != JVMTI_ERROR_NONE) {
        fprintf(stderr, "IterateOverReachableObjects failed: jvmti error=%d", err);
    }
    return err;
}

JNIEXPORT jint JNICALL Java_nsk_share_jvmti_unit_Heap_iterateOverObjectsReachableFromObject0
    (JNIEnv *env, jclass this_cls, jobject o)
{
    jvmtiError err;
    user_data_error_flag = JNI_FALSE;
    err = jvmti->IterateOverObjectsReachableFromObject(o,
        object_ref_callback, &dummy_user_data);
    if (err != JVMTI_ERROR_NONE) {
        fprintf(stderr, "IterateOverObjectsReachableFromObject failed: jvmti error=%d", err);
    }
    return err;
}


/*
 * GetObjectsWithTags tests
 */

static jobject object_results_ref;
static jobject tag_results_ref;

JNIEXPORT jlongArray JNICALL Java_nsk_share_jvmti_unit_Heap_tagResults
    (JNIEnv *env, jclass this_cls)
{
    return (jlongArray)tag_results_ref;
}

JNIEXPORT jobjectArray JNICALL Java_nsk_share_jvmti_unit_Heap_objectResults
    (JNIEnv *env, jclass this_cls)
{
    return (jobjectArray)object_results_ref;
}

JNIEXPORT jint JNICALL Java_nsk_share_jvmti_unit_Heap_getObjectsWithTags
    (JNIEnv *env, jclass this_cls, jint count, jlongArray array)
{
    jlong *tags;
    jint i;
    jvmtiError err;
    jobject *object_results;
    jlong *tag_results;
    jclass cls;
    jobjectArray object_array;
    jlongArray tag_array;

    /* get rid of any arrays that we are holding from a previous call */

    if (object_results_ref != NULL) {
        env->DeleteGlobalRef(object_results_ref);
        object_results_ref = NULL;
    }
    if (tag_results_ref != NULL) {
        env->DeleteGlobalRef(tag_results_ref);
        tag_results_ref = NULL;
    }

    /* copy input list-of-tags from java into C */

    tags = (jlong*)malloc(count * sizeof(jlong));
    env->GetLongArrayRegion(array, 0, count, tags);

    err = jvmti->GetObjectsWithTags(count, tags,
                &count, &object_results, &tag_results);

    /* free the input argument that we malloced */
    free(tags);
    if (err != JVMTI_ERROR_NONE) {
        fprintf(stderr, "ERROR: GetObjectsWithTags failed: %d\n", err);
        return err;
    }

    /* copy output from C arrays into Java arrays */

    cls = env->FindClass("java/lang/Object");

    object_array = env->NewObjectArray(count, cls, NULL);
    tag_array = env->NewLongArray(count);

    for (i=0; i<count; i++) {
        env->SetObjectArrayElement(object_array, i, object_results[i]);
    }
    env->SetLongArrayRegion(tag_array, 0, count, tag_results);

    /* create JNI global refs as the current refs are local */

    object_results_ref = env->NewGlobalRef(object_array);
    tag_results_ref = env->NewGlobalRef(tag_array);

    jvmti->Deallocate((unsigned char*)object_results);
    jvmti->Deallocate((unsigned char*)tag_results);

    return count;
}

/************* Basic Iteration Tests **************/

static jint object_count;

static jvmtiIterationControl JNICALL tagged_object_count_callback
    (jlong class_tag, jlong size, jlong* tag_ptr, void* user_data)
{
    if (user_data != &dummy_user_data && user_data_error_flag == JNI_FALSE) {
        user_data_error_flag = JNI_TRUE;
        fprintf(stderr, "WARNING: (tagged) unexpected value of user_data\n");
    }
    if (*tag_ptr != 0) {
        object_count++;
    }
    return JVMTI_ITERATION_CONTINUE;
}

static jvmtiIterationControl JNICALL total_object_count_callback
    (jlong class_tag, jlong size, jlong* tag_ptr, void* user_data)
{
    if (user_data != &dummy_user_data && user_data_error_flag == JNI_FALSE) {
        user_data_error_flag = JNI_TRUE;
        fprintf(stderr, "WARNING: (total) unexpected value of user_data\n");
    }
    if (*tag_ptr != 0) {
        object_count++;
    }
    return JVMTI_ITERATION_CONTINUE;
}


JNIEXPORT void JNICALL Java_nsk_share_jvmti_unit_Heap_setTaggedObjectCountCallback
    (JNIEnv *env, jclass cls)
{
    heap_object_callback = tagged_object_count_callback;
    object_count = 0;
}

JNIEXPORT void JNICALL Java_nsk_share_jvmti_unit_Heap_setTotalObjectCountCallback
    (JNIEnv *env, jclass cls)
{
    heap_object_callback = total_object_count_callback;
    object_count = 0;
}

JNIEXPORT jint JNICALL Java_nsk_share_jvmti_unit_Heap_getObjectCount
    (JNIEnv *env, jclass cls)
{
    return object_count;
}

JNIEXPORT void JNICALL Java_nsk_share_jvmti_unit_Heap_zeroObjectCount
    (JNIEnv *env, jclass cls)
{
    object_count = 0;
}


/************* Basic Iteration Tests *************/

static jvmtiIterationControl JNICALL klass_tag_test_callback
    (jlong class_tag, jlong size, jlong* tag_ptr, void* user_data)
{
    if (user_data != &dummy_user_data && user_data_error_flag == JNI_FALSE) {
        user_data_error_flag = JNI_TRUE;
        fprintf(stderr, "WARNING: (klass) unexpected value of user_data\n");
    }
    if (class_tag != 0) {
        *tag_ptr = class_tag;
    }
    return JVMTI_ITERATION_CONTINUE;
}

JNIEXPORT void JNICALL Java_nsk_share_jvmti_unit_Heap_setKlassTagTestCallback
    (JNIEnv *env, jclass cls)
{
    heap_object_callback = klass_tag_test_callback;
}

/************* Heap Walking Tests *************/

JNIEXPORT jobject JNICALL Java_nsk_share_jvmti_unit_Heap_newGlobalRef
    (JNIEnv *env, jclass cls, jobject o)
{
    return env->NewGlobalRef(o);
}

static jvmtiIterationControl JNICALL simple_heap_root_callback
    (jvmtiHeapRootKind root_kind, jlong class_tag, jlong size,
    jlong* tag_ptr, void* user_data)
{
    if (user_data != &dummy_user_data && user_data_error_flag == JNI_FALSE) {
        user_data_error_flag = JNI_TRUE;
        fprintf(stderr, "WARNING: (heap) unexpected value of user_data\n");
    }
    *tag_ptr = root_kind;
    return JVMTI_ITERATION_CONTINUE;
}

static jvmtiIterationControl JNICALL simple_stack_ref_callback
    (jvmtiHeapRootKind root_kind, jlong class_tag, jlong size, jlong* tag_ptr,
     jlong thread_tag, jint depth, jmethodID method, jint slot, void* user_data)
{
    if (root_kind == JVMTI_HEAP_ROOT_STACK_LOCAL) {
        if (method == NULL) {
            fprintf(stderr, "WARNING: jmethodID missing for STACK_LOCAL\n");
        }
    }
    if (user_data != &dummy_user_data && user_data_error_flag == JNI_FALSE) {
        user_data_error_flag = JNI_TRUE;
        fprintf(stderr, "WARNING: (stack) unexpected value of user_data\n");
    }
    *tag_ptr = thread_tag;
    return JVMTI_ITERATION_CONTINUE;
}

static jvmtiIterationControl JNICALL simple_object_ref_callback
    (jvmtiObjectReferenceKind reference_kind, jlong class_tag, jlong size, jlong* tag_ptr,
     jlong referrer_tag, jint referrer_index, void* user_data)
{
    if (user_data != &dummy_user_data && user_data_error_flag == JNI_FALSE) {
        user_data_error_flag = JNI_TRUE;
        fprintf(stderr, "WARNING: (object) unexpected value of user_data\n");
    }
    *tag_ptr = 777;
    return JVMTI_ITERATION_CONTINUE;
}


JNIEXPORT void JNICALL Java_nsk_share_jvmti_unit_Heap_setHeapRootCallback
    (JNIEnv *env, jclass cls)
{
    heap_root_callback = simple_heap_root_callback;
    stack_ref_callback = NULL;
    object_ref_callback = NULL;
}

JNIEXPORT void JNICALL Java_nsk_share_jvmti_unit_Heap_setStackRefCallback
    (JNIEnv *env, jclass cls)
{
    heap_root_callback = NULL;
    stack_ref_callback = simple_stack_ref_callback;
    object_ref_callback = NULL;
}

JNIEXPORT void JNICALL Java_nsk_share_jvmti_unit_Heap_setObjectRefCallback
    (JNIEnv *env, jclass cls)
{
    heap_root_callback = NULL;
    stack_ref_callback = NULL;
    object_ref_callback = simple_object_ref_callback;
}



/*************** OBJECT_FREE tests ************/

static jint object_free_count;

static void JNICALL
object_free_count_callback(jvmtiEnv *env, jlong tag) {
    if (tag == 0) {
        fprintf(stderr, "WARNING: OBJECT_FREE event called with tag 0!!!\n");
    }
    object_free_count++;
}

JNIEXPORT void JNICALL Java_nsk_share_jvmti_unit_Heap_setObjectFreeCallback
    (JNIEnv *env, jclass cls)
{
    object_free_callback = object_free_count_callback;
    object_free_count = 0;
}

JNIEXPORT jint JNICALL Java_nsk_share_jvmti_unit_Heap_getObjectFreeCount
    (JNIEnv *env, jclass cls)
{
    return object_free_count;
}

JNIEXPORT void JNICALL Java_nsk_share_jvmti_unit_Heap_zeroObjectFreeCount
    (JNIEnv *env, jclass cls)
{
    object_free_count = 0;
}

/*
 * Agent_Initialize - add capabilities and enables OBJECT_FREE event
 */
jint Agent_Initialize(JavaVM *vm, char *options, void *reserved)
{
    jint rc;
    jvmtiError err;
    jvmtiCapabilities capabilities;
    jvmtiEventCallbacks callbacks;

    /* get JVMTI environment */

    rc = vm->GetEnv((void **)&jvmti, JVMTI_VERSION);
    if (rc != JNI_OK) {
        fprintf(stderr, "Unable to create jvmtiEnv, GetEnv failed, error=%d\n", rc);
        return -1;
    }


    /* add annotate object capability */
    err = jvmti->GetCapabilities(&capabilities);
    if (err != JVMTI_ERROR_NONE) {
        fprintf(stderr, "GetCapabilities failed, error=%d\n", err);
    }
    capabilities.can_tag_objects = 1;
    capabilities.can_generate_object_free_events = 1;
    err = jvmti->AddCapabilities(&capabilities);

    if (err != JVMTI_ERROR_NONE) {
        fprintf(stderr, "AddCapabilities failed, error=%d\n", err);
        return -1;
    }


    /* enable OBJECT_FREE events */
    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_OBJECT_FREE, NULL);
    if (err != JVMTI_ERROR_NONE) {
        fprintf(stderr, "SetEventNotificationMode failed, error=%d\n", err);
        return -1;
    }


    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.ObjectFree = default_object_free;
    err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    if (err != JVMTI_ERROR_NONE) {
        fprintf(stderr, "SetEventCallbacks failed, error=%d\n", err);
        return -1;
    }

    return 0;
}

}
