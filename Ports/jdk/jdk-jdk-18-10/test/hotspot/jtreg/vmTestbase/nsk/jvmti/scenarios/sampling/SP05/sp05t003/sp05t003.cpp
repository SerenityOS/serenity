/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* constant names */
#define DEBUGEE_CLASS_NAME      "nsk/jvmti/scenarios/sampling/SP05/sp05t003"
#define THREAD_CLASS_NAME       "nsk/jvmti/scenarios/sampling/SP05/sp05t003Thread"
#define THREADS_FIELD_NAME      "threads"
#define THREADS_FIELD_SIG       "[L" THREAD_CLASS_NAME ";"

/* constants */
#define TIMEOUT_DELTA  1000

/* scaffold objects */
static JNIEnv* jni = NULL;
static jvmtiEnv *jvmti = NULL;
static jlong timeout = 0;

/* number of tested threads and events */
#define THREADS_COUNT   2
#define EVENTS_COUNT    2
#define MAX_STACK_DEPTH 64

/* tested events */
static jvmtiEvent eventsList[EVENTS_COUNT] = {
    JVMTI_EVENT_THREAD_START,
    JVMTI_EVENT_THREAD_END
};

/* tested threads names */
static const char* threadsName[THREADS_COUNT] = {
    "threadRunningJava",
    "threadRunningNative"
};

/* references to tested threads */
static jthread threadsList[THREADS_COUNT];

/* events counts */
static volatile int eventsStart = 0;
static volatile int eventsEnd   = 0;

/* ============================================================================= */

/* testcase(s) */
static int prepare();
static int checkThreads(const char* kind);
static int resumeThreads(const char* kind);
static int clean();

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* agentJNI, void* arg) {
    jni = agentJNI;

    /* wait for initial sync */
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    /* testcase(s) */
    {
        /* obtain threads list */
        if (!prepare()) {
            nsk_jvmti_setFailStatus();
            return;
        }

        /* testcase #1: suspend and check threads on THREAD_START event */
        {
            eventsStart = 0;
            if (!NSK_JVMTI_VERIFY(
                    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_START, NULL))) {
                nsk_jvmti_setFailStatus();
                return;
            }

            NSK_DISPLAY0("Let threads to start\n");
            if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
                return;

            NSK_DISPLAY0("Wait for THREAD_START events\n");
            {
                int i;

                for (i = 0; i * TIMEOUT_DELTA < timeout; i++) {
                    if (eventsStart >= THREADS_COUNT)
                        break;
                    nsk_jvmti_sleep(TIMEOUT_DELTA);
                }
            }

            if (!NSK_JVMTI_VERIFY(
                    jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_THREAD_START, NULL))) {
                nsk_jvmti_setFailStatus();
            }

            /* check if all threads suspended on THREAD_START events */
            if (eventsStart != THREADS_COUNT) {
                NSK_COMPLAIN2("Unexpected number of THREAD_START events:\n"
                             "#   received: %d\n"
                             "#   expected: %d\n",
                             eventsStart, THREADS_COUNT);
            }

            NSK_DISPLAY0("Testcase #1: check threads on THREAD_START\n");
            if (!NSK_VERIFY(checkThreads("starting")))
                return;

            NSK_DISPLAY0("Resume all threads\n");
            if (!NSK_VERIFY(resumeThreads("starting")))
                return;

            NSK_DISPLAY0("Wait for threads to run\n");
            if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
                return;
        }

        /* testcase #2: suspend and check threads on THREAD_END event */
        {
            eventsEnd = 0;
            if (!NSK_JVMTI_VERIFY(
                    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_END, NULL))) {
                nsk_jvmti_setFailStatus();
                return;
            }

            NSK_DISPLAY0("Let threads to finish\n");
            if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
                return;

            NSK_DISPLAY0("Wait for THREAD_END events\n");
            {
                int i;

                for (i = 0; i * TIMEOUT_DELTA < timeout; i++) {
                    if (eventsEnd >= THREADS_COUNT)
                        break;
                    nsk_jvmti_sleep(TIMEOUT_DELTA);
                }
            }

            if (!NSK_JVMTI_VERIFY(
                    jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_THREAD_END, NULL))) {
                nsk_jvmti_setFailStatus();
            }

            /* check ia all threads suspended on THREAD_END event */
            if (eventsEnd != THREADS_COUNT) {
                NSK_COMPLAIN2("Unexpected number of THREAD_END events:\n"
                             "#   received: %d\n"
                             "#   expected: %d\n",
                             eventsEnd, THREADS_COUNT);
            }

            NSK_DISPLAY0("Testcase #2: check threads on THREAD_END\n");
            if (!NSK_VERIFY(checkThreads("finishing")))
                return;

            NSK_DISPLAY0("Resume all threads\n");
            if (!NSK_VERIFY(resumeThreads("finishing")))
                return;

            NSK_DISPLAY0("Wait for threads to finish\n");
            if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
                return;
        }

        /* clean threads references */
        if (!clean()) {
            nsk_jvmti_setFailStatus();
            return;
        }
    }

    /* resume debugee after last sync */
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ============================================================================= */

