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
} frame_info;

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jmethodID mid;
static frame_info frames[] = {
    { "Lnsk/jvmti/GetStackTrace/getstacktr004$TestThread;", "checkPoint", "()V" },
    { "Lnsk/jvmti/GetStackTrace/getstacktr004$TestThread;", "chain4", "()V" },
    { "Lnsk/jvmti/GetStackTrace/getstacktr004$TestThread;", "chain3", "()V" },
    { "Lnsk/jvmti/GetStackTrace/getstacktr004$TestThread;", "chain2", "()V" },
    { "Lnsk/jvmti/GetStackTrace/getstacktr004$TestThread;", "chain1", "()V" },
    { "Lnsk/jvmti/GetStackTrace/getstacktr004$TestThread;", "run", "()V" },
};

#define NUMBER_OF_STACK_FRAMES ((int) (sizeof(frames)/sizeof(frame_info)))

void check(jvmtiEnv *jvmti_env, jthread thr) {
    jvmtiError err;
    jvmtiFrameInfo f[NUMBER_OF_STACK_FRAMES + 1];
    jclass callerClass;
    char *sigClass, *name, *sig, *generic;
    jint i, count;

    err = jvmti_env->GetStackTrace(thr,
        0, NUMBER_OF_STACK_FRAMES + 1, f, &count);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetStackTrace) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    if (count != NUMBER_OF_STACK_FRAMES) {
        printf("Wrong frame count, expected: %d, actual: %d\n",
               NUMBER_OF_STACK_FRAMES, count);
        result = STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
            printf(">>>   frame count: %d\n", count);
    }
    for (i = 0; i < count; i++) {
        if (printdump == JNI_TRUE) {
            printf(">>> checking frame#%d ...\n", i);
        }
        err = jvmti_env->GetMethodDeclaringClass(f[i].method,
            &callerClass);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetMethodDeclaringClass#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
            continue;
        }
        err = jvmti_env->GetClassSignature(callerClass,
            &sigClass, &generic);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetClassSignature#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
            continue;
        }
        err = jvmti_env->GetMethodName(f[i].method,
            &name, &sig, &generic);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetMethodName#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
            continue;
        }
        if (printdump == JNI_TRUE) {
            printf(">>>   class:  \"%s\"\n", sigClass);
            printf(">>>   method: \"%s%s\"\n", name, sig);
        }
        if (i < NUMBER_OF_STACK_FRAMES) {
            if (sigClass == NULL || strcmp(sigClass, frames[i].cls) != 0) {
                printf("(frame#%d) wrong class sig: \"%s\", expected: \"%s\"\n",
                       i, sigClass, frames[i].cls);
                result = STATUS_FAILED;
            }
            if (name == NULL || strcmp(name, frames[i].name) != 0) {
                printf("(frame#%d) wrong method name: \"%s\", expected: \"%s\"\n",
                       i, name, frames[i].name);
                result = STATUS_FAILED;
            }
            if (sig == NULL || strcmp(sig, frames[i].sig) != 0) {
                printf("(frame#%d) wrong method sig: \"%s\", expected: \"%s\"\n",
                       i, sig, frames[i].sig);
                result = STATUS_FAILED;
            }
        }
    }
}

void JNICALL Breakpoint(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method, jlocation location) {
    if (mid != method) {
        printf("ERROR: didn't know where we got called from");
        result = STATUS_FAILED;
        return;
    }
    check(jvmti_env, thr);
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getstacktr004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getstacktr004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getstacktr004(JavaVM *jvm, char *options, void *reserved) {
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

    if (caps.can_generate_breakpoint_events) {
        callbacks.Breakpoint = &Breakpoint;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: Breakpoint is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetStackTrace_getstacktr004_getReady(JNIEnv *env, jclass cls, jclass clazz) {
    jvmtiError err;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (!caps.can_generate_breakpoint_events) {
        return;
    }

    mid = env->GetMethodID(clazz, "checkPoint", "()V");
    if (mid == NULL) {
        printf("Cannot find Method ID for method checkPoint\n");
        result = STATUS_FAILED;
        return;
    }
    err = jvmti->SetBreakpoint(mid, 0);
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
Java_nsk_jvmti_GetStackTrace_getstacktr004_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
