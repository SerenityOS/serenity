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

static jvmtiEnv *jvmti;
static jint result = PASSED;

void checkMeth(JNIEnv *env, const char *cl_name, const char *name, const char *sig,
               int stat, const char *decl_cl_name) {
    jvmtiError err;
    jmethodID mid;
    char *cl_sig, *generic;
    jclass cl, ret_decl_cl;

    cl = env->FindClass(cl_name);
    if (stat) {
        mid = env->GetStaticMethodID(cl, name, sig);
    } else {
        mid = env->GetMethodID(cl, name, sig);
    }
    if (mid == NULL) {
        printf("%s.%s%s: mid = NULL\n", cl_name, name, sig);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->GetMethodDeclaringClass(mid, &ret_decl_cl);
    if (err != JVMTI_ERROR_NONE) {
        printf("%s.%s%s: ", cl_name, name, sig);
        printf("(GetMethodDeclaringClass) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->GetClassSignature(ret_decl_cl, &cl_sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("%s.%s%s: ", cl_name, name, sig);
        printf("(GetClassSignature) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (strcmp(decl_cl_name, cl_sig) != 0) {
        result = STATUS_FAILED;
        printf("%s.%s%s: ", cl_name, name, sig);
        printf("declaring class expected: %s, got: %s\n", decl_cl_name, cl_sig);
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_declcls001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_declcls001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_declcls001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetMethodDeclaringClass_declcls001_check(JNIEnv *env, jclass cls) {
    checkMeth(env, "nsk/jvmti/GetMethodDeclaringClass/declcls001", "meth",
        "(I)V", 1, "Lnsk/jvmti/GetMethodDeclaringClass/declcls001;");
    checkMeth(env, "nsk/jvmti/GetMethodDeclaringClass/declcls001b", "meth",
        "(I)V", 1, "Lnsk/jvmti/GetMethodDeclaringClass/declcls001;");
    checkMeth(env, "nsk/jvmti/GetMethodDeclaringClass/declcls001i", "meth_i",
        "()I", 0, "Lnsk/jvmti/GetMethodDeclaringClass/declcls001i;");
    checkMeth(env, "nsk/jvmti/GetMethodDeclaringClass/declcls001i1", "meth_i",
        "()I", 0, "Lnsk/jvmti/GetMethodDeclaringClass/declcls001i;");
    checkMeth(env, "nsk/jvmti/GetMethodDeclaringClass/declcls001i1", "meth_i1",
        "()I", 0, "Lnsk/jvmti/GetMethodDeclaringClass/declcls001i1;");
    checkMeth(env, "nsk/jvmti/GetMethodDeclaringClass/declcls001i_a", "meth_i",
        "()I", 0, "Lnsk/jvmti/GetMethodDeclaringClass/declcls001i;");
    checkMeth(env, "nsk/jvmti/GetMethodDeclaringClass/declcls001i_a", "meth_i1",
        "()I", 0, "Lnsk/jvmti/GetMethodDeclaringClass/declcls001i_a;");
    checkMeth(env, "nsk/jvmti/GetMethodDeclaringClass/declcls001i_a", "meth_z",
        "()I", 0, "Lnsk/jvmti/GetMethodDeclaringClass/declcls001z;");
    return result;
}

}
