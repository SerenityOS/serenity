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

/* event counts */
static int MonitorContendedEnterEventsCount = 0;
static int MonitorContendedEnteredEventsCount = 0;
static int MonitorWaitEventsCount = 0;
static int MonitorWaitedEventsCount = 0;

/* ========================================================================== */

/** callback functions **/

static void JNICALL
MonitorContendedEnter(jvmtiEnv *jvmti_env, JNIEnv* jni_env,
        jthread thread, jobject object) {
    jvmtiThreadInfo info;

    MonitorContendedEnterEventsCount++;

    /* get thread information */
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetThreadInfo(thread, &info))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY2("MonitorContendedEnter event: thread=\"%s\", object=0x%p\n",
        info.name, object);
}

static void JNICALL
MonitorContendedEntered(jvmtiEnv *jvmti_env, JNIEnv* jni_env,
        jthread thread, jobject object) {
    jvmtiThreadInfo info;

    MonitorContendedEnteredEventsCount++;

    /* get thread information */
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetThreadInfo(thread, &info))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY2("MonitorContendedEntered event: thread=\"%s\", object=0x%p\n",
        info.name, object);
}

static void JNICALL
MonitorWait(jvmtiEnv *jvmti_env, JNIEnv* jni_env,
        jthread thread, jobject object, jlong timeout) {
    jvmtiThreadInfo info;

    MonitorWaitEventsCount++;

    /* get thread information */
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetThreadInfo(thread, &info))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY2("MonitorWait event: thread=\"%s\", object=0x%p\n",
        info.name, object);
}

static void JNICALL
MonitorWaited(jvmtiEnv *jvmti_env, JNIEnv* jni_env,
        jthread thread, jobject object, jboolean timed_out) {
    jvmtiThreadInfo info;

    MonitorWaitedEventsCount++;

    /* get thread information */
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetThreadInfo(thread, &info))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY2("MonitorWaited event: thread=\"%s\", object=0x%p\n",
        info.name, object);
}

/* ========================================================================== */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    if (!nsk_jvmti_waitForSync(timeout))
        return;

    /* resume debugee and wait for sync */
    if (!nsk_jvmti_resumeSync())
        return;
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    NSK_DISPLAY1("MonitorContendedEnter events received: %d\n",
        MonitorContendedEnterEventsCount);
    if (!NSK_VERIFY(MonitorContendedEnterEventsCount != 0))
        nsk_jvmti_setFailStatus();

    NSK_DISPLAY1("MonitorContendedEntered events received: %d\n",
        MonitorContendedEnteredEventsCount);
    if (!NSK_VERIFY(MonitorContendedEnteredEventsCount != 0))
        nsk_jvmti_setFailStatus();

    NSK_DISPLAY1("MonitorWait events received: %d\n",
        MonitorWaitEventsCount);
    if (!NSK_VERIFY(MonitorWaitEventsCount != 0))
        nsk_jvmti_setFailStatus();

    NSK_DISPLAY1("MonitorWaited events received: %d\n",
        MonitorWaitedEventsCount);
    if (!NSK_VERIFY(MonitorWaitedEventsCount != 0))
        nsk_jvmti_setFailStatus();

    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ma10t008(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ma10t008(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ma10t008(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;
    jvmtiCapabilities caps;
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

    memset(&caps, 0, sizeof(caps));
    caps.can_generate_monitor_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
        return JNI_ERR;
    }

    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.MonitorContendedEnter = &MonitorContendedEnter;
    callbacks.MonitorContendedEntered = &MonitorContendedEntered;
    callbacks.MonitorWait = &MonitorWait;
    callbacks.MonitorWaited = &MonitorWaited;
    if (!NSK_VERIFY(nsk_jvmti_init_MA(&callbacks)))
        return JNI_ERR;

    /* enable events */
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTER, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_WAIT, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_WAITED, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

}
