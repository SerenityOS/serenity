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

#include <stdio.h>
#include <string.h>
#include <jvmti.h>
#include "agent_common.h"
#include "ExceptionCheckingJniEnv.hpp"
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

static const char* DEBUGEE_SIGNATURE = "Lnsk/jvmti/scenarios/allocation/AP04/ap04t003;";
static const char* ROOT_SIGNATURE    = "[Lnsk/jvmti/scenarios/allocation/AP04/ap04t003;";

static jclass debugeeClass = NULL;
static jfieldID rootFieldID;

static jrawMonitorID startLock = NULL;
static jrawMonitorID runLock = NULL;
static jrawMonitorID endLock = NULL;

static volatile int iterationCount = 0;
static volatile int objectCount = 0;

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

void notifyThread() {

    /* enter and notify runLock */
    {
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(runLock))) {
            nsk_jvmti_setFailStatus();
        }
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorNotify(runLock))) {
            nsk_jvmti_setFailStatus();
        }
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(runLock))) {
            nsk_jvmti_setFailStatus();
        }
    }
}

jvmtiIterationControl JNICALL
heapObjectCallback(jlong  class_tag,
                   jlong  size,
                   jlong* tag_ptr,
                   void*  user_data) {

    if (getCounter(&iterationCount) == 0) {
        notifyThread();
    }
    increaseCounter(&iterationCount);

    if (*tag_ptr > 0) {
        increaseCounter(&objectCount);
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

    if (getCounter(&iterationCount) == 0) {
        notifyThread();
    }
    increaseCounter(&iterationCount);

    if (*tag_ptr > 0) {
        increaseCounter(&objectCount);
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

    if (getCounter(&iterationCount) == 0) {
        notifyThread();
    }
    increaseCounter(&iterationCount);

    if (*tag_ptr > 0) {
        increaseCounter(&objectCount);
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

    if (getCounter(&iterationCount) == 0) {
        notifyThread();
    }
    increaseCounter(&iterationCount);

    if (*tag_ptr > 0) {
        increaseCounter(&objectCount);
    }

    return JVMTI_ITERATION_CONTINUE;
}

/********* Agent thread modifying tags of objects ************/

/** Body of new agent thread: modify tags of tagged object. */
void JNICALL agent_start(jvmtiEnv* jvmti, JNIEnv* jni, void *p) {

    jint taggedObjectsCount = 0;
    jobject* taggedObjectsList = NULL;

    NSK_DISPLAY0("Agent thread: started.\n");

    /* obtain tagged objects list */
    {
        jlong tag = (jlong)1;

        if (!NSK_JVMTI_VERIFY(jvmti->GetObjectsWithTags(
                1, &tag, &taggedObjectsCount, &taggedObjectsList, NULL))) {
            nsk_jvmti_setFailStatus();
            return;
        }
    }

    NSK_DISPLAY1("Agent thread: got tagged objects: %d\n", (int)taggedObjectsCount);

    if (!NSK_VERIFY(taggedObjectsCount == OBJ_MAX_COUNT)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* enter runLock */
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(runLock))) {
        nsk_jvmti_setFailStatus();
    }

    /* enter and notify startLock */
    {
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(startLock))) {
            nsk_jvmti_setFailStatus();
        }
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorNotify(startLock))) {
            nsk_jvmti_setFailStatus();
        }
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(startLock))) {
            nsk_jvmti_setFailStatus();
        }
    }

    NSK_DISPLAY0("Agent thread: wait for run notification\n");

    /* wait on runLock */
    {
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorWait(runLock, timeout))) {
            nsk_jvmti_setFailStatus();
        }
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(runLock))) {
            nsk_jvmti_setFailStatus();
        }
    }

    NSK_DISPLAY0("Agent thread: modify tags of each even object.\n");

    /* modify tags of each even object */
    {
        int modified = 0;
        int i;
        for (i = 0; i < taggedObjectsCount; i+=2) {
            if (!NSK_JVMTI_VERIFY(jvmti->SetTag(taggedObjectsList[i], 0))) {
                nsk_jvmti_setFailStatus();
                continue;
            }
            modified++;
        }

        NSK_DISPLAY2("Agent thread: tags modified: %d of %d\n",
                                            modified, (int)taggedObjectsCount);
    }

    /* destroy objects list */
    {
        if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)taggedObjectsList))) {
            nsk_jvmti_setFailStatus();
        }
    }

    /* enter and notify endLock */
    {
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(endLock))) {
            nsk_jvmti_setFailStatus();
        }
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorNotify(endLock))) {
            nsk_jvmti_setFailStatus();
        }
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(endLock))) {
            nsk_jvmti_setFailStatus();
        }
    }

    NSK_DISPLAY0("Agent thread: finished.\n");
}

