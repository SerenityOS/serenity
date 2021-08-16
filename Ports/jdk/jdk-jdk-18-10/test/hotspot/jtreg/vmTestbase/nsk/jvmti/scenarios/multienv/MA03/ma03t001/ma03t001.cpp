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

#include <stdlib.h>
#include <string.h>
#include "jni_tools.h"
#include "agent_common.h"
#include "jvmti_tools.h"

#define PASSED 0
#define STATUS_FAILED 2

extern "C" {

/* ========================================================================== */

/* scaffold objects */
static jlong timeout = 0;

/* test objects */
static int VMInitEventsCount = 0;
static int VMInitEventsExpected = 1;
static int VMDeathEventsCount = 0;
static int VMDeathEventsExpected = 1;
static int ThreadStartEventsCount = 0;

/* ========================================================================== */

/** callback functions **/

void JNICALL VMInit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr) {
    NSK_DISPLAY0("VMInit event\n");
    VMInitEventsCount++;
}

void JNICALL ThreadStart(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr) {
    NSK_DISPLAY0("ThreadStart event\n");
    ThreadStartEventsCount++;
}

void JNICALL
VMDeath(jvmtiEnv *jvmti_env, JNIEnv *env) {
    NSK_DISPLAY0("VMDeath event\n");
    VMDeathEventsCount++;
}

/* ========================================================================== */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (VMInitEventsCount != VMInitEventsExpected) {
        NSK_COMPLAIN2("Wrong number of VM init events: %d, expected: %d\n",
            VMInitEventsCount, VMInitEventsExpected);
        nsk_jvmti_setFailStatus();
    }

    if (ThreadStartEventsCount == 0) {
        NSK_COMPLAIN0("No thread start events\n");
        nsk_jvmti_setFailStatus();
    }

    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ma03t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ma03t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ma03t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;
    jvmtiEventCallbacks callbacks;

    NSK_DISPLAY0("Agent_OnLoad\n");

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.VMInit = &VMInit;
    callbacks.ThreadStart = &ThreadStart;
    callbacks.VMDeath = &VMDeath;
    if (!NSK_VERIFY(nsk_jvmti_init_MA(&callbacks)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_START, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

/* agent library shutdown */
JNIEXPORT void JNICALL
#ifdef STATIC_BUILD
Agent_OnUnload_ma03t001(JavaVM *jvm)
#else
Agent_OnUnload(JavaVM *jvm)
#endif
{
    if (VMDeathEventsCount != VMDeathEventsExpected) {
        NSK_COMPLAIN2("Wrong number of VM death events: %d, expected: %d\n",
            VMDeathEventsCount, VMDeathEventsExpected);
        exit(97);
    }
}

/* ========================================================================== */

}
