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
#define WAIT_STEP 100
#define INCREMENT_LIMIT 1000
#define DELAY 100

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jrawMonitorID monitor;
static jrawMonitorID wait_lock;
static jint monitorCount = 0;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_rawmonenter004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_rawmonenter004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_rawmonenter004(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    return JNI_OK;
}

static void JNICALL
increment_thread(jvmtiEnv* jvmti, JNIEnv* jni, void *unused) {
    jvmtiError err;
    jint temp;
    int i, j;

    for (i = 0; i < INCREMENT_LIMIT; i++) {
        err = jvmti->RawMonitorEnter(monitor);
        if (err != JVMTI_ERROR_NONE) {
            printf("(RawMonitorEnter#test) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }

        temp = monitorCount;
        for (j = 0; j < DELAY; j++) ;
        monitorCount = temp + 1;

        err = jvmti->RawMonitorExit(monitor);
        if (err != JVMTI_ERROR_NONE) {
            printf("(RawMonitorExit#test) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RawMonitorEnter_rawmonenter004_check(JNIEnv *env, jclass cls, jobjectArray threads, jint wtime) {
    jvmtiError err;
    jsize i, threads_limit;
    jthread thr;
    jint wait_time = wtime * 60000;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    err = jvmti->CreateRawMonitor("test monitor", &monitor);
    if (err != JVMTI_ERROR_NONE) {
        printf("(CreateRawMonitor#test) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return STATUS_FAILED;
    }

    err = jvmti->CreateRawMonitor("wait lock", &wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(CreateRawMonitor#wait) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return STATUS_FAILED;
    }

    threads_limit = env->GetArrayLength(threads);

    if (printdump == JNI_TRUE) {
        printf(">>> starting %d threads ...\n", threads_limit);
    }

    for (i = 0; i < threads_limit; i++) {
        thr = env->GetObjectArrayElement(threads, i);
        err = jvmti->RunAgentThread(thr, increment_thread, NULL,
                                    JVMTI_THREAD_NORM_PRIORITY);
        if (err != JVMTI_ERROR_NONE) {
            printf("(RunDebugThread) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return STATUS_FAILED;
        }
    }

    for (i = 0; i < wait_time/WAIT_STEP &&
            monitorCount != INCREMENT_LIMIT * threads_limit; i++) {
        err = jvmti->RawMonitorEnter(wait_lock);
        if (err != JVMTI_ERROR_NONE) {
            printf("(RawMonitorEnter#wait) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
            break;
        }
        err = jvmti->RawMonitorWait(wait_lock, (jlong)WAIT_STEP);
        if (err != JVMTI_ERROR_NONE) {
            printf("(RawMonitorWait) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
            break;
        }
        err = jvmti->RawMonitorExit(wait_lock);
        if (err != JVMTI_ERROR_NONE) {
            printf("(RawMonitorExit#wait) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
            break;
        }
    }

    if (monitorCount != INCREMENT_LIMIT * threads_limit) {
        printf("Timeout value is reached, monitorCount expected: %d",
            INCREMENT_LIMIT * threads_limit);
        printf(", actual: %d\n", monitorCount);
        result = STATUS_FAILED;
    } else if (printdump == JNI_TRUE) {
        printf(">>> final monitorCount: %d\n", monitorCount);
    }

    return result;
}

}
