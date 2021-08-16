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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <jvmti.h>
#include "agent_common.h"

#include "nsk_tools.h"
#include "JVMTITools.h"
#include "jvmti_tools.h"

extern "C" {

#define STATUS_FAILED 2
#define PASSED 0

#define MEM_SIZE 1024

static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;

static volatile jint result = PASSED;
static volatile int gcstart = 0;
unsigned char *mem;

static void rawMonitorFunc(jvmtiEnv *jvmti_env, const char *msg) {
    jrawMonitorID _lock;

    NSK_DISPLAY1("%s: creating a raw monitor ...\n",
        msg);
    if (!NSK_JVMTI_VERIFY(jvmti_env->CreateRawMonitor("_lock", &_lock))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: %s: unable to create a raw monitor\n\n",
            msg);
        return;
    }
    NSK_DISPLAY1("CHECK PASSED: %s: raw monitor created\n",
        msg);

    NSK_DISPLAY1("%s: entering the raw monitor ...\n",
        msg);
    if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorEnter(_lock))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: %s: unable to enter the raw monitor\n\n",
            msg);
        return;
    }
    else {
        NSK_DISPLAY1("CHECK PASSED: %s: the raw monitor entered\n",
            msg);

        NSK_DISPLAY1("%s: waiting the raw monitor ...\n",
            msg);
        if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorWait(_lock, (jlong)10))) {
            result = STATUS_FAILED;
            NSK_COMPLAIN1("TEST FAILED: %s: unable to wait the raw monitor\n\n",
                msg);
        }
        NSK_DISPLAY1("CHECK PASSED: %s: the raw monitor waited\n",
            msg);


        NSK_DISPLAY1("%s: notifying a single thread waiting on the raw monitor ...\n",
            msg);
        if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorNotify(_lock))) {
            result = STATUS_FAILED;
            NSK_COMPLAIN1("TEST FAILED: %s: unable to notify single thread\n\n",
                msg);
        }
        NSK_DISPLAY1("CHECK PASSED: %s: single thread notified\n",
            msg);


        NSK_DISPLAY1("%s: notifying all threads waiting on the raw monitor ...\n",
            msg);
        if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorNotifyAll(_lock))) {
            result = STATUS_FAILED;
            NSK_COMPLAIN1("TEST FAILED: %s: unable to notify all threads\n\n",
                msg);
        }
        NSK_DISPLAY1("CHECK PASSED: %s: all threads notified\n",
            msg);


        NSK_DISPLAY1("%s: exiting the raw monitor ...\n",
            msg);
        if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorExit(_lock))) {
            result = STATUS_FAILED;
            NSK_COMPLAIN1("TEST FAILED: %s: unable to exit the raw monitor\n\n",
                msg);
        }
        NSK_DISPLAY1("CHECK PASSED: %s: the raw monitor exited\n",
            msg);
    }

    NSK_DISPLAY1("%s: destroying the raw monitor ...\n",
        msg);
    if (!NSK_JVMTI_VERIFY(jvmti_env->DestroyRawMonitor(_lock))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: %s: unable to destroy a raw monitor\n",
            msg);
        return;
    }
    NSK_DISPLAY1("CHECK PASSED: %s: the raw monitor destroyed\n",
        msg);
}

static void memoryFunc(jvmtiEnv *jvmti_env, const char *msg) {
    NSK_DISPLAY1("%s: allocating memory ...\n",
        msg);
    if (!NSK_JVMTI_VERIFY(jvmti_env->Allocate(MEM_SIZE, &mem))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: %s: unable to allocate memory\n\n",
            msg);
        return;
    }
    else
        NSK_DISPLAY1("CHECK PASSED: %s: memory has been allocated successfully\n",
            msg);

    NSK_DISPLAY1("%s: deallocating memory ...\n",
        msg);
    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate(mem))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: %s: unable to deallocate memory\n\n",
            msg);
    }
    else
        NSK_DISPLAY1("CHECK PASSED: %s: memory has been deallocated successfully\n\n",
            msg);
}

/** callback functions **/
void JNICALL
GarbageCollectionStart(jvmtiEnv *jvmti_env) {
    gcstart++;
    NSK_DISPLAY1(">>>> GarbageCollectionStart event #%d received\n",
        gcstart);

    rawMonitorFunc(jvmti_env, "GarbageCollectionStart");

    memoryFunc(jvmti_env, "GarbageCollectionStart");

    NSK_DISPLAY0("<<<<\n\n");
}

void JNICALL
VMDeath(jvmtiEnv *jvmti_env, JNIEnv *env) {
    NSK_DISPLAY0("VMDeath event received\n");

    if (result == STATUS_FAILED)
        exit(95 + STATUS_FAILED);
}

/************************/

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_gcstart002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_gcstart002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_gcstart002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* add capability to generate compiled method events */
    memset(&caps, 0, sizeof(jvmtiCapabilities));
    caps.can_generate_garbage_collection_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    if (!caps.can_generate_garbage_collection_events)
        NSK_DISPLAY0("Warning: generation of garbage collection events is not implemented\n");

    /* set event callback */
    NSK_DISPLAY0("setting event callbacks ...\n");
    (void) memset(&callbacks, 0, sizeof(callbacks));
    callbacks.VMDeath = &VMDeath;
    callbacks.GarbageCollectionStart = &GarbageCollectionStart;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    NSK_DISPLAY0("setting event callbacks done\nenabling JVMTI events ...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_GARBAGE_COLLECTION_START, NULL)))
        return JNI_ERR;
    NSK_DISPLAY0("enabling the events done\n\n");

    return JNI_OK;
}

}
