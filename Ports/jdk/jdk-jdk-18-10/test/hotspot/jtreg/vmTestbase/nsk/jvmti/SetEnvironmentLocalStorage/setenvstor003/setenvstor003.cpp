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

#include <stdlib.h>
#include <string.h>
#include "jvmti.h"
#include "agent_common.h"
#include "jni_tools.h"
#include "jvmti_tools.h"

extern "C" {

/* ============================================================================= */

static jlong timeout = 0;

#define STATUS_FAIL     97

/* events */

#define EVENTS_COUNT    2
static jvmtiEvent events[EVENTS_COUNT] = {
    JVMTI_EVENT_VM_INIT,
    JVMTI_EVENT_VM_DEATH
};

/* storage data */

#define STORAGE_DATA_SIZE       1024
#define STORAGE_DATA_CHAR       'X'

typedef struct _StorageStructure {
    char data[STORAGE_DATA_SIZE];
} StorageStructure;

static StorageStructure storageData;
static StorageStructure* initialStorage = &storageData;

/* ============================================================================= */

/** Fill storage data with given char */
static void fillEnvStorage(StorageStructure* storage) {
    NSK_DISPLAY2("Fill storage data with char %c for size: %d bytes\n",
                (char)STORAGE_DATA_CHAR, (int)STORAGE_DATA_SIZE);
    memset(storage->data, STORAGE_DATA_CHAR, STORAGE_DATA_SIZE);
    NSK_DISPLAY0("  ... ok\n");
}

/**
 * Set JVMTI environment local storage with given pinter.
 * @returns NSK_FALSE if any error occured.
 */
static int setEnvStorage(jvmtiEnv* jvmti, StorageStructure* storage, const char where[]) {

    NSK_DISPLAY1("Set local storage for current JVMTI env: 0x%p\n", (void*)storage);
    if (!NSK_JVMTI_VERIFY(jvmti->SetEnvironmentLocalStorage(storage))) {
        return NSK_FALSE;
    }
    NSK_DISPLAY0("  ... ok\n");
    return NSK_TRUE;
}

/**
 * Check JVMTI environment local storage.
 * @returns NSK_FALSE if any error occured.
 */
static int checkEnvStorage(jvmtiEnv* jvmti, StorageStructure* initialStorage, const char where[]) {
    StorageStructure* storage = NULL;

    NSK_DISPLAY0("Get local storage for current JVMTI env\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetEnvironmentLocalStorage((void**)&storage))) {
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got storage: 0x%p\n", (void*)storage);

    if (storage != initialStorage) {
        NSK_COMPLAIN3("Wrong storage pointer returned for current JVMTI env in %s:\n"
                      "#   got pointer: %p\n"
                      "#   expected:    %p\n",
                        where,
                        (void*)storage, (void*)initialStorage);
        return NSK_FALSE;
    }

    {
        int changed = 0;
        int i;

        for (i = 0; i < STORAGE_DATA_SIZE; i++) {
            if (storage->data[i] != STORAGE_DATA_CHAR) {
                changed++;
            }
        }

        if (changed > 0) {
            NSK_COMPLAIN3("Data changed in returned storage for current JVMTI env in %s:\n"
                      "#   changed bytes: %d\n"
                      "#   total bytes:   %d\n",
                        where,
                        changed, STORAGE_DATA_SIZE);
            return NSK_FALSE;
        }
    }

    return NSK_TRUE;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    NSK_DISPLAY0("Wait for debugee to become ready\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    {
        NSK_DISPLAY0(">>> Testcase #0: Set env storage in agent thread()\n");
        if (!setEnvStorage(jvmti, initialStorage, "agent thread")) {
            nsk_jvmti_setFailStatus();
        }

        NSK_DISPLAY0("Let debugee to run\n");
        if (!nsk_jvmti_resumeSync())
            return;
        NSK_DISPLAY0("Wait for debugee to run some code\n");
        if (!nsk_jvmti_waitForSync(timeout))
            return;

        NSK_DISPLAY0(">>> Testcase #3: Check env storage in agent thread\n");
        if (!checkEnvStorage(jvmti, initialStorage, "agent thread")) {
            nsk_jvmti_setFailStatus();
        }
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ============================================================================= */

/**
 * Callback for VM_INIT event.
 */
JNIEXPORT void JNICALL
callbackVMInit(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread) {
}

/**
 * Callback for VM_DEATH event.
 */
JNIEXPORT void JNICALL
callbackVMDeath(jvmtiEnv* jvmti, JNIEnv* jni) {
    int success = NSK_TRUE;

    NSK_DISPLAY0(">>> Testcase #4: Check env storage in VM_DEATH callback\n");
    success = checkEnvStorage(jvmti, initialStorage, "VM_DEATH callback");

    NSK_DISPLAY1("Disable events: %d events\n", EVENTS_COUNT);
    if (!nsk_jvmti_enableEvents(JVMTI_DISABLE, EVENTS_COUNT, events, NULL)) {
        success = NSK_FALSE;
    } else {
        NSK_DISPLAY0("  ... disabled\n");
    }

    if (!success) {
        NSK_DISPLAY1("Exit with FAIL exit status: %d\n", STATUS_FAIL);
        NSK_BEFORE_TRACE(exit(STATUS_FAIL));
    }
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setenvstor003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setenvstor003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setenvstor003(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    {
        jvmtiEventCallbacks eventCallbacks;

        memset(&eventCallbacks, 0, sizeof(eventCallbacks));
        eventCallbacks.VMInit = callbackVMInit;
        eventCallbacks.VMDeath = callbackVMDeath;
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks)))) {
            return JNI_ERR;
        }

    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    NSK_DISPLAY1(">>> Prepare storage data at pointer: 0x%p\n", initialStorage);
    fillEnvStorage(initialStorage);

    NSK_DISPLAY1("Enable events: %d events\n", EVENTS_COUNT);
    if (nsk_jvmti_enableEvents(JVMTI_ENABLE, EVENTS_COUNT, events, NULL)) {
        NSK_DISPLAY0("  ... enabled\n");
    }

    return JNI_OK;
}

/* ============================================================================= */

}
