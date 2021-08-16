/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

#ifdef __cplusplus
extern "C" {
#endif

#ifndef JNI_ENV_ARG

#ifdef __cplusplus
#define JNI_ENV_ARG(x, y) y
#define JNI_ENV_PTR(x) x
#else
#define JNI_ENV_ARG(x,y) x, y
#define JNI_ENV_PTR(x) (*x)
#endif

#endif

#define TranslateError(err) "JVMTI error"

#define PASSED 0
#define FAILED 2

static const char *EXC_CNAME = "java/lang/Exception";

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;

static int thread_start_events_vm_start = 0;

static jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved);

JNIEXPORT
jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT
jint JNICALL Agent_OnAttach(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT
jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    return JNI_VERSION_9;
}

static
jint throw_exc(JNIEnv *env, char *msg) {
    jclass exc_class = JNI_ENV_PTR(env)->FindClass(JNI_ENV_ARG(env, EXC_CNAME));

    if (exc_class == NULL) {
        printf("throw_exc: Error in FindClass(env, %s)\n", EXC_CNAME);
        return -1;
    }
    return JNI_ENV_PTR(env)->ThrowNew(JNI_ENV_ARG(env, exc_class), msg);
}


void JNICALL Callback_ThreadStart(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread) {
    jvmtiError err;
    jvmtiPhase phase;

    err = (*jvmti)->GetPhase(jvmti_env,&phase);
    if (err != JVMTI_ERROR_NONE) {
        printf("ThreadStart event: GetPhase error: %s (%d)\n", TranslateError(err), err);
        result = FAILED;
        return;
    }

    if (phase == JVMTI_PHASE_START) {
        thread_start_events_vm_start++;
    }

    if (printdump == JNI_TRUE) {
        printf(">>>    ThreadStart event: phase(%d)\n", phase);
    }
}

static
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res, size;
    jvmtiCapabilities caps;
    jvmtiEventCallbacks callbacks;
    jvmtiError err;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    res = JNI_ENV_PTR(jvm)->GetEnv(JNI_ENV_ARG(jvm, (void **) &jvmti),
        JVMTI_VERSION_9);
    if (res != JNI_OK || jvmti == NULL) {
        printf("    Error: wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    printf("Enabling following capability: can_generate_early_vmstart\n");
    memset(&caps, 0, sizeof(caps));
    caps.can_generate_early_vmstart = 1;

    err = (*jvmti)->AddCapabilities(jvmti, &caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("    Error in AddCapabilites: %s (%d)\n", TranslateError(err), err);
        return JNI_ERR;
    }

    size = (jint)sizeof(callbacks);

    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.ThreadStart = Callback_ThreadStart;

    err = (*jvmti)->SetEventCallbacks(jvmti, &callbacks, size);
    if (err != JVMTI_ERROR_NONE) {
        printf("    Error in SetEventCallbacks: %s (%d)\n", TranslateError(err), err);
        return JNI_ERR;
    }

    err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_THREAD_START, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("    Error in SetEventNotificationMode: %s (%d)\n", TranslateError(err), err);
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_MAAThreadStart_check(JNIEnv *env, jclass cls) {
    jobject loader = NULL;

    if (jvmti == NULL) {
        throw_exc(env, "JVMTI client was not properly loaded!\n");
        return FAILED;
    }

    /*
     * Expecting that ThreadStart events are sent during VM Start phase when
     * can_generate_early_vmstart capability is enabled.
     */
    if (thread_start_events_vm_start == 0) {
        throw_exc(env, "Didn't get ThreadStart events in VM early start phase!\n");
        return FAILED;
    }

    return result;
}

#ifdef __cplusplus
}
#endif
