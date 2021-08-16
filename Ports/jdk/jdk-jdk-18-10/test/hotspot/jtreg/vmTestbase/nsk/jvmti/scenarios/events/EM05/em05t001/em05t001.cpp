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
static JNIEnv* jni = NULL;
static jvmtiEnv *jvmti = NULL;
static jlong timeout = 0;

/* constant names */
#define DEBUGEE_CLASS_NAME      "nsk/jvmti/scenarios/events/EM05/em05t001"
#define THREAD_CLASS_NAME       "nsk/jvmti/scenarios/events/EM05/em05t001Thread"
#define THREAD_FIELD_NAME       "thread"
#define THREAD_FIELD_SIG        "L" THREAD_CLASS_NAME ";"

/* constants */
#define MAX_NAME_LENGTH         64
#define EVENTS_COUNT            2
#define METHODS_COUNT           2

/* tested events */
static jvmtiEvent eventsList[EVENTS_COUNT] = {
    JVMTI_EVENT_COMPILED_METHOD_LOAD,
    JVMTI_EVENT_COMPILED_METHOD_UNLOAD
};

/* method description structure */
typedef struct {
    char methodName[MAX_NAME_LENGTH];
    char methodSig[MAX_NAME_LENGTH];
    jmethodID method;
    int compiled;
    int loadEvents;
    int unloadEvents;
} MethodDesc;

/* descriptions of tested methods */
static MethodDesc methodsDesc[METHODS_COUNT] = {
    { "javaMethod", "(I)I", NULL, 0, 0, 0 },
    { "nativeMethod", "(I)I", NULL, 0, 0, 0 }
};

/* ============================================================================= */

/* testcase(s) */
static int prepare();
static int checkEvents();
static int clean();

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* agentJNI, void* arg) {
    jni = agentJNI;

    NSK_DISPLAY0("Wait for debuggee to become ready\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    {
        NSK_DISPLAY0("Prepare data\n");
        if (!prepare()) {
            nsk_jvmti_setFailStatus();
            return;
        }

        NSK_DISPLAY0("Provoke methods compilation\n");
        if (!nsk_jvmti_resumeSync())
            return;

        NSK_DISPLAY0("Wait for threads to complete\n");
        if (!nsk_jvmti_waitForSync(timeout))
            return;

        NSK_DISPLAY0("Check if events received\n");
        if (!checkEvents())
            return;

        NSK_DISPLAY0("Clean data\n");
        if (!clean()) {
            nsk_jvmti_setFailStatus();
            return;
        }
    }

    NSK_DISPLAY0("Let debuggee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ============================================================================= */

/**
 * Enable or disable tested events.
 */
static int enableEvents(jvmtiEventMode enable) {
    int i;

    for (i = 0; i < EVENTS_COUNT; i++) {
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(enable, eventsList[i], NULL))) {
            nsk_jvmti_setFailStatus();
            return NSK_FALSE;
        }
    }
    return NSK_TRUE;
}

/**
 * Prepare data.
 *    - find tested thread
 *    - get tested methodIDs
 *    - enable events
 */
