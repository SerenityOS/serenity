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

/* ============================================================================= */

static jlong timeout = 0;

static long objectCount = 0;
static int userData = 0, allocationError = 0;

typedef struct ObjectDescStruct {
    jlong tag;
    jlong size;
} ObjectDesc;

static ObjectDesc *objectDescBuf;
static jvmtiEnv* st_jvmti = NULL;
static const char debugeeClassSignature[] = "Lnsk/jvmti/IterateOverInstancesOfClass/iterinstcls004;";

/* ============================================================================= */

/* ============================================================================= */


/** jvmtiHeapObjectCallback for first iteration. */
jvmtiIterationControl JNICALL
heapObjectCallback1(jlong class_tag,
                   jlong size,
                   jlong* tag_ptr,
                   void* userData) {

    objectCount++;
    /* Set tag */
    *tag_ptr = objectCount;

    if (!NSK_JVMTI_VERIFY(st_jvmti->Allocate((sizeof(ObjectDesc)), (unsigned char**)&objectDescBuf))) {
        nsk_jvmti_setFailStatus();
        allocationError = 1;
    }

    (*objectDescBuf).tag = *tag_ptr;
    (*objectDescBuf).size = size;

    return JVMTI_ITERATION_ABORT;
}

/** jvmtiHeapObjectCallback for second iteration. */
jvmtiIterationControl JNICALL
heapObjectCallback2(jlong class_tag,
                   jlong size,
                   jlong* tag_ptr,
                   void* userData) {

    objectCount--;

    if (!NSK_JVMTI_VERIFY(st_jvmti->Deallocate((unsigned char*)objectDescBuf))) {
        nsk_jvmti_setFailStatus();
    }

    return JVMTI_ITERATION_ABORT;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    NSK_DISPLAY0("Wait for debugee start\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    {
        jclass debugeeClass = NULL;

        do {
            NSK_DISPLAY1("Find debugee class: %s\n", debugeeClassSignature);
            debugeeClass = nsk_jvmti_classBySignature(debugeeClassSignature);
            if (debugeeClass == NULL) {
                nsk_jvmti_setFailStatus();
                break;
            }

            NSK_DISPLAY0("Calling IterateOverInstancesOfClass with filter JVMTI_HEAP_OBJECT_UNTAGGED\n");
            {
                if (!NSK_JVMTI_VERIFY(
                        jvmti->IterateOverInstancesOfClass(debugeeClass, JVMTI_HEAP_OBJECT_UNTAGGED, heapObjectCallback1, &userData))) {
                    nsk_jvmti_setFailStatus();
                    break;
                }
            }

            if (objectCount == 0) {
                NSK_COMPLAIN0("First IterateOverInstancesOfClass call had not visited any object\n");
                nsk_jvmti_setFailStatus();
                break;
            } else {
                NSK_DISPLAY1("Number of objects the IterateOverInstancesOfClass visited: %d\n", objectCount);
            }

            if (allocationError) break;

            NSK_DISPLAY0("Calling IterateOverInstancesOfClass with filter JVMTI_HEAP_OBJECT_TAGGED\n");
            {
                if (!NSK_JVMTI_VERIFY(
                        jvmti->IterateOverInstancesOfClass(debugeeClass, JVMTI_HEAP_OBJECT_TAGGED, heapObjectCallback2, &userData))) {
                    nsk_jvmti_setFailStatus();
                    break;
                }
            }

            if (objectCount > 0) {
                NSK_COMPLAIN0("Second IterateOverInstancesOfClass call had not visited any object\n");
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
JNIEXPORT jint JNICALL Agent_OnLoad_iterinstcls004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_iterinstcls004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_iterinstcls004(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* save pointer to environment to use it in callbacks */
    st_jvmti = jvmti;

    {
        jvmtiCapabilities caps;

        memset(&caps, 0, sizeof(caps));
        caps.can_tag_objects = 1;
        if (!NSK_JVMTI_VERIFY(
                jvmti->AddCapabilities(&caps))) {
            return JNI_ERR;
        }
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
