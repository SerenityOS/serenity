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
static const char* DEBUGEE_SIGNATURE   = "Lnsk/jvmti/scenarios/allocation/AP09/ap09t001;";
static const long OBJECT_TAG         = 1l;
static const long CLASS_TAG          = 2l;
static const long LOADER_TAG         = 3l;
static const long DOMAIN_TAG         = 4l;
static const long INSTANCE_FIELD_TAG = 5l;
static const long STATIC_FIELD_TAG   = 6l;
static const long ARRAY_TAG          = 7l;
static const long INTERFACE_TAG      = 8l;
static int classFound         = 0;
static int loaderFound        = 0;
static int domainFound        = 0;
static int instanceFieldFound = 0;
static int staticFieldFound   = 0;
static int arrayFound         = 0;
static int interfaceFound     = 0;

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

    if (*tag_ptr != 0 && referrer_tag != 0) {
        NSK_DISPLAY4("objectReferenceCallback: reference kind=%s, referrer_index=%d, referrer_tag=%d, referree_tag=%d\n",
            TranslateObjectRefKind(reference_kind), (int)referrer_index, (long)referrer_tag, (long)*tag_ptr);

        if (reference_kind == JVMTI_REFERENCE_CLASS && *tag_ptr == CLASS_TAG) {
            classFound++;
        }
        if (reference_kind == JVMTI_REFERENCE_CLASS_LOADER && *tag_ptr == LOADER_TAG) {
            loaderFound++;
        }
        if (reference_kind == JVMTI_REFERENCE_INTERFACE && *tag_ptr == INTERFACE_TAG) {
            interfaceFound++;
        }
        if (reference_kind == JVMTI_REFERENCE_PROTECTION_DOMAIN && *tag_ptr == DOMAIN_TAG) {
            domainFound++;
        }
        if (reference_kind == JVMTI_REFERENCE_ARRAY_ELEMENT && *tag_ptr == STATIC_FIELD_TAG && referrer_tag == ARRAY_TAG) {
            arrayFound++;
        }
        if (reference_kind == JVMTI_REFERENCE_STATIC_FIELD && *tag_ptr == ARRAY_TAG) {
            staticFieldFound++;
        }
        if (reference_kind == JVMTI_REFERENCE_FIELD && *tag_ptr == INSTANCE_FIELD_TAG) {
            instanceFieldFound++;
        }
    }
    return JVMTI_ITERATION_CONTINUE;
}


/************************/

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP09_ap09t001_setTag(JNIEnv* jni,
                                                         jobject obj,
                                                         jobject target,
                                                         jlong   tag) {

    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(target, tag))) {
        nsk_jvmti_setFailStatus();
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP09_ap09t001_setReferrer(JNIEnv* jni, jclass klass, jobject ref) {
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
        NSK_DISPLAY0("\nCalling IterateOverObjectsReachableFromObject\n");
        if (!NSK_JVMTI_VERIFY(jvmti->IterateOverObjectsReachableFromObject(referrer,
                                                                           objectReferenceCallback,
                                                                           NULL /*user_data*/))) {
            nsk_jvmti_setFailStatus();
        }
        if (!classFound) {
            NSK_COMPLAIN0("Expected reference with kind=JVMTI_REFERENCE_CLASS was not iterated.");
            nsk_jvmti_setFailStatus();
        }
        if (!loaderFound) {
            NSK_COMPLAIN0("Expected reference with kind=JVMTI_REFERENCE_CLASS_LOADER was not iterated.");
            nsk_jvmti_setFailStatus();
        }
        if (!interfaceFound) {
            NSK_COMPLAIN0("Expected reference with kind=JVMTI_REFERENCE_INTERFACE was not iterated.");
            nsk_jvmti_setFailStatus();
        }
        if (!domainFound) {
            NSK_COMPLAIN0("Expected reference with kind=JVMTI_REFERENCE_PROTECTION_DOMAIN was not iterated.");
            nsk_jvmti_setFailStatus();
        }
        if (!arrayFound) {
            NSK_COMPLAIN0("Expected reference with kind=JVMTI_REFERENCE_ARRAY_ELEMENT was not iterated.");
            nsk_jvmti_setFailStatus();
        }
        if (!staticFieldFound) {
            NSK_COMPLAIN0("Expected reference with kind=JVMTI_REFERENCE_STATIC_FIELD was not iterated.");
            nsk_jvmti_setFailStatus();
        }
        if (!instanceFieldFound) {
            NSK_COMPLAIN0("Expected reference with kind=JVMTI_REFERENCE_FIELD was not iterated.");
            nsk_jvmti_setFailStatus();
        }

        NSK_TRACE(jni->DeleteGlobalRef(referrer));
    } while (0);

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ap09t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ap09t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ap09t001(JavaVM *jvm, char *options, void *reserved) {
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

    if (!caps.can_tag_objects) {
        NSK_COMPLAIN0("Tagging objects is not available.\n");
        return JNI_ERR;
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;
    NSK_DISPLAY0("agentProc has been set\n\n");

    return JNI_OK;
}

}
