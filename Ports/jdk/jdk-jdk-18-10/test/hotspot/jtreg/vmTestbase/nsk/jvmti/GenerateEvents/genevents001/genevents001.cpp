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

#include <string.h>
#include "jvmti.h"
#include "agent_common.h"
#include "jni_tools.h"
#include "jvmti_tools.h"

extern "C" {

/* ============================================================================= */

/* scaffold objects */
static jlong timeout = 0;

/* constants */
#define EVENTS_COUNT    3

/* tested events */
static jvmtiEvent eventsList[EVENTS_COUNT] = {
    JVMTI_EVENT_COMPILED_METHOD_LOAD,
    JVMTI_EVENT_COMPILED_METHOD_UNLOAD,
    JVMTI_EVENT_DYNAMIC_CODE_GENERATED
};

/* event counts */
static int eventsCountList[EVENTS_COUNT];

/* ============================================================================= */

/** Check if all expected events received. */
static int checkEvents() {
    int success = NSK_TRUE;

    NSK_DISPLAY0("Events received:\n");
    NSK_DISPLAY1("   COMPILED_METHOD_LOAD:   %d events\n", eventsCountList[0]);
    NSK_DISPLAY1("   COMPILED_METHOD_UNLOAD: %d events\n", eventsCountList[1]);
    NSK_DISPLAY1("   DYNAMIC_CODE_GENERATED: %d events\n", eventsCountList[2]);

    if (eventsCountList[0] <= 0) {
        NSK_DISPLAY0("# WARNING: GenerateEvents() produced no COMPILED_METHOD_LOAD events\n");
        NSK_DISPLAY0("#    (but methods may not be compiled)\n");
    }

    if (eventsCountList[2] <= 0) {
        NSK_DISPLAY0("# WARNING: GenerateEvents() produced no DYNAMIC_CODE_GENERATED events\n");
        NSK_DISPLAY0("#    (but dynamic code may not be generated)\n");
    }

    if (eventsCountList[1] > 0) {
        NSK_DISPLAY1("# WARNING: COMPILED_METHOD_UNLOAD events were received: %d events\n",
                     eventsCountList[1]);
    }

    return success;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    NSK_DISPLAY0("Wait for tested method forced to compile\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    NSK_DISPLAY0(">>> Testcase #1: Check if GenerateEvents() sends missed events\n");
    {
        int i;

        for (i = 0; i < EVENTS_COUNT; i++) {
            eventsCountList[i] = 0;
        }

        NSK_DISPLAY1("Enable events: %d events\n", EVENTS_COUNT);
        if (!nsk_jvmti_enableEvents(JVMTI_ENABLE, EVENTS_COUNT, eventsList, NULL)) {
            nsk_jvmti_setFailStatus();
        }

        NSK_DISPLAY0("Call GenerateEvents() to send missed events\n");
        if (!NSK_JVMTI_VERIFY(jvmti->GenerateEvents(JVMTI_EVENT_COMPILED_METHOD_LOAD))) {
            nsk_jvmti_setFailStatus();
        }

        if (!NSK_JVMTI_VERIFY(jvmti->GenerateEvents(JVMTI_EVENT_DYNAMIC_CODE_GENERATED))) {
            nsk_jvmti_setFailStatus();
        }

        NSK_DISPLAY1("Disable events: %d events\n", EVENTS_COUNT);
        if (!nsk_jvmti_enableEvents(JVMTI_DISABLE, EVENTS_COUNT, eventsList, NULL)) {
            nsk_jvmti_setFailStatus();
        }

        NSK_DISPLAY0("Check received events\n");
        if (!checkEvents()) {
            nsk_jvmti_setFailStatus();
        }
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ============================================================================= */

/**
 * Callback for COMPILED_METHOD_LOAD event.
 */
JNIEXPORT void JNICALL
callbackCompiledMethodLoad(jvmtiEnv* jvmti, jmethodID method,
                            jint code_size, const void* code_addr,
                            jint map_length, const jvmtiAddrLocationMap* map,
                            const void* compile_info) {
    NSK_DISPLAY3("  <COMPILED_METHOD_LOAD>:   method: 0x%p, code: 0x%p, size: %d\n",
                        (void*)method, (void*)code_addr, (int)code_size);
    eventsCountList[0]++;
}

/**
 * Callback for COMPILED_METHOD_UNLOAD event.
 */
JNIEXPORT void JNICALL
callbackCompiledMethodUnload(jvmtiEnv* jvmti, jmethodID method,
                             const void* code_addr) {
    NSK_DISPLAY1("  <COMPILED_METHOD_UNLOAD>: method: 0x%p\n",
                        (void*)method);
    eventsCountList[1]++;
}

/**
 * Callback for DYNAMIC_CODE_GENERATED event.
 */
JNIEXPORT void JNICALL
callbackDynamicCodeGenerated(jvmtiEnv* jvmti, const char* name, const void* address, jint length) {
    NSK_DISPLAY3("  <DYNAMIC_CODE_GENERATED>: name: %s, address: 0x%p, length: %d\n",
                        nsk_null_string(name), (void*)address, (int)length);
    eventsCountList[2]++;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_genevents001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_genevents001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_genevents001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    /* add required capabilities */
    {
        jvmtiCapabilities caps;
        memset(&caps, 0, sizeof(caps));
        caps.can_generate_compiled_method_load_events = 1;
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
            return JNI_ERR;
    }

    /* set event callbacks */
    {
        jvmtiEventCallbacks eventCallbacks;
        memset(&eventCallbacks, 0, sizeof(eventCallbacks));
        eventCallbacks.CompiledMethodLoad = callbackCompiledMethodLoad;
        eventCallbacks.CompiledMethodUnload = callbackCompiledMethodUnload;
        eventCallbacks.DynamicCodeGenerated = callbackDynamicCodeGenerated;
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks))))
            return JNI_ERR;
    }

    return JNI_OK;
}

/* ============================================================================= */

}
