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
static jvmtiCapabilities caps;
static jlong timeout = 0;

/* ============================================================================= */


static volatile long objectDescCount = 0;
static volatile int callbackAborted = 0;
static volatile int numberOfDeallocatedFromCallbacksDescriptors = 0;

typedef struct ObjectDescStruct {
    struct ObjectDescStruct *next;
} ObjectDesc;

static ObjectDesc* volatile objectDescList;
static ObjectDesc* *objectDescArr;
static unsigned char* deallocatedFlagsArr;


/* ============================================================================= */


/** heapRootCallback for first heap iteration. */
jvmtiIterationControl JNICALL
heapObjectCallbackForFirstIteration(jlong class_tag,
                   jlong size,
                   jlong* tag_ptr,
                   void* user_data) {
    ObjectDesc *objectDescBuf;

    /* set tag */
    *tag_ptr = (jlong)++objectDescCount;

    /* Allocate memory for next list element*/
    if (!NSK_JVMTI_VERIFY(jvmti->Allocate((sizeof(ObjectDesc)), (unsigned char**)&objectDescBuf))) {
        nsk_jvmti_setFailStatus();
        callbackAborted = 1;
        NSK_COMPLAIN0("heapObjectCallbackForFirstIteration: Allocation failed. Iteration aborted.\n");
        return JVMTI_ITERATION_ABORT;
    }
    objectDescBuf->next = objectDescList;
    objectDescList = objectDescBuf;

    return JVMTI_ITERATION_CONTINUE;
}

/** heapRootCallback for second heap iterator. */
jvmtiIterationControl JNICALL
heapObjectCallbackForSecondIteration(jlong class_tag,
                   jlong size,
                   jlong* tag_ptr,
                   void* user_data) {

    long ind = (long)((*tag_ptr) - 1);

    if (ind < 0 || ind > objectDescCount) {
        NSK_COMPLAIN1("heapObjectCallbackForSecondIteration: invalid object tag value: %d\n", (long)*tag_ptr);
        nsk_jvmti_setFailStatus();
        callbackAborted = 1;
        return JVMTI_ITERATION_ABORT;
    }

    /* Deallocate memory of list element*/
    if (!NSK_JVMTI_VERIFY(
            jvmti->Deallocate((unsigned char*)objectDescArr[ind]))) {
        nsk_jvmti_setFailStatus();
        callbackAborted = 1;
        NSK_COMPLAIN0("heapObjectCallbackForSecondIteration: Deallocation failed. Iteration aborted.\n");
        return JVMTI_ITERATION_ABORT;
    }

    numberOfDeallocatedFromCallbacksDescriptors++;
    deallocatedFlagsArr[ind] = 1;

    /* unset tag */
    *tag_ptr = 0;

    return JVMTI_ITERATION_CONTINUE;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    long ind;
    ObjectDesc *objectDesc;
    int fakeUserData = 0;

    NSK_DISPLAY0("Wait for debugee start\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    {
        do {
            objectDescList = NULL;

            NSK_DISPLAY0("Calling IterateOverHeap with filter JVMTI_HEAP_OBJECT_UNTAGGED\n");
            {
                if (!NSK_JVMTI_VERIFY(
                        jvmti->IterateOverHeap(JVMTI_HEAP_OBJECT_UNTAGGED, heapObjectCallbackForFirstIteration, &fakeUserData))) {
                    nsk_jvmti_setFailStatus();
                    break;
                }
            }
            if (callbackAborted) break;

            if (objectDescCount == 0) {
                NSK_COMPLAIN0("First IterateOverHeap call had not visited any object\n");
                nsk_jvmti_setFailStatus();
                break;
            } else {
                NSK_DISPLAY1("Number of objects first IterateOverHeap visited: %d\n", objectDescCount);
            }

            /* Allocate memory for array to save pointers to ObjectDescList elements */
            if (!NSK_JVMTI_VERIFY(jvmti->Allocate((objectDescCount * sizeof(ObjectDesc*)), (unsigned char**)&objectDescArr))) {
                nsk_jvmti_setFailStatus();
                break;
            }

            /* Allocate memory for flags array and fill with false values */
            if (!NSK_JVMTI_VERIFY(jvmti->Allocate((objectDescCount * sizeof(unsigned char)), &deallocatedFlagsArr))) {
                nsk_jvmti_setFailStatus();
                break;
            }

            for (ind = 0; ind < objectDescCount; ind++) {
                deallocatedFlagsArr[ind] = 0;
            }

            /* Save all pointers to ObjectDescList elements in objectDescArr */
            objectDesc = objectDescList;
            for (ind = 0; ind < objectDescCount; ind++) {
                objectDescArr[ind] = objectDesc;
                objectDesc = objectDesc->next;
            }

            /* Verify objectDescCount and objectDescList length in agreement */
            if (!NSK_VERIFY(objectDesc == NULL)) {
               nsk_jvmti_setFailStatus();
               break;
            }

            NSK_DISPLAY0("Calling IterateOverHeap with filter JVMTI_HEAP_OBJECT_TAGGED\n");
            {
                if (!NSK_JVMTI_VERIFY(
                        jvmti->IterateOverHeap(JVMTI_HEAP_OBJECT_TAGGED, heapObjectCallbackForSecondIteration, &fakeUserData))) {
                    nsk_jvmti_setFailStatus();
                }
            }

            if (numberOfDeallocatedFromCallbacksDescriptors == 0) {
                NSK_COMPLAIN1("Deallocate func. hasn't been called from IterateOverHeap'callback. "
                        "numberOfDeallocatedFromCallbacksDescriptors = %d\n", numberOfDeallocatedFromCallbacksDescriptors);
                nsk_jvmti_setFailStatus();
            }

            for (ind = 0; ind < objectDescCount; ind++) {
                if (!deallocatedFlagsArr[ind]) {
                    if (!NSK_JVMTI_VERIFY(
                           jvmti->Deallocate((unsigned char*)objectDescArr[ind]))) {
                        NSK_COMPLAIN1("Unable to deallocate descriptor. Index = %d \n", ind);
                        nsk_jvmti_setFailStatus();
                        return;
                    }
                }
            }

            if (!NSK_JVMTI_VERIFY(
                    jvmti->Deallocate((unsigned char*)objectDescArr))) {
                nsk_jvmti_setFailStatus();
            }

         if (!NSK_JVMTI_VERIFY(
                    jvmti->Deallocate((unsigned char*)deallocatedFlagsArr))) {
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
JNIEXPORT jint JNICALL Agent_OnLoad_iterheap004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_iterheap004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_iterheap004(JavaVM *jvm, char *options, void *reserved) {
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
    if (!NSK_JVMTI_VERIFY(
            jvmti->AddCapabilities(&caps))) {
        return JNI_ERR;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    if (!caps.can_tag_objects)
        NSK_DISPLAY0("Warning: tagging objects is not available\n");

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
