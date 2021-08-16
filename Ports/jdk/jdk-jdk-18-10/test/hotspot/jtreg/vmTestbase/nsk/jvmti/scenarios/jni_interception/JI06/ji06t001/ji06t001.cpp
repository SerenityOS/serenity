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

#include "nsk_tools.h"
#include "JVMTITools.h"
#include "jvmti_tools.h"
#include "native_thread.h"

extern "C" {

#define PASSED  0
#define STATUS_FAILED  2

#define TRIES 30
#define MAX_THREADS 5

// Helper for thread detach and terminate
#define THREAD_return(status) \
  do { \
      int res = vm->DetachCurrentThread(); \
      if (res != 0) \
          NSK_COMPLAIN1("TEST WARNING: DetachCurrentThread() returns: %d\n", res); \
      else \
          NSK_DISPLAY0("Detaching thread ...\n"); \
      return status; \
  } while (0)


static const char *javaField = "_ji06t001a";
static const char *classSig =
    "Lnsk/jvmti/scenarios/jni_interception/JI06/ji06t001a;";

static JavaVM *vm;
static jvmtiEnv *jvmti = NULL;

static volatile int verbose = 0;

static volatile jint result = PASSED;
static volatile int monEntered = 0; /* the monitor entered */
static volatile int thrStarted[MAX_THREADS]; /* a thread started */
static volatile int releaseMon = 0; /* flag to release the monitor */

static volatile jobject clsObj;
static jrawMonitorID countLock;

/* the original JNI function table */
static jniNativeInterface *orig_jni_functions = NULL;

/* the redirected JNI function table */
static jniNativeInterface *redir_jni_functions = NULL;

/* number of the redirected JNI function calls */
static volatile int monent_calls = 0;

static void lock() {
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(countLock)))
        exit(STATUS_FAILED);
}

static void unlock() {
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(countLock)))
        exit(STATUS_FAILED);
}

/** redirected JNI functions **/
jint JNICALL MyMonitorEnter(JNIEnv *env, jobject obj) {
    lock();
    monent_calls++;
    unlock();

    NSK_DISPLAY1("MyMonitorEnter: the function called successfully: number of calls=%d\n",
        monent_calls);

    return orig_jni_functions->MonitorEnter(env, obj);
}
/*****************************/

static jint enterMonitor(JNIEnv *env, const char *thr) {
    jint result;

    result = env->MonitorEnter(clsObj);
    if (result != 0) {
        NSK_COMPLAIN2("TEST FAILURE: %s: MonitorEnter() returns: %d\n",
            thr, result);
        return STATUS_FAILED;
    }
    if (env->ExceptionOccurred()) {
        NSK_COMPLAIN1("TEST FAILURE: %s: exception occured\n",
            thr);
        env->ExceptionDescribe();
        env->ExceptionClear();
        return STATUS_FAILED;
    }

    return PASSED;
}

static jint exitMonitor(JNIEnv *env, const char *thr) {
    jint result;

    result = env->MonitorExit(clsObj);
    if (result != 0) {
        NSK_COMPLAIN2("TEST FAILURE: %s: MonitorExit() returns: %d\n",
            thr, result);
        return STATUS_FAILED;
    }

    return PASSED;
}

static void doRedirect(JNIEnv *env) {
    jvmtiError err;

    NSK_DISPLAY0("doRedirect: obtaining the JNI function table ...\n");
    err = jvmti->GetJNIFunctionTable(&orig_jni_functions);
    if (err != JVMTI_ERROR_NONE) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: failed to get original JNI function table: %s\n",
            TranslateError(err));
        env->FatalError("failed to get original JNI function table");
    }
    err = jvmti->GetJNIFunctionTable(&redir_jni_functions);
    if (err != JVMTI_ERROR_NONE) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: failed to get redirected JNI function table: %s\n",
            TranslateError(err));
        env->FatalError("failed to get redirected JNI function table");
    }

    NSK_DISPLAY0("doRedirect: the JNI function table obtained successfully\n"
                 "\toverwriting the function MonitorEnter ...\n");

    redir_jni_functions->MonitorEnter = MyMonitorEnter;

    err = jvmti->SetJNIFunctionTable(redir_jni_functions);
    if (err != JVMTI_ERROR_NONE) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: failed to set new JNI function table: %s\n",
            TranslateError(err));
        env->FatalError("failed to set new JNI function table");
    }

    NSK_DISPLAY0("doRedirect: the functions are overwritten successfully\n");
}

