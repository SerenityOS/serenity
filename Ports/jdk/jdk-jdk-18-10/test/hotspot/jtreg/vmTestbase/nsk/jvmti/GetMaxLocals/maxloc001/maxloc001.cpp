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

void checkMeth(JNIEnv *env, jclass cl, const char *name, const char *sig, int stat, int max_loc) {
    jvmtiError err;
    jmethodID mid = NULL;
    jint ret_loc;

    if (stat) {
        mid = env->GetStaticMethodID(cl, name, sig);
    } else {
        mid = env->GetMethodID(cl, name, sig);
    }
    if (mid == NULL) {
        printf("Name = %s, sig = %s: mid = 0\n", name, sig);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti->GetMaxLocals(mid, &ret_loc);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMaxLocals) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    } else if (ret_loc != max_loc) {
        printf("Name = %s, sig = %s: max locals expected: %d, got: %d\n", name, sig, max_loc, ret_loc);
        result = STATUS_FAILED;
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_maxloc001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_maxloc001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_maxloc001(JavaVM *jvm, char *options, void *reserved) {
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

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetMaxLocals_maxloc001_check(JNIEnv *env, jclass cls) {
    jclass clsId;

    checkMeth(env, cls, "<init>", "()V", 0, 1);
    checkMeth(env, cls, "run", "([Ljava/lang/String;Ljava/io/PrintStream;)I", 1, 2);
    checkMeth(env, cls, "meth_stat", "(ILjava/lang/String;)[F", 1, 3);
    checkMeth(env, cls, "meth_1", "(C)C", 0, 4);
    checkMeth(env, cls, "meth_2", "(FF)F", 0, 6);
    clsId = env->FindClass("nsk/jvmti/GetMaxLocals/maxloc001a");
    checkMeth(env, clsId, "meth_new", "()Lnsk/jvmti/GetMaxLocals/maxloc001;", 0, 3);
    checkMeth(env, clsId, "meth_abs", "()V", 0, 0);
    clsId = env->FindClass("nsk/jvmti/GetMaxLocals/maxloc001$Inn");
    checkMeth(env, clsId, "meth_inn", "(Ljava/lang/String;)V", 0, 2);
    return result;
}

}
