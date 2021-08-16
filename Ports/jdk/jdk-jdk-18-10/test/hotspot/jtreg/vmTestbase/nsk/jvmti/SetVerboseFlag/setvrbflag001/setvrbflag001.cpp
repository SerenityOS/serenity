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

/* test objects */
static jrawMonitorID access_lock;
static jvmtiPhase phase;

/* ========================================================================== */

/* Check SetVerboseFlag function
 */
static int checkSetVerboseFlag(jvmtiEnv *jvmti) {
    if (!NSK_JVMTI_VERIFY(jvmti->SetVerboseFlag(JVMTI_VERBOSE_OTHER, JNI_TRUE)))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(jvmti->SetVerboseFlag(JVMTI_VERBOSE_OTHER, JNI_FALSE)))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(jvmti->SetVerboseFlag(JVMTI_VERBOSE_GC, JNI_TRUE)))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(jvmti->SetVerboseFlag(JVMTI_VERBOSE_GC, JNI_FALSE)))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(jvmti->SetVerboseFlag(JVMTI_VERBOSE_CLASS, JNI_TRUE)))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(jvmti->SetVerboseFlag(JVMTI_VERBOSE_CLASS, JNI_FALSE)))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(jvmti->SetVerboseFlag(JVMTI_VERBOSE_JNI, JNI_TRUE)))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(jvmti->SetVerboseFlag(JVMTI_VERBOSE_JNI, JNI_FALSE)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ========================================================================== */

void JNICALL
VMInit(jvmtiEnv *jvmti, JNIEnv* jni, jthread thread) {

    if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
        nsk_jvmti_setFailStatus();

    NSK_DISPLAY1("Phase: %s\n", TranslatePhase(phase));

    /* testcase #3: check SetVerboseFlag in VMInit */
    NSK_DISPLAY0("Testcase #3: check SetVerboseFlag in VMInit\n");
    if (!checkSetVerboseFlag(jvmti))
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

        /* testcase #2: check SetVerboseFlag in ClassFileLoadHook */
        NSK_DISPLAY0("Testcase #2: check SetVerboseFlag in ClassFileLoadHook\n");
        if (!checkSetVerboseFlag(jvmti))
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

    /* testcase #4: check SetVerboseFlag in agentProc */
    NSK_DISPLAY0("Testcase #4: check SetVerboseFlag in agentProc\n");
    if (!checkSetVerboseFlag(jvmti))
        nsk_jvmti_setFailStatus();

    /* resume debugee after last sync */
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/* agent library initialization */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setvrbflag001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setvrbflag001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setvrbflag001(JavaVM *jvm, char *options, void *reserved) {
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

    /* testcase #1: check SetVerboseFlag in Agent_OnLoad */
    NSK_DISPLAY0("Testcase #1: check SetVerboseFlag in Agent_OnLoad\n");
    if (!checkSetVerboseFlag(jvmti))
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
