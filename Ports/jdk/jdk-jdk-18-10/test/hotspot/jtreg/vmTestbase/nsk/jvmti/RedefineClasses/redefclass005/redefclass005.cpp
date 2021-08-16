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
#include <jvmti.h>
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define STATUS_FAILED 2
#define PASSED 0

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static int watch_ev = 0;    /* ignore JVMTI events by default */
static int CFLH_gen_ev = 0; /* number of generated ClassFileLoadHook events */
static int gen_ev = 0;      /* number of generated events */
static int result = PASSED; /* total result of the test */

static jrawMonitorID watch_ev_monitor;

static void set_watch_ev(int value) {
    jvmti->RawMonitorEnter(watch_ev_monitor);

    watch_ev = value;

    jvmti->RawMonitorExit(watch_ev_monitor);
}

void JNICALL
ClassFileLoadHook(jvmtiEnv *jvmti_env, JNIEnv *env, jclass class_being_redefined,
        jobject loader, const char* name, jobject protection_domain,
        jint class_data_len, const unsigned char* class_data,
        jint *new_class_data_len, unsigned char** new_class_data) {

    jvmti->RawMonitorEnter(watch_ev_monitor);

    if (watch_ev && class_being_redefined != NULL) {
        printf("#### JVMTI_EVENT_CLASS_FILE_LOAD_HOOK occurred ####\n");
        CFLH_gen_ev++;
    }

    jvmti->RawMonitorExit(watch_ev_monitor);
}

void JNICALL
ClassLoad(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr, jclass cls) {
    jvmti->RawMonitorEnter(watch_ev_monitor);

    if (watch_ev) {
        printf("#### JVMTI_EVENT_CLASS_LOAD occurred ####\n");
        gen_ev++;
    }

    jvmti->RawMonitorExit(watch_ev_monitor);
}

void JNICALL
ClassPrepare(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr, jclass cls) {
    jvmti->RawMonitorEnter(watch_ev_monitor);

    if (watch_ev) {
        printf("#### JVMTI_EVENT_CLASS_PREPARE occurred ####\n");
        gen_ev++;
    }

    jvmti->RawMonitorExit(watch_ev_monitor);
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_redefclass005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_redefclass005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_redefclass005(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *vm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    res = vm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK) {
        printf("%s: Failed to call GetEnv: error=%d\n", __FILE__, res);
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

    if (!caps.can_redefine_classes) {
        printf("Warning: RedefineClasses is not implemented\n");
    }

    callbacks.ClassFileLoadHook = &ClassFileLoadHook;
    callbacks.ClassLoad = &ClassLoad;
    callbacks.ClassPrepare = &ClassPrepare;
    err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    if (err != JVMTI_ERROR_NONE) {
        printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->CreateRawMonitor("watch_ev_monitor", &watch_ev_monitor);
    if (err != JVMTI_ERROR_NONE) {
        printf("(CreateRawMonitor) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RedefineClasses_redefclass005_makeRedefinition(JNIEnv *env,
        jclass cls, jint vrb, jclass redefCls, jbyteArray classBytes) {
    jvmtiError err;
    jthread thread;
    jvmtiClassDefinition classDef;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    if (!caps.can_redefine_classes) {
        return PASSED;
    }

    err = jvmti->GetCurrentThread(&thread);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to get current thread: %s (%d)\n", TranslateError(err), err);
        result = STATUS_FAILED;
        return STATUS_FAILED;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable event JVMTI_EVENT_CLASS_FILE_LOAD_HOOK: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_LOAD, thread);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable event JVMTI_EVENT_CLASS_LOAD: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE, thread);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable JVMTI_EVENT_CLASS_PREPARE: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

/* filling the structure jvmtiClassDefinition */
    classDef.klass = redefCls;
    classDef.class_byte_count = env->GetArrayLength(classBytes);
    classDef.class_bytes = (unsigned char *) env->GetByteArrayElements(classBytes, NULL);

    set_watch_ev(1); /* watch JVMTI events */

    if (vrb == 1)
        printf(">>>>>>>> Invoke RedefineClasses():\n\tnew class byte count=%d\n",
            classDef.class_byte_count);
    err = jvmti->RedefineClasses(1, &classDef);
    if (err != JVMTI_ERROR_NONE) {
        printf("TEST FAILED: the function RedefineClasses() returned error %d: %s\n",
            err, TranslateError(err));
        printf("\tFor more info about this error see the JVMTI spec.\n");
        result = STATUS_FAILED;
    }
    else if (vrb == 1)
        printf("Check #1 PASSED: RedefineClasses() is successfully done\n");

    set_watch_ev(0); /* again ignore JVMTI events */

    if (CFLH_gen_ev == 0) {
        printf("TEST FAILED: ClassFileLoadHook event was not generated by the function RedefineClasses()\n");
        result = STATUS_FAILED;
    } else if (vrb == 1)
        printf("Check #2 PASSED: %d ClassFileLoadHook events were generated by the function RedefineClasses()\n",
            CFLH_gen_ev);
    if (gen_ev) {
        printf("TEST FAILED: %d unexpected JVMTI events were generated by the function RedefineClasses()\n",
            gen_ev);
        result = STATUS_FAILED;
    } else if (vrb == 1)
        printf("Check #2 PASSED: No unexpected JVMTI events were generated by the function RedefineClasses()\n");

    return(result);
}

}
