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


#define JVMTI_ERROR_CHECK(str,res) if (res != JVMTI_ERROR_NONE) { printf(str); printf(" %d\n",res); return res; }
#define JVMTI_ERROR_CHECK_EXPECTED_ERROR(str,res,err) if (res != err) { printf(str); printf("unexpected error %d\n",res); return res; }

#define JVMTI_ERROR_CHECK_VOID(str,res) if (res != JVMTI_ERROR_NONE) { printf(str); printf(" %d\n",res); iGlobalStatus = 2; }

#define JVMTI_ERROR_CHECK_EXPECTED_ERROR_VOID(str,res,err) if (res != err) { printf(str); printf(" unexpected error %d\n",res); iGlobalStatus = 2; }



jvmtiEnv *jvmti;
jint iGlobalStatus = 0;
static jvmtiCapabilities jvmti_caps;
static jvmtiEventCallbacks callbacks;


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
    jvmtiError res;
    debug_printf("VMInit event received\n");
    res = jvmti_env->DisposeEnvironment();
    JVMTI_ERROR_CHECK_VOID("DisposeEnvironment returned error", res);
}


void init_callbacks() {

    callbacks.VMInit = vmInit;

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
    jint count;
    char **properties;
    int i;

    if (options && strlen(options) > 0) {
        if (strstr(options, "printdump")) {
            printdump = 1;
        }
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res < 0) {
        printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    /* Enable event call backs. */
    init_callbacks();
    res = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    JVMTI_ERROR_CHECK("SetEventCallbacks returned error", res);

    /* Add capabilities */
    res = jvmti->GetPotentialCapabilities(&jvmti_caps);
    JVMTI_ERROR_CHECK("GetPotentialCapabilities returned error", res);

    res = jvmti->AddCapabilities(&jvmti_caps);
    JVMTI_ERROR_CHECK("GetAddCapabilities returned error", res);


    res = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);
    JVMTI_ERROR_CHECK("SetEventNotificationMode for VM_INIT returned error", res);

    res = jvmti->GetSystemProperties(&count, &properties);

    JVMTI_ERROR_CHECK("GetSystemProperties returned error", res);

    for (i=0; i< count; i++) {
        char *value;

        res = jvmti->GetSystemProperty((const char *) properties[i], &value);
        JVMTI_ERROR_CHECK("GetSystemProperty returned error", res);
        debug_printf(" %s    %s \n", properties[i], value);

        res = jvmti->SetSystemProperty((const char *) properties[i], value);
        debug_printf("SetSystemProperty returned error %d\n", res);
     }

    return JNI_OK;
}


JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_functions_Dispose_JvmtiTest_GetResult(JNIEnv * env, jclass cls) {
    return iGlobalStatus;
}


}
