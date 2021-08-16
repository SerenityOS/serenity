/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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


#define PASSED  0
#define STATUS_FAILED  2
#define WAIT_START 100

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jrawMonitorID access_lock, wait_lock;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jthread thr_ptr = NULL;
static jint wait_time = 0;
static jint state[] = {
    JVMTI_THREAD_STATE_RUNNABLE,
    JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER,
    JVMTI_THREAD_STATE_IN_OBJECT_WAIT
};

void printStateFlags(jint flags) {
    if (flags & JVMTI_THREAD_STATE_SUSPENDED)
        printf(" JVMTI_THREAD_STATE_SUSPENDED");
    if (flags & JVMTI_THREAD_STATE_INTERRUPTED)
        printf(" JVMTI_THREAD_STATE_INTERRUPTED");
    if (flags & JVMTI_THREAD_STATE_IN_NATIVE)
        printf(" JVMTI_THREAD_STATE_IN_NATIVE");
    printf(" (0x%0x)\n", flags);
}

void JNICALL VMInit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr) {
    jvmtiError err;

    err = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_THREAD_START, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable THREAD_START event: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

void JNICALL
ThreadStart(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread) {
    jvmtiThreadInfo thrInfo;
    jvmtiError err;

    err = jvmti_env->RawMonitorEnter(access_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorEnter) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti_env->GetThreadInfo(thread, &thrInfo);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetThreadInfo) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    if (thrInfo.name != NULL && strcmp(thrInfo.name, "thr1") == 0) {
        thr_ptr = env->NewGlobalRef(thread);
        if (printdump == JNI_TRUE) {
            printf(">>> ThreadStart: \"%s\", 0x%p\n", thrInfo.name, thr_ptr);
        }
    }

    err = jvmti_env->RawMonitorExit(access_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorExit) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_thrstat002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_thrstat002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_thrstat002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

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

    err = jvmti->CreateRawMonitor("_access_lock", &access_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(CreateRawMonitor) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->CreateRawMonitor("_wait_lock", &wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(CreateRawMonitor) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    callbacks.VMInit = &VMInit;
    callbacks.ThreadStart = &ThreadStart;
    err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    if (err != JVMTI_ERROR_NONE) {
        printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_VM_INIT, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable VM_INIT event: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetThreadState_thrstat002_init(JNIEnv *env, jclass cls,
        jint waitTime) {
    wait_time = waitTime * 60000;
}

void wait_for(jint millis) {
    jvmtiError err;

    err = jvmti->RawMonitorEnter(wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorEnter#check) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    err = jvmti->RawMonitorWait(wait_lock, (jlong)millis);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorWait#check) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    err = jvmti->RawMonitorExit(wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorExit#check) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetThreadState_thrstat002_checkStatus(JNIEnv *env, jclass cls,
        jint statInd, jboolean suspended) {
    jint thrState;
    jint suspState = -1;
    jint right_stat = (suspended ? JVMTI_THREAD_STATE_SUSPENDED : 0);
    jvmtiError right_ans = (suspended ? JVMTI_ERROR_THREAD_SUSPENDED : JVMTI_ERROR_NONE);
    const char *suspStr = (suspended ? ", suspended" : "");
    jvmtiError err;
    jint millis;
    jboolean timeout_is_reached;
    unsigned int waited_millis;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (thr_ptr == NULL) {
        printf("Missing thread \"thr1\" start event\n");
        result = STATUS_FAILED;
        return;
    }

    if (!caps.can_suspend) {
        return;
    }

    printf("START checkStatus for \"thr1\" (0x%p%s), check state: %s\n",
           thr_ptr, suspStr, TranslateState(state[statInd]));

    timeout_is_reached = JNI_TRUE;
    for (millis = WAIT_START, waited_millis=0; millis < wait_time; millis <<= 1) {
        err = jvmti->GetThreadState(thr_ptr, &thrState);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetThreadState#%d) unexpected error: %s (%d)\n",
                statInd, TranslateError(err), err);
            result = STATUS_FAILED;
            timeout_is_reached = JNI_FALSE;
            break;
        }
        suspState = thrState & JVMTI_THREAD_STATE_SUSPENDED;
        if (suspended || (thrState & JVMTI_THREAD_STATE_RUNNABLE) == 0 ||
            (state[statInd] == JVMTI_THREAD_STATE_RUNNABLE)) {
            timeout_is_reached = JNI_FALSE;
            break;
        }

        waited_millis += millis;
        wait_for(millis);
    }

    if (printdump == JNI_TRUE) {
        printf(">>> thread \"thr1\" (0x%p) state: %s (%d)\n",
            thr_ptr, TranslateState(thrState), thrState);
        printf(">>>\tflags:");
        printStateFlags(suspState);
    }

    if (timeout_is_reached == JNI_TRUE) {
        printf("Error: timeout (%d secs) has been reached\n", waited_millis/1000);
    }
    if ((thrState & state[statInd]) == 0) {
        printf("Wrong thread \"thr1\" (0x%p%s) state:\n", thr_ptr, suspStr);
        printf("    expected: %s (%d)\n",
            TranslateState(state[statInd]), state[statInd]);
        printf("      actual: %s (%d)\n",
            TranslateState(thrState), thrState);
        result = STATUS_FAILED;
    }
    if (suspState != right_stat) {
        printf("Wrong thread \"thr1\" (0x%p%s) state flags:\n",
               thr_ptr, suspStr);
        printf("    expected:");
        printStateFlags(right_stat);
        printf("    actual:");
        printStateFlags(suspState);
        result = STATUS_FAILED;
    }

    err = jvmti->SuspendThread(thr_ptr);
    if (err != right_ans) {
        printf("Wrong result of SuspendThread() for \"thr1\" (0x%p%s):\n",
               thr_ptr, suspStr);
        printf("    expected: %s (%d), actual: %s (%d)\n",
            TranslateError(right_ans), right_ans, TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (!suspended) {
        // wait till thread is not suspended
        timeout_is_reached = JNI_TRUE;
        for (millis = WAIT_START, waited_millis=0; millis < wait_time; millis <<= 1) {
            waited_millis += millis;
            wait_for(millis);
            err = jvmti->GetThreadState(thr_ptr, &thrState);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetThreadState#%d,after) unexpected error: %s (%d)\n",
                    statInd, TranslateError(err), err);
                timeout_is_reached = JNI_FALSE;
                result = STATUS_FAILED;
                break;
            }
            suspState = thrState & JVMTI_THREAD_STATE_SUSPENDED;
            if (suspState) {
                timeout_is_reached = JNI_FALSE;
                break;
            }
        }

        if (timeout_is_reached == JNI_TRUE) {
            printf("Error: timeout (%d secs) has been reached\n", waited_millis/1000);
        }
        if ((thrState & state[statInd]) == 0) {
            printf("Wrong thread \"thr1\" (0x%p) state after SuspendThread:\n",
                thr_ptr);
            printf("    expected: %s (%d)\n",
                TranslateState(state[statInd]), state[statInd]);
            printf("      actual: %s (%d)\n",
                TranslateState(thrState), thrState);
            result = STATUS_FAILED;
        }
        if (suspState != JVMTI_THREAD_STATE_SUSPENDED) {
            printf("Wrong thread \"thr1\" (0x%p) state flags", thr_ptr);
            printf(" after SuspendThread:\n");
            printf("    expected:");
            printStateFlags(JVMTI_THREAD_STATE_SUSPENDED);
            printf("    actual:");
            printStateFlags(suspState);
            result = STATUS_FAILED;
        }
        err = jvmti->ResumeThread(thr_ptr);
        if (err != JVMTI_ERROR_NONE) {
            printf("(ResumeThread#%d) unexpected error: %s (%d)\n",
                statInd, TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }
    fflush(0);
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetThreadState_thrstat002_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
