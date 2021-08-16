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
#include <string.h>
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jmethodID mid1;

// If mustPass is false we just check if we have reached the correct instruction location.
// This is used to wait for the child thread to reach the expected position.
jboolean checkFrame(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID exp_mid, jlocation exp_loc, jlocation exp_loc_alternative, jboolean mustPass) {
    jvmtiError err;
    jmethodID mid = NULL;
    jlocation loc = -1;
    char *meth, *sig, *generic;
    jboolean isOk = JNI_FALSE;

    err = jvmti_env->GetMethodName(exp_mid, &meth, &sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodName) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti_env->GetFrameLocation(thr, 0, &mid, &loc);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetFrameLocation#%s) unexpected error: %s (%d)\n",
               meth, TranslateError(err), err);
        result = STATUS_FAILED;
    } else {
        if (exp_mid != mid) {
            printf("Method \"%s\" current frame's method ID", meth);
            printf(" expected: 0x%p, got: 0x%p\n", exp_mid, mid);
            result = STATUS_FAILED;
        }
        isOk = exp_loc == loc || exp_loc_alternative == loc;
        if (!isOk && mustPass) {
            printf("Method \"%s\" current frame's location", meth);
            printf(" expected: 0x%x or 0x%x, got: 0x%x%08x\n",
                   (jint)exp_loc, (jint)exp_loc_alternative, (jint)(loc >> 32), (jint)loc);
            result = STATUS_FAILED;
        }
    }
    return isOk && result == PASSED;
}

void JNICALL
ExceptionCatch(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr,
        jmethodID method, jlocation location, jobject exception) {
    if (method == mid1) {
      checkFrame(jvmti_env, (JNIEnv *)env, thr, method, location, location, JNI_TRUE);
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_frameloc001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_frameloc001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_frameloc001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv !\n");
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

    if (!caps.can_suspend) {
        printf("Warning: suspend/resume is not implemented\n");
    }

    if (caps.can_generate_exception_events) {
        callbacks.ExceptionCatch = &ExceptionCatch;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: ExceptionCatch event is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetFrameLocation_frameloc001_getReady(JNIEnv *env, jclass cls,
        jclass klass) {
    jvmtiError err;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (!caps.can_generate_exception_events) {
        return;
    }

    mid1 = env->GetMethodID(klass, "meth01", "(I)V");
    if (mid1 == NULL) {
        printf("Cannot get jmethodID for method \"meth01\"\n");
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_EXCEPTION_CATCH, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("(SetEventNotificationMode) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_GetFrameLocation_frameloc001_checkFrame01(JNIEnv *env,
        jclass cls, jthread thr, jclass klass, jboolean mustPass) {
    jvmtiError err;
    jmethodID mid;
    jboolean isOk = JNI_FALSE;

    if (jvmti == NULL || !caps.can_suspend) {
        return JNI_TRUE;
    }

    mid = env->GetMethodID(klass, "run", "()V");
    if (mid == NULL) {
        printf("Cannot get jmethodID for method \"run\"\n");
        result = STATUS_FAILED;
        return JNI_TRUE;
    }

    err = jvmti->SuspendThread(thr);
    if (err != JVMTI_ERROR_NONE) {
        printf("(SuspendThread) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    // This tests the location of a throw/catch statement.
    // The returned location may be either the throw or the catch statement.
    // It seems like the throw statement is returned in compiled code (-Xcomp),
    // but the catch statement is returned in interpreted code.
    // Both locations are valid.
    // See bug JDK-4527281.
    isOk = checkFrame(jvmti, env, thr, mid, 31, 32, mustPass);

    err = jvmti->ResumeThread(thr);
    if (err != JVMTI_ERROR_NONE) {
        printf("(ResumeThread) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    return isOk && result == PASSED;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetFrameLocation_frameloc001_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
