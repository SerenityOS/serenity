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
static jboolean printdump = JNI_FALSE;
static const char *exp_class_sig = "Ljava/lang/Object;";
static const char *arr_sigs[] = { "[B", "[C", "[D", "[F", "[I", "[J", "[S", "[Z",
    "[Lnsk/jvmti/GetMethodDeclaringClass/declcls003;",
    "[[Lnsk/jvmti/GetMethodDeclaringClass/declcls003;"
};

void do_check(JNIEnv *env, const char *name, const char *meth, const char *sig) {
    jvmtiError err;
    jclass cl;
    jclass ret_decl_cl;
    jmethodID mid;
    char *cl_sig, *generic;

    if (printdump == JNI_TRUE) {
        printf(">>> checking: %s.%s%s\n", name, meth, sig);
    }

    cl = env->FindClass(name);
    mid = env->GetMethodID(cl, meth, sig);
    if (mid == NULL) {
        printf("%s.%s%s: mid = NULL\n", name, meth, sig);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->GetMethodDeclaringClass(mid, &ret_decl_cl);
    if (err != JVMTI_ERROR_NONE) {
        printf("%s.%s%s: ", name, meth, sig);
        printf("(GetMethodDeclaringClass) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->GetClassSignature(ret_decl_cl, &cl_sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("%s.%s%s: ", name, meth, sig);
        printf("(GetClassSignature) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>>\tmethod declaring class: %s\n", cl_sig);
    }

    if (strcmp(exp_class_sig, cl_sig) != 0) {
        printf("%s.%s%s: ", name, meth, sig);
        printf("declaring class expected: %s, actual: %s\n",
            exp_class_sig, cl_sig);
        result = STATUS_FAILED;
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_declcls003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_declcls003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_declcls003(JavaVM *jvm, char *options, void *reserved) {
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
Java_nsk_jvmti_GetMethodDeclaringClass_declcls003_check(JNIEnv *env, jclass cls) {
    size_t i;

    for (i = 0; i < sizeof(arr_sigs)/sizeof(char *); i++) {
        do_check(env, arr_sigs[i], "clone", "()Ljava/lang/Object;");
        do_check(env, arr_sigs[i], "equals", "(Ljava/lang/Object;)Z");
        do_check(env, arr_sigs[i], "finalize", "()V");
        do_check(env, arr_sigs[i], "getClass", "()Ljava/lang/Class;");
        do_check(env, arr_sigs[i], "hashCode", "()I");
        do_check(env, arr_sigs[i], "notify", "()V");
        do_check(env, arr_sigs[i], "notifyAll", "()V");
        do_check(env, arr_sigs[i], "toString", "()Ljava/lang/String;");
        do_check(env, arr_sigs[i], "wait", "()V");
        do_check(env, arr_sigs[i], "wait", "(J)V");
        do_check(env, arr_sigs[i], "wait", "(JI)V");
    }

    return result;
}

}
