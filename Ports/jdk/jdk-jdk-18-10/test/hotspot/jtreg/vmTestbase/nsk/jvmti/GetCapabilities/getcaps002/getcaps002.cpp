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

#include <stdlib.h>
#include <string.h>
#include "jvmti.h"
#include "agent_common.h"
#include "jni_tools.h"
#include "jvmti_tools.h"

extern "C" {

/* ============================================================================= */

static jlong timeout = 0;

#define STATUS_FAIL     97

#define EVENTS_COUNT    2

static jvmtiEvent events[EVENTS_COUNT] = {
    JVMTI_EVENT_VM_INIT,
    JVMTI_EVENT_VM_DEATH
};

/* ============================================================================= */

/** Prints capabilities structure as raw bits. */
static void printRawCapabilities(const jvmtiCapabilities* caps) {
    const unsigned char* p = (const unsigned char*)caps;
    int size = (int) sizeof(jvmtiCapabilities);
    int i, j, k;

    nsk_printf("            ");
    for (j = 0; j < 16; j++) {
        nsk_printf(" %1X", j);
    }
    nsk_printf("\n");

    for (i = 0; i < size; i += 2) {
        int prefix = i / 2;

        nsk_printf("    0x%03X.: ", prefix);
        for (k = 0; k < 2; k++) {
            unsigned char b = *(p++);

            for (j = 0; j < 8; j++) {
                int bit = b % 2;
                b /= 2;
                nsk_printf(" %1d", bit);
            }
        }
        nsk_printf("\n");
    }
}

#define PRINT_CAP(caps, name)  nsk_printf("    %-40s: %d\n", #name, (int)caps->name)

/** Print values of known capabilities. */
static void printKnownCapabilities(const jvmtiCapabilities* caps) {
    PRINT_CAP(caps, can_tag_objects);
    PRINT_CAP(caps, can_generate_field_modification_events);
    PRINT_CAP(caps, can_generate_field_access_events);
    PRINT_CAP(caps, can_get_bytecodes);
    PRINT_CAP(caps, can_get_synthetic_attribute);
    PRINT_CAP(caps, can_get_owned_monitor_info);
    PRINT_CAP(caps, can_get_current_contended_monitor);
    PRINT_CAP(caps, can_get_monitor_info);
    PRINT_CAP(caps, can_pop_frame);
    PRINT_CAP(caps, can_redefine_classes);
    PRINT_CAP(caps, can_signal_thread);
    PRINT_CAP(caps, can_get_source_file_name);
    PRINT_CAP(caps, can_get_line_numbers);
    PRINT_CAP(caps, can_get_source_debug_extension);
    PRINT_CAP(caps, can_access_local_variables);
    PRINT_CAP(caps, can_maintain_original_method_order);
    PRINT_CAP(caps, can_generate_single_step_events);
    PRINT_CAP(caps, can_generate_exception_events);
    PRINT_CAP(caps, can_generate_frame_pop_events);
    PRINT_CAP(caps, can_generate_breakpoint_events);
    PRINT_CAP(caps, can_suspend);
    /* :1 */
    PRINT_CAP(caps, can_get_current_thread_cpu_time);
    PRINT_CAP(caps, can_get_thread_cpu_time);
    PRINT_CAP(caps, can_generate_method_entry_events);
    PRINT_CAP(caps, can_generate_method_exit_events);
    PRINT_CAP(caps, can_generate_all_class_hook_events);
    PRINT_CAP(caps, can_generate_compiled_method_load_events);
    PRINT_CAP(caps, can_generate_monitor_events);
    PRINT_CAP(caps, can_generate_vm_object_alloc_events);
    PRINT_CAP(caps, can_generate_native_method_bind_events);
    PRINT_CAP(caps, can_generate_garbage_collection_events);
    PRINT_CAP(caps, can_generate_object_free_events);
    /* :15 */
    /* :16 */
    /* :16 */
    /* :16 */
    /* :16 */
    /* :16 */
}

#define CHECK_CAP(caps, name)                                                   \
    if (caps->name != 0) {                                                      \
        success = NSK_FALSE;                                                    \
        NSK_COMPLAIN4("GetCapabilities() in %s returned not added capability:"  \
                      "#   capability: %s\n"                                    \
                      "#   got value:  %d\n"                                    \
                      "#   expected:   %d\n",                                   \
                        where, #name, (int)caps->name, 0);                      \
    }

/**
 * Check value of capabilities.
 * @returns NSK_FALSE if any error occured.
 */
static int checkCapabilitiesValue(jvmtiCapabilities* caps, const char where[]) {
    int success = NSK_TRUE;

    CHECK_CAP(caps, can_tag_objects);
    CHECK_CAP(caps, can_generate_field_modification_events);
    CHECK_CAP(caps, can_generate_field_access_events);
    CHECK_CAP(caps, can_get_bytecodes);
    CHECK_CAP(caps, can_get_synthetic_attribute);
    CHECK_CAP(caps, can_get_owned_monitor_info);
    CHECK_CAP(caps, can_get_current_contended_monitor);
    CHECK_CAP(caps, can_get_monitor_info);
    CHECK_CAP(caps, can_pop_frame);
    CHECK_CAP(caps, can_redefine_classes);
    CHECK_CAP(caps, can_signal_thread);
    CHECK_CAP(caps, can_get_source_file_name);
    CHECK_CAP(caps, can_get_line_numbers);
    CHECK_CAP(caps, can_get_source_debug_extension);
    CHECK_CAP(caps, can_access_local_variables);
    CHECK_CAP(caps, can_maintain_original_method_order);
    CHECK_CAP(caps, can_generate_single_step_events);
    CHECK_CAP(caps, can_generate_exception_events);
    CHECK_CAP(caps, can_generate_frame_pop_events);
    CHECK_CAP(caps, can_generate_breakpoint_events);
    CHECK_CAP(caps, can_suspend);
    /* :1 */
    CHECK_CAP(caps, can_get_current_thread_cpu_time);
    CHECK_CAP(caps, can_get_thread_cpu_time);
    CHECK_CAP(caps, can_generate_method_entry_events);
    CHECK_CAP(caps, can_generate_method_exit_events);
    CHECK_CAP(caps, can_generate_all_class_hook_events);
    CHECK_CAP(caps, can_generate_compiled_method_load_events);
    CHECK_CAP(caps, can_generate_monitor_events);
    CHECK_CAP(caps, can_generate_vm_object_alloc_events);
    CHECK_CAP(caps, can_generate_native_method_bind_events);
    CHECK_CAP(caps, can_generate_garbage_collection_events);
    CHECK_CAP(caps, can_generate_object_free_events);
    /* :15 */
    /* :16 */
    /* :16 */
    /* :16 */
    /* :16 */
    /* :16 */

    return success;
}

/**
 * Get and check current capabilities.
 * @returns NSK_FALSE if any error occured.
 */
static int checkCapabilities(jvmtiEnv* jvmti, const char where[]) {
    int success = NSK_TRUE;
    jvmtiCapabilities caps;

    memset(&caps, 0, sizeof(jvmtiCapabilities));

    NSK_DISPLAY0("GetCapabilities() for current JVMTI env\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps))) {
        return NSK_FALSE;
    }

    NSK_DISPLAY0("Got raw capabilities:\n");
    printRawCapabilities(&caps);

    NSK_DISPLAY0("Known capabilities:\n");
    printKnownCapabilities(&caps);

    NSK_DISPLAY0("Checking capabilities value:\n");
    success = checkCapabilitiesValue(&caps, where);
    NSK_DISPLAY0("  ... checked\n");

    return success;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    NSK_DISPLAY0("Wait for debugee to become ready\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    NSK_DISPLAY0(">>> Testcase #3: Check capabilities in agent thread\n");
    if (!checkCapabilities(jvmti, "agent thread")) {
        nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ============================================================================= */

/**
 * Callback for VM_INIT event.
 */
JNIEXPORT void JNICALL
callbackVMInit(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread) {

    NSK_DISPLAY0(">>> Testcase #2: Check capabilities in VM_INIT callback\n");
    if (!checkCapabilities(jvmti, "VM_INIT callback")) {
        nsk_jvmti_setFailStatus();
    }

}

/**
 * Callback for VM_DEATH event.
 */
JNIEXPORT void JNICALL
callbackVMDeath(jvmtiEnv* jvmti, JNIEnv* jni) {
    int success = NSK_TRUE;

    NSK_DISPLAY0(">>> Testcase #4: Check capabilities in VM_DEATH callback\n");
    success = checkCapabilities(jvmti, "VM_DEATH callback");

    NSK_DISPLAY1("Disable events: %d events\n", EVENTS_COUNT);
    if (!nsk_jvmti_enableEvents(JVMTI_DISABLE, EVENTS_COUNT, events, NULL)) {
        success = NSK_FALSE;
    } else {
        NSK_DISPLAY0("  ... disabled\n");
    }

    if (!success) {
        NSK_DISPLAY1("Exit with FAIL exit status: %d\n", STATUS_FAIL);
        NSK_BEFORE_TRACE(exit(STATUS_FAIL));
    }
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getcaps002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getcaps002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getcaps002(JavaVM *jvm, char *options, void *reserved) {
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

    {
        jvmtiEventCallbacks eventCallbacks;

        memset(&eventCallbacks, 0, sizeof(eventCallbacks));
        eventCallbacks.VMInit = callbackVMInit;
        eventCallbacks.VMDeath = callbackVMDeath;
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks)))) {
            return JNI_ERR;
        }

    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    NSK_DISPLAY0(">>> Testcase #1: Check capabilities in Agent_OnLoad()\n");
    if (!checkCapabilities(jvmti, "Agent_OnLoad()")) {
        nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY1("Enable events: %d events\n", EVENTS_COUNT);
    if (nsk_jvmti_enableEvents(JVMTI_ENABLE, EVENTS_COUNT, events, NULL)) {
        NSK_DISPLAY0("  ... enabled\n");
    }

    return JNI_OK;
}

/* ============================================================================= */

}
