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


#define STATUS_FAILED 2
#define PASSED 0

static jvmtiEnv *jvmti;
static jvmtiCapabilities caps;
static int result = PASSED;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_srcdebugex002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_srcdebugex002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_srcdebugex002(JavaVM *jvm, char *options, void *reserved) {
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

    if (!caps.can_get_source_debug_extension) {
        printf("Warning: GetSourceDebugExtension is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetSourceDebugExtension_srcdebugex002_getSrcDebugX(JNIEnv *env,
        jclass cls, jint t_case) {
    jvmtiError err;
    char *srcDebugX = NULL;
    jobject invalCls; /* used as invalid class */

    if (!caps.can_get_source_debug_extension) return result;

    switch (t_case) {
        case 1: /* NULL pointer in test debug mode */
            printf("\nInvoke GetSourceDebugExtension() with NULL pointer...\n");
            fflush(stdout);
            // fallthrough
        case 0: /* NULL pointer */
            err = jvmti->GetSourceDebugExtension(cls, NULL);
            if (err != JVMTI_ERROR_NULL_POINTER) {
                printf("TEST FAILED: the function GetSourceDebugExtension() returned the error %s (%d)\n",
                       TranslateError(err), err);
                printf("\tBut it should return the error JVMTI_ERROR_NULL_POINTER.\n");
                result = STATUS_FAILED;
            }
            break;
        case 3: /* invalid class in test debug mode */
            printf("\nInvoke GetSourceDebugExtension() for an invalid class...\n");
            fflush(stdout);
            // fallthrough
        case 2: /* invalid class */
            err = jvmti->GetClassLoader(cls, &invalCls);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetClassLoader) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            } else {
                err = jvmti->GetSourceDebugExtension((jclass) invalCls, &srcDebugX);
                if (err != JVMTI_ERROR_INVALID_CLASS) {
                    printf("TEST FAILED: the function GetSourceDebugExtension() returned the error %s (%d)\n",
                           TranslateError(err), err);
                    printf("\tBut it should return the error JVMTI_ERROR_INVALID_CLASS.\n");
                    result = STATUS_FAILED;
                }
            }
            break;
        default:
            break;
    }
    return result;
}

}
