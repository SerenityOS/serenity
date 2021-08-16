/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "agent_common.h"
#include "jni_tools.h"
#include "jvmti_tools.h"

extern "C" {

static JNIEnv *jni = NULL;
static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;
static jlong timeout = 0;

/* ============================================================================= */

static volatile long objectCount = 0, objectCountMax = 0;
static int userData = 0, callbackAborted = 0;
static int numberOfDeallocatedFromCallbacksDescriptors = 0;

typedef struct ObjectDescStruct {
    jlong tag;
    jlong size;
    struct ObjectDescStruct *next;
} ObjectDesc;

static ObjectDesc *objectDescList, *objectDescListStart, *objectDescBuf;
static ObjectDesc* *objectDescArr;
static short* deallocatedFlagsArr;

/* ============================================================================= */

void JNICALL
ObjectFree(jvmtiEnv *jvmti_env, jlong tag) {
    /* decrement number of expected objects  */
    objectCount--;
}

/* ============================================================================= */


/** jvmtiHeapRootCallback for first iteration. */
jvmtiIterationControl JNICALL
heapRootCallbackForFirstObjectsIteration(jvmtiHeapRootKind root_kind,
                                         jlong class_tag,
                                         jlong size,
                                         jlong* tag_ptr,
                                         void* user_data) {

    if (*tag_ptr != 0) return JVMTI_ITERATION_CONTINUE;

    /* Set tag */
    *tag_ptr = (jlong)++objectCount;

    if (!NSK_JVMTI_VERIFY(jvmti->Allocate((sizeof(ObjectDesc)), (unsigned char**)&objectDescBuf))) {
        nsk_jvmti_setFailStatus();
        callbackAborted = 1;
        NSK_COMPLAIN0("heapRootCallbackForFirstObjectsIteration: Allocation failed. Iteration aborted.\n");
        return JVMTI_ITERATION_ABORT;
    }

    (*objectDescList).tag = *tag_ptr;
    (*objectDescList).size = size;
    (*objectDescList).next = objectDescBuf;

    /* step to next list element */
    objectDescList = (*objectDescList).next;

    return JVMTI_ITERATION_CONTINUE;
}

/** jvmtiHeapRootCallback for second iteration. */
jvmtiIterationControl JNICALL
heapRootCallbackForSecondObjectsIteration(jvmtiHeapRootKind root_kind,
                                          jlong class_tag,
                                          jlong size,
                                          jlong* tag_ptr,
                                          void* user_data) {

    long ind = (long)((*tag_ptr) - 1);

    if (*tag_ptr == 0) return JVMTI_ITERATION_CONTINUE;

/*
    ObjectDesc *objectDesc = objectDescArr[ind];
    jlong tag = (*objectDesc).tag;
*/
    if (ind < 0 || ind > objectCountMax) {
        NSK_COMPLAIN1("heapRootCallbackForSecondObjectsIteration: invalid object tag value: %d\n", (long)*tag_ptr);
        nsk_jvmti_setFailStatus();
        callbackAborted = 1;
        return JVMTI_ITERATION_ABORT;
    }
/*
    NSK_DISPLAY3("heapRootCallbackForSecondObjectsIteration: *tag_ptr %6d , tag %6d , objectCount %6d\n",
                     (long)*tag_ptr, (long)tag, objectCount);
*/
    /* Deallocate memory of list element*/
    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)objectDescArr[ind]))) {
        nsk_jvmti_setFailStatus();
        callbackAborted = 1;
        NSK_COMPLAIN0("heapRootCallbackForSecondObjectsIteration: Deallocation failed. Iteration aborted.\n");
        return JVMTI_ITERATION_ABORT;
    }

    numberOfDeallocatedFromCallbacksDescriptors++;
    deallocatedFlagsArr[ind] = 1;

    /* unset tag */
    *tag_ptr = 0;
    objectCount--;

    return JVMTI_ITERATION_CONTINUE;
}

/** jvmtiStackReferenceCallback for first iteration. */
jvmtiIterationControl JNICALL
stackReferenceCallbackForFirstObjectsIteration(jvmtiHeapRootKind root_kind,
                                               jlong     class_tag,
                                               jlong     size,
                                               jlong*    tag_ptr,
                                               jlong     thread_tag,
                                               jint      depth,
                                               jmethodID method,
                                               jint      slot,
                                               void*     user_data) {

    if (*tag_ptr != 0) return JVMTI_ITERATION_CONTINUE;

    /* Set tag */
    *tag_ptr = (jlong)++objectCount;

    if (!NSK_JVMTI_VERIFY(jvmti->Allocate((sizeof(ObjectDesc)), (unsigned char**)&objectDescBuf))) {
        nsk_jvmti_setFailStatus();
        callbackAborted = 1;
        NSK_COMPLAIN0("stackReferenceCallbackForFirstObjectsIteration: Allocation failed. Iteration aborted.\n");
        return JVMTI_ITERATION_ABORT;
    }

    (*objectDescList).tag = *tag_ptr;
    (*objectDescList).size = size;
    (*objectDescList).next = objectDescBuf;

    /* step to next list element */
    objectDescList = (*objectDescList).next;

    return JVMTI_ITERATION_CONTINUE;
}

