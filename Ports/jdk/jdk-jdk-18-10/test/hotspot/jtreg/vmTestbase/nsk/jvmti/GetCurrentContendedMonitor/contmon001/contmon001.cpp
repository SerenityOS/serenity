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

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_contmon001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_contmon001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_contmon001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiError err;
    jint res;

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv !\n");
        return JNI_ERR;
    }

    err = jvmti->GetCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    if (!caps.can_get_current_contended_monitor) {
        /* GetCurrentContendedMonitor is not currently available, but
         * is it potentially available?
         */
        err = jvmti->GetPotentialCapabilities(&caps);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetPotentialCapabilities) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
        if (caps.can_get_current_contended_monitor) {
            /* Yes, GetCurrentContendedMonitor is potentially available.
             * Let's turn it on!
             */
            memset(&caps, 0, sizeof(jvmtiCapabilities));
            caps.can_get_current_contended_monitor = 1;
            err = jvmti->AddCapabilities(&caps);
            if (err != JVMTI_ERROR_NONE) {
                printf("(AddCapabilities) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                return JNI_ERR;
            }
        } else {
            printf("Warning: GetCurrentContendedMonitor is not implemented\n");
        }
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL Java_nsk_jvmti_GetCurrentContendedMonitor_contmon001_checkMon(JNIEnv *env, jclass cls,
        jint point, jthread thr, jobject lock) {
    jvmtiError err;
    jobject mon = NULL;

    err = jvmti->GetCurrentContendedMonitor(thr, &mon);
    if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY &&
            !caps.can_get_current_contended_monitor) {
        /* It's OK */
    } else if (err == JVMTI_ERROR_THREAD_NOT_ALIVE && point == 5) {
        return; /* Ok, it must be a dead thread */
    } else if (err != JVMTI_ERROR_NONE) {
        printf("(GetCurrentContendedMonitor#%d) unexpected error: %s (%d)\n",
               point, TranslateError(err), err);
        result = STATUS_FAILED;
    } else if (env->IsSameObject(lock, mon) == JNI_FALSE) {
        printf("(IsSameObject#%d) unexpected monitor object: 0x%p\n",
               point, mon);
        result = STATUS_FAILED;
    }
}

JNIEXPORT jint JNICALL Java_nsk_jvmti_GetCurrentContendedMonitor_contmon001_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
