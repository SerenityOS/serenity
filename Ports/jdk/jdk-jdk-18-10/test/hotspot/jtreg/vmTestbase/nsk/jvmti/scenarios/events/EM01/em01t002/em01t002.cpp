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
#include "ExceptionCheckingJniEnv.hpp"
#include "jni_tools.h"
#include "jvmti_tools.h"
#include "JVMTITools.h"

extern "C" {

/* ============================================================================= */

/* scaffold objects */
static JNIEnv* jni = NULL;
static jvmtiEnv *jvmti = NULL;
static jlong timeout = 0;
static jrawMonitorID syncLock = NULL;

/* constant names */
#define JVMTI_EVENT_COUNT   (int)(JVMTI_MAX_EVENT_TYPE_VAL - JVMTI_MIN_EVENT_TYPE_VAL + 1)
#define EXPECTED_CLASS_NAME "Lnsk/jvmti/scenarios/events/EM01/em01t002a;"
#define CLASS_LOADER_COUNT_PARAM "classLoaderCount"

static int eventCount[JVMTI_EVENT_COUNT];

static int classLoaderCount = 0;

static jvmtiPhase currentPhase;

/* ============================================================================= */
/* ============================================================================= */


/*
 * Class:     nsk_jvmti_scenarios_events_EM01_em01t002
 * Method:    loadClass
 * Signature: (Lnsk/share/ClassLoader;Ljava/lang/String;)Ljava/lang/Class;
 */
JNIEXPORT jclass JNICALL
Java_nsk_jvmti_scenarios_events_EM01_em01t002_loadClass(JNIEnv *jni_env,
                        jobject o, jobject loader, jstring className) {
    ExceptionCheckingJniEnvPtr ec_jni(jni_env);
    jclass klass;
    jmethodID methodID;
    jclass loadedClass;

    klass = ec_jni->GetObjectClass(loader, TRACE_JNI_CALL);
    methodID = ec_jni->GetMethodID(
            klass, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", TRACE_JNI_CALL);
    loadedClass = (jclass) ec_jni->CallObjectMethod(loader, methodID,
                                                    TRACE_JNI_CALL_VARARGS(className));

    return loadedClass;
}

/*
 * Class:     nsk_jvmti_scenarios_events_EM01_em01t002
 * Method:    prepareClass
 * Signature: (Ljava/lang/Class;)Z
 */
JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_events_EM01_em01t002_prepareClass(JNIEnv *jni,
                        jobject o, jclass klass) {
    ExceptionCheckingJniEnvPtr ec_jni(jni);
    jfieldID fieldID;

    fieldID = ec_jni->GetStaticFieldID(klass, "toProvokePreparation", "I", TRACE_JNI_CALL);
    return NSK_TRUE;
}

/*
 * Class:     nsk_jvmti_scenarios_events_EM01_em01t002
 * Method:    startThread
 * Signature: (Ljava/lang/Thread;)Z
 */
JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_events_EM01_em01t002_startThread(JNIEnv *jni_env,
                        jobject o, jobject thread) {
    ExceptionCheckingJniEnvPtr ec_jni(jni_env);
    jclass klass;
    jmethodID methodID;

    klass = ec_jni->GetObjectClass(thread, TRACE_JNI_CALL);
    methodID = ec_jni->GetMethodID(klass, "start", "()V", TRACE_JNI_CALL);
    ec_jni->CallVoidMethod(thread, methodID, TRACE_JNI_CALL);
    return NSK_TRUE;
}

/* ============================================================================= */
/* ============================================================================= */

static void
changeCount(jvmtiEvent event) {

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(syncLock)))
        nsk_jvmti_setFailStatus();

    eventCount[event - JVMTI_MIN_EVENT_TYPE_VAL]++;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(syncLock)))
        nsk_jvmti_setFailStatus();

}

/* ============================================================================= */

static void
showEventStatistics() {
    int i;
    const char* str;

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

/* callbacks */
void
classEventsHandler(jvmtiEvent event, jvmtiEnv* jvmti_env, JNIEnv* jni_env,
                            jclass klass) {

    char *className;
    char *generic;
    jvmtiPhase phase;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(klass, &className, &generic))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (strcmp(className, EXPECTED_CLASS_NAME) == 0) {
        changeCount(event);
        NSK_DISPLAY3("%25s(%4d)>>\tclass: %s\n",
                            TranslateEvent(event),
                            eventCount[event - JVMTI_MIN_EVENT_TYPE_VAL],
                            className);
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetPhase(&phase))) {
        nsk_jvmti_setFailStatus();
    }

    if (phase != currentPhase) {
        NSK_DISPLAY2("Unexpected phase %s, but supposed %s",
                TranslatePhase(phase), TranslatePhase(currentPhase));
    }

    if ((phase != JVMTI_PHASE_LIVE) && (phase != JVMTI_PHASE_START)) {
        NSK_COMPLAIN4("%25s was sent during %s(%d)\n\tclass: %s\n",
                    TranslateEvent(event),
                    TranslatePhase(phase),
                    phase,
                    className);
        nsk_jvmti_setFailStatus();
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)className))) {
        nsk_jvmti_setFailStatus();
    }
    if (generic != NULL)
        if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)generic))) {
            nsk_jvmti_setFailStatus();
        }
}

