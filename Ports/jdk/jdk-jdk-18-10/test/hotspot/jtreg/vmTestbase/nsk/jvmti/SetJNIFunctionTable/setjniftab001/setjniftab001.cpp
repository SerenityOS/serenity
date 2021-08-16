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

#define TRIES 30
#define MAX_THREADS 5

static const char *javaField = "_setjniftab001a";
static const char *classSig =
    "Lnsk/jvmti/SetJNIFunctionTable/setjniftab001a;";

static JavaVM *vm;
static jvmtiEnv *jvmti = NULL;

static volatile int verbose = 0;

static volatile jint result = PASSED;
static volatile int thrStarted[MAX_THREADS];
static void *waitThr[MAX_THREADS];
static int waitContElem[MAX_THREADS]; /* context of a particular waiting thread */

static volatile jobject clsObj;
static jrawMonitorID countLock;

/* the original JNI function table */
static jniNativeInterface *orig_jni_functions = NULL;

/* the redirected JNI function table */
static jniNativeInterface *redir_jni_functions = NULL;

/* number of the redirected JNI function calls */
static volatile int monent_calls = 0;

static void lock() {
    jvmtiError err;

    err = jvmti->RawMonitorEnter(countLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(%s,%d): TEST FAILURE: RawMonitorEnter returns unexpected error: %s\n",
            __FILE__, __LINE__, TranslateError(err));
        exit(STATUS_FAILED);
    }
}

static void unlock() {
    jvmtiError err;

    err = jvmti->RawMonitorExit(countLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(%s,%d): TEST FAILURE: RawMonitorExit returns unexpected error: %s\n",
            __FILE__, __LINE__, TranslateError(err));
        exit(STATUS_FAILED);
    }
}

/** redirected JNI functions **/
jint JNICALL MyMonitorEnter(JNIEnv *env, jobject obj) {
    lock();

    monent_calls++;
    if (verbose)
        printf("\nMyMonitorEnter: the function called successfully: number of calls=%d\n",
            monent_calls);

    unlock();

    return orig_jni_functions->MonitorEnter(env, obj);
}
/*****************************/

/* zeroing the interception counter */
void zeroCounter() {
    lock();
    monent_calls = 0;
    unlock();
}

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
        printf("\ndoRedirect: overwriting the function MonitorEnter ...\n");
    redir_jni_functions->MonitorEnter = MyMonitorEnter;

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

