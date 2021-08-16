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
#define AGENTS 2

static JavaVM *vm;

static jvmtiEnv *jvmti[AGENTS]; /* JVMTI env of an agent */
static void *agentThr[AGENTS];
static volatile int redir[AGENTS]; /* redirection in an agent done */
static volatile int thrstarted[AGENTS]; /* an agent started */

static volatile int verbose = 0;

static volatile jint result = PASSED;

/* the original JNI function table */
static jniNativeInterface *orig_jni_functions[AGENTS];

/* the redirected JNI function table */
static jniNativeInterface *redir_jni_functions[AGENTS];

/* number of the redirected JNI function calls */
static volatile int redir_calls[AGENTS];

static void doRedirect(JNIEnv*, jvmtiEnv*, int);
static void provokeIntercept(JNIEnv*, const char*);
static int checkIntercept(int, int, int);
static int initAgent(int);
static void startAgent(int);
static int agentA(void*);
static int agentB(void*);
static void JNICALL VMInitA(jvmtiEnv*, JNIEnv*, jthread);
static void JNICALL VMInitB(jvmtiEnv*, JNIEnv*, jthread);

/** redirected JNI functions **/
/* function redirected inside the agent A */
jint JNICALL MyGetVersionA(JNIEnv *env) {
    redir_calls[0]++;

    NSK_DISPLAY1("\nMyGetVersionA: the function called successfully: number of calls=%d\n",
        redir_calls[0]);

    return orig_jni_functions[0]->GetVersion(env);
}

/* function redirected inside the agent B */
jint JNICALL MyGetVersionB(JNIEnv *env) {
    redir_calls[1]++;

    NSK_DISPLAY1("\nMyGetVersionB: the function called successfully: number of calls=%d\n",
        redir_calls[1]);

    return (orig_jni_functions[1])->GetVersion(env);
}
/*****************************/

static void doRedirect(JNIEnv *env, jvmtiEnv *jvmti, int indx) {
    jvmtiError err;

    NSK_DISPLAY1("\n%s JVMTI env: doRedirect: obtaining the JNI function table ...\n",
        (indx == 0) ? "first" : "second");
    err = jvmti->GetJNIFunctionTable(&orig_jni_functions[indx]);
    if (err != JVMTI_ERROR_NONE) {
        result = STATUS_FAILED;
        NSK_COMPLAIN2("TEST FAILED: %s JVMTI env: failed to get original JNI function table: %s\n",
            (indx == 0) ? "first" : "second", TranslateError(err));
        env->FatalError("failed to get original JNI function table");
    }
    err = jvmti->GetJNIFunctionTable(&redir_jni_functions[indx]);
    if (err != JVMTI_ERROR_NONE) {
        result = STATUS_FAILED;
        NSK_COMPLAIN2("TEST FAILED: %s JVMTI env: failed to get redirected JNI function table: %s\n",
            (indx == 0) ? "first" : "second", TranslateError(err));
        env->FatalError("failed to get redirected JNI function table");
    }

    NSK_DISPLAY1(
        "%s JVMTI env: doRedirect: the JNI function table obtained successfully\n"
        "\toverwriting the function GetVersion() ...\n",
        (indx == 0) ? "first" : "second");
    redir_jni_functions[indx]->GetVersion =
        (indx == 0) ? MyGetVersionA : MyGetVersionB;

    err = jvmti->SetJNIFunctionTable(redir_jni_functions[indx]);
    if (err != JVMTI_ERROR_NONE) {
        result = STATUS_FAILED;
        NSK_COMPLAIN2("TEST FAILED: %s JVMTI env: failed to set new JNI function table: %s\n",
            (indx == 0) ? "first" : "second", TranslateError(err));
        env->FatalError("failed to set new JNI function table");
    }

    NSK_DISPLAY1("%s JVMTI env: doRedirect: the functions are overwritten successfully\n",
        (indx == 0) ? "first" : "second");
}

