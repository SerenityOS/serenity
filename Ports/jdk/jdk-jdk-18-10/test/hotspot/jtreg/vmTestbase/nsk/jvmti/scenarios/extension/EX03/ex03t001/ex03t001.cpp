/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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

static JNIEnv *jni = NULL;
static jvmtiEnv* jvmti = NULL;
static jlong timeout = 0;
static jboolean eventEnabled = JNI_FALSE;
static volatile jboolean eventReceived1 = JNI_FALSE, eventReceived2 = JNI_FALSE;
static jclass checkedClass;
static jrawMonitorID eventMon;


/* ============================================================================= */

static void JNICALL
ClassUnload(jvmtiEnv* jvmti_env, ...) {
    JNIEnv *jni_env = NULL;
    va_list ap;

    va_start(ap, jvmti_env);
    jni_env = va_arg(ap, JNIEnv *);
    const char * name = va_arg(ap, const char *);
    va_end(ap);

    // The name argument should never be null
    if (name == NULL) {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN0("ClassUnload: 'name' input parameter is NULL.\n");
    } else {
        NSK_DISPLAY1("Class unloaded %s\n", name);
    }

    NSK_DISPLAY0("Received ClassUnload event.\n");
    if (eventEnabled == JNI_TRUE) {
        eventReceived1 = JNI_TRUE;
    } else {
        eventReceived2 = JNI_TRUE;
    }

    /* Notify main agent thread */
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(eventMon))) {
        nsk_jvmti_setFailStatus();
    }
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorNotify(eventMon))) {
        nsk_jvmti_setFailStatus();
    }
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(eventMon))) {
        nsk_jvmti_setFailStatus();
    }
}

jboolean isClassUnloadingEnabled() {
    jint extCount, i;
    jvmtiExtensionFunctionInfo* extList;
    jboolean found = JNI_FALSE;
    jboolean enabled = JNI_FALSE;
    jvmtiError err;

    NSK_DISPLAY0("Get extension functions list\n");

    if (!NSK_JVMTI_VERIFY(jvmti->GetExtensionFunctions(&extCount, &extList))) {
        nsk_jvmti_setFailStatus();
        return JNI_FALSE;
    }

    for (i = 0; i < extCount; i++) {
        if (strcmp(extList[i].id, (char*)"com.sun.hotspot.functions.IsClassUnloadingEnabled") == 0) {
            found = JNI_TRUE;

            err = (*extList[i].func)(jvmti, &enabled);
            if (err != JVMTI_ERROR_NONE) {
                NSK_COMPLAIN1("Error during invocation of IsClassUnloadingEnabled function: %d\n", err);
                nsk_jvmti_setFailStatus();
                return JNI_FALSE;
            }
        }
    }
    if (found == JNI_FALSE) {
        NSK_COMPLAIN0("IsClassUnloadingEnabled was not found among extension functions.\n");
        nsk_jvmti_setFailStatus();
        return JNI_FALSE;
    }

    return enabled;
}

jboolean checkParams(jvmtiExtensionEventInfo event) {
    // Check parameters are:
    // JNIEnv *jni_env, const char* name
    if (event.param_count != 2 ||
          event.params[0].kind != JVMTI_KIND_IN_PTR ||
          event.params[0].base_type != JVMTI_TYPE_JNIENV ||
          event.params[1].kind != JVMTI_KIND_IN_PTR ||
          event.params[1].base_type != JVMTI_TYPE_CCHAR) {
        return JNI_FALSE;
    } else {
        return JNI_TRUE;
    }
}

jboolean enableClassUnloadEvent (jboolean enable) {
    jint extCount, i;
    jvmtiExtensionEventInfo* extList;
    jboolean found = JNI_FALSE;

    NSK_DISPLAY0("Get extension events list\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetExtensionEvents(&extCount, &extList))) {
        nsk_jvmti_setFailStatus();
        return JNI_FALSE;
    }

    for (i = 0; i < extCount; i++) {
        if (strcmp(extList[i].id, (char*)"com.sun.hotspot.events.ClassUnload") == 0) {
            found = JNI_TRUE;

            NSK_DISPLAY1("%s", extList[i].short_description);

            if (!checkParams(extList[i])) {
                NSK_COMPLAIN0("ClassUnload event has wrong parameters.");
                nsk_jvmti_setFailStatus();
                return JNI_FALSE;
            }

            if (!NSK_JVMTI_VERIFY(
                    jvmti->SetExtensionEventCallback(extList[i].extension_event_index,
                                                     enable ? (jvmtiExtensionEvent)ClassUnload : NULL))) {
                nsk_jvmti_setFailStatus();
                return JNI_FALSE;
            }
            eventEnabled = enable;
            if (enable == JNI_TRUE) {
                NSK_DISPLAY1("%s callback enabled\n", extList[i].id);
            } else {
                NSK_DISPLAY1("%s callback disabled\n", extList[i].id);
            }
        }
    }
    if (found == JNI_FALSE) {
        NSK_COMPLAIN0("ClassUnload event was not found among extension events.\n");
        nsk_jvmti_setFailStatus();
        return JNI_FALSE;
    }
    return JNI_TRUE;
}


/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    do {
        if (isClassUnloadingEnabled() == JNI_FALSE) {
            NSK_COMPLAIN0("ClassUnloadingEnabled returned false.\n");
            nsk_jvmti_setFailStatus();
        }

        NSK_DISPLAY0("Wait for loading of ex03t001a class.\n");
        if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
            return;

        if (enableClassUnloadEvent(JNI_TRUE) == JNI_FALSE) {
            NSK_COMPLAIN0("Cannot set up ClassUnload event callback.\n");
            break;
        }

        NSK_DISPLAY0("Let debugee to unload ex03t001a class.\n");
        if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
            break;

        /* Wait for notifying from event's thread */
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(eventMon))) {
            nsk_jvmti_setFailStatus();
        }
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorWait(eventMon, timeout))) {
            nsk_jvmti_setFailStatus();
        }
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(eventMon))) {
            nsk_jvmti_setFailStatus();
        }

        NSK_DISPLAY0("Wait for loading of ex03t001b class.\n");
        if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
            return;

        if (enableClassUnloadEvent(JNI_FALSE) == JNI_FALSE) {
            NSK_COMPLAIN0("Cannot set off ClassUnload event callback.\n");
            break;
        }

        NSK_DISPLAY0("Let debugee to unload ex03t001b class.\n");
        if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
            return;

        /* Wait during 10 secs for notifying from event's thread */
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(eventMon))) {
            nsk_jvmti_setFailStatus();
        }
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorWait(eventMon, 10000))) {
            nsk_jvmti_setFailStatus();
        }
        if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(eventMon))) {
            nsk_jvmti_setFailStatus();
        }

        if (eventReceived1 == JNI_FALSE) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN0("Expected ClassUnload event was not received.\n");
        }

        if (eventReceived2 == JNI_TRUE) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN0("Received unexpected ClassUnload event.\n");
        }

        if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
            return;

    } while (0);

    NSK_TRACE(jvmti->DestroyRawMonitor(eventMon));

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ex03t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ex03t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ex03t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("eventMon", &eventMon))) {
        return JNI_ERR;
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
