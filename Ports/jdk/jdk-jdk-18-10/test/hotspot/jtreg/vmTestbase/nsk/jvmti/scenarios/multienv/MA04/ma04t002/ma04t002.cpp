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

#include <stdlib.h>
#include <string.h>
#include "jni_tools.h"
#include "agent_common.h"
#include "jvmti_tools.h"

#define PASSED 0
#define STATUS_FAILED 2
#define SAMPLE_TAG ((jlong) 111111)

extern "C" {

/* ========================================================================== */

/* scaffold objects */
static jlong timeout = 0;

/* test objects */
static jobject testedObject = NULL;
static jobject testedInstance = NULL;
static jclass testedClass = NULL;
static int ObjectsCount = 0;

/* ========================================================================== */

/** callback functions **/

static jvmtiIterationControl JNICALL heap_object_callback
    (jlong class_tag, jlong size, jlong* tag_ptr, void* user_data)
{
    char buffer[32];

    if (*tag_ptr != 0) {
        NSK_DISPLAY1("tag = %s\n", jlong_to_string(*tag_ptr, buffer));
        if (*tag_ptr == SAMPLE_TAG) {
            ObjectsCount++;
        } else {
            NSK_COMPLAIN1("testedObject tagged incorrectly, expected=%s,",
                jlong_to_string(SAMPLE_TAG, buffer));
            NSK_COMPLAIN1(" got=%s\n", jlong_to_string(*tag_ptr, buffer));
            nsk_jvmti_setFailStatus();
        }
    }

    return JVMTI_ITERATION_CONTINUE;
}

/* ========================================================================== */

static int prepare(JNIEnv* jni) {
    const char* CLASS_NAME = "nsk/jvmti/scenarios/multienv/MA04/ma04t002";
    const char* FIELD_NAME = "testedObject1";
    const char* FIELD_SIGNATURE = "Ljava/lang/Object;";
    const char* INSTANCE_NAME = "testedInstance1";
    const char* INSTANCE_SIGNATURE = "Lnsk/jvmti/scenarios/multienv/MA04/ma04t002;";
    jfieldID fid = NULL;

    NSK_DISPLAY0("Obtain tested object from a static field of debugee class\n");

    NSK_DISPLAY1("Find class: %s\n", CLASS_NAME);
    if (!NSK_JNI_VERIFY(jni, (testedClass = jni->FindClass(CLASS_NAME)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (testedClass = (jclass) jni->NewGlobalRef(testedClass)) != NULL))
        return NSK_FALSE;

    NSK_DISPLAY2("Find field: %s:%s\n", FIELD_NAME, FIELD_SIGNATURE);
    if (!NSK_JNI_VERIFY(jni, (fid =
            jni->GetStaticFieldID(testedClass, FIELD_NAME, FIELD_SIGNATURE)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (testedObject = jni->GetStaticObjectField(testedClass, fid)) != NULL))
        return NSK_FALSE;

    NSK_DISPLAY2("Find class instance: %s:%s\n",
        INSTANCE_NAME, INSTANCE_SIGNATURE);
    if (!NSK_JNI_VERIFY(jni, (fid =
            jni->GetStaticFieldID(testedClass, INSTANCE_NAME, INSTANCE_SIGNATURE)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (testedInstance =
            jni->GetStaticObjectField(testedClass, fid)) != NULL))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ========================================================================== */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    jint dummy;

    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!prepare(jni)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    NSK_DISPLAY0("Testcase #1: check that there are no tagged objects\n");

    ObjectsCount = 0;
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverHeap(JVMTI_HEAP_OBJECT_EITHER, heap_object_callback, &dummy))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY1("ObjectsCount = %d\n", ObjectsCount);
    if (ObjectsCount != 0) {
        NSK_COMPLAIN1("Some objects were unexpectedly tagged: %d\n",
            ObjectsCount);
        nsk_jvmti_setFailStatus();
    }

    ObjectsCount = 0;
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverHeap(JVMTI_HEAP_OBJECT_TAGGED, heap_object_callback, &dummy))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY1("ObjectsCount = %d\n", ObjectsCount);
    if (ObjectsCount != 0) {
        NSK_COMPLAIN1("Some objects were unexpectedly tagged: %d\n",
            ObjectsCount);
        nsk_jvmti_setFailStatus();
    }

    ObjectsCount = 0;
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverInstancesOfClass(testedClass,
                                                             JVMTI_HEAP_OBJECT_EITHER,
                                                             heap_object_callback,
                                                             &dummy))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY1("ObjectsCount = %d\n", ObjectsCount);
    if (ObjectsCount != 0) {
        NSK_COMPLAIN1("Some class instances were unexpectedly tagged: %d\n",
            ObjectsCount);
        nsk_jvmti_setFailStatus();
    }

    ObjectsCount = 0;
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverInstancesOfClass(testedClass,
                                                             JVMTI_HEAP_OBJECT_TAGGED,
                                                             heap_object_callback,
                                                             &dummy))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY1("ObjectsCount = %d\n", ObjectsCount);
    if (ObjectsCount != 0) {
        NSK_COMPLAIN1("Some class instances were unexpectedly tagged: %d\n",
            ObjectsCount);
        nsk_jvmti_setFailStatus();
    }

    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(testedObject, SAMPLE_TAG))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;


    NSK_DISPLAY0("Testcase #2: check that there is only one object tagged\n");

    ObjectsCount = 0;
    if (!NSK_JVMTI_VERIFY(
            jvmti->IterateOverHeap(JVMTI_HEAP_OBJECT_EITHER, heap_object_callback, &dummy))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY1("ObjectsCount = %d\n", ObjectsCount);
    if (ObjectsCount != 1) {
        NSK_COMPLAIN1("Expected 1 object to be tagged: %d\n", ObjectsCount);
        nsk_jvmti_setFailStatus();
    }

    ObjectsCount = 0;
    if (!NSK_JVMTI_VERIFY(
            jvmti->IterateOverHeap(JVMTI_HEAP_OBJECT_TAGGED, heap_object_callback, &dummy))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY1("ObjectsCount = %d\n", ObjectsCount);
    if (ObjectsCount != 1) {
        NSK_COMPLAIN1("Expected 1 object to be tagged: %d\n", ObjectsCount);
        nsk_jvmti_setFailStatus();
    }

    ObjectsCount = 0;
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverHeap(JVMTI_HEAP_OBJECT_UNTAGGED, heap_object_callback, &dummy))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY1("ObjectsCount = %d\n", ObjectsCount);
    if (ObjectsCount != 0) {
        NSK_COMPLAIN1("Some tagged objects were unexpectedly shown as untagged: %d\n",
            ObjectsCount);
        nsk_jvmti_setFailStatus();
    }

    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(testedInstance, SAMPLE_TAG))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;


    NSK_DISPLAY0("Testcase #3: check that there is only one class object tagged\n");

    ObjectsCount = 0;
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverInstancesOfClass(testedClass,
                                                             JVMTI_HEAP_OBJECT_EITHER,
                                                             heap_object_callback,
                                                             &dummy))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY1("ObjectsCount = %d\n", ObjectsCount);
    if (ObjectsCount != 1) {
        NSK_COMPLAIN1("Expected 1 class instance to be tagged: %d\n",
            ObjectsCount);
        nsk_jvmti_setFailStatus();
    }

    ObjectsCount = 0;
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverInstancesOfClass(testedClass,
                                                             JVMTI_HEAP_OBJECT_EITHER,
                                                             heap_object_callback,
                                                             &dummy))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY1("ObjectsCount = %d\n", ObjectsCount);
    if (ObjectsCount != 1) {
        NSK_COMPLAIN1("Expected 1 class instance to be tagged: %d\n",
            ObjectsCount);
        nsk_jvmti_setFailStatus();
    }

    ObjectsCount = 0;
    if (!NSK_JVMTI_VERIFY(jvmti->IterateOverHeap(JVMTI_HEAP_OBJECT_UNTAGGED, heap_object_callback, &dummy))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY1("ObjectsCount = %d\n", ObjectsCount);
    if (ObjectsCount != 0) {
        NSK_COMPLAIN1("Some tagged class instances were unexpectedly shown as untagged: %d\n",
            ObjectsCount);
        nsk_jvmti_setFailStatus();
    }


    NSK_TRACE(jni->DeleteGlobalRef(testedClass));

    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ma04t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ma04t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ma04t002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;
    jvmtiEventCallbacks callbacks;
    jvmtiCapabilities caps;

    NSK_DISPLAY0("Agent_OnLoad\n");

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    memset(&caps, 0, sizeof(caps));
    caps.can_tag_objects = 1;
    caps.can_generate_object_free_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
        return JNI_ERR;
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    memset(&callbacks, 0, sizeof(callbacks));
    if (!NSK_VERIFY(nsk_jvmti_init_MA(&callbacks)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_OBJECT_FREE, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

}
