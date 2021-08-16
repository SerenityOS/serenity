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

#include <stdlib.h>
#include <string.h>
#include "jni_tools.h"
#include "agent_common.h"
#include "jvmti_tools.h"

extern "C" {

/* ========================================================================== */

/* scaffold objects */
static jlong timeout = 0;

typedef struct {
    jvmtiError err;
    const char *name;
} error_info;

/* test objects */
static jrawMonitorID access_lock;
static jvmtiPhase phase;
static error_info errors[] = {
    { JVMTI_ERROR_NONE,
      "JVMTI_ERROR_NONE" },
    { JVMTI_ERROR_NULL_POINTER,
      "JVMTI_ERROR_NULL_POINTER" },
    { JVMTI_ERROR_OUT_OF_MEMORY,
      "JVMTI_ERROR_OUT_OF_MEMORY" },
    { JVMTI_ERROR_ACCESS_DENIED,
      "JVMTI_ERROR_ACCESS_DENIED" },
    { JVMTI_ERROR_UNATTACHED_THREAD,
      "JVMTI_ERROR_UNATTACHED_THREAD" },
    { JVMTI_ERROR_INVALID_ENVIRONMENT,
      "JVMTI_ERROR_INVALID_ENVIRONMENT" },
    { JVMTI_ERROR_WRONG_PHASE,
      "JVMTI_ERROR_WRONG_PHASE" },
    { JVMTI_ERROR_INTERNAL,
      "JVMTI_ERROR_INTERNAL" },
    { JVMTI_ERROR_INVALID_PRIORITY,
      "JVMTI_ERROR_INVALID_PRIORITY" },
    { JVMTI_ERROR_THREAD_NOT_SUSPENDED,
      "JVMTI_ERROR_THREAD_NOT_SUSPENDED" },
    { JVMTI_ERROR_THREAD_SUSPENDED,
      "JVMTI_ERROR_THREAD_SUSPENDED" },
    { JVMTI_ERROR_THREAD_NOT_ALIVE,
      "JVMTI_ERROR_THREAD_NOT_ALIVE" },
    { JVMTI_ERROR_CLASS_NOT_PREPARED,
      "JVMTI_ERROR_CLASS_NOT_PREPARED" },
    { JVMTI_ERROR_NO_MORE_FRAMES,
      "JVMTI_ERROR_NO_MORE_FRAMES" },
    { JVMTI_ERROR_OPAQUE_FRAME,
      "JVMTI_ERROR_OPAQUE_FRAME" },
    { JVMTI_ERROR_DUPLICATE,
      "JVMTI_ERROR_DUPLICATE" },
    { JVMTI_ERROR_NOT_FOUND,
      "JVMTI_ERROR_NOT_FOUND" },
    { JVMTI_ERROR_NOT_MONITOR_OWNER,
      "JVMTI_ERROR_NOT_MONITOR_OWNER" },
    { JVMTI_ERROR_INTERRUPT,
      "JVMTI_ERROR_INTERRUPT" },
    { JVMTI_ERROR_UNMODIFIABLE_CLASS,
      "JVMTI_ERROR_UNMODIFIABLE_CLASS" },
    { JVMTI_ERROR_NOT_AVAILABLE,
      "JVMTI_ERROR_NOT_AVAILABLE" },
    { JVMTI_ERROR_ABSENT_INFORMATION,
      "JVMTI_ERROR_ABSENT_INFORMATION" },
    { JVMTI_ERROR_INVALID_EVENT_TYPE,
      "JVMTI_ERROR_INVALID_EVENT_TYPE" },
    { JVMTI_ERROR_NATIVE_METHOD,
      "JVMTI_ERROR_NATIVE_METHOD" },
    { JVMTI_ERROR_INVALID_THREAD,
      "JVMTI_ERROR_INVALID_THREAD" },
    { JVMTI_ERROR_INVALID_FIELDID,
      "JVMTI_ERROR_INVALID_FIELDID" },
    { JVMTI_ERROR_INVALID_METHODID,
      "JVMTI_ERROR_INVALID_METHODID" },
    { JVMTI_ERROR_INVALID_LOCATION,
      "JVMTI_ERROR_INVALID_LOCATION" },
    { JVMTI_ERROR_INVALID_OBJECT,
      "JVMTI_ERROR_INVALID_OBJECT" },
    { JVMTI_ERROR_INVALID_CLASS,
      "JVMTI_ERROR_INVALID_CLASS" },
    { JVMTI_ERROR_TYPE_MISMATCH,
      "JVMTI_ERROR_TYPE_MISMATCH" },
    { JVMTI_ERROR_INVALID_SLOT,
      "JVMTI_ERROR_INVALID_SLOT" },
    { JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
      "JVMTI_ERROR_MUST_POSSESS_CAPABILITY" },
    { JVMTI_ERROR_INVALID_THREAD_GROUP,
      "JVMTI_ERROR_INVALID_THREAD_GROUP" },
    { JVMTI_ERROR_INVALID_MONITOR,
      "JVMTI_ERROR_INVALID_MONITOR" },
    { JVMTI_ERROR_ILLEGAL_ARGUMENT,
      "JVMTI_ERROR_ILLEGAL_ARGUMENT" },
    { JVMTI_ERROR_INVALID_TYPESTATE,
      "JVMTI_ERROR_INVALID_TYPESTATE" },
    { JVMTI_ERROR_UNSUPPORTED_VERSION,
      "JVMTI_ERROR_UNSUPPORTED_VERSION" },
    { JVMTI_ERROR_INVALID_CLASS_FORMAT,
      "JVMTI_ERROR_INVALID_CLASS_FORMAT" },
    { JVMTI_ERROR_CIRCULAR_CLASS_DEFINITION,
      "JVMTI_ERROR_CIRCULAR_CLASS_DEFINITION" },
    { JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED,
      "JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_ADDED" },
    { JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED,
      "JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED" },
    { JVMTI_ERROR_FAILS_VERIFICATION,
      "JVMTI_ERROR_FAILS_VERIFICATION" },
    { JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED,
      "JVMTI_ERROR_UNSUPPORTED_REDEFINITION_HIERARCHY_CHANGED" },
    { JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED,
      "JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_DELETED" },
    { JVMTI_ERROR_NAMES_DONT_MATCH,
      "JVMTI_ERROR_NAMES_DONT_MATCH" },
    { JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_MODIFIERS_CHANGED,
      "JVMTI_ERROR_UNSUPPORTED_REDEFINITION_CLASS_MODIFIERS_CHANGED" },
    { JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_MODIFIERS_CHANGED,
      "JVMTI_ERROR_UNSUPPORTED_REDEFINITION_METHOD_MODIFIERS_CHANGED" }
};

/* ========================================================================== */

/* Check GetErrorName function
 */
static int checkGetErrorName(jvmtiEnv *jvmti) {
    int return_value = NSK_TRUE;
    size_t i;
    char *name;

    for (i = 0; i < sizeof(errors)/sizeof(error_info); i++) {
        if (!NSK_JVMTI_VERIFY(
                jvmti->GetErrorName(errors[i].err, &name)))
            return NSK_FALSE;
        if (strcmp(name, errors[i].name) != 0) {
            NSK_COMPLAIN2("Error: function returns \"%s\", expected \"%s\"\n",
                name, errors[i].name);
            return_value = NSK_FALSE;
        }
        if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)name)))
            return NSK_FALSE;
    }

    return return_value;
}