/**
 * Resume all threads in given state.
 */
static int resumeThreads(const char* kind) {
    int i;

    for (i = 0; i < THREADS_COUNT; i++) {
        if (!NSK_JVMTI_VERIFY(jvmti->ResumeThread(threadsList[i]))) {
            nsk_jvmti_setFailStatus();
        }
    }
    return NSK_TRUE;
}

/**
 * Prepare data:
 *    - get threads array from static field
 *    - get each thread from array
 *    - make global refs
 */
static int prepare() {
    jclass debugeeClass = NULL;
    jfieldID threadsFieldID = NULL;
    jobjectArray threadsArray = NULL;
    jsize threadsArrayLength = 0;
    jsize i;

    /* find debugee class */
    if (!NSK_JNI_VERIFY(jni, (debugeeClass = jni->FindClass(DEBUGEE_CLASS_NAME)) != NULL))
        return NSK_FALSE;

    /* find static field with threads array */
    if (!NSK_JNI_VERIFY(jni, (threadsFieldID =
            jni->GetStaticFieldID(debugeeClass, THREADS_FIELD_NAME, THREADS_FIELD_SIG)) != NULL))
        return NSK_FALSE;

    /* get threads array from static field */
    if (!NSK_JNI_VERIFY(jni, (threadsArray = (jobjectArray)
            jni->GetStaticObjectField(debugeeClass, threadsFieldID)) != NULL))
        return NSK_FALSE;

    /* check array length */
    if (!NSK_JNI_VERIFY(jni, (threadsArrayLength =
            jni->GetArrayLength(threadsArray)) == THREADS_COUNT))
        return NSK_FALSE;

    /* get each thread from array */
    for (i = 0; i < THREADS_COUNT; i++) {
        if (!NSK_JNI_VERIFY(jni, (threadsList[i] = (jthread)
                jni->GetObjectArrayElement(threadsArray, i)) != NULL))
            return NSK_FALSE;
    }

    /* make global references to threads */
    for (i = 0; i < THREADS_COUNT; i++) {
        if (!NSK_JNI_VERIFY(jni, (threadsList[i] = (jthread)
                jni->NewGlobalRef(threadsList[i])) != NULL))
            return NSK_FALSE;
    }

    return NSK_TRUE;
}

/**
 * Check thread's stack on event.
 *
 * Returns NSK_TRUE if test may continue; or NSK_FALSE for test break.
 */
static int checkThread(jthread thread, int i, const char* kind) {
    jint framesCount = 0;
    jint stackDepth  = 0;
    jvmtiFrameInfo stackFrames[MAX_STACK_DEPTH];

    NSK_DISPLAY3("  thread #%d (%s): %p\n", i, threadsName[i], (void*)thread);

    /* get frames count */
    if (!NSK_JVMTI_VERIFY(jvmti->GetFrameCount(thread, &framesCount))) {
        nsk_jvmti_setFailStatus();
        return NSK_TRUE;
    }
    NSK_DISPLAY1("    frames count: %d\n", (int)framesCount);

    /* get stack frames */
    if (!NSK_JVMTI_VERIFY(
            jvmti->GetStackTrace(thread, 0, MAX_STACK_DEPTH, stackFrames, &stackDepth))) {
        nsk_jvmti_setFailStatus();
        return NSK_TRUE;
    }
    NSK_DISPLAY1("    stack depth:  %d\n", (int)stackDepth);

    /* check against emptyness */
    if (framesCount != 0) {
        NSK_COMPLAIN5("Unexpected GetFramesCount() for %s thread #%d (%s):\n"
                                "#   got frames: %d\n"
                                "#   expected:   %d\n",
                                kind, i, threadsName[i],
                                framesCount, 0);
        nsk_jvmti_setFailStatus();
    }
    if (stackDepth != 0) {
        NSK_COMPLAIN5("Unexpected GetStackTrace() for %s thread #%d (%s):\n"
                                "#   got frames: %d\n"
                                "#   expected:   %d\n",
                                kind, i, threadsName[i],
                                stackDepth, 0);
        nsk_jvmti_setFailStatus();
    }

    /* test may continue */
    return NSK_TRUE;
}

