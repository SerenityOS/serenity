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
 *  - agent receives ThreadStart event and tries to find thread provoked this event
 *    in all VM thread groups and finishes work
 */

#define STARTED_TEST_THREAD_NAME "attach041-TestThread"

static Options* options = NULL;
static const char* agentName;

int tryFindThread(jvmtiEnv *jvmti, jthreadGroup group, const char* threadNameToFind) {
    jint threadsCount = 0;
    jthread *threads = NULL;
    jint groupsCount = 0;
    jthreadGroup* groups = NULL;
    jvmtiThreadGroupInfo groupInfo;
    int i;
    char threadGroupName[MAX_STRING_LENGTH];

    if (!NSK_JVMTI_VERIFY(jvmti->GetThreadGroupInfo(group, &groupInfo))) {
        return 0;
    }

    strcpy(threadGroupName, groupInfo.name);
    nsk_jvmti_aod_deallocate(jvmti, (unsigned char*)groupInfo.name);

    NSK_DISPLAY3("%s: trying to find thread '%s' in group '%s'\n", agentName, threadNameToFind, threadGroupName);

    if (!NSK_JVMTI_VERIFY(jvmti->GetThreadGroupChildren(group, &threadsCount, &threads, &groupsCount, &groups))) {
        return 0;
    }

    for (i = 0; i < threadsCount; i++) {
        char threadName[MAX_STRING_LENGTH];
        if (!nsk_jvmti_aod_getThreadName(jvmti, threads[i], threadName)) {
            NSK_COMPLAIN1("%s: failed to get thread name\n", agentName);
            nsk_jvmti_aod_deallocate(jvmti, (unsigned char*)threads);
            return 0;
        }
        if (!strcmp(threadName, threadNameToFind)) {
            nsk_jvmti_aod_deallocate(jvmti, (unsigned char*)threads);
            nsk_jvmti_aod_deallocate(jvmti, (unsigned char*)groups);
            NSK_DISPLAY3("%s: thread '%s' was found in group '%s'\n", agentName, threadNameToFind, threadGroupName);
            return 1;
        }
    }

    // threads array isn't needed more
    nsk_jvmti_aod_deallocate(jvmti, (unsigned char*)threads);

    NSK_DISPLAY3("%s: thread '%s' wasn't found in group '%s'\n", agentName, threadNameToFind, threadGroupName);

    if (groupsCount != 0) {
        for (i = 0; i < groupsCount; i++) {
            if (tryFindThread(jvmti, groups[i], threadNameToFind)) {
                nsk_jvmti_aod_deallocate(jvmti, (unsigned char*)groups);
                return 1;
            }
        }
    }

    nsk_jvmti_aod_deallocate(jvmti, (unsigned char*)groups);

    return 0;
}

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
        int threadWasFound = 0;
        jint groupsCount = 0;
        jthreadGroup *topGroups;
        int i;

        if (!NSK_JVMTI_VERIFY(jvmti->GetTopThreadGroups(&groupsCount, &topGroups))) {
            NSK_COMPLAIN1("%s: failed to get top thread groups\n", agentName);
            nsk_jvmti_aod_disableEventAndFinish(agentName, JVMTI_EVENT_THREAD_START, 0, jvmti, jni);
            return;
        }

        for (i = 0; i < groupsCount; i++) {
            if (tryFindThread(jvmti, topGroups[i], startedThreadName)) {
                threadWasFound = 1;
                break;
            }
        }

        nsk_jvmti_aod_deallocate(jvmti, (unsigned char*)topGroups);

        if (!threadWasFound) {
            success = 0;
            NSK_COMPLAIN2("%s: failed to find thread '%s'\n", agentName, startedThreadName);
        }

        nsk_jvmti_aod_disableEventAndFinish(agentName, JVMTI_EVENT_THREAD_START, success, jvmti, jni);
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNI_OnLoad_attach041Agent00(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif

JNIEXPORT jint JNICALL
#ifdef STATIC_BUILD
Agent_OnAttach_attach041Agent00(JavaVM *vm, char *optionsString, void *reserved)
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
