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

#include <stdio.h>
#include <string.h>
#include <jvmti.h>
#include "agent_common.h"

#include "nsk_tools.h"
#include "jni_tools.h"
#include "JVMTITools.h"
#include "jvmti_tools.h"

extern "C" {

#define OBJ_MAX_COUNT 100000

static JNIEnv *jni = NULL;
static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;

static jlong timeout = 0;

static const char* DEBUGEE_SIGNATURE = "Lnsk/jvmti/scenarios/allocation/AP04/ap04t001;";
static const char* ROOT_SIGNATURE    = "[Lnsk/jvmti/scenarios/allocation/AP04/ap04t001;";

static const int eventsCount = 3;
static jvmtiEvent events[] = {
    JVMTI_EVENT_OBJECT_FREE,
    JVMTI_EVENT_GARBAGE_COLLECTION_START,
    JVMTI_EVENT_GARBAGE_COLLECTION_FINISH
};

static jclass debugeeClass = NULL;
static jfieldID rootFieldID = NULL;
static jmethodID unlockSecondaryID = NULL;

static volatile int eventCount = 0;
static volatile int iterationCount = 0;
static volatile int errorCount = 0;

/***********************************************************************/

static jrawMonitorID counterMonitor_ptr = NULL;

static void increaseCounter(volatile int* counterPtr) {

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(counterMonitor_ptr))) {
        nsk_jvmti_setFailStatus();
    }

    (*counterPtr)++;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(counterMonitor_ptr))) {
        nsk_jvmti_setFailStatus();
    }
}

static void setCounter(volatile int* counterPtr, int value) {

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(counterMonitor_ptr))) {
        nsk_jvmti_setFailStatus();
    }

    *counterPtr = value;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(counterMonitor_ptr))) {
        nsk_jvmti_setFailStatus();
    }
}

static int getCounter(volatile int* counterPtr) {
    int result;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(counterMonitor_ptr))) {
        nsk_jvmti_setFailStatus();
    }

    result = *counterPtr;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(counterMonitor_ptr))) {
        nsk_jvmti_setFailStatus();
    }

    return result;
}

/***********************************************************************/

jvmtiIterationControl JNICALL
heapObjectCallback(jlong  class_tag,
                   jlong  size,
                   jlong* tag_ptr,
                   void*  user_data) {

    int count = 0;

    /* clean eventCount on first iteration */
    if (getCounter(&iterationCount) == 0) {
        setCounter(&eventCount, 0);
    }
    increaseCounter(&iterationCount);

    /* check if eventCount is 0 for each iteration */
    count = getCounter(&eventCount);
    if (count > 0) {
        increaseCounter(&errorCount);
    }

    return JVMTI_ITERATION_CONTINUE;
}

/* jvmtiHeapRootCallback */
jvmtiIterationControl JNICALL
heapRootCallback(jvmtiHeapRootKind root_kind,
                 jlong class_tag,
                 jlong size,
                 jlong* tag_ptr,
                 void* user_data) {

    int count = 0;

    /* clean eventCount on first iteration */
    if (getCounter(&iterationCount) == 0) {
        setCounter(&eventCount, 0);
    }
    increaseCounter(&iterationCount);

    /* check if eventCount is 0 for each iteration */
    count = getCounter(&eventCount);
    if (count > 0) {
        increaseCounter(&errorCount);
    }

    return JVMTI_ITERATION_CONTINUE;
}

/* jvmtiStackReferenceCallback */
jvmtiIterationControl JNICALL
stackReferenceCallback(jvmtiHeapRootKind root_kind,
                       jlong     class_tag,
                       jlong     size,
                       jlong*    tag_ptr,
                       jlong     thread_tag,
                       jint      depth,
                       jmethodID method,
                       jint      slot,
                       void*     user_data) {

    int count = 0;

    /* clean eventCount on first iteration */
    if (getCounter(&iterationCount) == 0) {
        setCounter(&eventCount, 0);
    }
    increaseCounter(&iterationCount);

    /* check if eventCount is 0 for each iteration */
    count = getCounter(&eventCount);
    if (count > 0) {
        increaseCounter(&errorCount);
    }

    return JVMTI_ITERATION_CONTINUE;
}


/* jvmtiObjectReferenceCallback */
jvmtiIterationControl JNICALL
objectReferenceCallback(jvmtiObjectReferenceKind reference_kind,
                        jlong  class_tag,
                        jlong  size,
                        jlong* tag_ptr,
                        jlong  referrer_tag,
                        jint   referrer_index,
                        void*  user_data) {

    int count = 0;

    /* clean eventCount on first iteration */
    if (getCounter(&iterationCount) == 0) {
        setCounter(&eventCount, 0);
    }
    increaseCounter(&iterationCount);

    /* check if eventCount is 0 for each iteration */
    count = getCounter(&eventCount);
    if (count > 0) {
        increaseCounter(&errorCount);
    }

    return JVMTI_ITERATION_CONTINUE;
}


