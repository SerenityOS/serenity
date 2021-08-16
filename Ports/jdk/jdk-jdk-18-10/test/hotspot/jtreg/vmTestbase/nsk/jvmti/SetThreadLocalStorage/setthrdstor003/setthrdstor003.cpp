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
static jlong timeout = 0;

/* constan names */
#define THREAD_NAME     "TestedThread"

/* constants */
#define STORAGE_DATA_SIZE       1024
#define STORAGE_DATA_CHAR       'X'
#define EVENTS_COUNT            2

/* events list */
static jvmtiEvent eventsList[EVENTS_COUNT] = {
    JVMTI_EVENT_THREAD_START,
    JVMTI_EVENT_THREAD_END
};

/* storage structure */
typedef struct _StorageStructure {
    char data[STORAGE_DATA_SIZE];
} StorageStructure;

/* storage data */
static StorageStructure storageData;
static StorageStructure* initialStorage = &storageData;

/* events counts */
static int eventsStart = 0;
static int eventsEnd = 0;

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    NSK_DISPLAY0("Wait for thread to create\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    /* perform testing */
    {
        memset(storageData.data, STORAGE_DATA_CHAR, STORAGE_DATA_SIZE);

        eventsStart = 0;
        eventsEnd = 0;
        NSK_DISPLAY1("Enable events: %d events\n", EVENTS_COUNT);
        if (!nsk_jvmti_enableEvents(JVMTI_ENABLE, EVENTS_COUNT, eventsList, NULL))
            return;

        NSK_DISPLAY0("Let tested thread to run\n");
        if (!nsk_jvmti_resumeSync())
            return;

        NSK_DISPLAY0("Wait for tested thread to finish\n");
        if (!nsk_jvmti_waitForSync(timeout))
            return;

        NSK_DISPLAY1("Disable events: %d events\n", EVENTS_COUNT);
        if (!nsk_jvmti_enableEvents(JVMTI_DISABLE, EVENTS_COUNT, eventsList, NULL))
            return;

        NSK_DISPLAY1("Check if all expected events received for tested thread: %s\n",
                                                                        THREAD_NAME);
        if (eventsStart <= 0 || eventsStart != eventsEnd) {
            NSK_COMPLAIN3("Unexpected number of events received for tedted thread:\n"
                          "#   thread name:  %s\n"
                          "#   THREAD_START: %d events\n"
                          "#   THREAD_END:   %d events\n",
                            THREAD_NAME,
                            eventsStart, eventsEnd);
            nsk_jvmti_setFailStatus();
        }
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ============================================================================= */

/** THREAD_START callback. */
JNIEXPORT void JNICALL
callbackThreadStart(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread) {
    /* check if event is for tested thread */
    if (thread != NULL) {
        jvmtiThreadInfo info;

        if (!NSK_JVMTI_VERIFY(jvmti->GetThreadInfo(thread, &info))) {
            nsk_jvmti_setFailStatus();
            return;
        }

        if (info.name != NULL && strcmp(info.name, THREAD_NAME) == 0) {
            NSK_DISPLAY2("  ... received THREAD_START event for tested thread: %p (%s)\n",
                                                            (void*)thread, info.name);
            eventsStart++;

            NSK_DISPLAY1("SetThreadLocalStorage() for current thread with pointer: %p\n",
                                                                (void*)initialStorage);
            if (!NSK_JVMTI_VERIFY(jvmti->SetThreadLocalStorage(NULL, (void*)initialStorage))) {
                nsk_jvmti_setFailStatus();
                return;
            }
        }
    }
}


/** THREAD_END callback. */
JNIEXPORT void JNICALL
callbackThreadEnd(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread) {
    /* check if event is for tested thread */
    if (thread != NULL) {
        jvmtiThreadInfo info;

        if (!NSK_JVMTI_VERIFY(jvmti->GetThreadInfo(thread, &info))) {
            nsk_jvmti_setFailStatus();
            return;
        }

        if (info.name != NULL && strcmp(info.name, THREAD_NAME) == 0) {
            NSK_DISPLAY2("  ... received THREAD_END event for tested thread: %p (%s)\n",
                                                            (void*)thread, info.name);
            eventsEnd++;

            /* get storage data */
            {
                StorageStructure* obtainedStorage = NULL;

                NSK_DISPLAY0("GetThreadLocalStorage() for current thread\n");
                if (!NSK_JVMTI_VERIFY(
                        jvmti->GetThreadLocalStorage(NULL, (void**)&obtainedStorage))) {
                    nsk_jvmti_setFailStatus();
                    return;
                }
                NSK_DISPLAY1("  ... got pointer: %p\n", (void*)obtainedStorage);

                NSK_DISPLAY0("Check storage data obtained for current thread\n");
                if (obtainedStorage != initialStorage) {
                    NSK_COMPLAIN3("Wrong storage pointer returned for current thread:\n"
                                  "#   thread:      %p\n"
                                  "#   got pointer: %p\n"
                                  "#   expected:    %p\n",
                                    (void*)thread,
                                    (void*)obtainedStorage, (void*)initialStorage);
                    nsk_jvmti_setFailStatus();
                } else {
                    int changed = 0;
                    int i;

                    for (i = 0; i < STORAGE_DATA_SIZE; i++) {
                        if (obtainedStorage->data[i] != STORAGE_DATA_CHAR) {
                            changed++;
                        }
                    }

                    if (changed > 0) {
                        NSK_COMPLAIN3("Data changed in returned storage for current thread:\n"
                                  "#   thread:        %p\n"
                                  "#   changed bytes: %d\n"
                                  "#   total bytes:   %d\n",
                                    (void*)thread,
                                    changed, STORAGE_DATA_SIZE);
                        nsk_jvmti_setFailStatus();
                    }
                }
            }
        }
    }
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setthrdstor003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setthrdstor003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setthrdstor003(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* set callbacks for thread events */
    {
        jvmtiEventCallbacks callbacks;
        memset(&callbacks, 0, sizeof(callbacks));
        callbacks.ThreadStart = callbackThreadStart;
        callbacks.ThreadEnd = callbackThreadEnd;
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;
    }

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
