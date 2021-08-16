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
static const char *METHOD[] = {
    "nativeMethod", "()V"
};

/* counter for the original method calls */
static volatile int origCalls = 0;
/* counter for the redirected method calls */
static volatile int redirCalls = 0;

static volatile jint result = PASSED;
static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jrawMonitorID countLock;

/* method to be redirected used to check the native method redirection
   through the NativeMethodBind event */
JNIEXPORT void JNICALL
Java_nsk_jvmti_NativeMethodBind_nativemethbind004_nativeMethod(
        JNIEnv *env, jobject obj) {
    origCalls++;
    NSK_DISPLAY1("inside the nativeMethod(): calls=%d\n",
        origCalls);
}

/* redirected method used to check the native method redirection
   through the NativeMethodBind event */
static void JNICALL
redirNativeMethod(JNIEnv *env, jobject obj) {
    redirCalls++;
    NSK_DISPLAY1("inside the redirNativeMethod(): calls=%d\n",
        redirCalls);
}

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

    if ((strcmp(methNam,METHOD[0]) == 0) &&
            (strcmp(methSig,METHOD[1]) == 0)) {
        NSK_DISPLAY4("\tmethod: \"%s %s\"\nRedirecting the method address from 0x%p to 0x%p ...\n",
            methNam, methSig, addr, (void*) redirNativeMethod);

        *new_addr = (void*) redirNativeMethod;
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
/************************/

JNIEXPORT jint JNICALL
Java_nsk_jvmti_NativeMethodBind_nativemethbind004_check(
        JNIEnv *env, jobject obj) {

    if (origCalls == 0) {
        NSK_DISPLAY0(
            "CHECK PASSED: original nativeMethod() to be redirected\n"
            "\thas not been invoked as expected\n");
    } else {
        result = STATUS_FAILED;
        NSK_COMPLAIN1(
            "TEST FAILED: nativeMethod() has not been redirected by the NativeMethodBind:\n"
            "\t%d calls\texpected: 0\n\n",
            origCalls);
    }

    if (redirCalls == 1) {
        NSK_DISPLAY1(
            "CHECK PASSED: nativeMethod() has been redirected by the NativeMethodBind:\n"
            "\t%d calls of redirected method as expected\n",
            redirCalls);
    } else {
        result = STATUS_FAILED;
        NSK_COMPLAIN1(
            "TEST FAILED: nativeMethod() has not been redirected by the NativeMethodBind:\n"
            "\t%d calls of redirected method\texpected: 1\n\n",
            redirCalls);
    }

    return result;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_nativemethbind004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_nativemethbind004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_nativemethbind004(JavaVM *jvm, char *options, void *reserved) {
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
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    NSK_DISPLAY0("setting event callbacks done\nenabling JVMTI events ...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                                          JVMTI_EVENT_NATIVE_METHOD_BIND,
                                                          NULL)))
        return JNI_ERR;
    NSK_DISPLAY0("enabling the events done\n\n");

    return JNI_OK;
}

}
