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

/* number of tested threads */
#define THREADS_KINDS           6
#define DEFAULT_THREADS_NUMBER  1

/* names of tested threads */
static const char* threadsName[THREADS_KINDS] = {
    "ThreadRunning",
    "ThreadEntering",
    "ThreadWaiting",
    "ThreadSleeping",
    "ThreadRunningInterrupted",
    "ThreadRunningNative"
};

/* expected states of tested threads */
#define JVMTI_THREAD_STATE_NOT_STARTED 0
static jint threadsState[THREADS_KINDS] = {
    JVMTI_THREAD_STATE_RUNNABLE,
    JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER,
    JVMTI_THREAD_STATE_IN_OBJECT_WAIT,
    JVMTI_THREAD_STATE_SLEEPING,
    JVMTI_THREAD_STATE_RUNNABLE,
    JVMTI_THREAD_STATE_RUNNABLE
};

/* references to tested threads */
static jthread* threadsList[THREADS_KINDS];
static int threadsCounts[THREADS_KINDS];
static int threadsCount = 0;

/* indexes of known threads */
static const int interruptedThreadIndex = THREADS_KINDS - 2;
static const int nativeThreadIndex = THREADS_KINDS - 1;

/* ============================================================================= */

/* testcase(s) */
static int prepare();
static int checkThreads(int suspended, const char* kind, jlong timeout);
static int suspendThreadsList(int suspend);
static int suspendThreadsIndividually(int suspend);
static int clean();

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* agentJNI, void* arg) {
    jni = agentJNI;

    /* wait for initial sync */
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    /* perform testcase(s) */
    {
        /* prepare data: find threads */
        if (!prepare()) {
            nsk_jvmti_setFailStatus();
            return;
        }

        /* testcase #1: check not suspended threads */
        NSK_DISPLAY0("Testcase #1: check state of not suspended threads\n");
        if (!checkThreads(NSK_FALSE, "not suspended", timeout))
            return;

        /* suspend threads */
        NSK_DISPLAY0("Suspend threads individually\n");
        if (!suspendThreadsIndividually(NSK_TRUE))
            return;

        /* testcase #2: check suspended threads */
        NSK_DISPLAY0("Testcase #2: check state of suspended threads\n");
        if (!checkThreads(NSK_TRUE, "suspended", 0))
            return;

        /* resume threads */
        NSK_DISPLAY0("Resume threads list\n");
        if (!suspendThreadsList(NSK_FALSE))
            return;

        /* testcase #3: check resumed threads */
        NSK_DISPLAY0("Testcase #3: check state of resumed threads\n");
        if (!checkThreads(NSK_FALSE, "resumed", 0))
            return;

        /* clean date: delete threads references */
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
 * Prepare data:
 *    - clean threads list
 *    - get all live threads
 *    - get threads name
 *    - find tested threads
 *    - make global refs
 */
static int prepare() {
    jthread *allThreadsList = NULL;
    jint allThreadsCount = 0;
    int notfound = 0;
    int i, j;

    NSK_DISPLAY1("Prepare: find tested threads: %d kinds\n", THREADS_KINDS);

    /* allocate and clean threads list */
    for (i = 0; i < THREADS_KINDS; i++) {
        threadsCounts[i] = 0;
        threadsList[i] = NULL;

        if (!NSK_JVMTI_VERIFY(jvmti->Allocate(threadsCount * sizeof(jthread),
                                              (unsigned char**)&threadsList[i])))
            return NSK_FALSE;

        for (j = 0; j < threadsCount; j++) {
            threadsList[i][j] = NULL;
        }
    }

    /* get all live threads */
    if (!NSK_JVMTI_VERIFY(jvmti->GetAllThreads(&allThreadsCount, &allThreadsList)))
        return NSK_FALSE;

    if (!NSK_VERIFY(allThreadsCount > 0 && allThreadsList != NULL))
        return NSK_FALSE;

    /* find tested threads */
    for (i = 0; i < allThreadsCount; i++) {
        jvmtiThreadInfo threadInfo;

        if (!NSK_VERIFY(allThreadsList[i] != NULL))
            return NSK_FALSE;

        /* get thread name (info) */
        if (!NSK_JVMTI_VERIFY(jvmti->GetThreadInfo(allThreadsList[i], &threadInfo)))
            return NSK_FALSE;

        /* find by name */
        if (threadInfo.name != NULL) {
            for (j = 0; j < THREADS_KINDS; j++) {
                if (strcmp(threadInfo.name, threadsName[j]) == 0) {
                    int k = threadsCounts[j];
                    if (k < threadsCount)
                        threadsList[j][k] = allThreadsList[i];
                    threadsCounts[j]++;
                }
            }
        }
    }

    /* deallocate all threads list */
    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)allThreadsList)))
        return NSK_FALSE;

    /* check if all tested threads found */
    notfound = 0;
    for (i = 0; i < THREADS_KINDS; i++) {
        if (threadsCounts[i] != threadsCount) {
            NSK_COMPLAIN3("Found unexpected number of tested threads (%s):\n"
                            "#   found:    %d\n"
                            "#   expected: %d\n",
                            threadsName[i], threadsCounts[i], threadsCount);
            nsk_jvmti_setFailStatus();
            notfound++;
        }
    }

    if (notfound > 0)
        return NSK_FALSE;

    /* make global refs */
    for (i = 0; i < THREADS_KINDS; i++) {
        for (j = 0; j < threadsCount; j++) {
            if (!NSK_JNI_VERIFY(jni, (threadsList[i][j] =
                    jni->NewGlobalRef(threadsList[i][j])) != NULL))
                return NSK_FALSE;
        }
    }

    return NSK_TRUE;
}

