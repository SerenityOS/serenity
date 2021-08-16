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

#include <stdlib.h>
#include <string.h>
#include "jvmti.h"
#include "agent_common.h"
#include "jni_tools.h"
#include "jvmti_tools.h"

extern "C" {


/* ============================================================================= */

static jlong timeout = 0;
static jvmtiEnv *jvmti_2;

#define STATUS_FAIL     97
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
 * Check JVMTI environment local storage.
 * @returns NSK_FALSE if any error occured.
 */
static int checkEnvStorage(jvmtiEnv* jvmti, const char where[]) {
    void* storage = NULL;

    NSK_DISPLAY0("Calling GetEnvironmentLocalStorage():");
    if (!NSK_JVMTI_VERIFY(jvmti->GetEnvironmentLocalStorage(&storage))) {
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got storage: 0x%p\n", (void*)storage);

    if (storage != NULL) {
        NSK_COMPLAIN2("GetEnvironmentLocalStorage() returned NOT NULL storage in %s:\n"
                      "#   storage pointer: 0x%p\n",
                        where, (void*)storage);
        return NSK_FALSE;
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

    NSK_DISPLAY0("CASE #3: Check local storage in agent thread for second JVMTI env.\n");
    if (!checkEnvStorage(jvmti, "agent thread")) {
        nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;
}

jthread getEnvThread(JNIEnv *env) {
    jclass thrClass;
    jmethodID cid;
    jthread res;

    thrClass = env->FindClass("java/lang/Thread");
    cid = env->GetMethodID(thrClass, "<init>", "()V");
    res = env->NewObject(thrClass, cid);
    return res;
}

/* ============================================================================= */

/**
 * Callback for VM_INIT event.
 */
JNIEXPORT void JNICALL
callbackVMInit(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread) {

    NSK_DISPLAY0("CASE #2: Check local storage in VM_INIT callback for second JVMTI env.\n");
    if (!checkEnvStorage(jvmti, "VM_INIT callback")) {
        nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY0("Set agentProc for second JVMTI env.\n");
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        nsk_jvmti_setFailStatus();
}

/**
 * Callback for VM_DEATH event.
 */
JNIEXPORT void JNICALL
callbackVMDeath(jvmtiEnv* jvmti, JNIEnv* jni) {
    int success = NSK_TRUE;

    NSK_DISPLAY0("CASE #4: Check local storage in VM_DEATH callback for second JVMTI env.\n");
    success = checkEnvStorage(jvmti, "VM_DEATH callback");

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
JNIEXPORT jint JNICALL Agent_OnLoad_gf06t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_gf06t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_gf06t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiEnv* jvmti_1 = NULL;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    NSK_DISPLAY0("Create first JVMTI env.\n");
    res = jvm->GetEnv((void **) &jvmti_1, JVMTI_VERSION_1_1);
    if (res < 0) {
        NSK_COMPLAIN0("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    NSK_DISPLAY1("Set local storage in JVM_OnLoad() for first JVMTI env: 0x%p\n", (void*)initialStorage);
    if (!NSK_JVMTI_VERIFY(jvmti_1->SetEnvironmentLocalStorage(initialStorage))) {
        return JNI_ERR;
    }
    NSK_DISPLAY0("  ... ok\n");

    /* Create second environment */
    if (!NSK_VERIFY((jvmti_2 =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* Set callbacks for second environment */
    {
        jvmtiEventCallbacks eventCallbacks;

        memset(&eventCallbacks, 0, sizeof(eventCallbacks));
        eventCallbacks.VMInit = callbackVMInit;
        eventCallbacks.VMDeath = callbackVMDeath;
        if (!NSK_JVMTI_VERIFY(
                jvmti_2->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks)))) {
            return JNI_ERR;
        }

    }


    NSK_DISPLAY1("Prepare storage data at pointer: 0x%p\n", initialStorage);
    fillEnvStorage(initialStorage);

    NSK_DISPLAY0("CASE #1: Check local storage in JVM_OnLoad() for second JVMTI env.\n");
    if (!checkEnvStorage(jvmti_2, "JVM_OnLoad()")) {
        nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY1("Enable events: %d events\n", EVENTS_COUNT);
    if (nsk_jvmti_enableEvents(JVMTI_ENABLE, EVENTS_COUNT, events, NULL)) {
        NSK_DISPLAY0("  ... enabled\n");
    }

    return JNI_OK;
}

/* ============================================================================= */

}
