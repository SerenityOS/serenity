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
#include "jni.h"

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

#define PASSED 0
#define FAILED 2

#define TEST_CLASS  "GetOwnedMonitorStackDepthInfoTest"
#define LOCK1_CLASS "GetOwnedMonitorStackDepthInfoTest$Lock1"
#define LOCK2_CLASS "GetOwnedMonitorStackDepthInfoTest$Lock2"

#define TEST_OBJECT_LOCK_DEPTH 2
#define LOCK1_DEPTH 3
#define LOCK2_DEPTH 1
#define EXP_MONITOR_COUNT 3

static jvmtiEnv *jvmti;

static jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved);

static void ShowErrorMessage(jvmtiEnv *jvmti, jvmtiError errCode, const char *message) {
    char *errMsg;
    jvmtiError result;

    result = (*jvmti)->GetErrorName(jvmti, errCode, &errMsg);
    if (result == JVMTI_ERROR_NONE) {
        fprintf(stderr, "%s: %s (%d)\n", message, errMsg, errCode);
        (*jvmti)->Deallocate(jvmti, (unsigned char *)errMsg);
    } else {
        fprintf(stderr, "%s (%d)\n", message, errCode);
    }
}

static jboolean CheckLockObject(JNIEnv *env, jobject monitor, jclass testClass) {
    return (*env)->IsInstanceOf(env, monitor, testClass);
}

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT jint JNICALL
Agent_OnAttach(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *jvm, void *reserved) {
    jint res;
    JNIEnv *env;

    res = JNI_ENV_PTR(jvm)->GetEnv(JNI_ENV_ARG(jvm, (void **) &env),
                                   JNI_VERSION_9);
    if (res != JNI_OK || env == NULL) {
        fprintf(stderr, "Error: GetEnv call failed(%d)!\n", res);
        return JNI_ERR;
    }

    return JNI_VERSION_9;
}

