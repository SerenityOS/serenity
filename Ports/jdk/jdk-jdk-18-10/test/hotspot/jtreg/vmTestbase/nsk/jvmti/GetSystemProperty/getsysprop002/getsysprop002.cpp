/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

#define PROPERTIES_COUNT  2

typedef struct PropertyDescStruct {
    const char* name;
    const char* value;
} PropertyDesc;

static PropertyDesc propDescList[PROPERTIES_COUNT] = {
    { "nsk.jvmti.test.property", "value_of_nsk.jvmti.test.property" },
    { "nsk.jvmti.test.property.empty", "" }
};

/* ============================================================================= */

static int checkProperty(jvmtiEnv* jvmti, const char phase[], PropertyDesc* desc) {
    int success = NSK_TRUE;
    char* value = NULL;

    NSK_DISPLAY1("Get value of tested property: %s\n", desc->name);
    if (!NSK_JVMTI_VERIFY(
            jvmti->GetSystemProperty(desc->name, &value))) {
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got value: \"%s\"\n", nsk_null_string(value));

    if (value == NULL) {
        NSK_COMPLAIN4("In %s phase GetSystemProperty() returned NULL value for property:\n"
                      "#   defined as: -D%s=\"%s\"\n"
                      "#   got value:  0x%p\n",
                        phase, desc->name, desc->value, (void*)value);
        success = NSK_FALSE;
    } else if (strcmp(value, desc->value) != 0) {
        NSK_COMPLAIN4("In %s phase GetSystemProperty() returned unexpected value for property:\n"
                      "#   defined as: -D%s=\"%s\"\n"
                      "#   got value:  %s\n",
                        phase, desc->name, desc->value, value);
        success = NSK_FALSE;
    }

    if (!NSK_JVMTI_VERIFY(
            jvmti->Deallocate((unsigned char*)value))) {
        success = NSK_FALSE;
    }

    return success;
}

static int checkProperties(jvmtiEnv* jvmti, const char phase[]) {
    int success = NSK_TRUE;
    int i;

    for (i = 0; i < PROPERTIES_COUNT; i++) {
        PropertyDesc* desc = &propDescList[i];

        NSK_DISPLAY2("Check property: -D%s=\"%s\"\n", desc->name, desc->value);
        if (!checkProperty(jvmti, phase, desc)) {
            success = NSK_FALSE;
        }
    }

    return success;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    NSK_DISPLAY0("Wait for debugee to become ready\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    NSK_DISPLAY0(">>> Check defined system properties in live phase\n");
    if (!checkProperties(jvmti, "live")) {
        nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getsysprop002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getsysprop002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getsysprop002(JavaVM *jvm, char *options, void *reserved) {
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

    NSK_DISPLAY0(">>> Check defined system properties in OnLoad phase\n");
    if (!checkProperties(jvmti, "OnLoad")) {
        nsk_jvmti_setFailStatus();
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