static void provokeIntercept(JNIEnv *env, const char *name) {
    jint res;

    res = env->GetVersion();
    NSK_DISPLAY2("\nGetVersion() called by the agent %s returns %d\n",
        name, res);
}

static int checkIntercept(int indx, int env_num, int exCalls) {
    if (redir_calls[indx] == exCalls) {
        NSK_DISPLAY5(
            "\nCHECK PASSED: GetVersion() interception set in the %s JVMTI env %s properly:\n"
            "\t%d interception(s) with the%s%s JVMTI env as expected\n",
            (indx == 0) ? "first" : "second",
            (exCalls == 0) ? "overwritten by another environment" : "works",
            redir_calls[indx],
            (indx == env_num) ? " same " : " ",
            (env_num == 0) ? "first" : "second");
    }
    else {
        result = STATUS_FAILED;
        NSK_COMPLAIN6(
            "\nTEST FAILED: GetVersion() interception set in the %s JVMTI env doesn't %s properly:\n"
            "\t%d interception(s) with the%s%s JVMTI env instead of %d as expected\n",
            (indx == 0) ? "first" : "second",
            (exCalls == 0) ? "overwritten by another environment" : "work",
            redir_calls[indx],
            (indx == env_num) ? " same " : " ",
            (env_num == 0) ? "first" : "second",
            exCalls);
        return STATUS_FAILED;
    }

    return PASSED;
}

static int initAgent(int indx) {
    jvmtiEventCallbacks callbacks; /* callback functions */
    int exitCode = PASSED;
    jvmtiError err;
    jint res;

    thrstarted[indx] = redir[indx] = redir_calls[indx] = 0;

    NSK_DISPLAY1("\nagent %s initializer: obtaining the JVMTI env ...\n", (indx == 0) ? "A" : "B");
    res = vm->GetEnv((void **) &jvmti[indx], JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti[indx] == NULL) {
        NSK_COMPLAIN1("TEST FAILURE: failed to call GetEnv for the agent %s\n",
            (indx == 0) ? "A" : "B");
        result = STATUS_FAILED;
        return STATUS_FAILED;
    }

    NSK_DISPLAY1("\nagent %s initializer: the JVMTI env obtained\n\tsetting event callbacks ...\n",
        (indx == 0) ? "A" : "B");
    (void) memset(&callbacks, 0, sizeof(callbacks));
    switch (indx) {
    case 0:
        callbacks.VMInit = &VMInitA;
        break;
    case 1:
        callbacks.VMInit = &VMInitB;
        break;
    }
    err = jvmti[indx]->SetEventCallbacks(&callbacks, sizeof(callbacks));
    if (err != JVMTI_ERROR_NONE) {
        NSK_COMPLAIN1("TEST FAILURE: failed to set event callbacks: %s\n",
            TranslateError(err));
        result = STATUS_FAILED;
        return STATUS_FAILED;
    }

    NSK_DISPLAY1("\nagent %s initializer: setting event callbacks done\n\tenabling events ...\n",
        (indx == 0) ? "A" : "B");
    err = jvmti[indx]->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);
    if (err != JVMTI_ERROR_NONE) { /* enable event globally */
        NSK_COMPLAIN2("TEST FAILURE: failed to enable JVMTI_EVENT_VM_INIT event for the agent %s: %s\n",
            (indx == 0) ? "A" : "B", TranslateError(err));
        result = STATUS_FAILED;
        return STATUS_FAILED;
    }
    NSK_DISPLAY2("\nagent %s initializer: enabling events done, returning exit code %d\n",
        (indx == 0) ? "A" : "B", exitCode);

    return exitCode;
}

