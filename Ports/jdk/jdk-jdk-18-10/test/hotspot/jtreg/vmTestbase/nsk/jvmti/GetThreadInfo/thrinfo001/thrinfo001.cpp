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


#define PASSED  0
#define STATUS_FAILED  2

typedef struct {
    const char *name;
    int priority;
    int is_daemon;
} info;

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jvmtiThreadInfo inf;
static info threads[] = {
    { "main", JVMTI_THREAD_NORM_PRIORITY, 0 },
    { "thread1", JVMTI_THREAD_MIN_PRIORITY + 2, 1 },
    { "Thread-", JVMTI_THREAD_MIN_PRIORITY, 1 }
};

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_thrinfo001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_thrinfo001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_thrinfo001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv !\n");
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL Java_nsk_jvmti_GetThreadInfo_thrinfo001_checkInfo(JNIEnv *env, jclass cls,
        jthread thr, jthreadGroup group, jint ind) {
    jvmtiError err;

    err = jvmti->GetThreadInfo(thr, &inf);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetThreadInfo#%d) unexpected error: %s (%d)\n",
            ind, TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (inf.name == NULL ||
            strstr(inf.name, threads[ind].name) != inf.name ||
            (ind < 2 && strlen(inf.name) != strlen(threads[ind].name))) {
        printf("Thread %s: incorrect name: %s\n", threads[ind].name, inf.name);
        result = STATUS_FAILED;
    }
    if (inf.priority != threads[ind].priority) {
        printf("Thread %s: priority expected: %d, got: %d\n",
            threads[ind].name, threads[ind].priority, inf.priority);
        result = STATUS_FAILED;
    }
    if (inf.is_daemon != threads[ind].is_daemon) {
        printf("Thread %s: is_daemon expected: %d, got: %d\n",
           threads[ind].name, threads[ind].is_daemon, inf.is_daemon);
        result = STATUS_FAILED;
    }
    if (!env->IsSameObject(group, inf.thread_group)) {
        printf("Thread %s: invalid thread group\n", threads[ind].name);
        result = STATUS_FAILED;
    }
}

JNIEXPORT jint JNICALL Java_nsk_jvmti_GetThreadInfo_thrinfo001_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
