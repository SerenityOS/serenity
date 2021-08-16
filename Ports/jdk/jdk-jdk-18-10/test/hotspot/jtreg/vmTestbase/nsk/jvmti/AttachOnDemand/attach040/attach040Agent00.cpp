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
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include <jvmti.h>
#include <aod.h>
#include <jvmti_aod.h>

extern "C" {

/*
 * Expected agent work scenario:
 *  - during initialization agent enables ThreadStart events
 *  - target application starts several threads
 *  - agent receives ThreadStart events and tries to find thread provoked this event in the array returned
 *    by JVMTI function GetAllThreads
 *  - when expected number of ThreadStart events is received agent finishes work
 */

#define TEST_THREADS_NUMBER 5

#define TEST_THREAD_NAME_PREFIX "attach040-TestThread-"

static jrawMonitorID threadsCounterMonitor;

static volatile int testThreadsCounter = 0;

static Options* options = NULL;
static const char* agentName;

void JNICALL threadStartHandler(jvmtiEnv *jvmti,
        JNIEnv* jni,
        jthread thread) {
    int success = 1;
    jint threadsCount = 0;
    jthread * threads;
    jint i;
    char startedThreadName[MAX_STRING_LENGTH];

    if (!nsk_jvmti_aod_getThreadName(jvmti, thread, startedThreadName)) {
        nsk_jvmti_aod_disableEventAndFinish(agentName, JVMTI_EVENT_THREAD_START, success, jvmti, jni);
        return;
    }

    NSK_DISPLAY2("%s: ThreadStart event was received for thread '%s'\n", agentName, startedThreadName);

    if (NSK_JVMTI_VERIFY(jvmti->GetAllThreads(&threadsCount, &threads))) {
        int startedThreadWasFound = 0;

        for (i = 0; i < threadsCount; i++) {
            char threadName[MAX_STRING_LENGTH];

            if (!nsk_jvmti_aod_getThreadName(jvmti, thread, threadName)) {
                nsk_jvmti_aod_disableEventAndFinish(agentName, JVMTI_EVENT_THREAD_START, success, jvmti, jni);
                return;
            }

            if (!strcmp(threadName, startedThreadName)) {
                startedThreadWasFound = 1;
                break;
            }
        }

        if (!startedThreadWasFound) {
            NSK_COMPLAIN2("%s: GetAllThreads didn't return information about thread '%s'\n", agentName, startedThreadName);
            success = 0;
        }

        nsk_jvmti_aod_deallocate(jvmti, (unsigned char*)threads);
    } else {
        success = 0;
    }

    if (strstr(startedThreadName, TEST_THREAD_NAME_PREFIX)) {
        if (NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(threadsCounterMonitor))) {

            testThreadsCounter++;

            if (testThreadsCounter == TEST_THREADS_NUMBER) {
                nsk_jvmti_aod_disableEventAndFinish(agentName, JVMTI_EVENT_THREAD_START, success, jvmti, jni);
            }

            if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(threadsCounterMonitor))) {
                success = 0;
            }
        } else {
            success = 0;
        }
    }

    if (!success) {
        NSK_COMPLAIN1("%s: unexpected error during agent work, stop agent\n", agentName);
        nsk_jvmti_aod_disableEventAndFinish(agentName, JVMTI_EVENT_THREAD_START, 0, jvmti, jni);
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNI_OnLoad_attach040Agent00(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif

JNIEXPORT jint JNICALL
#ifdef STATIC_BUILD
Agent_OnAttach_attach040Agent00(JavaVM *vm, char *optionsString, void *reserved)
#else
Agent_OnAttach(JavaVM *vm, char *optionsString, void *reserved)
#endif
{
    jvmtiEventCallbacks eventCallbacks;
    jvmtiEnv* jvmti;
    JNIEnv* jni;

    options = (Options*) nsk_aod_createOptions(optionsString);
    if (!NSK_VERIFY(options != NULL))
        return JNI_ERR;

    agentName = nsk_aod_getOptionValue(options, NSK_AOD_AGENT_NAME_OPTION);

    jni = (JNIEnv*) nsk_aod_createJNIEnv(vm);
    if (jni == NULL)
        return JNI_ERR;

    jvmti = nsk_jvmti_createJVMTIEnv(vm, reserved);
    if (!NSK_VERIFY(jvmti != NULL))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("threadsCounterMonitor", &threadsCounterMonitor))) {
        return JNI_ERR;
    }

    memset(&eventCallbacks,0, sizeof(eventCallbacks));
    eventCallbacks.ThreadStart = threadStartHandler;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks)))) {
        return JNI_ERR;
    }

    if (!(nsk_jvmti_aod_enableEvent(jvmti, JVMTI_EVENT_THREAD_START))) {
        return JNI_ERR;
    }

    NSK_DISPLAY1("%s: initialization was done\n", agentName);

    if (!NSK_VERIFY(nsk_aod_agentLoaded(jni, agentName)))
        return JNI_ERR;

    return JNI_OK;
}

}
