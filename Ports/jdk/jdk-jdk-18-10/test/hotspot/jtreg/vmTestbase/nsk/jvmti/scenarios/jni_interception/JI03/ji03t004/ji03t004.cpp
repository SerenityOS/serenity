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

extern "C" {

#define PASSED  0
#define STATUS_FAILED  2

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static int verbose = 0;

static const char *classSig =
    "Lnsk/jvmti/scenarios/jni_interception/JI03/ji03t004a;";

/* the original JNI function table */
static jniNativeInterface *orig_jni_functions = NULL;

/* the redirected JNI function table */
static jniNativeInterface *redir_jni_functions = NULL;

/* number of the redirected JNI function calls */
int allobj_calls = 0;
int newobj_calls = 0;

/** redirected JNI functions **/
jobject JNICALL MyAllocObject(JNIEnv *env, jclass cls) {
    allobj_calls++;
    if (verbose)
        printf("\nMyAllocObject: the function called successfully: number of calls=%d\n",
            allobj_calls);

    return orig_jni_functions->AllocObject(env, cls);
}

jobject JNICALL MyNewObjectV(JNIEnv *env, jclass cls, jmethodID ctorId, va_list args) {
    newobj_calls++;
    if (verbose)
        printf("\nMyNewObjectV: the function called successfully: number of calls=%d\n",
            newobj_calls);

    return orig_jni_functions->NewObjectV(env, cls, ctorId, args);
}
/*****************************/

void doRedirect(JNIEnv *env) {
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
        printf("\ndoRedirect: overwriting the functions AllocObject,NewObjectV ...\n");
    redir_jni_functions->AllocObject = MyAllocObject;
    redir_jni_functions->NewObjectV = MyNewObjectV;

    err = jvmti->SetJNIFunctionTable(redir_jni_functions);
    if (err != JVMTI_ERROR_NONE) {
        result = STATUS_FAILED;
        printf("(%s,%d): TEST FAILED: failed to set new JNI function table: %s\n",
            __FILE__, __LINE__, TranslateError(err));
        env->FatalError("failed to set new JNI function table");
    }

    if (verbose)
        printf("\ndoRedirect: the functions are overwritten successfully\n");
}

void doRestore(JNIEnv *env) {
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

void doExec(JNIEnv *env, jclass allCls, jmethodID ctorId, const char *msg, ...) {
    jobject allObj;
    jobject newObj;
    va_list args;
    va_start(args, msg);
    allObj = env->AllocObject(allCls);
    if (allObj == NULL) {
        result = STATUS_FAILED;
        printf("(%s,%d): TEST FAILED: failed to call %s AllocObject()\n",
            __FILE__, __LINE__, msg);
        env->FatalError("failed to failed to call AllocObject()");
    }

    if (env->ExceptionOccurred()) {
            result = STATUS_FAILED;
            printf("(%s,%d): TEST FAILED: exception occured during the call of %s AllocObject()\n",
                __FILE__, __LINE__, msg);
            env->ExceptionDescribe();
            env->ExceptionClear();
    }

    newObj = env->NewObjectV(allCls, ctorId, args);
    if (newObj == NULL) {
        result = STATUS_FAILED;
        printf("(%s,%d): TEST FAILED: failed to call %s NewObjectV()\n",
            __FILE__, __LINE__, msg);
        env->FatalError("failed to failed to call NewObjectV()");
    }

    if (env->ExceptionOccurred()) {
        result = STATUS_FAILED;
        printf("(%s,%d): TEST FAILED: exception occured during the call of %s AllocObject()\n",
            __FILE__, __LINE__, msg);
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    va_end(args);
    env->DeleteLocalRef(allObj);
    env->DeleteLocalRef(newObj);
}

void checkCall(int step, int exAllObjCalls, int exNewObjCalls) {
    if (allobj_calls == exAllObjCalls) {
        if (verbose)
            printf("\nCHECK PASSED: the %s JNI function AllocObject() has been %s:\n\t%d intercepted call(s) as expected\n",
                (step == 1) ? "tested" : "original",
                (step == 1) ? "redirected" : "restored",
                allobj_calls);
    }
    else {
        result = STATUS_FAILED;
        printf("\nTEST FAILED: the %s JNI function AllocObject() has not been %s:\t%d intercepted call(s) instead of %d as expected\n\n",
            (step == 1) ? "tested" : "original",
            (step == 1) ? "redirected" : "restored",
            allobj_calls, exAllObjCalls);
    }
    allobj_calls = 0; /* zeroing an interception counter */

    if (newobj_calls == exNewObjCalls) {
        if (verbose)
            printf("\nCHECK PASSED: the %s JNI function NewObjectV() has been %s:\n\t%d intercepted call(s) as expected\n",
                (step == 1) ? "tested" : "original",
                (step == 1) ? "redirected" : "restored",
                newobj_calls);
    }
    else {
        result = STATUS_FAILED;
        printf("\nTEST FAILED: the %s JNI function NewObjectV() has not been %s:\n\t%d intercepted call(s) instead of %d as expected\n",
            (step == 1) ? "tested" : "original",
            (step == 1) ? "redirected" : "restored",
            newobj_calls, exNewObjCalls);
    }
    newobj_calls = 0; /* zeroing an interception counter */
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_scenarios_jni_1interception_JI03_ji03t004_check(JNIEnv *env, jobject obj) {
    jmethodID ctorId;
    jclass objCls;

    if (jvmti == NULL) {
        printf("(%s,%d): TEST FAILURE: JVMTI client was not properly loaded\n",
            __FILE__, __LINE__);
        return STATUS_FAILED;
    }

    objCls = env->FindClass(classSig);
    if (objCls == NULL) {
        printf("(%s,%d): TEST FAILED: failed to call FindClass() for \"%s\"\n",
            __FILE__, __LINE__, classSig);
        return STATUS_FAILED;
    }

    ctorId = env->GetMethodID(objCls, "<init>", "()V");
    if (ctorId == NULL) {
        printf("(%s,%d): TEST FAILED: failed to call GetMethodID() for a constructor\n",
            __FILE__, __LINE__);
        return STATUS_FAILED;
    }

    /* 1: check the JNI function table interception */
    if (verbose)
        printf("\na) Checking the JNI function table interception ...\n");
    doRedirect(env);
    doExec(env, objCls, ctorId, "redirected");
    checkCall(1, 1, 1);

    /* 2: check the restored JNI function table */
    if (verbose)
        printf("\nb) Checking the restored JNI function table ...\n");
    doRestore(env);
    doExec(env, objCls, ctorId, "restored");
    checkCall(2, 0, 0);

    return result;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ji03t004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ji03t004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ji03t004(JavaVM *jvm, char *options, void *reserved) {
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