/***********************************************************************/

static int startThread(jthread threadObj) {
    int success = NSK_TRUE;

    /* enter startLock */
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(startLock))) {
        nsk_jvmti_setFailStatus();
    }

    /* start thread */
    if (!NSK_JVMTI_VERIFY(
            jvmti->RunAgentThread(threadObj, agent_start, NULL, JVMTI_THREAD_NORM_PRIORITY))) {
        success = NSK_FALSE;
        nsk_jvmti_setFailStatus();
    } else {
        /* wait on startLock */
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorWait(startLock, timeout))) {
            nsk_jvmti_setFailStatus();
        }
    }

    /* exit starLock */
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(startLock))) {
        nsk_jvmti_setFailStatus();
    }

    return success;
}

/** Create thread object for new agent thread. */
static jthread newThreadObj(JNIEnv* jni_env) {
    ExceptionCheckingJniEnvPtr ec_jni(jni_env);
    jclass thrClass;
    jmethodID cid;

    thrClass = ec_jni->FindClass("java/lang/Thread", TRACE_JNI_CALL);
    cid = ec_jni->GetMethodID(thrClass, "<init>", "()V", TRACE_JNI_CALL);
    return ec_jni->NewObject(thrClass, cid, TRACE_JNI_CALL);
}

/***********************************************************************/

/** Clean counters and start new agent thread with agent_start() body. */
static int prepareToIteration(JNIEnv* jni) {
    jthread threadObj = NULL;

    setCounter(&iterationCount, 0);
    setCounter(&objectCount, 0);

    threadObj = newThreadObj(jni);

    /* enter endLock */
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(endLock))) {
        nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY0("Starting new agent thread...\n");
    return startThread(threadObj);
}

/** Wait for new agent thread to complete. */
static void afterIteration() {

    /* notify new agent thread (in case if not yet notified) */
    notifyThread();

    NSK_DISPLAY0("Wait for new agent thread to complete\n");

    /* wait on endLock */
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorWait(endLock, timeout))) {
        nsk_jvmti_setFailStatus();
    }

    /* exit endLock */
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(endLock))) {
        nsk_jvmti_setFailStatus();
    }
}

