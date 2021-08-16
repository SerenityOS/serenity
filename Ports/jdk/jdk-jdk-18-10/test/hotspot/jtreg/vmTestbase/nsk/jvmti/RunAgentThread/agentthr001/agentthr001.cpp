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
#define WAIT_TIME (jlong)100

static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static int eventsCount = 0;
static int count = 0;
static jrawMonitorID cpu_lock;
static int thr1_was_started = 0;

jthread jthr(JNIEnv *env) {
    jclass thrClass;
    jmethodID cid;
    jthread res;

    thrClass = env->FindClass("java/lang/Thread");
    cid = env->GetMethodID(thrClass, "<init>", "()V");
    res = env->NewObject(thrClass, cid);
    return res;
}

static void JNICALL
sys_thread_4(jvmtiEnv* jvmti, JNIEnv* jni, void *p) {
    jvmtiError err;

    err = jvmti->RawMonitorEnter(cpu_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorEnter#4) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    count |= 0x10;
    err = jvmti->RawMonitorWait(cpu_lock, WAIT_TIME);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorWait#4) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    count |= 0x20;
    err = jvmti->RawMonitorExit(cpu_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorExit#4) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

static void JNICALL
sys_thread_1(jvmtiEnv* jvmti, JNIEnv* jni, void *p) {
    jvmtiError err;

    err = jvmti->RawMonitorEnter(cpu_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorEnter#1) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    count |= 0x01;

    err = jvmti->RunAgentThread(jthr(jni), sys_thread_4, NULL,
        JVMTI_THREAD_MAX_PRIORITY);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RunAgentThread#4) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    count |= 0x02;
    err = jvmti->RawMonitorExit(cpu_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorExit#1) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

static void JNICALL
sys_thread_2(jvmtiEnv* jvmti, JNIEnv* jni, void *p) {
    jvmtiError err;

    err = jvmti->RawMonitorEnter(cpu_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorEnter#2) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    count |= 0x04;
    err = jvmti->RawMonitorWait(cpu_lock, WAIT_TIME);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorWait#2) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    count |= 0x08;
    err = jvmti->RawMonitorExit(cpu_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorExit#2) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

static void JNICALL
sys_thread_3(jvmtiEnv* jvmti, JNIEnv* jni, void *p) {
    while (1) {
    }
}

static void JNICALL
sys_thread_5(jvmtiEnv* jvmti, JNIEnv* jni, void *p) {
    jvmtiError err;

    err = jvmti->RawMonitorEnter(cpu_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorEnter#5) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    count |= 0x40;
    err = jvmti->RawMonitorWait(cpu_lock, WAIT_TIME);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorWait#5) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    count |= 0x80;
    err = jvmti->RawMonitorExit(cpu_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorExit#5) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

void JNICALL VMInit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr) {
    jvmtiError err;

    if (printdump == JNI_TRUE) {
        printf(">>> VMInit: enabling ThreadStart\n");
    }

    err = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_THREAD_START, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable JVMTI_EVENT_THREAD_START: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

void JNICALL
ThreadStart(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread) {
    jvmtiError err;
    jvmtiThreadInfo thrInfo;

    eventsCount++;
    err = jvmti_env->GetThreadInfo(thread, &thrInfo);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetThreadInfo#%d) unexpected error: %s (%d)\n",
               eventsCount, TranslateError(err), err);
        result = STATUS_FAILED;
    }
    if (printdump == JNI_TRUE) {
        printf(">>> ThreadStart: %s\n", thrInfo.name);
    }
    /* workaround to avoid bug:
     * 4334503 THREAD_START event is still sent twice for user-defined thread */
    if (strcmp(thrInfo.name, "thr1") == 0 && !thr1_was_started) {
        thr1_was_started = 1;
        err = jvmti_env->RunAgentThread(jthr((JNIEnv *)env),
            sys_thread_1, NULL, JVMTI_THREAD_MAX_PRIORITY);
        if (err != JVMTI_ERROR_NONE) {
            printf("(RunAgentThread#1) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
        err = jvmti_env->RunAgentThread(jthr((JNIEnv *)env),
            sys_thread_2, NULL, JVMTI_THREAD_NORM_PRIORITY);
        if (err != JVMTI_ERROR_NONE) {
            printf("(RunAgentThread#2) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
        err = jvmti_env->RunAgentThread(jthr((JNIEnv *)env),
            sys_thread_3, NULL, JVMTI_THREAD_MIN_PRIORITY);
        if (err != JVMTI_ERROR_NONE) {
            printf("(RunAgentThread#3) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_agentthr001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_agentthr001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_agentthr001(JavaVM *jvm, char *options, void *reserved) {
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

    err = jvmti->CreateRawMonitor("_CPU lock", &cpu_lock);
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
        printf("Failed to enable JVMTI_EVENT_VM_INIT: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_RunAgentThread_agentthr001_startSysThr(JNIEnv *env, jclass cls) {
    jvmtiError err;

    err = jvmti->RunAgentThread(jthr(env), sys_thread_5, NULL,
        JVMTI_THREAD_MAX_PRIORITY);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RunAgentThread#5) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_RunAgentThread_agentthr001_isOver(JNIEnv *env, jclass cls) {
    return (count == 0xFF ? JNI_TRUE : JNI_FALSE);
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RunAgentThread_agentthr001_getRes(JNIEnv *env, jclass cls) {
    jvmtiError err;

    if (printdump == JNI_TRUE) {
        printf(">>> getRes: disabling ThreadStart\n");
    }

    err = jvmti->SetEventNotificationMode(JVMTI_DISABLE,
            JVMTI_EVENT_THREAD_START, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to disable JVMTI_EVENT_THREAD_START: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (count != 0xFF) {
        printf("Some of the threads have not started (0x%x) !\n", count);
        result = STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> total of thread start events: %d\n", eventsCount);
    }

    return result;
}

}
