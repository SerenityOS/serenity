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
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static int count = 0;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_objmonusage004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_objmonusage004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_objmonusage004(JavaVM *jvm, char *options, void *reserved) {
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

    if (!caps.can_get_monitor_info) {
        printf("Warning: GetObjectMonitorUsage is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetObjectMonitorUsage_objmonusage004_check(JNIEnv *env,
        jclass cls, jobject obj, jthread owner,
        jint entryCount, jint waiterCount) {
    jvmtiError err;
    jvmtiMonitorUsage inf;
    jvmtiThreadInfo tinf;
    int j;

    count++;

    err = jvmti->GetObjectMonitorUsage(obj, &inf);
    if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY &&
            !caps.can_get_monitor_info) {
        /* Ok, it's expected */
        return;
    } else if (err != JVMTI_ERROR_NONE) {
        printf("(GetMonitorInfo#%d) unexpected error: %s (%d)\n",
               count, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        if (inf.owner == NULL) {
            printf(">>> [%2d]    owner: none (0x0)\n", count);
        } else {
            err = jvmti->GetThreadInfo(inf.owner, &tinf);
            printf(">>> [%2d]    owner: %s (0x%p)\n",
                   count, tinf.name, inf.owner);
        }
        printf(">>>   entry_count: %d\n", inf.entry_count);
        printf(">>>  waiter_count: %d\n", inf.waiter_count);
        if (inf.waiter_count > 0) {
            printf(">>>       waiters:\n");
            for (j = 0; j < inf.waiter_count; j++) {
                err = jvmti->GetThreadInfo(inf.waiters[j], &tinf);
                printf(">>>                %2d: %s (0x%p)\n",
                       j, tinf.name, inf.waiters[j]);
            }
        }
    }

    if (!env->IsSameObject(owner, inf.owner)) {
        printf("(%d) unexpected owner: 0x%p\n", count, inf.owner);
        result = STATUS_FAILED;
    }

    if (inf.entry_count != entryCount) {
        printf("(%d) entry_count expected: %d, actually: %d\n",
               count, entryCount, inf.entry_count);
        result = STATUS_FAILED;
    }

    if (inf.waiter_count != waiterCount) {
        printf("(%d) waiter_count expected: %d, actually: %d\n",
               count, waiterCount, inf.waiter_count);
        result = STATUS_FAILED;
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetObjectMonitorUsage_objmonusage004_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
