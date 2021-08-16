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

#define STATUS_FAILED 2
#define PASSED 0

/* tested method */
static const char *METHODS[] = {
    "nativeMethod", "()V"
};

/* event counter for the tested method and expected number
   of the events */
static volatile int bindEv[] = {
    0, 1
};

static const char *CLASS_SIG =
    "Lnsk/jvmti/NativeMethodBind/nativemethbind003$TestedClass;";

static volatile jint result = PASSED;
static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jrawMonitorID countLock;

static void lock(jvmtiEnv *jvmti_env, JNIEnv *jni_env) {
    if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorEnter(countLock)))
        jni_env->FatalError("failed to enter a raw monitor\n");
}

static void unlock(jvmtiEnv *jvmti_env, JNIEnv *jni_env) {
    if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorExit(countLock)))
        jni_env->FatalError("failed to exit a raw monitor\n");
}

/** callback functions **/
void JNICALL
NativeMethodBind(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
        jmethodID method, void *addr, void **new_addr) {
    jvmtiPhase phase;
    char *methNam, *methSig;

    lock(jvmti_env, jni_env);

    NSK_DISPLAY0(">>>> NativeMethodBind event received\n");

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetPhase(&phase))) {
        result = STATUS_FAILED;
        unlock(jvmti_env, jni_env);
        return;
    }

    if (phase != JVMTI_PHASE_LIVE && phase != JVMTI_PHASE_START) {
        unlock(jvmti_env, jni_env);
        return;
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &methNam, &methSig, NULL))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILED: unable to get method name during NativeMethodBind callback\n\n");
        unlock(jvmti_env, jni_env);
        return;
    }

    if ((strcmp(methNam,METHODS[0]) == 0) &&
            (strcmp(methSig,METHODS[1]) == 0)) {
        bindEv[0]++;

        NSK_DISPLAY2("\tmethod: \"%s %s\"\n", methNam, methSig);
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*) methNam))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILED: unable to deallocate memory storing method name\n\n");
    }
    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*) methSig))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILED: unable to deallocate memory storing method signature\n\n");
    }

    NSK_DISPLAY0("<<<<\n\n");

    unlock(jvmti_env, jni_env);
}

void JNICALL
VMDeath(jvmtiEnv *jvmti_env, JNIEnv *env) {
    NSK_DISPLAY0("VMDeath event received\n");

    if (bindEv[0] != bindEv[1]) {
        result = STATUS_FAILED;
        NSK_COMPLAIN5(
            "TEST FAILED: wrong NativeMethodBind events\n"
            "\tfor tested method \"%s %s\" bound with \"%s\":\n"
            "\tgot: %d\texpected: %d\n\n",
            METHODS[0], METHODS[1], CLASS_SIG, bindEv[0], bindEv[1]);
    } else {
        NSK_DISPLAY4(
            "CHECK PASSED: %d NativeMethodBind event(s)\n"
            "\tfor tested method \"%s %s\" bound with \"%s\"\n"
            "\tas expected\n",
            bindEv[0], METHODS[0], METHODS[1], CLASS_SIG);
    }

    if (result == STATUS_FAILED)
        exit(95 + STATUS_FAILED);
}
/************************/

/* dummy method used only to provoke NativeMethodBind event */
static void JNICALL
nativeMethod(JNIEnv *env, jobject obj) {
    NSK_DISPLAY0("inside the nativeMethod()\n");
}

/* dummy method used only to provoke NativeMethodBind event */
JNIEXPORT void JNICALL
Java_nsk_jvmti_NativeMethodBind_nativemethbind003_registerNative(
        JNIEnv *env, jobject obj) {
    jclass testedCls = NULL;
    JNINativeMethod meth;

    NSK_DISPLAY1("Inside the registerNative()\n"
                 "Finding class \"%s\" ...\n",
                 CLASS_SIG);
    if (!NSK_JNI_VERIFY(env, (testedCls = env->FindClass(CLASS_SIG)) != NULL)) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILURE: unable to find class \"%s\"\n\n",
            CLASS_SIG);
        return;
    }

    meth.name = (char*) METHODS[0];
    meth.signature = (char*) METHODS[1];
    meth.fnPtr = (void*) nativeMethod;

    NSK_DISPLAY3(
        "Calling RegisterNatives() with \"%s %s\"\n"
        "\tfor class \"%s\" ...\n",
        METHODS[0], METHODS[1], CLASS_SIG);
    if (!NSK_JNI_VERIFY_VOID(env, (env->RegisterNatives(testedCls, &meth, 1)) != 0)) {
        result = STATUS_FAILED;
        NSK_COMPLAIN3("TEST FAILURE: unable to RegisterNatives() \"%s %s\" for class \"%s\"\n\n",
            METHODS[0], METHODS[1], CLASS_SIG);
    }

    NSK_DISPLAY1("Calling UnregisterNatives() for class \"%s\" ...\n",
        CLASS_SIG);
    if (!NSK_JNI_VERIFY_VOID(env, (env->UnregisterNatives(testedCls)) != 0)) {
        result = STATUS_FAILED;
        NSK_COMPLAIN3("TEST FAILURE: unable to UnregisterNatives() \"%s %s\" for class \"%s\"\n\n",
            METHODS[1][0], METHODS[1][1], CLASS_SIG);
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_nativemethbind003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_nativemethbind003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_nativemethbind003(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiCapabilities caps;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* create a raw monitor */
    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("_counter_lock", &countLock)))
        return JNI_ERR;

    /* add capability to generate compiled method events */
    memset(&caps, 0, sizeof(jvmtiCapabilities));
    caps.can_generate_native_method_bind_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;
    if (!caps.can_generate_native_method_bind_events)
        NSK_DISPLAY0("Warning: generation of native method bind events is not implemented\n");

    /* set event callback */
    NSK_DISPLAY0("setting event callbacks ...\n");
    (void) memset(&callbacks, 0, sizeof(callbacks));
    callbacks.NativeMethodBind = &NativeMethodBind;
    callbacks.VMDeath = &VMDeath;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    NSK_DISPLAY0("setting event callbacks done\nenabling JVMTI events ...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                                          JVMTI_EVENT_NATIVE_METHOD_BIND,
                                                          NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                                          JVMTI_EVENT_VM_DEATH,
                                                          NULL)))
        return JNI_ERR;
    NSK_DISPLAY0("enabling the events done\n\n");

    return JNI_OK;
}

}
