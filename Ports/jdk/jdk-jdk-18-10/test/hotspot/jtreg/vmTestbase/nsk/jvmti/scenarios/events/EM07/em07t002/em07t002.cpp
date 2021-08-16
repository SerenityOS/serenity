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
static jrawMonitorID syncLock = NULL;

static int methodLoadCount = 0;
static int methodUnloadCount = 0;

#define NAME_LENGTH 50
const void *plist = NULL;
static volatile int callbacksEnabled = NSK_TRUE;

typedef struct nsk_jvmti_CompiledMethodIDStruct {
    jmethodID method;
    const void* code_addr;
    char name[NAME_LENGTH];
} nsk_jvmti_CompiledMethod;


/* ============================================================================= */

/* callbacks */
void JNICALL
cbCompiledMethodLoad(jvmtiEnv *jvmti_env, jmethodID method, jint code_size,
                const void* code_addr, jint map_length,
                const jvmtiAddrLocationMap* map, const void* compile_info) {
    char *name;
    char *sign;
    char *genc;

    jvmti->RawMonitorEnter(syncLock);
    if (!callbacksEnabled) {
        jvmti->RawMonitorExit(syncLock);
        return;
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &name, &sign, &genc))) {
        nsk_jvmti_setFailStatus();
        jvmti->RawMonitorExit(syncLock);
        return;
    }

    if (!strncmp(name,"javaMethod", 8)) {
        nsk_jvmti_CompiledMethod *rec =
            (nsk_jvmti_CompiledMethod *)malloc(sizeof(nsk_jvmti_CompiledMethod));

        rec->method = method;
        rec->code_addr = code_addr;
        strncpy(rec->name, name, NAME_LENGTH);
        rec->name[NAME_LENGTH - 1] = '\0';

        if (!NSK_VERIFY(nsk_list_add(plist, rec))) {
            nsk_jvmti_setFailStatus();
            free((void *)rec);
        } else {
            NSK_DISPLAY0(">>>JVMTI_EVENT_COMPILED_METHOD_LOAD received for\n");
            NSK_DISPLAY1("\t\tmethod: %s\n", rec->name);

            if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(syncLock)))
                nsk_jvmti_setFailStatus();

            methodLoadCount++;

            if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(syncLock)))
                nsk_jvmti_setFailStatus();

        }
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)name))) {
        nsk_jvmti_setFailStatus();
    }
    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)sign))) {
        nsk_jvmti_setFailStatus();
    }
    if (genc != NULL)
        if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)genc))) {
            nsk_jvmti_setFailStatus();
        }

    jvmti->RawMonitorExit(syncLock);

}

void JNICALL
cbCompiledMethodUnload(jvmtiEnv *jvmti_env, jmethodID method,
                const void* code_addr) {

    nsk_jvmti_CompiledMethod *rec;

    jvmti->RawMonitorEnter(syncLock);
    if (!callbacksEnabled) {
        jvmti->RawMonitorExit(syncLock);
        return;
    }
    int count = nsk_list_getCount(plist);

    int i;

    for (i = 0; i < count; i ++) {
        rec = (nsk_jvmti_CompiledMethod *)nsk_list_get(plist, i);
        if ((rec->code_addr == code_addr) && (rec->method == method)) {
            NSK_DISPLAY0(">>>JVMTI_EVENT_COMPILED_METHOD_UNLOAD received for\n");
            NSK_DISPLAY1("\t\tmethod: %s\n", rec->name);

            methodUnloadCount++;

            free(rec);
            nsk_list_remove(plist, i);
            jvmti->RawMonitorExit(syncLock);
            return;
        }

    }
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

int checkEvents() {

    int result = methodUnloadCount <= methodLoadCount;

    if (result) {
        NSK_DISPLAY0("Received correct number of events:\n");
        NSK_DISPLAY1("\t\tCOMPILED_METHOD_LOAD events number = %d\n",
                                methodLoadCount);
        NSK_DISPLAY1("\t\tCOMPILED_METHOD_UNLOAD events number = %d\n",
                                methodUnloadCount);
    } else {
        NSK_COMPLAIN0("Received incorrect number of events:\n");
        NSK_COMPLAIN1("\t\tCOMPILED_METHOD_LOAD events number = %d\n",
                                methodLoadCount);
        NSK_COMPLAIN1("\t\tCOMPILED_METHOD_UNLOAD events number = %d\n",
                                methodUnloadCount);
    }

    return result;
}

/* ============================================================================= */

static int
setCallBacks() {

    jvmtiEventCallbacks eventCallbacks;

    memset(&eventCallbacks, 0, sizeof(eventCallbacks));

    eventCallbacks.CompiledMethodLoad        = cbCompiledMethodLoad;
    eventCallbacks.CompiledMethodUnload      = cbCompiledMethodUnload;

    return NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks)));
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* agentJNI, void* arg) {

    int i;

    int attempts = nsk_jvmti_findOptionIntValue("attempts", 1);

    for (i = 0; i < attempts; i++) {

        if (!nsk_jvmti_waitForSync(timeout))
            return;

        if (!checkEvents())
            nsk_jvmti_setFailStatus();

        NSK_DISPLAY0("Let debuggee to continue\n");
        if (!nsk_jvmti_resumeSync())
            return;
    }

    jvmti->RawMonitorEnter(syncLock);
    callbacksEnabled = NSK_FALSE;

    {
        int count = nsk_list_getCount(plist);

        while (count > 0) {
            free((void *)nsk_list_get(plist, 0));
            nsk_list_remove(plist, 0);
            count = nsk_list_getCount(plist);
        }

    }

    jvmti->RawMonitorExit(syncLock);

    if (!NSK_JVMTI_VERIFY(jvmti->DestroyRawMonitor(syncLock)))
        nsk_jvmti_setFailStatus();

}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_em07t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_em07t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_em07t002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    if (!NSK_VERIFY((jvmti = nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("_syncLock", &syncLock))) {
        nsk_jvmti_setFailStatus();
        return JNI_ERR;
    }

    if (!NSK_VERIFY((plist = (const void *)nsk_list_create()) != NULL))
        return JNI_ERR;

    {
        jvmtiCapabilities caps;
        memset(&caps, 0, sizeof(caps));

        caps.can_generate_compiled_method_load_events = 1;
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
            return JNI_ERR;
    }

    if (!setCallBacks()) {
        return JNI_ERR;
    }

    if (!enableEvent(JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_LOAD) ||
                !enableEvent(JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_UNLOAD)) {
        return JNI_ERR;
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */


}