static void checkCall(int exMonEntCalls) {
    if (monent_calls >= exMonEntCalls) {
        NSK_DISPLAY1(
            "CHECK PASSED: the tested JNI function MonitorEnter() has been redirected:\n"
            "\tat least %d intercepted call(s) as expected",
            monent_calls);
    }
    else {
        result = STATUS_FAILED;
        NSK_COMPLAIN2(
            "TEST FAILED: the tested JNI function MonitorEnter() has not been redirected properly:\n"
            "\tonly %d intercepted call(s) instead of at least %d as expected\n",
            monent_calls, exMonEntCalls);
    }
}

/* thread procedures */
static int waitingThread(void *context) {
    JNIEnv *env;
    int exitCode = PASSED;
    jint res;
    int tries = 0;
    /* 4932877 fix in accordance with ANSI C: thread context of type void* -> int* -> int */
    int indx = *((int *) context);

    NSK_DISPLAY1(
        "waitingThread: thread #%d started\n"
        "\tattaching the thread to the VM ...\n",
        indx);
    res = vm->AttachCurrentThread((void **) &env, (void *) 0);
    if (res != 0) {
        NSK_COMPLAIN1("TEST FAILURE: waitingThread: AttachCurrentThread() returns: %d\n",
            res);
        return STATUS_FAILED;
    }

    NSK_DISPLAY1("waitingThread: thread #%d is trying to enter the monitor ...\n",
       indx);

    thrStarted[indx-1] = 1; /* the thread is started */

    if (enterMonitor(env, "waitingThread") == STATUS_FAILED)
        THREAD_return(STATUS_FAILED);
    if (verbose)
        printf("waitingThread: thread #%d entered the monitor\n",
            indx);
    if (exitMonitor(env, "waitingThread") == STATUS_FAILED)
        THREAD_return(STATUS_FAILED);

    NSK_DISPLAY2("waitingThread: thread #%d exits the monitor\n\treturning %d\n",
        indx, exitCode);
    THREAD_return(exitCode);
}

static int ownerThread(void *context) {
    JNIEnv *env;
    int exitCode = PASSED;
    jint res;
    int tries = 0;

    NSK_DISPLAY0("ownerThread: thread started\n\tattaching the thread to the VM ...\n");
    res = vm->AttachCurrentThread((void **) &env, (void *) 0);
    if (res != 0) {
        NSK_COMPLAIN1("TEST FAILURE: ownerThread: AttachCurrentThread() returns: %d\n",
            res);
        return STATUS_FAILED;
    }

    NSK_DISPLAY0("ownerThread: trying to enter the monitor ...\n");
    if (enterMonitor(env, "ownerThread") == STATUS_FAILED)
        THREAD_return(STATUS_FAILED);

    monEntered = 1; /* the monitor has been entered */
    NSK_DISPLAY1(
        "ownerThread: entered the monitor: monEntered=%d\n"
        "\twaiting ...\n",
        monEntered);
    do {
        THREAD_sleep(1);
        tries++;
        if (tries > TRIES) {
            NSK_COMPLAIN1("TEST FAILED: ownerThread: time exceed after %d attempts\n",
                TRIES);
            env->FatalError("ownerThread: time exceed");
        }
    } while (releaseMon != 1);

    if (exitMonitor(env, "ownerThread") == STATUS_FAILED)
        THREAD_return(STATUS_FAILED);

    NSK_DISPLAY1("ownerThread: exits the monitor\n\treturning %d\n",
        exitCode);

    THREAD_return(exitCode);
}