/**
 * Suspend or resume tested threads list.
 */
static int suspendThreadsList(int suspend) {
    jlong resultsSize = threadsCount * sizeof(jvmtiError);
    jvmtiError* results = NULL;
    const char* kind = (suspend ? "suspending" : "resuming");
    int i, j;

    /* allocate results array */
    if (!NSK_JVMTI_VERIFY(jvmti->Allocate(resultsSize, (unsigned char**)&results))) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }

    for (i = 0; i < THREADS_KINDS; i++) {
        /* suspend or resume threads list */
        if (suspend) {
            if (!NSK_JVMTI_VERIFY(jvmti->SuspendThreadList(threadsCount, threadsList[i], results)))
                nsk_jvmti_setFailStatus();
        } else {
            if (!NSK_JVMTI_VERIFY(jvmti->ResumeThreadList(threadsCount, threadsList[i], results)))
                nsk_jvmti_setFailStatus();
        }

        /* check results */
        for (j = 0; j < threadsCount; j++) {
            if (results[j] != JVMTI_ERROR_NONE) {
                NSK_COMPLAIN5("Unexpected result of %s thread #%d (%s):\n"
                                "#   got result: %s (%d)\n",
                                kind, j, threadsName[i],
                                TranslateError(results[j]), (int)results[j]);
                nsk_jvmti_setFailStatus();
            }
        }
    }

    /* deallocate results array */
    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)results))) {
        nsk_jvmti_setFailStatus();
    }

    return NSK_TRUE;
}

/**
 * Suspend or resume tested threads individually.
 */
static int suspendThreadsIndividually(int suspend) {
    int i, j;

    for (i = 0; i < THREADS_KINDS; i++) {
        for (j = 0; j < threadsCount; j++) {
            if (suspend)  {
                NSK_DISPLAY2("    suspend thread #%d (%s)\n", j, threadsName[i]);
                if (!NSK_JVMTI_VERIFY(jvmti->SuspendThread(threadsList[i][j])))
                    nsk_jvmti_setFailStatus();
            } else {
                NSK_DISPLAY2("    resume thread #%d (%s)\n", j, threadsName[i]);
                if (!NSK_JVMTI_VERIFY(jvmti->ResumeThread(threadsList[i][j])))
                    nsk_jvmti_setFailStatus();
            }
        }
    }
    return NSK_TRUE;
}

