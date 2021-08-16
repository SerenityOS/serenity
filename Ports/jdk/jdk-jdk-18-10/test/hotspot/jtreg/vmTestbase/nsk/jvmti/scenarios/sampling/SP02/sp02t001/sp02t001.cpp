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

/* constants */
#define THREADS_COUNT   6
#define MAX_NAME_LENGTH 100
#define MAX_STACK_SIZE  100

/* thread description structure */
typedef struct {
    char threadName[MAX_NAME_LENGTH];
    int minDepth;
    jthread thread;
} ThreadDesc;

/* descriptions of tested threads */
static ThreadDesc threadsDesc[THREADS_COUNT] = {
    { "threadRunning", 2, NULL },
    { "threadEntering", 2, NULL },
    { "threadWaiting", 2, NULL },
    { "threadSleeping", 2, NULL },
    { "threadRunningInterrupted", 2, NULL },
    { "threadRunningNative", 2, NULL }
};

/* ============================================================================= */

/* testcase(s) */
static int prepare();
static int checkSuspendedThreads();
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
        NSK_DISPLAY0("Prepare data\n");
        if (!prepare()) {
            nsk_jvmti_setFailStatus();
            return;
        }
        /* suspend threads */
        NSK_DISPLAY0("Suspend each thread\n");
        if (!suspendThreadsIndividually(NSK_TRUE))
            return;

        /* check suspended threads */
        NSK_DISPLAY0("Check stack frames of suspended threads\n");
        if (!checkSuspendedThreads())
            return;

        /* resume threads */
        NSK_DISPLAY0("Resume each thread\n");
        if (!suspendThreadsIndividually(NSK_FALSE))
            return;

        /* clean date: delete threads references */
        NSK_DISPLAY0("Clean data\n");
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
 *    - get all live threads with names
 *    - find tested threads by name
 *    - make global refs
 */
static int prepare() {
    jthread *allThreadsList = NULL;
    jint allThreadsCount = 0;
    int found = 0;
    int i;

    NSK_DISPLAY1("Find tested threads: %d\n", THREADS_COUNT);

    /* clean threads list */
    for (i = 0; i < THREADS_COUNT; i++) {
        threadsDesc[i].thread = (jthread)NULL;
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
            int j;

            for (j = 0; j < THREADS_COUNT; j++) {
                if (strcmp(threadInfo.name, threadsDesc[j].threadName) == 0) {
                    threadsDesc[j].thread = allThreadsList[i];
                    NSK_DISPLAY3("    thread #%d (%s): %p\n",
                                            j, threadInfo.name, (void*)threadsDesc[j].thread);
                }
            }
        }
    }

    /* deallocate all threads list */
    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)allThreadsList)))
        return NSK_FALSE;

    /* check if all tested threads found */
    found = 0;
    for (i = 0; i < THREADS_COUNT; i++) {
        if (threadsDesc[i].thread == NULL) {
            NSK_COMPLAIN2("Not found tested thread #%d (%s)\n", i, threadsDesc[i].threadName);
        } else {
            found++;
        }
    }

    if (found < THREADS_COUNT)
        return NSK_FALSE;

    /* make global refs */
    for (i = 0; i < THREADS_COUNT; i++) {
        if (!NSK_JNI_VERIFY(jni, (threadsDesc[i].thread = (jthread)
                jni->NewGlobalRef(threadsDesc[i].thread)) != NULL))
            return NSK_FALSE;
    }

    return NSK_TRUE;
}

/**
 * Suspend or resume tested threads.
 */
