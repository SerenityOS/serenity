/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include <string.h>
#include "jni_tools.h"
#include "agent_common.h"
#include "jvmti_tools.h"

extern "C" {

/* ========================================================================== */

/* scaffold objects */
static JNIEnv* jni = NULL;
static jvmtiEnv *jvmti = NULL;
static jlong timeout = 0;

/* test objects */
static jthread thread = NULL;
static jobject object = NULL;
static volatile int eventsCount = 0;

/* ========================================================================== */

void JNICALL
MonitorWait(jvmtiEnv *jvmti, JNIEnv* jni, jthread thr, jobject obj, jlong tout) {

    NSK_DISPLAY3("MonitorWait event:\n\tthread: %p, object: %p, timeout: %ld\n",
        thr, obj, (int)tout);

    if (!NSK_VERIFY(thread != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* check if event is for tested thread and for tested object */
    if (jni->IsSameObject(thread, thr) &&
            jni->IsSameObject(object, obj)) {
        eventsCount++;
        if (tout != timeout) {
            NSK_COMPLAIN1("Unexpected timeout value: %d\n", (int)tout);
            nsk_jvmti_setFailStatus();
        }
    }
}

/* ========================================================================== */

static int prepare() {
    const char* THREAD_NAME = "Debuggee Thread";
    jclass klass = NULL;
    jfieldID field = NULL;
    jvmtiThreadInfo info;
    jthread *threads = NULL;
    jint threads_count = 0;
    int i;

    NSK_DISPLAY0("Prepare: find tested thread\n");

    /* get all live threads */
    if (!NSK_JVMTI_VERIFY(jvmti->GetAllThreads(&threads_count, &threads)))
        return NSK_FALSE;

    if (!NSK_VERIFY(threads_count > 0 && threads != NULL))
        return NSK_FALSE;

    /* find tested thread */
    for (i = 0; i < threads_count; i++) {
        if (!NSK_VERIFY(threads[i] != NULL))
            return NSK_FALSE;

        /* get thread information */
        if (!NSK_JVMTI_VERIFY(jvmti->GetThreadInfo(threads[i], &info)))
            return NSK_FALSE;

        NSK_DISPLAY3("    thread #%d (%s): %p\n", i, info.name, threads[i]);

        /* find by name */
        if (info.name != NULL && (strcmp(info.name, THREAD_NAME) == 0)) {
            thread = threads[i];
        }
    }

    /* deallocate threads list */
    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)threads)))
        return NSK_FALSE;

    if (thread == NULL) {
        NSK_COMPLAIN0("Debuggee thread not found");
        return NSK_FALSE;
    }

    /* make thread accessable for a long time */
    if (!NSK_JNI_VERIFY(jni, (thread = jni->NewGlobalRef(thread)) != NULL))
        return NSK_FALSE;

    /* get tested thread class */
    if (!NSK_JNI_VERIFY(jni, (klass = jni->GetObjectClass(thread)) != NULL))
        return NSK_FALSE;

    /* get tested thread field 'waitingMonitor' */
    if (!NSK_JNI_VERIFY(jni, (field =
            jni->GetFieldID(klass, "waitingMonitor", "Ljava/lang/Object;")) != NULL))
        return NSK_FALSE;

    /* get 'waitingMonitor' object */
    if (!NSK_JNI_VERIFY(jni, (object = jni->GetObjectField(thread, field)) != NULL))
        return NSK_FALSE;

    /* make object accessable for a long time */
    if (!NSK_JNI_VERIFY(jni, (object = jni->NewGlobalRef(object)) != NULL))
        return NSK_FALSE;

    /* enable MonitorWait event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_WAIT, NULL)))
        return NSK_FALSE;

    return NSK_TRUE;
}

static int clean() {

    /* disable MonitorWait event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_MONITOR_WAIT, NULL)))
        nsk_jvmti_setFailStatus();

    return NSK_TRUE;
}

/* ========================================================================== */

/* agent algorithm
 */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* agentJNI, void* arg) {
    jni = agentJNI;

    /* wait for initial sync */
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!prepare()) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* clear events count */
    eventsCount = 0;

    /* resume debugee to catch MonitorWait event */
    if (!(NSK_VERIFY(nsk_jvmti_resumeSync()) &&
          NSK_VERIFY(nsk_jvmti_waitForSync(timeout))))
        return;

    NSK_DISPLAY1("Number of MonitorWait events: %d\n", eventsCount);

    if (eventsCount == 0) {
        NSK_COMPLAIN0("No any MonitorWait event\n");
        nsk_jvmti_setFailStatus();
    }

    if (!clean()) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* resume debugee after last sync */
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/* agent library initialization
 */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_monitorwait001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_monitorwait001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_monitorwait001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
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

    if (!NSK_JVMTI_VERIFY(jvmti->GetPotentialCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_VERIFY(caps.can_generate_monitor_events))
        return JNI_ERR;

    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.MonitorWait = &MonitorWait;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

}
