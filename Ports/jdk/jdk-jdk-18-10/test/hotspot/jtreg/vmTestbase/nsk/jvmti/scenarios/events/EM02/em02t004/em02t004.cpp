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
#include "JVMTITools.h"

extern "C" {

/* ============================================================================= */

/* scaffold objects */
static jvmtiEnv *jvmti = NULL;
static jlong timeout = 0;
static jrawMonitorID syncLock = NULL;

/* constant names */
#define STEP_AMOUNT 3
#define JVMTI_EVENT_COUNT   (int)(JVMTI_MAX_EVENT_TYPE_VAL - JVMTI_MIN_EVENT_TYPE_VAL + 1)

static int eventCount[JVMTI_EVENT_COUNT];
static int newEventCount[JVMTI_EVENT_COUNT];

/* ============================================================================= */
JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_events_EM02_em02t004_nativeMethod1(JNIEnv *jni_env,
                        jobject o) {
    NSK_DISPLAY0("called nativeMethod1\n");
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_events_EM02_em02t004_nativeMethod2(JNIEnv *jni_env,
                        jobject o) {
    NSK_DISPLAY0("called nativeMethod2\n");
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_events_EM02_em02t004_nativeMethod3(JNIEnv *jni_env,
                        jobject o) {
    NSK_DISPLAY0("called nativeMethod3\n");
}

static void
showEventStatistics(int step) {
    int i;
    const char* str;
    int *currentCounts = (step == 1) ? &eventCount[0] : &newEventCount[0];

    NSK_DISPLAY0("\n");
    NSK_DISPLAY1("Event statistics for %d step:\n", step);
    NSK_DISPLAY0("-----------------------------\n");
    for (i = 0; i < JVMTI_EVENT_COUNT; i++) {
        if (currentCounts[i] > 0) {
            str = TranslateEvent((jvmtiEvent)(i+JVMTI_MIN_EVENT_TYPE_VAL));
            NSK_DISPLAY2("%-40s %7d\n", str, currentCounts[i]);
        }
    }
}

/* ========================================================================== */

bool checkEvents(int step) {
    int i;
    jvmtiEvent curr;
    bool result = true;
    int *currentCounts;
    int isExpected = 0;

    switch (step) {
        case 1:
            currentCounts = &eventCount[0];
            break;

        case 2:
        case 3:
            currentCounts = &newEventCount[0];
            break;

        default:
            NSK_COMPLAIN1("Unexpected step no: %d\n", step);
            return false;
    }

    for (i = 0; i < JVMTI_EVENT_COUNT; i++) {

        curr = (jvmtiEvent) (i + JVMTI_MIN_EVENT_TYPE_VAL);

        switch (step) {
            case 1:
                isExpected = ((curr == JVMTI_EVENT_NATIVE_METHOD_BIND)
                                || (curr == JVMTI_EVENT_VM_INIT));
                break;

            case 2:
                isExpected = ((curr == JVMTI_EVENT_NATIVE_METHOD_BIND));
                break;

            case 3:
                isExpected = (curr == JVMTI_EVENT_VM_DEATH);
                break;
        }

        if (isExpected) {
            if (currentCounts[i] < 1) {
                    NSK_COMPLAIN2("Unexpected events number %7d for %s\n\texpected value must be greater than 1\n",
                                        currentCounts[i],
                                        TranslateEvent(curr));
                result = false;
            }
        } else {
            if (currentCounts[i] > 0) {
                NSK_COMPLAIN2("Unexpected event %s was sent %d times\n",
                                    TranslateEvent(curr),
                                    currentCounts[i]);
                result = false;
            }
        }
    }

    return result;
}

static void
changeCount(jvmtiEvent event, int *currentCounts) {

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(syncLock)))
        nsk_jvmti_setFailStatus();

    currentCounts[event - JVMTI_MIN_EVENT_TYPE_VAL]++;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(syncLock)))
        nsk_jvmti_setFailStatus();

}

