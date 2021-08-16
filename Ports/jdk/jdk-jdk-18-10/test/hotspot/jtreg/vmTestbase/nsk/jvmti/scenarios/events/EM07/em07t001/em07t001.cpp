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

#include <string.h>
#include "jvmti.h"
#include "agent_common.h"
#include "jni_tools.h"
#include "jvmti_tools.h"
#include "JVMTITools.h"

extern "C" {

/* ============================================================================= */

/* scaffold objects */
static jlong timeout = 0;
static jvmtiEnv *jvmti = NULL;
static jrawMonitorID syncLock = NULL;

/* constant names */
#define JVMTI_EVENT_COUNT   (int)(JVMTI_MAX_EVENT_TYPE_VAL - JVMTI_MIN_EVENT_TYPE_VAL + 1)
#define EXPECTED_COUNT 0

static int eventCount[JVMTI_EVENT_COUNT];

/* ============================================================================= */

void showEventStatistics() {
    int i;
    const char* str;

    NSK_DISPLAY0("\n");
    NSK_DISPLAY0("Event statistics\n");
    NSK_DISPLAY0("----------------\n");
    for (i = 0; i < JVMTI_EVENT_COUNT; i++) {
        if (eventCount[i] > 0) {
            str = TranslateEvent((jvmtiEvent)(i+JVMTI_MIN_EVENT_TYPE_VAL));
            NSK_DISPLAY2("%-40s %7d\n", str, eventCount[i]);
        }
    }
}

/* ========================================================================== */

void changeCount(jvmtiEvent event) {

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(syncLock)))
        nsk_jvmti_setFailStatus();

    eventCount[event - JVMTI_MIN_EVENT_TYPE_VAL]++;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(syncLock)))
        nsk_jvmti_setFailStatus();

}

/* ============================================================================= */

/* callbacks */
JNIEXPORT void JNICALL
cbVMInit(jvmtiEnv* jvmti, JNIEnv* jni_env, jthread thread) {
    changeCount(JVMTI_EVENT_VM_INIT);
    NSK_DISPLAY0("--->VMINit is received\n");
}

JNIEXPORT void JNICALL
cbVMDeath(jvmtiEnv* jvmti, JNIEnv* jni_env) {
    changeCount(JVMTI_EVENT_VM_DEATH);

    if (!NSK_JVMTI_VERIFY(jvmti->DestroyRawMonitor(syncLock)))
        nsk_jvmti_setFailStatus();

}

void JNICALL
cbException(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method, jlocation location, jobject exception,
                jmethodID catch_method, jlocation catch_location) {
    changeCount(JVMTI_EVENT_EXCEPTION);
}

void JNICALL
cbExceptionCatch(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method, jlocation location, jobject exception) {
    changeCount(JVMTI_EVENT_EXCEPTION_CATCH);
}

void JNICALL
cbSingleStep(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method, jlocation location) {
    changeCount(JVMTI_EVENT_SINGLE_STEP);
}

void JNICALL
cbFramePop(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method, jboolean was_popped_by_exception) {
    changeCount(JVMTI_EVENT_FRAME_POP);
}

void JNICALL
cbBreakpoint(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method, jlocation location) {
    changeCount(JVMTI_EVENT_BREAKPOINT);
}

void JNICALL
cbFieldAccess(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method, jlocation location, jclass field_klass,
                jobject object, jfieldID field) {
    changeCount(JVMTI_EVENT_FIELD_ACCESS);
}

void JNICALL
cbFieldModification(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method, jlocation location, jclass field_klass,
                jobject object, jfieldID field, char signature_type,
                jvalue new_value) {
    changeCount(JVMTI_EVENT_FIELD_MODIFICATION);
}

void JNICALL
cbMethodEntry(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method) {
    changeCount(JVMTI_EVENT_METHOD_ENTRY);
}

void JNICALL
cbMethodExit(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method, jboolean was_popped_by_exception,
                jvalue return_value) {
    changeCount(JVMTI_EVENT_METHOD_EXIT);
}

void JNICALL
cbNativeMethodBind(jvmtiEnv *jvmti_env, JNIEnv* jni_env,jthread thread,
                jmethodID method, void* address, void** new_address_ptr) {
    changeCount(JVMTI_EVENT_NATIVE_METHOD_BIND);
}

void JNICALL
cbCompiledMethodLoad(jvmtiEnv *jvmti_env, jmethodID method, jint code_size,
                const void* code_addr, jint map_length,
                const jvmtiAddrLocationMap* map, const void* compile_info) {
    changeCount(JVMTI_EVENT_COMPILED_METHOD_LOAD);
}

void JNICALL
cbCompiledMethodUnload(jvmtiEnv *jvmti_env, jmethodID method,
                const void* code_addr) {
    changeCount(JVMTI_EVENT_COMPILED_METHOD_UNLOAD);
}

void JNICALL
cbMonitorWait(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                    jobject object, jlong tout) {

    changeCount(JVMTI_EVENT_MONITOR_WAIT);
}

void JNICALL
cbMonitorWaited(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                    jobject object, jboolean timed_out) {

    changeCount(JVMTI_EVENT_MONITOR_WAITED);
}

