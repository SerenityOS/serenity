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
static jvmtiEnv* st_jvmti = NULL;
static const char *storage_data = "local_storage_data";
static void *storage_ptr = NULL;
static const char debugeeClassSignature[] = "Lnsk/jvmti/IterateOverInstancesOfClass/iterinstcls006;";

/* ============================================================================= */

jvmtiIterationControl JNICALL
heapObjectCallback(jlong class_tag,
                   jlong size,
                   jlong* tag_ptr,
                   void* storage_data) {

    if (!NSK_JVMTI_VERIFY(st_jvmti->SetEnvironmentLocalStorage(storage_data))) {
        nsk_jvmti_setFailStatus();
    }

    if (!NSK_JVMTI_VERIFY(st_jvmti->GetEnvironmentLocalStorage(&storage_ptr))) {
        nsk_jvmti_setFailStatus();
    }

    /*  Iterate over only first object */
    return JVMTI_ITERATION_ABORT;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    NSK_DISPLAY0("Wait for debugee start\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    do {
        jclass debugeeClass = NULL;

        NSK_DISPLAY1("Find debugee class: %s\n", debugeeClassSignature);
        debugeeClass = nsk_jvmti_classBySignature(debugeeClassSignature);
        if (debugeeClass == NULL) {
            nsk_jvmti_setFailStatus();
            break;
        }

        NSK_DISPLAY0("Calling IterateOverInstancesOfClass with filter JVMTI_HEAP_OBJECT_EITHER\n");
        {
            if (!NSK_JVMTI_VERIFY(
                    jvmti->IterateOverInstancesOfClass(debugeeClass,
                                                       JVMTI_HEAP_OBJECT_EITHER,
                                                       heapObjectCallback,
                                                       (void *) storage_data))) {
                nsk_jvmti_setFailStatus();
            }
        }

        if (storage_data != storage_ptr) {
            NSK_COMPLAIN2("Local storage address was corrupted: %p ,\n\texpected value: %p\n",
                          storage_ptr, storage_data);
            nsk_jvmti_setFailStatus();
        }

        if (strcmp(storage_data, (char *)storage_ptr) != 0) {
            NSK_COMPLAIN2("Local storage was corrupted: %s ,\n\texpected value: %s\n",
                          (char *)storage_ptr, storage_data);
            nsk_jvmti_setFailStatus();
        }
    } while (0);

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_iterinstcls006(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_iterinstcls006(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_iterinstcls006(JavaVM *jvm, char *options, void *reserved) {
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
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
            return JNI_ERR;
        }
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