/* ============================================================================= */

/* callbacks */
JNIEXPORT void JNICALL
cbVMInit(jvmtiEnv* jvmti, JNIEnv* jni_env, jthread thread) {
    changeCount(JVMTI_EVENT_VM_INIT, &eventCount[0]);
}

JNIEXPORT void JNICALL
cbVMDeath(jvmtiEnv* jvmti, JNIEnv* jni_env) {
    changeCount(JVMTI_EVENT_VM_DEATH, &newEventCount[0]);
    showEventStatistics(STEP_AMOUNT);
    if (!checkEvents(STEP_AMOUNT))
        nsk_jvmti_setFailStatus();

    if (!NSK_JVMTI_VERIFY(jvmti->DestroyRawMonitor(syncLock)))
        nsk_jvmti_setFailStatus();

}

void JNICALL
cbException(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method, jlocation location, jobject exception,
                jmethodID catch_method, jlocation catch_location) {
    changeCount(JVMTI_EVENT_EXCEPTION, &eventCount[0]);
}

void JNICALL
cbExceptionCatch(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method, jlocation location, jobject exception) {
    changeCount(JVMTI_EVENT_EXCEPTION_CATCH, &eventCount[0]);
}

void JNICALL
cbSingleStep(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method, jlocation location) {
    changeCount(JVMTI_EVENT_SINGLE_STEP, &eventCount[0]);
}

void JNICALL
cbFramePop(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method, jboolean was_popped_by_exception) {
    changeCount(JVMTI_EVENT_FRAME_POP, &eventCount[0]);
}

void JNICALL
cbBreakpoint(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method, jlocation location) {
    changeCount(JVMTI_EVENT_BREAKPOINT, &eventCount[0]);
}

void JNICALL
cbFieldAccess(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method, jlocation location, jclass field_klass,
                jobject object, jfieldID field) {
    changeCount(JVMTI_EVENT_FIELD_ACCESS, &eventCount[0]);
}

void JNICALL
cbFieldModification(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method, jlocation location, jclass field_klass,
                jobject object, jfieldID field, char signature_type,
                jvalue new_value) {
    changeCount(JVMTI_EVENT_FIELD_MODIFICATION, &eventCount[0]);
}

void JNICALL
cbMethodEntry(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method) {
    changeCount(JVMTI_EVENT_METHOD_ENTRY, &eventCount[0]);
}

void JNICALL
cbMethodExit(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                jmethodID method, jboolean was_popped_by_exception,
                jvalue return_value) {
    changeCount(JVMTI_EVENT_METHOD_EXIT, &eventCount[0]);
}

void JNICALL
cbCompiledMethodLoad(jvmtiEnv *jvmti_env, jmethodID method, jint code_size,
                const void* code_addr, jint map_length,
                const jvmtiAddrLocationMap* map, const void* compile_info) {
    changeCount(JVMTI_EVENT_COMPILED_METHOD_LOAD, &eventCount[0]);
}

void JNICALL
cbCompiledMethodUnload(jvmtiEnv *jvmti_env, jmethodID method,
                const void* code_addr) {
    changeCount(JVMTI_EVENT_COMPILED_METHOD_UNLOAD, &eventCount[0]);
}

void JNICALL
cbMonitorWait(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                    jobject object, jlong tout) {

    changeCount(JVMTI_EVENT_MONITOR_WAIT, &eventCount[0]);
}

void JNICALL
cbMonitorWaited(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                    jobject object, jboolean timed_out) {

    changeCount(JVMTI_EVENT_MONITOR_WAITED, &eventCount[0]);
}

JNIEXPORT void JNICALL
cbMonitorContendedEnter(jvmtiEnv* jvmti, JNIEnv* jni_env, jthread thread,
                            jobject object) {

    changeCount(JVMTI_EVENT_MONITOR_CONTENDED_ENTER, &eventCount[0]);
}

