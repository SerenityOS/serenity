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
#define WAIT_FOREVER ((jlong)(3600*1000))

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jrawMonitorID breakpointLock = NULL;
static jrawMonitorID popFrameLock = NULL;
static jrawMonitorID suspendLock = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jboolean popDone = JNI_FALSE;
static jmethodID midCheckPoint = NULL;
static jmethodID midRun = NULL;
static int bpCount = 0;
static int framesCount = 0;

void JNICALL Breakpoint(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jlocation location) {
    jvmtiError err;

    if (midCheckPoint != method) {
        printf("bp: don't know where we get called from");
        result = STATUS_FAILED;
        return;
    }

    err = jvmti_env->RawMonitorEnter(breakpointLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("step: Cannot enter breakpointLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    bpCount++;

    if (printdump == JNI_TRUE) {
        printf(">>> breakpoint %d\n", bpCount);
    }

    err = jvmti_env->RawMonitorWait(breakpointLock, WAIT_FOREVER);
    if (err != JVMTI_ERROR_NONE) {
        printf("step: Cannot wait breakpointLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (bpCount == 2) {
        err = jvmti_env->ClearBreakpoint(midCheckPoint, 0);
        if (err != JVMTI_ERROR_NONE) {
            printf("(ClearBreakpoint) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }

    err = jvmti_env->RawMonitorExit(breakpointLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("step: Cannot exit breakpointLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

void JNICALL SingleStep(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jlocation location) {
    jvmtiError err;
    framesCount++;

    if (method == midRun) {
        popDone = JNI_TRUE;
        if (printdump == JNI_TRUE) {
            printf(">>> poped %d frames till \"run()\"\n", framesCount);
        }
    }

    err = jvmti_env->RawMonitorEnter(suspendLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("step: Cannot enter suspendLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti_env->RawMonitorEnter(popFrameLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("step: Cannot enter popFrameLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti_env->RawMonitorNotify(popFrameLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("step: Cannot notify popFrameLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti_env->RawMonitorExit(popFrameLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("step: Cannot exit popFrameLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti_env->RawMonitorWait(suspendLock, WAIT_FOREVER);
    if (err != JVMTI_ERROR_NONE) {
        printf("step: Cannot wait suspendLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti_env->RawMonitorExit(suspendLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("step: Cannot exit suspendLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_popframe009(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_popframe009(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_popframe009(JavaVM *jvm, char *options, void *reserved) {
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

    if (!caps.can_suspend) {
        printf("Warning: suspend/resume is not implemented\n");
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
Java_nsk_jvmti_PopFrame_popframe009_getReady(JNIEnv *env, jclass cls) {
    jvmtiError err;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (!caps.can_pop_frame || !caps.can_suspend ||
            !caps.can_generate_breakpoint_events ||
            !caps.can_generate_single_step_events) {
        return;
    }

    midCheckPoint = env->GetStaticMethodID(cls, "checkPoint", "()V");
    if (midCheckPoint == NULL) {
        printf("Cannot find Method ID for method checkPoint\n");
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->CreateRawMonitor("Breakpoint Lock", &breakpointLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("Cannot create breakpointLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->SetBreakpoint(midCheckPoint, 0);
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

void popFrames(jthread thr) {
    jvmtiError err;

    while (popDone != JNI_TRUE && result != STATUS_FAILED) {
        err = jvmti->PopFrame(thr);
        if (err != JVMTI_ERROR_NONE) {
            printf("(PopFrame) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
            break;
        }

        err = jvmti->ResumeThread(thr);
        if (err != JVMTI_ERROR_NONE) {
            printf("(ResumeThread) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
            break;
        }

        err = jvmti->RawMonitorWait(popFrameLock, WAIT_FOREVER);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot wait popFrameLock: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }

        err = jvmti->RawMonitorEnter(suspendLock);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot enter suspendLock: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }

        err = jvmti->SuspendThread(thr);
        if (err != JVMTI_ERROR_NONE) {
            printf("(SuspendThread) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }

        err = jvmti->RawMonitorNotify(suspendLock);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot notify suspendLock: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }

        err = jvmti->RawMonitorExit(suspendLock);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot exit suspendLock: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_PopFrame_popframe009_check(JNIEnv *env, jclass cls, jthread thr) {
    jvmtiError err;
    jmethodID midFibonacci;
    jclass clazz;
    jmethodID method = NULL;
    jlocation loc;
    char *name, *sig, *generic;
    jlong delayTime = 1;
    int popCount = 0;
    int i;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    if (!caps.can_pop_frame || !caps.can_suspend ||
            !caps.can_generate_breakpoint_events ||
            !caps.can_generate_single_step_events) {
        return result;
    }

    err = jvmti->CreateRawMonitor("Pop Frame Lock", &popFrameLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("Cannot create popFrameLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> waiting breakpoint 1\n");
    }

    while (bpCount < 1) {
        err = jvmti->RawMonitorEnter(popFrameLock);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot enter popFrameLock: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }

        err = jvmti->RawMonitorWait(popFrameLock, (jlong)delayTime);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot wait popFrameLock: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }

        err = jvmti->RawMonitorExit(popFrameLock);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot exit popFrameLock: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }

    err = jvmti->RawMonitorEnter(breakpointLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("Cannot enter breakpointLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti->RawMonitorNotify(breakpointLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("Cannot notify breakpointLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti->SuspendThread(thr);
    if (err != JVMTI_ERROR_NONE) {
        printf("(SuspendThread) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return STATUS_FAILED;
    }

    err = jvmti->RawMonitorExit(breakpointLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("Cannot exit breakpointLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    midFibonacci = env->GetStaticMethodID(cls, "fibonacci", "(I)I");
    if (midFibonacci == NULL) {
        printf("Cannot get method ID for method \"fibonacci\"\n");
        result = STATUS_FAILED;
    }

    clazz = env->GetObjectClass(thr);
    if (clazz == NULL) {
        printf("Cannot get class of thread object\n");
        return STATUS_FAILED;
    }

    midRun = env->GetMethodID(clazz, "run", "()V");
    if (midRun == NULL) {
        printf("Cannot get method ID for \"run\"\n");
        return STATUS_FAILED;
    }

    err = jvmti->RawMonitorEnter(popFrameLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("Cannot enter popFrameLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> catching method \"fibonacci\"\n");
    }

    for (i = 1; i < 10 && bpCount == 1; i++) {
        err = jvmti->GetFrameLocation(thr, 0, &method, &loc);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetFrameLocation) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
            break;
        }

        if (printdump == JNI_TRUE) {
            jvmti->GetMethodName(method, &name, &sig, &generic);
            printf(">>> %d: \"%s%s\"\n", i, name, sig);
        }

        if (method == midFibonacci) break;

        err = jvmti->ResumeThread(thr);
        if (err != JVMTI_ERROR_NONE) {
            printf("(ResumeThread) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }

        err = jvmti->RawMonitorWait(popFrameLock, (jlong)delayTime);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot wait popFrameLock: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }

        err = jvmti->SuspendThread(thr);
        if (err != JVMTI_ERROR_NONE) {
            printf("(SuspendThread) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }

    err = jvmti->RawMonitorExit(popFrameLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("Cannot exit popFrameLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (method == midFibonacci) {
        framesCount = 0;
        err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_SINGLE_STEP, thr);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot enable single step: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }

        err = jvmti->CreateRawMonitor("Suspend Lock", &suspendLock);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot create suspendLock: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }

        if (printdump == JNI_TRUE) {
            printf(">>> popping frames\n");
        }

        err = jvmti->RawMonitorEnter(popFrameLock);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot enter popFrameLock: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }

        popFrames(thr);

        err = jvmti->RawMonitorExit(popFrameLock);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot exit popFrameLock: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }

        err = jvmti->SetEventNotificationMode(JVMTI_DISABLE,
            JVMTI_EVENT_SINGLE_STEP, thr);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot disable single step: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
    } else {
        printf("Warning: method \"fibonacci\" was missed\n");
    }

    err = jvmti->ResumeThread(thr);
    if (err != JVMTI_ERROR_NONE) {
        printf("(ResumeThread) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> waiting breakpoint 2\n");
    }

    while (bpCount < 2) {
        err = jvmti->RawMonitorEnter(popFrameLock);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot enter popFrameLock: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }

        err = jvmti->RawMonitorWait(popFrameLock, (jlong)delayTime);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot wait popFrameLock: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }

        err = jvmti->RawMonitorExit(popFrameLock);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot exit popFrameLock: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }

    err = jvmti->RawMonitorEnter(breakpointLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("Cannot enter breakpointLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti->RawMonitorNotify(breakpointLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("Cannot notify breakpointLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti->RawMonitorExit(breakpointLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("Cannot exit breakpointLock: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    return result;
}

}
