/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jboolean framePopReceived = JNI_FALSE;

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


static void reportError(const char *msg, int err) {
    fprintf(stdout, "%s, error: %d\n", msg, err);
    fflush(stdout);
}


static void JNICALL
FramePop(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread,
         jmethodID method, jboolean wasPoppedByException)
{
    jvmtiError err;
    jclass cls = NULL;
    char* csig = NULL;
    char* name = NULL;
    char* sign = NULL;

    framePopReceived = JNI_TRUE;

    err = (*jvmti_env)->GetMethodDeclaringClass(jvmti_env, method, &cls);
    if (err != JVMTI_ERROR_NONE) {
        reportError("FramePop: GetMethodDeclaringClass failed", err);
    }
    err = (*jvmti_env)->GetClassSignature(jvmti_env, cls, &csig, NULL);
    if (err != JVMTI_ERROR_NONE) {
        reportError("FramePop: GetClassSignature failed", err);
    }
    err = (*jvmti_env)->GetMethodName(jvmti_env, method, &name, &sign, NULL);
    if (err != JVMTI_ERROR_NONE) {
        reportError("FramePop: GetMethodName failed", err);
    }
    fprintf(stdout, "FramePop event from method: %s %s%s\n", csig, name, sign);
    fflush(stdout);

    (*jvmti_env)->Deallocate(jvmti_env, (unsigned char*)csig);
    (*jvmti_env)->Deallocate(jvmti_env, (unsigned char*)name);
    (*jvmti_env)->Deallocate(jvmti_env, (unsigned char*)sign);
}

static
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    res = JNI_ENV_PTR(jvm)->GetEnv(JNI_ENV_ARG(jvm, (void **) &jvmti), JVMTI_VERSION_9);
    if (res != JNI_OK || jvmti == NULL) {
        reportError("GetEnv(JVMTI_VERSION_9) failed", res);
        return JNI_ERR;
    }
    err = (*jvmti)->GetPotentialCapabilities(jvmti, &caps);
    if (err != JVMTI_ERROR_NONE) {
        reportError("GetPotentialCapabilities failed", err);
        return JNI_ERR;
    }
    err = (*jvmti)->AddCapabilities(jvmti, &caps);
    if (err != JVMTI_ERROR_NONE) {
        reportError("AddCapabilities failed",err);
        return JNI_ERR;
    }
    err = (*jvmti)->GetCapabilities(jvmti, &caps);
    if (err != JVMTI_ERROR_NONE) {
        reportError("GetCapabilities failed", err);
        return JNI_ERR;
    }
    if (caps.can_generate_frame_pop_events) {
        callbacks.FramePop = &FramePop;
        err = (*jvmti)->SetEventCallbacks(jvmti, &callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            reportError("SetEventCallbacks failed", err);
            return JNI_ERR;
        }
    }
    return JNI_OK;
}

JNIEXPORT jboolean JNICALL
Java_NotifyFramePopTest_canGenerateFramePopEvents(JNIEnv *env, jclass cls) {
    return caps.can_generate_frame_pop_events ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_NotifyFramePopTest_setFramePopNotificationMode(JNIEnv *env, jclass cl, jboolean enable) {
    jvmtiEventMode mode = enable ? JVMTI_ENABLE : JVMTI_DISABLE;
    jvmtiError err = (*jvmti)->SetEventNotificationMode(jvmti, mode, JVMTI_EVENT_FRAME_POP, NULL);
    if (err != JVMTI_ERROR_NONE) {
        reportError("Failed to set notification mode for FRAME_POP events", err);
    }
}

JNIEXPORT void JNICALL
Java_NotifyFramePopTest_notifyFramePop(JNIEnv *env, jclass cls, jthread thread)
{
    jvmtiError err= (*jvmti)->NotifyFramePop(jvmti, thread, 1);
    if (err != JVMTI_ERROR_NONE) {
        reportError("NotifyFramePop failed", err);
    }
}

JNIEXPORT jboolean JNICALL
Java_NotifyFramePopTest_framePopReceived(JNIEnv *env, jclass cls) {
    jboolean result = framePopReceived;
    framePopReceived = JNI_FALSE;
    return result;
}

#ifdef __cplusplus
}
#endif