/***********************************************************************/

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP04_ap04t003_setTag(JNIEnv* jni,
                                                         jclass  klass,
                                                         jobject target, /* object to be tagged */
                                                         jlong   tag) {

    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(target, tag))) {
        nsk_jvmti_setFailStatus();
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP04_ap04t003_runIterateOverHeap(JNIEnv* jni,
                                                                     jclass  klass) {
    int modified = 0;
    int found = 0;

    if (!prepareToIteration(jni))
        return;

    NSK_DISPLAY0("Calling IterateOverHeap...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverHeap(JVMTI_HEAP_OBJECT_TAGGED,
                                                 heapObjectCallback,
                                                 NULL /*user_data*/))) {
        nsk_jvmti_setFailStatus();
    }
    NSK_DISPLAY0("IterateOverHeap finished.\n");

    afterIteration();

    found = getCounter(&objectCount);
    NSK_DISPLAY1("Found tagged objects: %d\n", found);

    modified = OBJ_MAX_COUNT - found;
    if (modified > 0) {
        NSK_COMPLAIN2("Tags were modified by other thread during heap iteration: %d of %d\n",
                                                        modified, OBJ_MAX_COUNT);
        nsk_jvmti_setFailStatus();
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP04_ap04t003_runIterateOverReachableObjects(JNIEnv* jni,
                                                                                 jclass  klass) {
    int modified = 0;
    int found = 0;

    if (!prepareToIteration(jni))
        return;

    NSK_DISPLAY0("Calling IterateOverReachableObjects...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverReachableObjects(heapRootCallback,
                                                             stackReferenceCallback,
                                                             objectReferenceCallback,
                                                             NULL /*user_data*/))) {
        nsk_jvmti_setFailStatus();
    }
    NSK_DISPLAY0("IterateOverReachableObjects finished.\n");

    afterIteration();

    found = getCounter(&objectCount);
    NSK_DISPLAY1("Found tagged objects: %d\n", found);

    modified = OBJ_MAX_COUNT - found;
    if (modified > 0) {
        NSK_COMPLAIN2("Tags were modified by other thread during heap iteration: %d of %d\n",
                                                        modified, OBJ_MAX_COUNT);
        nsk_jvmti_setFailStatus();
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP04_ap04t003_runIterateOverInstancesOfClass(JNIEnv* jni,
                                                                                 jclass  klass) {
    int modified = 0;
    int found = 0;

    if (!prepareToIteration(jni))
        return;

    NSK_DISPLAY0("Calling IterateOverInstancesOfClass...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverInstancesOfClass(debugeeClass,
                                                             JVMTI_HEAP_OBJECT_TAGGED,
                                                             heapObjectCallback,
                                                             NULL /*user_data*/))) {
        nsk_jvmti_setFailStatus();
    }
    NSK_DISPLAY0("IterateOverInstancesOfClass finished.\n");

    afterIteration();

    found = getCounter(&objectCount);
    NSK_DISPLAY1("Found tagged objects: %d\n", found);

    modified = OBJ_MAX_COUNT - found;
    if (modified > 0) {
        NSK_COMPLAIN2("Tags were modified by other thread during heap iteration: %d of %d\n",
                                                        modified, OBJ_MAX_COUNT);
        nsk_jvmti_setFailStatus();
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP04_ap04t003_runIterateOverObjectsReachableFromObject(JNIEnv* jni_env,
                                                                                           jclass  klass) {
    ExceptionCheckingJniEnvPtr ec_jni(jni_env);
    jobject root = NULL;
    int modified = 0;
    int found = 0;

    root = ec_jni->GetStaticObjectField(debugeeClass, rootFieldID, TRACE_JNI_CALL);

    if (!prepareToIteration(jni_env))
        return;

    NSK_DISPLAY0("Calling IterateOverObjectsReachableFromObject...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverObjectsReachableFromObject(root,
                                                                       objectReferenceCallback,
                                                                       NULL /*user_data*/))) {
        nsk_jvmti_setFailStatus();
    }
    NSK_DISPLAY0("IterateOverObjectsReachableFromObject finished.\n");

    afterIteration();

    found = getCounter(&objectCount);
    NSK_DISPLAY1("Found tagged objects: %d\n", found);

    modified = OBJ_MAX_COUNT - found;
    if (modified > 0) {
        NSK_COMPLAIN2("Tags were modified by other thread during heap iteration: %d of %d\n",
                                                        modified, OBJ_MAX_COUNT);
        nsk_jvmti_setFailStatus();
    }
}

static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni_env, void* arg) {
    ExceptionCheckingJniEnvPtr ec_jni(jni_env);
    NSK_DISPLAY0("Wait for debugee start\n\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    NSK_DISPLAY1("Find debugee class: %s\n", DEBUGEE_SIGNATURE);
    debugeeClass = nsk_jvmti_classBySignature(DEBUGEE_SIGNATURE);
    if (debugeeClass == NULL) {
        nsk_jvmti_setFailStatus();
        return;
    }

    debugeeClass = (jclass) ec_jni->NewGlobalRef(debugeeClass, TRACE_JNI_CALL);

    NSK_DISPLAY1("Find ID of 'root' field: %s\n", ROOT_SIGNATURE);
    rootFieldID = ec_jni->GetStaticFieldID(debugeeClass, "root",
                                        ROOT_SIGNATURE, TRACE_JNI_CALL);

    NSK_DISPLAY0("Let debugee to run test cases\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;

    NSK_DISPLAY0("Wait for completion of test cases\n\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    ec_jni->DeleteGlobalRef(debugeeClass, TRACE_JNI_CALL);
    NSK_TRACE(jvmti->DestroyRawMonitor(counterMonitor_ptr));
    NSK_TRACE(jvmti->DestroyRawMonitor(startLock));
    NSK_TRACE(jvmti->DestroyRawMonitor(runLock));
    NSK_TRACE(jvmti->DestroyRawMonitor(endLock));

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ap04t003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ap04t003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ap04t003(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    /* create JVMTI environment */
    jvmti = nsk_jvmti_createJVMTIEnv(jvm, reserved);
    if (!NSK_VERIFY(jvmti != NULL))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("counterMonitor", &counterMonitor_ptr))) {
        return JNI_ERR;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("startLock", &startLock))) {
        return JNI_ERR;
    }
    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("runLock", &runLock))) {
        return JNI_ERR;
    }
    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("endLock", &endLock))) {
        return JNI_ERR;
    }

    memset(&caps, 0, sizeof(jvmtiCapabilities));
    caps.can_tag_objects = 1;

    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    if (!caps.can_tag_objects)
        NSK_DISPLAY0("Warning: tagging objects is not available\n");

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;
    NSK_DISPLAY0("agentProc has been set\n\n");

    return JNI_OK;
}

}
