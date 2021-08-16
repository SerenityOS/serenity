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

#include <stdio.h>
#include <stdarg.h>
#include <stdlib.h>
#include <string.h>

#include <jvmti.h>
#include "agent_common.h"

#include "JVMTITools.h"
#include "jvmti_tools.h"
#include "nsk_tools.h"

extern "C" {

/* ====================================================================== */

static const char *classSig =
    "Lnsk/jvmti/scenarios/jni_interception/JI01/ji01t001;";

static jvmtiEnv *jvmti = NULL;
static jrawMonitorID eventLock;
static jvmtiEventCallbacks callbacks;
static jint result = NSK_STATUS_PASSED;

/* the original JNI function table */
static jniNativeInterface *orig_jni_functions = NULL;

/* the redirected JNI function table */
static jniNativeInterface *redir_jni_functions = NULL;

/* number of the redirected JNI function calls */
static volatile int fnd_calls = 0;

/* ====================================================================== */
/** redirected JNI functions **/
jclass JNICALL MyFindClass(JNIEnv *env, const char *name) {
    if (isThreadExpected(jvmti, NULL)) {
        fnd_calls++;

        NSK_DISPLAY1("MyFindClass: the function was called successfully: number of calls so far =  %d\n", fnd_calls);
    }

    return orig_jni_functions->FindClass(env, name);
}

/* ====================================================================== */
static jvmtiPhase getVMPhase(jvmtiEnv *jvmti) {
    jvmtiPhase phase;

    if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase)))
        exit(NSK_STATUS_FAILED);

    return phase;
}

/* ====================================================================== */
static void doRedirect(jvmtiEnv *jvmti, jvmtiPhase phase) {
    jvmtiError err;
    NSK_DISPLAY0("doRedirect: obtaining the JNI function table ...\n");

    // Store original function table
    err = jvmti->GetJNIFunctionTable(&orig_jni_functions);
    if (!NSK_VERIFY((err == JVMTI_ERROR_NONE || phase != JVMTI_PHASE_LIVE)))
    {
        NSK_COMPLAIN2("TEST FAILED: failed to get original JNI function table during %s: %s\n"
                     , TranslatePhase(phase)
                     , TranslateError(err)
                     );

        result = NSK_STATUS_FAILED;
        exit(NSK_STATUS_FAILED);
    }
    else {
        NSK_DISPLAY3("CHECK PASSED: the original JNI function table %s during %s phase: %s\n"
                    , (err == JVMTI_ERROR_NONE) ? "has been obtained" : "hasn't been obtained"
                    , TranslatePhase(phase)
                    , TranslateError(err)
                    );
    }

    // Get a duplicate of the function table for future modification
    if (!NSK_VERIFY(
            (err = jvmti->GetJNIFunctionTable(&redir_jni_functions)) == JVMTI_ERROR_NONE || phase != JVMTI_PHASE_LIVE))
    {
        NSK_COMPLAIN2("TEST FAILED: failed to get JNI function table for interception during %s: %s\n"
                     , TranslatePhase(phase)
                     , TranslateError(err)
                     );

        result = NSK_STATUS_FAILED;
        exit(NSK_STATUS_FAILED);
    }
    else {
        NSK_DISPLAY3("CHECK PASSED: the original JNI function table for interception %s during %s phase: %s\n"
                    , (err == JVMTI_ERROR_NONE) ? "has been obtained" : "hasn't been obtained"
                    , TranslatePhase(phase)
                    , TranslateError(err)
                    );
    }

    // Redefine desired JNI functions
    if (phase == JVMTI_PHASE_LIVE) {
        NSK_DISPLAY0("doRedirect: overwriting the function FindClass; ...\n");
        redir_jni_functions->FindClass = MyFindClass;
    }

    // Set new JNI function table
    if (!NSK_VERIFY(
            (err = jvmti->SetJNIFunctionTable(redir_jni_functions)) == JVMTI_ERROR_NONE || phase != JVMTI_PHASE_LIVE))
    {
        NSK_COMPLAIN2("TEST FAILED: failed to set redirected JNI function table during %s: %s\n"
                     , TranslatePhase(phase)
                     , TranslateError(err)
                     );

        result = NSK_STATUS_FAILED;
        exit(NSK_STATUS_FAILED);
    }
    else {
        NSK_DISPLAY3("CHECK PASSED: the redirected JNI function table %s during %s phase: %s\n"
                    , (err == JVMTI_ERROR_NONE) ? "has been set" : "hasn't been set"
                    , TranslatePhase(phase)
                    , TranslateError(err)
                    );
    }
}

/* ====================================================================== */
static void doRestore(jvmtiEnv *jvmti) {
    NSK_DISPLAY0("doRestore: restoring the original JNI function table ...\n");

    // Set new JNI function table
    if (!NSK_JVMTI_VERIFY(jvmti->SetJNIFunctionTable(orig_jni_functions)))
    {
        NSK_COMPLAIN0("TEST FAILED: failed to restore original JNI function table\n");

        result = NSK_STATUS_FAILED;
        exit(NSK_STATUS_FAILED);
    }

    NSK_DISPLAY0("doRestore: the original JNI function table is restored successfully\n");
}

/* ====================================================================== */
static void lock(jvmtiEnv *jvmti) {
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(eventLock)))
    {
        result = NSK_STATUS_FAILED;
        exit(NSK_STATUS_FAILED);
    }
}

