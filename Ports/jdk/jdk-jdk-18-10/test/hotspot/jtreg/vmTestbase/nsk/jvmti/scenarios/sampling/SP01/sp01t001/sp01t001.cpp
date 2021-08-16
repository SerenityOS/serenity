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

/* constant names */
#define DEBUGEE_CLASS_NAME      "nsk/jvmti/scenarios/sampling/SP01/sp01t001"
#define THREAD_CLASS_NAME       "nsk/jvmti/scenarios/sampling/SP01/sp01t001Thread"
#define THREADS_FIELD_NAME      "threads"
#define THREADS_FIELD_SIG       "[L" THREAD_CLASS_NAME ";"

/* scaffold objects */
static JNIEnv* jni = NULL;
static jvmtiEnv *jvmti = NULL;
static jlong timeout = 0;

/* number of tested threads */
#define THREADS_COUNT 2

/* names of tested threads */
static const char* threadsName[THREADS_COUNT] = {
    "NotStarted",
    "Finished"
};

#define JVMTI_THREAD_STATE_NOT_STARTED 0
/* expected states of tested threads */
static jint threadsState[THREADS_COUNT] = {
    JVMTI_THREAD_STATE_NOT_STARTED,    /* JVMTI_THREAD_STATUS_NOT_STARTED */
    JVMTI_THREAD_STATE_TERMINATED      /* JVMTI_THREAD_STATUS_ZOMBIE */
};

/* references to tested threads */
static jthread threadsList[THREADS_COUNT];

/* ============================================================================= */

/* testcase(s) */
static int prepare();
static int checkThreads(const char* kind);
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
        /* obtain threads list */
        if (!prepare()) {
            nsk_jvmti_setFailStatus();
            return;
        }

        /* testcase #1: check not alive threads */
        NSK_DISPLAY0("Testcase #1: check state of not alive threads\n");
        if (!checkThreads("not alive"))
            return;

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
 * Testcase: check threads state for given suspension
 *
 * Returns NSK_TRUE if test may continue; or NSK_FALSE for test break.
 */
static int checkThreads(const char* kind) {
    int i;

    /* check each thread */
    for (i = 0; i < THREADS_COUNT; i++) {
        jthread thread = threadsList[i];
        jint state = JVMTI_THREAD_STATE_NOT_STARTED;

        NSK_DISPLAY2("    thread #%d (%s):\n", i, threadsName[i]);

        /* get thread state */
        if (!NSK_JVMTI_VERIFY(jvmti->GetThreadState(threadsList[i], &state))) {
            nsk_jvmti_setFailStatus();
            return NSK_TRUE;
        }

        /* display thread state */
        NSK_DISPLAY2("        state=%s (%d)\n",
                                TranslateState(state), (int)state);

        /* check thread state */
        if ((state & threadsState[i]) == 0) {
            if (state == JVMTI_THREAD_STATE_NOT_STARTED) {
                NSK_DISPLAY1("state of thread #%d is NOT_STARTED\n", kind);
            } else {
                NSK_COMPLAIN7("Unexpected state of %s thread #%d (%s):\n"
                                "#   got: %s (%d), expected: %s (%d)\n",
                                kind, i, threadsName[i],
                                TranslateState(state), (int)state,
                                TranslateState(threadsState[i]), (int)threadsState[i]);
                nsk_jvmti_setFailStatus();
            }
        }

        /* check SUSPENDED state flag */
        if (state & JVMTI_THREAD_STATE_SUSPENDED) {
            NSK_COMPLAIN3("Unexpected SUSPENDED state flag for %s thread #%d: %d\n",
                                                    kind, i, (int)state);
            nsk_jvmti_setFailStatus();
        }

        /* check INTERRUPTED state flag */
        if (state & JVMTI_THREAD_STATE_INTERRUPTED) {
            NSK_COMPLAIN3("Unexpected INTERRUPTED state flag for %s thread #%d: %d\n",
                                                    kind, i, (int)state);
            nsk_jvmti_setFailStatus();
        }

        /* check NATIVE state flag */
        if (state & JVMTI_THREAD_STATE_IN_NATIVE) {
            NSK_COMPLAIN3("Unexpected NATIVE state flag for %s thread #%d: %d\n",
                                                    kind, i, (int)state);
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
        NSK_TRACE(jni->DeleteGlobalRef(threadsList[i]));
    }

    return NSK_TRUE;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_sp01t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_sp01t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_sp01t001(JavaVM *jvm, char *options, void *reserved) {
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

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
