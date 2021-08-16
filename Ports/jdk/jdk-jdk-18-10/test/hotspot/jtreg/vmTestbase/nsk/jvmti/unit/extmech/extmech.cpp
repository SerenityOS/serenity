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

#define PASSED 0
#define STATUS_FAILED 2

static jvmtiEnv *jvmti;
static jint result = PASSED;

/*
 * Class:     nsk.jvmti.unit.extmech
 * Method:    isClassUnloadingEnabled
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_unit_extmech_isClassUnloadingEnabled
  (JNIEnv *env, jclass cls)
{
    jint count, i;
    jvmtiExtensionFunctionInfo* ext_funcs;
    jvmtiError err;

    err = jvmti->GetExtensionFunctions(&count, &ext_funcs);
    if (err != JVMTI_ERROR_NONE) {
        fprintf(stderr, "GetExtensionFunctions failed: %d\n", err);
        result = STATUS_FAILED;
        return JNI_FALSE;
    }

    for (i=0; i<count; i++) {
        if (strcmp(ext_funcs[i].id, (char*)"com.sun.hotspot.functions.IsClassUnloadingEnabled") == 0) {
            jboolean enabled;
            err = (*ext_funcs[i].func)(jvmti, &enabled);

            if (err != JVMTI_ERROR_NONE) {
                fprintf(stderr, "IsClassUnloadingEnabled failed: %d\n", err);
                result = STATUS_FAILED;
                return JNI_FALSE;
            } else {
                return enabled;
            }
        }
    }

    return JNI_FALSE;
}

static void JNICALL
ClassUnload(jvmtiEnv* jvmti_env, JNIEnv *jni_env, jthread thread, jclass cls) {
}

/*
 * Class:     nsk.jvmti.unit.extmech
 * Method:    enableClassUnloadEvent
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_extmech_enableClassUnloadEvent
  (JNIEnv *env, jclass cls, jboolean enable)
{
    jint count, i;
    jvmtiExtensionEventInfo* ext_events;
    jvmtiError err;

    err = jvmti->GetExtensionEvents(&count, &ext_events);
    if (err != JVMTI_ERROR_NONE) {
        fprintf(stderr, "GetExtensionEvents failed: %d\n", err);
        result = STATUS_FAILED;
        return;
    }

    for (i=0; i<count; i++) {
        if (strcmp(ext_events[i].id, (char*)"com.sun.hotspot.events.ClassUnload") == 0) {

            err = jvmti->SetExtensionEventCallback(ext_events[i].extension_event_index,
                enable ? (jvmtiExtensionEvent)ClassUnload : NULL);

            if (err != JVMTI_ERROR_NONE) {
                fprintf(stderr, "SetExtenionEventCallback failed: %d\n", err);
                result = STATUS_FAILED;
            } else {
                char* id = ext_events[i].id;
                if (enable) {
                    fprintf(stderr, "%s callback enabled\n", id);
                } else {
                    fprintf(stderr, "%s callback disabled\n", id);
                }
            }
            return;
        }
    }
}

/*
 * Class:     nsk.jvmti.unit.extmech
 * Method:    dumpExtensions
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_extmech_dumpExtensions
  (JNIEnv *env, jclass cls)
{
    jint count, i, j;
    jvmtiExtensionFunctionInfo* ext_funcs;
    jvmtiExtensionEventInfo* ext_events;
    jvmtiError err;

    err = jvmti->GetExtensionFunctions(&count, &ext_funcs);
    if (err != JVMTI_ERROR_NONE) {
        fprintf(stderr, "GetExtensionFunctions failed: %d\n", err);
        result = STATUS_FAILED;
        return;
    }

    fprintf(stderr, "Extension functions:\n");
    for (i=0; i<count; i++) {
        jvmtiParamInfo* params = ext_funcs[i].params;
        jvmtiError* errors = ext_funcs[i].errors;

        fprintf(stderr, "%s (%s)\n", ext_funcs[i].id, ext_funcs[i].short_description);

        fprintf(stderr, "    Parameters:\n");

        for (j=0; j<ext_funcs[i].param_count; j++) {
            fprintf(stderr, "      %s type:%d\n", params[j].name, params[j].base_type);
        }

        fprintf(stderr, "    Errors:\n");
        for (j=0; j<ext_funcs[i].error_count; j++) {
            fprintf(stderr, "      %d\n", errors[j]);
        }
    }

    /* --- */

    err = jvmti->GetExtensionEvents(&count, &ext_events);
    if (err != JVMTI_ERROR_NONE) {
        fprintf(stderr, "GetExtensionEvents failed: %d\n", err);
        result = STATUS_FAILED;
        return;
    }

    fprintf(stderr, "Extension events:\n");
    for (i=0; i<count; i++) {
        jvmtiParamInfo* params = ext_events[i].params;

        fprintf(stderr, "event: %d, %s (%s)\n",
            ext_events[i].extension_event_index,
            ext_events[i].id, ext_events[i].short_description);

        fprintf(stderr, "    Parameters:\n");

        for (j=0; j<ext_events[i].param_count; j++) {
            fprintf(stderr, "      %s type:%d\n", params[j].name, params[j].base_type);
        }

    }
}

/*
 * Class:     nsk.jvmti.unit.extmech
 * Method:    getResult
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_extmech_getResult
  (JNIEnv *env, jclass cls)
{
    return result;
}

/*
 * JVM_OnLoad - add capabilities and enables OBJECT_FREE event
 */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_extmech(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_extmech(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_extmech(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *vm, char *options, void *reserved)
{
    jint rc;

    /* get JVMTI environment */

    rc = vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_1);
    if (rc != JNI_OK) {
        fprintf(stderr, "Unable to create jvmtiEnv, GetEnv failed, error=%d\n", rc);
        return JNI_ERR;
    }

    return JNI_OK;
}

}
