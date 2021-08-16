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
#include <string.h>
#include "jvmti.h"
#include "agent_common.h"

extern "C" {

#define JVMTI_ERROR_CHECK(str,res) if (res != JVMTI_ERROR_NONE) { printf(str); printf("%d\n",res); return res; }
#define JVMTI_ERROR_CHECK_EXPECTED_ERROR(str,res,err) if (res != err) { printf(str); printf("unexpected error %d\n",res); return res; }

#define JVMTI_ERROR_CHECK_VOID(str,res) if (res != JVMTI_ERROR_NONE) { printf(str); printf("%d\n",res); iGlobalStatus = 2; }

#define JVMTI_ERROR_CHECK_EXPECTED_ERROR_VOID(str,res,err) if (res != err) { printf(str); printf("unexpected error %d\n",res); iGlobalStatus = 2; }

#define THREADS_LIMIT 2000


jvmtiEnv *jvmti;
jint iGlobalStatus = 0;
jthread susp_thrd[THREADS_LIMIT];
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities jvmti_caps;
jrawMonitorID jraw_monitor[20];

int process_once = 0;



int printdump = 0;


void debug_printf(const char *fmt, ...) {
    va_list args;

    va_start(args, fmt);
    if (printdump) {
        vprintf(fmt, args);
    }
    va_end(args);
}


void JNICALL vmInit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread) {

    debug_printf("VMInit event  done\n");

}

void JNICALL vmExit(jvmtiEnv *jvmti_env, JNIEnv *env) {
    debug_printf("------------ JVMTI_EVENT_VM_DEATH ------------\n");
}

void JNICALL classFileLoadEvent(jvmtiEnv *jvmti_env, JNIEnv *env,
                        jclass class_being_redifined,
                        jobject loader, const char* name,
                        jobject protection_domain,
                        jint class_data_len,
                        const unsigned char* class_data,
                        jint* new_class_data_len,
                        unsigned char** new_class_data) {

}



void init_callbacks() {
    memset((void *)&callbacks, 0, sizeof(jvmtiEventCallbacks));
    callbacks.VMInit = vmInit;
    callbacks.VMDeath = vmExit;
    callbacks.ClassFileLoadHook = classFileLoadEvent;
}


#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_JvmtiTest(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_JvmtiTest(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_JvmtiTest(JavaVM *jvm, char *options, void *reserved) {
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

    return JNI_OK;
}


JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_functions_nosuspendStackTrace_JvmtiTest_GetResult(JNIEnv * env, jclass cls) {
    return iGlobalStatus;
}


JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_functions_nosuspendStackTrace_JvmtiTest_CreateRawMonitor(JNIEnv * env, jclass cls, jint i) {
    jvmtiError ret;
    char sz[128];

    sprintf(sz, "Rawmonitor-%d",i);
    debug_printf("jvmti create raw monitor \n");
    ret = jvmti->CreateRawMonitor(sz, &jraw_monitor[i]);

    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: ForceGarbageCollection %d \n", ret);
        iGlobalStatus = 2;
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_functions_nosuspendStackTrace_JvmtiTest_RawMonitorEnter(JNIEnv * env, jclass cls, jint i) {
    jvmtiError ret;

    debug_printf("jvmti Raw monitor enter \n");
    ret = jvmti->RawMonitorEnter(jraw_monitor[i]);

    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: Raw monitor enter %d \n", ret);
        iGlobalStatus = 2;
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_functions_nosuspendStackTrace_JvmtiTest_RawMonitorExit(JNIEnv * env, jclass cls, jint i) {
    jvmtiError ret;

    debug_printf("jvmti raw monitor exit \n");
    ret = jvmti->RawMonitorExit(jraw_monitor[i]);

    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: RawMonitorExit %d \n", ret);
        iGlobalStatus = 2;
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_functions_nosuspendStackTrace_JvmtiTest_RawMonitorWait(JNIEnv * env, jclass cls, jint i) {
    jvmtiError ret;

    debug_printf("jvmti RawMonitorWait \n");
    ret = jvmti->RawMonitorWait(jraw_monitor[i], -1);

    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: RawMonitorWait %d \n", ret);
        iGlobalStatus = 2;
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_functions_nosuspendStackTrace_JvmtiTest_RawMonitorNotify(JNIEnv * env, jclass cls, jint i) {
    jvmtiError ret;

    debug_printf("jvmti RawMonitorNotify \n");
    ret = jvmti->RawMonitorNotifyAll(jraw_monitor[i]);

    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: RawMonitorNotify %d \n", ret);
        iGlobalStatus = 2;
    }
}

JNIEXPORT int JNICALL
Java_nsk_jvmti_unit_functions_nosuspendStackTrace_JvmtiTest_GetFrameCount(JNIEnv * env, jclass cls, jobject thr) {
    jvmtiError ret;
    jint count;

    debug_printf("jvmti GetFrameCount \n");
    ret = jvmti->GetFrameCount((jthread) thr,  &count);
    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: GetFrameCount returned  %d \n", ret);
        iGlobalStatus = 2;
    }
    return count;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_functions_nosuspendStackTrace_JvmtiTest_GetStackTrace(JNIEnv * env, jclass cls, jobject thr) {
    jvmtiError ret;
    jvmtiFrameInfo *stack_buffer = NULL;
    jint count = 20;
    jclass klass;
    char *mname;
    char *signature;
    char *clname;
    char *generic;
    int i;


    debug_printf("jvmti GetStackTrace \n");

    ret = jvmti->Allocate(sizeof(jvmtiFrameInfo) * count, (unsigned char**)&stack_buffer);
    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: Allocate failed with  %d \n", ret);
        iGlobalStatus = 2;
    }


    ret = jvmti->GetStackTrace(thr, 0, count, stack_buffer, &count);

    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: GetStackTrace %d \n", ret);
        iGlobalStatus = 2;
    }

    debug_printf(" Java Stack trace ---\n");

    for (i = 0; i < count; i++) {
        ret = jvmti->GetMethodDeclaringClass(stack_buffer[i].method, &klass);
        if (ret != JVMTI_ERROR_NONE) {
            printf("Error: GetMethodDeclaringClass %d  \n", ret);
            iGlobalStatus = 2;
            return;
        }

        ret = jvmti->GetClassSignature(klass, &clname, &generic);
        if (ret != JVMTI_ERROR_NONE) {
            printf("Error: GetMethodDeclaringClass %d  \n", ret);
            iGlobalStatus = 2;
            return;
        }

        ret = jvmti->GetMethodName(stack_buffer[i].method, &mname, &signature, &generic);
        if (ret != JVMTI_ERROR_NONE) {
            printf("Error: GetMethodDeclaringClass %d  \n", ret);
            iGlobalStatus = 2;
            return;
        }

        debug_printf("[%d]  %s::%s(%s) at %lld \n",i,clname, mname, signature, stack_buffer[i].location);


    }


    ret = jvmti->Deallocate((unsigned char *) stack_buffer);
    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: Deallocate failed with  %d \n", ret);
        iGlobalStatus = 2;
    }


}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_functions_nosuspendStackTrace_JvmtiTest_SaveThreadInfo(JNIEnv * env, jclass cls, jobject oobj) {

}

}
