/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

static const char *METHODS[] = {
    "bpMethod",
    "runThis"
};

static const char *METHOD_SIGS[] = {
    "()V",
    "([Ljava/lang/String;Ljava/io/PrintStream;)I"
};

static volatile long stepEv[] = { 0, 0 };

static const char *CLASS_SIG =
    "Lnsk/jvmti/SingleStep/singlestep001;";

static volatile jint result = PASSED;
static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;

static volatile int callbacksEnabled = NSK_FALSE;
static jrawMonitorID agent_lock;

static void setBP(jvmtiEnv *jvmti_env, JNIEnv *env, jclass klass) {
    jmethodID mid;

    if (!NSK_JNI_VERIFY(env, (mid = env->GetMethodID(klass, METHODS[0], METHOD_SIGS[0])) != NULL))
        env->FatalError("failed to get ID for the java method\n");

    if (!NSK_JVMTI_VERIFY(jvmti_env->SetBreakpoint(mid, 0)))
        env->FatalError("failed to set breakpoint\n");
}

/** callback functions **/
void JNICALL
ClassLoad(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread, jclass klass) {
    char *sig, *generic;

    jvmti->RawMonitorEnter(agent_lock);

    if (callbacksEnabled) {
        if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(klass, &sig, &generic)))
            env->FatalError("failed to obtain a class signature\n");

        if (sig != NULL && (strcmp(sig, CLASS_SIG) == 0)) {
            NSK_DISPLAY1(
                "ClassLoad event received for the class \"%s\"\n"
                "\tsetting breakpoint ...\n",
                sig);
            setBP(jvmti_env, env, klass);
        }
    }

    jvmti->RawMonitorExit(agent_lock);
}

void JNICALL
Breakpoint(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr, jmethodID method,
        jlocation loc) {
    jclass klass;
    char *sig, *generic;

    jvmti->RawMonitorEnter(agent_lock);

    if (!callbacksEnabled) {
        jvmti->RawMonitorExit(agent_lock);
        return;
    }

    NSK_DISPLAY0("Breakpoint event received\n");
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodDeclaringClass(method, &klass)))
        NSK_COMPLAIN0("TEST FAILURE: unable to get method declaring class\n\n");

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(klass, &sig, &generic)))
        env->FatalError("Breakpoint: failed to obtain a class signature\n");

    if (sig != NULL && (strcmp(sig, CLASS_SIG) == 0)) {
        NSK_DISPLAY1("method declaring class \"%s\"\n\tenabling SingleStep events ...\n",
            sig);
        if (!NSK_JVMTI_VERIFY(jvmti_env->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_SINGLE_STEP, thr))) {
            result = STATUS_FAILED;
            NSK_COMPLAIN0("TEST FAILURE: cannot enable SingleStep events\n\n");
        }
    } else {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILURE: unexpected breakpoint event in method of class \"%s\"\n\n",
            sig);
    }
    jvmti->RawMonitorExit(agent_lock);
}

void JNICALL
SingleStep(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
        jmethodID method, jlocation location) {
    jclass klass;
    char *sig, *generic, *methNam, *methSig;

    if (result == STATUS_FAILED) {
        return;
    }

    NSK_DISPLAY0(">>>> SingleStep event received\n");

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &methNam, &methSig, NULL))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILED: unable to get method name during SingleStep callback\n\n");
        return;
    }
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodDeclaringClass(method, &klass))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILED: unable to get method declaring class during SingleStep callback\n\n");
        return;
    }
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(klass, &sig, &generic))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILED: unable to obtain a class signature during SingleStep callback\n\n");
        return;
    }

    if (sig != NULL) {
        NSK_DISPLAY3(
            "\tmethod name: \"%s\"\n"
            "\tsignature: \"%s\"\n"
            "\tmethod declaring class: \"%s\"\n",
            methNam, methSig, sig);

        if (stepEv[1] == 1) {
            result = STATUS_FAILED;
            NSK_COMPLAIN0("TEST FAILED: SingleStep event received after disabling the event generation\n\n");
        }
        else if ((strcmp(methNam,METHODS[0]) == 0) &&
                (strcmp(methSig,METHOD_SIGS[0]) == 0) &&
                (strcmp(sig,CLASS_SIG) == 0)) {
            stepEv[0]++;
            NSK_DISPLAY1("CHECK PASSED: SingleStep event received for the method \"%s\" as expected\n",
                methNam);
        }
        else if ((strcmp(methNam,METHODS[1]) == 0) &&
                (strcmp(methSig,METHOD_SIGS[1]) == 0) &&
                (strcmp(sig,CLASS_SIG) == 0)) {
            stepEv[1]++;
            NSK_DISPLAY1(
                "CHECK PASSED: SingleStep event received for the method \"%s\" as expected\n"
                "\tdisabling the event generation\n",
                methNam);
            if (!NSK_JVMTI_VERIFY(jvmti_env->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_SINGLE_STEP, thread))) {
                result = STATUS_FAILED;
                NSK_COMPLAIN0("TEST FAILED: cannot disable SingleStep events\n\n");
            }
        }
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
Java_nsk_jvmti_SingleStep_singlestep001_check(
        JNIEnv *env, jobject obj) {
    int i;

    for (i=0; i<METH_NUM; i++)
        if (stepEv[i] == 0) {
            result = STATUS_FAILED;
            NSK_COMPLAIN1("TEST FAILED: no SingleStep events for the method \"%s\"\n\n",
                METHODS[i]);
        }

    return result;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_singlestep001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_singlestep001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_singlestep001(JavaVM *jvm, char *options, void *reserved) {
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

    /* add capability to generate compiled method events */
    memset(&caps, 0, sizeof(jvmtiCapabilities));
    caps.can_generate_breakpoint_events = 1;
    caps.can_generate_single_step_events = 1;
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
    callbacks.SingleStep = &SingleStep;
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

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("agent lock", &agent_lock)))
        return JNI_ERR;

    return JNI_OK;
}

}
