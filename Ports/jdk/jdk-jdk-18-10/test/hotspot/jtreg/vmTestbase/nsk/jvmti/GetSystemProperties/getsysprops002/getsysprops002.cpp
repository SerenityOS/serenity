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
    int found;
} PropertyDesc;

static PropertyDesc propDescList[PROPERTIES_COUNT] = {
    { "nsk.jvmti.test.property", "value_of_nsk.jvmti.test.property", 0 },
    { "nsk.jvmti.test.property.empty", "", 0 }
};

/* ============================================================================= */

/** Agent algorithm. */
static int checkProperties (jvmtiEnv* jvmti, const char phase[]) {
    int success = NSK_TRUE;
    jint count = 0;
    char** properties = NULL;

    NSK_DISPLAY0("Get system properties list\n");
    if (!NSK_JVMTI_VERIFY(
            jvmti->GetSystemProperties(&count, &properties))) {
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got properties: %d\n", (int)count);

    if (!NSK_VERIFY(properties != NULL)) {
        return NSK_FALSE;
    }

    NSK_DISPLAY0("Find tested properties defined via command line\n");
    {
        int foundWithValue = 0;
        int foundEmpty = 0;
        int i, j;

        for (j = 0; j < PROPERTIES_COUNT; j++) {
            propDescList[j].found = 0;
        }

        for (i = 0; i < count; i++) {
            NSK_DISPLAY2("    property #%d: [%s]\n", i, nsk_null_string(properties[i]));
            if (properties[i] != NULL) {
                for (j = 0; j < PROPERTIES_COUNT; j++) {
                    if (strcmp(properties[i], propDescList[j].name) == 0) {
                        NSK_DISPLAY1("SUCCESS: found tested property: %s\n",
                                                        propDescList[j].name);
                        propDescList[j].found++;
                        break;
                    }
                }
            }
        }

        for (j = 0; j < PROPERTIES_COUNT; j++) {
            if (propDescList[j].found <= 0) {
                NSK_COMPLAIN3("In %s phase GetSystemPropeties() returns no property defined via command line:\n"
                              "#   -D%s=\"%s\"\n",
                              phase, propDescList[j].name, propDescList[j].value);
                success = NSK_FALSE;
            } else if (propDescList[j].found > 1) {
                NSK_COMPLAIN4("In %s phase GetSystemPropeties() returns too many entries for property defined via command line:\n"
                              "#   -D%s=\"%s\"\n"
                              "#   found entries: %d\n",
                              phase, propDescList[j].name, propDescList[j].value,
                              propDescList[j].found);
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
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getsysprops002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getsysprops002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getsysprops002(JavaVM *jvm, char *options, void *reserved) {
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