/**
 * Testcase: check tested threads
 *    - get thread state
 *    - wait for WAITIME if state is not expected
 *    - check if thread state is as expected
 *
 * Returns NSK_TRUE if test may continue; or NSK_FALSE for test break.
 */
static int checkThreads(int suspended, const char* kind, jlong timeout) {
    int i, j;

    /* check each thread */
    for (i = 0; i < THREADS_KINDS; i++) {
        for (j = 0; j < threadsCount; j++) {
            jint state = JVMTI_THREAD_STATE_NOT_STARTED;
            jlong t = 0;

            NSK_DISPLAY2("    thread #%d (%s):\n", j, threadsName[i]);

            /* get thread state */
            if (!NSK_JVMTI_VERIFY(jvmti->GetThreadState(threadsList[i][j], &state))) {
                nsk_jvmti_setFailStatus();
                return NSK_FALSE;
            }

            NSK_DISPLAY2("        flags  = %s (%d)\n",
                                        TranslateState(state), (int)state);

            /* check SUSPENDED state */
            if (suspended) {
                if (!(state & JVMTI_THREAD_STATE_SUSPENDED)) {
                    NSK_COMPLAIN5("No SUSPENDED state for %s thread #%d (%s):\n"
                                    "#    got flags: %s (%d)\n",
                                    kind, j, threadsName[i],
                                    TranslateState(state), (int)state);
                    nsk_jvmti_setFailStatus();
                }
            } else {
                if (state & JVMTI_THREAD_STATE_SUSPENDED) {
                    NSK_COMPLAIN5("Unexpected SUSPENDED state for %s thread #%d (%s):\n"
                                    "#   got flags: %s (%d)\n",
                                    kind, j, threadsName[i],
                                    TranslateState(state), (int)state);
                    nsk_jvmti_setFailStatus();
                }
            }
        }
    }

    /* test may continue */
    return NSK_TRUE;
}

/**
 * Clean data:
 *   - dispose global references to tested threads
 */
static int clean() {
    int i, j;

    /* dispose global references to threads */
    for (i = 0; i < THREADS_KINDS; i++) {
        for (j = 0; j < threadsCount; j++) {
            NSK_TRACE(jni->DeleteGlobalRef(threadsList[i][j]));
        }
    }

    /* deallocate memory */
    for (i = 0; i < THREADS_KINDS; i++) {
        if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)threadsList[i])))
            return NSK_FALSE;
        threadsList[i] = NULL;
    }

    return NSK_TRUE;
}

/* ============================================================================= */

static volatile int testedThreadRunning = NSK_FALSE;
static volatile int testedThreadShouldFinish = NSK_FALSE;

/** Native running method in tested thread. */
JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_sampling_SP04_sp04t002ThreadRunningNative_nativeMethod(JNIEnv* jni,
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
Java_nsk_jvmti_scenarios_sampling_SP04_sp04t002ThreadRunningNative_checkReady(JNIEnv* jni,
                                                                                   jobject obj) {
    while (!testedThreadRunning) {
        nsk_jvmti_sleep(1000);
    }
    return testedThreadRunning ? JNI_TRUE : JNI_FALSE;
}

/* Let native method to finish. */
JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_sampling_SP04_sp04t002ThreadRunningNative_letFinish(JNIEnv* jni,
                                                                                  jobject obj) {
    testedThreadShouldFinish = NSK_TRUE;
}

/* ============================================================================= */

/* Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_sp04t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_sp04t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_sp04t002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    /* get number of threads for each kind */
    threadsCount = nsk_jvmti_findOptionIntValue("threads", DEFAULT_THREADS_NUMBER);
    if (!NSK_VERIFY(threadsCount > 0))
        return JNI_ERR;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* add specific capabilities for suspending thread */
    {
        jvmtiCapabilities suspendCaps;
        memset(&suspendCaps, 0, sizeof(suspendCaps));
        suspendCaps.can_suspend = 1;
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&suspendCaps)))
            return JNI_ERR;
    }

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