void JNICALL
cbMonitorContendedEntered(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                            jobject object) {

    changeCount(JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, &eventCount[0]);
}

void JNICALL
cbGarbageCollectionStart(jvmtiEnv *jvmti_env) {

    changeCount(JVMTI_EVENT_GARBAGE_COLLECTION_START, &eventCount[0]);
}

void JNICALL
cbGarbageCollectionFinish(jvmtiEnv *jvmti_env) {

    changeCount(JVMTI_EVENT_GARBAGE_COLLECTION_FINISH, &eventCount[0]);
}

void JNICALL
cbObjectFree(jvmtiEnv *jvmti_env, jlong tag) {
    changeCount(JVMTI_EVENT_OBJECT_FREE, &eventCount[0]);
}

void JNICALL
cbVMObjectAlloc(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
                    jobject object, jclass object_klass, jlong size) {

    changeCount(JVMTI_EVENT_VM_OBJECT_ALLOC, &eventCount[0]);
}

void JNICALL
cbNativeMethodBind(jvmtiEnv *jvmti_env, JNIEnv* jni_env,jthread thread,
                jmethodID method, void* address, void** new_address_ptr) {

    char *name;
    char *sign;
    char *genc;
    jvmtiPhase phase;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetPhase(&phase))) {
        nsk_jvmti_setFailStatus();
    }

    if (phase != JVMTI_PHASE_START && phase != JVMTI_PHASE_LIVE) {
        return;
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &name, &sign, &genc))) {
        return;
    }

    if (!strcmp(name,"nativeMethod1")) {
        NSK_DISPLAY0("NATIVE_METHOD_BIND recieved for\n");
        NSK_DISPLAY4("\tmethod: %s, signature: %s address: %p new_address: %p\n",
                            name, sign, address, new_address_ptr);
        changeCount(JVMTI_EVENT_NATIVE_METHOD_BIND, &eventCount[0]);
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)name))) {
        nsk_jvmti_setFailStatus();
    }
    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)sign))) {
        nsk_jvmti_setFailStatus();
    }
    if (genc != NULL)
        if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)genc))) {
            nsk_jvmti_setFailStatus();
        }
}

void JNICALL
cbNewNativeMethodBind(jvmtiEnv *jvmti_env, JNIEnv* jni_env,jthread thread,
                jmethodID method, void* address, void** new_address_ptr) {
    char *name;
    char *sign;
    char *genc;

    jvmtiPhase phase;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetPhase(&phase))) {
        nsk_jvmti_setFailStatus();
    }

    if (phase != JVMTI_PHASE_START && phase != JVMTI_PHASE_LIVE) {
        return;
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &name, &sign, &genc))) {
        return;
    }

    if (!strcmp(name,"nativeMethod2")) {
        NSK_DISPLAY0("NATIVE_METHOD_BIND recieved for\n");
        NSK_DISPLAY4("\tmethod: %s, signature: %s address: %p new_address: %p\n",
                            name, sign, address, new_address_ptr);
        changeCount(JVMTI_EVENT_NATIVE_METHOD_BIND, &newEventCount[0]);
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)name))) {
        nsk_jvmti_setFailStatus();
    }
    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)sign))) {
        nsk_jvmti_setFailStatus();
    }
    if (genc != NULL)
        if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)genc))) {
            nsk_jvmti_setFailStatus();
        }
}

/* ============================================================================= */

static bool enableEvent(jvmtiEvent event) {

    if (nsk_jvmti_isOptionalEvent(event)
            && (event != JVMTI_EVENT_NATIVE_METHOD_BIND)) {
        if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
                jvmti->SetEventNotificationMode(JVMTI_ENABLE, event, NULL))) {
            NSK_COMPLAIN1("Unexpected error enabling %s\n",
                TranslateEvent(event));
            return false;
        }
    } else {
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, event, NULL))) {
            NSK_COMPLAIN1("Unexpected error enabling %s\n",
                TranslateEvent(event));
            return false;
        }
    }

    return true;
}

