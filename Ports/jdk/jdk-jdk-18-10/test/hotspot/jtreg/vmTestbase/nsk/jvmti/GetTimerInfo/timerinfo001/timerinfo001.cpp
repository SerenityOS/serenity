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

#include <stdlib.h>
#include <string.h>
#include "jvmti.h"
#include "agent_common.h"
#include "jni_tools.h"
#include "jvmti_tools.h"

extern "C" {

/* ============================================================================= */

static jlong timeout = 0;

#define STATUS_FAIL     97

#define EVENTS_COUNT    2

static jvmtiEvent events[EVENTS_COUNT] = {
    JVMTI_EVENT_VM_INIT,
    JVMTI_EVENT_VM_DEATH
};

static jvmtiTimerInfo initInfo;

/* ============================================================================= */

/**
 * Get timer info and optionally compares it with initial one.
 * @returns false if any error occured.
 */
static bool checkTimerInfo(jvmtiEnv* jvmti, jvmtiTimerInfo* info,
                           jvmtiTimerInfo* initInfo, const char where[]) {

    char buf[32], buf2[32];
    bool success = true;

    NSK_DISPLAY0("GetTimerInfo() for current JVMTI env\n");
    if (!NSK_JVMTI_VERIFY(
            jvmti->GetTimerInfo(info))) {
        return false;
    }
    NSK_DISPLAY0("Got timer info:\n");

    NSK_DISPLAY1("    max_value:         %s\n",
                 julong_to_string((julong)info->max_value, buf));
    NSK_DISPLAY1("    may_skip_forward:  %d\n", (int)info->may_skip_forward);
    NSK_DISPLAY1("    may_skip_backward: %d\n", (int)info->may_skip_backward);

    if (initInfo != NULL) {
        NSK_DISPLAY0("Compare with initial timer info\n");
        if (info->max_value != initInfo->max_value) {
            NSK_COMPLAIN4("In %s GetTimerInfo() returned different info:\n"
                          "#   field:     %s\n"
                          "#   got value: %s\n"
                          "#   initial:   %s\n",
                          where, "max_value",
                          julong_to_string((julong)info->max_value, buf),
                          julong_to_string((julong)initInfo->max_value, buf2));
            success = false;
        }
        if (info->may_skip_forward != initInfo->may_skip_forward) {
            NSK_COMPLAIN4("In %s GetTimerInfo() returned different info:\n"
                          "#   field:     %s\n"
                          "#   got value: %d\n"
                          "#   initial:   %d\n",
                            where, "may_skip_forward",
                            (int)info->may_skip_forward,
                            (int)initInfo->may_skip_forward);
            success = false;
        }
        if (info->may_skip_backward != initInfo->may_skip_backward) {
            NSK_COMPLAIN4("In %s GetTimerInfo() returned different info:\n"
                          "#   field:     %s\n"
                          "#   got value: %d\n"
                          "#   initial:   %d\n",
                            where, "may_skip_backward",
                            (int)info->may_skip_backward,
                            (int)initInfo->may_skip_backward);
            success = false;
        }
    }

    return success;
}
/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    NSK_DISPLAY0("Wait for debugee to become ready\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    NSK_DISPLAY0(">>> Testcase #3: Check timer info in agent thread\n");
    {
        jvmtiTimerInfo info;
        if (!checkTimerInfo(jvmti, &info, &initInfo, "agent thread")) {
            nsk_jvmti_setFailStatus();
        }
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ============================================================================= */

/**
 * Callback for VM_INIT event.
 */
JNIEXPORT void JNICALL
callbackVMInit(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread) {

    NSK_DISPLAY0(">>> Testcase #2: Check timer info in VM_INIT callback\n");
    {
        jvmtiTimerInfo info;
        if (!checkTimerInfo(jvmti, &info, &initInfo, "VM_INIT callback")) {
            nsk_jvmti_setFailStatus();
        }
    }
}

/**
 * Callback for VM_DEATH event.
 */
JNIEXPORT void JNICALL
callbackVMDeath(jvmtiEnv* jvmti, JNIEnv* jni) {
    bool success = true;

    NSK_DISPLAY0(">>> Testcase #4: Check timer info in VM_DEATH callback\n");
    {
        jvmtiTimerInfo info;
        success = checkTimerInfo(jvmti, &info, &initInfo, "VM_DEATH callback");
    }

    NSK_DISPLAY1("Disable events: %d events\n", EVENTS_COUNT);
    if (!nsk_jvmti_enableEvents(JVMTI_DISABLE, EVENTS_COUNT, events, NULL)) {
        success = false;
    } else {
        NSK_DISPLAY0("  ... disabled\n");
    }

    if (!success) {
        NSK_DISPLAY1("Exit with FAIL exit status: %d\n", STATUS_FAIL);
        NSK_BEFORE_TRACE(exit(STATUS_FAIL));
    }
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_timerinfo001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_timerinfo001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_timerinfo001(JavaVM *jvm, char *options, void *reserved) {
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

    {
        jvmtiEventCallbacks eventCallbacks;

        memset(&eventCallbacks, 0, sizeof(eventCallbacks));
        eventCallbacks.VMInit = callbackVMInit;
        eventCallbacks.VMDeath = callbackVMDeath;
        if (!NSK_JVMTI_VERIFY(
                jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks)))) {
            return JNI_ERR;
        }

    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    NSK_DISPLAY0(">>> Testcase #1: Check initial timer info in Agent_OnLoad()\n");
    {
        if (!checkTimerInfo(jvmti, &initInfo, NULL, "Agent_OnLoad()")) {
            nsk_jvmti_setFailStatus();
        }
    }

    NSK_DISPLAY1("Enable events: %d events\n", EVENTS_COUNT);
    if (nsk_jvmti_enableEvents(JVMTI_ENABLE, EVENTS_COUNT, events, NULL)) {
        NSK_DISPLAY0("  ... enabled\n");
    }

    return JNI_OK;
}

/* ============================================================================= */

}
