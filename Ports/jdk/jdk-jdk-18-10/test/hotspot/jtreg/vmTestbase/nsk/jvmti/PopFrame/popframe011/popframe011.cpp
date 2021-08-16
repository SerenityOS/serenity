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
#include <stdlib.h>
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define STATUS_FAILED 2
#define PASSED 0

static jvmtiEnv *jvmti;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static int watch_ev = 0;        /* ignore JVMTI events by default */
static int gen_ev = 0;          /* number of generated events */
static jvmtiError popframe_err = JVMTI_ERROR_NONE;

static jrawMonitorID watch_ev_monitor;

static void set_watch_ev(int value) {
    jvmti->RawMonitorEnter(watch_ev_monitor);

    watch_ev = value;

    jvmti->RawMonitorExit(watch_ev_monitor);
}

void JNICALL
FramePop(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread,
        jmethodID method, jboolean wasPopedByException) {
    jvmti->RawMonitorEnter(watch_ev_monitor);

    if (watch_ev) {
        printf("#### FramePop event occurred ####\n");
        gen_ev++;
    }

    jvmti->RawMonitorExit(watch_ev_monitor);
}

void JNICALL
MethodExit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr,
        jmethodID method, jboolean was_poped_by_exc, jvalue return_value) {
    jvmti->RawMonitorEnter(watch_ev_monitor);

    if (watch_ev) {
        printf("#### MethodExit event occurred ####\n");
        gen_ev++;
    }

    jvmti->RawMonitorExit(watch_ev_monitor);
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_PopFrame_popframe011_doPopFrame(JNIEnv *env,
        jclass cls, jint t_case, jobject frameThr) {
    if (!caps.can_pop_frame) {
        return PASSED;
    }

    if (t_case != 6 && t_case != 7) {
        /*
         * Only enable this event for test cases where the event
         * should not happen.
         */
        popframe_err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_METHOD_EXIT, frameThr);
        if (popframe_err != JVMTI_ERROR_NONE) {
            printf("Failed to enable METHOD_EXIT event: %s (%d)\n",
                   TranslateError(popframe_err), popframe_err);
            return STATUS_FAILED;
        }
    }

    popframe_err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_FRAME_POP, frameThr);
    if ((t_case == 6 || t_case == 7) &&
        popframe_err == JVMTI_ERROR_THREAD_NOT_ALIVE) {
        // Our target thread has exited which is okay.
        return PASSED;
    }
    if (popframe_err != JVMTI_ERROR_NONE) {
        printf("Failed to enable FRAME_POP event: %s (%d)\n",
               TranslateError(popframe_err), popframe_err);
        return STATUS_FAILED;
    }

    switch (t_case) {
/* NULL pointer to the thread in debug mode */
    case 1:
        printf("\nInvoke PopFrame() with NULL pointer to a thread...\n");
        fflush(stdout);
        // fallthrough
/* NULL pointer to the thread */
    case 0:
        set_watch_ev(1); /* watch JVMTI events */
        popframe_err = (jvmti->PopFrame(NULL)); /* explode the bomb */
        if (popframe_err != JVMTI_ERROR_INVALID_THREAD) {
            printf("TEST FAILED: the function PopFrame() returned the error %d: %s\n",
                popframe_err, TranslateError(popframe_err));
            printf("\tBut it should return the error JVMTI_ERROR_INVALID_THREAD.\n");
            return STATUS_FAILED;
        }
        break;
/* invalid thread in debug mode */
    case 3:
        printf("\nInvoke PopFrame() for an invalid thread...\n");
        fflush(stdout);
        // fallthrough
/* invalid thread */
    case 2:
        set_watch_ev(1); /* watch JVMTI events */
        popframe_err = (jvmti->PopFrame(cls)); /* explode the bomb */
        set_watch_ev(0); /* ignore again JVMTI events */
        if (popframe_err != JVMTI_ERROR_INVALID_THREAD) {
            printf("TEST FAILED: the function PopFrame() returned the error %d: %s\n",
                popframe_err, TranslateError(popframe_err));
            printf("\tBut it should return the error JVMTI_ERROR_INVALID_THREAD.\n");
            return STATUS_FAILED;
        }
        break;
/* non suspended thread in debug mode */
    case 5:
        printf("\nInvoke PopFrame() for a non suspended thread...\n");
        fflush(stdout);
        // fallthrough
/* non suspended thread */
    case 4:
        set_watch_ev(1); /* watch JVMTI events */
        popframe_err = (jvmti->PopFrame(frameThr)); /* explode the bomb */
        set_watch_ev(0); /* ignore again JVMTI events */
        if (popframe_err != JVMTI_ERROR_THREAD_NOT_SUSPENDED) {
            printf("TEST FAILED: the function PopFrame() returned the error %d: %s\n",
                popframe_err, TranslateError(popframe_err));
            printf("\tBut it should return the error JVMTI_ERROR_THREAD_NOT_SUSPENDED.\n");
            return STATUS_FAILED;
        }
        break;
/* non suspended and exiting thread in debug mode */
    case 7:
        printf("\nInvoke PopFrame() for a non suspended and exiting thread...\n");
        fflush(stdout);
        // fallthrough
/* non suspended and exiting thread */
    case 6:
        set_watch_ev(1); /* watch JVMTI events */
        popframe_err = (jvmti->PopFrame(frameThr)); /* explode the bomb */
        set_watch_ev(0); /* ignore again JVMTI events */
        if (popframe_err != JVMTI_ERROR_THREAD_NOT_SUSPENDED &&
            popframe_err != JVMTI_ERROR_THREAD_NOT_ALIVE) {
            printf("TEST FAILED: the function PopFrame() returned the error %d: %s\n",
                popframe_err, TranslateError(popframe_err));
            printf("\tBut it should return the error JVMTI_ERROR_THREAD_NOT_SUSPENDED or JVMTI_ERROR_THREAD_NOT_ALIVE.\n");
            return STATUS_FAILED;
        }
        break;
    }

    if (gen_ev) {
        printf("TEST FAILED: %d JVMTI events were generated by the function PopFrame()\n",
            gen_ev);
        return STATUS_FAILED;
    } else if (t_case == 1 || t_case == 3 || t_case == 5 || t_case == 7)
        printf("Check #%d PASSED: No JVMTI events were generated by the function PopFrame()\n",
            t_case+1);

    set_watch_ev(0); /* ignore again JVMTI events */

    if (t_case != 6 && t_case != 7) {
        /*
         * Only disable this event for test cases where the event
         * should not happen.
         */
        popframe_err = jvmti->SetEventNotificationMode(JVMTI_DISABLE,
            JVMTI_EVENT_METHOD_EXIT, frameThr);
        if (popframe_err != JVMTI_ERROR_NONE) {
            printf("Failed to disable METHOD_EXIT event: %s (%d)\n",
                   TranslateError(popframe_err), popframe_err);
            return STATUS_FAILED;
        }
    }

    popframe_err = jvmti->SetEventNotificationMode(JVMTI_DISABLE,
        JVMTI_EVENT_FRAME_POP, frameThr);
    if ((t_case == 6 || t_case == 7) &&
        popframe_err == JVMTI_ERROR_THREAD_NOT_ALIVE) {
        // Our target thread has exited which is okay.
        return PASSED;
    }
    if (popframe_err != JVMTI_ERROR_NONE) {
        printf("Failed to disable FRAME_POP event: %s (%d)\n",
               TranslateError(popframe_err), popframe_err);
        return STATUS_FAILED;
    }

    return PASSED;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_PopFrame_popframe011_isThreadNotAliveError(JNIEnv *env, jclass cls) {
    if (popframe_err == JVMTI_ERROR_THREAD_NOT_ALIVE) {
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_popframe011(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_popframe011(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_popframe011(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

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
        return JNI_OK;
    }

    if (caps.can_generate_frame_pop_events &&
            caps.can_generate_method_exit_events) {
        callbacks.MethodExit = &MethodExit;
        callbacks.FramePop = &FramePop;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: FramePop or MethodExit event is not implemented\n");
    }

    err = jvmti->CreateRawMonitor("watch_ev_monitor", &watch_ev_monitor);
    if (err != JVMTI_ERROR_NONE) {
        printf("(CreateRawMonitor) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    return JNI_OK;
}

}
