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
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jvmtiCapabilities caps;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getsrcfn005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getsrcfn005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getsrcfn005(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiError res;
    jint code;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    code = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (code != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    res = jvmti->GetPotentialCapabilities(&caps);
    if (res != JVMTI_ERROR_NONE) {
        printf("(GetPotentialCapabilities) unexpected error: %s (%d)\n",
               TranslateError(res), res);
        return JNI_ERR;
    }

    res = jvmti->AddCapabilities(&caps);
    if (res != JVMTI_ERROR_NONE) {
        printf("(AddCapabilities) unexpected error: %s (%d)\n",
               TranslateError(res), res);
        return JNI_ERR;
    }

    res = jvmti->GetCapabilities(&caps);
    if (res != JVMTI_ERROR_NONE) {
        printf("(GetCapabilities) unexpected error: %s (%d)\n",
               TranslateError(res), res);
        return JNI_ERR;
    }

    if (!caps.can_get_source_file_name) {
        printf("Warning: Not implemented capability can_get_source_file_name\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetSourceFileName_getsrcfn005_check(JNIEnv *env, jclass cls, jint i, jclass clazz) {
    jvmtiError err;
    char *name;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> trying #%d\n", i);
    }

    err = jvmti->GetSourceFileName(clazz, &name);
    if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY && caps.can_get_source_file_name) {
        printf("Error expected: JVMTI_ERROR_ABSENT_INFORMATION,\n");
        printf("\tactual: %s (%d)\n", TranslateError(err), err);
        result = STATUS_FAILED;
    } else if (err != JVMTI_ERROR_ABSENT_INFORMATION) {
        printf("(%d) error expected: JVMTI_ERROR_ABSENT_INFORMATION,\n", i);
        printf("\tactual: %s (%d)\n", TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

JNIEXPORT int JNICALL Java_nsk_jvmti_GetSourceFileName_getsrcfn005_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
