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

#define TESTED_THREAD_NAME      "curthrcputime001Thread"

#define STATUS_FAIL     97

#define EVENTS_COUNT    2

static jvmtiEvent events[EVENTS_COUNT] = {
    JVMTI_EVENT_VM_INIT,
    JVMTI_EVENT_VM_DEATH
};

#define THREAD_EVENTS_COUNT    2

static jvmtiEvent threadEvents[THREAD_EVENTS_COUNT] = {
    JVMTI_EVENT_THREAD_START,
    JVMTI_EVENT_THREAD_END
};

static julong prevTestedThreadTime = 0;
static julong prevAgentThreadTime = 0;

static int iterations = 0;

/* ============================================================================= */

/**
 * Get time and optionally compares it with initial one.
 * @returns NSK_FALSE if any error occured.
 */
static int checkCpuTime(jvmtiEnv* jvmti, jthread thread, julong* time,
                            julong* prevTime, const char where[]) {

    char buf[64], buf2[64], buf3[64];
    int success = NSK_TRUE;

    NSK_DISPLAY1("GetCurrentThreadCpuTime() for current thread: 0x%p\n", (void*)thread);
    if (!NSK_JVMTI_VERIFY(jvmti->GetCurrentThreadCpuTime((jlong *)time))) {
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got cpu time: %s\n", julong_to_string(*time, buf));

    if (*time == 0) {
        NSK_DISPLAY2("# WARNING: In %s GetCurrentThreadCpuTime() returned zero cpu time: %s\n",
                            where, julong_to_string(*time, buf));
    }

    if (prevTime != NULL) {
        julong diff = *time - *prevTime;

        NSK_DISPLAY1("Compare with previous time: %s\n",
                            julong_to_string(*prevTime, buf));
        NSK_DISPLAY1("  ... difference: %s\n",
                            julong_to_string(diff, buf));

        if (*time < *prevTime) {
            NSK_COMPLAIN4("In %s GetCurrentThreadCpuTime() returned decreased cpu time:\n"
                          "#   got cpu time: %s\n"
                          "#   previous:     %s\n"
                          "#   difference:   %s\n",
                            where,
                            julong_to_string(*time, buf),
                            julong_to_string(*prevTime, buf2),
                            julong_to_string(diff, buf3));
            success = NSK_FALSE;
        }

        if (*time == *prevTime) {
            NSK_DISPLAY3("# WARNING: In %s GetCurrentThreadCpuTime() returned not increased cpu time:\n"
                          "#   got cpu time: %s\n"
                          "#   previous:     %s\n",
                            where,
                            julong_to_string(*time, buf),
                            julong_to_string(*prevTime, buf2));
        }
        *prevTime = *time;
    }

    return success;
}

/** Run some code. */
static void runIterations(int n) {
    int k;

    for (k = 0; k < n; k++) {
        int s = k;
        int i;

        for (i = 0; i < n; i++) {
            if (i % 2 == 0) {
                s += i * 10;
            } else {
                s -= i * 10;
            }
        }
    }
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    jthread testAgentThread = nsk_jvmti_getAgentThread();
    NSK_DISPLAY1("Started agent thread: 0x%p\n", testAgentThread);

    NSK_DISPLAY0("Wait for debugee to become ready\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    NSK_DISPLAY0(">>> Testcase #2: Check initial cpu time in agent thread\n");
    {
        if (!checkCpuTime(jvmti, testAgentThread, &prevAgentThreadTime, NULL, "agent thread")) {
            nsk_jvmti_setFailStatus();
        }
    }

    NSK_DISPLAY0(">>> Testcases #3,5: Check cpu times in tested thread events\n");
    {
        runIterations(iterations);

        NSK_DISPLAY1("Enable thread events: %d events\n", THREAD_EVENTS_COUNT);
        if (nsk_jvmti_enableEvents(JVMTI_ENABLE, THREAD_EVENTS_COUNT, threadEvents, NULL)) {
            NSK_DISPLAY0("  ... enabled\n");
        }

        NSK_DISPLAY0("Let tested thread to start\n");
        if (!nsk_jvmti_resumeSync())
            return;
        if (!nsk_jvmti_waitForSync(timeout))
            return;


        NSK_DISPLAY0(">>> Testcase #4: Check middle cpu time in agent thread\n");
        {
            julong time = 0;
            runIterations(iterations);
            if (!checkCpuTime(jvmti, testAgentThread, &time, &prevAgentThreadTime, "agent thread")) {
                nsk_jvmti_setFailStatus();
            }
        }

        NSK_DISPLAY0("Let tested thread to finish\n");
        if (!nsk_jvmti_resumeSync())
            return;
        if (!nsk_jvmti_waitForSync(timeout))
            return;

        NSK_DISPLAY1("Disable thread events: %d events\n", THREAD_EVENTS_COUNT);
        if (nsk_jvmti_enableEvents(JVMTI_DISABLE, THREAD_EVENTS_COUNT, threadEvents, NULL)) {
            NSK_DISPLAY0("  ... disabled\n");
        }
    }

    NSK_DISPLAY0(">>> Testcase #6: Check final cpu time in agent thread\n");
    {
        julong time = 0;
        runIterations(iterations);
        if (!checkCpuTime(jvmti, testAgentThread, &time, &prevAgentThreadTime, "agent thread")) {
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

    NSK_DISPLAY0(">>> Testcase #1: Check initial cpu time in VM_INIT callback\n");
    {
        julong time = 0;
        if (!checkCpuTime(jvmti, thread, &time, NULL, "VM_INIT callback")) {
            nsk_jvmti_setFailStatus();
        }
    }
}

/**
 * Callback for VM_DEATH event.
 */
JNIEXPORT void JNICALL
callbackVMDeath(jvmtiEnv* jvmti, JNIEnv* jni) {
    int success = NSK_TRUE;

    NSK_DISPLAY0(">>> Testcase #7: Check initial cpu time in VM_DEATH callback\n");
    {
        julong time = 0;
        if (!checkCpuTime(jvmti, NULL, &time, NULL, "VM_DEATH callback")) {
            success = NSK_FALSE;
            nsk_jvmti_setFailStatus();
        }
    }

    NSK_DISPLAY1("Disable events: %d events\n", EVENTS_COUNT);
    if (!nsk_jvmti_enableEvents(JVMTI_DISABLE, EVENTS_COUNT, events, NULL)) {
        success = NSK_FALSE;
    } else {
        NSK_DISPLAY0("  ... disabled\n");
    }

    if (!success) {
        NSK_DISPLAY1("Exit with FAIL exit status: %d\n", STATUS_FAIL);
        NSK_BEFORE_TRACE(exit(STATUS_FAIL));
    }
}

/* ============================================================================= */

/**
 * Callback for THREAD_START event.
 */
JNIEXPORT void JNICALL
callbackThreadStart(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread) {

    jvmtiThreadInfo threadInfo;
    {
        if (!NSK_JVMTI_VERIFY(jvmti->GetThreadInfo(thread, &threadInfo))) {
            nsk_jvmti_setFailStatus();
            return;
        }
        NSK_DISPLAY1("    <THREAD_START> for thread: %s\n", nsk_null_string(threadInfo.name));
    }

    if (threadInfo.name != NULL && strcmp(threadInfo.name, TESTED_THREAD_NAME) == 0) {
        NSK_DISPLAY0(">>> Testcase #3: Check initial cpu time in THREAD_START callback\n");
        if (!checkCpuTime(jvmti, thread, &prevTestedThreadTime, NULL, "THREAD_START callback")) {
            nsk_jvmti_setFailStatus();
        }
    }
}

/**
 * Callback for THREAD_END event.
 */
JNIEXPORT void JNICALL
callbackThreadEnd(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread) {

    jvmtiThreadInfo threadInfo;
    {
        if (!NSK_JVMTI_VERIFY(jvmti->GetThreadInfo(thread, &threadInfo))) {
            nsk_jvmti_setFailStatus();
            return;
        }
        NSK_DISPLAY1("    <THREAD_END>   for thread: %s\n", nsk_null_string(threadInfo.name));
    }

    if (threadInfo.name != NULL && strcmp(threadInfo.name, TESTED_THREAD_NAME) == 0) {
        julong time = 0;
        NSK_DISPLAY0(">>> Testcase #5: Check final cpu time in THREAD_END callback\n");
        if (!checkCpuTime(jvmti, thread, &time, &prevTestedThreadTime, "THREAD_END callback")) {
            nsk_jvmti_setFailStatus();
        }
    }
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_curthrcputime001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_curthrcputime001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_curthrcputime001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    iterations = nsk_jvmti_findOptionIntValue("iterations", 1000);
    if (!NSK_VERIFY(iterations >= 1000))
        return JNI_ERR;

    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    NSK_DISPLAY1("Add required capability: %s\n", "can_get_current_thread_cpu_time");
    {
        jvmtiCapabilities caps;

        memset(&caps, 0, sizeof(caps));
        caps.can_get_current_thread_cpu_time = 1;
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
            return JNI_ERR;
        }
    }
    NSK_DISPLAY0("  ... capability added\n");

    NSK_DISPLAY1("Set events callbacks: %s\n", "VM_INIT, VM_DEATH, THREAD_START, THREAD_END");
    {
        jvmtiEventCallbacks eventCallbacks;

        memset(&eventCallbacks, 0, sizeof(eventCallbacks));
        eventCallbacks.VMInit = callbackVMInit;
        eventCallbacks.VMDeath = callbackVMDeath;
        eventCallbacks.ThreadStart = callbackThreadStart;
        eventCallbacks.ThreadEnd = callbackThreadEnd;
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks)))) {
            return JNI_ERR;
        }
    }
    NSK_DISPLAY0("  ... callbacks set\n");

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    NSK_DISPLAY1("Enable events: %d events\n", EVENTS_COUNT);
    if (nsk_jvmti_enableEvents(JVMTI_ENABLE, EVENTS_COUNT, events, NULL)) {
        NSK_DISPLAY0("  ... enabled\n");
    }

    return JNI_OK;
}

/* ============================================================================= */

}
