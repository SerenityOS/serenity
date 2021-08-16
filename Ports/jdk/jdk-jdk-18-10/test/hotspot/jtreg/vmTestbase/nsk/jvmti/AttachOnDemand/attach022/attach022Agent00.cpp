/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "ExceptionCheckingJniEnv.hpp"

extern "C" {

#define OBJECTS_FOR_ALLOCATION_TEST_CLASS_NAME "Lnsk/jvmti/AttachOnDemand/attach022/ClassForAllocationEventsTest;"

static jvmtiEnv* jvmti;
static Options* options = NULL;
static const char* agentName;

static jvmtiEvent testEvents[] = { JVMTI_EVENT_OBJECT_FREE, JVMTI_EVENT_VM_OBJECT_ALLOC };
static const int testEventsNumber = 2;

static volatile int taggedObjectsCounter = 0;
static volatile int freedObjectsCounter = 0;

static jrawMonitorID objectTagMonitor;
static jrawMonitorID objectFreeMonitor;

volatile int success = 1;

volatile int agentFinished;

void shutdownAgent(JNIEnv* jni) {
    if (agentFinished)
        return;

    if (!nsk_jvmti_aod_disableEvents(jvmti, testEvents, testEventsNumber))
        success = 0;

    nsk_aod_agentFinished(jni, agentName, success);

    agentFinished = 1;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_AttachOnDemand_attach022_attach022Target_shutdownAgent(JNIEnv * jni,
        jclass klass, jint expectedTaggedObjectsCounter) {

    // Flush any pending ObjectFree events.
    if (!nsk_jvmti_aod_disableEvents(jvmti, testEvents, testEventsNumber))
        success = 0;

    if (taggedObjectsCounter != expectedTaggedObjectsCounter) {
        success = 0;
        NSK_COMPLAIN2("ERROR: unexpected taggedObjectsCounter: %d (expected value is %d)\n", taggedObjectsCounter, expectedTaggedObjectsCounter);
    }

    if (taggedObjectsCounter != freedObjectsCounter) {
        success = 0;
        NSK_COMPLAIN2("ERROR: taggedObjectsCounter != freedObjectsCounter (taggedObjectsCounter: %d, freedObjectsCounter: %d)\n",
                taggedObjectsCounter, freedObjectsCounter);
    }

    shutdownAgent(jni);

    return success ? JNI_TRUE : JNI_FALSE;
}

void JNICALL objectFreeHandler(jvmtiEnv *jvmti, jlong tag) {
    NSK_DISPLAY2("%s: ObjectFree event received (object tag: %ld)\n", agentName, tag);

    if (NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(objectFreeMonitor))) {
        freedObjectsCounter++;

        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(objectFreeMonitor))) {
            success = 0;
        }
    } else {
        success = 0;
    }
}

#define ATTACH022_TARGET_APP_CLASS_NAME "nsk/jvmti/AttachOnDemand/attach022/attach022Target"

void registerNativeMethods(JNIEnv* jni_env) {
    ExceptionCheckingJniEnvPtr ec_jni(jni_env);
    jclass appClass;
    JNINativeMethod nativeMethods[] = {
            { (char*)"shutdownAgent", (char*)"(I)Z",
              (void*) Java_nsk_jvmti_AttachOnDemand_attach022_attach022Target_shutdownAgent } };
    jint nativeMethodsNumber = 1;

    appClass = ec_jni->FindClass(ATTACH022_TARGET_APP_CLASS_NAME, TRACE_JNI_CALL);
    ec_jni->RegisterNatives(appClass, nativeMethods, nativeMethodsNumber, TRACE_JNI_CALL);
}

void JNICALL vmObjectAllocHandler(jvmtiEnv * jvmti,
        JNIEnv * jni,
        jthread thread,
        jobject object,
        jclass object_class,
        jlong size) {
    char className[MAX_STRING_LENGTH];

    if (!nsk_jvmti_aod_getClassName(jvmti, object_class, className)) {
        success = 0;
        shutdownAgent(jni);
        return;
    }

    NSK_DISPLAY2("%s: ObjectAlloc event received (object class: %s)\n", agentName, className);

    if (!strcmp(className, OBJECTS_FOR_ALLOCATION_TEST_CLASS_NAME)) {
        if (NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(objectTagMonitor))) {
            jlong tagValue = taggedObjectsCounter + 1;

            if (!NSK_JVMTI_VERIFY(jvmti->SetTag(object, tagValue))) {
                NSK_COMPLAIN1("%s: failed to set tag\n", agentName);
                success = 0;
            } else {
                NSK_DISPLAY2("%s: object was tagged (tag value: %ld)\n", agentName, tagValue);
                taggedObjectsCounter++;
            }

            if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(objectTagMonitor))) {
                success = 0;
            }
        } else {
            success = 0;
        }
    }

    if (!success) {
        NSK_COMPLAIN1("%s: error happened during agent work, stop agent\n", agentName);
        shutdownAgent(jni);
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNI_OnLoad_attach022Agent00(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif

JNIEXPORT jint JNICALL
#ifdef STATIC_BUILD
Agent_OnAttach_attach022Agent00(JavaVM *vm, char *optionsString, void *reserved)
#else
Agent_OnAttach(JavaVM *vm, char *optionsString, void *reserved)
#endif
{
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

    registerNativeMethods(jni);

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("ObjectTagMonitor", &objectTagMonitor))) {
        return JNI_ERR;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("ObjectFreeMonitor", &objectFreeMonitor))) {
        return JNI_ERR;
    }

    memset(&caps, 0, sizeof(caps));
    caps.can_tag_objects = 1;
    caps.can_generate_object_free_events = 1;
    caps.can_generate_vm_object_alloc_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
        return JNI_ERR;
    }

    memset(&eventCallbacks,0, sizeof(eventCallbacks));
    eventCallbacks.ObjectFree = objectFreeHandler;
    eventCallbacks.VMObjectAlloc = vmObjectAllocHandler;
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
