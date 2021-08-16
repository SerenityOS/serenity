/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

#define PASSED  0
#define STATUS_FAILED  2

/* ============================================================================= */

static jlong timeout = 0;
static const char* segment = NULL;
static const char* EXP_CLASS_SIGNATURE = "Lnsk/jvmti/scenarios/general_functions/GF04/gf04t001;";
static jrawMonitorID countLock;
static jboolean classLoadReceived = JNI_FALSE, classPrepareReceived = JNI_FALSE;
static jint result = PASSED;

/* ============================================================================= */

/**
 * Add segment to bootstrap classloader path.
 * @returns NSK_FALSE if any error occured.
 */
static int addSegment(jvmtiEnv* jvmti, const char segment[], const char where[]) {
    void* storage = NULL;

    NSK_DISPLAY1("Add segment: %s\n", segment);
    if (!NSK_JVMTI_VERIFY(jvmti->AddToBootstrapClassLoaderSearch(segment))) {
        return NSK_FALSE;
    }
    NSK_DISPLAY0("  ... added\n");

    return NSK_TRUE;
}

static void setupLock(jvmtiEnv *jvmti_env, JNIEnv *jni_env) {
    if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorEnter(countLock)))
        jni_env->FatalError("failed to enter a raw monitor\n");
}

static void setoffLock(jvmtiEnv *jvmti_env, JNIEnv *jni_env) {
    if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorExit(countLock)))
        jni_env->FatalError("failed to exit a raw monitor\n");
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_scenarios_general_1functions_GF04_gf04t001_check(
        JNIEnv *env, jobject obj) {
    if (result == PASSED && classLoadReceived == JNI_TRUE && classPrepareReceived == JNI_TRUE) {
        return PASSED;
    }
    return STATUS_FAILED;
}

/* ============================================================================= */

/** callback for ClassLoad event **/
void JNICALL
ClassLoad(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread, jclass klass) {
    char *sig, *generic;

    setupLock(jvmti_env, env);

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(klass, &sig, &generic))) {
        result = STATUS_FAILED;
    }

    if (strcmp(sig, EXP_CLASS_SIGNATURE) == 0) {
        NSK_DISPLAY1("CHECK PASSED: ClassLoad event received for the class \"%s\" as expected\n",
            sig);
        classLoadReceived = JNI_TRUE;

        if (!NSK_JVMTI_VERIFY(jvmti_env->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_CLASS_LOAD, NULL))) {
            result = STATUS_FAILED;
        } else {
            NSK_DISPLAY0("ClassLoad event disabled\n");
        }
    }

    setoffLock(jvmti_env, env);
}

/** callback for ClassPrepare event **/
void JNICALL
ClassPrepare(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread, jclass klass) {
    char *sig, *generic;

    setupLock(jvmti_env, env);

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(klass, &sig, &generic))) {
        result = STATUS_FAILED;
    }

    if (strcmp(sig, EXP_CLASS_SIGNATURE) == 0) {
        NSK_DISPLAY1("CHECK PASSED: ClassPrepare event received for the class \"%s\" as expected\n",
            sig);
        classPrepareReceived = JNI_TRUE;

        if (!NSK_JVMTI_VERIFY(jvmti_env->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_CLASS_PREPARE, NULL))) {
            result = STATUS_FAILED;
        } else {
            NSK_DISPLAY0("ClassPrepare event disabled\n");
        }
    }

    setoffLock(jvmti_env, env);
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_gf04t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_gf04t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_gf04t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    segment = nsk_jvmti_findOptionStringValue("segment", NULL);
    if (!NSK_VERIFY(segment != NULL))
        return JNI_ERR;

    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("eventLock", &countLock)))
        return JNI_ERR;

    NSK_DISPLAY0("Add bootstrap class load segment in Agent_OnLoad()\n");
    if (!addSegment(jvmti, segment, "Agent_OnLoad()")) {
        return JNI_ERR;
    }

    NSK_DISPLAY0("Setting callbacks for events:\n");
    {
        jvmtiEventCallbacks callbacks;
        jint size = (jint)sizeof(callbacks);

        memset(&callbacks, 0, sizeof(callbacks));
        callbacks.ClassLoad = &ClassLoad;
        callbacks.ClassPrepare = &ClassPrepare;
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, size))) {
            return JNI_ERR;
        }
    }
    NSK_DISPLAY0("  ... set\n");

    NSK_DISPLAY0("Enabling events: \n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_LOAD, NULL))) {
        return JNI_ERR;
    } else {
        NSK_DISPLAY0("  ... ClassLoad enabled\n");
    }
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE, NULL))) {
        return JNI_ERR;
    } else {
        NSK_DISPLAY0("  ... ClassPrepare enabled\n");
    }

    return JNI_OK;
}

/* ============================================================================= */

}