/** jvmtiStackReferenceCallback for second iteration. */
jvmtiIterationControl JNICALL
stackReferenceCallbackForSecondObjectsIteration(jvmtiHeapRootKind root_kind,
                                                jlong     class_tag,
                                                jlong     size,
                                                jlong*    tag_ptr,
                                                jlong     thread_tag,
                                                jint      depth,
                                                jmethodID method,
                                                jint      slot,
                                                void*     user_data) {

    long ind = (long)((*tag_ptr) - 1);

    if (*tag_ptr == 0) return JVMTI_ITERATION_CONTINUE;

/*
    ObjectDesc *objectDesc = objectDescArr[ind];
    jlong tag = (*objectDesc).tag;
*/
    if (ind < 0 || ind > objectCountMax) {
        NSK_COMPLAIN1("stackReferenceCallbackForSecondObjectsIteration: invalid object tag value: %d\n", (long)*tag_ptr);
        nsk_jvmti_setFailStatus();
        callbackAborted = 1;
        return JVMTI_ITERATION_ABORT;
    }
/*
    NSK_DISPLAY3("stackReferenceCallbackForSecondObjectsIteration: *tag_ptr %6d , tag %6d , objectCount %6d\n",
                     (long)*tag_ptr, (long)tag, objectCount);
*/
    /* Deallocate memory of list element*/
    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)objectDescArr[ind]))) {
        nsk_jvmti_setFailStatus();
        callbackAborted = 1;
        NSK_COMPLAIN0("stackReferenceCallbackForSecondObjectsIteration: Deallocation failed. Iteration aborted.\n");
        return JVMTI_ITERATION_ABORT;
    }

    numberOfDeallocatedFromCallbacksDescriptors++;
    deallocatedFlagsArr[ind] = 1;

    /* unset tag */
    *tag_ptr = 0;
    objectCount--;

    return JVMTI_ITERATION_CONTINUE;
}

/** jvmtiObjectReferenceCallback for first iteration. */
jvmtiIterationControl JNICALL
objectReferenceCallbackForFirstObjectsIteration(jvmtiObjectReferenceKind reference_kind,
                                                jlong  class_tag,
                                                jlong  size,
                                                jlong* tag_ptr,
                                                jlong  referrer_tag,
                                                jint   referrer_index,
                                                void*  user_data) {

    if (*tag_ptr != 0) return JVMTI_ITERATION_CONTINUE;

    /* Set tag */
    *tag_ptr = (jlong)++objectCount;

    if (!NSK_JVMTI_VERIFY(jvmti->Allocate((sizeof(ObjectDesc)), (unsigned char**)&objectDescBuf))) {
        nsk_jvmti_setFailStatus();
        callbackAborted = 1;
        NSK_COMPLAIN0("objectReferenceCallbackForFirstObjectsIteration: Allocation failed. Iteration aborted.\n");
        return JVMTI_ITERATION_ABORT;
    }

    (*objectDescList).tag = *tag_ptr;
    (*objectDescList).size = size;
    (*objectDescList).next = objectDescBuf;

    /* step to next list element */
    objectDescList = (*objectDescList).next;

    return JVMTI_ITERATION_CONTINUE;
}

