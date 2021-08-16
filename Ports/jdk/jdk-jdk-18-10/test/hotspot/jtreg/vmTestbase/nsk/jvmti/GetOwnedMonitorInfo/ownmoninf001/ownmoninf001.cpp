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

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jint result = PASSED;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ownmoninf001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ownmoninf001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ownmoninf001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv !\n");
        return JNI_ERR;
    }

    err = jvmti->GetPotentialCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetPotentialCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->AddCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(AddCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->GetCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    if (!caps.can_get_owned_monitor_info) {
        printf("Warning: GetOwnedMonitorInfo is not implemented\n");
    }

    return JNI_OK;
}

jobject *getInfo(JNIEnv *env, jint point, jthread thr, int count) {
    jvmtiError err;
    jint owned_monitor_count;
    jobject *owned_monitors = NULL;

    err = jvmti->GetOwnedMonitorInfo(thr,
        &owned_monitor_count, &owned_monitors);
    if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY &&
            !caps.can_get_owned_monitor_info) {
        /* Ok, it's expected */
    } else if (err != JVMTI_ERROR_NONE) {
        printf("(GetOwnedMonitorInfo#%d) unexpected error: %s (%d)\n",
               point, TranslateError(err), err);
        result = STATUS_FAILED;
        return NULL;
    } else {
        if (owned_monitor_count != count) {
            result = STATUS_FAILED;
            printf("Point %d: number of owned monitors expected: %d, got: %d\n",
                   point, count, owned_monitor_count);
            return NULL;
        }
    }
    return owned_monitors;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetOwnedMonitorInfo_ownmoninf001_checkMon0(JNIEnv *env,
        jclass cls, jint point, jthread thr) {
    getInfo(env, point, thr, 0);
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetOwnedMonitorInfo_ownmoninf001_checkMon1(JNIEnv *env,
        jclass cls, jint point, jthread thr, jobject lock) {
    jobject *monitors;
    monitors = getInfo(env, point, thr, 1);
    if (monitors == NULL) {
        return;
    }
    if (!env->IsSameObject(lock, monitors[0])) {
        result = STATUS_FAILED;
        printf("Point %d: not expected monitor: 0x%p\n", point, monitors[0]);
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetOwnedMonitorInfo_ownmoninf001_checkMon2(JNIEnv *env,
        jclass cls, jint point, jthread thr, jobject lock1, jobject lock2) {
    jobject *monitors;
    monitors = getInfo(env, point, thr, 2);
    if (monitors == NULL) {
        return;
    }
    if (!env->IsSameObject(lock1, monitors[0]) && !env->IsSameObject(lock2, monitors[0])) {
        result = STATUS_FAILED;
        printf("Point %d: not expected monitor: 0x%p\n", point, monitors[0]);
    }
    if ((!env->IsSameObject(lock1, monitors[1]) && !env->IsSameObject(lock2, monitors[1]))
        || env->IsSameObject(monitors[0], monitors[1])) {
        result = STATUS_FAILED;
        printf("Point %d: not expected monitor: 0x%p\n", point, monitors[1]);
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetOwnedMonitorInfo_ownmoninf001_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
