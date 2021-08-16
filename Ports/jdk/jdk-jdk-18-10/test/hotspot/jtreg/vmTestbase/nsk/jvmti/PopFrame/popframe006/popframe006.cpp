/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
} check_info;

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jboolean wasFramePop = JNI_FALSE;
static jmethodID mid_run, mid_A, mid_B, mid_C;
static int bpCount = 0, bpExpected = 0;
static int stepCount = 0, stepExpected = 0;
static int popCount = 0, popExpected = 0;
static check_info checks[] = {
    { "Lnsk/jvmti/PopFrame/popframe006$TestThread;", "run", "()V", 0 },
    { "Lnsk/jvmti/PopFrame/popframe006$TestThread;", "A", "()V", 0 },
    { "Lnsk/jvmti/PopFrame/popframe006$TestThread;", "B", "()V", 0 },
    { "Lnsk/jvmti/PopFrame/popframe006$TestThread;", "A", "()V", 0 },
    { "Lnsk/jvmti/PopFrame/popframe006$TestThread;", "B", "()V", 0 },
    { "Lnsk/jvmti/PopFrame/popframe006$TestThread;", "C", "()V", 0 },
    { "Lnsk/jvmti/PopFrame/popframe006$TestThread;", "C", "()V", 0 },
    { "Lnsk/jvmti/PopFrame/popframe006$TestThread;", "B", "()V", 3 },
    { "Lnsk/jvmti/PopFrame/popframe006$TestThread;", "A", "()V", 3 }
};

