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
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static int eventsCount = 0;
static int eventsExpected = 0;
static const char *prefix = NULL;

void JNICALL ThreadEnd(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread) {
    jvmtiError err;
    jvmtiThreadInfo inf;
    char name[32];

    err = jvmti_env->GetThreadInfo(thread, &inf);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetThreadInfo#%d) unexpected error: %s (%d)\n",
               eventsCount, TranslateError(err), err);
        result = STATUS_FAILED;
    }
    if (printdump == JNI_TRUE) {
        printf(">>> %s\n", inf.name);
    }
    if (inf.name != NULL && strstr(inf.name, prefix) == inf.name) {
        eventsCount++;
        sprintf(name, "%s%d", prefix, eventsCount);
        if (inf.name == NULL || strcmp(name, inf.name) != 0) {
            printf("(#%d) wrong thread name: \"%s\"",
                   eventsCount, inf.name);
            printf(", expected: \"%s\"\n", name);
            result = STATUS_FAILED;
        }
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_threadend001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_threadend001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_threadend001(JavaVM *jvm, char *options, void *reserved) {
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

    callbacks.ThreadEnd = &ThreadEnd;
    err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    if (err != JVMTI_ERROR_NONE) {
        printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_ThreadEnd_threadend001_getReady(JNIEnv *env,
        jclass cls, jint i, jstring name) {
    jvmtiError err;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return;
    }

    prefix = env->GetStringUTFChars(name, NULL);
    if (prefix == NULL) {
        printf("Failed to copy UTF-8 string!\n");
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_THREAD_END, NULL);
    if (err == JVMTI_ERROR_NONE) {
        eventsExpected = i;
    } else {
        printf("Failed to enable JVMTI_EVENT_THREAD_END: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_ThreadEnd_threadend001_check(JNIEnv *env, jclass cls) {
    jvmtiError err;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_DISABLE,
            JVMTI_EVENT_THREAD_END, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to disable JVMTI_EVENT_THREAD_END: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (eventsCount != eventsExpected) {
        printf("Wrong number of thread end events: %d, expected: %d\n",
            eventsCount, eventsExpected);
        result = STATUS_FAILED;
    }
    return result;
}

}
