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
    int count;
    jvmtiLineNumberEntry *table;
} info;

static jvmtiEnv *jvmti;
static jvmtiCapabilities caps;
static jint result = PASSED;
static jvmtiLineNumberEntry m0[] = {
    { 0, 64 }
};
static jvmtiLineNumberEntry m1[] = {
    { 0, 67 },
    { 4, 68 },
    { 7, 69 },
    { 12,70 }
};
static info meth_tab[] = { { 1, m0 }, { 4, m1 } };

void checkMeth(JNIEnv *env, jclass cl, const char *name, const char *sig,
               int stat, int meth_ind) {
    jvmtiError err;
    int i, j, flag;
    jmethodID mid;
    jint count = -1;
    jvmtiLineNumberEntry *table;
    int exp_count = meth_tab[meth_ind].count;
    jvmtiLineNumberEntry *exp_table = meth_tab[meth_ind].table;

    if (stat) {
        mid = env->GetStaticMethodID(cl, name, sig);
    } else {
        mid = env->GetMethodID(cl, name, sig);
    }
    if (mid == NULL) {
        printf("Name = %s, sig = %s: mid = NULL\n", name, sig);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti->GetLineNumberTable(mid, &count, &table);
    if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY &&
            !caps.can_get_line_numbers) {
        /* It is OK */
        return;
    } else if (err != JVMTI_ERROR_NONE) {
        printf("Name = %s, sig = %s:\n", name, sig);
        printf("  Failed get line number table: (%s) %d\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    if (count != exp_count) {
        result = STATUS_FAILED;
        printf("Name = %s, sig = %s: number of entries expected: %d, got: %d\n",
            name, sig, exp_count, count);
        return;
    }

    for (i = 0; i < count; i++) {  /* for each expected entry */
        flag = 0;
        for (j = 0; j < count; j++) {  /* search returned table for the entry */
            if (table[j].line_number == exp_table[i].line_number) {
                flag = 1;
                if (table[j].start_location != exp_table[i].start_location) {
                    result = STATUS_FAILED;
                    printf("Name = %s, sig = %s, line %d:\n",
                           name, sig, table[j].line_number);
                    printf("  start_location expected: 0x%x, got: 0x%08x%08x\n",
                           (jint)exp_table[i].start_location,
                           (jint)(table[j].start_location >> 32),
                           (jint)table[j].start_location);
                }
                break;
            }
        }
        if (!flag) {
            result = STATUS_FAILED;
            printf("Name = %s, sig = %s, no line: %d\n",
                   name, sig, exp_table[i].line_number);
        }
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_linetab001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_linetab001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_linetab001(JavaVM *jvm, char *options, void *reserved) {
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

    if (!caps.can_get_line_numbers) {
        printf("Warning: GetLineNumberTable is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetLineNumberTable_linetab001_check(JNIEnv *env, jclass cls) {
    checkMeth(env, cls, "meth00", "()V", 1, 0);
    checkMeth(env, cls, "meth01", "()D", 0, 1);
    return result;
}

}
