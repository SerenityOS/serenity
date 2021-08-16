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

static void JNICALL
callbackExtensionEvent(jvmtiEnv* jvmti, ...) {
    NSK_DISPLAY0("    event: callbackExtensionEvent\n");
}

/** Check extension events. */
static int checkExtensions(jvmtiEnv* jvmti, const char phase[]) {
    int success = NSK_TRUE;
    jint extCount = 0;
    jvmtiExtensionEventInfo* extList = NULL;
    int i;

    NSK_DISPLAY0("Get extension events list\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetExtensionEvents(&extCount, &extList))) {
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got count: %d\n", (int)extCount);
    NSK_DISPLAY1("  ... got list:  0x%p\n", (void*)extList);

    if (extCount <= 0) {
        NSK_DISPLAY1("# WARNING: No extension events implemented to check: %d\n", extCount);
    } else {

        if (!NSK_VERIFY(extList != NULL))
            return NSK_FALSE;

        NSK_DISPLAY1("Set/clear callback for each extension event: %d events\n", (int)extCount);
        for (i = 0; i < extCount; i++) {
            NSK_DISPLAY1("  event #%d:\n", i);
            NSK_DISPLAY1("    event_index: %d\n", (int)extList[i].extension_event_index);
            NSK_DISPLAY1("    id:          \"%s\"\n", nsk_null_string(extList[i].id));
            NSK_DISPLAY1("    short_desc:  \"%s\"\n", nsk_null_string(extList[i].short_description));
            NSK_DISPLAY1("    param_count: %d\n", (int)extList[i].param_count);

            NSK_DISPLAY1("    ... setting callback: 0x%p\n", (void*)callbackExtensionEvent);
            if (!NSK_JVMTI_VERIFY(
                    jvmti->SetExtensionEventCallback(extList[i].extension_event_index, callbackExtensionEvent))) {
                success = NSK_FALSE;
            }
            NSK_DISPLAY0("    ... done\n");

            NSK_DISPLAY1("    ... clearing callback: 0x%p\n", (void*)NULL);
            if (!NSK_JVMTI_VERIFY(
                    jvmti->SetExtensionEventCallback(extList[i].extension_event_index, NULL))) {
                success = NSK_FALSE;
            }
            NSK_DISPLAY0("    ... done\n");
        }
    }

    NSK_DISPLAY1("Deallocate extension events list: 0x%p\n", (void*)extList);
    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)extList))) {
        return NSK_FALSE;
    }
    NSK_DISPLAY0("  ... deallocated\n");

    return success;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    NSK_DISPLAY0("Wait for debugee class ready\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    NSK_DISPLAY0(">>> Testcase #2: Check setting extension event callbacks in live phase\n");
    {
        if (!checkExtensions(jvmti, "live")) {
            nsk_jvmti_setFailStatus();
        }
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setextevent001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setextevent001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setextevent001(JavaVM *jvm, char *options, void *reserved) {
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

    NSK_DISPLAY0(">>> Testcase #1: Check setting extension event callbacks in OnLoad phase\n");
    {
        if (!checkExtensions(jvmti, "OnLoad")) {
            nsk_jvmti_setFailStatus();
        }
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
