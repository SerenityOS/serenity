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
#include <stdlib.h>
#include <string.h>

#include <jvmti.h>
#include "agent_common.h"

#include "JVMTITools.h"

extern "C" {

#define PASSED  0
#define STATUS_FAILED  2

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static int verbose = 0;

/* the original JNI function table */
static jniNativeInterface *orig_jni_functions = NULL;

/* the redirected JNI function table */
static jniNativeInterface *redir_jni_functions = NULL;

/* number of the redirected JNI function calls */
int call_count = 0;

jint JNICALL MyGetVersion(JNIEnv *env) {
    call_count++;
    if (verbose) {
        printf("\nMyGetVersion: the function called successfully: getVer_count=%d\n",
            call_count);
    }
    return orig_jni_functions->GetVersion(env);
}

void doRedirect(JNIEnv *env) {
    jvmtiError err;

    if (verbose)
        printf("\ndoRedirect: obtaining the JNI function ...\n");

    err = jvmti->GetJNIFunctionTable(&orig_jni_functions);
    if (err != JVMTI_ERROR_NONE) {
        printf("(%s,%d): TEST FAILED: failed to get original JNI function table: %s\n",
            __FILE__, __LINE__, TranslateError(err));
        env->FatalError("failed to get original JNI function table");
        result = STATUS_FAILED;
    }

    err = jvmti->GetJNIFunctionTable(&redir_jni_functions);
    if (err != JVMTI_ERROR_NONE) {
        printf("(%s,%d): TEST FAILED: failed to get redirected JNI function table: %s\n",
            __FILE__, __LINE__, TranslateError(err));
        env->FatalError("failed to get redirected JNI function table");
        result = STATUS_FAILED;
    }
    if (verbose)
        printf("doRedirect: the JNI function table obtained successfully\n\toverwriting the function GetVersion ...\n");

    redir_jni_functions->GetVersion = MyGetVersion;

    err = jvmti->SetJNIFunctionTable(redir_jni_functions);
    if (err != JVMTI_ERROR_NONE) {
        printf("(%s,%d): TEST FAILED: failed to set new JNI function table: %s\n",
            __FILE__, __LINE__, TranslateError(err));
        env->FatalError("failed to set new JNI function table");
        result = STATUS_FAILED;
    }

    if (verbose)
        printf("doRedirect: the function GetVersion is overwritten successfully\n");
}

void doRestore(JNIEnv *env) {
    jvmtiError err;

    if (verbose)
        printf("\ndoRestore: restoring the original JNI function ...\n");
    err = jvmti->SetJNIFunctionTable(orig_jni_functions);
    if (err != JVMTI_ERROR_NONE) {
        printf("(%s,%d): TEST FAILED: failed to restore original JNI function table: %s\n",
            __FILE__, __LINE__, TranslateError(err));
        env->FatalError("failed to restore original JNI function table");
        result = STATUS_FAILED;
    }
    if (verbose)
        printf("doRestore: the original function GetVersion is restored successfully\n");
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_scenarios_jni_1interception_JI03_ji03t001_check(JNIEnv *env, jclass cls) {
    jint ver;

    if (jvmti == NULL) {
        printf("(%s,%d): TEST FAILURE: JVMTI client was not properly loaded\n",
            __FILE__, __LINE__);
        return STATUS_FAILED;
    }

    if (verbose)
        printf("\na) invoking the original function GetVersion() ...\n");

    ver = env->GetVersion();

    if (verbose)
        printf("JNIenv version=%d\n", ver);

    /* check JNI function table interception */
    doRedirect(env);

    if (verbose)
        printf("\nb) invoking the redirected function GetVersion() ...\n");
    ver = env->GetVersion();

    if (call_count == 1) {
        if (verbose)
            printf("JNIenv version=%d\nCHECK PASSED: the redirected JNI function has been invoked:\n\t1 intercepted call as expected\n",
                ver);
    }
    else {
        printf("(%s,%d): TEST FAILED: the tested JNI function has not been redirected:\n\t%d intercepted call(s) instead of 1 as expected\n",
            __FILE__, __LINE__, call_count);
        result = STATUS_FAILED;
    }

    /* check restored JNI function table */
    call_count = 0;
    doRestore(env);

    if (verbose)
        printf("\nc) invoking the restored function GetVersion ...\n");
    ver = env->GetVersion();

    if (call_count == 0) {
        if (verbose)
            printf("JNIenv version=%d\nCHECK PASSED: the original JNI function has been restored:\n\t0 intercepted call(s) as expected\n",
                ver);
    }
    else {
        printf("(%s,%d): TEST FAILED: the tested JNI function has not been restored:\n\t%d intercepted call(s) instead of 0 as expected\n",
            __FILE__, __LINE__, call_count);
        result = STATUS_FAILED;
    }

    return result;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ji03t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ji03t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ji03t001(JavaVM *jvm, char *options, void *reserved) {
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
