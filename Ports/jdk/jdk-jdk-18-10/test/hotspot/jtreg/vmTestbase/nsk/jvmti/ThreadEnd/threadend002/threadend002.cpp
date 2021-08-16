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
#include "JVMTITools.h"

extern "C" {

/* ============================================================================= */

/* scaffold objects */
static jvmtiEnv *jvmti = NULL;
static jlong timeout = 0;

static int eventCount = 0;

/* ============================================================================= */

JNIEXPORT void JNICALL
cbThreadEnd(jvmtiEnv* jvmti, JNIEnv* jni_env, jthread thread) {

    eventCount++;
}

/* ============================================================================= */

static int
enableEvent(jvmtiEventMode enable, jvmtiEvent event) {

    if (enable == JVMTI_ENABLE) {
        NSK_DISPLAY1("enabling %s\n", TranslateEvent(event));
    } else {
        NSK_DISPLAY1("disabling %s\n", TranslateEvent(event));
    }

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(enable, event, NULL))) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }

    return NSK_TRUE;
}

/* ============================================================================= */

int checkEvents() {

    int result = NSK_TRUE;

    if (eventCount == 0) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN0("Number of THREAD_END events must be greater than 0\n");
        nsk_jvmti_setFailStatus();
        result = NSK_FALSE;
    }

    return result;
}

/* ============================================================================= */

static int
setCallBacks() {
    jvmtiEventCallbacks eventCallbacks;
    memset(&eventCallbacks, 0, sizeof(eventCallbacks));

    eventCallbacks.ThreadEnd = cbThreadEnd;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks))))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* agentJNI, void* arg) {

    NSK_DISPLAY0("Wait for debuggee to become ready\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    NSK_DISPLAY0("Let debuggee to continue\n");
    if (!nsk_jvmti_resumeSync())
        return;

    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!checkEvents()) {
        nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY0("Let debuggee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;

}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_threadend002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_threadend002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_threadend002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    jvmti = nsk_jvmti_createJVMTIEnv(jvm, reserved);
    if (!NSK_VERIFY(jvmti != NULL))
        return JNI_ERR;

    if (!setCallBacks()) {
        return JNI_ERR;
    }

    if (!enableEvent(JVMTI_ENABLE, JVMTI_EVENT_THREAD_END)) {
        NSK_COMPLAIN0("Events could not be enabled");
        nsk_jvmti_setFailStatus();
        return JNI_ERR;
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

}
