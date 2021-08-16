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
static jthread thread = NULL;
static jobject object_M = NULL;
/* line numbers of "synchronized (M)" clauses in java part of the test */
static jint lines[] = { 48, 53, 58 };
static volatile int enterEventsCount = 0;
static volatile int enteredEventsCount = 0;

/* ========================================================================== */

static jint findLineNumber(jvmtiEnv *jvmti, jthread thread) {
    jmethodID method = NULL;
    jlocation location;
    jvmtiLineNumberEntry* table = NULL;
    jint count = 0;
    jint line = 0;
    int i;

    if (!NSK_JVMTI_VERIFY(jvmti->GetFrameLocation(thread, 0, &method, &location)))
        return 0;

    if (!NSK_VERIFY(method != NULL))
        return 0;

    if (!NSK_VERIFY(location != -1))
        return 0;

    if (!NSK_JVMTI_VERIFY(jvmti->GetLineNumberTable(method, &count, &table)))
        return 0;

    if (!NSK_VERIFY(table != NULL))
        return 0;

    if (!NSK_VERIFY(count > 0))
        return 0;

    for (i = 0; i < count; i++) {
        if (location < table[i].start_location) {
            break;
        }
    }

    line = table[i-1].line_number;

    if (table != NULL) {
        if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)table)))
            return 0;
    }

    return line;
}

/* ========================================================================== */

void JNICALL
MonitorContendedEnter(jvmtiEnv *jvmti, JNIEnv* jni, jthread thr, jobject obj) {
    jint line = 0;

    if (!NSK_VERIFY(thr != NULL)) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN1("MonitorContendedEnter event: thread=%p\n", thr);
        return;
    }

    if (!NSK_VERIFY(obj != NULL)) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN1("MonitorContendedEnter event: object=%p\n", obj);
        return;
    }

    /* check if event is for tested thread and object */
    if (jni->IsSameObject(thread, thr) &&
            jni->IsSameObject(object_M, obj)) {

        line = findLineNumber(jvmti, thread);
        if (!line) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN2("MonitorContendedEnter event: thread=%p, object=%p\n",
                thr, obj);
            return;
        }

        NSK_DISPLAY3("MonitorContendedEnter event: thread=%p, object=%p, line=%d\n",
            thr, obj, line);

        /* workaround of 4527285 bug: in -Xint mode GetFrameLocation
           returns the location after the monitor enter.
         */
        if (!NSK_VERIFY(line == lines[enterEventsCount] ||
                line == (lines[enterEventsCount] + 1))) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN3("MonitorContendedEnter event: thread=%p, object=%p, line=%d\n",
                thr, obj, line);
        }

        enterEventsCount++;
    }
}

void JNICALL
MonitorContendedEntered(jvmtiEnv *jvmti, JNIEnv* jni, jthread thr, jobject obj) {
    jint line = 0;

    if (!NSK_VERIFY(thr != NULL)) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN1("MonitorContendedEntered event: thread=%p\n", thr);
        return;
    }

    if (!NSK_VERIFY(obj != NULL)) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN1("MonitorContendedEntered event: object=%p\n", obj);
        return;
    }

    /* check if event is for tested thread and object */
    if (jni->IsSameObject(thread, thr) &&
            jni->IsSameObject(object_M, obj)) {

        line = findLineNumber(jvmti, thread);
        if (!line) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN2("MonitorContendedEntered event: thread=%p, object=%p\n",
                thr, obj);
            return;
        }

        NSK_DISPLAY3("MonitorContendedEntered event: thread=%p, object=%p, line=%d\n",
            thr, obj, line);

        /* workaround of 4527285 bug: in -Xint mode GetFrameLocation
           returns the location after the monitor enter.
         */
        if (!NSK_VERIFY(line == lines[enteredEventsCount] ||
                line == (lines[enteredEventsCount] + 1))) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN3("MonitorContendedEntered event: thread=%p, object=%p, line=%d\n",
                thr, obj, line);
        }

        enteredEventsCount++;
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

    /* enable MonitorContendedEntered event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(
                JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTER, NULL)))
        return NSK_FALSE;

    /* enable MonitorContendedEntered event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(
                JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, NULL)))
        return NSK_FALSE;

    return NSK_TRUE;
}

static int clean(jvmtiEnv* jvmti, JNIEnv* jni) {

    /* disable MonitorContendedEntered event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(
                JVMTI_DISABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, NULL)))
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

    /* resume debugee to catch MonitorContendedEntered events */
    if (!(NSK_VERIFY(nsk_jvmti_resumeSync()) &&
          NSK_VERIFY(nsk_jvmti_waitForSync(timeout))))
        return;

    NSK_DISPLAY1("Number of MonitorContendedEnter events: %d\n",
        enterEventsCount);

    if (!(NSK_VERIFY(enterEventsCount == 3))) {
        nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY1("Number of MonitorContendedEntered events: %d\n",
        enteredEventsCount);

    if (!(NSK_VERIFY(enteredEventsCount == 3))) {
        nsk_jvmti_setFailStatus();
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
JNIEXPORT jint JNICALL Agent_OnLoad_tc02t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_tc02t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_tc02t001(JavaVM *jvm, char *options, void *reserved) {
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
    caps.can_get_line_numbers = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    memset(&callbacks, 0, sizeof(callbacks));
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
Java_nsk_jvmti_scenarios_contention_TC02_tc02t001_enterEventsCount(JNIEnv* jni, jclass klass) {
    return enterEventsCount;
}

/* ========================================================================== */

}
