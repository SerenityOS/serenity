/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "jni_tools.h"
#include "jvmti_tools.h"
#include "agent_common.h"

extern "C" {

/* ============================================================================= */

static jlong timeout = 0;

static char segment1[3000] = "";
static char segment2[3000] = "";

static const char* const illegal_segments[] = { "", "tmp/" };

jboolean use_segment2 = JNI_FALSE;

jvmtiPhase jvmti_phase_to_check = JVMTI_PHASE_ONLOAD;

/* ============================================================================= */

/**
 * Add segment to bootstrap classloader path.
 * @returns NSK_FALSE if any error occured.
 */
static int addSegment(jvmtiEnv* jvmti, const char segment[], const char where[]) {
    NSK_DISPLAY1("Add segment: \"%s\"\n", segment);
    if (!NSK_JVMTI_VERIFY(jvmti->AddToSystemClassLoaderSearch(segment))) {
        NSK_COMPLAIN1("TEST FAILURE: failed to add segment %s\n", segment);
        return NSK_FALSE;
    }
    NSK_DISPLAY0("  ... added\n");

    return NSK_TRUE;
}

/**
 * Try to add illegal segment to bootstrap classloader path and check that expected error
 *  (expectedError) is returned.
 * @returns NSK_FALSE if no error or wronf error is occured.
 */
static int addIllegalSegment(jvmtiEnv* jvmti, const char segment[], const char where[], jvmtiError expectedError) {
    NSK_DISPLAY1("Add illegal segment: \"%s\"\n", segment);
    if (!NSK_JVMTI_VERIFY_CODE(expectedError, jvmti->AddToSystemClassLoaderSearch(segment))) {

        NSK_COMPLAIN2("TEST FAILURE: got wrong error when tried to add segment %s (expected error=%s)\n",
                      segment, TranslateError(expectedError));
        return NSK_FALSE;
    }
    NSK_DISPLAY0("  ... not added\n");

    return NSK_TRUE;
}

/*
 * Check that attempt to add illegal segment causes the error.
 */
static void checkLivePhaseForIllegalArgs(jvmtiEnv* jvmti, const char where[]) {
    size_t i;

    for (i = 0; i < sizeof(illegal_segments)/sizeof(char*); i++) {
        if (!addIllegalSegment(jvmti, illegal_segments[i], where, JVMTI_ERROR_ILLEGAL_ARGUMENT)) {
            nsk_jvmti_setFailStatus();
            NSK_BEFORE_TRACE(exit(nsk_jvmti_getStatus()));
        }
    }
}

/* ============================================================================= */

void JNICALL
callbackVMInit(jvmtiEnv *jvmti, JNIEnv *env, jthread thread) {
    NSK_DISPLAY0(">>> Testcase #1: Add bootstrap class load segment(s) in VMInit (live phase)\n");

    // At first check that it is not possible to add anything other than an existing JAR file
    checkLivePhaseForIllegalArgs(jvmti, "VMInit()");

    if (!addSegment(jvmti, segment1, "VMInit()")) {
        nsk_jvmti_setFailStatus();
        NSK_BEFORE_TRACE(exit(nsk_jvmti_getStatus()));
    }

    if (use_segment2 == JNI_FALSE) return;

    if (!addSegment(jvmti, segment2, "VMInit()")) {
        nsk_jvmti_setFailStatus();
        NSK_BEFORE_TRACE(exit(nsk_jvmti_getStatus()));
    }
}

/*
 * Check that it is possible to add to the boot class path before VMDeath event return.
 */
void JNICALL
callbackVMDeath(jvmtiEnv *jvmti, JNIEnv* jni) {
    jvmtiPhase phase;

    if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase))) {
        NSK_COMPLAIN0("TEST FAILURE: unable to get phase\n");
        nsk_jvmti_setFailStatus();
        NSK_BEFORE_TRACE(exit(nsk_jvmti_getStatus()));
    }

    if (!NSK_VERIFY(phase == JVMTI_PHASE_LIVE)) {
        NSK_DISPLAY0(">>> Testcase #1: Add bootstrap class load segment(s) in VMDeath (live phase)\n");

        // At first check that it is not possible to add anything other than an existing JAR file
        checkLivePhaseForIllegalArgs(jvmti, "VMDeath()");

        /* Check, that it is possible to add a JAR file containing a class that is already
        *  loaded (or is in the process of being loaded) by a _bootstrap_ class loader.
        */

        if (!addSegment(jvmti, segment1, "VMDeath()")) {
            nsk_jvmti_setFailStatus();
            NSK_BEFORE_TRACE(exit(nsk_jvmti_getStatus()));
        }

        if (use_segment2 == JNI_FALSE) return;
        // otherwise add to classpath
        if (!addSegment(jvmti, segment2, "VMDeath()")) {
            nsk_jvmti_setFailStatus();
            NSK_BEFORE_TRACE(exit(nsk_jvmti_getStatus()));
        }
    }
}