static int suspendThreadsIndividually(int suspend) {
    int i;

    for (i = 0; i < THREADS_COUNT; i++) {
        if (suspend) {
            NSK_DISPLAY2("    suspend thread #%d (%s)\n", i, threadsDesc[i].threadName);
            if (!NSK_JVMTI_VERIFY(jvmti->SuspendThread(threadsDesc[i].thread)))
                nsk_jvmti_setFailStatus();
        } else {
            NSK_DISPLAY2("    resume thread #%d (%s)\n", i, threadsDesc[i].threadName);
            if (!NSK_JVMTI_VERIFY(jvmti->ResumeThread(threadsDesc[i].thread)))
                nsk_jvmti_setFailStatus();
        }
    }
    return NSK_TRUE;
}

/**
 * Testcase: check tested threads
 *    - invoke getFrameCount() for each thread
 *    - check if frameCount is not less than minimal stack depth
 *    - invoke getStackTrace() for each thread
 *    - check if stack depth is equal to frameCount
 *
 * Returns NSK_TRUE if test may continue; or NSK_FALSE for test break.
 */
static int checkSuspendedThreads() {
    int i;

    /* check each thread */
    for (i = 0; i < THREADS_COUNT; i++) {
        jint frameCount = 0;
        jint frameStackSize = 0;
        jvmtiFrameInfo frameStack[MAX_STACK_SIZE];
        int found = 0;

        NSK_DISPLAY2("  thread #%d (%s):\n", i, threadsDesc[i].threadName);

        /* get frame count */
        if (!NSK_JVMTI_VERIFY(jvmti->GetFrameCount(threadsDesc[i].thread, &frameCount))) {
            nsk_jvmti_setFailStatus();
            return NSK_TRUE;
        }

        NSK_DISPLAY1("    frameCount:  %d\n", (int)frameCount);

        /* get stack trace */
        if (!NSK_JVMTI_VERIFY(
                jvmti->GetStackTrace(threadsDesc[i].thread, 0, MAX_STACK_SIZE, frameStack, &frameStackSize))) {
            nsk_jvmti_setFailStatus();
            return NSK_TRUE;
        }

        NSK_DISPLAY1("    stack depth: %d\n", (int)frameStackSize);

        /* check frame count */
        if (frameCount < threadsDesc[i].minDepth) {
            NSK_COMPLAIN4("Too few frameCount of suspended thread #%d (%s):\n"
                            "#   got frameCount:   %d\n"
                            "#   expected minimum: %d\n",
                            i, threadsDesc[i].threadName,
                            (int)frameCount, threadsDesc[i].minDepth);
            nsk_jvmti_setFailStatus();
        }

        if (frameStackSize != frameCount) {
            NSK_COMPLAIN4("Different frames count for suspended thread #%d (%s):\n"
                            "#   getStackTrace(): %d\n"
                            "#   getFrameCount(): %d\n",
                            i, threadsDesc[i].threadName,
                            (int)frameStackSize, (int)frameCount);
            nsk_jvmti_setFailStatus();
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
    int i;

    /* dispose global references to threads */
    for (i = 0; i < THREADS_COUNT; i++) {
        NSK_TRACE(jni->DeleteGlobalRef(threadsDesc[i].thread));
    }

    return NSK_TRUE;
}

/* ============================================================================= */

static volatile int testedThreadRunning = NSK_FALSE;
static volatile int testedThreadShouldFinish = NSK_FALSE;

/** Native running method in tested thread. */
JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_sampling_SP02_sp02t001ThreadRunningNative_testedMethod(JNIEnv* jni,
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
Java_nsk_jvmti_scenarios_sampling_SP02_sp02t001ThreadRunningNative_checkReady(JNIEnv* jni,
                                                                            jobject obj) {
    while (!testedThreadRunning) {
        nsk_jvmti_sleep(1000);
    }
    return testedThreadRunning ? JNI_TRUE : JNI_FALSE;
}

/** Let native method to finish. */
JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_sampling_SP02_sp02t001ThreadRunningNative_letFinish(JNIEnv* jni,
                                                                            jobject obj) {
    testedThreadShouldFinish = NSK_TRUE;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_sp02t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_sp02t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_sp02t001(JavaVM *jvm, char *options, void *reserved) {
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
