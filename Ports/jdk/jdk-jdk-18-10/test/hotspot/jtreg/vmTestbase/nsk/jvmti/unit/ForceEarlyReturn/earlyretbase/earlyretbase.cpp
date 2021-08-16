/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include <string.h>
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define STATUS_FAILED 2
#define PASSED 0

#define RETURN_FAILED errCode = STATUS_FAILED; fflush(0); return errCode

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;

/* MethodExit and PopFrame events expected */
static int meth_exit_exp_events = 0;
static int pop_frame_exp_events = 0;

/* MethodExit and PopFrame events generated */
static int meth_exit_gen_events = 0;
static int pop_frame_gen_events = 0;

static int errCode = PASSED;

static const char *sig_exp       = "()J";
static const char *name_exp      = "activeMethod";
static jmethodID midActiveMethod = NULL;

void JNICALL
MethodExit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr,
        jmethodID method, jboolean was_poped_by_exc, jvalue return_value) {

    if (method == midActiveMethod) {
        printf("#### MethodExit event occurred ####\n");
        fflush(0);
        meth_exit_gen_events++;
    }
}

void JNICALL
FramePop(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread,
        jmethodID method, jboolean wasPopedByException) {

    if (method == midActiveMethod) {
        printf("#### FramePop event occurred ####\n");
        fflush(0);
        pop_frame_gen_events++;
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_ForceEarlyReturn_earlyretbase_suspThread(JNIEnv *env,
        jclass cls, jobject earlyretThr) {

    if (!caps.can_force_early_return || !caps.can_suspend) {
        return PASSED;
    }

    jclass clazz = env->GetObjectClass(earlyretThr);
    if (clazz == NULL) {
        printf("Cannot get class of thread object\n");
        RETURN_FAILED;
    }

    midActiveMethod = env->GetMethodID(clazz, name_exp, sig_exp);
    if (midActiveMethod == NULL) {
        printf("Cannot find Method ID for method %s\n", name_exp);
        RETURN_FAILED;
    }

    int result = suspendThreadAtMethod(jvmti, cls, earlyretThr, midActiveMethod);
    if (result) {
        return PASSED;
    } else {
        RETURN_FAILED;
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_ForceEarlyReturn_earlyretbase_resThread(JNIEnv *env,
        jclass cls, jobject earlyretThr) {
    jvmtiError err;

    if (!caps.can_force_early_return || !caps.can_suspend) {
        return PASSED;
    }

    printf(">>>>>>>> Invoke ResumeThread()\n");
    err = jvmti->ResumeThread(earlyretThr);
    if (err != JVMTI_ERROR_NONE) {
        printf("%s: Failed to call ResumeThread(): error=%d: %s\n",
            __FILE__, err, TranslateError(err));
        return JNI_ERR;
    }
    printf("<<<<<<<< ResumeThread() is successfully done\n");
    fflush(0);
    return PASSED;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_ForceEarlyReturn_earlyretbase_doForceEarlyReturn(JNIEnv *env,
        jclass cls, jthread earlyretThr, jlong valToRet) {
    jvmtiError err;

    if (!caps.can_force_early_return || !caps.can_suspend) {
        return PASSED;
    }

    /* Turn on the JVMTI MetodExit and PopFrame events to check
     * that ForceEarlyReturn correctly generates them */

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_METHOD_EXIT, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable METHOD_EXIT event: %s (%d)\n",
               TranslateError(err), err);
        RETURN_FAILED;
    } else {
        meth_exit_exp_events++;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_FRAME_POP, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable FRAME_POP event: %s (%d)\n",
               TranslateError(err), err);
        RETURN_FAILED;
    }

    err = jvmti->NotifyFramePop(earlyretThr, 0);
    if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY &&
               !caps.can_generate_frame_pop_events) {
        /* Ok, it's expected */
    } else if (err != JVMTI_ERROR_NONE) {
        printf("(NotifyFramePop) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        RETURN_FAILED;
    } else {
        pop_frame_exp_events++;
    }
    printf(">>>>>>>> Invoke ForceEarlyReturn()\n");

    err = jvmti->ForceEarlyReturnLong(earlyretThr, valToRet);
    if (err != JVMTI_ERROR_NONE) {
        printf("TEST FAILED: the function ForceEarlyReturn()"
               " returned the error %d: %s\n",
               err, TranslateError(err));
        printf("\tFor more info about this error see the JVMTI spec.\n");
        RETURN_FAILED;
    }
    printf("Check #1 PASSED: ForceEarlyReturn() is successfully done\n");
    fflush(0);

    return(errCode);
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_earlyretbase(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_earlyretbase(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_earlyretbase(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong error code from a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    err = jvmti->GetPotentialCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetPotentialCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->AddCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(AddCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->GetCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    if (!caps.can_force_early_return) {
        printf("Warning: ForceEarlyReturn is not implemented\n");
        return JNI_OK;
    }

    if (!caps.can_suspend) {
        printf("Warning: suspend/resume is not implemented\n");
        return JNI_OK;
    }

    if (caps.can_generate_frame_pop_events && caps.can_generate_method_exit_events) {
        callbacks.MethodExit = &MethodExit;
        callbacks.FramePop   = &FramePop;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: FramePop or MethodExit event is not implemented\n");
    }
    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_ForceEarlyReturn_earlyretbase_check(JNIEnv *env, jclass cls) {

    printf("JVMTI  PopFrame  events: expected: %d, generated: %d\n",
            meth_exit_exp_events, meth_exit_gen_events);

    printf("JVMTI MethodExit events: expected: %d, generated: %d\n",
            pop_frame_exp_events, pop_frame_gen_events);

    /* Check if the JVMTI events were generated correctly by ForceEarlyReturn */
    if (meth_exit_exp_events != meth_exit_gen_events ||
        pop_frame_exp_events != pop_frame_gen_events) {
        printf("TEST FAILED: JVMTI MethodExit or PopFrame events "
               "generated incorrectly\n");
        errCode = STATUS_FAILED;
    } else {
        printf("Check #2 PASSED: JVMTI MethodExit and PopFrame "
               "events generated correctly\n");
        errCode = PASSED;
    }
    fflush(0);
    return errCode;
}

}