/** Agent library initialization. */
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti;
    const char *p_segment1, *p_segment2, *phase_to_check;

    jvmti = NULL;
    p_segment1 = p_segment2 = phase_to_check = NULL;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    p_segment1 = nsk_jvmti_findOptionStringValue("segment1", NULL);
    if (!NSK_VERIFY(p_segment1 != NULL)) {
        return JNI_ERR;
    } else {
        strncpy(segment1, p_segment1, (size_t) sizeof(segment1)/sizeof(char));
        segment1[(size_t) sizeof(segment1)/sizeof(char) - 1] = 0;
    }

    // 'segment2' parameter is not mandatory
    p_segment2 = nsk_jvmti_findOptionStringValue("segment2", NULL);
    if (p_segment2 != NULL) {
        strncpy(segment2, p_segment2, (size_t) sizeof(segment2)/sizeof(char));
        segment2[(size_t) sizeof(segment2)/sizeof(char) - 1] = 0;
        use_segment2 = JNI_TRUE;
    }

    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    // Check what phase(s) we are going to test
    phase_to_check = nsk_jvmti_findOptionStringValue("phasetocheck", NULL);
    if (!NSK_VERIFY(phase_to_check != NULL)) {
        return JNI_ERR;
    } else if (strcmp(phase_to_check, "onload") == 0) {
        jvmti_phase_to_check = JVMTI_PHASE_ONLOAD;
    } else if (strcmp(phase_to_check, "live") == 0) {
        jvmti_phase_to_check = JVMTI_PHASE_LIVE;
    }

    if (jvmti_phase_to_check == JVMTI_PHASE_ONLOAD) {
        NSK_DISPLAY0(">>> Testcase #1: Add bootstrap class load segment in Agent_OnLoad()\n");
        if (!addSegment(jvmti, segment1, "Agent_OnLoad()")) {
            return JNI_ERR;
        }

        if (!addSegment(jvmti, segment2, "Agent_OnLoad()")) {
            return JNI_ERR;
        }

        return JNI_OK;
    }

    /* For Live phase enable events and set callbacks for them */
    NSK_DISPLAY1("Set callback for events: %s\n", "VM_INIT, VM_DEATH");
    {
        jvmtiEventCallbacks eventCallbacks;
        memset(&eventCallbacks, 0, sizeof(eventCallbacks));

        eventCallbacks.VMInit = callbackVMInit;
        eventCallbacks.VMDeath = callbackVMDeath;

        if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks)))) {
            return JNI_ERR;
        }
    }
    NSK_DISPLAY0("  ... set\n");

    NSK_DISPLAY1("Enable events: %s\n", "VM_INIT, VM_DEATH");
    {

        jvmtiEvent eventsList[] = { JVMTI_EVENT_VM_INIT, JVMTI_EVENT_VM_DEATH };
        if (!NSK_VERIFY(nsk_jvmti_enableEvents(
                     JVMTI_ENABLE, sizeof(eventsList)/sizeof(jvmtiEvent), eventsList, NULL))) {
            return JNI_ERR;
        }
    }
    NSK_DISPLAY0("  ... enabled\n");

    return JNI_OK;
}

/* ============================================================================= */

}
