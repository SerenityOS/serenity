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
#include <stdarg.h>
#include <stdlib.h>
#include <string.h>

#include <jvmti.h>
#include "agent_common.h"

#include "JVMTITools.h"
#include "native_thread.h"

extern "C" {

#define PASSED  0
#define STATUS_FAILED  2

static JavaVM *vm;
static jvmtiEnv *jvmti = NULL;

static int verbose = 0;

static jint result = PASSED;

/* the original JNI function table */
static jniNativeInterface *orig_jni_functions = NULL;

/* the redirected JNI function table */
static jniNativeInterface *redir_jni_functions = NULL;

/* number of the redirected JNI function calls */
static volatile int redir_calls = 0;

/** redirected JNI functions **/
jint JNICALL MyGetVersion(JNIEnv *env) {
    redir_calls++;

    if (verbose)
        printf("\nMyGetVersion: the function called successfully: number of calls=%d\n",
            redir_calls);

    return orig_jni_functions->GetVersion(env);
}
/*****************************/

static void doRedirect(JNIEnv *env) {
    jvmtiError err;

    if (verbose)
        printf("\ndoRedirect: obtaining the JNI function table ...\n");
    err = jvmti->GetJNIFunctionTable(&orig_jni_functions);
    if (err != JVMTI_ERROR_NONE) {
        result = STATUS_FAILED;
        printf("(%s,%d): TEST FAILED: failed to get original JNI function table: %s\n",
            __FILE__, __LINE__, TranslateError(err));
        env->FatalError("failed to get original JNI function table");
    }
    err = jvmti->GetJNIFunctionTable(&redir_jni_functions);
    if (err != JVMTI_ERROR_NONE) {
        result = STATUS_FAILED;
        printf("(%s,%d): TEST FAILED: failed to get redirected JNI function table: %s\n",
            __FILE__, __LINE__, TranslateError(err));
        env->FatalError("failed to get redirected JNI function table");
    }
    if (verbose)
        printf("doRedirect: the JNI function table obtained successfully\n");

    if (verbose)
        printf("\ndoRedirect: overwriting the function GetVersion() ...\n");
    redir_jni_functions->GetVersion = MyGetVersion;

    err = jvmti->SetJNIFunctionTable(redir_jni_functions);
    if (err != JVMTI_ERROR_NONE) {
        result = STATUS_FAILED;
        printf("(%s,%d): TEST FAILED: failed to get new JNI function table: %s\n",
            __FILE__, __LINE__, TranslateError(err));
        env->FatalError("failed to get new JNI function table");
    }

    if (verbose)
        printf("\ndoRedirect: the function is overwritten successfully\n");
}

static void doRestore(JNIEnv *env) {
    jvmtiError err;

    if (verbose)
        printf("\ndoRestore: restoring the original JNI function table ...\n");
    err = jvmti->SetJNIFunctionTable(orig_jni_functions);
    if (err != JVMTI_ERROR_NONE) {
        result = STATUS_FAILED;
        printf("(%s,%d): TEST FAILED: failed to restore original JNI function table: %s\n",
            __FILE__, __LINE__, TranslateError(err));
        env->FatalError("failed to restore original JNI function table");
    }
    if (verbose)
        printf("doRestore: the original JNI function table is restored successfully\n");
}

static void checkRedir(JNIEnv *env, int exCalls) {
    jniNativeInterface *tested_jni_functions = NULL;
    jvmtiError err;
    jint res;

    redir_calls = 0;

    err = jvmti->GetJNIFunctionTable(&tested_jni_functions);
    if (err != JVMTI_ERROR_NONE) {
        result = STATUS_FAILED;
        printf("(%s,%d): TEST FAILED: failed to get modified JNI function table: %s\n",
            __FILE__, __LINE__, TranslateError(err));
        env->FatalError("failed to get modified JNI function table");
    }
    if (verbose)
        printf("checkRedir: the JNI function table obtained successfully\n\tcalling GetVersion() ...\n");
    res = tested_jni_functions->GetVersion(env);

    if (redir_calls == exCalls) {
        if (verbose)
            printf("\nCHECK PASSED: the %s JNI function table is returned by GetJNIFunctionTable():\n\t%d interception of GetVersion() calls as expected\n",
                (exCalls == 0) ? "original" : "modified",
                redir_calls);
    }
    else {
        result = STATUS_FAILED;
        printf("\nTEST FAILED: the %s JNI function table is returned by GetJNIFunctionTable() instead of the %s one:\n\t%d interception of GetVersion() calls instead of %d as expected\n",
            (exCalls == 0) ? "modified" : "original",
            (exCalls == 0) ? "original" : "modified",
            redir_calls, exCalls);
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetJNIFunctionTable_getjniftab001_check(JNIEnv *env, jobject obj) {
    int exitCode = PASSED;

    if (jvmti == NULL) {
        printf("(%s,%d): TEST FAILURE: JVMTI client was not properly loaded\n",
            __FILE__, __LINE__);
        return STATUS_FAILED;
    }

    /* 1: check the assertion with the modified function table */
    doRedirect(env);
    if (verbose)
        printf("\na) Checking the assertion with the modified function table ...\n");
    checkRedir(env, 1);

    /* 2: check the assertion with the original function table */
    doRestore(env);
    if (verbose)
        printf("\nb) Checking the assertion with the original function table ...\n");
    checkRedir(env, 0);

    return result;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getjniftab001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getjniftab001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getjniftab001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;

    if (options != NULL && strcmp(options, "-verbose") == 0)
        verbose = 1;

    if (verbose)
        printf("verbose mode on\n");

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("(%s,%d): Failed to call GetEnv\n", __FILE__, __LINE__);
        return JNI_ERR;
    }

    return JNI_OK;
}

}
