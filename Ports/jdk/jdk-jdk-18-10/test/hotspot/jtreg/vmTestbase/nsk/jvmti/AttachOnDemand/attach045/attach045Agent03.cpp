/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * Agent receives expected number of VMObjectAlloc events and finishes work
 * (events should be provoked by target application)
 */

#define EXPECTED_EVENTS_NUMBER 500

static Options* options = NULL;
static const char* agentName;

static jvmtiEvent testEvents[] = { JVMTI_EVENT_VM_OBJECT_ALLOC };
static const int testEventsNumber = 1;

static jrawMonitorID eventsCounterMonitor;

static volatile int eventsCounter = 0;

void JNICALL
VMObjectAllocHandler(jvmtiEnv *jvmti,
        JNIEnv* jni,
        jthread thread,
        jobject object,
        jclass object_klass,
        jlong size) {
    char threadName[MAX_STRING_LENGTH];
    char className[MAX_STRING_LENGTH];
    int success = 1;

    if (!nsk_jvmti_aod_getClassName(jvmti, object_klass, className)) {
        nsk_jvmti_aod_disableEventsAndFinish(agentName, testEvents, testEventsNumber, 0, jvmti, jni);
        return;
    }

    if (!nsk_jvmti_aod_getThreadName(jvmti, thread, threadName)) {
        nsk_jvmti_aod_disableEventsAndFinish(agentName, testEvents, testEventsNumber, 0, jvmti, jni);
        return;
    }

    if (NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(eventsCounterMonitor))) {

        eventsCounter++;

        NSK_DISPLAY4("%s: VMObjectAlloc received in thread '%s' for instance of '%s' (eventsCounter: %d)\n",
                agentName, threadName, className, eventsCounter);

        if ((eventsCounter % 10) == 0) {
            NSK_DISPLAY1("%s: force garbage collection\n", agentName);

            if (!NSK_JVMTI_VERIFY(jvmti->ForceGarbageCollection()))
                success = 0;
        }

        if (eventsCounter == EXPECTED_EVENTS_NUMBER) {
            NSK_DISPLAY2("%s: all expected events were received (eventsCounter: %d)\n", agentName, eventsCounter);

            nsk_jvmti_aod_disableEventsAndFinish(agentName, testEvents, testEventsNumber, success, jvmti, jni);
        }

        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(eventsCounterMonitor))) {
            success = 0;
        }
    } else {
        success = 0;
    }

    if (!success) {
        nsk_jvmti_aod_disableEventsAndFinish(agentName, testEvents, testEventsNumber, 0, jvmti, jni);
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNI_OnLoad_attach045Agent03(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif

JNIEXPORT jint JNICALL
#ifdef STATIC_BUILD
Agent_OnAttach_attach045Agent03(JavaVM *vm, char *optionsString, void *reserved)
#else
Agent_OnAttach(JavaVM *vm, char *optionsString, void *reserved)
#endif
{
    jvmtiEventCallbacks eventCallbacks;
    jvmtiEnv* jvmti;
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

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("attach045-agent03-eventsCounterMonitor", &eventsCounterMonitor))) {
        return JNI_ERR;
    }

    memset(&caps, 0, sizeof(caps));
    caps.can_generate_vm_object_alloc_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
        return JNI_ERR;
    }

    memset(&eventCallbacks,0, sizeof(eventCallbacks));
    eventCallbacks.VMObjectAlloc = VMObjectAllocHandler;
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
