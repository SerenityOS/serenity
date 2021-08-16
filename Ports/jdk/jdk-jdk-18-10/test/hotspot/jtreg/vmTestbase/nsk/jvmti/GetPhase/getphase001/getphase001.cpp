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
static volatile jboolean was_VMStart = JNI_FALSE;
static volatile jboolean was_VMInit = JNI_FALSE;
static volatile jboolean was_VMDeath = JNI_FALSE;

/* ========================================================================== */

static void JNICALL
VMStart(jvmtiEnv *jvmti, JNIEnv* jni) {
    jvmtiPhase phase;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(access_lock)))
        nsk_jvmti_setFailStatus();

    NSK_DISPLAY0("VMStart\n");

    /* testcase #2: check JVMTI_PHASE_START */
    NSK_DISPLAY0("Testcase #2: check if GetPhase returns JVMTI_PHASE_START\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
        nsk_jvmti_setFailStatus();

    if (!NSK_VERIFY(phase == JVMTI_PHASE_START))
        nsk_jvmti_setFailStatus();

    was_VMStart = JNI_TRUE;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(access_lock)))
        nsk_jvmti_setFailStatus();
}

static void JNICALL
VMInit(jvmtiEnv *jvmti, JNIEnv* jni, jthread thread) {
    jvmtiPhase phase;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(access_lock)))
        nsk_jvmti_setFailStatus();

    NSK_DISPLAY0("VMInit\n");

    /* testcase #3: check JVMTI_PHASE_LIVE */
    NSK_DISPLAY0("Testcase #3: check if GetPhase returns JVMTI_PHASE_LIVE\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
        nsk_jvmti_setFailStatus();

    if (!NSK_VERIFY(phase == JVMTI_PHASE_LIVE))
        nsk_jvmti_setFailStatus();

    was_VMInit = JNI_TRUE;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(access_lock)))
        nsk_jvmti_setFailStatus();
}

static void JNICALL
NativeMethodBind(jvmtiEnv* jvmti, JNIEnv *jni,
                 jthread thread, jmethodID method,
                 void* address, void** new_address_ptr) {
    jvmtiPhase phase;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(access_lock)))
        nsk_jvmti_setFailStatus();

    NSK_DISPLAY0("NativeMethodBind\n");

    if (was_VMStart == JNI_FALSE) {

        /* testcase #5: check JVMTI_PHASE_PRIMORDIAL */
        NSK_DISPLAY0("Testcase #2: check if GetPhase returns JVMTI_PHASE_PRIMORDIAL\n");
        if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
            nsk_jvmti_setFailStatus();

        if (!NSK_VERIFY(phase == JVMTI_PHASE_PRIMORDIAL))
            nsk_jvmti_setFailStatus();

    } else if (was_VMInit == JNI_FALSE) {

        /* testcase #2: check JVMTI_PHASE_START */
        NSK_DISPLAY0("Testcase #2: check if GetPhase returns JVMTI_PHASE_START\n");
        if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
            nsk_jvmti_setFailStatus();

        if (!NSK_VERIFY(phase == JVMTI_PHASE_START))
            nsk_jvmti_setFailStatus();

    } else if (was_VMDeath == JNI_FALSE) {

        /* testcase #3: check JVMTI_PHASE_LIVE */
        NSK_DISPLAY0("Testcase #3: check if GetPhase returns JVMTI_PHASE_LIVE\n");
        if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
            nsk_jvmti_setFailStatus();

        if (!NSK_VERIFY(phase == JVMTI_PHASE_LIVE))
            nsk_jvmti_setFailStatus();

    } else { /* was_VMDeath == JNI_TRUE */

        /* testcase #4: check JVMTI_PHASE_DEAD */
        NSK_DISPLAY0("Testcase #4: check if GetPhase returns JVMTI_PHASE_DEAD\n");
        if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
            nsk_jvmti_setFailStatus();

        if (!NSK_VERIFY(phase == JVMTI_PHASE_DEAD))
            nsk_jvmti_setFailStatus();
    }

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(access_lock)))
        nsk_jvmti_setFailStatus();
}

