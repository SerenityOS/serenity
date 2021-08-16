/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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
#define WAIT_TIME (2*60*1000)

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jrawMonitorID access_lock;
static jrawMonitorID wait_lock;
static jint result = PASSED;
static jthread thr_ptr = NULL;

static jint state[] = {
    JVMTI_THREAD_STATE_RUNNABLE,
    JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER,
    JVMTI_THREAD_STATE_IN_OBJECT_WAIT
};

static void
lock(const char* func_name, jrawMonitorID lock) {
    jvmtiError err = jvmti->RawMonitorEnter(lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("%s: unexpected error in RawMonitorEnter: %s (%d)\n",
               func_name, TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

static void
unlock(const char* func_name, jrawMonitorID lock) {
    jvmtiError err = jvmti->RawMonitorExit(lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("%s: unexpected error in RawMonitorExit: %s (%d)\n",
               func_name, TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

static void
wait(const char* func_name, jrawMonitorID lock, jint millis) {
    jvmtiError err = jvmti->RawMonitorWait(lock, (jlong)millis);
    if (err != JVMTI_ERROR_NONE) {
        printf("%s: unexpected error in RawMonitorWait: %s (%d)\n",
               func_name, TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

static void
set_notification_mode(const char* event_name,
                      jvmtiEventMode mode,
                      jvmtiEvent event_type,
                      jthread event_thread) {
    const char* action = (mode == JVMTI_ENABLE) ? "enable" : "disable";
    jvmtiError err = jvmti->SetEventNotificationMode(mode, event_type, event_thread);

    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to %s %s event: %s (%d)\n",
               action, event_name, TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

void JNICALL VMInit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr) {
    set_notification_mode("JVMTI_EVENT_THREAD_START", JVMTI_ENABLE,
                          JVMTI_EVENT_THREAD_START, NULL);
}

void JNICALL
ThreadStart(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread) {
    jvmtiError err;
    jvmtiThreadInfo thrInfo;

    lock("ThreadStart", access_lock);

    err = jvmti_env->GetThreadInfo(thread, &thrInfo);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetThreadInfo#TS) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    if (thrInfo.name != NULL && strcmp(thrInfo.name, "thr1") == 0) {
        thr_ptr = env->NewGlobalRef(thread);
        printf(">>> ThreadStart: \"%s\", 0x%p\n", thrInfo.name, thr_ptr);
        set_notification_mode("JVMTI_EVENT_THREAD_START", JVMTI_DISABLE,
                              JVMTI_EVENT_THREAD_START, NULL);
    }

    unlock("ThreadStart", access_lock);
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_thrstat001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_thrstat001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_thrstat001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif

jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    printf("Agent_Initialize started\n");

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

    err = jvmti->CreateRawMonitor("_access_lock", &access_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(CreateRawMonitor)#access_lock unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->CreateRawMonitor("_wait_lock", &wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(CreateRawMonitor#wait_lock) unexpected error: %s (%d)\n",
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

    set_notification_mode("JVMTI_EVENT_VM_INIT", JVMTI_ENABLE,
                          JVMTI_EVENT_VM_INIT, NULL);

    printf("Agent_Initialize finished\n\n");
    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetThreadState_thrstat001_checkStatus(JNIEnv *env,
        jclass cls, jint statInd) {
    jvmtiError err;
    jint thrState;
    jint millis;

    printf("native method checkStatus started\n");
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

    /* wait until thread gets an expected state */
    for (millis = WAIT_START; millis < WAIT_TIME; millis <<= 1) {
        err = jvmti->GetThreadState(thr_ptr, &thrState);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetThreadState#%d) unexpected error: %s (%d)\n",
                statInd, TranslateError(err), err);
            result = STATUS_FAILED;
        }
        if ((thrState & state[statInd]) != 0) {
            break;
        }
        lock("checkStatus", wait_lock);
        wait("checkStatus", wait_lock, millis);
        unlock("checkStatus", wait_lock);
    }

    printf(">>> thread \"thr1\" (0x%p) state: %s (%d)\n",
            thr_ptr, TranslateState(thrState), thrState);

    if ((thrState & state[statInd]) == 0) {
        printf("Wrong thread \"thr1\" (0x%p) state:\n", thr_ptr);
        printf("    expected: %s (%d)\n",
            TranslateState(state[statInd]), state[statInd]);
        printf("      actual: %s (%d)\n",
            TranslateState(thrState), thrState);
        result = STATUS_FAILED;
    }
    printf("native method checkStatus finished\n\n");
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetThreadState_thrstat001_getRes(JNIEnv *env, jclass cls) {
    printf("native method getRes: result: %d\n\n", result);
    return result;
}

}