void doExec(JNIEnv *env, int thrNum) {
    jint res;

    res = env->MonitorEnter(clsObj);
    if (res != 0) {
        result = STATUS_FAILED;
        printf("(%s,%d): TEST FAILURE: MonitorEnter() returns %d for thread #%d\n",
            __FILE__, __LINE__, res, thrNum);
    }
    if (env->ExceptionOccurred()) {
        result = STATUS_FAILED;
        printf("(%s,%d): TEST FAILURE: exception occured for thread #%d\n",
            __FILE__, __LINE__, thrNum);
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    if (verbose)
        printf("\ndoExec: thread #%d entered the monitor\n",
            thrNum);
    res = env->MonitorExit(clsObj);
    if (res != 0) {
        result = STATUS_FAILED;
        printf("(%s,%d): TEST FAILURE: MonitorExit() returns %d for thread #%d\n",
            __FILE__, __LINE__, res, thrNum);
    }
    if (verbose)
        printf("doExec: thread #%d exited the monitor\n",
            thrNum);
}

void checkCall(int step, int exMonEntCalls, const char *msg) {
    if (monent_calls == exMonEntCalls) {
        if (verbose)
            printf("\nCHECK PASSED: the %s JNI function MonitorEnter() has been %s inside %s:\n\t%d intercepted call(s) as expected\n",
                (step == 1) ? "tested" : "original",
                (step == 1) ? "redirected" : "restored", msg,
                monent_calls);
    }
    else {
        result = STATUS_FAILED;
        printf("\nTEST FAILED: the %s JNI function MonitorEnter() has not been %s inside %s:\n\t%d intercepted call(s) instead of %d as expected\n",
            (step == 1) ? "tested" : "original",
            (step == 1) ? "redirected" : "restored", msg,
            monent_calls, exMonEntCalls);
    }
}

/* thread procedures */
static int waitingThread(void *context) {
    JNIEnv *env;
    int exitCode = PASSED;
    jint res;
    int tries = 0;
    int indx = *((int *) context);

    if (verbose)
        printf("\nwaitingThread: thread #%d started\n\tattaching the thread to the VM ...\n",
            indx);
    res = vm->AttachCurrentThread((void **) &env, (void *) 0);
    if (res != 0) {
        printf("(%s,%d): TEST FAILURE: waitingThread: AttachCurrentThread() returns: %d\n",
            __FILE__, __LINE__, res);
        return STATUS_FAILED;
    }

    thrStarted[indx-1] = 1;

    doExec(env, indx);

    res = vm->DetachCurrentThread();
    if (res != 0) {
        printf("(%s,%d): TEST FAILURE: waitingThread: DetachCurrentThread() returns: %d\n",
            __FILE__, __LINE__, res);
        return STATUS_FAILED;
    }
    if (verbose)
        printf("waitingThread: the thread #%d exits with %d\n",
            indx, exitCode);
    return exitCode;
}
/*********************/

static jobject getObjectFromField(JNIEnv *env, jobject obj) {
    jfieldID fid;
    jclass _objCls;

    _objCls = env->GetObjectClass(obj);

    if (verbose)
       printf("\ngetObjectFromField: obtaining field ID for name=\"%s\" signature=\"%s\"...\n",
           javaField, classSig);
    fid = env->GetFieldID(_objCls, javaField, classSig);
    if (fid == 0) {
        result = STATUS_FAILED;
        printf("(%s,%d): TEST FAILURE: failed to get ID for the field \"%s\"\n",
            __FILE__, __LINE__, javaField);
        env->FatalError("failed to get ID for the java field");
    }

    return env->GetObjectField(obj, fid);
}

void startThreads() {
    int i;
    int tries = 0;

    for (i=0; i<MAX_THREADS; i++) {
        if (verbose)
            printf("\nstarting waiting thread #%d ...\n",
                i+1);
        thrStarted[i] = 0;
        waitContElem[i] = i+1;
        waitThr[i] = THREAD_new(waitingThread, &waitContElem[i]);
        if (THREAD_start(waitThr[i]) == NULL) {
            printf("TEST FAILURE: cannot start waiting thread #%d\n",
                i+1);
            result = STATUS_FAILED;
        }

        do {
            THREAD_sleep(1);
            tries++;
            if (tries > TRIES) {
                printf("TEST FAILURE: waiting thread #%d is still not started\n",
                    i+1);
                result = STATUS_FAILED;
            }
        } while (thrStarted[i] != 1);
        if (verbose)
            printf("\nthe waiting thread #%d started\n",
                i+1);
    }
}

void waitThreads() {
    int i;

    for (i=0; i<MAX_THREADS; i++) {
        if (verbose)
            printf("\nwaiting for the thread #%d...\n",
                i+1);
        THREAD_waitFor(waitThr[i]);
        if (THREAD_status(waitThr[i]) != PASSED) {
            result = STATUS_FAILED;
            printf("TEST FAILED: the waiting thread #%d done with the error code %d\n",
                i+1, THREAD_status(waitThr[i]));
        }
        else if (verbose)
            printf("the thread #%d done with the code %d\n",
                i+1, THREAD_status(waitThr[i]));
        free(waitThr[i]);
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_SetJNIFunctionTable_setjniftab001_check(JNIEnv *env, jobject obj) {
    int exitCode = PASSED;
    jint res;
    JNIEnv *nextEnv = NULL; /* JNI env used to verify the assertion */

    if (jvmti == NULL) {
        printf("(%s,%d): TEST FAILURE: JVMTI client was not properly loaded\n",
            __FILE__, __LINE__);
        return STATUS_FAILED;
    }

    clsObj = env->NewGlobalRef(getObjectFromField(env, obj));
    if (clsObj == NULL) {
        printf("(%s,%d): TEST FAILURE: cannot create a new global reference of class \"%s\"\n",
            __FILE__, __LINE__, classSig);
        env->FatalError("failed to create a new global reference");
    }

    doRedirect(env);

    /* 1: check the assertion inside current thread and new threads */
    if (verbose)
        printf("\na) Checking the assertion inside current thread and new threads ...\n");
    doExec(env, 0);
    checkCall(1, 1, "main thread");

    zeroCounter();
    startThreads();
    waitThreads();
    checkCall(1, MAX_THREADS, "new threads");

    /* 2: detach current thread then the attach it again and check the assertion
       Note: the JNI spec says that the main thread can be detached from the VM
       only since JDK 1.2 */
    if (verbose)
        printf("\nb) Checking the assertion inside main thread detached and attached again ...\n\ndetaching the main thread ...\n");

    res = vm->DetachCurrentThread();
    if (res != 0) {
        printf(
            "(%s,%d): Warning: DetachCurrentThread() returns: %d\n"
            "\tcheck with the detached main thread skipped\n",
            __FILE__, __LINE__, res);
    } else {
        if (verbose)
            printf("\nattaching the main thread again ...\n");
        res = vm->AttachCurrentThread((void **) &nextEnv, (void *) 0);
        if (res != 0) {
            printf("(%s,%d): TEST FAILURE: waitingThread: AttachCurrentThread() returns: %d\n",
                __FILE__, __LINE__, res);
            return STATUS_FAILED;
        }

        zeroCounter();
        doExec(nextEnv, 0);
        checkCall(1, 1, "main thread with new JNI env");
    }

    /* 3: restore the function, zeroing the interception counter
       and check the assertion with current thread and new threads */
    if (verbose)
        printf("\nc) Checking the restored JNI function table ...\n");
    doRestore((nextEnv == NULL) ? env : nextEnv);

    zeroCounter();
    doExec((nextEnv == NULL) ? env : nextEnv, 0);
    checkCall(2, 0, "main thread");

    zeroCounter();
    startThreads();
    waitThreads();
    checkCall(2, 0, "new threads");

    env->DeleteGlobalRef(clsObj);

    return result;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setjniftab001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setjniftab001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setjniftab001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    if (options != NULL && strcmp(options, "-verbose") == 0)
        verbose = 1;

    if (verbose)
        printf("verbose mode on\n");

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("(%s,%d): Failed to call GetEnv\n", __FILE__, __LINE__);
        return JNI_ERR;
    }

    vm = jvm;

    err = jvmti->CreateRawMonitor("_counter_lock", &countLock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(%s,%d): TEST FAILURE: CreateRawMonitor() returns unexpected error: %s\n",
            __FILE__, __LINE__, TranslateError(err));
        return JNI_ERR;
    }

    return JNI_OK;
}

}
