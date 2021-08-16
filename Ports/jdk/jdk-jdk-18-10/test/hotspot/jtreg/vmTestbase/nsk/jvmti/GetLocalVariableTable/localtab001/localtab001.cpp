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


#define PASSED  0
#define STATUS_FAILED  2

typedef struct {
    int count;
    jvmtiLocalVariableEntry *table;
} info;

static jvmtiEnv *jvmti;
static jvmtiCapabilities caps;
static jint result = PASSED;
static jvmtiLocalVariableEntry m1[] = {
    { 0, 21, (char*) "this", (char*) "Lnsk/jvmti/GetLocalVariableTable/localtab001;", NULL, 0 },
    { 4, 17, (char*) "l",    (char*) "J", NULL, 1 },
    { 7, 14, (char*) "f",    (char*) "F", NULL, 2 },
    { 12, 9, (char*) "d",    (char*) "D", NULL, 3 }
};
static jvmtiLocalVariableEntry m2[] = {
    { 0, 32, (char*) "this", (char*) "Lnsk/jvmti/GetLocalVariableTable/localtab001;", NULL, 0 },
    { 0, 32, (char*) "step", (char*) "I", NULL, 1 },
    { 2, 29, (char*) "i2",   (char*) "S", NULL, 2 },
    { 4, 27, (char*) "i3",   (char*) "C", NULL, 3 },
    { 7, 24, (char*) "i4",   (char*) "B", NULL, 4 },
    { 10,21, (char*) "i5",   (char*) "Z", NULL, 5 },
    { 13,18, (char*) "i1",   (char*) "I", NULL, 6 }
};
static jvmtiLocalVariableEntry m3[] = {
    { 0, 70, (char*) "ob",  (char*) "Lnsk/jvmti/GetLocalVariableTable/localtab001;", NULL, 0 },
    { 2, 67, (char*) "ob1", (char*) "Lnsk/jvmti/GetLocalVariableTable/localtab001;", NULL, 1 },
    { 56,13, (char*) "ob2", (char*) "[I", NULL, 2 },
    { 61, 0, (char*) "i",   (char*) "I", NULL, 3 },
    { 64, 5, (char*) "e",   (char*) "Ljava/lang/IndexOutOfBoundsException;", NULL, 4 }
};
static jvmtiLocalVariableEntry m4[] = {
    { 0, 33, (char*) "i1", (char*) "I", NULL, 0 },
    { 0, 33, (char*) "l",  (char*) "J", NULL, 1 },
    { 0, 33, (char*) "i2", (char*) "S", NULL, 2 },
    { 0, 33, (char*) "d",  (char*) "D", NULL, 3 },
    { 0, 33, (char*) "i3", (char*) "C", NULL, 4 },
    { 0, 33, (char*) "f",  (char*) "F", NULL, 5 },
    { 0, 33, (char*) "i4", (char*) "B", NULL, 6 },
    { 0, 33, (char*) "b",  (char*) "Z", NULL, 7 }
};
static jvmtiLocalVariableEntry m5[] = {
    { 0, 6, (char*) "this", (char*) "Lnsk/jvmti/GetLocalVariableTable/localtab001;", NULL, 0 },
    { 0, 6, (char*) "i",    (char*) "I", NULL, 1 },
    { 2, 4, (char*) "i12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678", (char*) "I", NULL, 2 }
};
static jvmtiLocalVariableEntry m6[] = {
    { 0, 5, (char*) "this", (char*) "Lnsk/jvmti/GetLocalVariableTable/localtab001;", NULL, 0 },
};
static info meth_tab[] = {
    { 0, NULL },
    { 4, m1 },
    { 7, m2 },
    { 5, m3 },
    { 8, m4 },
    { 3, m5 },
    { 1, m6 }
};

void checkMeth(JNIEnv *env, jclass cl, const char *name, const char *sig,
               int stat, int meth_ind) {
    jvmtiError err;
    int i, j, flag, loc_err;
    jmethodID mid;
    jint count = -1;
    jvmtiLocalVariableEntry *table;
    int exp_count = meth_tab[meth_ind].count;
    jvmtiLocalVariableEntry *exp_table = meth_tab[meth_ind].table;

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
    err = jvmti->GetLocalVariableTable(mid, &count, &table);
    if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY &&
            !caps.can_access_local_variables) {
        /* It is OK */
        return;
    } else if (err != JVMTI_ERROR_NONE) {
        printf("Name = %s, sig = %s:\n", name, sig);
        printf("  Failed get local variable table: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    if (count != exp_count) {
        printf("Name = %s, sig = %s: number of entries expected: %d, got: %d\n",
            name, sig, exp_count, count);
        result = STATUS_FAILED;
    }

    for (i = 0; i < exp_count; i++) {  /* for each expected entry */
        flag = 0;
        loc_err = 0;
        for (j = 0; j < count; j++) {  /* search returned table for the entry */
            if (strcmp(table[j].name, exp_table[i].name) == 0) {
                flag = 1;
                if (strcmp(table[j].signature, exp_table[i].signature) != 0) {
                    if (!loc_err) {
                        loc_err = 1;
                        printf("Name = %s, sig = %s, local %s:\n",
                               name, sig, table[j].name);
                    }
                    printf("  signature expected: \"%s\", got: \"%s\"\n",
                           exp_table[i].signature, table[j].signature);
                    result = STATUS_FAILED;
                }
                if (table[j].start_location != exp_table[i].start_location) {
                    if (!loc_err) {
                        loc_err = 1;
                        printf("Name = %s, sig = %s, local %s:\n",
                               name, sig, table[j].name);
                    }
                    printf("  start_location expected: 0x%x, got: 0x%08x%08x\n",
                           (jint)exp_table[i].start_location,
                           (jint)(table[j].start_location >> 32),
                           (jint)table[j].start_location);
                    result = STATUS_FAILED;
                }
                if (table[j].length != exp_table[i].length) {
                    if (!loc_err) {
                        loc_err = 1;
                        printf("Name = %s, sig = %s, local %s:\n",
                               name, sig, table[j].name);
                    }
                    printf("  length expected: %d, got: %d\n",
                           exp_table[i].length, table[j].length);
                    result = STATUS_FAILED;
                }
                break;
            }
        }
        if (!flag) {
            printf("Name = %s, sig = %s: no local: %s\n",
                   name, sig, exp_table[i].name);
            result = STATUS_FAILED;
        }
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_localtab001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_localtab001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_localtab001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

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

    if (!caps.can_access_local_variables) {
        printf("Warning: Access to local variables is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetLocalVariableTable_localtab001_check(JNIEnv *env, jclass cls) {
    checkMeth(env, cls, "meth00", "()V", 1, 0);
    checkMeth(env, cls, "meth01", "()D", 0, 1);
    checkMeth(env, cls, "meth02", "(I)V", 0, 2);
    checkMeth(env, cls, "meth03", "(Lnsk/jvmti/GetLocalVariableTable/localtab001;)V", 1, 3);
    checkMeth(env, cls, "meth04", "(IJSDCFBZ)D", 1, 4);
    checkMeth(env, cls, "meth05", "(I)I", 0, 5);
    checkMeth(env, cls, "<init>", "()V", 0, 6);
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetLocalVariableTable_localtab001_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