/* ====================================================================== */
static void unlock(jvmtiEnv *jvmti) {
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(eventLock)))
    {
        result = NSK_STATUS_FAILED;
        exit(NSK_STATUS_FAILED);
    }
}

/* ====================================================================== */
static void checkCall(JNIEnv *env
                     , int step
                     , const char *callBackFunc
                     , const char *msg
                     , int exFndCalls
                     )
{
    jclass cls;

    NSK_TRACE(
        (cls = env->FindClass(classSig))
        );

    NSK_TRACE(
        env->ExceptionClear()
        );

    // The check should pass if the actual number of invocations is not less that the expected number (fnd_calls >= exFndCalls).
    // If the invocation is not expected (exFndCalls == 0), fnd_calls should be also == 0.
    if ((exFndCalls > 0 && fnd_calls >= exFndCalls) || (fnd_calls == exFndCalls)) {
            NSK_DISPLAY5("CHECK PASSED: %s: the %s JNI function FindClass() has been %s during %s phase\n\t%d intercepted call(s) as expected\n"
                        , callBackFunc
                        , (step == 1) ? "tested" : "original"
                        , (step == 1) ? "redirected" : "restored"
                        , msg
                        , fnd_calls
                        );

            if (fnd_calls != exFndCalls) {
                NSK_COMPLAIN2("WARNING: the number of occured calls (%d) exceeds the expected number of calls (%d).\n"
                             , fnd_calls
                             , exFndCalls
                             );
            }
    } else {
        result = NSK_STATUS_FAILED;

        NSK_COMPLAIN6("TEST FAILED: %s: the %s JNI function FindClass() has not been %s during %s phase\n\t%d intercepted call(s) instead of %d as expected\n"
                     , callBackFunc
                     , (step == 1) ? "tested" : "original"
                     , (step == 1) ? "redirected" : "restored"
                     , msg
                     , fnd_calls
                     , exFndCalls
                     );
    }
}

/* ====================================================================== */
// callback functions
void JNICALL
VMInit(jvmtiEnv *jvmti, JNIEnv *env, jthread thread) {
    jvmtiPhase phase = getVMPhase(jvmti);

    NSK_TRACE(lock(jvmti));

    NSK_DISPLAY1("b) VMInit: the current phase of VM execution %s\n"
                , TranslatePhase(phase)
                );

    // check JNI function table interception
    fnd_calls = 0;
    NSK_TRACE(doRedirect(jvmti, phase));
    NSK_TRACE(checkCall(env, 1, "VMInit", TranslatePhase(phase), 1));

    // check restored JNI function table
    fnd_calls = 0;
    NSK_TRACE(doRestore(jvmti));
    NSK_TRACE(checkCall(env, 2, "VMInit", TranslatePhase(phase), 0));

    NSK_TRACE(unlock(jvmti));
}

/* ====================================================================== */
void JNICALL
VMDeath(jvmtiEnv *jvmti, JNIEnv *env) {
    jvmtiPhase phase = getVMPhase(jvmti);

    NSK_TRACE(lock(jvmti));

    NSK_DISPLAY1("c) VMDeath: the current phase of VM execution %s\n"
                , TranslatePhase(phase)
                );

    // check JNI function table interception
    fnd_calls = 0;
    NSK_TRACE(doRedirect(jvmti, phase));
    NSK_TRACE(checkCall(env, 1, "VMDeath", TranslatePhase(phase), 1));

    // check restored JNI function table
    fnd_calls = 0;
    NSK_TRACE(doRestore(jvmti));
    NSK_TRACE(checkCall(env, 2, "VMDeath", TranslatePhase(phase), 0));

    (void) memset(&callbacks, 0, sizeof(callbacks));

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        result = NSK_STATUS_FAILED;

    NSK_TRACE(unlock(jvmti));

    if (result == NSK_STATUS_FAILED) {
        exit(NSK_STATUS_FAILED);
    }
}

/* ====================================================================== */
JNIEXPORT jint JNICALL
Java_nsk_jvmti_scenarios_jni_1interception_JI01_ji01t001_check(JNIEnv *env, jobject obj) {
    return result;
}

/* ====================================================================== */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ji01t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ji01t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ji01t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    if (!NSK_VERIFY(
                nsk_jvmti_parseOptions(options)
                )
       )
        return JNI_ERR;


    if (!NSK_VERIFY(jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1) == JNI_OK && jvmti != NULL))
        return JNI_ERR;


    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("_event_lock", &eventLock)))
        return JNI_ERR;

    NSK_DISPLAY1("a) Trying to intercept JNI functions during %s phase ...\n"
                , TranslatePhase(getVMPhase(jvmti))
                );

    NSK_TRACE(doRedirect(jvmti, getVMPhase(jvmti)));

    NSK_DISPLAY0("Setting event callbacks...\n");

    (void) memset(&callbacks, 0, sizeof(callbacks));
    callbacks.VMInit = &VMInit;
    callbacks.VMDeath = &VMDeath;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;


    NSK_DISPLAY0("Event callbacks are set\nEnabling events...\n");

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL)))
        return JNI_ERR;


    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL)))
        return JNI_ERR;

    NSK_DISPLAY0("Events are enabled\n");

    return JNI_OK;
}

}
