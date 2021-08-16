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

#define PROPERTIES_COUNT  3
#define STEPS_COUNT       3

typedef struct PropertyDescStruct {
    const char* name;
    const char* values[STEPS_COUNT];
} PropertyDesc;

static PropertyDesc propDescList[PROPERTIES_COUNT] = {
    {
        "nsk.jvmti.test.property",
        {
            "initial_value_of_nsk.jvmti.test.property",
            "OnLoad phase value of nsk.jvmti.test.property",
            "live phase value of nsk.jvmti.test.property"
        }
    },
    {
        "nsk.jvmti.test.property.empty.old",
        {
            "",
            "OnLoad phase value of nsk.jvmti.test.property.empty.old",
            ""
        }
    },
    {
        "nsk.jvmti.test.property.empty.new",
        {
            "initial_value_of_nsk.jvmti.test.property.empty.new",
            "",
            "live phase value of nsk.jvmti.test.property.empty.new"
        }
    }
};

/* ============================================================================= */

static int checkPropertyValue(jvmtiEnv* jvmti, const char phase[],
                                const char name[], const char* expectedValue) {
    int success = NSK_TRUE;
    char* value = NULL;

    NSK_DISPLAY1("  property: %s\n", name);
    if (!NSK_JVMTI_VERIFY(jvmti->GetSystemProperty(name, &value))) {
        return NSK_FALSE;
    }
    NSK_DISPLAY1("     value: \"%s\"\n", nsk_null_string(value));

    if (value == NULL
            || strcmp(value, expectedValue) != 0) {
        NSK_COMPLAIN4("In %s phase GetSystemProperty() returned unexpected value for property:\n"
                      "#   property name: %s\n"
                      "#   got value:     \"%s\"\n"
                      "#   expected:      \"%s\"\n",
                        phase, name,
                        nsk_null_string(value), expectedValue);
        success = NSK_FALSE;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)value))) {
        success = NSK_FALSE;
    }

    return success;
}

static int checkProperties(jvmtiEnv* jvmti, const char phase[], int step) {
    int success = NSK_TRUE;
    int i;

    NSK_DISPLAY0("Check previously set values of tested properties\n");
    for (i = 0; i < PROPERTIES_COUNT; i++) {
        if (!checkPropertyValue(jvmti, phase,
                            propDescList[i].name, propDescList[i].values[step-1])) {
            success = NSK_FALSE;
        }
    }

    NSK_DISPLAY1("Set new values for tested properties %s\n",
        (step > 1) ? "(negative)" : "");
    for (i = 0; i < PROPERTIES_COUNT; i++) {
        NSK_DISPLAY1("  property: %s\n", propDescList[i].name);
        NSK_DISPLAY1("     value: \"%s\"\n", propDescList[i].values[step]);
        if (step > 1) {
            if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_WRONG_PHASE,
                   jvmti->SetSystemProperty(propDescList[i].name, propDescList[i].values[step]))) {
                success = NSK_FALSE;
            }
        } else {
            if (!NSK_JVMTI_VERIFY(
                   jvmti->SetSystemProperty(propDescList[i].name, propDescList[i].values[step]))) {
                success = NSK_FALSE;
            }
        }
    }

    NSK_DISPLAY0("Check newly set values of tested properties\n");
    for (i = 0; i < PROPERTIES_COUNT; i++) {
        if (!checkPropertyValue(jvmti, phase, propDescList[i].name,
                propDescList[i].values[1])) {
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

    NSK_DISPLAY0(">>> Check setting defined system properties in live phase\n");
    if (!checkProperties(jvmti, "live", 2)) {
        nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setsysprop002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setsysprop002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setsysprop002(JavaVM *jvm, char *options, void *reserved) {
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

    NSK_DISPLAY0(">>> Check setting defined system properties in OnLoad phase\n");
    if (!checkProperties(jvmti, "OnLoad", 1)) {
        nsk_jvmti_setFailStatus();
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