static int redirectorThread(void *context) {
    JNIEnv *env;
    int exitCode = PASSED;
    jint res;
    int tries = 0;

    NSK_DISPLAY0("redirectorThread: thread started\n\tattaching the thread to the VM ...\n");
    res = vm->AttachCurrentThread((void **) &env, (void *) 0);
    if (res != 0) {
        NSK_COMPLAIN1("TEST FAILURE: redirectorThread: AttachCurrentThread() returns: %d\n",
            res);
        return STATUS_FAILED;
    }

    NSK_DISPLAY0("redirectorThread: trying to redirect the MonitorEnter() ...\n");
    doRedirect(env);

    NSK_DISPLAY1("redirectorThread: the MonitorEnter() redirected\n\treturning %d\n",
        exitCode);

    THREAD_return(exitCode);
}
/*********************/

static jobject getObjectFromField(JNIEnv *env, jobject obj) {
    jfieldID fid;
    jclass _objCls;

    _objCls = env->GetObjectClass(obj);

    NSK_DISPLAY2("getObjectFromField: obtaining field ID for name=\"%s\" signature=\"%s\"...\n",
        javaField, classSig);
    fid = env->GetFieldID(_objCls, javaField, classSig);
    if (fid == 0) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILURE: failed to get ID for the field \"%s\"\n",
            javaField);
        env->FatalError("failed to get ID for the java field");
    }

    return env->GetObjectField(obj, fid);
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_scenarios_jni_1interception_JI06_ji06t001_check(JNIEnv *env, jobject obj) {
    char *ownContext = (char*) "ownerThr";
    char *redirContext = (char*) "redirectorThr";
    int exitCode = PASSED;
    void *ownThr = NULL;
    void *redirThr = NULL;
    void *waitThr[MAX_THREADS];
    int waitContElem[MAX_THREADS]; /* context of a particular waiting thread */
    int i;
    int tries = 0;

    if (jvmti == NULL) {
        NSK_COMPLAIN0("TEST FAILURE: JVMTI client was not properly loaded\n");
        return STATUS_FAILED;
    }

/* prepare the testing */
    clsObj = env->NewGlobalRef(getObjectFromField(env, obj));
    if (clsObj == NULL) {
        NSK_COMPLAIN1("TEST FAILURE: cannot create a new global reference of class \"%s\"\n",
            classSig);
        env->FatalError("failed to create a new global reference");
    }

    NSK_DISPLAY0("starting monitor owner thread ...\n");
    ownThr = THREAD_new(ownerThread, ownContext);
    if (THREAD_start(ownThr) == NULL) {
        NSK_COMPLAIN0("TEST FAILURE: cannot start monitor owner thread\n");
        exit(STATUS_FAILED);
    }

    NSK_DISPLAY0("waiting for the monitor to be entered ...\n");
    do {
        THREAD_sleep(1);
        tries++;
        if (tries > TRIES) {
            NSK_COMPLAIN1("TEST FAILURE: the monitor is still not entered by the owner thread after %d attempts\n",
                TRIES);
            env->FatalError(" the monitor is still not entered by the owner thread");
        }
    } while (monEntered != 1);

    for (i=0; i<MAX_THREADS-1; i++) {
        NSK_DISPLAY1("starting waiting thread #%d ...\n",
            i+1);
        thrStarted[i] = 0;
        waitContElem[i] = i+1;
        /* 4932877 fix in accordance with ANSI C: thread context of type int -> int* -> void*  */
        waitThr[i] = THREAD_new(waitingThread, (void *) &(waitContElem[i]));
        if (THREAD_start(waitThr[i]) == NULL) {
            NSK_COMPLAIN1("TEST FAILURE: cannot start waiting thread #%d\n",
                i+1);
            exit(STATUS_FAILED);
        }

        tries = 0;
        do {
            THREAD_sleep(1);
            tries++;
            if (tries > TRIES) {
                NSK_COMPLAIN1("TEST FAILURE: waiting thread #%d is still not started\n",
                    i+1);
                exit(STATUS_FAILED);
            }
        } while (thrStarted[i] != 1);
        NSK_DISPLAY1("the waiting thread #%d started\n",
            i+1);
    }

/* begin the testing */
    NSK_DISPLAY0(">>> TEST CASE a) Trying to redirect the JNI function ...\n\n"
                 "starting redirector thread ...\n");
    redirThr = THREAD_new(redirectorThread, redirContext);
    if (THREAD_start(redirThr) == NULL) {
        NSK_COMPLAIN0("TEST FAILURE: cannot start redirector thread\n");
        exit(STATUS_FAILED);
    }

    NSK_DISPLAY0("waiting for the redirector thread ...\n");
    THREAD_waitFor(redirThr);
    if (THREAD_status(redirThr) != PASSED)
        exitCode = result = STATUS_FAILED;
    if (exitCode == STATUS_FAILED)
        NSK_COMPLAIN1("the redirector thread done with the code %d\n",
            THREAD_status(redirThr));
    else
        NSK_DISPLAY1("the redirector thread done with the code %d\n",
            THREAD_status(redirThr));
    free(redirThr);

    releaseMon = 1;

    NSK_DISPLAY0("waiting for the monitor owner thread ...\n");
    THREAD_waitFor(ownThr);
    if (THREAD_status(ownThr) != PASSED)
        exitCode = result = STATUS_FAILED;
    if (exitCode == STATUS_FAILED)
        NSK_COMPLAIN1("the monitor owner thread done with the code %d\n",
            THREAD_status(ownThr));
    else
        NSK_DISPLAY1("the monitor owner thread done with the code %d\n",
            THREAD_status(ownThr));
    free(ownThr);
    NSK_DISPLAY0("<<<\n\n");

/*  verification of the interception */
    NSK_DISPLAY0(">>> TEST CASE b) Exercising the interception ...\n\n"
                 "main thread: trying to enter the monitor ...\n");
    if (enterMonitor(env, "mainThread") == STATUS_FAILED)
        exitCode = STATUS_FAILED;
    NSK_DISPLAY0("main thread: entered the monitor\n");
    if (exitMonitor(env, "mainThread") == STATUS_FAILED)
        exitCode = STATUS_FAILED;
    NSK_DISPLAY0("main thread: exited the monitor\n");

    NSK_DISPLAY0("starting a separate verification thread ...\n");
    waitContElem[MAX_THREADS-1] = MAX_THREADS;
    /* 4932877 fix in accordance with ANSI C: thread context of type int -> int* -> void*  */
    waitThr[MAX_THREADS-1] = THREAD_new(waitingThread,
        (void *) &(waitContElem[MAX_THREADS-1]));
    if (THREAD_start(waitThr[MAX_THREADS-1]) == NULL) {
        NSK_COMPLAIN0("TEST FAILURE: cannot start verification thread\n");
        exit(STATUS_FAILED);
    }
    NSK_DISPLAY0("the verification thread started\n");

/* finish the testing */
    for (i=0; i<MAX_THREADS; i++) {
        NSK_DISPLAY1("waiting for the thread #%d...\n",
            i+1);
        THREAD_waitFor(waitThr[i]);
        if (THREAD_status(waitThr[i]) != PASSED) {
            result = STATUS_FAILED;
            NSK_COMPLAIN2("TEST FAILED: the waiting thread #%d done with the error code %d\n",
                i+1, THREAD_status(waitThr[i]));
        }
        else
            NSK_DISPLAY2("the thread #%d done with the code %d\n",
                i+1, THREAD_status(waitThr[i]));

        free(waitThr[i]);
    }

    env->DeleteGlobalRef(clsObj);
    NSK_DISPLAY0("<<<\n\n");

    NSK_DISPLAY0(">>> TEST CASE c) Checking number of the intercepted calls ...\n");
    checkCall(2);
    NSK_DISPLAY0("<<<\n\n");

    return result;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ji06t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ji06t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ji06t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    vm = jvm;

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("_counter_lock", &countLock)))
        return JNI_ERR;

    return JNI_OK;
}

}
