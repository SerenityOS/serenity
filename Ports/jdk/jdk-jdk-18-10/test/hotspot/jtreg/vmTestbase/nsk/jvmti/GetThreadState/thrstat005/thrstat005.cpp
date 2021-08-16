/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 */

#include <stdio.h>
#include <string.h>
#include "jvmti.h"
#include "agent_common.h"

#ifndef STANDALONE
#include "JVMTITools.h"
#endif

extern "C" {


#define THREAD_STATE_MASK ~(JVMTI_THREAD_STATE_SUSPENDED \
                            | JVMTI_THREAD_STATE_INTERRUPTED \
                            | JVMTI_THREAD_STATE_IN_NATIVE \
                            | JVMTI_THREAD_STATE_VENDOR_1 \
                            | JVMTI_THREAD_STATE_VENDOR_2 \
                            | JVMTI_THREAD_STATE_VENDOR_3)

static int g_ThreadState[] = {
    0,                                                 /* TS_NEW */
    JVMTI_THREAD_STATE_TERMINATED,                     /* TS_TERMINATED */
    JVMTI_THREAD_STATE_ALIVE
        | JVMTI_THREAD_STATE_RUNNABLE,                 /* TS_RUN_RUNNING */
    JVMTI_THREAD_STATE_ALIVE
        | JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER, /* TS_RUN_BLOCKED */
    JVMTI_THREAD_STATE_ALIVE
        | JVMTI_THREAD_STATE_IN_OBJECT_WAIT
        | JVMTI_THREAD_STATE_WAITING
        | JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT,     /* TS_RUN_WAIT_TIMED */
    JVMTI_THREAD_STATE_ALIVE
        | JVMTI_THREAD_STATE_IN_OBJECT_WAIT
        | JVMTI_THREAD_STATE_WAITING
        | JVMTI_THREAD_STATE_WAITING_INDEFINITELY,     /* TS_RUN_WAIT_INDEF */
    JVMTI_THREAD_STATE_ALIVE
        | JVMTI_THREAD_STATE_PARKED
        | JVMTI_THREAD_STATE_WAITING
        | JVMTI_THREAD_STATE_WAITING_INDEFINITELY,     /* TS_RUN_WAIT_PARKED_INDEF */
    JVMTI_THREAD_STATE_ALIVE
        | JVMTI_THREAD_STATE_PARKED
        | JVMTI_THREAD_STATE_WAITING
        | JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT,     /* TS_RUN_WAIT_PARKED_TIMED */
    JVMTI_THREAD_STATE_ALIVE
        | JVMTI_THREAD_STATE_SLEEPING
        | JVMTI_THREAD_STATE_WAITING
        | JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT,     /* TS_RUN_WAIT_SLEEPING */
};

static jvmtiEnv * g_ppJvmtiEnv = NULL;
static int g_waitTime = 1000;
jrawMonitorID g_waitMon; /* Monitor is used just for sleeping */

void reportError(const char * szErr, jvmtiError res) {
#ifndef STANDALONE
    printf("%s (%d: %s)\n", szErr, res, TranslateError(res));
#else
    printf("%s (%d)\n", szErr, res);
#endif
    fflush(stdout);
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_thrstat005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_thrstat005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_thrstat005(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiError error;
    jint res;

    res = jvm->GetEnv((void **) &g_ppJvmtiEnv, JVMTI_VERSION_1_1);
    if (res != JNI_OK || !g_ppJvmtiEnv) {
        printf("Agent_OnLoad: Error: GetEnv returned error or NULL\n");
        return JNI_ERR;
    }

    error = g_ppJvmtiEnv->CreateRawMonitor("beast", &g_waitMon);
    if (error != JVMTI_ERROR_NONE) {
        reportError("Agent_OnLoad: error creating raw monitor", error);
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetThreadState_thrstat005_setWaitTime(JNIEnv * pEnv, jclass klass, jint waitTime) {
    g_waitTime = waitTime;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_GetThreadState_thrstat005_checkThreadState(JNIEnv * pEnv, jclass klass, jthread thread, jint stateIdx) {

    jint thrState;
    jint maskedThrState;
    int waitTime = 10;

    /* Repeat querying status until waitTime < g_waitTime */
    do {
        jvmtiError res = g_ppJvmtiEnv->GetThreadState(thread, &thrState);
        if (res != JVMTI_ERROR_NONE) {
            reportError("GetThreadState: unexpected error", res);
            return JNI_FALSE;
        }

        maskedThrState = thrState & THREAD_STATE_MASK;
        printf("GetThreadState = %x. Masked: %x. Must be: %x\n", thrState, maskedThrState, g_ThreadState[stateIdx]);
        fflush(stdout);

        if (maskedThrState == g_ThreadState[stateIdx])
            return JNI_TRUE;

        printf("checkThreadState: wait %d ms\n", waitTime);
        fflush(stdout);
        res = g_ppJvmtiEnv->RawMonitorEnter(g_waitMon);
        if (res != JVMTI_ERROR_NONE) {
            reportError("GetThreadState: unexpected error from RawMontiorEnter", res);
            return JNI_FALSE;
        }
        res = g_ppJvmtiEnv->RawMonitorWait(g_waitMon, waitTime);
        if (res != JVMTI_ERROR_NONE) {
            reportError("GetThreadState: unexpected error from RawMontiorWait", res);
            return JNI_FALSE;
        }
        res = g_ppJvmtiEnv->RawMonitorExit(g_waitMon);
        if (res != JVMTI_ERROR_NONE) {
            reportError("GetThreadState: unexpected error from RawMonitorExit", res);
            return JNI_FALSE;
        }

        waitTime <<= 1;

    } while (waitTime < g_waitTime);

    return JNI_FALSE;
}

}
