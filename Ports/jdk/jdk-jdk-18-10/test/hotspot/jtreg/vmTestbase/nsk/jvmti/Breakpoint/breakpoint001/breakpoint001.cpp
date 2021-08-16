/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include <jvmti.h>
#include "agent_common.h"

#include "nsk_tools.h"
#include "JVMTITools.h"
#include "jvmti_tools.h"
#include "jni_tools.h"

extern "C" {

#define STATUS_FAILED 2
#define PASSED 0

#define METH_NUM 2
static const char *METHODS[][2] = {
    { "bpMethod", "()V" },
    { "bpMethod2", "()I" }
};

static const char *CLASS_SIG =
    "Lnsk/jvmti/Breakpoint/breakpoint001;";

static const char *THREAD_NAME = "breakpoint001Thr";

static volatile int bpEvents[METH_NUM];
static volatile jint result = PASSED;
static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;

static volatile int callbacksEnabled = NSK_TRUE;
static jrawMonitorID agent_lock;

static void initCounters() {
    int i;

    for (i=0; i<METH_NUM; i++)
        bpEvents[i] = 0;
}

static void setBP(jvmtiEnv *jvmti_env, JNIEnv *env, jclass klass) {
    jmethodID mid;
    int i;

    for (i=0; i<METH_NUM; i++) {
        if (!NSK_JNI_VERIFY(env, (mid = env->GetMethodID(klass, METHODS[i][0], METHODS[i][1])) != NULL))
            env->FatalError("failed to get ID for the java method\n");

        if (!NSK_JVMTI_VERIFY(jvmti_env->SetBreakpoint(mid, 0)))
            env->FatalError("failed to set breakpoint\n");
    }
}

/** callback functions **/
void JNICALL
ClassLoad(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread, jclass klass) {
    char *sig, *generic;

    jvmti->RawMonitorEnter(agent_lock);

    if (callbacksEnabled) {
        // GetClassSignature may be called only during the start or the live phase
        if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(klass, &sig, &generic)))
            env->FatalError("failed to obtain a class signature\n");

        if (sig != NULL && (strcmp(sig, CLASS_SIG) == 0)) {
            NSK_DISPLAY1(
                "ClassLoad event received for the class \"%s\"\n"
                "\tsetting breakpoints ...\n",
                sig);
            setBP(jvmti_env, env, klass);
        }
    }

    jvmti->RawMonitorExit(agent_lock);
}

void JNICALL
Breakpoint(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
        jmethodID method, jlocation location) {
    jclass klass;
    char *clsSig, *generic, *methNam, *methSig;
    jvmtiThreadInfo thr_info;
    int checkStatus = PASSED;
    int i;

    NSK_DISPLAY0(">>>> Breakpoint event received\n");

    /* checking thread info */
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetThreadInfo(thread, &thr_info))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILED: unable to get thread info during Breakpoint callback\n\n");
        return;
    }
    if (thr_info.name == NULL ||
            strcmp(thr_info.name,THREAD_NAME) != 0 ||
            thr_info.is_daemon == JNI_TRUE) {
        result = checkStatus = STATUS_FAILED;
        NSK_COMPLAIN2(
            "TEST FAILED: Breakpoint event with unexpected thread info:\n"
            "\tname: \"%s\"\ttype: %s thread\n\n",
            (thr_info.name == NULL) ? "NULL" : thr_info.name,
            (thr_info.is_daemon == JNI_TRUE) ? "deamon" : "user");
    }
    else
        NSK_DISPLAY2("CHECK PASSED: thread name: \"%s\"\ttype: %s thread\n",
            thr_info.name, (thr_info.is_daemon == JNI_TRUE) ? "deamon" : "user");

    /* checking location */
    if (location != 0) {
        result = checkStatus = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: Breakpoint event with unexpected location %ld:\n\n",
            (long) location);
    }
    else
        NSK_DISPLAY1("CHECK PASSED: location: %ld as expected\n",
            (long) location);

    /* checking method info */
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodDeclaringClass(method, &klass))) {
        result = checkStatus = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILED: unable to get method declaring class during Breakpoint callback\n\n");
        return;
    }
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(klass, &clsSig, &generic))) {
        result = checkStatus = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILED: unable to obtain a class signature during Breakpoint callback\n\n");
        return;
    }
    if (clsSig == NULL ||
            strcmp(clsSig,CLASS_SIG) != 0) {
        result = checkStatus = STATUS_FAILED;
        NSK_COMPLAIN1(
            "TEST FAILED: Breakpoint event with unexpected class signature:\n"
            "\t\"%s\"\n\n",
            (clsSig == NULL) ? "NULL" : clsSig);
    }
    else
        NSK_DISPLAY1("CHECK PASSED: class signature: \"%s\"\n",
            clsSig);

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &methNam, &methSig, NULL))) {
        result = checkStatus = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILED: unable to get method name during Breakpoint callback\n\n");
        return;
    }

    for (i=0; i<METH_NUM; i++)
        if (strcmp(methNam, METHODS[i][0]) == 0 &&
                strcmp(methSig, METHODS[i][1]) == 0) {
            NSK_DISPLAY2("CHECK PASSED: method name: \"%s\"\tsignature: \"%s\"\n",
                methNam, methSig);
            if (checkStatus == PASSED)
                bpEvents[i]++;
            break;
        }

    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*) methNam))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILED: unable to deallocate memory pointed to method name\n\n");
    }
    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*) methSig))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILED: unable to deallocate memory pointed to method signature\n\n");
    }

    NSK_DISPLAY0("<<<<\n\n");
}


