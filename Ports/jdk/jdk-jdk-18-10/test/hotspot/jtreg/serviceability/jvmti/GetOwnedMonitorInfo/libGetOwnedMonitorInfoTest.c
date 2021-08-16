/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

#define TEST_CLASS "GetOwnedMonitorInfoTest"

static volatile jboolean event_has_posted = JNI_FALSE;
static volatile jint status = PASSED;
static volatile jclass testClass = NULL;

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

static jboolean CheckLockObject(JNIEnv *env, jobject monitor) {
    if (testClass == NULL) {
        // JNI_OnLoad has not been called yet, so can't possibly be an instance of TEST_CLASS.
        return JNI_FALSE;
    }
    return (*env)->IsInstanceOf(env, monitor, testClass);
}

JNIEXPORT void JNICALL
MonitorContendedEnter(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jobject monitor) {
    jvmtiError err;
    jvmtiThreadInfo threadInfo;
    jint monitorCount;
    jobject *ownedMonitors;

    if (CheckLockObject(env, monitor) == JNI_FALSE) {
        return;
    }

    err = (*jvmti)->GetThreadInfo(jvmti, thread, &threadInfo);
    if (err != JVMTI_ERROR_NONE) {
        ShowErrorMessage(jvmti, err,
                         "MonitorContendedEnter: error in JVMTI GetThreadInfo");
        status = FAILED;
        event_has_posted = JNI_TRUE;
        return;
    }
    err = (*jvmti)->GetOwnedMonitorInfo(jvmti, thread, &monitorCount, &ownedMonitors);
    if (err != JVMTI_ERROR_NONE) {
        ShowErrorMessage(jvmti, err,
                         "MonitorContendedEnter: error in JVMTI GetOwnedMonitorInfo");
        status = FAILED;
        event_has_posted = JNI_TRUE;
        return;
    }

    printf("MonitorContendedEnter: %s owns %d monitor(s)\n",
           threadInfo.name, monitorCount);

    (*jvmti)->Deallocate(jvmti, (unsigned char *)ownedMonitors);
    (*jvmti)->Deallocate(jvmti, (unsigned char *)threadInfo.name);

    if (monitorCount != 0) {
        fprintf(stderr, "MonitorContendedEnter: FAIL: monitorCount should be zero.\n");
        status = FAILED;
    }

    event_has_posted = JNI_TRUE;
}

JNIEXPORT void JNICALL
MonitorContendedEntered(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jobject monitor) {
    jvmtiError err;
    jvmtiThreadInfo threadInfo;
    jint monitorCount;
    jobject *ownedMonitors;

    if (CheckLockObject(env, monitor) == JNI_FALSE) {
        return;
    }

    err = (*jvmti)->GetThreadInfo(jvmti, thread, &threadInfo);
    if (err != JVMTI_ERROR_NONE) {
        ShowErrorMessage(jvmti, err,
                         "MonitorContendedEntered: error in JVMTI GetThreadInfo");
        status = FAILED;
        return;
    }
    err = (*jvmti)->GetOwnedMonitorInfo(jvmti, thread, &monitorCount, &ownedMonitors);
    if (err != JVMTI_ERROR_NONE) {
        ShowErrorMessage(jvmti, err,
                         "MonitorContendedEntered: error in JVMTI GetOwnedMonitorInfo");
        status = FAILED;
        return;
    }

    printf("MonitorContendedEntered: %s owns %d monitor(s)\n",
           threadInfo.name, monitorCount);

    (*jvmti)->Deallocate(jvmti, (unsigned char *)ownedMonitors);
    (*jvmti)->Deallocate(jvmti, (unsigned char *)threadInfo.name);

    if (monitorCount != 1) {
        fprintf(stderr, "MonitorContendedEnter: FAIL: monitorCount should be one.\n");
        status = FAILED;
    }
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

    testClass = (*env)->FindClass(env, TEST_CLASS);
    if (testClass != NULL) {
      testClass = (*env)->NewGlobalRef(env, testClass);
    }
    if (testClass == NULL) {
        fprintf(stderr, "Error: Could not load class %s!\n", TEST_CLASS);
        return JNI_ERR;
    }

    return JNI_VERSION_9;
}

static
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;
    jvmtiEnv *jvmti;
    jvmtiCapabilities caps;
    jvmtiEventCallbacks callbacks;

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

    if (!caps.can_generate_monitor_events) {
        fprintf(stderr, "Warning: Monitor events are not implemented\n");
        return JNI_ERR;
    }
    if (!caps.can_get_owned_monitor_info) {
        fprintf(stderr, "Warning: GetOwnedMonitorInfo is not implemented\n");
        return JNI_ERR;
    }

    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.MonitorContendedEnter   = &MonitorContendedEnter;
    callbacks.MonitorContendedEntered = &MonitorContendedEntered;

    err = (*jvmti)->SetEventCallbacks(jvmti, &callbacks, sizeof(jvmtiEventCallbacks));
    if (err != JVMTI_ERROR_NONE) {
        ShowErrorMessage(jvmti, err,
                         "Agent_OnLoad: error in JVMTI SetEventCallbacks");
        return JNI_ERR;
    }

    err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE,
                                             JVMTI_EVENT_MONITOR_CONTENDED_ENTER, NULL);
    if (err != JVMTI_ERROR_NONE) {
        ShowErrorMessage(jvmti, err,
                         "Agent_OnLoad: error in JVMTI SetEventNotificationMode #1");
        return JNI_ERR;
    }
    err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE,
                                             JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, NULL);
    if (err != JVMTI_ERROR_NONE) {
        ShowErrorMessage(jvmti, err,
                         "Agent_OnLoad: error in JVMTI SetEventNotificationMode #2");
        return JNI_ERR;
    }
    printf("Agent_OnLoad finished\n");
    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_GetOwnedMonitorInfoTest_check(JNIEnv *env, jclass cls) {
    return status;
}

JNIEXPORT jboolean JNICALL
Java_GetOwnedMonitorInfoTest_hasEventPosted(JNIEnv *env, jclass cls) {
    return event_has_posted;
}

#ifdef __cplusplus
}
#endif
