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
    jboolean stat;
    jint count;
    unsigned char *codes;
} info;

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;

static unsigned char m0[] = { 0x2A, 0xB7, 0x00, 0x01, 0xB1 };
static unsigned char m1[] = { 0xB1 };
static unsigned char m2[] = { 0x1A, 0xBC, 0x06, 0x4C, 0x2B, 0xB0 };
static info meth_tab[3] = {
    { "<init>", "()V",   JNI_FALSE, 5, m0 },
    { "meth1",  "()V",   JNI_FALSE, 1, m1 },
    { "meth2",  "(I)[F", JNI_TRUE,  6, m2 }
};

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_bytecodes001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_bytecodes001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_bytecodes001(JavaVM *jvm, char *options, void *reserved) {
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

    err = jvmti->GetCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
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

    if (!caps.can_get_bytecodes) {
        printf("Warning: GetBytecodes is not implemented\n");
    }

    return JNI_OK;
}

void checkMeth(JNIEnv *env, jclass cl, int meth_ind) {
    jvmtiError err;
    jmethodID mid = NULL;
    jint count = -1;
    unsigned char *codes = NULL;
    int i;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (meth_tab[meth_ind].stat == JNI_TRUE) {
        mid = env->GetStaticMethodID(cl,
            meth_tab[meth_ind].name, meth_tab[meth_ind].sig);
    } else {
        mid = env->GetMethodID(cl,
            meth_tab[meth_ind].name, meth_tab[meth_ind].sig);
    }
    if (mid == NULL) {
        printf("\"%s%s\": cannot get method ID!\n",
            meth_tab[meth_ind].name, meth_tab[meth_ind].sig);
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> \"%s%s\" check ...\n",
            meth_tab[meth_ind].name, meth_tab[meth_ind].sig);
    }

    err = jvmti->GetBytecodes(mid, &count, &codes);
    if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY && !caps.can_get_bytecodes) {
        /* It is OK */
        return;
    } else if (err != JVMTI_ERROR_NONE) {
        printf("(GetBytecodes#%s) unexpected error: %s (%d)\n",
               meth_tab[meth_ind].name, TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (count != meth_tab[meth_ind].count) {
        printf("\"%s%s\": byte codes count expected: %d, actual: %d\n",
            meth_tab[meth_ind].name, meth_tab[meth_ind].sig,
            meth_tab[meth_ind].count, count);
        result = STATUS_FAILED;
        return;
    }
    for (i = 0; i < count; i++) {
        if (codes[i] != meth_tab[meth_ind].codes[i]) {
            printf("\"%s%s\": [%d] byte expected: 0x%x, actual: 0x%x\n",
                meth_tab[meth_ind].name, meth_tab[meth_ind].sig, i,
                meth_tab[meth_ind].codes[i], codes[i]);
            result = STATUS_FAILED;
        }
    }
}

JNIEXPORT jint JNICALL Java_nsk_jvmti_GetBytecodes_bytecodes001_check(JNIEnv *env, jclass cls) {
    checkMeth(env, cls, 0);
    checkMeth(env, cls, 1);
    checkMeth(env, cls, 2);
    return result;
}

}