static int prepare() {
    jclass debugeeClass = NULL;
    jclass threadClass = NULL;
    jfieldID threadFieldID = NULL;
    jthread thread = NULL;
    int i;

    for (i = 0; i < METHODS_COUNT; i++) {
        methodsDesc[i].method = (jmethodID)NULL;
        methodsDesc[i].loadEvents = 0;
        methodsDesc[i].unloadEvents = 0;
    }

    if (!NSK_JNI_VERIFY(jni, (debugeeClass = jni->FindClass(DEBUGEE_CLASS_NAME)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (threadFieldID =
            jni->GetStaticFieldID(debugeeClass, THREAD_FIELD_NAME, THREAD_FIELD_SIG)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (thread = (jthread)
            jni->GetStaticObjectField(debugeeClass, threadFieldID)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (threadClass = jni->GetObjectClass(thread)) != NULL))
        return NSK_FALSE;

    NSK_DISPLAY0("Find tested methods:\n");
    for (i = 0; i < METHODS_COUNT; i++) {
        if (!NSK_JNI_VERIFY(jni, (methodsDesc[i].method =
                jni->GetMethodID(threadClass, methodsDesc[i].methodName, methodsDesc[i].methodSig)) != NULL))
            return NSK_FALSE;
        NSK_DISPLAY3("    method #%d (%s): 0x%p\n",
                                i, methodsDesc[i].methodName, (void*)methodsDesc[i].method);
    }

    NSK_DISPLAY0("Enable events\n");
    if (!nsk_jvmti_enableEvents(JVMTI_ENABLE, EVENTS_COUNT, eventsList, NULL))
        return NSK_FALSE;

    return NSK_TRUE;
}

/**
 * Testcase: check tested events.
 *   - check if expected events received for each method
 *
 * Returns NSK_TRUE if test may continue; or NSK_FALSE for test break.
 */
static int checkEvents() {
    int i;

    for (i = 0; i < METHODS_COUNT; i++) {
        NSK_DISPLAY2("  method #%d (%s):\n",
                                i, methodsDesc[i].methodName);
        NSK_DISPLAY2("    COMPILED_METHOD_LOAD: %d, COMPILED_METHOD_UNLOAD: %d\n",
                                methodsDesc[i].loadEvents, methodsDesc[i].unloadEvents);

        if (methodsDesc[i].loadEvents <= 0) {
            NSK_DISPLAY1("# WARNING: No COMPILED_METHOD_LOAD events for method: %s\n",
                                methodsDesc[i].methodName);
        }

        if (methodsDesc[i].unloadEvents > methodsDesc[i].loadEvents) {
            NSK_DISPLAY1("# WARNING: Too many COMPILED_METHOD_UNLOAD events for method: %s\n",
                                methodsDesc[i].methodName);
            NSK_DISPLAY2("#   COMPILED_METHOD_LOAD: %d, COMPILED_METHOD_UNLOAD: %d\n",
                                methodsDesc[i].loadEvents, methodsDesc[i].unloadEvents);
        }
    }
    return NSK_TRUE;
}

/**
 * Clean data:
 *   - disable events
 */
static int clean() {
    NSK_DISPLAY0("Disable events\n");
    if (!nsk_jvmti_enableEvents(JVMTI_DISABLE, EVENTS_COUNT, eventsList, NULL))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ============================================================================= */

/**
 * COMPILED_METHOD_LOAD callback.
 *   - turn on flag if method is compiled
 */
JNIEXPORT void JNICALL
callbackCompiledMethodLoad(jvmtiEnv* jvmti, jmethodID method,
                            jint code_size, const void* code_addr,
                            jint map_length, const jvmtiAddrLocationMap* map,
                            const void* compile_info) {
    int i;

    for (i = 0; i < METHODS_COUNT; i++) {
        if (methodsDesc[i].method == method) {
            methodsDesc[i].loadEvents++;
            NSK_DISPLAY3("  COMPILED_METHOD_LOAD for method #%d (%s): %d times\n",
                                i, methodsDesc[i].methodName, methodsDesc[i].loadEvents);
            NSK_DISPLAY1("    methodID:   0x%p\n",
                                (void*)methodsDesc[i].method);
            NSK_DISPLAY1("    code_size:  %d\n",
                                (int)code_size);
            NSK_DISPLAY1("    map_length: %d\n",
                                (int)map_length);
            break;
        }
    }
}

/**
 * COMPILED_METHOD_UNLOAD callback.
 *   - turn off flag that method is compiled
 */
JNIEXPORT void JNICALL
callbackCompiledMethodUnload(jvmtiEnv* jvmti, jmethodID method,
                             const void* code_addr) {
    int i;

    for (i = 0; i < METHODS_COUNT; i++) {
        if (methodsDesc[i].method == method) {
            methodsDesc[i].unloadEvents++;
            NSK_DISPLAY3("  COMPILED_METHOD_UNLOAD for method #%d (%s): %d times\n",
                                i, methodsDesc[i].methodName, methodsDesc[i].loadEvents);
            NSK_DISPLAY1("    methodID:   0x%p\n",
                                (void*)methodsDesc[i].method);
            break;
        }
    }
}

/* ============================================================================= */

/** Native running method in tested thread. */
JNIEXPORT jint JNICALL
Java_nsk_jvmti_scenarios_events_EM05_em05t001Thread_nativeMethod(JNIEnv* jni,
                                                                    jobject obj,
                                                                    jint i) {
    jint k = 0;
    jint j;

    for (j = 0; j < i; j++) {
        k += (i - j);
    }
    return k;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_em05t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_em05t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_em05t001(JavaVM *jvm, char *options, void *reserved) {
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

    {
        jvmtiCapabilities caps;
        memset(&caps, 0, sizeof(caps));
        caps.can_generate_compiled_method_load_events = 1;
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
            return JNI_ERR;
    }

    {
        jvmtiEventCallbacks eventCallbacks;
        memset(&eventCallbacks, 0, sizeof(eventCallbacks));
        eventCallbacks.CompiledMethodLoad = callbackCompiledMethodLoad;
        eventCallbacks.CompiledMethodUnload = callbackCompiledMethodUnload;
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks))))
            return JNI_ERR;
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
