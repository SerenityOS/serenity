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
 *  - target application starts thread
 *  - agent receives ThreadStart event for this thread and tries to call GetThreadState for
 *    all VM threads, then finishes work
 */

#define STARTED_TEST_THREAD_NAME "attach042-TestThread"

static Options* options = NULL;
static const char* agentName;

void JNICALL threadStartHandler(jvmtiEnv *jvmti,
        JNIEnv* jni,
        jthread thread) {
    char startedThreadName[MAX_STRING_LENGTH];

    if (!nsk_jvmti_aod_getThreadName(jvmti, thread, startedThreadName)) {
        nsk_jvmti_aod_disableEventAndFinish(agentName, JVMTI_EVENT_THREAD_START, 0, jvmti, jni);
        return;
    }

    NSK_DISPLAY2("%s: ThreadStart event was received for thread '%s'\n", agentName, startedThreadName);

    if (!strcmp(startedThreadName, STARTED_TEST_THREAD_NAME)) {
        int success = 1;
        jint threadsCount = 0;
        jthread* threads = NULL;
        int i;
        int startedThreadWasFound = 0;

        if (!NSK_JVMTI_VERIFY(jvmti->GetAllThreads(&threadsCount, &threads))) {
            NSK_COMPLAIN1("%s: failed to get all threads\n", agentName);
            nsk_jvmti_aod_disableEventAndFinish(agentName, JVMTI_EVENT_THREAD_START, 0, jvmti, jni);
            return;
        }

        NSK_DISPLAY1("%s: displaying threads status:\n", agentName);

        for (i = 0; i < threadsCount; i++) {
            jint threadState;
            char threadName[MAX_STRING_LENGTH];

            if (!nsk_jvmti_aod_getThreadName(jvmti, threads[i], threadName)) {
                NSK_COMPLAIN1("%s: failed to get thread name\n", agentName);
                nsk_jvmti_aod_deallocate(jvmti, (unsigned char*)threads);
                nsk_jvmti_aod_disableEventAndFinish(agentName, JVMTI_EVENT_THREAD_START, 0, jvmti, jni);
                return;
            }

            if (!strcmp(threadName, startedThreadName)) {
                startedThreadWasFound = 1;
            }

            if (!NSK_JVMTI_VERIFY(jvmti->GetThreadState(threads[i], &threadState))) {
                NSK_COMPLAIN2("%s: failed to get status of thread '%s'\n", agentName, threadName);
                nsk_jvmti_aod_deallocate(jvmti, (unsigned char*)threads);
                nsk_jvmti_aod_disableEventAndFinish(agentName, JVMTI_EVENT_THREAD_START, 0, jvmti, jni);
                return;
            }

            NSK_DISPLAY3("%s: status of '%s': %s\n", agentName, threadName, TranslateState(threadState));
        }

        nsk_jvmti_aod_deallocate(jvmti, (unsigned char*)threads);

        if (!startedThreadWasFound) {
            NSK_COMPLAIN2("%s: thread '%s' wasn't returned by GetAllThreads\n", agentName, startedThreadName);
            success = 0;
        }

        nsk_jvmti_aod_disableEventAndFinish(agentName, JVMTI_EVENT_THREAD_START, success, jvmti, jni);
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNI_OnLoad_attach042Agent00(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif

JNIEXPORT jint JNICALL
#ifdef STATIC_BUILD
Agent_OnAttach_attach042Agent00(JavaVM *vm, char *optionsString, void *reserved)
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
