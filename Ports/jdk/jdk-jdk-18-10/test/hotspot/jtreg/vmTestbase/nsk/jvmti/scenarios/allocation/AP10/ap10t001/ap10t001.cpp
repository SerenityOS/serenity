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

#include <stdio.h>
#include <string.h>
#include <jvmti.h>
#include "agent_common.h"

#include "nsk_tools.h"
#include "JVMTITools.h"
#include "jvmti_tools.h"

extern "C" {

#define STATUS_FAILED 2
#define PASSED 0

#define MAX_SIZE 256

static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;

static volatile int gcfinish = 0, gcstart = 0, objfree = 0;
static long objectCount = 0;

static jlong nanos = 0;
static jlong timeout = 0;
static jvmtiTimerInfo timer_info1, timer_info2;
static int user_data = 0;

typedef struct _LocalStorage {
    unsigned char data[MAX_SIZE];
} LocalStorage;

static LocalStorage stor;

static void envStorageFunc(jvmtiEnv *jvmti_env, const char *msg) {
    LocalStorage* obtainedData = NULL;
    LocalStorage* storedData = &stor;

    NSK_DISPLAY2("%s: setting an environment local storage 0x%p ...\n",
        msg, (void*) &stor);
    if (!NSK_JVMTI_VERIFY(jvmti_env->SetEnvironmentLocalStorage((const void*) &stor))) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN1("%s: unable to set an environment local storage\n\n",
            msg);
        return;
    }

    NSK_DISPLAY1("%s: getting an environment local storage ...\n",
        msg);
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetEnvironmentLocalStorage((void**) &obtainedData))) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN1("%s: unable to get an environment local storage\n\n",
            msg);
        return;
    }
    else {
        if (obtainedData != storedData) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN3(
                "%s: obtained an environment local storage has unexpected pointer:\n"
                "got: 0x%p\texpected: 0x%p\n\n",
                msg, (void*) obtainedData, (void*) storedData);
        }
    }
}

static void timerFunc(jvmtiEnv *jvmti_env, const char *msg) {
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetCurrentThreadCpuTimerInfo(&timer_info1))) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN1("%s: GetCurrentThreadCpuTimerInfo returned unexpected error code\n\n",
            msg);
    }
    /* Check the returned jvmtiTimerInfo structure */
    if (timer_info1.max_value == 0) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN1("%s: GetCurrentThreadCpuTimerInfo returned zero in jvmtiTimerInfo.max_value\n\n",
            msg);
    }
    if (timer_info1.may_skip_forward != JNI_TRUE && timer_info1.may_skip_forward != JNI_FALSE) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN1("%s: GetCurrentThreadCpuTimerInfo returned unknown type value in jvmtiTimerInfo.may_skip_forward\n\n",
            msg);
    }
    if (timer_info1.may_skip_backward != JNI_TRUE && timer_info1.may_skip_backward != JNI_FALSE) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN1("%s: GetCurrentThreadCpuTimerInfo returned unknown type value in jvmtiTimerInfo.may_skip_backward\n\n",
            msg);
    }
    /* ---------------------------------------------------------------------- */

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetCurrentThreadCpuTime(&nanos))) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN1("%s: GetCurrentThreadCpuTime returned unexpected error code\n\n",
            msg);
    }
    /* ---------------------------------------------------------------------- */


    if (!NSK_JVMTI_VERIFY(jvmti_env->GetTimerInfo(&timer_info2))) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN1("%s: GetTimerInfo returned unexpected error code\n\n",
            msg);
    }
    /* Check the returned jvmtiTimerInfo structure */
    if (timer_info2.max_value == 0) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN1("%s: GetTimerInfo returned zero in jvmtiTimerInfo.max_value\n\n",
            msg);
    }
    if (timer_info2.may_skip_forward != JNI_TRUE && timer_info2.may_skip_forward != JNI_FALSE) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN1("%s: GetTimerInfo returned unknown type value in jvmtiTimerInfo.may_skip_forward\n\n",
            msg);
    }
    if (timer_info2.may_skip_backward != JNI_TRUE && timer_info2.may_skip_backward != JNI_FALSE) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN1("%s: GetTimerInfo returned unknown type value in jvmtiTimerInfo.may_skip_backward\n\n",
            msg);
    }
    /* ---------------------------------------------------------------------- */

    nanos = 0;
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetTime(&nanos))) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN1("%s: GetTime returned unexpected error code\n\n",
            msg);
    }

}

