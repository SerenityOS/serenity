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
#include <jvmti_tools.h>
#include "JVMTITools.h"

extern "C" {

// Deallocate memory region allocated by VM
#define DEALLOCATE(p) \
    if (p != NULL)                 \
        if (!NSK_JVMTI_VERIFY(jvmti->Deallocate(p)))        \
        {                          \
            NSK_COMPLAIN0("Failed to deallocate: ##p##\n"); \
        }

static jvmtiEnv *jvmti;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static int watch_jvmti_events = 0;        /* ignore JVMTI events by default */
static volatile int number_of_generated_events = 0;          /* number of generated events */
static jboolean result = JNI_TRUE; /* total result of the test */

static jrawMonitorID watch_ev_monitor;

static void set_watch_jvmti_events(int value) {
    jvmti->RawMonitorEnter(watch_ev_monitor);

    watch_jvmti_events = value;

    jvmti->RawMonitorExit(watch_ev_monitor);
}

void JNICALL
FramePop(
        jvmtiEnv *jvmti_env
        , JNIEnv *env
        , jthread thread
        , jmethodID method
        , jboolean wasPopedByException
        )
{
    jvmti->RawMonitorEnter(watch_ev_monitor);

    if (watch_jvmti_events) {
        NSK_COMPLAIN1("#### FramePop event occurred (%d) ####\n", method);
        number_of_generated_events++;
    }

    jvmti->RawMonitorExit(watch_ev_monitor);
}

void JNICALL
MethodExit(
        jvmtiEnv *jvmti_env
        , JNIEnv *env
        , jthread thread
        , jmethodID method
        , jboolean was_poped_by_exc
        , jvalue return_value
        )
{
    jvmti->RawMonitorEnter(watch_ev_monitor);

    if (watch_jvmti_events) {
        jvmtiThreadInfo thr_info;
        char *class_signature;
        char *entry_name;
        char *entry_sig;
        jclass klass;

        int failure = 1;

        do {
            if (!NSK_JVMTI_VERIFY(jvmti->GetThreadInfo(thread, &thr_info)))
            {
                break;
            }

            if (!NSK_JVMTI_VERIFY(jvmti->GetMethodDeclaringClass(method, &klass)))
            {
                break;
            }

            if (!NSK_JVMTI_VERIFY(jvmti->GetClassSignature(klass, &class_signature, NULL)))
            {
                break;
            }

            if (!NSK_JVMTI_VERIFY(jvmti->GetMethodName(method, &entry_name, &entry_sig, NULL)))
            {
                break;
            }

            failure = 0;
            NSK_COMPLAIN5("#### MethodExit event occurred: (tid: %d), thread: %s, %s %s %s\n"
                    , thread
                    , thr_info.name == NULL ? "<Unnamed>" : thr_info.name
                    , class_signature
                    , entry_name
                    , entry_sig
                    );
        } while (0);

        if (failure) {
            NSK_COMPLAIN1("#### MethodExit event occurred (tid: %d) ####\n"
                    , thread
                    );
        }

        DEALLOCATE((unsigned char *)class_signature);
        DEALLOCATE((unsigned char *)entry_name);
        DEALLOCATE((unsigned char *)entry_sig);

        number_of_generated_events++;
    }

    jvmti->RawMonitorExit(watch_ev_monitor);
}

jboolean suspendThread(jobject suspendedThread) {
    if (!caps.can_pop_frame || !caps.can_suspend) {
        return JNI_TRUE;
    }

    NSK_DISPLAY0(">>>>>>>> Invoke SuspendThread()\n");

    if (!NSK_JVMTI_VERIFY(jvmti->SuspendThread(suspendedThread)))
    {
        return JNI_FALSE;
    }

    NSK_DISPLAY0("<<<<<<<< SuspendThread() is successfully done\n");

    return JNI_TRUE;
}

jboolean resThread(jobject suspendedThread) {
    if (!caps.can_pop_frame || !caps.can_suspend) {
        return JNI_TRUE;
    }

    NSK_DISPLAY0(">>>>>>>> Invoke ResumeThread()\n");

    if (!NSK_JVMTI_VERIFY(jvmti->ResumeThread(suspendedThread)))
    {
        return JNI_FALSE;
    }

    NSK_DISPLAY0("<<<<<<<< ResumeThread() is successfully done\n");

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_PopFrame_popframe005_doPopFrame(
    JNIEnv *env
    , jclass klass
    , jobject frameThr
    )
{
    if (!caps.can_pop_frame || !caps.can_suspend) {
        return JNI_TRUE;
    }

    if (suspendThread(frameThr) != JNI_TRUE) {
        return JNI_FALSE;
    }

    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_EXIT, frameThr)))
    {
        result = JNI_FALSE;
    }

    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_FRAME_POP, frameThr)))
    {
        result = JNI_FALSE;
    }

    NSK_DISPLAY0(">>>>>>>> Invoke PopFrame()\n");

    set_watch_jvmti_events(1); /* watch JVMTI events */

    if (!NSK_JVMTI_VERIFY(jvmti->PopFrame(frameThr)))
    {
        result = JNI_FALSE;
    } else {
        NSK_DISPLAY0("Check #1 PASSED: PopFrame() is successfully done\n");
    }

    set_watch_jvmti_events(0); /* ignore again JVMTI events */

    if (number_of_generated_events) {
        NSK_COMPLAIN1("%d JVMTI events have been generated by the function PopFrame()\n"
                , number_of_generated_events
                );
        result = JNI_FALSE;
    } else {
        NSK_DISPLAY0("Check #2 PASSED: No JVMTI events have been generated by the function PopFrame()\n");
    }

    if (resThread(frameThr) != JNI_TRUE)
        return JNI_FALSE;

    return result;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_popframe005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_popframe005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_popframe005(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *vm, char *options, void *reserved)
{
    jvmtiError err;

    if (!NSK_VERIFY(
            nsk_jvmti_parseOptions(options)
            ))
    {
        return JNI_ERR;
    }

    if (!NSK_VERIFY(
            (jvmti = nsk_jvmti_createJVMTIEnv(vm, reserved)) != NULL
            ))
    {
        return JNI_ERR;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->GetPotentialCapabilities(&caps)))
    {
        return JNI_ERR;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
    {
        return JNI_ERR;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
    {
        return JNI_ERR;
    }

    if (!caps.can_pop_frame) {
        NSK_COMPLAIN0("Warning: PopFrame is not implemented\n");
        return JNI_OK;
    }

    if (!caps.can_suspend) {
        NSK_COMPLAIN0("Warning: suspend/resume is not implemented\n");
        return JNI_OK;
    }

    if (caps.can_generate_frame_pop_events
        && caps.can_generate_method_exit_events)
    {
        callbacks.MethodExit = &MethodExit;
        callbacks.FramePop = &FramePop;

        if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks)
                    )))
        {
            return JNI_ERR;
        }
    } else {
        NSK_COMPLAIN0("Warning: FramePop or MethodExit event is not implemented\n");
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
