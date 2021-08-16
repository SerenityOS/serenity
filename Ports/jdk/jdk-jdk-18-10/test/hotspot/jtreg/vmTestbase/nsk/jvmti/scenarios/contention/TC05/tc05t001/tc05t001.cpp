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

static const jlong EXPECTED_TIMEOUT = 1;
/*
 * The expected timeout accuracy was already increased from 100000 to 300000.
 * Please, do not increase it anymore if the test still fails with the message:
 *  "(waitedTime - waitTime) >= (EXPECTED_TIMEOUT * 1000000) - EXPECTED_TIMEOUT_ACCURACY_NS"
 */
static const jlong EXPECTED_TIMEOUT_ACCURACY_NS = 300000;

#if (defined(WIN32) || defined(_WIN32))
static const jlong EXPECTED_ACCURACY = 16; // 16ms is longest clock update interval
#else
static const jlong EXPECTED_ACCURACY = 10; // high frequency clock updates expected
#endif

/* scaffold objects */
static jlong timeout = 0;

/* test objects */
static jthread thread = NULL;
static jobject object_M = NULL;
static volatile int waitEventsCount = 0;
static volatile int waitedEventsCount = 0;
static jlong waitTime = 0;
static jlong waitThreadCpuTime = 0;
static jlong waitedTime = 0;
static jlong waitedThreadCpuTime = 0;

/* ========================================================================== */

void JNICALL
MonitorWait(jvmtiEnv *jvmti, JNIEnv* jni,
        jthread thr, jobject obj, jlong tout) {
    char buffer[32];

    if (!NSK_VERIFY(thr != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (!NSK_VERIFY(obj != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* check if event is for tested thread and object */
    if (jni->IsSameObject(thread, thr) &&
            jni->IsSameObject(object_M, obj)) {
        if (!NSK_JVMTI_VERIFY(jvmti->GetThreadCpuTime(thr, &waitThreadCpuTime))) {
            nsk_jvmti_setFailStatus();
        }
        if (!NSK_JVMTI_VERIFY(jvmti->GetTime(&waitTime))) {
            nsk_jvmti_setFailStatus();
        }
        waitEventsCount++;
        NSK_DISPLAY0("MonitorWait event:\n");
        NSK_DISPLAY3("\tthread: %p, object: %p, timeout: %s\n",
            thr, obj, jlong_to_string(tout, buffer));
        NSK_DISPLAY1("\ttime: %s\n",
            jlong_to_string(waitTime, buffer));
        NSK_DISPLAY1("\tthread CPU time: %s\n",
            jlong_to_string(waitThreadCpuTime, buffer));

        if (!NSK_VERIFY(tout == EXPECTED_TIMEOUT)) {
            nsk_jvmti_setFailStatus();
        }
    }
}

void JNICALL
MonitorWaited(jvmtiEnv *jvmti, JNIEnv* jni,
        jthread thr, jobject obj, jboolean timed_out) {
    char buffer[32];

    if (!NSK_VERIFY(thr != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (!NSK_VERIFY(obj != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* check if event is for tested thread and object */
    if (jni->IsSameObject(thread, thr) &&
            jni->IsSameObject(object_M, obj)) {
        if (!NSK_JVMTI_VERIFY(jvmti->GetThreadCpuTime(thr, &waitedThreadCpuTime))) {
            nsk_jvmti_setFailStatus();
        }
        if (!NSK_JVMTI_VERIFY(jvmti->GetTime(&waitedTime))) {
            nsk_jvmti_setFailStatus();
        }
        waitedEventsCount++;
        NSK_DISPLAY0("MonitorWaited event:\n");
        NSK_DISPLAY3("\tthread: %p, object: %p, timed_out: %s\n",
            thr, obj, (timed_out == JNI_TRUE) ? "true" : "false");
        NSK_DISPLAY1("\tGetTime: %s\n",
            jlong_to_string(waitedTime, buffer));
        NSK_DISPLAY1("\tthread CPU time: %s\n",
            jlong_to_string(waitedThreadCpuTime, buffer));
    }
}

/* ========================================================================== */

static int prepare(jvmtiEnv* jvmti, JNIEnv* jni) {
    const char* THREAD_NAME = "Debuggee Thread";
    const char* FIELD_SIG = "Ljava/lang/Object;";
    jvmtiThreadInfo info;
    jthread *threads = NULL;
    jint threads_count = 0;
    jfieldID field = NULL;
    jclass klass = NULL;
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

        if (info.name != NULL) {
            if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)info.name)))
                return NSK_FALSE;
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

    /* get tested thread field 'M' */
    if (!NSK_JNI_VERIFY(jni, (field = jni->GetFieldID(klass, "M", FIELD_SIG)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (object_M = jni->GetObjectField(thread, field)) != NULL))
        return NSK_FALSE;

    /* make object accessable for a long time */
    if (!NSK_JNI_VERIFY(jni, (object_M = jni->NewGlobalRef(object_M)) != NULL))
        return NSK_FALSE;

    /* enable MonitorWait event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_WAIT, NULL)))
        return NSK_FALSE;

    /* enable MonitorWaited event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_MONITOR_WAITED, NULL)))
        return NSK_FALSE;

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

    return NSK_TRUE;
}

/* ========================================================================== */

/* agent algorithm */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    char buffer[32];

    /* wait for initial sync */
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!prepare(jvmti, jni)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* resume debugee to catch MonitorContendedEntered events */
    if (!(NSK_VERIFY(nsk_jvmti_resumeSync()) &&
          NSK_VERIFY(nsk_jvmti_waitForSync(timeout))))
        return;

    NSK_DISPLAY1("Number of MonitorWait events: %d\n", waitEventsCount);
    if (!(NSK_VERIFY(waitEventsCount == 1))) {
        nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY1("Number of MonitorWaited events: %d\n", waitedEventsCount);
    if (!(NSK_VERIFY(waitedEventsCount == 1))) {
        nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY1("Time frame between the events: %s ns\n",
        jlong_to_string(waitedTime - waitTime, buffer));
    if (!(NSK_VERIFY((waitedTime - waitTime) >= (EXPECTED_TIMEOUT * 1000000) - EXPECTED_TIMEOUT_ACCURACY_NS))) {
#if (defined(WIN32) || defined(_WIN32))
        /* Do not fail on Windows as early returns are expected and wait() treats them as spurious wakeups. */
#else
        nsk_jvmti_setFailStatus();
#endif
        printf("waitedTime: %" LL "d,  waitTime: %" LL "d, waitedTime - waitTime: %" LL "d\n",
                waitedTime, waitTime, waitedTime - waitTime);
    }

    NSK_DISPLAY1("Thread CPU time between the events: %s ns\n",
        jlong_to_string(waitedThreadCpuTime - waitThreadCpuTime, buffer));
    if (!(NSK_VERIFY((waitedThreadCpuTime - waitThreadCpuTime)
            < (EXPECTED_ACCURACY * 1000000)))) {
        nsk_jvmti_setFailStatus();
        printf("waitedThreadCpuTime: %" LL "d, waitThreadCpuTime: %" LL "d, waitedThreadCpuTime - waitThreadCpuTime: %" LL "d\n",
                waitedThreadCpuTime, waitThreadCpuTime, waitedThreadCpuTime - waitThreadCpuTime);
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
JNIEXPORT jint JNICALL Agent_OnLoad_tc05t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_tc05t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_tc05t001(JavaVM *jvm, char *options, void *reserved) {
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
    caps.can_get_thread_cpu_time = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.MonitorWait = &MonitorWait;
    callbacks.MonitorWaited = &MonitorWaited;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

}