static
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;
    jvmtiCapabilities caps;

    printf("Agent_OnLoad started\n");

    res = JNI_ENV_PTR(jvm)->GetEnv(JNI_ENV_ARG(jvm, (void **) &jvmti),
                                   JVMTI_VERSION_9);
    if (res != JNI_OK || jvmti == NULL) {
        fprintf(stderr, "Error: wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    err = (*jvmti)->GetPotentialCapabilities(jvmti, &caps);
    if (err != JVMTI_ERROR_NONE) {
        ShowErrorMessage(jvmti, err,
                         "Agent_OnLoad: error in JVMTI GetPotentialCapabilities");
        return JNI_ERR;
    }

    err = (*jvmti)->AddCapabilities(jvmti, &caps);
    if (err != JVMTI_ERROR_NONE) {
        ShowErrorMessage(jvmti, err,
                         "Agent_OnLoad: error in JVMTI AddCapabilities");
        return JNI_ERR;
    }

    err = (*jvmti)->GetCapabilities(jvmti, &caps);
    if (err != JVMTI_ERROR_NONE) {
        ShowErrorMessage(jvmti, err,
                         "Agent_OnLoad: error in JVMTI GetCapabilities");
        return JNI_ERR;
    }

    if (!caps.can_get_owned_monitor_stack_depth_info) {
        fprintf(stderr, "Warning: GetOwnedMonitorStackDepthInfo is not implemented\n");
        return JNI_ERR;
    }

    printf("Agent_OnLoad finished\n");
    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_GetOwnedMonitorStackDepthInfoTest_verifyOwnedMonitors(JNIEnv *env, jclass cls) {
    jthread thread;
    jvmtiError err;
    jvmtiThreadInfo threadInfo;
    jint monitorCount;
    jvmtiMonitorStackDepthInfo* stackDepthInfo;
    jclass testClass;
    jclass lock1Class;
    jclass lock2Class;

    jint status = PASSED;

    jint idx = 0;

    testClass = (*env)->FindClass(env, TEST_CLASS);
    if (testClass == NULL) {
        fprintf(stderr, "Error: Could not load class %s!\n", TEST_CLASS);
        return FAILED;
    }

    lock1Class = (*env)->FindClass(env, LOCK1_CLASS);
    if (lock1Class == NULL) {
        fprintf(stderr, "Error: Could not load class %s!\n", LOCK1_CLASS);
        return FAILED;
    }

    lock2Class = (*env)->FindClass(env, LOCK2_CLASS);
    if (lock2Class == NULL) {
        fprintf(stderr, "Error: Could not load class %s!\n", LOCK2_CLASS);
        return FAILED;
    }

    err = (*jvmti) -> GetCurrentThread(jvmti, &thread);
    if (err != JVMTI_ERROR_NONE) {
        ShowErrorMessage(jvmti, err,
                "VerifyOwnedMonitors: error in JVMTI GetCurrentThread");
        return FAILED;
    }
    err = (*jvmti)->GetThreadInfo(jvmti, thread, &threadInfo);
    if (err != JVMTI_ERROR_NONE) {
        ShowErrorMessage(jvmti, err,
                "VerifyOwnedMonitors: error in JVMTI GetThreadInfo");
        return FAILED;
    }

    err = (*jvmti)->GetOwnedMonitorStackDepthInfo(jvmti, thread, &monitorCount, &stackDepthInfo);
    if (err != JVMTI_ERROR_NONE) {
        ShowErrorMessage(jvmti, err,
                "VerifyOwnedMonitors: error in JVMTI GetOwnedMonitorStackDepthInfo");
        return FAILED;
    }

    printf("VerifyOwnedMonitors: %s owns %d monitor(s)\n", threadInfo.name, monitorCount);

    if (monitorCount != EXP_MONITOR_COUNT) {
        fprintf(stderr, "VerifyOwnedMonitors: FAIL: invalid monitorCount, expected: %d, found: %d.\n",
                EXP_MONITOR_COUNT, monitorCount);
        status = FAILED;
    }
    for (idx = 0; idx < monitorCount; idx++) {
        if (CheckLockObject(env, stackDepthInfo[idx].monitor, testClass) == JNI_TRUE) {
            if (stackDepthInfo[idx].stack_depth != TEST_OBJECT_LOCK_DEPTH) {
                fprintf(stderr, "VerifyOwnedMonitors: FAIL: invalid stack_depth for %s monitor, expected: %d, found: %d.\n",
                        TEST_CLASS, TEST_OBJECT_LOCK_DEPTH, stackDepthInfo[idx].stack_depth);
                status = FAILED;
            }
        } else if (CheckLockObject(env, stackDepthInfo[idx].monitor, lock1Class) == JNI_TRUE) {
            if (stackDepthInfo[idx].stack_depth != LOCK1_DEPTH) {
                fprintf(stderr, "VerifyOwnedMonitors: FAIL: invalid stack_depth for %s monitor, expected: %d, found: %d.\n",
                        LOCK1_CLASS, LOCK1_DEPTH, stackDepthInfo[idx].stack_depth);
                status = FAILED;
            }
        } else if (CheckLockObject(env, stackDepthInfo[idx].monitor, lock2Class) == JNI_TRUE) {
            if (stackDepthInfo[idx].stack_depth != LOCK2_DEPTH) {
                fprintf(stderr, "VerifyOwnedMonitors: FAIL: invalid stack_depth for %s monitor, expected: %d, found: %d.\n",
                        LOCK2_CLASS, LOCK2_DEPTH, stackDepthInfo[idx].stack_depth);
                status = FAILED;
            }
        } else {
            fprintf(stderr, "VerifyOwnedMonitors: "
                    "FAIL: monitor should be instance of %s, %s, or %s\n", TEST_CLASS, LOCK1_CLASS, LOCK2_CLASS);
            status = FAILED;

        }
    }

    (*jvmti)->Deallocate(jvmti, (unsigned char *) stackDepthInfo);
    (*jvmti)->Deallocate(jvmti, (unsigned char *) threadInfo.name);
    return status;
}

#ifdef __cplusplus
}
#endif