JNIEXPORT void JNICALL
cbMonitorContendedEnter(jvmtiEnv* jvmti, JNIEnv* jni_env, jthread thread,
                            jobject object) {

    changeCount(JVMTI_EVENT_MONITOR_CONTENDED_ENTER);
}

void JNICALL
cbMonitorContendedEntered(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                            jobject object) {

    changeCount(JVMTI_EVENT_MONITOR_CONTENDED_ENTERED);
}

void JNICALL
cbGarbageCollectionStart(jvmtiEnv *jvmti_env) {
    changeCount(JVMTI_EVENT_GARBAGE_COLLECTION_START);
}

void JNICALL
cbGarbageCollectionFinish(jvmtiEnv *jvmti_env) {
    changeCount(JVMTI_EVENT_GARBAGE_COLLECTION_FINISH);
}

void JNICALL
cbObjectFree(jvmtiEnv *jvmti_env, jlong tag) {
    changeCount(JVMTI_EVENT_OBJECT_FREE);
}

void JNICALL
cbVMObjectAlloc(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                    jobject object, jclass object_klass, jlong size) {

    changeCount(JVMTI_EVENT_VM_OBJECT_ALLOC);
}

/* ============================================================================= */

int enableOptionalEvents(jvmtiEnv *jvmti) {
    int i;
    int result = NSK_TRUE;

    NSK_DISPLAY0("Enable events\n");

    /* enabling optional events */
    for (i = 0; i < JVMTI_EVENT_COUNT; i++) {
        jvmtiEvent event = (jvmtiEvent)(i + JVMTI_MIN_EVENT_TYPE_VAL);
        if (nsk_jvmti_isOptionalEvent(event))
            if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
                    jvmti->SetEventNotificationMode(JVMTI_ENABLE, event, NULL))) {
                NSK_COMPLAIN1("Unexpected error enabling %s\n",
                    TranslateEvent(event));
                result = NSK_FALSE;
            }
    }

    return result;
}

/* ============================================================================= */

/**
 * Testcase: check tested events.
 *   - check if expected events received for each method
 *
 * Returns NSK_TRUE if test may continue; or NSK_FALSE for test break.
 */
int checkEvents() {
    int i;
    jvmtiEvent event;
    int result = NSK_TRUE;

    for (i = 0; i < JVMTI_EVENT_COUNT; i++) {

        event = (jvmtiEvent) (i + JVMTI_MIN_EVENT_TYPE_VAL);

        if (nsk_jvmti_isOptionalEvent(event) && eventCount[i] > EXPECTED_COUNT) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN3("Unexpected number of %s events:\n\treceived: %7d\n\texpected: %7d\n",
                                TranslateEvent(event),
                                eventCount[i],
                                EXPECTED_COUNT);
            result = NSK_FALSE;
        }
    }

    return result;
}

/* ============================================================================= */

static int setCallBacks(jvmtiEnv *jvmti) {
    int i;

    jvmtiEventCallbacks eventCallbacks;
    memset(&eventCallbacks, 0, sizeof(eventCallbacks));

    for (i = 0; i < JVMTI_EVENT_COUNT; i++) {
        eventCount[i] = 0;
    }

    eventCallbacks.VMInit                    = cbVMInit;
    eventCallbacks.VMDeath                   = cbVMDeath;
    eventCallbacks.Exception                 = cbException;
    eventCallbacks.ExceptionCatch            = cbExceptionCatch;
    eventCallbacks.SingleStep                = cbSingleStep;
    eventCallbacks.FramePop                  = cbFramePop;
    eventCallbacks.Breakpoint                = cbBreakpoint;
    eventCallbacks.FieldAccess               = cbFieldAccess;
    eventCallbacks.FieldModification         = cbFieldModification;
    eventCallbacks.MethodEntry               = cbMethodEntry;
    eventCallbacks.MethodExit                = cbMethodExit;
    eventCallbacks.NativeMethodBind          = cbNativeMethodBind;
    eventCallbacks.CompiledMethodLoad        = cbCompiledMethodLoad;
    eventCallbacks.MonitorWait               = cbMonitorWait;
    eventCallbacks.MonitorWaited             = cbMonitorWaited;
    eventCallbacks.MonitorContendedEnter     = cbMonitorContendedEnter;
    eventCallbacks.MonitorContendedEntered   = cbMonitorContendedEntered;
    eventCallbacks.GarbageCollectionStart    = cbGarbageCollectionStart;
    eventCallbacks.GarbageCollectionFinish   = cbGarbageCollectionFinish;
    eventCallbacks.ObjectFree                = cbObjectFree;
    eventCallbacks.VMObjectAlloc             = cbVMObjectAlloc;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks))))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* agentJNI, void* arg) {

    NSK_DISPLAY0("Wait for debuggee to become ready\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!checkEvents()) {
        nsk_jvmti_setFailStatus();
    }
    showEventStatistics();

    NSK_DISPLAY0("Let debuggee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;

}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_em07t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_em07t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_em07t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    if (!NSK_VERIFY((jvmti = nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("_syncLock", &syncLock))) {
        nsk_jvmti_setFailStatus();
        return JNI_ERR;
    }

    if (!setCallBacks(jvmti)) {
        return JNI_ERR;
    }

    nsk_jvmti_showPossessedCapabilities(jvmti);

    if (!enableOptionalEvents(jvmti)) {
        return JNI_ERR;
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */


}
