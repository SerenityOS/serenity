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

extern "C" {

typedef struct {
    jthread thread;
    char* name;
    int dfn;
} threadDesc;

/* ========================================================================== */

/* scaffold objects */
static jlong timeout = 0;

/* test objects */
static threadDesc *threadList = NULL;
static jint threads_count = 0;
static int numberOfDeadlocks = 0;

/* ========================================================================== */

static int printDeadlock(jvmtiEnv* jvmti, JNIEnv* jni, int dThread) {
    jobject monitor = NULL;
    jclass klass = NULL;
    jvmtiMonitorUsage usageInfo;
    int pThread, cThread;
    char* name;

    NSK_DISPLAY1("Found deadlock #%d:\n", numberOfDeadlocks);
    for (pThread = dThread;;pThread = cThread) {
        NSK_DISPLAY1(" \"%s\":\n", threadList[pThread].name);
        if (!NSK_JVMTI_VERIFY(
                jvmti->GetCurrentContendedMonitor(threadList[pThread].thread, &monitor)))
            return NSK_FALSE;
        if (monitor != NULL) {
            if (!NSK_JNI_VERIFY(jni, (klass = jni->GetObjectClass(monitor)) != NULL))
                return NSK_FALSE;
            if (!NSK_JVMTI_VERIFY(jvmti->GetClassSignature(klass, &name, NULL)))
                return NSK_FALSE;
            NSK_DISPLAY2("    waiting to lock %p (%s),\n", monitor, name);
            jvmti->Deallocate((unsigned char*)name);
        } else {
            NSK_DISPLAY0(" (JVMTI raw monitor),\n");
        }
        if (!NSK_JVMTI_VERIFY(jvmti->GetObjectMonitorUsage(monitor, &usageInfo)))
            return NSK_FALSE;
        if (usageInfo.owner == NULL)
            break;
        for (cThread = 0; cThread < threads_count; cThread++) {
            if (jni->IsSameObject(threadList[cThread].thread, usageInfo.owner))
                break;
        }
        if (usageInfo.waiters != NULL) {
            jvmti->Deallocate((unsigned char*)usageInfo.waiters);
        }
        if (usageInfo.notify_waiters != NULL) {
            jvmti->Deallocate((unsigned char*)usageInfo.notify_waiters);
        }
        if (!NSK_VERIFY(cThread != threads_count))
            return NSK_FALSE;
        NSK_DISPLAY1("    which is held by \"%s\"\n",
            threadList[cThread].name);
        if (cThread == dThread)
            break;
    }

    return NSK_TRUE;
}

static int findDeadlockThreads(jvmtiEnv* jvmti, JNIEnv* jni) {
    jvmtiThreadInfo info;
    jthread *threads = NULL;
    jobject monitor = NULL;
    jvmtiMonitorUsage usageInfo;
    int tDfn = 0, gDfn = 0;
    int pThread, cThread;
    int i;

    NSK_DISPLAY0("Create threadList\n");

    /* get all live threads */
    if (!NSK_JVMTI_VERIFY(jvmti->GetAllThreads(&threads_count, &threads)))
        return NSK_FALSE;

    if (!NSK_VERIFY(threads_count > 0 && threads != NULL))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(
            jvmti->Allocate(threads_count*sizeof(threadDesc), (unsigned char**)&threadList)))
        return NSK_FALSE;

    for (i = 0; i < threads_count; i++) {
        if (!NSK_VERIFY(threads[i] != NULL))
            return NSK_FALSE;

        /* get thread information */
        if (!NSK_JVMTI_VERIFY(jvmti->GetThreadInfo(threads[i], &info)))
            return NSK_FALSE;

        NSK_DISPLAY3("    thread #%d (%s): %p\n", i, info.name, threads[i]);

        threadList[i].thread = threads[i];
        threadList[i].dfn = -1;
        threadList[i].name = info.name;
    }

    /* deallocate thread list */
    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)threads)))
        return NSK_FALSE;

    for (i = 0; i < threads_count; i++) {
        if (threadList[i].dfn < 0) {
            tDfn = gDfn;
            threadList[i].dfn = gDfn++;
            for (pThread = i;;pThread = cThread) {
                if (!NSK_JVMTI_VERIFY(
                        jvmti->GetCurrentContendedMonitor(threadList[pThread].thread, &monitor)))
                    return NSK_FALSE;
                if (monitor == NULL)
                    break;
                if (!NSK_JVMTI_VERIFY(jvmti->GetObjectMonitorUsage(monitor, &usageInfo)))
                    return NSK_FALSE;
                if (usageInfo.owner == NULL)
                    break;
                for (cThread = 0; cThread < threads_count; cThread++) {
                    if (jni->IsSameObject(threadList[cThread].thread, usageInfo.owner))
                        break;
                }
                if (usageInfo.waiters != NULL) {
                    jvmti->Deallocate((unsigned char*)usageInfo.waiters);
                }
                if (usageInfo.notify_waiters != NULL) {
                    jvmti->Deallocate((unsigned char*)usageInfo.notify_waiters);
                }
                if (!NSK_VERIFY(cThread != threads_count))
                    return NSK_FALSE;
                if (threadList[cThread].dfn < 0) {
                    threadList[cThread].dfn = gDfn++;
                } else if (cThread == pThread) {
                    break;
                } else {
                    numberOfDeadlocks++;
                    if (nsk_getVerboseMode()) {
                        if (!printDeadlock(jvmti, jni, cThread))
                            return NSK_FALSE;
                    }
                    break;
                }
            }
        }
    }

    /* deallocate thread names */
    for (i = 0; i < threads_count; i++) {
        if (threadList[i].name != NULL) {
            if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)threadList[i].name)))
                return NSK_FALSE;
        }
    }

    return NSK_TRUE;
}

/* ========================================================================== */

/* agent algorithm */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    /* wait for initial sync */
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!findDeadlockThreads(jvmti, jni)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    NSK_DISPLAY1("Total deadlocks found: %d\n", numberOfDeadlocks);
    if (!NSK_VERIFY(numberOfDeadlocks > 0))
        nsk_jvmti_setFailStatus();

    /* resume debugee after last sync */
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/* agent library initialization */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_tc03t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_tc03t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_tc03t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;
    jvmtiCapabilities caps;

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
    caps.can_get_current_contended_monitor = 1;
    caps.can_get_monitor_info = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

}
