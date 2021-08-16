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
 * - from Agent_OnAttach function start auxiliary thread waiting on 'gcFinishMonitor'
 * - receive GarbageCollectionStart event
 * - receive GarbageCollectionFinish event, notify 'gcFinishMonitor'
 * - notified auxiliary thread calls function 'nsk_aod_agentFinished' and agent completes work
 * (such schema is used because of agent can't call nsk_aod_agentFinished from GarbageCollectionFinish handler,
 * nsk_aod_agentFinished calls JNI functions and it is prohibited in GarbageCollectionFinish handler)
 *
 */

static Options* options = NULL;
static const char* agentName;

static jvmtiEvent testEvents[] = { JVMTI_EVENT_GARBAGE_COLLECTION_START, JVMTI_EVENT_GARBAGE_COLLECTION_FINISH };
static const int testEventsNumber = 2;

static jrawMonitorID gcFinishMonitor;

static volatile int gcStartEventReceived = 0;

static volatile int gcFinishEventReceived = 0;

static volatile int success = 1;

void JNICALL garbageCollectionStartHandler(jvmtiEnv *jvmti) {
    NSK_DISPLAY1("%s: GC start event received\n", agentName);

    gcStartEventReceived = 1;
}

void JNICALL garbageCollectionFinishHandler(jvmtiEnv *jvmti) {
    int auxiliaryThreadNotified = 0;

    NSK_DISPLAY1("%s: GC finish event received\n", agentName);

    if (!gcStartEventReceived) {
        NSK_COMPLAIN1("%s: GC start event wasn't received before GC finish event\n", agentName);
        success = 0;
    }

    if (NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(gcFinishMonitor))) {

        gcFinishEventReceived = 1;

        if (NSK_JVMTI_VERIFY(jvmti->RawMonitorNotify(gcFinishMonitor))) {
            auxiliaryThreadNotified = 1;
        }

        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(gcFinishMonitor))) {
            success = 0;
        }
    } else {
        success = 0;
    }

    if (!auxiliaryThreadNotified) {
        NSK_COMPLAIN1("%s: Error happenned during auxiliary thread notification, test may hang\n", agentName);
    }
}

void JNICALL auxiliaryThreadFunction(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    NSK_DISPLAY1("%s: auxiliary thread is running\n", agentName);

    if (NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(gcFinishMonitor))) {

        if (!gcFinishEventReceived) {
            if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorWait(gcFinishMonitor, 0))) {
                success = 0;
            }
        }

        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(gcFinishMonitor))) {
            success = 0;
        }
    } else {
        success = 0;
    }

    nsk_jvmti_aod_disableEventsAndFinish(agentName, testEvents, testEventsNumber, success, jvmti, jni);
}

int startAuxiliaryThread(jvmtiEnv* jvmti, JNIEnv* jni) {
    jthread thread;

    thread = nsk_jvmti_aod_createThread(jni);
    if (!NSK_VERIFY(thread != NULL))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(jvmti->RunAgentThread(thread, auxiliaryThreadFunction, NULL, JVMTI_THREAD_NORM_PRIORITY))) {
        return NSK_FALSE;
    }

    NSK_DISPLAY1("%s: auxiliary thread was started\n", agentName);

    return NSK_TRUE;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNI_OnLoad_attach020Agent00(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif

JNIEXPORT jint JNICALL
#ifdef STATIC_BUILD
Agent_OnAttach_attach020Agent00(JavaVM *vm, char *optionsString, void *reserved)
#else
Agent_OnAttach(JavaVM *vm, char *optionsString, void *reserved)
#endif
{
    jvmtiEnv* jvmti;
    jvmtiEventCallbacks eventCallbacks;
    jvmtiCapabilities caps;
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

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("GCFinishMonitor", &gcFinishMonitor))) {
        return JNI_ERR;
    }

    if (!NSK_VERIFY(startAuxiliaryThread(jvmti, jni)))
        return JNI_ERR;

    memset(&caps, 0, sizeof(caps));
    caps.can_generate_garbage_collection_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
        return JNI_ERR;
    }

    memset(&eventCallbacks,0, sizeof(eventCallbacks));
    eventCallbacks.GarbageCollectionStart  = garbageCollectionStartHandler;
    eventCallbacks.GarbageCollectionFinish = garbageCollectionFinishHandler;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks)))) {
        return JNI_ERR;
    }

    if (!(nsk_jvmti_aod_enableEvents(jvmti, testEvents, testEventsNumber))) {
        return JNI_ERR;
    }

    NSK_DISPLAY1("%s: initialization was done\n", agentName);

    if (!NSK_VERIFY(nsk_aod_agentLoaded(jni, agentName)))
        return JNI_ERR;

    return JNI_OK;
}
}
