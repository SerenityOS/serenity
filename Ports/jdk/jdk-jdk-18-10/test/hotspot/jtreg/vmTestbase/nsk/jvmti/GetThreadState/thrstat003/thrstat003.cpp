/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
#define WAIT_START 100

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jint wait_time = 0;
static jint state[] = {
    0,                               /*  JVMTI_THREAD_STATUS_NOT_STARTED, */
    JVMTI_THREAD_STATE_SLEEPING,
    JVMTI_THREAD_STATE_TERMINATED    /*  JVMTI_THREAD_STATUS_ZOMBIE */
};

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetThreadState_thrstat003_init(JNIEnv *env, jclass cls,
        jint waitTime) {
    wait_time = waitTime * 60000;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_thrstat003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_thrstat003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_thrstat003(JavaVM *jvm, char *options, void *reserved) {
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

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetThreadState_thrstat003_check(JNIEnv *env, jclass cls,
        jthread thr, jint statInd) {
    jvmtiError err;
    jrawMonitorID wait_lock;
    jint thrState;
    jint i;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    err = jvmti->CreateRawMonitor("_wait_lock", &wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(CreateRawMonitor#%d) unexpected error: %s (%d)\n",
               statInd, TranslateError(err), err);
        result = STATUS_FAILED;
    }

    for (i = WAIT_START; i < wait_time; i <<= 1) {
        err = jvmti->GetThreadState(thr, &thrState);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetThreadState#%d) unexpected error: %s (%d)\n",
                statInd, TranslateError(err), err);
            result = STATUS_FAILED;
        }

        if (printdump == JNI_TRUE) {
            printf(">>> thread state: %s (%d)\n",
                TranslateState(thrState), thrState);
        }

        if ((thrState & JVMTI_THREAD_STATE_RUNNABLE) == 0) {
            break;
        }

        err = jvmti->RawMonitorEnter(wait_lock);
        if (err != JVMTI_ERROR_NONE) {
            printf("(RawMonitorEnter) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
        err = jvmti->RawMonitorWait(wait_lock, (jlong)i);
        if (err != JVMTI_ERROR_NONE) {
            printf("(RawMonitorWait) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
        err = jvmti->RawMonitorExit(wait_lock);
        if (err != JVMTI_ERROR_NONE) {
            printf("(RawMonitorExit) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }

    err = jvmti->DestroyRawMonitor(wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(DestroyRawMonitor#%d) unexpected error: %s (%d)\n",
               statInd, TranslateError(err), err);
        result = STATUS_FAILED;
    }

    /* We expect that thread is NOT_STARTED if statInd == 0 */
    if (statInd == 0 && thrState != state[statInd]) {
        result = STATUS_FAILED;
    } else if (statInd != 0 && (thrState & state[statInd]) == 0) {
        result = STATUS_FAILED;
    }
    if (result == STATUS_FAILED) {
        printf("Wrong state: %s (%d)\n", TranslateState(thrState), thrState);
        printf("   expected: %s (%d)\n",
            TranslateState(state[statInd]), state[statInd]);
    }

    return result;
}

}