static void startAgent(int indx) {
    int tries = 0;

    NSK_DISPLAY1("\nstartAgent: starting agent %s thread ...\n",
        (indx == 0) ? "A" : "B");
    void* context = (void*) ((indx == 0) ? "agent A" : "agent B");
    agentThr[indx] = THREAD_new((indx == 0) ? agentA : agentB, context);
    if (THREAD_start(agentThr[indx]) == NULL) {
        NSK_COMPLAIN1("TEST FAILURE: cannot start the agent %s thread\n",
            (indx == 0) ? "A" : "B");
        exit(STATUS_FAILED);
    }

    NSK_DISPLAY1("\nstartAgent: waiting for the agent %s to be started ...\n",
        (indx == 0) ? "A" : "B");
    do {
        THREAD_sleep(1);
        tries++;
        if (tries > TRIES) {
            NSK_COMPLAIN2("TEST FAILURE: the agent %s thread is still not started after %d attempts\n",
                (indx == 0) ? "A" : "B", TRIES);
            exit(STATUS_FAILED);
        }
    } while (thrstarted[indx] != 1);

    NSK_DISPLAY1("\nstartAgent: the agent %s thread started\n",
        (indx == 0) ? "A" : "B");
}

/* agent thread procedures */
static int agentA(void *context) {
    JNIEnv *env;
    jint res;
    int tries = 0;
    int i;
    int exitCode = PASSED;

    NSK_DISPLAY0("\nthe agent A started\n\tattaching the thread to the VM ...\n");
    res = vm->AttachCurrentThread((void **) &env, (void *) 0);
    if (res != 0) {
        NSK_COMPLAIN1("TEST FAILURE: AttachCurrentThread() returns: %d\n", res);
        exit(STATUS_FAILED);
    }

    /* intercept the JNI function table */
    /* check the interception set in another JVMTI env */
    NSK_DISPLAY0(
        "\n>>> TEST CASE #1) First JVMTI env: checking the redirection set in the same env ...\n"
        "\nagent A (first JVMTI env): redirecting the function table ...\n");
    doRedirect(env, jvmti[0], 0);

    /* check that the interception has been set properly */
    NSK_DISPLAY0("\nagent A (first JVMTI env): checking that the interception has been set properly ...\n");
    provokeIntercept(env, "A");
    checkIntercept(0, 0, 1); /* expected interceptions: 1 */
    NSK_DISPLAY0("\n<<< TEST CASE #1) done\n");

    /* the flag set too late in order to make sure that
       the agent B will be started _after_ the interception */
    thrstarted[0] = 1;

    redir[0] = 1;

    NSK_DISPLAY0("\nagent A: waiting for the redirection in agent B ...\n");
    do {
        THREAD_sleep(1);
        tries++;
        if (tries > TRIES) {
            NSK_COMPLAIN1("TEST FAILURE: failed to wait for the redirection in agent B after %d attempts\n",
                TRIES);
            exit(STATUS_FAILED);
        }
    } while (redir[1] != 1);

    /* check the interception set in another JVMTI env */
    NSK_DISPLAY0("\n>>> TEST CASE #4) First JVMTI env: checking the redirection set in second JVMTI env ...\n");
    for (i=0; i<AGENTS; i++) {
        redir_calls[i] = 0;
    }
    provokeIntercept(env, "A");
    /* check that the previous interception has been overwritten */
    checkIntercept(0, 0, 1); /* expected interceptions: 1 */
    /* check the current interception set in another JVMTI env */
    checkIntercept(1, 0, 1); /* expected interceptions: 1 */
    NSK_DISPLAY0("\n<<< TEST CASE #4) done\n");

    NSK_DISPLAY1("\nagent A: detaching and returning exit code %d\n",
        exitCode);
    res = vm->DetachCurrentThread();
    if (res != 0) {
        NSK_COMPLAIN1("TEST WARNING: agent A: DetachCurrentThread() returns: %d\n", res);
    }
    return exitCode;
}

