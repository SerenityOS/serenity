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

#define METH_NUM 2 /* overall number of methods */
typedef struct {   /* line numbers of a method */
    int inst;      /* type of a method: 0- static; 1- instance */
    const char *m_name;  /* a method name */
    const char *m_sign;  /* JVM signature of a method */
    jmethodID mid; /* method ID */
    const char *f_name;  /* field name for checking the method calls */
    jfieldID fid;  /* field ID for checking the method calls */
    int jni_calls; /* number of the redirected JNI function calls */
    int java_calls; /* number of java method calls */
} methInfo;

static methInfo meth_info[] = {
    { 0, "statMeth", "(I)D", NULL, "statMeth_calls", NULL, 0, 0 },
    { 1, "voidMeth", "()V", NULL, "voidMeth_calls", NULL, 0, 0 }
};

/* the original JNI function table */
static jniNativeInterface *orig_jni_functions = NULL;

/* the redirected JNI function table */
static jniNativeInterface *redir_jni_functions = NULL;

/** redirected JNI functions **/
jdouble JNICALL MyCallStaticDoubleMethodV(JNIEnv *env, jclass cls, jmethodID mid, va_list args) {
    jdouble res;

    meth_info[0].jni_calls++;
    if (verbose)
        printf("\nMyCallStaticDoubleMethodV: the function called successfully: number of calls=%d\n",
            meth_info[0].jni_calls);

    res = orig_jni_functions->CallStaticDoubleMethodV(env, cls, mid, args);

    if (verbose)
        printf("MyCallStaticDoubleMethodV: returning\n");
    return res;
}

void JNICALL MyCallVoidMethodV(JNIEnv *env, jobject obj, jmethodID mid, va_list args) {
    meth_info[1].jni_calls++;
    if (verbose)
        printf("\nMyCallVoidMethodV: the function called successfully: number of calls=%d\n",
            meth_info[1].jni_calls);

    orig_jni_functions->CallVoidMethodV(env, obj, mid, args);

    if (verbose)
        printf("MyCallVoidMethod: returning\n");
}
/*****************************/

