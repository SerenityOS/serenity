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
#include "jni_tools.h"
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

typedef struct {
    const char *cls_sig;
    const char *name;
    const char *sig;
    jlocation loc;
} exit_info;

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static size_t eventsExpected = 0;
static size_t eventsCount = 0;
static exit_info exits[] = {
    { "Lnsk/jvmti/MethodExit/mexit001a;", "chain", "()V", -1 },
    { "Lnsk/jvmti/MethodExit/mexit001a;", "dummy", "()V", 3 }
};

void JNICALL MethodExit(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method,
        jboolean was_poped_by_exc, jvalue return_value) {
    jvmtiError err;
    char *cls_sig, *name, *sig, *generic;
    jclass cls;
    jmethodID mid;
    jlocation loc;
    char buffer[32];

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
    if (cls_sig != NULL &&
            strcmp(cls_sig, "Lnsk/jvmti/MethodExit/mexit001a;") == 0) {
        if (printdump == JNI_TRUE) {
            printf(">>> retrieving method exit info ...\n");
        }
        err = jvmti_env->GetMethodName(method,
            &name, &sig, &generic);
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
            return;
        }
        if (printdump == JNI_TRUE) {
            printf(">>>      class: \"%s\"\n", cls_sig);
            printf(">>>     method: \"%s%s\"\n", name, sig);
            printf(">>>   location: %s\n", jlong_to_string(loc, buffer));
            printf(">>> ... done\n");
        }
        if (eventsCount < sizeof(exits)/sizeof(exit_info)) {
            if (cls_sig == NULL ||
                    strcmp(cls_sig, exits[eventsCount].cls_sig) != 0) {
                printf("(exit#%" PRIuPTR ") wrong class: \"%s\"",
                       eventsCount, cls_sig);
                printf(", expected: \"%s\"\n", exits[eventsCount].cls_sig);
                result = STATUS_FAILED;
            }
            if (name == NULL ||
                    strcmp(name, exits[eventsCount].name) != 0) {
                printf("(exit#%" PRIuPTR ") wrong method name: \"%s\"",
                       eventsCount, name);
                printf(", expected: \"%s\"\n", exits[eventsCount].name);
                result = STATUS_FAILED;
            }
            if (sig == NULL ||
                    strcmp(sig, exits[eventsCount].sig) != 0) {
                printf("(exit#%" PRIuPTR ") wrong method sig: \"%s\"",
                       eventsCount, sig);
                printf(", expected: \"%s\"\n", exits[eventsCount].sig);
                result = STATUS_FAILED;
            }
            if (loc != exits[eventsCount].loc) {
                printf("(exit#%" PRIuPTR ") wrong location: %s",
                       eventsCount, jlong_to_string(loc, buffer));
                printf(", expected: %s\n",
                       jlong_to_string(exits[eventsCount].loc, buffer));
                result = STATUS_FAILED;
            }
        } else {
            printf("Unexpected method exit catched:");
            printf("     class: \"%s\"\n", cls_sig);
            printf("    method: \"%s%s\"\n", name, sig);
            printf("  location: %s\n", jlong_to_string(loc, buffer));
            result = STATUS_FAILED;
        }
        eventsCount++;
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_mexit001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_mexit001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_mexit001(JavaVM *jvm, char *options, void *reserved) {
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

    if (caps.can_generate_method_exit_events) {
        callbacks.MethodExit = &MethodExit;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: MethodExit event is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_MethodExit_mexit001_init0(JNIEnv *env, jclass cls) {
    jvmtiError err;
    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_METHOD_EXIT, NULL);
    if (err == JVMTI_ERROR_NONE) {
        eventsExpected = sizeof(exits)/sizeof(exit_info);
    } else {
        printf("Failed to enable JVMTI_EVENT_METHOD_EXIT event: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    // TODO: should we return result instead?
    return PASSED;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_MethodExit_mexit001_check(JNIEnv *env, jclass cls) {
    jvmtiError err;
    jclass clz;
    jmethodID mid;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    if (!caps.can_generate_method_exit_events) {
        return result;
    }

    clz = env->FindClass("nsk/jvmti/MethodExit/mexit001a");
    if (clz == NULL) {
        printf("Cannot find nsk.jvmti.MethodExit.mexit001a class!\n");
        return STATUS_FAILED;
    }

    mid = env->GetStaticMethodID(clz, "dummy", "()V");
    if (mid == NULL) {
        printf("Cannot find metod \"dummy()\"!\n");
        return STATUS_FAILED;
    }

    env->CallStaticVoidMethod(clz, mid);

    err = jvmti->SetEventNotificationMode(JVMTI_DISABLE,
            JVMTI_EVENT_METHOD_EXIT, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to disable JVMTI_EVENT_METHOD_EXIT event: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (eventsCount != eventsExpected) {
        printf("Wrong number of MethodExit events: %" PRIuPTR ", expected: %" PRIuPTR "\n",
            eventsCount, eventsExpected);
        result = STATUS_FAILED;
    }
    return result;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_MethodExit_mexit001a_chain(JNIEnv *env, jclass cls) {
    printf("Executing chain()\n");
}

}
