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

#define EXP_OBJ_NUMBER 1

static JNIEnv *jni = NULL;
static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;

static jlong timeout = 0;
static jlong threadTag = 1;
static int rootThreadCount      = 0;
static int rootJNIGlobalCount   = 0;
static int rootJNILocalCount    = 0;
static const char* DEBUGEE_SIGNATURE    = "Lnsk/jvmti/scenarios/allocation/AP06/ap06t001;";
static const char* THREAD_CLS_SIGNATURE = "Lnsk/jvmti/scenarios/allocation/AP06/ap06t001Thread;";

/* jvmtiHeapRootCallback */
jvmtiIterationControl JNICALL
heapRootCallback(jvmtiHeapRootKind root_kind,
                  jlong class_tag,
                  jlong size,
                  jlong* tag_ptr,
                  void* user_data) {

    if (*tag_ptr == threadTag) {
        NSK_DISPLAY1("heapRootCallback: root kind=%s\n", TranslateRootKind(root_kind));
        switch (root_kind) {
        case JVMTI_HEAP_ROOT_THREAD:
           rootThreadCount++;
           break;
        case JVMTI_HEAP_ROOT_JNI_GLOBAL:
           rootJNIGlobalCount++;
           break;
        default:
           nsk_jvmti_setFailStatus();
           NSK_COMPLAIN1("heapRootCallback: unexpected root kind=%s\n", TranslateRootKind(root_kind));
        }
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

    if (*tag_ptr == threadTag) {
        NSK_DISPLAY4("stackReferenceCallback: root kind=%s, "
                     "method=%p, depth=%d, slot=%d\n", TranslateRootKind(root_kind), (void *)method, (int)depth, (int)slot);
        switch (root_kind) {
        case JVMTI_HEAP_ROOT_STACK_LOCAL:
           /* it's OK */
           break;

        case JVMTI_HEAP_ROOT_JNI_LOCAL:
           rootJNILocalCount++;
           break;
        default:
           nsk_jvmti_setFailStatus();
           NSK_COMPLAIN1("stackReferenceCallback: unexpected root kind: %s\n\n", TranslateRootKind(root_kind));
        }
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

    return JVMTI_ITERATION_ABORT;
}


/************************/

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP06_ap06t001Thread_setTag(JNIEnv* jni, jobject obj) {

    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(obj, threadTag))) {
        nsk_jvmti_setFailStatus();
    } else {
        NSK_DISPLAY0("setTag: the tag was set for checked thread.");
    }
}

static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    jclass debugeeClass = NULL;

    NSK_DISPLAY0("Wait for debugee start\n\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    do {
        jthread localRefThread;
        jthread globalRefThread;
        jfieldID fid;

        NSK_DISPLAY1("Find debugee class: %s\n", DEBUGEE_SIGNATURE);
        debugeeClass = nsk_jvmti_classBySignature(DEBUGEE_SIGNATURE);
        if (debugeeClass == NULL) {
            nsk_jvmti_setFailStatus();
            break;
        }

        if (!NSK_JNI_VERIFY(jni, (fid =
                jni->GetStaticFieldID(debugeeClass, "thread", THREAD_CLS_SIGNATURE)) != NULL)) {
            nsk_jvmti_setFailStatus();
            break;
        }

        if (!NSK_JNI_VERIFY(jni, (localRefThread =
                jni->GetStaticObjectField(debugeeClass, fid)) != NULL)) {
            NSK_COMPLAIN0("GetStaticObjectField returned NULL for 'thread' field value\n\n");
            nsk_jvmti_setFailStatus();
            break;
        }

        if (!NSK_JNI_VERIFY(jni, (globalRefThread = jni->NewGlobalRef(localRefThread)) != NULL))
            return;

        NSK_DISPLAY0("Calling IterateOverReachableObjects\n");
        if (!NSK_JVMTI_VERIFY(jvmti->IterateOverReachableObjects(heapRootCallback,
                                                                 stackReferenceCallback,
                                                                 objectReferenceCallback,
                                                                 NULL /*user_data*/))) {
            nsk_jvmti_setFailStatus();
            break;
        }

        if (rootJNILocalCount != 1) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN1("JVMTI_HEAP_ROOT_JNI_LOCAL root kind was returned wrong %d times "
                          "while iteration with IterateOverReachableObjects.\n\n", rootJNILocalCount);
        }

        if (rootJNIGlobalCount != 1) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN1("JVMTI_HEAP_ROOT_JNI_GLOBAL root kind was returned wrong %d times "
                          "while iteration with IterateOverReachableObjects.\n\n", rootJNIGlobalCount);
        }

        if (rootThreadCount != 1) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN1("JVMTI_HEAP_ROOT_THREAD root kind was returned wrong %d times "
                          "while iteration with IterateOverReachableObjects.\n\n", rootThreadCount);
        }

    } while (0);

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ap06t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ap06t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ap06t001(JavaVM *jvm, char *options, void *reserved) {
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
    caps.can_tag_objects = 1;

    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    if (!caps.can_tag_objects)
        NSK_DISPLAY0("Warning: tagging objects is not implemented\n");

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;
    NSK_DISPLAY0("agentProc has been set\n\n");

    return JNI_OK;
}

}
