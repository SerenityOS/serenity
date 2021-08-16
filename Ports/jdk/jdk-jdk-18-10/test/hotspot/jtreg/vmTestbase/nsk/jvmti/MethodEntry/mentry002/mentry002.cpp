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
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static int MethodEntriesExpected = 0;
static int MethodExitsExpected = 0;
static int MethodEntriesCount = 0;
static int MethodExitsCount = 0;
static jmethodID mid = NULL;

void JNICALL MethodEntry(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method) {
    if (mid == method) {
        MethodEntriesCount++;
    }
}

void JNICALL MethodExit(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method,
        jboolean was_poped_by_exc, jvalue return_value) {
    if (mid == method) {
        MethodExitsCount++;
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_mentry002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_mentry002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_mentry002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv!\n");
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

    if (caps.can_generate_method_entry_events &&
            caps.can_generate_method_exit_events) {
        callbacks.MethodEntry = &MethodEntry;
        callbacks.MethodExit = &MethodExit;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: MethodEntry or MethodExit event is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_MethodEntry_mentry002_getReady(JNIEnv *env, jclass cls, jint i) {
    jvmtiError err;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return;
    }

    if (!caps.can_generate_method_entry_events ||
            !caps.can_generate_method_exit_events) {
        return;
    }

    mid = env->GetStaticMethodID(cls, "emptyMethod", "()V");
    if (mid == NULL) {
        printf("Cannot find Method ID for emptyMethod\n");
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_METHOD_ENTRY, NULL);
    if (err == JVMTI_ERROR_NONE) {
        MethodEntriesExpected = i;
    } else {
        printf("Failed to enable JVMTI_EVENT_METHOD_ENTRY event: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_METHOD_EXIT, NULL);
    if (err == JVMTI_ERROR_NONE) {
        MethodExitsExpected = i;
    } else {
        printf("Failed to enable JVMTI_EVENT_METHOD_EXIT event: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_MethodEntry_mentry002_check(JNIEnv *env, jclass cls) {
    if (printdump == JNI_TRUE) {
        printf(">>> MethodEntry events: %d, MethodExit events: %d\n",
            MethodEntriesCount, MethodExitsCount);
    }
    if (MethodEntriesCount != MethodEntriesExpected) {
        printf("Wrong number of method entry events: %d, expected: %d\n",
            MethodEntriesCount, MethodEntriesExpected);
        result = STATUS_FAILED;
    }
    if (MethodExitsCount != MethodExitsExpected) {
        printf("Wrong number of method exit events: %d, expected: %d\n",
            MethodExitsCount, MethodExitsExpected);
        result = STATUS_FAILED;
    }
    return result;
}

}