/** jvmtiObjectReferenceCallback for second iteration. */
jvmtiIterationControl JNICALL
objectReferenceCallbackForSecondObjectsIteration(jvmtiObjectReferenceKind reference_kind,
                                                 jlong  class_tag,
                                                 jlong  size,
                                                 jlong* tag_ptr,
                                                 jlong  referrer_tag,
                                                 jint   referrer_index,
                                                 void*  user_data) {

    long ind = (long)((*tag_ptr) - 1);

    if (*tag_ptr == 0) return JVMTI_ITERATION_CONTINUE;

/*
    ObjectDesc *objectDesc = objectDescArr[ind];
    jlong tag = (*objectDesc).tag;
*/
    if (ind < 0 || ind > objectCountMax) {
        NSK_COMPLAIN1("objectReferenceCallbackForSecondObjectsIteration: invalid object tag value: %d\n", (long)*tag_ptr);
        nsk_jvmti_setFailStatus();
        callbackAborted = 1;
        return JVMTI_ITERATION_ABORT;
    }
/*
    NSK_DISPLAY3("objectReferenceCallbackForSecondObjectsIteration: *tag_ptr %6d , tag %6d , objectCount %6d\n",
                     (long)*tag_ptr, (long)tag, objectCount);
*/
    /* Deallocate memory of list element*/
    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)objectDescArr[ind]))) {
        nsk_jvmti_setFailStatus();
        callbackAborted = 1;
        NSK_COMPLAIN0("objectReferenceCallbackForSecondObjectsIteration: Deallocation failed. Iteration aborted.\n");
        return JVMTI_ITERATION_ABORT;
    }

    numberOfDeallocatedFromCallbacksDescriptors++;
    deallocatedFlagsArr[ind] = 1;

    /* unset tag */
    *tag_ptr = 0;
    objectCount--;

    return JVMTI_ITERATION_CONTINUE;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    long ind;

    NSK_DISPLAY0("Wait for debugee start\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    {
        do {
            /* Allocate memory for first element of objectList */
            if (!NSK_JVMTI_VERIFY(jvmti->Allocate((sizeof(ObjectDesc)),
                                                  (unsigned char**)&objectDescBuf))) {
                nsk_jvmti_setFailStatus();
                break;
            }
            objectDescList = objectDescBuf;
            objectDescListStart = objectDescList;

            NSK_DISPLAY0("Calling IterateOverReachableObjects with allocating object descriptors\n");
            {
                if (!NSK_JVMTI_VERIFY(jvmti->IterateOverReachableObjects(
                        heapRootCallbackForFirstObjectsIteration,
                        stackReferenceCallbackForFirstObjectsIteration,
                        objectReferenceCallbackForFirstObjectsIteration,
                        &userData))) {
                    nsk_jvmti_setFailStatus();
                    break;
                }
            }
            if (callbackAborted) break;

            if (objectCount == 0) {
                NSK_COMPLAIN0("First IterateOverReachableObjects call had not visited any object\n");
                nsk_jvmti_setFailStatus();
                break;
            } else {
                NSK_DISPLAY1("Number of objects the first IterateOverReachableObjects visited: %d\n", objectCount);
            }

            if (callbackAborted) break;

            objectCountMax = objectCount;

            /* Deallocate last unnecessary descriptor */
            if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)objectDescList))) {
                NSK_COMPLAIN0("Unable to deallocate last unnecessary descriptor. \n");
                nsk_jvmti_setFailStatus();
                break;
            }

            /* Allocate memory for array to save pointers to ObjectDescList elements */
            if (!NSK_JVMTI_VERIFY(jvmti->Allocate((objectCount * sizeof(ObjectDesc*)),
                                                  (unsigned char**)&objectDescArr))) {
                nsk_jvmti_setFailStatus();
                break;
            }

            /* Allocate memory for flags array and fill with false values */
            if (!NSK_JVMTI_VERIFY(jvmti->Allocate((objectCountMax * sizeof(short)),
                                                  (unsigned char**)&deallocatedFlagsArr))) {
                nsk_jvmti_setFailStatus();
                break;
            }

            for (ind = 0; ind < objectCountMax; ind++) {
                deallocatedFlagsArr[ind] = 0;
            }

            objectDescList = objectDescListStart;
            {
                /* Save all pointers to ObjectDescList elements in objectDescArr */
                for (ind = 0; ind < objectCount; ind++) {
                    objectDescArr[ind] = objectDescList;
                    objectDescList = (*objectDescList).next;
                }
            }

            NSK_DISPLAY0("Calling IterateOverReachableObjects with deallocating object descriptors\n");
            {
                if (!NSK_JVMTI_VERIFY(jvmti->IterateOverReachableObjects(
                    heapRootCallbackForSecondObjectsIteration,
                    stackReferenceCallbackForSecondObjectsIteration,
                    objectReferenceCallbackForSecondObjectsIteration,
                    &userData))) {
                    nsk_jvmti_setFailStatus();
                    break;
                }
            }

            if (numberOfDeallocatedFromCallbacksDescriptors == 0) {
                NSK_COMPLAIN1("Deallocate func. hasn't been called from IterateOverReachableObjects'callbacks. "
                        "numberOfDeallocatedFromCallbacksDescriptors = %d\n", numberOfDeallocatedFromCallbacksDescriptors);
                nsk_jvmti_setFailStatus();
            }

            for (ind = 0; ind < objectCountMax; ind++) {
                if (!deallocatedFlagsArr[ind]) {
                    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)objectDescArr[ind]))) {
                        NSK_COMPLAIN1("Unable to deallocate descriptor. Index = %d \n", ind);
                        nsk_jvmti_setFailStatus();
                        return;
                    }
                }
            }

            if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)objectDescArr))) {
                nsk_jvmti_setFailStatus();
            }

            if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)deallocatedFlagsArr))) {
                nsk_jvmti_setFailStatus();
            }

        } while (0);
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

/* ============================================================================= */

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_iterreachobj002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_iterreachobj002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_iterreachobj002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    memset(&caps, 0, sizeof(caps));
    caps.can_tag_objects = 1;
    caps.can_generate_object_free_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
        return JNI_ERR;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    if (!caps.can_tag_objects)
        NSK_DISPLAY0("Warning: tagging objects is not available\n");
    if (!caps.can_generate_object_free_events)
        NSK_DISPLAY0("Warning: generation of object free events is not available\n");

    /* set event callback */
    NSK_DISPLAY0("setting event callbacks ...\n");
    (void) memset(&callbacks, 0, sizeof(callbacks));

    callbacks.ObjectFree = &ObjectFree;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    NSK_DISPLAY0("setting event callbacks done.\n");

    NSK_DISPLAY0("enabling JVMTI events ...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                                          JVMTI_EVENT_OBJECT_FREE,
                                                          NULL)))
        return JNI_ERR;
    NSK_DISPLAY0("enabling the events done.\n");

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
