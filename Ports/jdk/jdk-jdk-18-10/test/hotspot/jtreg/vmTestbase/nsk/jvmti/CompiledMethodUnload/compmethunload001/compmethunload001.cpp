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

extern "C" {

#define STATUS_FAILED 2
#define PASSED 0

static volatile jint result = PASSED;
static volatile int compunload = 0;
static volatile int class_unloaded = 0;
static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;

/** callback functions **/
void JNICALL
VMInit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr) {
    NSK_DISPLAY0("VMInit event received\n\n");

    if (!NSK_JVMTI_VERIFY(jvmti_env->GenerateEvents(JVMTI_EVENT_COMPILED_METHOD_LOAD))) {
        NSK_COMPLAIN0("TEST FAILED: unable to generate events to represent the current state of the VM\n");
        result = STATUS_FAILED;
    }
}

void JNICALL
CompiledMethodLoad(jvmtiEnv *jvmti_env, jmethodID method, jint code_size,
        const void* code_addr,  jint map_length, const jvmtiAddrLocationMap* map,
        const void* compile_info) {
    char *name;
    char *sig;
    char *generic;

    NSK_DISPLAY0("CompiledMethodLoad event received for:\n");

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &name, &sig, &generic))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILURE: unable to obtain method info\n\n");
        return;
    }
    NSK_DISPLAY4("\tmethod: name=\"%s\" signature=\"%s\"\n\tcompiled code size=%d\n\tnumber of address location map entries=%d\n\n",
        name, sig, code_size, map_length);
}

void JNICALL
CompiledMethodUnload(jvmtiEnv *jvmti_env, jmethodID method,
        const void* code_addr) {
    char *name;
    char *sig;
    jvmtiPhase phase;
    jvmtiError err;

    compunload++;

    /* jmethodID: Identifies a Java programming language method, initializer, or constructor.
     * jmethodIDs returned by JVM TI functions and events may be safely stored. However, if
     * the class is unloaded, they become invalid and must not be used. */

    NSK_DISPLAY0("CompiledMethodUnload event received\n");
    // Check for the case that the class has been unloaded
    err = jvmti_env->GetMethodName(method, &name, &sig, NULL);
    if (err == JVMTI_ERROR_NONE) {
        NSK_DISPLAY3("for: \tmethod: name=\"%s\" signature=\"%s\"\n\tnative address=0x%p\n",
          name, sig, code_addr);
        jvmti_env->Deallocate((unsigned char*)name);
        jvmti_env->Deallocate((unsigned char*)sig);
    } else {
        // The class metadata has been completely unloaded so the name is not available.
        NSK_DISPLAY0("for: \tmethod: name=<not available>\n");
    }

    // Count unloaded events
    class_unloaded++;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetPhase(&phase))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILURE: unable to obtain phase of the VM execution\n");
        return;
    }

    if (phase != JVMTI_PHASE_START && phase != JVMTI_PHASE_LIVE) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: CompiledMethodUnload event received during wrong phase %s\n",
            TranslatePhase(phase));
    }
    else
        NSK_DISPLAY0("CHECK PASSED: CompiledMethodUnload event received during the start or live phase as expected\n\n");
}
/************************/

JNIEXPORT jint JNICALL
Java_nsk_jvmti_CompiledMethodUnload_compmethunload001_check(
        JNIEnv *env, jobject obj) {
    if (!caps.can_generate_compiled_method_load_events)
        return PASSED;

    if (compunload == 0)
        NSK_DISPLAY0("Warning: no CompiledMethodUnload events\n\tthe test has no results\n");

    return result;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_CompiledMethodUnload_compmethunload001_unloaded(
        JNIEnv *env, jobject obj) {
    if (!caps.can_generate_compiled_method_load_events)
        return 1;

    return class_unloaded;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_compmethunload001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_compmethunload001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_compmethunload001(JavaVM *jvm, char *options, void *reserved) {
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

    /* add capability to generate compiled method events */
    memset(&caps, 0, sizeof(jvmtiCapabilities));
    caps.can_generate_compiled_method_load_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    if (!caps.can_generate_compiled_method_load_events)
        NSK_DISPLAY0("Warning: generation of compiled method events is not implemented\n");

    /* set event callback */
    NSK_DISPLAY0("setting event callbacks ...\n");
    (void) memset(&callbacks, 0, sizeof(callbacks));
    callbacks.VMInit = &VMInit;
    callbacks.CompiledMethodLoad = &CompiledMethodLoad;
    callbacks.CompiledMethodUnload = &CompiledMethodUnload;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    NSK_DISPLAY0("setting event callbacks done\nenabling JVMTI events ...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_LOAD, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_UNLOAD, NULL)))
        return JNI_ERR;
    NSK_DISPLAY0("enabling the events done\n\n");

    return JNI_OK;
}

}
