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
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;

void chk(JNIEnv *env, jclass cl, const char *name, const char *sig, int stat, int size) {
    jvmtiError err;
    jmethodID mid = NULL;
    jint ret_size;

    if (stat) {
        mid = env->GetStaticMethodID(cl, name, sig);
    } else {
        mid = env->GetMethodID(cl, name, sig);
    }
    if (mid == NULL) {
        printf("Name = %s, sig = %s: mid = 0\n", name, sig);
    }
    err = jvmti->GetArgumentsSize(mid, &ret_size);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetArgumentsSize) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    if (ret_size != size) {
        result = STATUS_FAILED;
        printf("Name = %s, sig = %s: arg size expected: %d, got: %d\n",
               name, sig, size, ret_size);
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_argsize001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_argsize001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_argsize001(JavaVM *jvm, char *options, void *reserved) {
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
Java_nsk_jvmti_GetArgumentsSize_argsize001_check(JNIEnv *env, jclass cls,
        jclass klass1, jclass klass2) {
    chk(env, cls, "<init>", "()V", 0, 1);
    chk(env, cls, "run", "([Ljava/lang/String;Ljava/io/PrintStream;)I", 1, 2);
    chk(env, cls, "meth_stat", "(ILjava/lang/String;)[F", 1, 2);
    chk(env, cls, "meth_1", "(CCC)C", 0, 4);
    chk(env, cls, "meth_2", "(FDJ)F", 0, 6);
    chk(env, klass1, "meth_new",
        "(Lnsk/jvmti/GetArgumentsSize/argsize001;Lnsk/jvmti/GetArgumentsSize/argsize001;)Lnsk/jvmti/GetArgumentsSize/argsize001;", 0, 3);
    chk(env, klass1, "meth_abs", "()V", 0, 1);
    chk(env, klass2, "meth_inn", "(Ljava/lang/String;J)V", 0, 4);
    return result;
}

}