static int agentB(void *context) {
    JNIEnv *env;
    jint res;
    int tries = 0;
    int i;
    int exitCode = PASSED;

    NSK_DISPLAY0("\nthe agent B started\n\tattaching the thread to the VM ...\n");
    res = vm->AttachCurrentThread((void **) &env, (void *) 0);
    if (res != 0) {
        NSK_COMPLAIN1("TEST FAILURE: AttachCurrentThread() returns: %d\n",
            res);
        exit(STATUS_FAILED);
    }

    thrstarted[1] = 1;

    NSK_DISPLAY0("\nagent B: waiting for the redirection in agent A ...\n");
    do {
        THREAD_sleep(1);
        tries++;
        if (tries > TRIES) {
            NSK_COMPLAIN1("TEST FAILURE: failed to wait for the redirection in agent A after %d attempts\n",
                TRIES);
            exit(STATUS_FAILED);
        }
    } while (redir[0] != 1);

    /* check the interception set in another JVMTI env */
    NSK_DISPLAY0("\n>>> TEST CASE #2) Second JVMTI env: checking the redirection set in first JVMTI env ...\n");
    for (i=0; i<AGENTS; i++) {
        redir_calls[i] = 0;
    }
    provokeIntercept(env, "B");
    checkIntercept(0, 1, 1); /* expected interceptions: 1 */
    NSK_DISPLAY0("\n<<< TEST CASE #2) done\n");

    /* intercept the JNI function table */
    NSK_DISPLAY0(
        "\n>>> TEST CASE #3) Second JVMTI env: checking the redirection set in the same env ...\n"
        "\nagent B (second JVMTI env): redirecting the function table ...\n");
    doRedirect(env, jvmti[1], 1);

    for (i=0; i<AGENTS; i++) {
        redir_calls[i] = 0;
    }
    provokeIntercept(env, "B");
    /* check that the previous interception has been overwritten */
    checkIntercept(0, 1, 1); /* expected interceptions: 1 */
    /* check that the current interception has been set properly */
    checkIntercept(1, 1, 1); /* expected interceptions: 1 */
    NSK_DISPLAY0("\n<<< TEST CASE #3) done\n");

    redir[1] = 1;

    NSK_DISPLAY1("\nagent B: detaching and returning exit code %d\n",
        exitCode);
    res = vm->DetachCurrentThread();
    if (res != 0) {
        NSK_COMPLAIN1("TEST WARNING: agent B: DetachCurrentThread() returns: %d\n", res);
    }
    return exitCode;
}
/*********************/

/* callback functions */
void JNICALL
VMInitA(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread) {
    NSK_DISPLAY0("\nagent A: VMInit event\n");

    startAgent(0);
}

void JNICALL
VMInitB(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread) {
    NSK_DISPLAY0("\nagent B: VMInit event\n");

    startAgent(1);
}
/*********************/

JNIEXPORT jint JNICALL
Java_nsk_jvmti_scenarios_jni_1interception_JI05_ji05t001_getResult(JNIEnv *env, jobject obj) {
    int i;

    for (i=0; i<AGENTS; i++) {
        NSK_DISPLAY1("\ngetResult: waiting for the agent %s thread...\n",
            (i == 0) ? "A" : "B");
        THREAD_waitFor(agentThr[i]);
        if (THREAD_status(agentThr[i]) != PASSED) {
            result = STATUS_FAILED;
            NSK_COMPLAIN2("TEST FAILED: the agent %s thread done with the error code %d\n",
                (i == 0) ? "A" : "B", THREAD_status(agentThr[i]));
        }
        else NSK_DISPLAY2("getResult: the agent %s thread done with the code %d\n",
                (i == 0) ? "A" : "B", THREAD_status(agentThr[i]));
        free(agentThr[i]);
    }

    return result;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ji05t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ji05t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ji05t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    int i;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    vm = jvm;

    for (i=0; i<AGENTS; i++) {
        NSK_DISPLAY1("initializing agent %s ...\n",
                (i == 0) ? "A" : "B");
        if (initAgent(i) != PASSED)
            return JNI_ERR;
    }

    return JNI_OK;
}

}