void doRedirect(JNIEnv *env, jclass cls) {
    int i;
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

    for (i=0; i<METH_NUM; i++) {
        if (verbose)
            printf("\ndoRedirect: obtaining method ID for \"%s %s\"...\n",
            meth_info[i].m_name, meth_info[i].m_sign);
        if (meth_info[i].inst) { /* an instance method */
            meth_info[i].mid = env->GetMethodID(
                cls, meth_info[i].m_name, meth_info[i].m_sign);
        }
        else {                   /* a static method */
            meth_info[i].mid = env->GetStaticMethodID(
                cls, meth_info[i].m_name, meth_info[i].m_sign);
        }
        if (meth_info[i].mid == NULL) {
           result = STATUS_FAILED;
           printf("(%s,%d): TEST FAILURE: failed to get the ID for the method \"%s %s\"\n",
                __FILE__, __LINE__, meth_info[i].m_name, meth_info[i].m_sign);
           env->FatalError("failed to get the ID for a method");
        }

        if (verbose)
            printf("\ndoRedirect: obtaining field ID for \"%s\"...\n",
                meth_info[i].f_name);
        meth_info[i].fid = env->GetStaticFieldID(cls, meth_info[i].f_name, "I");
        if (meth_info[i].fid == 0) {
            result = STATUS_FAILED;
            printf("(%s,%d): TEST FAILED: failed to get ID for the field %s\n",
                __FILE__, __LINE__, meth_info[i].f_name);
            env->FatalError("cannot get field ID");
        }

        switch (i) {
        case 0:
            if (verbose)
                printf("\ndoRedirect: overwriting the function CallStaticDoubleMethodV ...\n");
            redir_jni_functions->CallStaticDoubleMethodV = MyCallStaticDoubleMethodV;
            break;
        case 1:
            if (verbose)
                printf("\ndoRedirect: overwriting the function CallVoidMethodV ...\n");
            redir_jni_functions->CallVoidMethodV = MyCallVoidMethodV;
            break;
        }
    }

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

int getFieldVal(JNIEnv *env, jclass cls, jfieldID fid) {
    return env->GetStaticIntField(cls, fid);
}

void doCall(JNIEnv *env, jobject obj, jclass objCls, const char *msg) {
    int i;
    jdouble dVal;

    for (i=0; i<METH_NUM; i++) {
        if (verbose)
            printf("\ndoCall: calling %s JNI method for \"%s %s\"...\n",
                msg, meth_info[i].m_name, meth_info[i].m_sign);
        switch (i) {
        case 0:
            dVal = env->CallStaticDoubleMethod(objCls, meth_info[i].mid, 73);
            break;
        case 1:
            env->CallVoidMethod(obj, meth_info[i].mid);
            break;
        }

        if (env->ExceptionOccurred()) {
            result = STATUS_FAILED;
            printf("(%s,%d): TEST FAILED: exception occured during the execution of the %s method\n",
                __FILE__, __LINE__, msg);
            env->ExceptionDescribe();
            env->ExceptionClear();
        }

        meth_info[i].java_calls = getFieldVal(env, objCls, meth_info[i].fid);

        if (env->ExceptionOccurred()) {
            result = STATUS_FAILED;
            printf("(%s,%d): TEST FAILED: exception occured during getting value of the %s fieldn",
                __FILE__, __LINE__, meth_info[i].f_name);
            env->ExceptionDescribe();
            env->ExceptionClear();
        }

    }
}

void checkCall(int step, int exJniCalls, int exJavaCalls) {
    int i;

    for (i=0; i<METH_NUM; i++) {
        if (meth_info[i].jni_calls == exJniCalls) {
            if (verbose)
                printf("\nCHECK PASSED: the %s JNI function for calling method \"%s %s\" has been %s\n\t%d intercepted call(s) as expected\n",
                    (step == 1) ? "tested" : "original",
                    meth_info[i].m_name, meth_info[i].m_sign,
                    (step == 1) ? "redirected" : "restored",
                    meth_info[i].jni_calls);
        }
        else {
            result = STATUS_FAILED;
            printf("\nTEST FAILED: the %s JNI function for calling method \"%s %s\" has not been %s\n\t%d intercepted call(s) instead of %d as expected\n",
                (step == 1) ? "tested" : "original",
                meth_info[i].m_name, meth_info[i].m_sign,
                (step == 1) ? "redirected" : "restored",
                meth_info[i].jni_calls, exJniCalls);
        }
        meth_info[i].jni_calls = 0; /* zeroing interception counter */

        if (meth_info[i].java_calls == exJavaCalls) {
            if (verbose)
                printf("CHECK PASSED: the java method \"%s %s\" has been really invoked by the %s JNI function\n",
                    meth_info[i].m_name, meth_info[i].m_sign,
                    (step == 1) ? "redirected" : "restored");
        }
        else {
            result = STATUS_FAILED;
            printf("TEST FAILED: the tested java method \"%s %s\" has not been really invoked by the %s JNI function\n",
                meth_info[i].m_name, meth_info[i].m_sign,
                (step == 1) ? "redirected" : "restored");
        }
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_scenarios_jni_1interception_JI03_ji03t002_check(JNIEnv *env, jobject obj) {
    jclass objCls;

    if (jvmti == NULL) {
        printf("(%s,%d): TEST FAILURE: JVMTI client was not properly loaded\n",
            __FILE__, __LINE__);
        return STATUS_FAILED;
    }

    objCls = env->GetObjectClass(obj);

    /* 1: check the JNI function table interception */
    if (verbose)
        printf("\na) Checking the JNI function table interception ...\n");
    doRedirect(env, objCls);
    doCall(env, obj, objCls, "redirected");
    checkCall(1, 1, 1);

    /* 2: check the restored JNI function table */
    if (verbose)
        printf("\nb) Checking the restored JNI function table ...\n");
    doRestore(env);
    doCall(env, obj, objCls, "restored");
    checkCall(2, 0, 2);

    return result;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ji03t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ji03t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ji03t002(JavaVM *jvm, char *options, void *reserved) {
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
