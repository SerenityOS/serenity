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
/*

 Unit test to test the following:

 Onload phase:

 1.  CreateRawMonitor
 2.  RawMonitorEnter
 3.  RawMonitorExit
 4.  DestroyRawMonitor
 5.  Recursive RawMonitorEnter and DestroyRawMonitor
 6.  RawMonitorExit for not owned monitor in onload phase.
 7.  RawMonitorExit for not owned monitor in live phase.

 Mixed phase:

 1. Onload RawMonitorEnter and live phase RawMonitorExit
 2. Onload RawMonitorEnter and start phase RawMonitorExit
 3. Start phase RawMonitorEnter and RawMonitorExit.
 4. Onload RawmonitorEnter and start phase Destroy

 */

#include <stdio.h>
#include <string.h>
#include "jvmti.h"
#include "agent_common.h"

extern "C" {


#define JVMTI_ERROR_CHECK(str,res) if (res != JVMTI_ERROR_NONE) { printf(str); printf(" %d\n",res); return res; }
#define JVMTI_ERROR_CHECK_EXPECTED_ERROR(str,res,err) if (res != err) { printf(str); printf(" unexpected error %d\n",res); return res; }

#define JVMTI_ERROR_CHECK_VOID(str,res) if (res != JVMTI_ERROR_NONE) { printf(str); printf(" %d\n",res); iGlobalStatus = 2; }

#define JVMTI_ERROR_CHECK_EXPECTED_ERROR_VOID(str,res,err) if (res != err) { printf(str); printf(" unexpected error %d\n",res); iGlobalStatus = 2; }

#define THREADS_LIMIT 8

jrawMonitorID access_lock;
jrawMonitorID access_lock_not_entered;
jvmtiEnv *jvmti;
jint iGlobalStatus = 0;
jthread main_thread;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities jvmti_caps;
jrawMonitorID jraw_monitor[20];

static volatile int process_once = 1;



int printdump = 0;


void debug_printf(const char *fmt, ...) {
    va_list args;

    va_start(args, fmt);
    if (printdump) {
        vprintf(fmt, args);
    }
    va_end(args);
}


void JNICALL vmStart(jvmtiEnv *jvmti_env, JNIEnv *env) {
    jvmtiError res;
    res = jvmti_env->GetCurrentThread(&main_thread);
    JVMTI_ERROR_CHECK_VOID(" JVMTI GetCurrentThread returned error", res);
    main_thread = (jthread)env->NewGlobalRef(main_thread);
}

void JNICALL vmInit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread) {

    jvmtiError res;

    debug_printf("VMInit event  done\n");
    res = jvmti_env->RawMonitorExit(access_lock);
    JVMTI_ERROR_CHECK_VOID(" Raw monitor exit returned error", res);
    res = jvmti_env->RawMonitorExit(access_lock);
    JVMTI_ERROR_CHECK_VOID(" Raw monitor exit returned error", res);
}

void JNICALL vmExit(jvmtiEnv *jvmti_env, JNIEnv *env) {
    debug_printf("------------ JVMTI_EVENT_VM_DEATH ------------\n");
}

void JNICALL classFileLoadHookEvent(jvmtiEnv *jvmti_env, JNIEnv *env,
                        jclass class_being_redifined,
                        jobject loader, const char* name,
                        jobject protection_domain,
                        jint class_data_len,
                        const unsigned char* class_data,
                        jint* new_class_data_len,
                        unsigned char** new_class_data) {

    jvmtiError res;
    jvmtiPhase phase;
    jthread    thread;

    res = jvmti_env->GetPhase(&phase);
    JVMTI_ERROR_CHECK_VOID(" JVMTI GetPhase returned error", res);
    if (phase != JVMTI_PHASE_START) {
        return; /* only the start phase is tested */
    }
    res = jvmti_env->GetCurrentThread(&thread);
    JVMTI_ERROR_CHECK_VOID(" JVMTI GetCurrentThread returned error", res);
    if (!env->IsSameObject(thread, main_thread)) {
        return; /* only the main thread is tested */
    }

    debug_printf("------------ classFileLoadHookEvent ------------\n");

    /* Test raw monitor in start phase */

    if (process_once) {

        process_once = 0;

            /* test not entered raw monitor */
        res = jvmti_env->RawMonitorExit(access_lock_not_entered);
        JVMTI_ERROR_CHECK_EXPECTED_ERROR_VOID("Raw monitor exit returned error", res,JVMTI_ERROR_NOT_MONITOR_OWNER);

            /* release lock in start phase */
        res = jvmti_env->RawMonitorExit(access_lock);
        JVMTI_ERROR_CHECK_VOID("Raw monitor exit returned error", res);

            /* release lock in start phase */
        res = jvmti_env->RawMonitorExit(access_lock);
        JVMTI_ERROR_CHECK_VOID("Raw monitor exit returned error", res);

        res = jvmti_env->RawMonitorEnter(access_lock);
        JVMTI_ERROR_CHECK_VOID("Raw monitor enter returned error", res);

        res = jvmti_env->RawMonitorEnter(access_lock);
        JVMTI_ERROR_CHECK_VOID("Raw monitor enter returned error", res);
    }

}



void init_callbacks() {
    memset((void *)&callbacks, 0, sizeof(jvmtiEventCallbacks));
    callbacks.VMStart = vmStart;
    callbacks.VMInit = vmInit;
    callbacks.VMDeath = vmExit;
    callbacks.ClassFileLoadHook = classFileLoadHookEvent;
}