/***********************************************************************/

void JNICALL
ObjectFree(jvmtiEnv *jvmti_env, jlong tag) {
    increaseCounter(&eventCount);
//    NSK_DISPLAY0("  event: ObjectFree\n");
}

void JNICALL
GarbageCollectionStart(jvmtiEnv *jvmti_env) {
    increaseCounter(&eventCount);
    NSK_DISPLAY0("  event: GarbageCollectionStart\n");
}

void JNICALL
GarbageCollectionFinish(jvmtiEnv *jvmti_env) {
    increaseCounter(&eventCount);
    NSK_DISPLAY0("  event: GarbageCollectionFinish\n");
}

/***********************************************************************/

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP04_ap04t001_setTag(JNIEnv* jni,
                                                         jclass  klass,
                                                         jobject target, /* object to be tagged */
                                                         jlong   tag) {

    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(target, tag))) {
        nsk_jvmti_setFailStatus();
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP04_ap04t001_forceGC(JNIEnv* jni,
                                                          jclass  klass) {

NSK_DISPLAY0("  run: ForceGarbageCollection\n");
    if (!NSK_JVMTI_VERIFY(jvmti->ForceGarbageCollection())) {
        nsk_jvmti_setFailStatus();
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP04_ap04t001_runIterateOverHeap(JNIEnv* jni,
                                                                     jclass  klass) {
    int count = 0;

    setCounter(&errorCount, 0);
    setCounter(&eventCount, 0);
    setCounter(&iterationCount, 0);

    NSK_DISPLAY0("Calling IterateOverHeap...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverHeap(JVMTI_HEAP_OBJECT_TAGGED,
                                                 heapObjectCallback,
                                                 NULL /*user_data*/))) {
        nsk_jvmti_setFailStatus();
    }
    NSK_DISPLAY0("IterateOverHeap finished.\n");

    NSK_DISPLAY1("Iterations count: %d\n", getCounter(&iterationCount));
    NSK_DISPLAY1("Events count: %d\n", getCounter(&eventCount));

    count = getCounter(&errorCount);
    NSK_DISPLAY1("Errors detected: %d\n", count);
    if (count > 0) {
        NSK_COMPLAIN1("Events detected during heap iteration: %d\n", count);
        nsk_jvmti_setFailStatus();
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP04_ap04t001_runIterateOverReachableObjects(JNIEnv* jni,
                                                                                 jclass  klass) {
    int count = 0;

    setCounter(&errorCount, 0);
    setCounter(&eventCount, 0);
    setCounter(&iterationCount, 0);

    NSK_DISPLAY0("Calling IterateOverReachableObjects...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverReachableObjects(heapRootCallback,
                                                             stackReferenceCallback,
                                                             objectReferenceCallback,
                                                             NULL /*user_data*/))) {
        nsk_jvmti_setFailStatus();
    }
    NSK_DISPLAY0("IterateOverReachableObjects finished.\n");

    NSK_DISPLAY1("Iterations count: %d\n", getCounter(&iterationCount));
    NSK_DISPLAY1("Events count: %d\n", getCounter(&eventCount));

    count = getCounter(&errorCount);
    NSK_DISPLAY1("Errors detected: %d\n", count);
    if (count > 0) {
        NSK_COMPLAIN1("Events detected during heap iteration: %d\n", count);
        nsk_jvmti_setFailStatus();
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP04_ap04t001_runIterateOverInstancesOfClass(JNIEnv* jni,
                                                                                 jclass  klass) {
    int count = 0;

    setCounter(&errorCount, 0);
    setCounter(&eventCount, 0);
    setCounter(&iterationCount, 0);

    NSK_DISPLAY0("Calling IterateOverInstancesOfClass...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverInstancesOfClass(debugeeClass,
                                                             JVMTI_HEAP_OBJECT_TAGGED,
                                                             heapObjectCallback,
                                                             NULL /*user_data*/))) {
        nsk_jvmti_setFailStatus();
    }
    NSK_DISPLAY0("IterateOverInstancesOfClass finished.\n");

    NSK_DISPLAY1("Iterations count: %d\n", getCounter(&iterationCount));
    NSK_DISPLAY1("Events count: %d\n", getCounter(&eventCount));

    count = getCounter(&errorCount);
    NSK_DISPLAY1("Errors detected: %d\n", count);
    if (count > 0) {
        NSK_COMPLAIN1("Events detected during heap iteration: %d\n", count);
        nsk_jvmti_setFailStatus();
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP04_ap04t001_runIterateOverObjectsReachableFromObject(JNIEnv* jni,
                                                                                           jclass  klass) {

    jobject root = NULL;
    int count = 0;

    if (!NSK_JNI_VERIFY(jni, (root =
            jni->GetStaticObjectField(debugeeClass, rootFieldID)) != NULL)) {
        NSK_COMPLAIN0("GetStaticObjectField returned NULL for 'root' field value\n\n");
        nsk_jvmti_setFailStatus();
        return;
    }

    // release secondary lock
    jni->CallStaticVoidMethod(debugeeClass, unlockSecondaryID);

    setCounter(&errorCount, 0);
    setCounter(&eventCount, 0);
    setCounter(&iterationCount, 0);

    NSK_DISPLAY0("Calling IterateOverObjectsReachableFromObject...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverObjectsReachableFromObject(root,
                                                                       objectReferenceCallback,
                                                                       NULL /*user_data*/))) {
        nsk_jvmti_setFailStatus();
    }
    NSK_DISPLAY0("IterateOverObjectsReachableFromObject finished.\n");

    NSK_DISPLAY1("Iterations count: %d\n", getCounter(&iterationCount));
    NSK_DISPLAY1("Events count: %d\n", getCounter(&eventCount));

    count = getCounter(&errorCount);
    NSK_DISPLAY1("Errors detected: %d\n", count);
    if (count > 0) {
        NSK_COMPLAIN1("Events detected during heap iteration: %d\n", count);
        nsk_jvmti_setFailStatus();
    }
}

static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    NSK_DISPLAY0("Wait for debugee start\n\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    NSK_DISPLAY1("Find debugee class: %s\n", DEBUGEE_SIGNATURE);
    debugeeClass = nsk_jvmti_classBySignature(DEBUGEE_SIGNATURE);
    if (debugeeClass == NULL) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (!NSK_JNI_VERIFY(jni, (debugeeClass = (jclass)jni->NewGlobalRef(debugeeClass)) != NULL))
        return;

    NSK_DISPLAY1("Find ID of 'root' field: %s\n", ROOT_SIGNATURE);
    if (!NSK_JNI_VERIFY(jni, (rootFieldID =
            jni->GetStaticFieldID(debugeeClass, "root", ROOT_SIGNATURE)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    NSK_DISPLAY1("Find ID of 'unlockSecondary' method: %s\n", ROOT_SIGNATURE);
    if (!NSK_JNI_VERIFY(jni, (unlockSecondaryID =
            jni->GetStaticMethodID(debugeeClass, "unlockSecondary", "()V")) != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }


    NSK_DISPLAY0("Let debugee to run test cases\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;

    NSK_DISPLAY0("Wait for completion of test cases\n\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    nsk_jvmti_enableEvents(JVMTI_DISABLE, eventsCount, events, NULL);

    NSK_TRACE(jni->DeleteGlobalRef(debugeeClass));
    NSK_TRACE(jvmti->DestroyRawMonitor(counterMonitor_ptr));

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ap04t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ap04t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ap04t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("counterMonitor", &counterMonitor_ptr))) {
        return JNI_ERR;
    }

    memset(&caps, 0, sizeof(jvmtiCapabilities));
    caps.can_tag_objects = 1;
    caps.can_generate_object_free_events = 1;
    caps.can_generate_garbage_collection_events = 1;

    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    if (!caps.can_tag_objects)
        NSK_DISPLAY0("Warning: tagging objects is not available\n");
    if (!caps.can_generate_object_free_events)
        NSK_DISPLAY0("Warning: generation of object free events is not available\n");
    if (!caps.can_generate_garbage_collection_events)
        NSK_DISPLAY0("Warning: generation of garbage collection events is not available\n");

    /* set event callback */
    NSK_DISPLAY0("setting event callbacks ...\n");
    (void) memset(&callbacks, 0, sizeof(callbacks));

    callbacks.ObjectFree = &ObjectFree;
    callbacks.GarbageCollectionStart = &GarbageCollectionStart;
    callbacks.GarbageCollectionFinish = &GarbageCollectionFinish;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    NSK_DISPLAY0("setting event callbacks done.\n");

    NSK_DISPLAY0("enabling JVMTI events ...\n");
    if (!nsk_jvmti_enableEvents(JVMTI_ENABLE, eventsCount, events, NULL)) {
        return JNI_ERR;
    }
    NSK_DISPLAY0("enabling the events done.\n");

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;
    NSK_DISPLAY0("agentProc has been set\n\n");

    return JNI_OK;
}

}
