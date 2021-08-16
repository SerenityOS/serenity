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

static long objCounter = 0;
static int userData = 0;
static jvmtiEnv* st_jvmti = NULL;
static jlong nanos = 0;
static jvmtiTimerInfo timer_info1, timer_info2;

/* ============================================================================= */

/* jvmtiHeapRootCallback */
jvmtiIterationControl JNICALL
heapRootCallback(jvmtiHeapRootKind root_kind,
                 jlong class_tag,
                 jlong size,
                 jlong* tag_ptr,
                 void* user_data) {

    *tag_ptr = (jlong)++objCounter;

    if (!nsk_jvmti_isFailStatus()) {

        if (!NSK_JVMTI_VERIFY(st_jvmti->GetCurrentThreadCpuTimerInfo(&timer_info1))) {
            nsk_jvmti_setFailStatus();
        }
        /* Check the returned jvmtiTimerInfo structure */
        if (timer_info1.max_value == 0) {
            NSK_COMPLAIN0("heapRootCallback: GetCurrentThreadCpuTimerInfo returned zero in jvmtiTimerInfo.max_value\n");
            nsk_jvmti_setFailStatus();
        }
        if (timer_info1.may_skip_forward != JNI_TRUE && timer_info1.may_skip_forward != JNI_FALSE) {
            NSK_COMPLAIN0("heapRootCallback: GetCurrentThreadCpuTimerInfo returned unknown type value in jvmtiTimerInfo.may_skip_forward\n");
            nsk_jvmti_setFailStatus();
        }
        if (timer_info1.may_skip_backward != JNI_TRUE && timer_info1.may_skip_backward != JNI_FALSE) {
            NSK_COMPLAIN0("heapRootCallback: GetCurrentThreadCpuTimerInfo returned unknown type value in jvmtiTimerInfo.may_skip_backward\n");
            nsk_jvmti_setFailStatus();
        }
        /* ---------------------------------------------------------------------- */

        if (!NSK_JVMTI_VERIFY(st_jvmti->GetCurrentThreadCpuTime(&nanos))) {
            nsk_jvmti_setFailStatus();
        }
        /* ---------------------------------------------------------------------- */

        if (!NSK_JVMTI_VERIFY(st_jvmti->GetTimerInfo(&timer_info2))) {
            nsk_jvmti_setFailStatus();
        }
        /* Check the returned jvmtiTimerInfo structure */
        if (timer_info2.max_value == 0) {
            NSK_COMPLAIN0("heapRootCallback: GetTimerInfo returned zero in jvmtiTimerInfo.max_value\n");
            nsk_jvmti_setFailStatus();
        }
        if (timer_info2.may_skip_forward != JNI_TRUE && timer_info2.may_skip_forward != JNI_FALSE) {
            NSK_COMPLAIN0("heapRootCallback: GetTimerInfo returned unknown type value in jvmtiTimerInfo.may_skip_forward\n");
            nsk_jvmti_setFailStatus();
        }
        if (timer_info2.may_skip_backward != JNI_TRUE && timer_info2.may_skip_backward != JNI_FALSE) {
            NSK_COMPLAIN0("heapRootCallback: GetTimerInfo returned unknown type value in jvmtiTimerInfo.may_skip_backward\n");
            nsk_jvmti_setFailStatus();
        }
        /* ---------------------------------------------------------------------- */

        nanos = 0;
        if (!NSK_JVMTI_VERIFY(st_jvmti->GetTime(&nanos))) {
            nsk_jvmti_setFailStatus();
        }
    }

/*
    NSK_DISPLAY1("heapRootCallback: %d\n", objCounter);
*/
    return JVMTI_ITERATION_CONTINUE;
}

/* jvmtiStackReferenceCallback */
jvmtiIterationControl JNICALL
stackReferenceCallback(jvmtiHeapRootKind root_kind,
                       jlong     class_tag,
                       jlong     size,
                       jlong*    tag_ptr,
                       jlong     thread_tag,
                       jint      depth,
                       jmethodID method,
                       jint      slot,
                       void*     user_data) {

    *tag_ptr = (jlong)++objCounter;

    if (!nsk_jvmti_isFailStatus()) {

        if (!NSK_JVMTI_VERIFY(st_jvmti->GetCurrentThreadCpuTimerInfo(&timer_info1))) {
            nsk_jvmti_setFailStatus();
        }
        /* Check the returned jvmtiTimerInfo structure */
        if (timer_info1.max_value == 0) {
            NSK_COMPLAIN0("stackReferenceCallback: GetCurrentThreadCpuTimerInfo returned zero in jvmtiTimerInfo.max_value\n");
            nsk_jvmti_setFailStatus();
        }
        if (timer_info1.may_skip_forward != JNI_TRUE && timer_info1.may_skip_forward != JNI_FALSE) {
            NSK_COMPLAIN0("stackReferenceCallback: GetCurrentThreadCpuTimerInfo returned unknown type value in jvmtiTimerInfo.may_skip_forward\n");
            nsk_jvmti_setFailStatus();
        }
        if (timer_info1.may_skip_backward != JNI_TRUE && timer_info1.may_skip_backward != JNI_FALSE) {
            NSK_COMPLAIN0("stackReferenceCallback: GetCurrentThreadCpuTimerInfo returned unknown type value in jvmtiTimerInfo.may_skip_backward\n");
            nsk_jvmti_setFailStatus();
        }
        /* ---------------------------------------------------------------------- */

        if (!NSK_JVMTI_VERIFY(st_jvmti->GetCurrentThreadCpuTime(&nanos))) {
            nsk_jvmti_setFailStatus();
        }
        /* ---------------------------------------------------------------------- */

        if (!NSK_JVMTI_VERIFY(st_jvmti->GetTimerInfo(&timer_info2))) {
            nsk_jvmti_setFailStatus();
        }
        /* Check the returned jvmtiTimerInfo structure */
        if (timer_info2.max_value == 0) {
            NSK_COMPLAIN0("stackReferenceCallback: GetTimerInfo returned zero in jvmtiTimerInfo.max_value\n");
            nsk_jvmti_setFailStatus();
        }
        if (timer_info2.may_skip_forward != JNI_TRUE && timer_info2.may_skip_forward != JNI_FALSE) {
            NSK_COMPLAIN0("stackReferenceCallback: GetTimerInfo returned unknown type value in jvmtiTimerInfo.may_skip_forward\n");
            nsk_jvmti_setFailStatus();
        }
        if (timer_info2.may_skip_backward != JNI_TRUE && timer_info2.may_skip_backward != JNI_FALSE) {
            NSK_COMPLAIN0("stackReferenceCallback: GetTimerInfo returned unknown type value in jvmtiTimerInfo.may_skip_backward\n");
            nsk_jvmti_setFailStatus();
        }
        /* ---------------------------------------------------------------------- */

        nanos = 0;
        if (!NSK_JVMTI_VERIFY(st_jvmti->GetTime(&nanos))) {
            nsk_jvmti_setFailStatus();
        }
   }

/*
    NSK_DISPLAY1("stackRefenceCallback: %d\n", objCounter);
*/
    return JVMTI_ITERATION_CONTINUE;
}


