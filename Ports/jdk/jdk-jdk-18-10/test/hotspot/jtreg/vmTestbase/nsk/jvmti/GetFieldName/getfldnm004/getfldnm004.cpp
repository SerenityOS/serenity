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
    jboolean is_static;
} field_info;

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static field_info fields[] = {
    { "staticBoolean", "Z", JNI_TRUE },
    { "staticByte", "B", JNI_TRUE },
    { "staticShort", "S", JNI_TRUE },
    { "staticInt", "I", JNI_TRUE },
    { "staticLong", "J", JNI_TRUE },
    { "staticFloat", "F", JNI_TRUE },
    { "staticDouble", "D", JNI_TRUE },
    { "staticChar", "C", JNI_TRUE },
    { "staticObject", "Ljava/lang/Object;", JNI_TRUE },
    { "staticArrInt", "[I", JNI_TRUE },

    { "instanceBoolean", "Z", JNI_FALSE },
    { "instanceByte", "B", JNI_FALSE },
    { "instanceShort", "S", JNI_FALSE },
    { "instanceInt", "I", JNI_FALSE },
    { "instanceLong", "J", JNI_FALSE },
    { "instanceFloat", "F", JNI_FALSE },
    { "instanceDouble", "D", JNI_FALSE },
    { "instanceChar", "C", JNI_FALSE },
    { "instanceObject", "Ljava/lang/Object;", JNI_FALSE },
    { "instanceArrInt", "[I", JNI_FALSE }
};

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getfldnm004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getfldnm004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getfldnm004(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetFieldName_getfldnm004_check(JNIEnv *env,
        jclass cls, jclass clazz) {
    jvmtiError err;
    jfieldID fid;
    char *name, *sig, *generic;
    size_t i;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    for (i = 0; i < sizeof(fields)/sizeof(field_info); i++) {
        if (fields[i].is_static == JNI_TRUE) {
            fid = env->GetStaticFieldID(
                clazz, fields[i].name, fields[i].sig);
        } else {
            fid = env->GetFieldID(
                clazz, fields[i].name, fields[i].sig);
        }
        if (fid == NULL) {
            printf("(%" PRIuPTR ") cannot get field ID for %s:\"%s\"\n",
                   i, fields[i].name, fields[i].sig);
            result = STATUS_FAILED;
            continue;
        }
        err = jvmti->GetFieldName(clazz, fid, &name, &sig, &generic);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetFieldName#%" PRIuPTR ") unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
            continue;
        }
        if (printdump == JNI_TRUE) {
            printf(">>> %" PRIuPTR " -- %s:\"%s\"\n", i, name, sig);
        }
        if (name == NULL || strcmp(name, fields[i].name) != 0) {
            printf("(%" PRIuPTR ") wrong field name: \"%s\"", i, name);
            printf(", expected: \"%s\"\n", fields[i].name);
            result = STATUS_FAILED;
        }
        if (sig == NULL || strcmp(sig, fields[i].sig) != 0) {
            printf("(%" PRIuPTR ") wrong field sig: \"%s\"", i, sig);
            printf(", expected: \"%s\"\n", fields[i].sig);
            result = STATUS_FAILED;
        }
    }

    return result;
}

}
