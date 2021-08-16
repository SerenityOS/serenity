/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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

extern "C" {

/* ========================================================================== */

/* scaffold objects */
static jlong timeout = 0;

/* test objects */
static jclass object_M = NULL;
static volatile int waitEventsCount = 0;
static volatile int waitedEventsCount = 0;
static volatile int enterEventsCount = 0;
static volatile int enteredEventsCount = 0;
static jrawMonitorID syncLock = NULL;


static jboolean lockSyncLock(jvmtiEnv* jvmti) {
    jboolean status = NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(syncLock));
    if (!status)
        nsk_jvmti_setFailStatus();
    return status;
}

static void unlockSyncLock(jvmtiEnv* jvmti) {
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(syncLock)))
        nsk_jvmti_setFailStatus();
}


/* ========================================================================== */

void JNICALL
MonitorWait(jvmtiEnv *jvmti, JNIEnv* jni,
        jthread thr, jobject obj, jlong tout) {

    if (!NSK_VERIFY(thr != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (!NSK_VERIFY(obj != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* check if event is for tested object */
    if (jni->IsInstanceOf(obj, object_M)) {
        if (lockSyncLock(jvmti)) {
            waitEventsCount++;
            unlockSyncLock(jvmti);
        }
    }
}

void JNICALL
MonitorWaited(jvmtiEnv *jvmti, JNIEnv* jni,
        jthread thr, jobject obj, jboolean timed_out) {

    if (!NSK_VERIFY(thr != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (!NSK_VERIFY(obj != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* check if event is for tested object */
    if (jni->IsInstanceOf(obj, object_M)) {
        if (lockSyncLock(jvmti)) {
            waitedEventsCount++;
            unlockSyncLock(jvmti);
        }
    }
}

void JNICALL
MonitorContendedEnter(jvmtiEnv *jvmti, JNIEnv* jni, jthread thr, jobject obj) {

    if (!NSK_VERIFY(thr != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (!NSK_VERIFY(obj != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* check if event is for tested object */
    if (jni->IsSameObject(object_M, obj)) {
        if (lockSyncLock(jvmti)) {
            enterEventsCount++;
            unlockSyncLock(jvmti);
        }
    }
}

void JNICALL
MonitorContendedEntered(jvmtiEnv *jvmti, JNIEnv* jni, jthread thr, jobject obj) {

    if (!NSK_VERIFY(thr != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (!NSK_VERIFY(obj != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* check if event is for tested object */
    if (jni->IsSameObject(object_M, obj)) {
        if (lockSyncLock(jvmti)) {
            enteredEventsCount++;
            unlockSyncLock(jvmti);
        }
    }
}

/* ========================================================================== */

static int prepare(jvmtiEnv* jvmti, JNIEnv* jni) {
    const char* CLASS_NAME = "nsk/jvmti/scenarios/contention/TC04/tc04t001Thread";

    NSK_DISPLAY0("Obtain tested object from debugee thread class\n");

    if (!NSK_JNI_VERIFY(jni, (object_M = jni->FindClass(CLASS_NAME)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (object_M = (jclass)jni->NewGlobalRef(object_M)) != NULL))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("_syncLock", &syncLock)))
        return NSK_FALSE;

    /* enable MonitorWait event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_WAIT, NULL)))
        nsk_jvmti_setFailStatus();

    /* enable MonitorWaited event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_WAITED, NULL)))
        nsk_jvmti_setFailStatus();

    /* enable MonitorContendedEnter event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(
                JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTER, NULL)))
        nsk_jvmti_setFailStatus();

    /* enable MonitorContendedEntered event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(
                JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, NULL)))
        nsk_jvmti_setFailStatus();

    return NSK_TRUE;
}

static int clean(jvmtiEnv* jvmti, JNIEnv* jni) {

    /* disable MonitorWait event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_MONITOR_WAIT, NULL)))
        nsk_jvmti_setFailStatus();

    /* disable MonitorWaited event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_MONITOR_WAITED, NULL)))
        nsk_jvmti_setFailStatus();

    /* disable MonitorContendedEnter event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(
                JVMTI_DISABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTER, NULL)))
        nsk_jvmti_setFailStatus();

    /* disable MonitorContendedEntered event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(
                JVMTI_DISABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, NULL)))
        nsk_jvmti_setFailStatus();

    if (!NSK_JVMTI_VERIFY(jvmti->DestroyRawMonitor(syncLock)))
        nsk_jvmti_setFailStatus();

    return NSK_TRUE;
}

/* ========================================================================== */

/* agent algorithm */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    /* wait for initial sync */
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!prepare(jvmti, jni)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* resume debugee */
    if (!(NSK_VERIFY(nsk_jvmti_resumeSync()) &&
          NSK_VERIFY(nsk_jvmti_waitForSync(timeout))))
        return;

    // lock
    if (lockSyncLock(jvmti)) {
        NSK_DISPLAY1("Number of MonitorWait events: %d\n", waitEventsCount);
        if (!NSK_VERIFY(waitEventsCount >= 200)) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN1("Number of MonitorWait events: %d\n", waitEventsCount);
        }

        NSK_DISPLAY1("Number of MonitorWaited events: %d\n", waitedEventsCount);
        if (!NSK_VERIFY(waitedEventsCount >= 200)) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN1("Number of MonitorWaited events: %d\n", waitedEventsCount);
        }

        NSK_DISPLAY1("Number of MonitorContendedEnter events: %d\n",
            enterEventsCount);
        if (!NSK_VERIFY(enterEventsCount == 199)) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN1("Number of MonitorContendedEnter events: %d\n",
                enterEventsCount);
        }

        NSK_DISPLAY1("Number of MonitorContendedEntered events: %d\n",
            enteredEventsCount);
        if (!NSK_VERIFY(enteredEventsCount == 199)) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN1("Number of MonitorContendedEntered events: %d\n",
                enteredEventsCount);
        }
        unlockSyncLock(jvmti);
    }

    if (!clean(jvmti, jni)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* resume debugee after last sync */
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/* agent library initialization */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_tc04t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_tc04t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_tc04t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;
    jvmtiCapabilities caps;
    jvmtiEventCallbacks callbacks;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60000;
    NSK_DISPLAY1("Timeout: %d msc\n", (int)timeout);

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* add capabilities */
    memset(&caps, 0, sizeof(caps));
    caps.can_generate_monitor_events = 1;
    caps.can_get_monitor_info = 1;
    caps.can_signal_thread = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.MonitorWait = &MonitorWait;
    callbacks.MonitorWaited = &MonitorWaited;
    callbacks.MonitorContendedEnter = &MonitorContendedEnter;
    callbacks.MonitorContendedEntered = &MonitorContendedEntered;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_scenarios_contention_TC04_tc04t001Thread_enterEventsCount(JNIEnv* jni, jclass klass) {
    return enterEventsCount;
}

/* ========================================================================== */

}