/* jvmtiObjectReferenceCallback */
jvmtiIterationControl JNICALL
objectReferenceCallback(jvmtiObjectReferenceKind reference_kind,
                        jlong  class_tag,
                        jlong  size,
                        jlong* tag_ptr,
                        jlong  referrer_tag,
                        jint   referrer_index,
                        void*  user_data) {

    *tag_ptr = (jlong)++objCounter;

    if (!NSK_JVMTI_VERIFY(st_jvmti->GetCurrentThreadCpuTimerInfo(&timer_info1))) {
        nsk_jvmti_setFailStatus();
    }
    /* Check the returned jvmtiTimerInfo structure */
    if (timer_info1.max_value == 0) {
        NSK_COMPLAIN0("objectReferenceCallback: GetCurrentThreadCpuTimerInfo returned zero in jvmtiTimerInfo.max_value\n");
        nsk_jvmti_setFailStatus();
    }
    if (timer_info1.may_skip_forward != JNI_TRUE && timer_info1.may_skip_forward != JNI_FALSE) {
        NSK_COMPLAIN0("objectReferenceCallback: GetCurrentThreadCpuTimerInfo returned unknown type value in jvmtiTimerInfo.may_skip_forward\n");
        nsk_jvmti_setFailStatus();
    }
    if (timer_info1.may_skip_backward != JNI_TRUE && timer_info1.may_skip_backward != JNI_FALSE) {
        NSK_COMPLAIN0("objectReferenceCallback: GetCurrentThreadCpuTimerInfo returned unknown type value in jvmtiTimerInfo.may_skip_backward\n");
        nsk_jvmti_setFailStatus();
    }
    /* ---------------------------------------------------------------------- */

    if (!NSK_JVMTI_VERIFY(st_jvmti->GetCurrentThreadCpuTime(&nanos))) {
        nsk_jvmti_setFailStatus();
    }
    /* ---------------------------------------------------------------------- */


    if (!NSK_JVMTI_VERIFY(st_jvmti->GetTimerInfo(&timer_info2))) {
        nsk_jvmti_setFailStatus();
    }
    /* Check the returned jvmtiTimerInfo structure */
    if (timer_info2.max_value == 0) {
        NSK_COMPLAIN0("objectReferenceCallback: GetTimerInfo returned zero in jvmtiTimerInfo.max_value\n");
        nsk_jvmti_setFailStatus();
    }
    if (timer_info2.may_skip_forward != JNI_TRUE && timer_info2.may_skip_forward != JNI_FALSE) {
        NSK_COMPLAIN0("objectReferenceCallback: GetTimerInfo returned unknown type value in jvmtiTimerInfo.may_skip_forward\n");
        nsk_jvmti_setFailStatus();
    }
    if (timer_info2.may_skip_backward != JNI_TRUE && timer_info2.may_skip_backward != JNI_FALSE) {
        NSK_COMPLAIN0("objectReferenceCallback: GetTimerInfo returned unknown type value in jvmtiTimerInfo.may_skip_backward\n");
        nsk_jvmti_setFailStatus();
    }
    /* ---------------------------------------------------------------------- */

    nanos = 0;
    if (!NSK_JVMTI_VERIFY(st_jvmti->GetTime(&nanos))) {
        nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY1("objectRefenceCallback: %d\n", objCounter);
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
        do {
            NSK_DISPLAY0("Calling IterateOverReachableObjects\n");
            {
                if (!NSK_JVMTI_VERIFY(jvmti->IterateOverReachableObjects(heapRootCallback,
                                                                         stackReferenceCallback,
                                                                         objectReferenceCallback,
                                                                         &userData))) {
                    nsk_jvmti_setFailStatus();
                    break;
                }
            }

            if (objCounter == 0) {
                NSK_COMPLAIN0("IterateOverReachableObjects call had not visited any object\n");
                nsk_jvmti_setFailStatus();
                break;
            } else {
                NSK_DISPLAY1("Number of objects the IterateOverReachableObjects visited: %d\n", objCounter);
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
JNIEXPORT jint JNICALL Agent_OnLoad_iterreachobj005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_iterreachobj005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_iterreachobj005(JavaVM *jvm, char *options, void *reserved) {
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
        caps.can_get_current_thread_cpu_time = 1;
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
