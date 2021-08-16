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

typedef struct {
    const char *cls;
    const char *name;
    const char *sig;
    jlocation loc;
} frame_info;

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jmethodID midD = NULL;
static jmethodID midRun = NULL;
static int framesExpected = 0;
static int framesCount = 0;
static frame_info frames[] = {
    { "Lnsk/jvmti/PopFrame/popframe008$TestThread;", "C", "()V", 1 },
    { "Lnsk/jvmti/PopFrame/popframe008$TestThread;", "B", "()V", 1 },
    { "Lnsk/jvmti/PopFrame/popframe008$TestThread;", "A", "()V", 1 },
    { "Lnsk/jvmti/PopFrame/popframe008$TestThread;", "run", "()V", 1 }
};

void check(jvmtiEnv *jvmti_env, jmethodID mid, jlocation loc, int i) {
    jvmtiError err;
    jclass cls;
    char *sigClass, *name, *sig, *generic;

    err = jvmti_env->GetMethodDeclaringClass(mid, &cls);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodDeclaringClass#%d) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti_env->GetClassSignature(cls, &sigClass, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassSignature#%d) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti_env->GetMethodName(mid, &name, &sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodName#%d) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (sigClass == NULL || strcmp(sigClass, frames[i].cls) != 0) {
        printf("(%d) wrong class sig: \"%s\",\n", i, sigClass);
        printf(" expected: \"%s\"\n", frames[i].cls);
        result = STATUS_FAILED;
    }
    if (name == NULL || strcmp(name, frames[i].name) != 0) {
        printf("(%d) wrong method name: \"%s\",", i, name);
        printf(" expected: \"%s\"\n", frames[i].name);
        result = STATUS_FAILED;
    }
    if (sig == NULL || strcmp(sig, frames[i].sig) != 0) {
        printf("(%d) wrong method sig: \"%s\",", i, sig);
        printf(" expected: \"%s\"\n", frames[i].sig);
        result = STATUS_FAILED;
    }
    if (loc != frames[i].loc) {
        printf("(%d) wrong location: 0x%x%08x,",
               i, (jint)(loc >> 32), (jint)loc);
        printf(" expected: 0x%x\n", (jint)frames[i].loc);
        result = STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> \"%s.%s%s\"", sigClass, name, sig);
        printf(", location: 0x%x%08x\n", (jint)(loc >> 32), (jint)loc);
    }
}


void JNICALL Breakpoint(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jlocation location) {
    jvmtiError err;

    if (method != midD) {
        printf("bp: don't know where we get called from");
        result = STATUS_FAILED;
        return;
    }
    if (printdump == JNI_TRUE) {
        printf(">>> breakpoint in D\n");
    }
    err = jvmti_env->ClearBreakpoint(midD, 0);
    if (err != JVMTI_ERROR_NONE) {
        printf("(ClearBreakpoint) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (caps.can_pop_frame) {
        framesExpected = 4;
        err = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_SINGLE_STEP, thread);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot enable single step: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }

        err = jvmti_env->PopFrame(thread);
        if (err != JVMTI_ERROR_NONE) {
            printf("(PopFrame) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }
}

void JNICALL SingleStep(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jlocation location) {
    jvmtiError err;

    check(jvmti_env, method, location, framesCount);
    framesCount++;

    if (method == midRun) {
        if (printdump == JNI_TRUE) {
            printf(">>> poped %d frames till method \"run()\"\n",
                   framesCount);
        }
        err = jvmti_env->SetEventNotificationMode(JVMTI_DISABLE,
            JVMTI_EVENT_SINGLE_STEP, thread);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot disable single step: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
    } else {
        err = jvmti_env->PopFrame(thread);
        if (err != JVMTI_ERROR_NONE) {
            printf("(PopFrame) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_popframe008(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_popframe008(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_popframe008(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiError err;
    jint res;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv!\n");
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

    if (!caps.can_pop_frame) {
        printf("Warning: PopFrame is not implemented\n");
    }

    if (caps.can_generate_breakpoint_events &&
            caps.can_generate_single_step_events) {
        callbacks.Breakpoint = &Breakpoint;
        callbacks.SingleStep = &SingleStep;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: Breakpoint or SingleStep event are not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_PopFrame_popframe008_getReady(JNIEnv *env,
        jclass cls, jthread thr) {
    jvmtiError err;
    jclass clazz;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (!caps.can_pop_frame ||
            !caps.can_generate_breakpoint_events ||
            !caps.can_generate_single_step_events) {
        return;
    }

    clazz = env->GetObjectClass(thr);
    if (clazz == NULL) {
        printf("Cannot get class of the thread object\n");
        result = STATUS_FAILED;
        return;
    }

    midD = env->GetMethodID(clazz, "D", "()V");
    if (midD == NULL) {
        printf("Cannot get Method ID for method \"D\"\n");
        result = STATUS_FAILED;
        return;
    }

    midRun = env->GetMethodID(clazz, "run", "()V");
    if (midRun == NULL) {
        printf("Cannot get Method ID for method \"run\"\n");
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->SetBreakpoint(midD, 0);
    if (err != JVMTI_ERROR_NONE) {
        printf("(SetBreakpoint) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_BREAKPOINT, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable BREAKPOINT event: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_PopFrame_popframe008_getRes(JNIEnv *env, jclass cls) {
    if (framesCount != framesExpected) {
        printf("Wrong number of poped frames: %d, expected: %d\n",
            framesCount, framesExpected);
        result = STATUS_FAILED;
    }
    return result;
}

}
