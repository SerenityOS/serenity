/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <jvmti.h>
#include "agent_common.h"

#include "nsk_tools.h"
#include "jni_tools.h"
#include "JVMTITools.h"
#include "jvmti_tools.h"

extern "C" {

#define PASSED  0
#define STATUS_FAILED  2

#define EXP_OBJ_NUMBER 7

static JNIEnv *jni = NULL;
static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;

static volatile int obj_free = 0;
static volatile long obj_count = 0;

static jlong timeout = 0;
static int user_data = 0;
static const char* DEBUGEE_SIGNATURE = "Lnsk/jvmti/scenarios/allocation/AP01/ap01t001;";
static const jlong DEBUGEE_CLASS_TAG = (jlong)1024;

void JNICALL
ObjectFree(jvmtiEnv *jvmti_env, jlong tag) {
    NSK_DISPLAY1("ObjectFree event received for an object with tag %ld\n\n", (long)tag);
    obj_free++;
}

void JNICALL
VMDeath(jvmtiEnv *jvmti_env, JNIEnv *env) {

    NSK_DISPLAY0("VMDeath event received\n");

    if (obj_free != (EXP_OBJ_NUMBER - 1)) {
        NSK_COMPLAIN2(
            "Received unexpected number of ObjectFree events: %d\n"
            "\texpected number: %d\n",
            obj_free, (EXP_OBJ_NUMBER - 1));
        exit(95 + STATUS_FAILED);
    }

    exit(95 + PASSED);
}

jvmtiIterationControl JNICALL
heapObjectCallback(jlong class_tag,
                   jlong size,
                   jlong* tag_ptr,
                   void* user_data) {

    if (class_tag == DEBUGEE_CLASS_TAG) {
        obj_count++;
    }

    return JVMTI_ITERATION_CONTINUE;
}

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

    if (class_tag == DEBUGEE_CLASS_TAG && *tag_ptr == 0) {
        obj_count++;
        *tag_ptr = obj_count;
    }

    return JVMTI_ITERATION_CONTINUE;
}

jvmtiIterationControl JNICALL
heapRootCallback(jvmtiHeapRootKind root_kind,
                 jlong class_tag,
                 jlong size,
                 jlong* tag_ptr,
                 void* user_data) {

    if (class_tag == DEBUGEE_CLASS_TAG && *tag_ptr == 0) {
        obj_count++;
        *tag_ptr = obj_count;
    }

    return JVMTI_ITERATION_CONTINUE;
}

jvmtiIterationControl JNICALL
objectReferenceCallback(jvmtiObjectReferenceKind reference_kind,
                        jlong  class_tag,
                        jlong  size,
                        jlong* tag_ptr,
                        jlong  referrer_tag,
                        jint   referrer_index,
                        void*  user_data) {

    if (class_tag == DEBUGEE_CLASS_TAG && *tag_ptr == 0) {
        obj_count++;
        *tag_ptr = obj_count;
    }

    return JVMTI_ITERATION_CONTINUE;
}


/************************/

JNIEXPORT jobject JNICALL
Java_nsk_jvmti_scenarios_allocation_AP01_ap01t001_newObject(JNIEnv* jni, jclass cls) {
    jmethodID cid;
    jobject result;

    if (!NSK_JNI_VERIFY(jni, (cid = jni->GetMethodID(cls, "<init>", "()V")) != NULL)) {
         NSK_COMPLAIN0("newObject: GetMethodID returned NULL\n\n");
         nsk_jvmti_setFailStatus();
         return NULL;
    }

    if (!NSK_JNI_VERIFY(jni, (result = jni->NewObject(cls, cid)) != NULL)) {

         NSK_COMPLAIN0("newObject: NewObject returned NULL\n\n");
         nsk_jvmti_setFailStatus();
         return NULL;
    }

    return result;
}

JNIEXPORT jobject JNICALL
Java_nsk_jvmti_scenarios_allocation_AP01_ap01t001_allocObject(JNIEnv* jni, jclass cls) {
    jmethodID cid;
    jobject result;

    if (!NSK_JNI_VERIFY(jni, (cid = jni->GetMethodID(cls, "<init>", "()V")) != NULL)) {

         NSK_COMPLAIN0("allocObject: GetMethodID returned NULL\n\n");
         nsk_jvmti_setFailStatus();
         return NULL;
    }

    if (!NSK_JNI_VERIFY(jni, (result = jni->AllocObject(cls)) != NULL)) {

         NSK_COMPLAIN0("allocObject: AllocObject returned NULL\n\n");
         nsk_jvmti_setFailStatus();
         return NULL;
    }

    if (!NSK_JNI_VERIFY_VOID(jni,jni->CallNonvirtualVoidMethod(result, cls, cid))) {

         NSK_COMPLAIN0("newObject: CallNonvirtualVoidMethod failed\n\n");
         nsk_jvmti_setFailStatus();
         return NULL;
    }

    return result;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP01_ap01t001_flushObjectFreeEvents(JNIEnv* jni, jobject obj) {
    // Already enabled, but this triggers flush of pending events.
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                                          JVMTI_EVENT_OBJECT_FREE,
                                                          NULL))) {
        nsk_jvmti_setFailStatus();
    }
}

