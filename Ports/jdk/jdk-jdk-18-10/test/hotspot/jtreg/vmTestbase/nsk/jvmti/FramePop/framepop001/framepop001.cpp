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
#include <inttypes.h>
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

typedef struct {
    const char *cls_sig;
    const char *name;
    const char *sig;
    jlocation loc;
} pop_info;

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static size_t eventsExpected = 0;
static size_t eventsCount = 0;
static pop_info pops[] = {
    { "Lnsk/jvmti/FramePop/framepop001;", "chain", "()V", 0 },
    { "Lnsk/jvmti/FramePop/framepop001a;", "dummy", "()V", 3 },
};

void JNICALL Breakpoint(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method, jlocation location) {
    jvmtiError err;

    err = jvmti_env->NotifyFramePop(thr, 0);
    if (err == JVMTI_ERROR_NONE) {
        eventsExpected++;
    } else {
        printf("(NotifyFramePop#0) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti_env->NotifyFramePop(thr, 1);
    if (err == JVMTI_ERROR_NONE) {
        eventsExpected++;
    } else {
        printf("(NotifyFramePop#1) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

void JNICALL FramePop(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method, jboolean wasPopedByException) {
    jvmtiError err;
    char *cls_sig, *name, *sig, *generic;
    jclass cls;
    jmethodID mid;
    jlocation loc;

    if (printdump == JNI_TRUE) {
        printf(">>> retrieving frame pop info ...\n");
    }
    err = jvmti_env->GetMethodDeclaringClass(method, &cls);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodDeclaringClass) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti_env->GetClassSignature(cls, &cls_sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassSignature) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti_env->GetMethodName(method, &name, &sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodName) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti_env->GetFrameLocation(thr, 0, &mid, &loc);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetFrameLocation) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    if (printdump == JNI_TRUE) {
        printf(">>>      class: \"%s\"\n", cls_sig);
        printf(">>>     method: \"%s%s\"\n", name, sig);
        printf(">>>   location: 0x%x%08x\n",
            (jint)(loc >> 32), (jint)loc);
        printf(">>> ... done\n");
    }
    if (eventsCount < sizeof(pops)/sizeof(pop_info)) {
        if (cls_sig == NULL ||
                strcmp(cls_sig, pops[eventsCount].cls_sig) != 0) {
            printf("(pop#%" PRIuPTR ") wrong class: \"%s\"",
                   eventsCount, cls_sig);
            printf(", expected: \"%s\"\n", pops[eventsCount].cls_sig);
            result = STATUS_FAILED;
        }
        if (name == NULL ||
                strcmp(name, pops[eventsCount].name) != 0) {
            printf("(pop#%" PRIuPTR ") wrong method name: \"%s\"",
                   eventsCount, name);
            printf(", expected: \"%s\"\n", pops[eventsCount].name);
            result = STATUS_FAILED;
        }
        if (sig == NULL ||
                strcmp(sig, pops[eventsCount].sig) != 0) {
            printf("(pop#%" PRIuPTR ") wrong method sig: \"%s\"",
                   eventsCount, sig);
            printf(", expected: \"%s\"\n", pops[eventsCount].sig);
            result = STATUS_FAILED;
        }
        if (loc != pops[eventsCount].loc) {
            printf("(pop#%" PRIuPTR ") wrong location: 0x%x%08x",
                   eventsCount, (jint)(loc >> 32), (jint)loc);
            printf(", expected: 0x%x\n", (jint)pops[eventsCount].loc);
            result = STATUS_FAILED;
        }
    } else {
        printf("Unexpected frame pop catched:");
        printf("     class: \"%s\"\n", cls_sig);
        printf("    method: \"%s%s\"\n", name, sig);
        printf("  location: 0x%x%08x\n", (jint)(loc >> 32), (jint)loc);
        result = STATUS_FAILED;
    }
    eventsCount++;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_framepop001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_framepop001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_framepop001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiError err;
    jint res;

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

    if (caps.can_generate_frame_pop_events &&
            caps.can_generate_breakpoint_events) {
        callbacks.Breakpoint = &Breakpoint;
        callbacks.FramePop = &FramePop;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: FramePop or Breakpoint event is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_FramePop_framepop001_check(JNIEnv *env, jclass cls) {
    jvmtiError err;
    jclass clz;
    jmethodID mid;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    if (!caps.can_generate_frame_pop_events ||
            !caps.can_generate_breakpoint_events) {
        return result;
    }

    mid = env->GetStaticMethodID(cls, "chain", "()V");
    if (mid == 0) {
        printf("Cannot find Method ID for method chain\n");
        return STATUS_FAILED;
    }
    err = jvmti->SetBreakpoint(mid, 0);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to SetBreakpoint: %s (%d)\n",
               TranslateError(err), err);
        return STATUS_FAILED;
    }
    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_FRAME_POP, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable JVMTI_EVENT_FRAME_POP event: %s (%d)\n",
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

    clz = env->FindClass("nsk/jvmti/FramePop/framepop001a");
    if (clz == NULL) {
        printf("Cannot find framepop001a class!\n");
        result = STATUS_FAILED;
        return STATUS_FAILED;
    }
    mid = env->GetStaticMethodID(clz, "dummy", "()V");
    if (mid == 0) {
        printf("Cannot find Method ID for method dummy\n");
        return STATUS_FAILED;
    }
    env->CallStaticVoidMethod(clz, mid);

    if (eventsCount != eventsExpected) {
        printf("Wrong number of frame pop events: %" PRIuPTR ", expected: %" PRIuPTR "\n",
            eventsCount, eventsExpected);
        result = STATUS_FAILED;
    }

    return result;
}

}
