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
#include <string.h>
#include <jvmti.h>
#include "agent_common.h"

#include "nsk_tools.h"
#include "JVMTITools.h"
#include "jvmti_tools.h"

extern "C" {

#define STATUS_FAILED 2
#define PASSED 0

static volatile jint result = PASSED;
static volatile int gcstart = 0;
static volatile int gcfinish = 0;
static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;

/** callback functions **/
void JNICALL
GarbageCollectionStart(jvmtiEnv *jvmti_env) {
    gcstart++;
    NSK_DISPLAY1("GarbageCollectionStart event #%d received\n",
        gcstart);

    if (gcstart != (gcfinish+1)) {
        result = STATUS_FAILED;
        NSK_COMPLAIN2(
            "TEST FAILED: GarbageCollectionStart event has no a matched pair GarbageCollectionFinish:\n"
            "\t%d GarbageCollectionStart events\t%d GarbageCollectionFinish events\n\n",
            gcstart, gcfinish);
    }
    else
        NSK_DISPLAY0("CHECK PASSED: GarbageCollectionStart event has a matched pair GarbageCollectionFinish as expected\n\n");
}

void JNICALL
GarbageCollectionFinish(jvmtiEnv *jvmti_env) {
    gcfinish++;
    NSK_DISPLAY1("GarbageCollectionFinish event #%d received\n",
        gcfinish);

    if (gcstart != gcfinish) {
        result = STATUS_FAILED;
        NSK_COMPLAIN2(
            "TEST FAILED: GarbageCollectionFinish event has no a matched pair GarbageCollectionStart:\n"
            "\t%d GarbageCollectionStart events\t%d GarbageCollectionFinish events\n\n",
            gcstart, gcfinish);
    }
    else
        NSK_DISPLAY0("CHECK PASSED: GarbageCollectionFinish event has a matched pair GarbageCollectionStart as expected\n\n");
}

void JNICALL
VMDeath(jvmtiEnv *jvmti_env, JNIEnv *env) {
    NSK_DISPLAY0("VMDeath event received\n");

    if (gcstart != gcfinish || result == STATUS_FAILED) {
        NSK_COMPLAIN2(
            "TEST FAILED: some GarbageCollectionFinish events have no a matched pair GarbageCollectionStart:\n"
            "\t%d GarbageCollectionStart events\t%d GarbageCollectionFinish events\n\n",
            gcstart, gcfinish);

        exit(95 + STATUS_FAILED);
    }
    else
        NSK_DISPLAY0("CHECK PASSED: all GarbageCollectionStart/GarbageCollectionFinish events have a matched pair as expected\n\n");
}

/************************/

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_gcstart001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_gcstart001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_gcstart001(JavaVM *jvm, char *options, void *reserved) {
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
    caps.can_generate_garbage_collection_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    if (!caps.can_generate_garbage_collection_events)
        NSK_DISPLAY0("Warning: generation of garbage collection events is not implemented\n");

    /* set event callback */
    NSK_DISPLAY0("setting event callbacks ...\n");
    (void) memset(&callbacks, 0, sizeof(callbacks));
    callbacks.VMDeath = &VMDeath;
    callbacks.GarbageCollectionStart = &GarbageCollectionStart;
    callbacks.GarbageCollectionFinish = &GarbageCollectionFinish;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    NSK_DISPLAY0("setting event callbacks done\nenabling JVMTI events ...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_GARBAGE_COLLECTION_START, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_GARBAGE_COLLECTION_FINISH, NULL)))
        return JNI_ERR;
    NSK_DISPLAY0("enabling the events done\n\n");

    return JNI_OK;
}

}
