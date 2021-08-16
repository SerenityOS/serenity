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

#define PASSED  0
#define STATUS_FAILED  2

#define TESTED_CLASS "ap02t001Exception"

static JNIEnv *jni = NULL;
static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;

static volatile int obj_count = 0;
static int first_count = 0;

static jlong timeout = 0;
static int user_data = 0;
static const char* TESTED_CLASS_SIGNATURE = "Lnsk/jvmti/scenarios/allocation/AP02/ap02t001Exception;";
static const jlong TESTED_CLASS_TAG = (jlong)1024;
static jclass testedClass = NULL;

jvmtiIterationControl JNICALL
heapObjectCallback(jlong class_tag,
                   jlong size,
                   jlong* tag_ptr,
                   void* user_data) {

    if (class_tag == TESTED_CLASS_TAG) {
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

    if (class_tag == TESTED_CLASS_TAG) {
        obj_count++;
    }

    return JVMTI_ITERATION_CONTINUE;
}

/************************/

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_allocation_AP02_ap02t001_throwException(JNIEnv* jni,
                                                                 jclass cls,
                                                                 jclass exception_cls) {
    jint result;

    result = jni->ThrowNew(exception_cls, "Got expected exception thrown from native code");
    if (result != 0) {
        NSK_COMPLAIN1("throwException: Unable to throw exception in native code: %d\n\n", result);
        nsk_jvmti_setFailStatus();
    } else {
        NSK_DISPLAY0("throwException: ThrowNew returned success code: 0\n\n");
    }
}

static void runIterations (jvmtiEnv* jvmti, jclass testedClass, jint exp_count) {
    NSK_DISPLAY0("Calling IterateOverInstancesOfClass with filter JVMTI_HEAP_OBJECT_EITHER\n");
    obj_count = 0;
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverInstancesOfClass(testedClass,
                                                             JVMTI_HEAP_OBJECT_EITHER,
                                                             heapObjectCallback,
                                                             &user_data))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (obj_count != exp_count) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN3(
            "IterateOverInstancesOfClass found unexpected number of %s objects: %d\n"
            "\texpected number: %d\n\n",
            TESTED_CLASS, obj_count, exp_count);
    } else {
        NSK_DISPLAY2("Number of %s objects IterateOverInstancesOfClass has found: %d\n\n", TESTED_CLASS,
            obj_count);
    }

    NSK_DISPLAY0("Calling IterateOverHeap with filter JVMTI_HEAP_OBJECT_EITHER\n");
    obj_count = 0;
    if (!NSK_JVMTI_VERIFY(
            jvmti->IterateOverHeap(JVMTI_HEAP_OBJECT_EITHER, heapObjectCallback, &user_data))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (obj_count != exp_count) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN3(
            "IterateOverHeap found unexpected number of %s objects: %d\n"
            "\texpected number: %d\n\n",
            TESTED_CLASS, obj_count, exp_count);
    } else {
        NSK_DISPLAY2("Number of %s objects IterateOverHeap has found: %d\n\n", TESTED_CLASS, obj_count);
    }

    NSK_DISPLAY0("Calling IterateOverReachableObjects\n");
    obj_count = 0;
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverReachableObjects(NULL /*heapRootCallback*/,
                                                             stackReferenceCallback,
                                                             NULL /*objectReferenceCallback*/,
                                                             &user_data))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (obj_count != exp_count) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN3(
            "IterateOverReachableObjects found unexpected number of %s objects: %d\n"
            "\texpected number: %d\n\n",
            TESTED_CLASS, obj_count, exp_count);
    } else {
        NSK_DISPLAY2("Number of %s objects IterateOverReachableObjects has found: %d\n\n", TESTED_CLASS,
            obj_count);
    }

    first_count = obj_count;
}

static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    NSK_DISPLAY0("Wait for debugee start\n\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    NSK_DISPLAY1("Find tested class: %s\n", TESTED_CLASS_SIGNATURE);
    testedClass = nsk_jvmti_classBySignature(TESTED_CLASS_SIGNATURE);
    if (testedClass == NULL) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (!NSK_JNI_VERIFY(jni, (testedClass = (jclass)jni->NewGlobalRef(testedClass)) != NULL))
        return;

    NSK_DISPLAY0("Set tag for tested class\n\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(testedClass, TESTED_CLASS_TAG))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    runIterations (jvmti, testedClass, 1);

    NSK_DISPLAY0("Go to next case\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    runIterations (jvmti, testedClass, 2);

    NSK_TRACE(jni->DeleteGlobalRef(testedClass));

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ap02t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ap02t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ap02t001(JavaVM *jvm, char *options, void *reserved) {
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
