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

static jlong timeout = 0;

/* ============================================================================= */

static int checkProperties(jvmtiEnv* jvmti, const char phase[]) {
    int success = NSK_TRUE;
    jint count = 0;
    char** properties = NULL;

    NSK_DISPLAY0("Invoke GetSystemProperties()\n");
    if (!NSK_JVMTI_VERIFY(
            jvmti->GetSystemProperties(&count, &properties))) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("    properties count: %d\n", (int)count);
    NSK_DISPLAY1("    properties list:  0x%p\n", (void*)properties);

    NSK_DISPLAY0("Check obtained properties list\n");
    if (count <= 0) {
        NSK_COMPLAIN2("In %s phase GetSystemProperties() returned unexpected properties count: %d\n",
                        phase, (int)count);
        success = NSK_FALSE;
    }

    if (properties == NULL) {
        NSK_COMPLAIN2("In %s phase GetSystemProperties() returned NULL pointer for properties list: 0x%p\n",
                        phase, (void*)properties);
        success = NSK_FALSE;
    } else {
        int i;

        NSK_DISPLAY1("Check each property: %d properties\n", (int)count);
        for (i = 0; i < count; i++) {
            NSK_DISPLAY2("    property #%d: [%s]\n", i, nsk_null_string(properties[i]));
            if (properties[i] == NULL) {
                NSK_COMPLAIN3("In %s phase GetSystemProperties() returned NULL for property #%d: 0x%p\n",
                                phase, i, (void*)properties[i]);
                success = NSK_FALSE;
            }
        }
    }
    NSK_DISPLAY0("Deallocate properties list\n");
    if (!NSK_JVMTI_VERIFY(
            jvmti->Deallocate((unsigned char*)properties))) {
        return NSK_FALSE;
    }
    NSK_DISPLAY0("  ... deallocated\n");

    return success;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    NSK_DISPLAY0("Wait for debugee to become ready\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    NSK_DISPLAY0(">>> Check system properties in live phase\n");
    if (!checkProperties(jvmti, "live")) {
        nsk_jvmti_setFailStatus();
        return;
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getsysprops001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getsysprops001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getsysprops001(JavaVM *jvm, char *options, void *reserved) {
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

    NSK_DISPLAY0(">>> Check system properties in OnLoad phase\n");
    if (!checkProperties(jvmti, "OnLoad")) {
        nsk_jvmti_setFailStatus();
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
