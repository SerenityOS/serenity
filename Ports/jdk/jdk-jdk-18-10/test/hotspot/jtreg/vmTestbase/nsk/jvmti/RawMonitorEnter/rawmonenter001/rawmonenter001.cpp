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
#define RAW_MONITORS_NUMBER 1024

static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jrawMonitorID monitors[RAW_MONITORS_NUMBER];

void JNICALL VMInit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr) {
    jvmtiError err;
    char name[32];
    int i;

    if (printdump == JNI_TRUE) {
        printf(">>> VMInit event\n");
        printf(">>> creating %d raw monitors\n", RAW_MONITORS_NUMBER);
    }

    for (i = 0; i < RAW_MONITORS_NUMBER; i++) {
        sprintf(name, "RawMonitor-%d", i);
        err = jvmti->CreateRawMonitor(name, &monitors[i]);
        if (err != JVMTI_ERROR_NONE) {
            printf("(CreateRawMonitor#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }

    if (printdump == JNI_TRUE) {
        printf(">>> entering %d raw monitors\n", RAW_MONITORS_NUMBER);
    }

    for (i = 0; i < RAW_MONITORS_NUMBER; i++) {
        err = jvmti->RawMonitorEnter(monitors[i]);
        if (err != JVMTI_ERROR_NONE) {
            printf("(EnterRawMonitor#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }

    if (printdump == JNI_TRUE) {
        printf(">>> destroying %d raw monitors\n", RAW_MONITORS_NUMBER);
    }

    for (i = 0; i < RAW_MONITORS_NUMBER; i++) {
        err = jvmti->DestroyRawMonitor(monitors[i]);
        if (err != JVMTI_ERROR_NONE) {
            printf("(DestroyRawMonitor#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_rawmonenter001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_rawmonenter001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_rawmonenter001(JavaVM *jvm, char *options, void *reserved) {
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

    callbacks.VMInit = &VMInit;
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

JNIEXPORT jint JNICALL Java_nsk_jvmti_RawMonitorEnter_rawmonenter001_check(JNIEnv *env, jclass cls) {
    jvmtiError err;
    char name[32];
    int i;

    if (printdump == JNI_TRUE) {
        printf(">>> native call\n");
        printf(">>> creating %d raw monitors\n", RAW_MONITORS_NUMBER);
    }

    for (i = 0; i < RAW_MONITORS_NUMBER; i++) {
        sprintf(name, "RawMonitor-%d", i);
        err = jvmti->CreateRawMonitor(name, &monitors[i]);
        if (err != JVMTI_ERROR_NONE) {
            printf("(CreateRawMonitor#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }

    if (printdump == JNI_TRUE) {
        printf(">>> entering %d raw monitors\n", RAW_MONITORS_NUMBER);
    }

    for (i = 0; i < RAW_MONITORS_NUMBER; i++) {
        err = jvmti->RawMonitorEnter(monitors[i]);
        if (err != JVMTI_ERROR_NONE) {
            printf("(EnterRawMonitor#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }

    if (printdump == JNI_TRUE) {
        printf(">>> destroying %d raw monitors\n", RAW_MONITORS_NUMBER);
    }

    for (i = 0; i < RAW_MONITORS_NUMBER; i++) {
        err = jvmti->DestroyRawMonitor(monitors[i]);
        if (err != JVMTI_ERROR_NONE) {
            printf("(DestroyRawMonitor#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }

    return result;
}

}