/**
 * Testcase: check each thread's stack on event.
 *
 * Returns NSK_TRUE if test may continue; or NSK_FALSE for test break.
 */
static int checkThreads(const char* kind) {
    int i;

    for (i = 0; i < THREADS_COUNT; i++) {
        if (!checkThread(threadsList[i], i, kind))
            return NSK_FALSE;
    }
    return NSK_TRUE;
}

/**
 * Clean data:
 *   - dispose global references to tested threads
 */
static int clean() {
    int i;

    /* dispose global references to threads */
    for (i = 0; i < THREADS_COUNT; i++) {
        NSK_TRACE(jni->DeleteGlobalRef(threadsList[i]));
    }

    return NSK_TRUE;
}

/* ============================================================================= */

/**
 * THREAD_START callback:
 *   - check thread's stack on THREAD_START
 */
JNIEXPORT void JNICALL
callbackThreadStart(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread) {
    int i;

    /* check if thread is not NULL */
    if (!NSK_VERIFY(thread != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* check if event is for tested thread */
    for (i = 0; i < THREADS_COUNT; i++) {
        if (jni->IsSameObject(threadsList[i], thread)) {
            NSK_DISPLAY0("SUCCESS: expected THREAD_START event\n");

            /* suspend thread */
            NSK_DISPLAY3("  suspend starting thread #%d (%s): %p\n",
                                i, threadsName[i], (void*)thread);
            /* must bump the count before we suspend */
            eventsStart++;

            if (!NSK_JVMTI_VERIFY(jvmti->SuspendThread(thread))) {
                nsk_jvmti_setFailStatus();
                return;
            }

            break;
        }
    }
}

/**
 * THREAD_END callback:
 *   - check thread's stack on THREAD_END
 */
JNIEXPORT void JNICALL
callbackThreadEnd(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread) {
    int i;

    /* check if thread is not NULL */
    if (!NSK_VERIFY(thread != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* check if event is for tested thread */
    for (i = 0; i < THREADS_COUNT; i++) {
        if (jni->IsSameObject(threadsList[i], thread)) {
                NSK_DISPLAY0("SUCCESS: expected THREAD_END event\n");
            /* must bump the count before we suspend */
            eventsEnd++;

            /* suspend thread */
            NSK_DISPLAY3("  suspend finishing thread #%d (%s): %p\n",
                                i, threadsName[i], (void*)thread);

            if (!NSK_JVMTI_VERIFY(jvmti->SuspendThread(thread))) {
                nsk_jvmti_setFailStatus();
                return;
            }

            break;
        }
    }
}

/* ============================================================================= */

static volatile int testedThreadRunning = NSK_FALSE;
static volatile int testedThreadShouldFinish = NSK_FALSE;

/** Native running method in tested thread. */
JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_sampling_SP05_sp05t003ThreadRunningNative_run(JNIEnv* jni,
                                                                            jobject obj) {
    volatile int i = 0, n = 1000;

    /* run in a loop */
    testedThreadRunning = NSK_TRUE;
    while (!testedThreadShouldFinish) {
        if (n <= 0)
            n = 1000;
        if (i >= n)
            i = 0;
        i++;
    }
    testedThreadRunning = NSK_FALSE;
}

/** Wait for native method is running. */
JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_sampling_SP05_sp05t003ThreadRunningNative_checkStarted(JNIEnv* jni,
                                                                            jobject obj) {
    while (!testedThreadRunning) {
        nsk_jvmti_sleep(1000);
    }
    return testedThreadRunning ? JNI_TRUE : JNI_FALSE;
}

/** Let native method to finish. */
JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_sampling_SP05_sp05t003ThreadRunningNative_letFinish(JNIEnv* jni,
                                                                            jobject obj) {
    testedThreadShouldFinish = NSK_TRUE;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_sp05t003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_sp05t003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_sp05t003(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* add capabilities for suspending thread */
    {
        jvmtiCapabilities suspendCaps;
        memset(&suspendCaps, 0, sizeof(suspendCaps));
        suspendCaps.can_suspend = 1;
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&suspendCaps)))
            return JNI_ERR;
    }

    /* set events callbacks */
    {
        jvmtiEventCallbacks eventCallbacks;
        memset(&eventCallbacks, 0, sizeof(eventCallbacks));
        eventCallbacks.ThreadStart = callbackThreadStart;
        eventCallbacks.ThreadEnd = callbackThreadEnd;
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks))))
            return JNI_ERR;
    }

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
