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
static jobject referrer = NULL;
static const char* SUBCLASS_SIGNATURE   = "Lnsk/jvmti/scenarios/allocation/AP05/ap05t001Subclass;";
static const int   EXPECTED_STATIC_FIELDS_COUNT = 8;
/* 8 ones declared in ap05t001Superclass + 8 ones declared in ap05t001Subclass */
static const int   EXPECTED_INSTANCE_FIELDS_COUNT = 16;
static const long  CLS_TAG = 1l, REFERRER_TAG = 2l, REFERREE_TAG = 10l;
static int staticFieldsCount = 0, instanceFieldsCount = 0;

/* jvmtiHeapRootCallback */
jvmtiIterationControl JNICALL
heapRootCallback(jvmtiHeapRootKind root_kind,
                 jlong class_tag,
                 jlong size,
                 jlong* tag_ptr,
                 void* user_data) {
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

    if (*tag_ptr == REFERREE_TAG && (referrer_tag == CLS_TAG || referrer_tag == REFERRER_TAG)) {
        NSK_DISPLAY4("objectReferenceCallback: reference kind=%s, referrer_index=%d, referrer_tag=%d, referree_tag=%d\n",
            TranslateObjectRefKind(reference_kind), (int)referrer_index, (long)referrer_tag, (long)*tag_ptr);
        if (reference_kind == JVMTI_REFERENCE_FIELD) {
             instanceFieldsCount++;
        } else if (reference_kind == JVMTI_REFERENCE_STATIC_FIELD) {
             staticFieldsCount++;
        }
    }
    return JVMTI_ITERATION_CONTINUE;
}


/************************/

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP05_ap05t001_setTag(JNIEnv* jni,
                                                         jobject obj,
                                                         jobject target,
                                                         jlong   tag) {

    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(target, tag))) {
        nsk_jvmti_setFailStatus();
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP05_ap05t001_setReferrer(JNIEnv* jni, jclass klass, jobject ref) {
    if (!NSK_JNI_VERIFY(jni, (referrer = jni->NewGlobalRef(ref)) != NULL))
        nsk_jvmti_setFailStatus();
}

static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    jclass debugeeClass = NULL;

    NSK_DISPLAY0("Wait for debugee start\n\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    do {
        staticFieldsCount = 0;
        instanceFieldsCount = 0;
        NSK_DISPLAY0("\nCalling IterateOverReachableObjects\n");
        if (!NSK_JVMTI_VERIFY(jvmti->IterateOverReachableObjects(heapRootCallback,
                                                                 stackReferenceCallback,
                                                                 objectReferenceCallback,
                                                                 NULL /*user_data*/))) {
            nsk_jvmti_setFailStatus();
            break;
        }
        if (instanceFieldsCount != EXPECTED_INSTANCE_FIELDS_COUNT) {
            NSK_COMPLAIN3("IterateOverReachableObjects found wrong number of instance fields:\n\t"
                  " signature: %s\n\t found number: %d\n\t expected number: %d\n\n",
                  SUBCLASS_SIGNATURE, instanceFieldsCount, EXPECTED_INSTANCE_FIELDS_COUNT);
            nsk_jvmti_setFailStatus();
        }
        if (staticFieldsCount != EXPECTED_STATIC_FIELDS_COUNT) {
            NSK_COMPLAIN3("IterateOverReachableObjects found wrong number of static fields:\n\t"
                  " signature: %s\n\t found number: %d\n\t expected number: %d\n\n",
                  SUBCLASS_SIGNATURE, staticFieldsCount, EXPECTED_STATIC_FIELDS_COUNT);
            nsk_jvmti_setFailStatus();
        }

        staticFieldsCount = 0;
        instanceFieldsCount = 0;
        NSK_DISPLAY0("\nCalling IterateOverObjectsReachableFromObject\n");
        {
            if (!NSK_JVMTI_VERIFY(jvmti->IterateOverObjectsReachableFromObject(
                    referrer, objectReferenceCallback, NULL /*user_data*/))) {
                nsk_jvmti_setFailStatus();
                break;
            }
        }
        if (instanceFieldsCount != EXPECTED_INSTANCE_FIELDS_COUNT) {
            NSK_COMPLAIN3("IterateOverObjectsReachableFromObject found wrong number of instance fields:\n\t"
                  " signature: %s\n\t found number: %d\n\t expected number: %d\n\n",
                  SUBCLASS_SIGNATURE, instanceFieldsCount, EXPECTED_INSTANCE_FIELDS_COUNT);
            nsk_jvmti_setFailStatus();
        }
        if (staticFieldsCount != EXPECTED_STATIC_FIELDS_COUNT) {
            NSK_COMPLAIN3("IterateOverObjectsReachableFromObject found wrong number of static fields:\n\t"
                  " signature: %s\n\t found number: %d\n\t expected number: %d\n\n",
                  SUBCLASS_SIGNATURE, staticFieldsCount, EXPECTED_STATIC_FIELDS_COUNT);
            nsk_jvmti_setFailStatus();
        }


        NSK_TRACE(jni->DeleteGlobalRef(referrer));

    } while (0);

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ap05t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ap05t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ap05t001(JavaVM *jvm, char *options, void *reserved) {
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
