/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include <stdlib.h>
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"
#include "jvmti_tools.h"
#include "mlvmJvmtiUtils.h"

extern "C" {

static jvmtiEnv* gJvmtiEnv = NULL;

static char * gszDebuggeeMethodName = (char*) "NONE";
static char * gszDebuggeeClassName = (char*) "NONE";
static jboolean gIsMethodEntryWorking = JNI_FALSE;
static jboolean gIsSingleStepWorking = JNI_FALSE;
static jboolean gIsBreakpointWorking = JNI_FALSE;
static jboolean gErrorHappened = JNI_FALSE;

static jboolean gIsBreakpointSet = JNI_FALSE;
static jboolean gIsFirstCall = JNI_TRUE;
static jboolean gIsDebuggerCompatible = JNI_FALSE;

JNIEXPORT void JNICALL
Java_vm_mlvm_indy_func_jvmti_stepBreakPopReturn_INDIFY_1Test_setDebuggeeMethodName(JNIEnv * pEnv, jclass clazz, jstring name) {
    copyFromJString(pEnv, name, &gszDebuggeeMethodName);
    NSK_DISPLAY1("Setting debuggee method name to %s\n", gszDebuggeeMethodName);
}

JNIEXPORT void JNICALL
Java_vm_mlvm_indy_func_jvmti_stepBreakPopReturn_INDIFY_1Test_setDebuggeeClassName(JNIEnv * pEnv, jclass clazz, jstring name) {
    copyFromJString(pEnv, name, &gszDebuggeeClassName);
    NSK_DISPLAY1("Setting debuggee class name to %s\n", gszDebuggeeClassName);
}

JNIEXPORT jboolean JNICALL
Java_vm_mlvm_indy_func_jvmti_stepBreakPopReturn_INDIFY_1Test_checkStatus(JNIEnv * pEnv, jclass clazz) {
    NSK_DISPLAY1("Are we running in debugger-compatible mode? %i\n", gIsDebuggerCompatible);
    NSK_DISPLAY0("The following values should be non-zero for test to pass:\n");
    NSK_DISPLAY1("Method entry event fired? %i\n", gIsMethodEntryWorking);
    NSK_DISPLAY1("Single step event fired? %i\n", gIsSingleStepWorking);
    if (!gIsDebuggerCompatible)
        NSK_DISPLAY1("Breakpoint event fired? %i\n", gIsBreakpointWorking);

    return gIsMethodEntryWorking && !gErrorHappened && gIsSingleStepWorking
        && (gIsBreakpointWorking || gIsDebuggerCompatible);
}

static void JNICALL
MethodEntry(jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jmethodID method) {

    struct MethodName * mn;

    mn = getMethodName(jvmti_env, method);
    if (!mn)
        return;

    if (strcmp(mn->classSig, gszDebuggeeClassName) == 0) {
        NSK_DISPLAY2("Entering method: %s.%s\n", mn->classSig, mn->methodName);

        if (strcmp(mn->methodName, gszDebuggeeMethodName) == 0) {
            gIsMethodEntryWorking = JNI_TRUE;

            if (!gIsBreakpointSet)
                NSK_JVMTI_VERIFY(jvmti_env->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_SINGLE_STEP, NULL));
        }
    }

    free(mn);
}

static void JNICALL
SingleStep(jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jmethodID method,
            jlocation location) {

    char * locStr;
    gIsSingleStepWorking = JNI_TRUE;

    locStr = locationToString(jvmti_env, method, location);

    if (locStr == NULL) {
        NSK_DISPLAY0("Error: Single step event has no location\n");
        gErrorHappened = JNI_TRUE;
    } else {
        NSK_DISPLAY1("Single step event: %s\n", locStr);
        free(locStr);
    }

    NSK_JVMTI_VERIFY(gJvmtiEnv->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_SINGLE_STEP, NULL));

    if (!gIsDebuggerCompatible) {
        if (!NSK_JVMTI_VERIFY(jvmti_env->SetBreakpoint(method, location)))
            return;

        NSK_JVMTI_VERIFY(gJvmtiEnv->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_BREAKPOINT, NULL));
        gIsBreakpointSet = JNI_TRUE;

        NSK_DISPLAY0("Pop a frame\n");
        NSK_JVMTI_VERIFY(gJvmtiEnv->PopFrame(thread));
    } else {
        if (gIsFirstCall) {
            NSK_DISPLAY0("Pop a frame\n");
            NSK_JVMTI_VERIFY(gJvmtiEnv->PopFrame(thread));
            gIsFirstCall = JNI_FALSE;
        } else {
            gIsFirstCall = JNI_TRUE;
        }
    }
}

static void JNICALL
Breakpoint(jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jmethodID method,
            jlocation location) {


    char * locStr;
    gIsBreakpointWorking = JNI_TRUE;

    locStr = locationToString(jvmti_env, method, location);
    if (locStr == NULL) {
        NSK_DISPLAY0("Error: Breakpoint event has no location\n");
        gErrorHappened = JNI_TRUE;
    } else {
        NSK_DISPLAY1("Breakpoint event at: %s\n", locStr);
        free(locStr);
    }

    NSK_JVMTI_VERIFY(jvmti_env->ClearBreakpoint(method, location));
    NSK_JVMTI_VERIFY(gJvmtiEnv->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_BREAKPOINT, NULL));
    gIsBreakpointSet = JNI_FALSE;

    NSK_DISPLAY0("Forcing early return.\n");
    NSK_JVMTI_VERIFY(jvmti_env->ForceEarlyReturnInt(thread, 0));
}

jint Agent_Initialize(JavaVM * vm, char * options, void * reserved) {
    jvmtiEventCallbacks callbacks;
    jvmtiCapabilities caps;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    if (!NSK_VERIFY((gJvmtiEnv = nsk_jvmti_createJVMTIEnv(vm, reserved)) != NULL))
        return JNI_ERR;

    if (nsk_jvmti_findOptionValue("debuggerCompatible")) {
        gIsDebuggerCompatible = JNI_TRUE;
    }

    memset(&caps, 0, sizeof(caps));
    caps.can_generate_method_entry_events = 1;
    caps.can_generate_single_step_events = 1;
    caps.can_generate_breakpoint_events = !gIsDebuggerCompatible;
    caps.can_pop_frame = 1;
    caps.can_force_early_return = 1;

    if (!NSK_JVMTI_VERIFY(gJvmtiEnv->AddCapabilities(&caps)))
        return JNI_ERR;

    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.MethodEntry = &MethodEntry;
    callbacks.SingleStep = &SingleStep;
    callbacks.Breakpoint = &Breakpoint;

    if (!NSK_JVMTI_VERIFY(gJvmtiEnv->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(gJvmtiEnv->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_ENTRY, NULL)))
        return JNI_ERR;

    return JNI_OK;
}
}
