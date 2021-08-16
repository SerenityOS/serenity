/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
} meth_info;

typedef struct {
    const char *name;
    jint mcount;
    meth_info *meths;
} class_info;

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;

static meth_info m0[] = {
    { "<init>", "(Lnsk/jvmti/GetClassMethods/getclmthd007;)V" },
    { "meth_1", "(Ljava/lang/String;)V" }
};

static meth_info m1[] = {
    { "meth_n1", "()V" },
    { "meth_def1", "()V" }
};

static meth_info m2[] = {
    { "<init>", "()V" },
    { "meth_n1", "()V" },
    { "meth_n2", "()I" },
    { "<clinit>", "()V" }
};

static meth_info m3[] = {
    { "<init>", "()V" }
};

static meth_info m4[] = {
    { "<init>", "()V" },
    { "meth_o2", "()V" }
};

static meth_info m5[] = {
    { "<init>", "()V" },
    { "meth_o3", "()I" }
};

static meth_info m6[] = {
    { "meth_i1", "()I" }
};

static meth_info m7[] = {
    { "meth_i2", "()I" }
};

static meth_info m8[] = {
    { "<init>", "()V" },
    { "meth_i2", "()I" }
};

static meth_info m9[] = {
    { "<init>", "()V" },
    { "meth_i1", "()I" }
};

static class_info classes[] = {
    { "InnerClass1", 2, m0 },
    { "InnerInterface", 2, m1 },
    { "InnerClass2", 4, m2 },
    { "OuterClass1", 1, m3 },
    { "OuterClass2", 2, m4 },
    { "OuterClass3", 2, m5 },
    { "OuterInterface1", 1, m6 },
    { "OuterInterface2", 1, m7 },
    { "OuterClass4", 2, m8 },
    { "OuterClass5", 2, m9 }
};

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getclmthd007(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getclmthd007(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getclmthd007(JavaVM *jvm, char *options, void *reserved) {
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
Java_nsk_jvmti_GetClassMethods_getclmthd007_check(JNIEnv *env,
        jclass cls, jint i, jclass clazz) {
    jvmtiError err;
    jint mcount;
    jmethodID *methods;
    char *name, *sig, *generic;
    int j, k;

    int failed = JNI_FALSE; // enable debugging on failure
    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> %s:\n", classes[i].name);
    }

    err = jvmti->GetClassMethods(clazz, &mcount, &methods);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassMethods#%d) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (mcount != classes[i].mcount) {
        printf("(%d) wrong number of methods: %d, expected: %d\n",
               i, mcount, classes[i].mcount);
        result = STATUS_FAILED;
        failed = JNI_TRUE; // show the methods found
        printf(">>> %s:\n", classes[i].name);
    }
    for (k = 0; k < mcount; k++) {
        if (methods[k] == NULL) {
            printf("(%d:%d) methodID = null\n", i, k);
            result = STATUS_FAILED;
        } else if (printdump == JNI_TRUE || failed == JNI_TRUE) {
            err = jvmti->GetMethodName(methods[k],
                &name, &sig, &generic);
            if (err == JVMTI_ERROR_NONE) {
                printf(">>>   [%d]: %s%s\n", k, name, sig);
            }
        }
    }
    for (j = 0; j < classes[i].mcount; j++) {
        /* search the returned table for each expected entry */
        for (k = 0; k < mcount; k++) {
            if (methods[k] != NULL) {
                err = jvmti->GetMethodName(methods[k],
                    &name, &sig, &generic);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(GetMethodName#%d:%d) unexpected error: %s (%d)\n",
                           i, k, TranslateError(err), err);
                    result = STATUS_FAILED;
                } else {
                    if (name != NULL && sig != NULL &&
                        strcmp(name, classes[i].meths[j].name) == 0 &&
                        strcmp(sig, classes[i].meths[j].sig) == 0) break;
                }
            }
        }
        if (k == mcount) {
            printf("(%d:%d) method not found: \"%s%s\"", i, j,
                   classes[i].meths[j].name, classes[i].meths[j].sig);
            result = STATUS_FAILED;
        }
    }
}

JNIEXPORT int JNICALL
Java_nsk_jvmti_GetClassMethods_getclmthd007_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