void
threadEventHandler(jvmtiEvent event, jvmtiEnv* jvmti_env, JNIEnv* jni_env,
                            jthread thread) {
    ExceptionCheckingJniEnvPtr ec_jni(jni_env);
    jclass classObject;
    char *className;
    char *generic;
    jvmtiPhase phase;


    classObject = ec_jni->GetObjectClass(thread, TRACE_JNI_CALL);

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(classObject, &className, &generic))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (strcmp(className, EXPECTED_CLASS_NAME) == 0) {
        changeCount(event);
        NSK_DISPLAY3("%25s(%4d)>>\tclass: %s\n",
                            TranslateEvent(event),
                            eventCount[event - JVMTI_MIN_EVENT_TYPE_VAL],
                            className);
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetPhase(&phase))) {
        nsk_jvmti_setFailStatus();
    }

    if (phase != currentPhase) {
        NSK_DISPLAY2("Unexpected phase %s, but supposed %s",
                TranslatePhase(phase), TranslatePhase(currentPhase));
    }

    if ((phase != JVMTI_PHASE_START) && (phase != JVMTI_PHASE_LIVE)) {
        NSK_COMPLAIN4("%25s was sent during %s(%d)\n\tclass: %s\n",
                    TranslateEvent(event),
                    TranslatePhase(phase),
                    phase,
                    className);
        nsk_jvmti_setFailStatus();
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)className))) {
        nsk_jvmti_setFailStatus();
    }
    if (generic != NULL)
        if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)generic))) {
            nsk_jvmti_setFailStatus();
        }
}

JNIEXPORT void JNICALL
cbVMStart(jvmtiEnv* jvmti_env, JNIEnv* jni_env) {

    jvmtiPhase phase;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetPhase(&phase))) {
        nsk_jvmti_setFailStatus();
    }

    if ((phase != JVMTI_PHASE_START) && (phase != JVMTI_PHASE_LIVE)) {
        NSK_COMPLAIN3("%25s was sent during %s(%d)\n",
                    TranslateEvent(JVMTI_EVENT_VM_START),
                    TranslatePhase(phase),
                    phase);
        nsk_jvmti_setFailStatus();
    }

    changeCount(JVMTI_EVENT_VM_START);
    currentPhase = JVMTI_PHASE_START;
}

JNIEXPORT void JNICALL
cbVMInit(jvmtiEnv* jvmti_env, JNIEnv* jni_env, jthread thread) {

    jvmtiPhase phase;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetPhase(&phase))) {
        nsk_jvmti_setFailStatus();
    }

    if (phase != JVMTI_PHASE_LIVE) {
        NSK_COMPLAIN3("%25s was sent during %s(%d)\n",
                    TranslateEvent(JVMTI_EVENT_VM_INIT),
                    TranslatePhase(phase),
                    phase);
        nsk_jvmti_setFailStatus();
    }

    changeCount(JVMTI_EVENT_VM_INIT);
    currentPhase = JVMTI_PHASE_LIVE;
}

JNIEXPORT void JNICALL
cbVMDeath(jvmtiEnv* jvmti_env, JNIEnv* jni_env) {

    jvmtiPhase phase;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetPhase(&phase))) {
        nsk_jvmti_setFailStatus();
    }

    if (phase != JVMTI_PHASE_LIVE) {
        NSK_COMPLAIN3("%25s was sent during %s(%d)\n",
                    TranslateEvent(JVMTI_EVENT_VM_INIT),
                    TranslatePhase(phase),
                    phase);
        nsk_jvmti_setFailStatus();
    }

    currentPhase = JVMTI_PHASE_DEAD;
    changeCount(JVMTI_EVENT_VM_DEATH);

    if (!NSK_JVMTI_VERIFY(jvmti->DestroyRawMonitor(syncLock)))
        nsk_jvmti_setFailStatus();

}

JNIEXPORT void JNICALL
cbClassLoad(jvmtiEnv* jvmti_env, JNIEnv* jni_env, jthread thread,
                    jclass klass) {

    classEventsHandler(JVMTI_EVENT_CLASS_LOAD, jvmti_env, jni_env, klass);
}

JNIEXPORT void JNICALL
cbClassPrepare(jvmtiEnv* jvmti_env, JNIEnv* jni_env, jthread thread,
                    jclass klass) {

    classEventsHandler(JVMTI_EVENT_CLASS_PREPARE, jvmti_env, jni_env, klass);
}

JNIEXPORT void JNICALL
cbThreadStart(jvmtiEnv* jvmti_env, JNIEnv* jni_env, jthread thread) {

    threadEventHandler(JVMTI_EVENT_THREAD_START, jvmti_env, jni_env, thread);
}

