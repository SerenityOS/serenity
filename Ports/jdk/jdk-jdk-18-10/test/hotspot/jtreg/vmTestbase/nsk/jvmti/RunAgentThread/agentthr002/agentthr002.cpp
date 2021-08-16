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

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jrawMonitorID wait_lock;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_agentthr002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_agentthr002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_agentthr002(JavaVM *jvm, char *options, void *reserved) {
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
debug_thread(jvmtiEnv* jvmti, JNIEnv* jni, void *unused) {
    jvmtiError err;

    err = jvmti->RawMonitorEnter(wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorEnter) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    err = jvmti->RawMonitorWait(wait_lock, (jlong)0);
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

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RunAgentThread_agentthr002_check(JNIEnv *env, jclass cls, jthread thr) {
    jvmtiError err;
    jvmtiThreadInfo inf;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    err = jvmti->CreateRawMonitor("_wait_lock", &wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(CreateRawMonitor) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> starting debug thread ...\n");
    }
    err = jvmti->RunAgentThread(thr, debug_thread, NULL,
                                JVMTI_THREAD_NORM_PRIORITY);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RunAgentThread) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> getting info about it ...\n");
    }
    err = jvmti->GetThreadInfo(thr, &inf);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetThreadInfo) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (inf.is_daemon != JNI_TRUE) {
        printf("ERROR: thread is not a daemon thread!\n");
        result = STATUS_FAILED;
    }

    err = jvmti->RawMonitorEnter(wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorEnter) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    err = jvmti->RawMonitorNotify(wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorNotify) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    err = jvmti->RawMonitorExit(wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorExit) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> done ...\n");
    }

    return result;
}

}
