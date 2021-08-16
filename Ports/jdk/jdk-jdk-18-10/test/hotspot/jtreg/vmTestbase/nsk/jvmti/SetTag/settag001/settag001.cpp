/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <string.h>
#include "jvmti.h"
#include "agent_common.h"
#include "jni_tools.h"
#include "jvmti_tools.h"

extern "C" {

/* ============================================================================= */

/* scaffold objects */
static jlong timeout = 0;

/* constant names */
#define DEBUGEE_CLASS_NAME    "nsk/jvmti/SetTag/settag001"
#define OBJECT_CLASS_NAME     "nsk/jvmti/SetTag/settag001TestedClass"
#define OBJECT_CLASS_SIG      "L" OBJECT_CLASS_NAME ";"
#define OBJECT_FIELD_NAME     "testedObject"

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    NSK_DISPLAY0("Wait for object created\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    /* perform testing */
    {
        jobject testedObject = NULL;
        jlong objectTag = 111;

        NSK_DISPLAY0(">>> Obtain tested object from a static field of debugee class\n");
        {
            jclass debugeeClass = NULL;
            jfieldID objectField = NULL;

            NSK_DISPLAY1("Find debugee class: %s\n", DEBUGEE_CLASS_NAME);
            if (!NSK_JNI_VERIFY(jni, (debugeeClass =
                    jni->FindClass(DEBUGEE_CLASS_NAME)) != NULL)) {
                nsk_jvmti_setFailStatus();
                return;
            }
            NSK_DISPLAY1("  ... found class: 0x%p\n", (void*)debugeeClass);

            NSK_DISPLAY1("Find static field: %s\n", OBJECT_FIELD_NAME);
            if (!NSK_JNI_VERIFY(jni, (objectField =
                    jni->GetStaticFieldID(debugeeClass, OBJECT_FIELD_NAME, OBJECT_CLASS_SIG)) != NULL)) {
                nsk_jvmti_setFailStatus();
                return;
            }
            NSK_DISPLAY1("  ... got fieldID: 0x%p\n", (void*)objectField);

            NSK_DISPLAY1("Get object from static field: %s\n", OBJECT_FIELD_NAME);
            if (!NSK_JNI_VERIFY(jni, (testedObject =
                    jni->GetStaticObjectField(debugeeClass, objectField)) != NULL)) {
                nsk_jvmti_setFailStatus();
                return;
            }
            NSK_DISPLAY1("  ... got object: 0x%p\n", (void*)testedObject);

            NSK_DISPLAY1("Create global reference for object: 0x%p\n", (void*)testedObject);
            if (!NSK_JNI_VERIFY(jni, (testedObject = jni->NewGlobalRef(testedObject)) != NULL)) {
                nsk_jvmti_setFailStatus();
                return;
            }
            NSK_DISPLAY1("  ... got reference: 0x%p\n", (void*)testedObject);
        }

        NSK_DISPLAY0(">>> Testcase #1: set tag for the tested object\n");
        {
            NSK_DISPLAY1("Set tag for object: 0x%p\n", (void*)testedObject);
            if (!NSK_JVMTI_VERIFY(jvmti->SetTag(testedObject, objectTag))) {
                nsk_jvmti_setFailStatus();
                return;
            }
            NSK_DISPLAY1("  ... tag set: %ld\n", (long)objectTag);
        }

        NSK_DISPLAY0(">>> Testcase #2: get tag of not changed object and compare with initial\n");
        {
            jlong tag = 222;

            NSK_DISPLAY1("Get tag for object: 0x%p\n", (void*)testedObject);
            if (!NSK_JVMTI_VERIFY(jvmti->GetTag(testedObject, &tag))) {
                nsk_jvmti_setFailStatus();
                return;
            }
            NSK_DISPLAY1("  ... got tag: %ld\n", (long)tag);

            if (tag != objectTag) {
                NSK_COMPLAIN2("GetTag() returns different tag for not changed object:\n"
                              "#   got tag:  %ld\n"
                              "#   expected: %ld\n",
                              (long)tag, (long)objectTag);
                nsk_jvmti_setFailStatus();
            } else {
                NSK_DISPLAY2("SUCCESS: Got tag is equal to initial: %ld = %ld\n",
                              (long)tag, (long)objectTag);
            }
        }

        NSK_DISPLAY0(">>> Testcase #3: get tag of changed object and compare with initial\n");
        {
            jlong tag = 333;

            NSK_DISPLAY0("Let debugee to change object data\n");
            if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
                return;
            if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
                return;

            NSK_DISPLAY1("Get tag for object: 0x%p\n", (void*)testedObject);
            if (!NSK_JVMTI_VERIFY(jvmti->GetTag(testedObject, &tag))) {
                nsk_jvmti_setFailStatus();
                return;
            }
            NSK_DISPLAY1("  ... got tag: %ld\n", (long)tag);

            if (tag != objectTag) {
                NSK_COMPLAIN2("GetTag() returns different tag for changed object:\n"
                              "#   got tag:  %ld\n"
                              "#   expected: %ld\n",
                              (long)tag, (long)objectTag);
                nsk_jvmti_setFailStatus();
            } else {
                NSK_DISPLAY2("SUCCESS: Got tag is equal to initial: %ld = %ld\n",
                              (long)tag, (long)objectTag);
            }
        }

        NSK_DISPLAY0(">>> Clean used data\n");
        {
            NSK_DISPLAY1("Delete object reference: 0x%p\n", (void*)testedObject);
            NSK_TRACE(jni->DeleteGlobalRef(testedObject));
        }
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_settag001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_settag001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_settag001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* add required capabilities */
    {
        jvmtiCapabilities caps;

        memset(&caps, 0, sizeof(caps));
        caps.can_tag_objects = 1;
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
            return JNI_ERR;
        }
    }

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