JNIEXPORT void JNICALL
cbThreadEnd(jvmtiEnv* jvmti_env, JNIEnv* jni_env, jthread thread) {

    threadEventHandler(JVMTI_EVENT_THREAD_END, jvmti_env, jni_env, thread);
}

/* ============================================================================= */

static int
enableEvent(jvmtiEventMode enable, jvmtiEvent event) {

    if (enable == JVMTI_ENABLE) {
        NSK_DISPLAY1("enabling %s\n", TranslateEvent(event));
    } else {
        NSK_DISPLAY1("disabling %s\n", TranslateEvent(event));
    }


    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(enable, event, NULL))) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }

    return NSK_TRUE;
}

/* ============================================================================= */

/**
 * Testcase: check tested events.
 *   - check if expected events received for each method
 *
 * Returns NSK_TRUE if test may continue; or NSK_FALSE for test break.
 */
int checkEvents(int step) {

    int i;
    jvmtiEvent curr;
    int result = NSK_TRUE;
    int mustBeChecked;

    showEventStatistics();

    for (i = 0; i < JVMTI_EVENT_COUNT; i++) {

        curr = (jvmtiEvent) (i + JVMTI_MIN_EVENT_TYPE_VAL);
        switch (step) {
        case 1:
            mustBeChecked = ((curr == JVMTI_EVENT_CLASS_LOAD)
                    || (curr == JVMTI_EVENT_CLASS_PREPARE));
            break;

        case 2:
            mustBeChecked = ((curr == JVMTI_EVENT_CLASS_LOAD)
                    || (curr == JVMTI_EVENT_CLASS_PREPARE)
                    || (curr == JVMTI_EVENT_THREAD_START)
                    || (curr == JVMTI_EVENT_THREAD_END));
            break;
        default:
            mustBeChecked = NSK_TRUE;
        }

        if (mustBeChecked && eventCount[i] != classLoaderCount) {
                nsk_jvmti_setFailStatus();
                NSK_COMPLAIN3("Unexpected number of %s events %7d\n\texpected value %d\n",
                                    TranslateEvent(curr),
                                    eventCount[i],
                                    classLoaderCount);
                nsk_jvmti_setFailStatus();
                result = NSK_FALSE;
        }
    }

    return result;
}

/* ============================================================================= */

static int
setCallBacks() {
    jvmtiEventCallbacks eventCallbacks;
    memset(&eventCallbacks, 0, sizeof(eventCallbacks));

    eventCallbacks.VMStart      = cbVMStart;
    eventCallbacks.VMInit       = cbVMInit;
    eventCallbacks.VMDeath      = cbVMDeath;
    eventCallbacks.ClassLoad    = cbClassLoad;
    eventCallbacks.ClassPrepare = cbClassPrepare;
    eventCallbacks.ThreadStart  = cbThreadStart;
    eventCallbacks.ThreadEnd    = cbThreadEnd;

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

    NSK_DISPLAY0("Let debuggee to load class\n");
    if (!nsk_jvmti_resumeSync())
        return;

    if (!nsk_jvmti_waitForSync(timeout))
        return;

    /* check only CLASS_LOAD and CLASS_PREPARE events */
    if (!checkEvents(1)) {
        nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY0("Let debuggee to start threads\n");
    if (!nsk_jvmti_resumeSync())
        return;

    if (!nsk_jvmti_waitForSync(timeout))
        return;

    NSK_DISPLAY0("check event 2\n");
    if (!checkEvents(2)) {
        nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY0("Let debuggee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;

}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_em01t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_em01t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_em01t002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {

    currentPhase = JVMTI_PHASE_ONLOAD;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;
    classLoaderCount = nsk_jvmti_findOptionIntValue(CLASS_LOADER_COUNT_PARAM, 10);

    jvmti = nsk_jvmti_createJVMTIEnv(jvm, reserved);
    if (!NSK_VERIFY(jvmti != NULL))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("_syncLock", &syncLock))) {
        nsk_jvmti_setFailStatus();
        return JNI_ERR;
    }

    if (!setCallBacks()) {
        return JNI_ERR;
    }

    if (!enableEvent(JVMTI_ENABLE, JVMTI_EVENT_CLASS_LOAD)
            || !enableEvent(JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE)
            || !enableEvent(JVMTI_ENABLE, JVMTI_EVENT_THREAD_START)
            || !enableEvent(JVMTI_ENABLE, JVMTI_EVENT_THREAD_END)
            || !enableEvent(JVMTI_ENABLE, JVMTI_EVENT_VM_START)
            || !enableEvent(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT)
            || !enableEvent(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH)
            ) {
        NSK_COMPLAIN0("Events could not be enabled");
        nsk_jvmti_setFailStatus();
        return JNI_ERR;
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    currentPhase = JVMTI_PHASE_PRIMORDIAL;

    return JNI_OK;
}

}
