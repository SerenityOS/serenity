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

static julong prevTime;

/* ============================================================================= */

/**
 * Get time and optionally compare it with previous one.
 * @returns false if any error occured.
 */
static bool checkTime(jvmtiEnv* jvmti, julong* time,
                      julong* prevTime, const char where[]) {

    char buf[32], buf2[32], buf3[32];
    bool success = true;

    NSK_DISPLAY0("GetTime() for current JVMTI env\n");
    if (!NSK_JVMTI_VERIFY(
            jvmti->GetTime((jlong *)time))) {
        return false;
    }
    NSK_DISPLAY1("  ... got time: %s\n", julong_to_string(*time, buf));

    if (prevTime != NULL) {
        julong diff = *time - *prevTime;

        NSK_DISPLAY1("Compare with previous time: %s\n",
                            julong_to_string(*prevTime, buf));
        NSK_DISPLAY1("  ... difference: %s\n",
                            julong_to_string(diff, buf));

        if (*time < *prevTime) {
            NSK_COMPLAIN4("In %s GetTime() returned value less than previous:\n"
                          "#   got value:  %s\n"
                          "#   previous:   %s\n"
                          "#   difference: %s\n",
                            where,
                            julong_to_string(*time, buf),
                            julong_to_string(*prevTime, buf2),
                            julong_to_string(diff, buf3));
            success = false;
        }
        *prevTime = *time;
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

    NSK_DISPLAY0(">>> Testcase #3: Check time in agent thread\n");
    {
        julong time;
        if (!checkTime(jvmti, &time, &prevTime, "agent thread")) {
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

    NSK_DISPLAY0(">>> Testcase #2: Check time in VM_INIT callback\n");
    {
        julong time;
        if (!checkTime(jvmti, &time, &prevTime, "VM_INIT callback")) {
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

    NSK_DISPLAY0(">>> Testcase #4: Check time in VM_DEATH callback\n");
    {
        julong time;
        success = checkTime(jvmti, &time, &prevTime, "VM_DEATH callback");
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
JNIEXPORT jint JNICALL Agent_OnLoad_gettime001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_gettime001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_gettime001(JavaVM *jvm, char *options, void *reserved) {
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

    NSK_DISPLAY0(">>> Testcase #1: Check initial time in Agent_OnLoad()\n");
    {
        if (!checkTime(jvmti, &prevTime, NULL, "Agent_OnLoad()")) {
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
