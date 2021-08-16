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
#include "jvmti.h"
#include "agent_common.h"

extern "C" {


#define PASSED  0
#define STATUS_FAILED  2

typedef struct {
    jlocation start;
    jlocation end;
} info;

static jvmtiEnv *jvmti;
static jint result = PASSED;
static info meth_tab[] = {
    { 0, 4 },  /* 0 <init> */
    { 0, 0 },  /* 1 meth1 */
    { 0, 5 }   /* 2 meth2 */
};

void checkMeth(JNIEnv *env, jclass cl, const char *name, const char *sig,
               int stat, int meth_ind) {
    jvmtiError ans;
    int err = 0;
    jmethodID mid;
    jlocation start;
    jlocation end;
    jlocation exp_start = meth_tab[meth_ind].start;
    jlocation exp_end = meth_tab[meth_ind].end;

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
    ans = jvmti->GetMethodLocation(mid, &start, &end);
    if (ans != JVMTI_ERROR_NONE) {
        printf("Name = %s, sig = %s:\n", name, sig);
        printf("  Failed get method location: err = %d\n", ans);
        result = STATUS_FAILED;
        return;
    }
    if (start != exp_start) {
        result = STATUS_FAILED;
        err = 1;
        printf("Name = %s, sig = %s:\n", name, sig);
        printf("  first location expected: 0x%x, got: 0x%08x%08x\n",
               (jint)exp_start, (jint)(start >> 32), (jint)start);
    }
    if (end != exp_end) {
        result = STATUS_FAILED;
        if (!err) {
            printf("Name = %s, sig = %s:\n", name, sig);
        }
        printf("  last location expected: 0x%x, got: 0x%08x%08x\n",
               (jint)exp_end, (jint)(end >> 32), (jint)end);
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_methloc001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_methloc001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_methloc001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv !\n");
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL Java_nsk_jvmti_GetMethodLocation_methloc001_check(JNIEnv *env, jclass cls) {
    checkMeth(env, cls, "<init>", "()V", 0, 0);
    checkMeth(env, cls, "meth1", "()V", 0, 1);
    checkMeth(env, cls, "meth2", "(I)[F", 1, 2);
    return result;
}

}