void check(jvmtiEnv *jvmti_env, jmethodID mid, jlocation loc, int i) {
    jvmtiError err;
    jclass cls;
    const char *note;
    char *sigClass, *name, *sig, *generic;

    switch (i) {
    case 0:
        bpCount++;
        note = "bp";
        break;
    case 1: case 2: case 3: case 4: case 5:
        stepCount++;
        note = "step";
        break;
    case 6: case 7: case 8:
        popCount++;
        note = "pop";
        break;
    default:
        return;
    }

    err = jvmti_env->GetMethodDeclaringClass(mid, &cls);
    if (err != JVMTI_ERROR_NONE) {
        printf("(%s, GetMethodDeclaringClass#%d) unexpected error: %s (%d)\n",
               note, i, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti_env->GetClassSignature(cls, &sigClass, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(%s, GetClassSignature#%d) unexpected error: %s (%d)\n",
               note, i, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti_env->GetMethodName(mid, &name, &sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(%s, GetMethodName#%d) unexpected error: %s (%d)\n",
               note, i, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (sigClass == NULL || strcmp(sigClass, checks[i].cls) != 0) {
        printf("(%s, %d) wrong class sig: \"%s\",\n", note, i, sigClass);
        printf(" expected: \"%s\"\n", checks[i].cls);
        result = STATUS_FAILED;
    }
    if (name == NULL || strcmp(name, checks[i].name) != 0) {
        printf("(%s, %d) wrong method name: \"%s\",", note, i, name);
        printf(" expected: \"%s\"\n", checks[i].name);
        result = STATUS_FAILED;
    }
    if (sig == NULL || strcmp(sig, checks[i].sig) != 0) {
        printf("(%s, %d) wrong method sig: \"%s\",", note, i, sig);
        printf(" expected: \"%s\"\n", checks[i].sig);
        result = STATUS_FAILED;
    }
    if (loc != checks[i].loc) {
        printf("(%s, %d) wrong location: 0x%x%08x,",
               note, i, (jint)(loc >> 32), (jint)loc);
        printf(" expected: 0x%x\n", (jint)checks[i].loc);
        result = STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> (%s, %d) \"%s.%s%s\"", note, i, sigClass, name, sig);
        printf(", location: 0x%x%08x\n", (jint)(loc >> 32), (jint)loc);
    }
}


void JNICALL Breakpoint(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jlocation location) {
    jvmtiError err;

    if (method != mid_run) {
        printf("bp: don't know where we get called from");
        result = STATUS_FAILED;
        return;
    }
    if (printdump == JNI_TRUE) {
        printf(">>> breakpoint in run\n");
    }
    err = jvmti_env->ClearBreakpoint(mid_run, 0);
    if (err != JVMTI_ERROR_NONE) {
        printf("(ClearBreakpoint) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    check(jvmti_env, method, location, 0);

    if (!caps.can_generate_single_step_events) {
        return;
    }

    err = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_SINGLE_STEP, thread);
    if (err != JVMTI_ERROR_NONE) {
        printf("Cannot enable single step: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    } else {
        stepExpected = 3;
    }
    if (printdump == JNI_TRUE) {
        printf(">>> about to step into A\n");
    }
}

void JNICALL SingleStep(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jlocation location) {
    jvmtiError err;

    if (method == mid_A) {
        if (wasFramePop == JNI_FALSE) {
            if (printdump == JNI_TRUE) {
                printf(">>> step into A\n");
            }
            check(jvmti_env, method, location, 1);
            err = jvmti_env->NotifyFramePop(thread, 0);
            if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY &&
                    !caps.can_generate_frame_pop_events) {
                /* Ok, it's expected */
            } else if (err != JVMTI_ERROR_NONE) {
                printf("(NotifyFramePop#A) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            } else {
                popExpected = popExpected + 1;
            }
            if (printdump == JNI_TRUE) {
                printf(">>> about to step into B\n");
            }
        } else {
            if (printdump == JNI_TRUE) {
                printf(">>> step into A after pop frame\n");
            }
            check(jvmti_env, method, location, 3);
            if (printdump == JNI_TRUE) {
                printf(">>> about to step into B after pop\n");
            }
        }
    } else if (method == mid_B) {
        if (wasFramePop == JNI_FALSE) {
            if (printdump == JNI_TRUE) {
                printf(">>> step into B\n");
            }
            check(jvmti_env, method, location, 2);
            if (!caps.can_pop_frame) {
                return;
            }
            if (printdump == JNI_TRUE) {
                printf(">>> about to pop frame\n");
            }
            err = jvmti_env->PopFrame(thread);
            if (err != JVMTI_ERROR_NONE) {
                printf("(PopFrame) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
                return;
            } else {
                stepExpected = stepExpected + 2;
            }
            wasFramePop = JNI_TRUE;
            if (printdump == JNI_TRUE) {
                printf(">>> about to step into A after pop frame\n");
            }
        } else {
            if (printdump == JNI_TRUE) {
                printf(">>> step into B after pop frame\n");
            }
            check(jvmti_env, method, location, 4);
            err = jvmti->NotifyFramePop(thread, 0);
            if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY &&
                    !caps.can_generate_frame_pop_events) {
                /* Ok, it's expected */
            } else if (err != JVMTI_ERROR_NONE) {
                printf("(NotifyFramePop#B) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            } else {
                popExpected = popExpected + 1;
            }
            if (printdump == JNI_TRUE) {
                printf(">>> about to step into C\n");
            }
        }
    } else if (method == mid_C) {
        if (printdump == JNI_TRUE) {
            printf(">>> step into C\n");
        }
        err = jvmti_env->SetEventNotificationMode(JVMTI_DISABLE,
            JVMTI_EVENT_SINGLE_STEP, thread);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot disable single step: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
        check(jvmti_env, method, location, 5);
        err = jvmti->NotifyFramePop(thread, 0);
        if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY &&
                !caps.can_generate_frame_pop_events) {
            /* Ok, it's expected */
        } else if (err != JVMTI_ERROR_NONE) {
            printf("(NotifyFramePop#C) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        } else {
            popExpected = popExpected + 1;
        }
        if (printdump == JNI_TRUE) {
            printf(">>> about to step out of C\n");
        }
    } else {
        printf("step: don't know where we get called from");
        result = STATUS_FAILED;
        return;
    }
}

void JNICALL
FramePop(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jboolean wasPopedByException) {
    jvmtiError err;
    jmethodID mid;
    jlocation loc;

    err = jvmti_env->GetFrameLocation(thread, 0, &mid, &loc);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetFrameLocation) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (method == mid_C) {
        if (printdump == JNI_TRUE) {
            printf(">>> step out of C\n");
        }
        check(jvmti_env, mid, loc, 6);
        if (printdump == JNI_TRUE) {
            if (wasFramePop == JNI_FALSE) {
                printf(">>> about to step out of A\n");
            } else {
                printf(">>> about to step out of B\n");
            }
        }
    } else if (method == mid_B) {
        if (printdump == JNI_TRUE) {
            printf(">>> step out of B\n");
        }
        check(jvmti_env, mid, loc, 7);
        if (printdump == JNI_TRUE) {
            printf(">>> about to step out of A\n");
        }
    } else if (method == mid_A) {
        if (printdump == JNI_TRUE) {
            printf(">>> step out of A\n");
        }
        check(jvmti_env, mid, loc, 8);
    } else {
        printf("pop: don't know where we get called from");
        result = STATUS_FAILED;
        return;
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_popframe006(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_popframe006(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_popframe006(JavaVM *jvm, char *options, void *reserved) {
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
            caps.can_generate_single_step_events &&
            caps.can_generate_frame_pop_events) {
        callbacks.Breakpoint = &Breakpoint;
        callbacks.SingleStep = &SingleStep;
        callbacks.FramePop = &FramePop;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: Breakpoint, SingleStep or FramePop event are not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_PopFrame_popframe006_getReady(JNIEnv *env,
        jclass cls, jthread thr) {
    jvmtiError err;
    jclass clazz;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (!caps.can_pop_frame) {
        return;
    }

    if (!caps.can_generate_breakpoint_events) {
        return;
    }

    clazz = env->GetObjectClass(thr);
    if (clazz == NULL) {
        printf("Cannot get the class of thread object\n");
        result = STATUS_FAILED;
        return;
    }

    mid_run = env->GetMethodID(clazz, "run", "()V");
    if (mid_run == NULL) {
        printf("Cannot find Method ID for method \"run\"\n");
        result = STATUS_FAILED;
        return;
    }

    mid_A = env->GetStaticMethodID(clazz, "A", "()V");
    if (mid_A == NULL) {
        printf("Cannot find Method ID for method \"A\"\n");
        result = STATUS_FAILED;
        return;
    }

    mid_B = env->GetStaticMethodID(clazz, "B", "()V");
    if (mid_B == NULL) {
        printf("Cannot find Method ID for method \"B\"\n");
        result = STATUS_FAILED;
        return;
    }

    mid_C = env->GetStaticMethodID(clazz, "C", "()V");
    if (mid_C == NULL) {
        printf("Cannot find Method ID for method \"C\"\n");
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->SetBreakpoint(mid_run, 0);
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
    } else {
        bpExpected = 1;
    }

    if (!caps.can_generate_frame_pop_events) {
        return;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_FRAME_POP, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable FRAME_POP event: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_PopFrame_popframe006_getRes(JNIEnv *env, jclass cls) {
    if (printdump == JNI_TRUE) {
        printf(">>> Total: %d breakpoint, %d steps, %d frame pops\n",
               bpCount, stepCount, popCount);
    }
    if (bpCount != bpExpected) {
        printf("Wrong number of breakpoint events: %d, expected: %d\n",
            bpCount, bpExpected);
        result = STATUS_FAILED;
    }
    if (stepCount != stepExpected) {
        printf("Wrong number of step events: %d, expected: %d\n",
            stepCount, stepExpected);
        result = STATUS_FAILED;
    }
    if (popCount != popExpected) {
        printf("Wrong number of frame pop events: %d, expected: %d\n",
            popCount, popExpected);
        result = STATUS_FAILED;
    }
    return result;
}

}
