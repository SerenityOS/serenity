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
#include <inttypes.h>
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

typedef struct {
    const char *name;
    const char *sig;
    jboolean is_synthetic;
} field_info;

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static field_info fields[] = {
    { "fld", "I", JNI_FALSE },
    { "this$0", "Lnsk/jvmti/IsFieldSynthetic/isfldsin003a;", JNI_TRUE }
};

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_isfldsin003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_isfldsin003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_isfldsin003(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
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

    if (!caps.can_get_synthetic_attribute) {
        printf("Warning: IsFieldSynthetic is not implemented\n");
    }

    return JNI_OK;
}

char const *jbooleanToString(jboolean flag) {
    return ((flag == JNI_TRUE) ? "true" : "false");
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_IsFieldSynthetic_isfldsin003a_check(JNIEnv *env,
        jclass cls, jclass clazz) {
    jvmtiError err;
    jfieldID fid;
    jboolean isSynthetic;
    size_t i;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    for (i = 0; i < sizeof(fields) / sizeof(field_info); i++) {
        fid = env->GetFieldID(clazz, fields[i].name, fields[i].sig);
        if (fid == NULL) {
            printf("(%" PRIuPTR ") cannot get field ID for %s:\"%s\"\n",
                   i, fields[i].name, fields[i].sig);
            result = STATUS_FAILED;
            continue;
        }

        err = jvmti->IsFieldSynthetic(clazz, fid, &isSynthetic);
        if (err != JVMTI_ERROR_NONE) {
            printf("(IsFieldSynthetic#%" PRIuPTR ") unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
        }

        if (printdump == JNI_TRUE) {
            printf(">>> %s:\"%s\", isSynthetic: %s\n",
                   fields[i].name, fields[i].sig,
                   jbooleanToString(isSynthetic));
        }

        if (isSynthetic != fields[i].is_synthetic) {
            printf("%s:\"%s\"\n\t - isSynthetic expected: %s, actual: %s\n",
                   fields[i].name, fields[i].sig,
                   jbooleanToString(fields[i].is_synthetic),
                   jbooleanToString(isSynthetic));
            result = STATUS_FAILED;
        }
    }

    return result;
}

}