static void JNICALL
ClassFileLoadHook(jvmtiEnv *jvmti, JNIEnv *jni,
                  jclass class_being_redefined,
                  jobject loader, const char* name,
                  jobject protection_domain,
                  jint class_data_len,
                  const unsigned char* class_data,
                  jint* new_class_data_len,
                  unsigned char** new_class_data) {
    jvmtiPhase phase;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(access_lock)))
        nsk_jvmti_setFailStatus();

    NSK_DISPLAY1("ClassFileLoadHook: %s\n", name);

    if (was_VMStart == JNI_FALSE) {

        /* testcase #5: check JVMTI_PHASE_PRIMORDIAL */
        NSK_DISPLAY0("Testcase #2: check if GetPhase returns JVMTI_PHASE_PRIMORDIAL\n");
        if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
            nsk_jvmti_setFailStatus();

        if (!NSK_VERIFY(phase == JVMTI_PHASE_PRIMORDIAL))
            nsk_jvmti_setFailStatus();

    } else if (was_VMInit == JNI_FALSE) {

        /* testcase #2: check JVMTI_PHASE_START */
        NSK_DISPLAY0("Testcase #2: check if GetPhase returns JVMTI_PHASE_START\n");
        if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
            nsk_jvmti_setFailStatus();

        if (!NSK_VERIFY(phase == JVMTI_PHASE_START))
            nsk_jvmti_setFailStatus();

    } else if (was_VMDeath == JNI_FALSE) {

        /* testcase #3: check JVMTI_PHASE_LIVE */
        NSK_DISPLAY0("Testcase #3: check if GetPhase returns JVMTI_PHASE_LIVE\n");
        if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
            nsk_jvmti_setFailStatus();

        if (!NSK_VERIFY(phase == JVMTI_PHASE_LIVE))
            nsk_jvmti_setFailStatus();

    } else { /* was_VMDeath == JNI_TRUE */

        /* testcase #4: check JVMTI_PHASE_DEAD */
        NSK_DISPLAY0("Testcase #4: check if GetPhase returns JVMTI_PHASE_DEAD\n");
        if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
            nsk_jvmti_setFailStatus();

        if (!NSK_VERIFY(phase == JVMTI_PHASE_DEAD))
            nsk_jvmti_setFailStatus();
    }

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(access_lock)))
        nsk_jvmti_setFailStatus();
}

static void JNICALL
VMDeath(jvmtiEnv *jvmti, JNIEnv* jni) {
    jvmtiPhase phase;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(access_lock)))
        nsk_jvmti_setFailStatus();

    NSK_DISPLAY0("VMDeath\n");

    /* testcase #3: check JVMTI_PHASE_LIVE */
    NSK_DISPLAY0("Testcase #3: check if GetPhase returns JVMTI_PHASE_LIVE\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
        nsk_jvmti_setFailStatus();

    if (!NSK_VERIFY(phase == JVMTI_PHASE_LIVE))
        nsk_jvmti_setFailStatus();

    was_VMDeath = JNI_TRUE;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(access_lock)))
        nsk_jvmti_setFailStatus();
}

/* ========================================================================== */

/* agent algorithm */
static void JNICALL
agentProc(jvmtiEnv *jvmti, JNIEnv* jni, void* arg) {
    jvmtiPhase phase;

    NSK_DISPLAY0("agentProc\n");

    /* wait for debuggee start */
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    /* testcase #3: check JVMTI_PHASE_LIVE */
    NSK_DISPLAY0("Testcase #3: check if GetPhase returns JVMTI_PHASE_LIVE\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
        nsk_jvmti_setFailStatus();

    if (!NSK_VERIFY(phase == JVMTI_PHASE_LIVE))
        nsk_jvmti_setFailStatus();

    /* resume debugee after last sync */
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/* agent library initialization */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getphase001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getphase001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getphase001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv *jvmti = NULL;
    jvmtiPhase phase;
    jvmtiCapabilities caps;
    jvmtiEventCallbacks callbacks;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    NSK_DISPLAY0("Agent_OnLoad\n");

    timeout = nsk_jvmti_getWaitTime() * 60000;
    NSK_DISPLAY1("Timeout: %d msc\n", (int)timeout);

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* Create data access lock */
    if (!NSK_JVMTI_VERIFY(
            jvmti->CreateRawMonitor("_access_lock", &access_lock)))
        return JNI_ERR;

    /* testcase #1: check JVMTI_PHASE_ONLOAD */
    NSK_DISPLAY0("Testcase #1: check if GetPhase returns JVMTI_PHASE_ONLOAD\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
        nsk_jvmti_setFailStatus();

    if (!NSK_VERIFY(phase == JVMTI_PHASE_ONLOAD))
        nsk_jvmti_setFailStatus();

    /* add capabilities */
    memset(&caps, 0, sizeof(caps));
    caps.can_generate_all_class_hook_events = 1;
    caps.can_generate_native_method_bind_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.VMStart = &VMStart;
    callbacks.VMInit = &VMInit;
    callbacks.VMDeath = &VMDeath;
    callbacks.NativeMethodBind = &NativeMethodBind;
    callbacks.ClassFileLoadHook = &ClassFileLoadHook;
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    /* enable VMStart event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_START, NULL)))
        return JNI_ERR;

    /* enable VMInit event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL)))
        return JNI_ERR;

    /* enable NativeMethodBind event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_NATIVE_METHOD_BIND, NULL)))
        return JNI_ERR;

    /* enable ClassFileLoadHook event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL)))
        return JNI_ERR;

    /* enable VMDeath event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL)))
        return JNI_ERR;

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

/* agent library shutdown */
JNIEXPORT void JNICALL
#ifdef STATIC_BUILD
Agent_OnUnload_getphase001(JavaVM *jvm)
#else
Agent_OnUnload(JavaVM *jvm)
#endif
{
    jvmtiEnv *jvmti = nsk_jvmti_getAgentJVMTIEnv();
    jvmtiPhase phase;

    NSK_DISPLAY0("Agent_OnUnload\n");

    /* testcase #4: check JVMTI_PHASE_DEAD */
    NSK_DISPLAY0("Testcase #4: check if GetPhase returns JVMTI_PHASE_DEAD\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
        exit(97);

    if (!NSK_VERIFY(phase == JVMTI_PHASE_DEAD))
        exit(97);
}

/* ========================================================================== */

}
