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
#include "jni_tools.h"

extern "C" {

#define STATUS_FAILED 2
#define PASSED 0

static jint result = STATUS_FAILED;
static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jboolean eventReceived = JNI_FALSE;

static jrawMonitorID dataDumpRequestMonitor;

/** callback functions **/
static void JNICALL DataDumpRequest(jvmtiEnv *env) {
    jvmtiPhase phase;

    rawMonitorEnter(env, dataDumpRequestMonitor);

    NSK_DISPLAY0(">>>> DataDumpRequest event received\n");
    eventReceived = JNI_TRUE;

    getPhase(jvmti, &phase);

    if (phase != JVMTI_PHASE_LIVE) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: DataDumpRequest event received during non-live phase %s\n",
                      TranslatePhase(phase));
    } else {
        result = PASSED;
        NSK_DISPLAY0("CHECK PASSED: DataDumpRequest event received during the live phase as expected\n");
    }

    NSK_DISPLAY0("<<<<\n\n");

    rawMonitorNotify(env, dataDumpRequestMonitor);
    rawMonitorExit(env, dataDumpRequestMonitor);
}

static void waitDumpRequestReceived(jvmtiEnv *env) {
    rawMonitorEnter(env, dataDumpRequestMonitor);

    while (eventReceived == JNI_FALSE) {
        NSK_DISPLAY0("waiting for DataDumpRequest event...\n");
        rawMonitorWait(env, dataDumpRequestMonitor, 0);
    }

    rawMonitorExit(env, dataDumpRequestMonitor);
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_DataDumpRequest_datadumpreq001_waitForResult(JNIEnv *env, jclass cls) {
    waitDumpRequestReceived(jvmti);
    return result;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_datadumpreq001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_datadumpreq001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_datadumpreq001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif

jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    /* create JVMTI environment */
    jvmti = nsk_jvmti_createJVMTIEnv(jvm, reserved);
    if (!NSK_VERIFY(jvmti != NULL)) {
        return JNI_ERR;
    }

    if (createRawMonitor(jvmti, "data dump request monitor", &dataDumpRequestMonitor) != JNI_OK) {
        return JNI_ERR;
    }

    /* set event callbacks */
    NSK_DISPLAY0("setting event callbacks ...\n");
    (void) memset(&callbacks, 0, sizeof(callbacks));
    callbacks.DataDumpRequest = &DataDumpRequest;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    NSK_DISPLAY0("setting event callbacks done\nenabling JVMTI events ...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_DATA_DUMP_REQUEST, NULL)))
        return JNI_ERR;
    NSK_DISPLAY0("enabling the events done\n\n");

    return JNI_OK;
}

}