/* ========================================================================== */

void JNICALL
VMInit(jvmtiEnv *jvmti, JNIEnv* jni, jthread thread) {

    if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
        nsk_jvmti_setFailStatus();

    NSK_DISPLAY1("Phase: %s\n", TranslatePhase(phase));

    /* testcase #3: check GetErrorName in VMInit */
    NSK_DISPLAY0("Testcase #3: check GetErrorName in VMInit\n");
    if (!checkGetErrorName(jvmti))
        nsk_jvmti_setFailStatus();
}

void JNICALL
ClassFileLoadHook(jvmtiEnv *jvmti, JNIEnv *jni,
                  jclass class_being_redefined,
                  jobject loader, const char* name,
                  jobject protection_domain,
                  jint class_data_len,
                  const unsigned char* class_data,
                  jint* new_class_data_len,
                  unsigned char** new_class_data) {
    jvmtiPhase curr_phase;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(access_lock)))
        nsk_jvmti_setFailStatus();

    if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&curr_phase)))
        nsk_jvmti_setFailStatus();

    if (phase != curr_phase) {
        phase = curr_phase;
        NSK_DISPLAY1("Phase: %s\n", TranslatePhase(phase));

        /* testcase #2: check GetErrorName in ClassFileLoadHook */
        NSK_DISPLAY0("Testcase #2: check GetErrorName in ClassFileLoadHook\n");
        if (!checkGetErrorName(jvmti))
            nsk_jvmti_setFailStatus();
    }

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(access_lock)))
        nsk_jvmti_setFailStatus();
}

/* ========================================================================== */

/* agent algorithm */
static void JNICALL
agentProc(jvmtiEnv *jvmti, JNIEnv* jni, void* arg) {

    /* wait for debuggee start */
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
        nsk_jvmti_setFailStatus();

    NSK_DISPLAY1("Phase: %s\n", TranslatePhase(phase));

    /* testcase #4: check GetErrorName in agentProc */
    NSK_DISPLAY0("Testcase #4: check GetErrorName in agentProc\n");
    if (!checkGetErrorName(jvmti))
        nsk_jvmti_setFailStatus();

    /* resume debugee after last sync */
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/* agent library initialization */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_geterrname001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_geterrname001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_geterrname001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv *jvmti = NULL;
    jvmtiEventCallbacks callbacks;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60000;
    NSK_DISPLAY1("Timeout: %d msc\n", (int)timeout);

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* Create data access lock */
    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("_access_lock", &access_lock)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
        return JNI_ERR;

    NSK_DISPLAY1("Phase: %s\n", TranslatePhase(phase));

    /* testcase #1: check GetErrorName in Agent_OnLoad */
    NSK_DISPLAY0("Testcase #1: check GetErrorName in Agent_OnLoad\n");
    if (!checkGetErrorName(jvmti))
        nsk_jvmti_setFailStatus();

    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.VMInit = &VMInit;
    callbacks.ClassFileLoadHook = &ClassFileLoadHook;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    /* enable VMInit event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL)))
        return JNI_ERR;

    /* enable ClassFileLoadHook event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL)))
        return JNI_ERR;

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

}