#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_rawmonitor(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_rawmonitor(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_rawmonitor(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM * jvm, char *options, void *reserved) {
    jint res;

    if (options && strlen(options) > 0) {
        if (strstr(options, "printdump")) {
            printdump = 1;
        }
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res < 0) {
        debug_printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    /* Onload phase Create data access lock */
    res = jvmti->CreateRawMonitor("_access_lock", &access_lock);
    JVMTI_ERROR_CHECK("CreateRawMonitor failed with error code ", res);
    res = jvmti->CreateRawMonitor("_access_lock_not_entered", &access_lock_not_entered);
    JVMTI_ERROR_CHECK("CreateRawMonitor failed with error code ", res);
    /* Create this raw monitor in onload and it is used in live phase */
    res = jvmti->CreateRawMonitor("RawMonitor-0", &jraw_monitor[0]);
    JVMTI_ERROR_CHECK("CreateRawMonitor failed with error code ", res);


    /* Add capabilities */
    res = jvmti->GetPotentialCapabilities(&jvmti_caps);
    JVMTI_ERROR_CHECK("GetPotentialCapabilities returned error", res);

    res = jvmti->AddCapabilities(&jvmti_caps);
    JVMTI_ERROR_CHECK("GetPotentialCapabilities returned error", res);

    /* Enable events */
    init_callbacks();
    res = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    JVMTI_ERROR_CHECK("SetEventCallbacks returned error", res);

    res = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);
    JVMTI_ERROR_CHECK("SetEventNotificationMode for VM_INIT returned error", res);

    res = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL);
    JVMTI_ERROR_CHECK("SetEventNotificationMode for vm death event returned error", res);

    res = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
    JVMTI_ERROR_CHECK("SetEventNotificationMode CLASS_FILE_LOAD_HOOK returned error", res);

     /* acquire lock in onload */
    res = jvmti->RawMonitorEnter(access_lock);
    JVMTI_ERROR_CHECK("Raw monitor enter returned error", res);

    /* release lock in onload */
    res = jvmti->RawMonitorExit(access_lock);
    JVMTI_ERROR_CHECK("Raw monitor exit returned error", res);

    /* test not entered raw monitor */
    res = jvmti->RawMonitorExit(access_lock_not_entered);
    JVMTI_ERROR_CHECK_EXPECTED_ERROR("Raw monitor exit returned error", res,JVMTI_ERROR_NOT_MONITOR_OWNER);

    /* acquire lock in onload */
    res = jvmti->RawMonitorEnter(access_lock);
    JVMTI_ERROR_CHECK("Raw monitor enter returned error", res);

    res = jvmti->RawMonitorEnter(access_lock);
    JVMTI_ERROR_CHECK("Raw monitor enter returned error", res);

    res = jvmti->RawMonitorEnter(access_lock);
    JVMTI_ERROR_CHECK("Raw monitor enter returned error", res);

    /* test Destroy raw monitor in onload phase */
    res = jvmti->DestroyRawMonitor(access_lock);
    JVMTI_ERROR_CHECK("Destroy Raw monitor returned error", res);

    /* Create data access lock  in onload and enter in onload phase */
    res = jvmti->CreateRawMonitor("_access_lock", &access_lock);
    JVMTI_ERROR_CHECK("CreateRawMonitor failed with error code ", res);

    res = jvmti->RawMonitorEnter(access_lock);
    JVMTI_ERROR_CHECK("Raw monitor enter returned error", res);

    res = jvmti->RawMonitorEnter(access_lock);
    JVMTI_ERROR_CHECK("Raw monitor enter returned error", res);


    /* This monitor is entered here and it is released in live phase by a call from java code
     */
    res = jvmti->RawMonitorEnter(jraw_monitor[0]);
    JVMTI_ERROR_CHECK("Raw monitor enter returned error", res);
    res = jvmti->RawMonitorEnter(jraw_monitor[0]);
    JVMTI_ERROR_CHECK("Raw monitor enter returned error", res);
    res = jvmti->RawMonitorExit(jraw_monitor[0]);
    JVMTI_ERROR_CHECK("Raw monitor exit returned error", res);

    return JNI_OK;
}


JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_functions_rawmonitor_GetResult(JNIEnv * env, jclass cls) {
    return iGlobalStatus;
}


JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_functions_rawmonitor_CreateRawMonitor(JNIEnv * env, jclass klass, jint i) {
    jvmtiError ret;
    char sz[128];

    sprintf(sz, "Rawmonitor-%d",i);
    debug_printf("jvmti create raw monitor \n");
    ret = jvmti->CreateRawMonitor(sz, &jraw_monitor[i]);

    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: CreateRawMonitor %d \n", ret);
        iGlobalStatus = 2;
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_functions_rawmonitor_RawMonitorEnter(JNIEnv * env, jclass cls, jint i) {
    jvmtiError ret;

    debug_printf("jvmti Raw monitor enter \n");
    ret = jvmti->RawMonitorEnter(jraw_monitor[i]);

    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: RawMonitorEnter %d \n", ret);
        iGlobalStatus = 2;
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_functions_rawmonitor_RawMonitorExit(JNIEnv * env, jclass cls, jint i) {
    jvmtiError ret;

    debug_printf("jvmti raw monitor exit \n");
    ret = jvmti->RawMonitorExit(jraw_monitor[i]);

    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: RawMonitorExit %d \n", ret);
        iGlobalStatus = 2;
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_functions_rawmonitor_RawMonitorWait(JNIEnv * env, jclass cls, jint i) {
    jvmtiError ret;

    debug_printf("jvmti RawMonitorWait \n");
    ret = jvmti->RawMonitorWait(jraw_monitor[i], -1);

    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: RawMonitorWait %d \n", ret);
        iGlobalStatus = 2;
    }
}

}