static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    jclass debugeeClass = NULL;

    NSK_DISPLAY0("Wait for debugee start\n\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    NSK_DISPLAY1("Find debugee class: %s\n", DEBUGEE_SIGNATURE);
    debugeeClass = nsk_jvmti_classBySignature(DEBUGEE_SIGNATURE);
    if (debugeeClass == NULL) {
        nsk_jvmti_setFailStatus();
        return;
    }

    NSK_DISPLAY0("Set tag for debugee class\n\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(debugeeClass, DEBUGEE_CLASS_TAG))) {
        nsk_jvmti_setFailStatus();
        return;
    }


    NSK_DISPLAY0("Calling IterateOverInstancesOfClass with filter JVMTI_HEAP_OBJECT_UNTAGGED\n");
    obj_count = 0;
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverInstancesOfClass(debugeeClass,
                                                             JVMTI_HEAP_OBJECT_UNTAGGED,
                                                             heapObjectCallback,
                                                             &user_data))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (obj_count != EXP_OBJ_NUMBER) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN2(
            "IterateOverInstancesOfClass found unexpected number of objects: %d\n"
            "\texpected number: %d\n\n",
            obj_count, EXP_OBJ_NUMBER);

    } else {
        NSK_DISPLAY1("Number of objects IterateOverInstancesOfClass has found: %d\n\n", obj_count);
    }

    NSK_DISPLAY0("Calling IterateOverHeap with filter JVMTI_HEAP_OBJECT_UNTAGGED\n");
    obj_count = 0;
    if (!NSK_JVMTI_VERIFY(
            jvmti->IterateOverHeap(JVMTI_HEAP_OBJECT_UNTAGGED, heapObjectCallback, &user_data))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (obj_count != EXP_OBJ_NUMBER) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN2(
            "IterateOverHeap found unexpected number of objects: %d\n"
            "\texpected number: %d\n\n",
            obj_count, EXP_OBJ_NUMBER);
    } else {
        NSK_DISPLAY1("Number of objects IterateOverHeap has found: %d\n\n", obj_count);
    }

    NSK_DISPLAY0("Calling IterateOverReachableObjects\n");
    obj_count = 0;
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverReachableObjects(heapRootCallback,
                                                             stackReferenceCallback,
                                                             objectReferenceCallback,
                                                             &user_data))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (obj_count != EXP_OBJ_NUMBER) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN2(
            "IterateOverReachableObjects found unexpected number of objects: %d\n"
            "\texpected number: %d\n\n",
            obj_count, EXP_OBJ_NUMBER);
    } else {
        NSK_DISPLAY1("Number of objects IterateOverReachableObjects has found: %d\n\n", obj_count);
    }

    NSK_DISPLAY0("Let debugee to provoke GC\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;


    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ap01t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ap01t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ap01t001(JavaVM *jvm, char *options, void *reserved) {
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

    memset(&caps, 0, sizeof(jvmtiCapabilities));
    caps.can_generate_object_free_events = 1;
    caps.can_tag_objects = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    if (!caps.can_generate_object_free_events)
        NSK_DISPLAY0("Warning: generation of object free events is not implemented\n");
    if (!caps.can_tag_objects)
        NSK_DISPLAY0("Warning: tagging objects is not implemented\n");

    /* set event callback */
    NSK_DISPLAY0("setting event callbacks ...\n");
    (void) memset(&callbacks, 0, sizeof(callbacks));

    callbacks.ObjectFree = &ObjectFree;
    callbacks.VMDeath = &VMDeath;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    NSK_DISPLAY0("setting event callbacks done\nenabling JVMTI events ...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                                          JVMTI_EVENT_OBJECT_FREE,
                                                          NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                                          JVMTI_EVENT_VM_DEATH,
                                                          NULL)))
        return JNI_ERR;
    NSK_DISPLAY0("enabling the events done\n\n");

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;
    NSK_DISPLAY0("agentProc has been set\n\n");

    return JNI_OK;
}

}
