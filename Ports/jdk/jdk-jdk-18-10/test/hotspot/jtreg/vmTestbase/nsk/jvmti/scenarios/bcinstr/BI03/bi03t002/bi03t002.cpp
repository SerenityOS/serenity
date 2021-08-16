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

#include <stdlib.h>
#include <string.h>
#include "jni_tools.h"
#include "agent_common.h"
#include "jvmti_tools.h"

#define PASSED 0
#define STATUS_FAILED 2

extern "C" {

/* ========================================================================== */

/* scaffold objects */
static jlong timeout = 0;

/* test objects */
static jclass debugeeClass = NULL;
static jclass testedClass = NULL;
static jbyteArray classBytes = NULL;

const char* CLASS_NAME = "nsk/jvmti/scenarios/bcinstr/BI03/bi03t002a";

/* ========================================================================== */

static int prepare(jvmtiEnv* jvmti, JNIEnv* jni) {
    const char* DEBUGEE_CLASS_NAME =
        "nsk/jvmti/scenarios/bcinstr/BI03/bi03t002";
    jfieldID field = NULL;

    NSK_DISPLAY1("Find class: %s\n", DEBUGEE_CLASS_NAME);
    if (!NSK_JNI_VERIFY(jni, (debugeeClass = jni->FindClass(DEBUGEE_CLASS_NAME)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (debugeeClass = (jclass)jni->NewGlobalRef(debugeeClass)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (field =
            jni->GetStaticFieldID(debugeeClass, "newClassBytes", "[B")) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (classBytes = (jbyteArray)
            jni->GetStaticObjectField(debugeeClass, field)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (classBytes = (jbyteArray)jni->NewGlobalRef(classBytes)) != NULL))
        return NSK_FALSE;

    NSK_DISPLAY1("Find class: %s\n", CLASS_NAME);
    if (!NSK_JNI_VERIFY(jni, (testedClass = jni->FindClass(CLASS_NAME)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (testedClass = (jclass)jni->NewGlobalRef(testedClass)) != NULL))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ========================================================================== */

static int redefine(jvmtiEnv* jvmti, JNIEnv* jni) {
    jvmtiClassDefinition class_def;

    NSK_DISPLAY0("Redefining ...\n");

    if (!NSK_JNI_VERIFY(jni, (class_def.class_byte_count = jni->GetArrayLength(classBytes)) > 0))
        return NSK_TRUE;

    if (!NSK_JNI_VERIFY(jni, (class_def.class_bytes = (unsigned char*)
            jni->GetByteArrayElements(classBytes, NULL)) != NULL))
        return NSK_TRUE;

    class_def.klass = testedClass;
    if (!NSK_JVMTI_VERIFY(jvmti->RedefineClasses(1, &class_def)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ========================================================================== */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!prepare(jvmti, jni)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (!redefine(jvmti, jni))
        nsk_jvmti_setFailStatus();

    /* resume debugee and wait for sync */
    if (!nsk_jvmti_resumeSync())
        return;
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    NSK_TRACE(jni->DeleteGlobalRef(debugeeClass));
    NSK_TRACE(jni->DeleteGlobalRef(classBytes));
    NSK_TRACE(jni->DeleteGlobalRef(testedClass));

    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_bi03t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_bi03t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_bi03t002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;
    jvmtiCapabilities caps;

    NSK_DISPLAY0("Agent_OnLoad\n");

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    memset(&caps, 0, sizeof(caps));
    caps.can_redefine_classes = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

}
