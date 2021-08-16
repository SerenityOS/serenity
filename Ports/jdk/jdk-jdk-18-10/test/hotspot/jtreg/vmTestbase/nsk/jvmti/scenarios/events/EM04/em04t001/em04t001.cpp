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
#include <stdlib.h>
#include "jvmti.h"
#include "agent_common.h"
#include "jni_tools.h"
#include "jvmti_tools.h"
#include "JVMTITools.h"
#include "nsk_list.h"

extern "C" {

/* ============================================================================= */

/* scaffold objects */
static jvmtiEnv *jvmti = NULL;
static jlong timeout = 0;
const void *plist = NULL;

#define NAME_LENGTH 50

typedef struct nsk_jvmti_DCG_paramsStruct {
    char name[NAME_LENGTH];
    const void *address;
    jint length;
    int sign;
} nsk_jvmti_DCG_params;

static jrawMonitorID syncLock = NULL;
static volatile int callbacksEnabled = NSK_TRUE;
/* ============================================================================= */

/* callbacks */
void JNICALL
cbDynamicCodeGenerated1(jvmtiEnv *jvmti_env, const char *name,
                            const void *address, jint length) {
    nsk_jvmti_DCG_params *rec;
    int b;

    jvmti->RawMonitorEnter(syncLock);
    if (!callbacksEnabled) {
        jvmti->RawMonitorExit(syncLock);
        return;
    }

    rec = (nsk_jvmti_DCG_params *)malloc(sizeof(nsk_jvmti_DCG_params));
    strncpy(rec->name, name, NAME_LENGTH);
    rec->name[NAME_LENGTH - 1] = '\0';
    rec->address = address;
    rec->length = length;
    rec->sign = 0;

    NSK_DISPLAY3("received: 0x%p %7d %s\n", rec->address, rec->length, rec->name);

    b = NSK_VERIFY(nsk_list_add(plist, rec));

    if (!b) {
        nsk_jvmti_setFailStatus();
        free((void *)rec);
    }
    jvmti->RawMonitorExit(syncLock);
}

void JNICALL
cbDynamicCodeGenerated2(jvmtiEnv *jvmti_env, const char *name,
                            const void *address, jint length) {

    int i;
    nsk_jvmti_DCG_params *rec;

    jvmti->RawMonitorEnter(syncLock);
    if (!callbacksEnabled) {
        jvmti->RawMonitorExit(syncLock);
        return;
    }

    int count = nsk_list_getCount(plist);
    int compLength = NAME_LENGTH - 1;

    for (i = 0; i < count; i ++) {
        rec = (nsk_jvmti_DCG_params *)nsk_list_get(plist, i);
        if ((rec->address == address) && (rec->length == length)) {
            rec->sign = 1;
            NSK_DISPLAY3("checked: 0x%p %7d %s\n", rec->address, rec->length,
                                rec->name);
            if (strncmp(rec->name, name, compLength) != 0) {
                NSK_DISPLAY2("\t<%s> was renamed to <%s>\n", rec->name, name);
            }
            jvmti->RawMonitorExit(syncLock);
            return;
        }

    }
    NSK_DISPLAY3("NOT FOUND: 0x%p %7d %s\n", address, length, name);
    jvmti->RawMonitorExit(syncLock);

}

/* ============================================================================= */

static int
enableEvent(jvmtiEventMode enable, jvmtiEvent event) {
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(enable, event, NULL))) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }

    return NSK_TRUE;
}

int setCallBacks(int stage) {

    jvmtiEventCallbacks eventCallbacks;
    memset(&eventCallbacks, 0, sizeof(eventCallbacks));

    eventCallbacks.DynamicCodeGenerated = (stage == 1) ?
                            cbDynamicCodeGenerated1 : cbDynamicCodeGenerated2;

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

    /* stage 2 */
    if (!setCallBacks(2)) {
        return;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->GenerateEvents(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)))
        nsk_jvmti_setFailStatus();

    jvmti->RawMonitorEnter(syncLock);
    callbacksEnabled = NSK_FALSE;

    {
        int i;
        const nsk_jvmti_DCG_params *rec;
        int count = nsk_list_getCount(plist);

        for (i = 0; i < count; i++) {
            rec = (const nsk_jvmti_DCG_params *)nsk_list_get(plist, 0);
            if (!rec->sign) {
                NSK_COMPLAIN3("missed event for\n\t0x%p %7d %s\n", rec->address, rec->length, rec->name);
                nsk_jvmti_setFailStatus();
            }
            free((void *)rec);
            nsk_list_remove(plist, 0);
        }

    }

    jvmti->RawMonitorExit(syncLock);

    NSK_DISPLAY0("Let debuggee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;

}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_em04t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_em04t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_em04t001(JavaVM *jvm, char *options, void *reserved) {
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

    plist = (const void *)nsk_list_create();
    if (!NSK_VERIFY(plist != NULL))
        return JNI_ERR;

    NSK_DISPLAY1("plist = 0x%p\n", plist);

    if (!setCallBacks(1)) {
        return JNI_ERR;
    }

    if (!enableEvent(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT)
            || !enableEvent(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH)
            || !enableEvent(JVMTI_ENABLE, JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
        return JNI_ERR;
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

JNIEXPORT void JNICALL
#ifdef STATIC_BUILD
Agent_OnUnload_em04t001(JavaVM *jvm)
#else
Agent_OnUnload(JavaVM *jvm)
#endif
{

    if (!NSK_VERIFY(nsk_list_destroy(plist))) {
        nsk_jvmti_setFailStatus();
    }

    if (!NSK_JVMTI_VERIFY(jvmti->DestroyRawMonitor(syncLock))) {
        nsk_jvmti_setFailStatus();
    }
}

}