/** callback functions **/
void JNICALL
GarbageCollectionFinish(jvmtiEnv *jvmti_env) {
    gcfinish++;
    NSK_DISPLAY1(">>>> GarbageCollectionFinish event #%d received\n",
        gcfinish);

    timerFunc(jvmti_env, "GarbageCollectionFinish");

    envStorageFunc(jvmti_env, "GarbageCollectionFinish");

    NSK_DISPLAY0("<<<<\n\n");
}

void JNICALL
GarbageCollectionStart(jvmtiEnv *jvmti_env) {
    gcstart++;
    NSK_DISPLAY1(">>>> GarbageCollectionStart event #%d received\n",
        gcstart);

    timerFunc(jvmti_env, "GarbageCollectionStart");

    envStorageFunc(jvmti_env, "GarbageCollectionStart");

    NSK_DISPLAY0("<<<<\n\n");
}

void JNICALL
ObjectFree(jvmtiEnv *jvmti_env, jlong tag) {
    NSK_DISPLAY0(">>>> ObjectFree event received\n");
    objfree++;

    timerFunc(jvmti_env, "ObjectFree");

    envStorageFunc(jvmti_env, "ObjectFree");

    NSK_DISPLAY0("<<<<\n\n");
}

jvmtiIterationControl JNICALL
heapObjectCallback(jlong class_tag,
                   jlong size,
                   jlong* tag_ptr,
                   void* user_data) {

    /* set tag */
    *tag_ptr = (jlong)++objectCount;

    /* iterate over only first MAX_SIZE objects */
    if (objectCount >= MAX_SIZE)
        return JVMTI_ITERATION_ABORT;

    return JVMTI_ITERATION_CONTINUE;
}

/************************/

static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    NSK_DISPLAY0("Wait for debugee start\n\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    NSK_DISPLAY0("Call IterateOverHeap to tag random objects for ObjectFree evnts\n\n");
    {
        if (!NSK_JVMTI_VERIFY(jvmti->IterateOverHeap(JVMTI_HEAP_OBJECT_UNTAGGED,
                                                     heapObjectCallback,
                                                     &user_data))) {
            nsk_jvmti_setFailStatus();
        }
    }
    if (objectCount == 0) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN0("IterateOverHeap call had not visited any object\n\n");
    } else {
        NSK_DISPLAY1("Number of objects IterateOverHeap visited: %d\n\n", objectCount);
    }

    NSK_DISPLAY0("Let debugee to provoke GC\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ap10t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ap10t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ap10t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* add capability to generate compiled method events */
    memset(&caps, 0, sizeof(jvmtiCapabilities));
    caps.can_tag_objects = 1;
    caps.can_get_current_thread_cpu_time = 1;
    caps.can_get_thread_cpu_time = 1;
    caps.can_generate_object_free_events = 1;
    caps.can_generate_garbage_collection_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    if (!caps.can_generate_garbage_collection_events)
        NSK_DISPLAY0("Warning: generation of garbage collection events is not implemented\n");
    if (!caps.can_generate_object_free_events)
        NSK_DISPLAY0("Warning: generation of object free events is not implemented\n");
    if (!caps.can_tag_objects)
        NSK_DISPLAY0("Warning: tagging objects is not implemented\n");

    /* set event callback */
    NSK_DISPLAY0("setting event callbacks ...\n");
    (void) memset(&callbacks, 0, sizeof(callbacks));

    callbacks.GarbageCollectionStart = &GarbageCollectionStart;
    callbacks.GarbageCollectionFinish = &GarbageCollectionFinish;
    callbacks.ObjectFree = &ObjectFree;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    NSK_DISPLAY0("setting event callbacks done\nenabling JVMTI events ...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                                          JVMTI_EVENT_GARBAGE_COLLECTION_START,
                                                          NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                                          JVMTI_EVENT_GARBAGE_COLLECTION_FINISH,
                                                          NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                                          JVMTI_EVENT_OBJECT_FREE,
                                                          NULL)))
        return JNI_ERR;
    NSK_DISPLAY0("enabling the events done\n\n");

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

}
