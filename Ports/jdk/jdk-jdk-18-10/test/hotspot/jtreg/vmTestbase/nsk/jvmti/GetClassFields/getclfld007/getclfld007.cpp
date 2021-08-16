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

typedef struct {
    const char *name;
    const char *sig;
} fld_info;

typedef struct {
    const char *name;
    jint fcount;
    fld_info *flds;
} class_info;

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;

static fld_info f0[] = {
    { "fld_1", "Ljava/lang/String;" }
};

static fld_info f1[] = {
    { "fld_n1", "I" }
};

static fld_info f2[] = {
    { "fld_n2", "I" }
};

static fld_info f4[] = {
    { "fld_o2", "I" }
};

static fld_info f5[] = {
    { "fld_o3", "I" }
};

static fld_info f6[] = {
    { "fld_i1", "I" }
};

static fld_info f7[] = {
    { "fld_i2", "I" }
};

static fld_info f8[] = {
    { "fld_i2", "I" }
};

static fld_info f9[] = {
    { "fld_i1", "I" }
};

static class_info classes[] = {
    { "InnerClass1", 1, f0 },
    { "InnerInterface", 1, f1 },
    { "InnerClass2", 1, f2 },
    { "OuterClass1", 0, NULL },
    { "OuterClass2", 1, f4 },
    { "OuterClass3", 1, f5 },
    { "OuterInterface1", 1, f6 },
    { "OuterInterface2", 1, f7 },
    { "OuterClass4", 1, f8 },
    { "OuterClass5", 1, f9 }
};

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getclfld007(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getclfld007(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getclfld007(JavaVM *jvm, char *options, void *reserved) {
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

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetClassFields_getclfld007_check(JNIEnv *env, jclass cls, jint i, jclass clazz) {
    jvmtiError err;
    jint fcount;
    jfieldID *fields;
    char *name, *sig, *generic;
    int j;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> %s:\n", classes[i].name);
    }

    err = jvmti->GetClassFields(clazz, &fcount, &fields);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassFields#%d) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (fcount != classes[i].fcount) {
        printf("(%d) wrong number of fields: %d, expected: %d\n",
               i, fcount, classes[i].fcount);
        result = STATUS_FAILED;
    }
    for (j = 0; j < fcount; j++) {
        if (fields[j] == NULL) {
            printf("(%d:%d) fieldID = null\n", i, j);
        } else {
            err = jvmti->GetFieldName(clazz, fields[j],
                &name, &sig, &generic);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetFieldName#%d:%d) unexpected error: %s (%d)\n",
                       i, j, TranslateError(err), err);
            } else {
                if (printdump == JNI_TRUE) {
                    printf(">>>   [%d]: %s, sig = \"%s\"\n", j, name, sig);
                }
                if ((j < classes[i].fcount) &&
                       (name == NULL || sig == NULL ||
                        strcmp(name, classes[i].flds[j].name) != 0 ||
                        strcmp(sig, classes[i].flds[j].sig) != 0)) {
                    printf("(%d:%d) wrong field: \"%s%s\"", i, j, name, sig);
                    printf(", expected: \"%s%s\"\n",
                           classes[i].flds[j].name, classes[i].flds[j].sig);
                    result = STATUS_FAILED;
                }
            }
        }
    }
}

JNIEXPORT int JNICALL
Java_nsk_jvmti_GetClassFields_getclfld007_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
