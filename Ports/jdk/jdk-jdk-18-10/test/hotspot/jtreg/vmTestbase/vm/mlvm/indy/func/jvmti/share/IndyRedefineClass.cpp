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

static jboolean gIsMethodEntryWorking = JNI_FALSE;
static jboolean gIsSingleStepWorking = JNI_FALSE;
static jboolean gIsErrorOccured = JNI_FALSE;

static jboolean gIsDebuggerCompatible = JNI_FALSE;

static jint gPopFrameDepth = 2;

typedef struct TLS {
    jint countOfFramesToPop;
} TLSStruct;

static char * gszRedefineTriggerMethodName = (char*) "NONE";
static char * gszRedefinedClassFileName = (char*) "NONE";
static jboolean gIsClassRedefined = JNI_FALSE;

JNIEXPORT void JNICALL
Java_vm_mlvm_indy_func_jvmti_share_IndyRedefineClass_setRedefineTriggerMethodName(JNIEnv * pEnv, jclass clazz, jstring name) {
    copyFromJString(pEnv, name, &gszRedefineTriggerMethodName);
    NSK_DISPLAY1("Setting redefine trigger method name to %s\n", gszRedefineTriggerMethodName);
}

JNIEXPORT void JNICALL
Java_vm_mlvm_indy_func_jvmti_share_IndyRedefineClass_setRedefinedClassFileName(JNIEnv * pEnv, jclass clazz, jstring name) {
    copyFromJString(pEnv, name, &gszRedefinedClassFileName);
    NSK_DISPLAY1("Setting redefined class name to %s\n", gszRedefinedClassFileName);
    gIsClassRedefined = JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_vm_mlvm_indy_func_jvmti_share_IndyRedefineClass_setPopFrameDepthAfterRedefine(JNIEnv * pEnv, jclass clazz, jint depth) {
    gPopFrameDepth = depth;
}

JNIEXPORT jboolean JNICALL
Java_vm_mlvm_indy_func_jvmti_share_IndyRedefineClass_checkStatus(JNIEnv * pEnv, jclass clazz) {
        NSK_DISPLAY0("The following values should be non-zero for test to pass:\n");
    NSK_DISPLAY1("Method entry event fired? %i\n", gIsMethodEntryWorking);
    NSK_DISPLAY1("Single step event fired? %i\n", gIsSingleStepWorking);
        NSK_DISPLAY0("The following value should be zero for test to pass:\n");
    NSK_DISPLAY1("Any other error occured? %i\n", gIsErrorOccured);
    return gIsMethodEntryWorking && gIsSingleStepWorking && !gIsErrorOccured;
}

static void popFrameLogic(jvmtiEnv * jvmti_env, jthread thread) {

    TLSStruct * tls = (TLSStruct *) getTLS(jvmti_env, thread, sizeof(TLSStruct));

    if (!tls)
        return;

    if (tls->countOfFramesToPop <= 0) {

        NSK_DISPLAY0("Disabling single step\n");
        if (!NSK_JVMTI_VERIFY(jvmti_env->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_SINGLE_STEP, NULL)))
            gIsErrorOccured = JNI_TRUE;

    } else {

        NSK_DISPLAY0("Enabling single step\n");
        if (!NSK_JVMTI_VERIFY(jvmti_env->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_SINGLE_STEP, NULL)))
            gIsErrorOccured = JNI_TRUE;

        if (tls->countOfFramesToPop == 1) {
            NSK_DISPLAY0("Popping a frame\n");
            if (!NSK_JVMTI_VERIFY(jvmti_env->PopFrame(thread)))
                gIsErrorOccured = JNI_TRUE;
        } else {
            NSK_DISPLAY0("Forcing early return\n");
            if (!NSK_JVMTI_VERIFY(jvmti_env->ForceEarlyReturnVoid(thread)))
                gIsErrorOccured = JNI_TRUE;
        }

        --tls->countOfFramesToPop;
    }
}

static void JNICALL
MethodEntry(jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jmethodID method) {

    struct MethodName * mn;
        TLSStruct * tls;
    jclass clazz;

    gIsMethodEntryWorking = JNI_TRUE;
    mn = getMethodName(jvmti_env, method);
    if (!mn)
        return;

    if (strcmp(mn->methodName, gszRedefineTriggerMethodName) != 0) {
        free(mn);
        return;
    }

    NSK_DISPLAY2("Entering redefine tigger method: %s.%s\n", mn->classSig, mn->methodName);
    free(mn); mn = NULL;

    if (gIsClassRedefined) {
        NSK_DISPLAY0("Class is already redefined.\n");
        return;
    }

    NSK_DISPLAY1("Redefining class %s\n", gszRedefinedClassFileName);

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodDeclaringClass(method, &clazz)))
        return;

    if (!NSK_VERIFY(nsk_jvmti_redefineClass(jvmti_env, clazz, gszRedefinedClassFileName))) {
        gIsErrorOccured = JNI_TRUE;
        return;
    }

    gIsClassRedefined = JNI_TRUE;

    tls = (TLSStruct *) getTLS(jvmti_env, thread, sizeof(TLSStruct));
    tls->countOfFramesToPop = gPopFrameDepth;

    popFrameLogic(jvmti_env, thread);
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
        NSK_DISPLAY0("Error in Single step event: locationToString failed\n");
        gIsErrorOccured = JNI_TRUE;
    } else {
        NSK_DISPLAY1("Single step event: %s\n", locStr);
        free(locStr);
    }

    popFrameLogic(jvmti_env, thread);
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
    caps.can_pop_frame = 1;
    caps.can_force_early_return = 1;
    caps.can_redefine_classes = 1;

    if (!NSK_JVMTI_VERIFY(gJvmtiEnv->AddCapabilities(&caps)))
        return JNI_ERR;

    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.MethodEntry = &MethodEntry;
    callbacks.SingleStep = &SingleStep;

    if (!NSK_JVMTI_VERIFY(gJvmtiEnv->SetEventCallbacks(&callbacks, sizeof(callbacks))))
            return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(gJvmtiEnv->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_ENTRY, NULL)))
            return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(gJvmtiEnv->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_SINGLE_STEP, NULL)))
            return JNI_ERR;

    return JNI_OK;
}

}