void JNICALL
VMStart(jvmtiEnv *jvmti_env, JNIEnv* jni_env) {
    jvmti->RawMonitorEnter(agent_lock);

    callbacksEnabled = NSK_TRUE;

    jvmti->RawMonitorExit(agent_lock);
}


void JNICALL
VMDeath(jvmtiEnv *jvmti_env, JNIEnv* jni_env) {
    jvmti->RawMonitorEnter(agent_lock);

    callbacksEnabled = NSK_FALSE;

    jvmti->RawMonitorExit(agent_lock);
}
/************************/

JNIEXPORT jint JNICALL
Java_nsk_jvmti_Breakpoint_breakpoint001_check(
        JNIEnv *env, jobject obj) {
    int i;

    for (i=0; i<METH_NUM; i++) {
        if (bpEvents[i] != 1) {
            result = STATUS_FAILED;
            NSK_COMPLAIN3(
                "TEST FAILED: wrong number of Breakpoint events\n"
                "\tfor the method \"%s %s\":\n"
                "\t\tgot: %d\texpected: 1\n",
                METHODS[i][0], METHODS[i][1], bpEvents[i]);
        }
        else
            NSK_DISPLAY3("CHECK PASSED: %d Breakpoint event(s) for the method \"%s %s\" as expected\n",
                bpEvents[i], METHODS[i][0], METHODS[i][1]);
    }

    return result;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_breakpoint001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_breakpoint001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_breakpoint001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiCapabilities caps;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    initCounters();

    /* add capability to generate compiled method events */
    memset(&caps, 0, sizeof(jvmtiCapabilities));
    caps.can_generate_breakpoint_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    if (!caps.can_generate_single_step_events)
        NSK_DISPLAY0("Warning: generation of single step events is not implemented\n");

    /* set event callback */
    NSK_DISPLAY0("setting event callbacks ...\n");
    (void) memset(&callbacks, 0, sizeof(callbacks));
    callbacks.ClassLoad = &ClassLoad;
    callbacks.Breakpoint = &Breakpoint;
    callbacks.VMStart = &VMStart;
    callbacks.VMDeath = &VMDeath;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    NSK_DISPLAY0("setting event callbacks done\nenabling JVMTI events ...\n");

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_START, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_LOAD, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_BREAKPOINT, NULL)))
        return JNI_ERR;
    NSK_DISPLAY0("enabling the events done\n\n");

    if (jvmti->CreateRawMonitor("agent_lock", &agent_lock) != JVMTI_ERROR_NONE) {
        return JNI_ERR;
    }

    return JNI_OK;
}

}
