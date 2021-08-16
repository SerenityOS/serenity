/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static int eventsCount = 0;
static int eventsExpected = 0;
static const char *exp_csig = "Lnsk/jvmti/SetBreakpoint/setbrk008;";
static const char *exp_name = "checkPoint";
static const char *exp_sig = "(I)V";


void JNICALL Breakpoint(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method, jlocation loc) {
    jvmtiError err;
    jclass cls;
    char *cls_sig, *name, *sig, *generic;

    eventsCount++;
    if (printdump == JNI_TRUE && eventsCount == 1) {
        printf(">>> retrieving bp event info ...\n");
    }

    err = jvmti_env->GetMethodDeclaringClass(method, &cls);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodDeclaringClass#%d) unexpected error: %s (%d)\n",
               eventsCount, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti_env->GetClassSignature(cls, &cls_sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassSignature#%d) unexpected error: %s (%d)\n",
               eventsCount, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti->GetMethodName(method, &name, &sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodName#%d) unexpected error: %s (%d)\n",
               eventsCount, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    if (printdump == JNI_TRUE && eventsCount == 1) {
        printf(">>>      class: \"%s\"\n", cls_sig);
        printf(">>>     method: \"%s%s\"\n", name, sig);
        printf(">>>   location: 0x%x%08x\n", (jint)(loc >> 32), (jint)loc);
    }
    if (name == NULL || strcmp(cls_sig, exp_csig) != 0) {
        printf("(bp#%d) wrong class: \"%s\"", eventsCount, cls_sig);
        printf(", expected: \"%s\"\n", exp_csig);
        result = STATUS_FAILED;
    }
    if (name == NULL || strcmp(name, exp_name) != 0) {
        printf("(bp#%d) wrong method name: \"%s\"", eventsCount, name);
        printf(", expected: \"%s\"\n", exp_name);
        result = STATUS_FAILED;
    }
    if (sig == NULL || strcmp(sig, exp_sig) != 0) {
        printf("(bp#%d) wrong method sig: \"%s\"", eventsCount, sig);
        printf(", expected: \"%s\"\n", exp_sig);
        result = STATUS_FAILED;
    }
    if (loc != 0) {
        printf("(bp#%d) wrong location: 0x%x%08x expected: 0x0\n",
               eventsCount, (jint)(loc >> 32), (jint)loc);
        result = STATUS_FAILED;
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setbrk008(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setbrk008(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setbrk008(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    err = jvmti->GetPotentialCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetPotentialCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->AddCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(AddCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->GetCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    if (caps.can_generate_breakpoint_events) {
        callbacks.Breakpoint = &Breakpoint;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: Breakpoint is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_SetBreakpoint_setbrk008_getReady(JNIEnv *env,
        jclass cls, jint n) {
    jvmtiError err;
    jmethodID mid;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (!caps.can_generate_breakpoint_events) {
        return;
    }

    mid = env->GetStaticMethodID(cls, exp_name, exp_sig);
    if (mid == NULL) {
        printf("Cannot find Method ID for method checkPoint\n");
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> setting breakpoint ...\n");
    }
    err = jvmti->SetBreakpoint(mid, 0);
    if (err == JVMTI_ERROR_NONE) {
        eventsExpected = n;
    } else {
        printf("(SetBreakpoint) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_BREAKPOINT, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable BREAKPOINT event: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_SetBreakpoint_setbrk008_check(JNIEnv *env, jclass cls) {
    if (printdump == JNI_TRUE) {
        printf(">>> hitted %d breakpoints\n", eventsCount);
    }
    if (eventsCount != eventsExpected) {
        printf("Wrong number of breakpoint events: %d, expected: %d\n",
            eventsCount, eventsExpected);
        result = STATUS_FAILED;
    }
    return result;
}

}
