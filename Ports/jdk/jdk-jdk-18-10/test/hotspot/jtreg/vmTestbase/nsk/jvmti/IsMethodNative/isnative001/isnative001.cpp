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

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_isnative001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_isnative001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_isnative001(JavaVM *jvm, char *options, void *reserved) {
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

char const *jbooleanToString(jboolean flag) {
    return ((flag == JNI_TRUE) ? "true" : "false");
}

void checkMeth(JNIEnv *env, jclass cl,
        const char *name, const char *sig, int stat, jboolean flag) {
    jvmtiError err;
    jmethodID mid;
    jboolean is_native;

    if (stat) {
        mid = env->GetStaticMethodID(cl, name, sig);
    } else {
        mid = env->GetMethodID(cl, name, sig);
    }
    if (mid == NULL) {
        printf("Cannot find MethodID for \"%s%s\"\n", name, sig);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->IsMethodNative(mid, &is_native);
    if (err != JVMTI_ERROR_NONE) {
        printf("(IsMethodNative) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> %s%s - %s\n", name, sig, jbooleanToString(is_native));
    }

    if (is_native != flag) {
        printf("(%s%s) wrong is_native value: %s, expected: %s\n",
            name, sig, jbooleanToString(is_native), jbooleanToString(flag));
        result = STATUS_FAILED;
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_IsMethodNative_isnative001_check(JNIEnv *env, jclass cls) {
    jclass clsId;

    checkMeth(env, cls, "<init>", "()V", 0, JNI_FALSE);
    checkMeth(env, cls, "run",
        "([Ljava/lang/String;Ljava/io/PrintStream;)I", 1, JNI_FALSE);
    checkMeth(env, cls, "meth_stat", "(ILjava/lang/String;)[F", 1, JNI_FALSE);
    checkMeth(env, cls, "meth_1", "(CCC)C", 0, JNI_FALSE);
    checkMeth(env, cls, "nmeth", "()V", 0, JNI_TRUE);
    checkMeth(env, cls, "check", "()I", 1, JNI_TRUE);

    clsId = env->FindClass("nsk/jvmti/IsMethodNative/isnative001$Inn");
    if (clsId == NULL) {
        printf("Cannot find nsk.jvmti.IsMethodNative.isnative001$Inn class!\n");
        return STATUS_FAILED;
    }

    checkMeth(env, clsId, "meth_inn", "(Ljava/lang/String;J)V", 0, JNI_FALSE);

    return result;
}

}
