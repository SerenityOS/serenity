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


static jvmtiEnv *jvmti = NULL;

typedef enum {
    Yes = 1,
    No = 0,
    Error = -1
} IsAvail;

static IsAvail onLoadIsAvail = Error;

void reportError(const char *msg, int err) {
    printf("%s, error: %d\n", msg, err);
}

static IsAvail isClassHookAvail() {
    IsAvail result = Error;
    do {
        jvmtiCapabilities caps;
        jvmtiPhase phase;
        jvmtiError err;

        if (jvmti == NULL) {
            reportError("jvmti is NULL", -1);
            break;
        }

        err = (*jvmti)->GetPhase(jvmti, &phase);
        if (err != JVMTI_ERROR_NONE) {
            reportError("GetPhase failed", err);
            break;
        }

        err = (*jvmti)->GetPotentialCapabilities(jvmti, &caps);
        if (err != JVMTI_ERROR_NONE) {
            reportError("GetPotentialCapabilities failed", err);
            break;
        }

        result = caps.can_generate_all_class_hook_events ? Yes : No;

        printf("isClassHookAvail: phase=%d, value=%d\n", phase, result);
    } while (0);
    return result;
}


JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *jvm, char *options, void *reserved)
{
    jint res = JNI_ENV_PTR(jvm)->GetEnv(JNI_ENV_ARG(jvm, (void **) &jvmti), JVMTI_VERSION_9);
    if (res != JNI_OK || jvmti == NULL) {
        reportError("GetEnv failed", res);
        return JNI_ERR;
    }

    // check and save can_generate_all_class_hook_events for ONLOAD phase
    onLoadIsAvail = isClassHookAvail();

    return JNI_OK;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    return JNI_VERSION_9;
}


JNIEXPORT jint JNICALL
Java_CanGenerateAllClassHook_getClassHookAvail(JNIEnv *env, jclass cls) {
    return isClassHookAvail();
}

JNIEXPORT jint JNICALL
Java_CanGenerateAllClassHook_getOnLoadClassHookAvail(JNIEnv *env, jclass cls) {
    return onLoadIsAvail;
}


#ifdef __cplusplus
}
#endif