/**
 * Enable or disable tested events.
 */
static bool enableEventList() {
    int i;
    bool result = true;

    NSK_DISPLAY0("Enable events\n");

    result = enableEvent(JVMTI_EVENT_VM_INIT);

    result = result && enableEvent(JVMTI_EVENT_VM_DEATH);

    /* enabling optional events */
    for (i = 0; i < JVMTI_EVENT_COUNT; i++) {
        jvmtiEvent event = (jvmtiEvent)(i+JVMTI_MIN_EVENT_TYPE_VAL);

        if (nsk_jvmti_isOptionalEvent(event))
            result = result && enableEvent(event);
    }

    if (!result) {
        nsk_jvmti_setFailStatus();
        return false;
    }

    return true;
}

/* ============================================================================= */

static bool setCallBacks(int step) {

    int i;

    jvmtiEventCallbacks eventCallbacks;
    memset(&eventCallbacks, 0, sizeof(eventCallbacks));

    NSK_DISPLAY0("\n");
    NSK_DISPLAY1("===============step %d===============\n", step);
    NSK_DISPLAY0("\n");
    switch (step) {
        case 1:
            for (i = 0; i < JVMTI_EVENT_COUNT; i++) {
                eventCount[i] = 0;
            }

            eventCallbacks.VMInit                    = cbVMInit;
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
            eventCallbacks.CompiledMethodUnload      = cbCompiledMethodUnload;
            eventCallbacks.MonitorWait               = cbMonitorWait;
            eventCallbacks.MonitorWaited             = cbMonitorWaited;
            eventCallbacks.MonitorContendedEnter     = cbMonitorContendedEnter;
            eventCallbacks.MonitorContendedEntered   = cbMonitorContendedEntered;
            eventCallbacks.GarbageCollectionStart    = cbGarbageCollectionStart;
            eventCallbacks.GarbageCollectionFinish   = cbGarbageCollectionFinish;
            eventCallbacks.ObjectFree                = cbObjectFree;
            eventCallbacks.VMObjectAlloc             = cbVMObjectAlloc;
            break;

        case 2:
            for (i = 0; i < JVMTI_EVENT_COUNT; i++) {
                newEventCount[i] = 0;
            }

            eventCallbacks.NativeMethodBind = cbNewNativeMethodBind;
            break;

        case 3:
            for (i = 0; i < JVMTI_EVENT_COUNT; i++) {
                newEventCount[i] = 0;
            }

            eventCallbacks.VMDeath                   = cbVMDeath;
            break;

    }
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks))))
        return false;

    return true;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* agentJNI, void* arg) {

    int i;

    for (i = 1; i <= STEP_AMOUNT; i++) {
        if (i > 1) {
            NSK_DISPLAY0("Check received events\n");

            showEventStatistics(i-1);
            if (!checkEvents(i-1))
                nsk_jvmti_setFailStatus();

            if (!setCallBacks(i)) {
                return;
            }

            if (!nsk_jvmti_resumeSync())
                return;
        }

        NSK_DISPLAY0("Wait for debuggee to become ready\n");
        if (!nsk_jvmti_waitForSync(timeout))
            return;

    }

    NSK_DISPLAY0("Let debuggee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;

}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_em02t004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_em02t004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_em02t004(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    jvmti = nsk_jvmti_createJVMTIEnv(jvm, reserved);
    if (!NSK_VERIFY(jvmti != NULL))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("_syncLock", &syncLock))) {
        nsk_jvmti_setFailStatus();
        return JNI_ERR;
    }

    {
        jvmtiCapabilities caps;
        memset(&caps, 0, sizeof(caps));

        caps.can_generate_native_method_bind_events = 1;
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
            return JNI_ERR;
    }

    if (!setCallBacks(1)) {
        return JNI_ERR;
    }

    nsk_jvmti_showPossessedCapabilities(jvmti);

    if (!enableEventList()) {
        return JNI_ERR;
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */


}
