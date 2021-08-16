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
static const char* fields[] = { "x", "y", "z" };

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getfldecl001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getfldecl001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getfldecl001(JavaVM *jvm, char *options, void *reserved) {
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
Java_nsk_jvmti_GetFieldDeclaringClass_getfldecl001_check(JNIEnv *env,
        jclass cls, jint i, jclass cls1, jclass cls2) {
    jvmtiError err;
    jfieldID fid;
    jclass declaringClass;
    char *sig, *generic;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (i == 0) {
        fid = env->GetStaticFieldID(cls1, fields[i], "I");
    } else {
        fid = env->GetFieldID(cls1, fields[i], "I");
    }
    if (fid == NULL) {
        printf("(%d) cannot get field ID for %s:\"I\"\n", i, fields[i]);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->GetFieldDeclaringClass(cls1, fid, &declaringClass);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetFieldDeclaringClass#%d) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->GetClassSignature(declaringClass, &sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassSignature#%d) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> %d -- %s: \"%s\"\n", i, fields[i], sig);
    }

    if (env->IsSameObject(cls2, declaringClass) != JNI_TRUE) {
        printf("(%d) unexpected declaringClass: %s\n", i, sig);
        result = STATUS_FAILED;
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetFieldDeclaringClass_getfldecl001_getResult(JNIEnv *env,
        jclass cls) {
    return result;
}

}
