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

extern "C" {

/* ========================================================================== */

/* scaffold objects */
static jlong timeout = 0;

/* test objects */
static jclass testedClass = NULL;
static jint klass_byte_count = 0;
static unsigned char *klass_bytes = NULL;
static int magicIndex = 0;
static int ClassFileLoadHookEventFlag = NSK_FALSE;

const char* CLASS_NAME = "nsk/jvmti/scenarios/multienv/MA06/ma06t001a";
static const jint magicNumber = 0x12345678;

/* ========================================================================== */

/** callback functions **/

static void JNICALL
ClassFileLoadHook(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
        jclass class_being_redefined, jobject loader,
        const char* name, jobject protection_domain,
        jint class_data_len, const unsigned char* class_data,
        jint *new_class_data_len, unsigned char** new_class_data) {
    jint i;

    if (name != NULL && (strcmp(name, CLASS_NAME) == 0)) {
        ClassFileLoadHookEventFlag = NSK_TRUE;
        NSK_DISPLAY0("ClassFileLoadHook event\n");

        if (class_being_redefined == NULL) {
            /* sent by class load */
            if (!NSK_JVMTI_VERIFY(jvmti_env->Allocate(class_data_len, &klass_bytes)))
                nsk_jvmti_setFailStatus();
            else {
                memcpy(klass_bytes, class_data, class_data_len);
                klass_byte_count = class_data_len;
                for (i = 0; i < klass_byte_count - 3; i++) {
                    if (((jint)klass_bytes[i+3] |
                        ((jint)klass_bytes[i+2] << 8) |
                        ((jint)klass_bytes[i+1] << 16) |
                        ((jint)klass_bytes[i] << 24)) == magicNumber) {
                        magicIndex = i;
                        break;
                    }
                }
                if (klass_byte_count == 0) {
                    NSK_COMPLAIN0("Cannot find magic number\n");
                    nsk_jvmti_setFailStatus();
                }
            }
        }
    }
}

/* ========================================================================== */

static int prepare(jvmtiEnv* jvmti, JNIEnv* jni) {

    NSK_DISPLAY1("Find class: %s\n", CLASS_NAME);
    if (!NSK_JNI_VERIFY(jni, (testedClass = jni->FindClass(CLASS_NAME)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (testedClass = (jclass) jni->NewGlobalRef(testedClass)) != NULL))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ========================================================================== */

static int redefine(jvmtiEnv* jvmti, jint value) {
    jvmtiClassDefinition class_def;

    if (!NSK_VERIFY(klass_byte_count != 0 && klass_bytes != NULL))
        return NSK_FALSE;

    if (!NSK_VERIFY(magicIndex != 0))
        return NSK_FALSE;

    NSK_DISPLAY1("Redefining with %d\n", value);

    klass_bytes[magicIndex] = 0;
    klass_bytes[magicIndex+1] = 0;
    klass_bytes[magicIndex+2] = 0;
    klass_bytes[magicIndex+3] = (jbyte)value;

    class_def.klass = testedClass;
    class_def.class_byte_count = klass_byte_count;
    class_def.class_bytes = klass_bytes;
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

    if (!NSK_VERIFY(ClassFileLoadHookEventFlag)) {
        NSK_COMPLAIN0("Missing initial ClassFileLoadHook event\n");
        nsk_jvmti_setFailStatus();
        return;
    }

    ClassFileLoadHookEventFlag = NSK_FALSE;

    if (!prepare(jvmti, jni)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (!redefine(jvmti, 1))
        nsk_jvmti_setFailStatus();

    /* resume debugee and wait for sync */
    if (!nsk_jvmti_resumeSync())
        return;
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!NSK_VERIFY(ClassFileLoadHookEventFlag)) {
        NSK_COMPLAIN0("Missing ClassFileLoadHook event #1\n");
        nsk_jvmti_setFailStatus();
    }

    ClassFileLoadHookEventFlag = NSK_FALSE;

    /* resume debugee and wait for sync */
    if (!nsk_jvmti_resumeSync())
        return;
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!NSK_VERIFY(ClassFileLoadHookEventFlag)) {
        NSK_COMPLAIN0("Missing ClassFileLoadHook event #3\n");
        nsk_jvmti_setFailStatus();
    }

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL)))
        nsk_jvmti_setFailStatus();

    NSK_TRACE(jni->DeleteGlobalRef(testedClass));

    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ma06t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ma06t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ma06t001(JavaVM *jvm, char *options, void *reserved) {
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
    caps.can_redefine_classes = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.ClassFileLoadHook = &ClassFileLoadHook;
    if (!NSK_VERIFY(nsk_jvmti_init_MA(&callbacks)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

}
