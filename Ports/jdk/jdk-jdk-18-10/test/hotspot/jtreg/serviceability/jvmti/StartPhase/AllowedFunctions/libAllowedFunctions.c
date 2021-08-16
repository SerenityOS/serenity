/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "jvmti.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifndef JNI_ENV_ARG

#ifdef __cplusplus
#define JNI_ENV_ARG(x, y) y
#define JNI_ENV_PTR(x) x
#else
#define JNI_ENV_ARG(x,y) x, y
#define JNI_ENV_PTR(x) (*x)
#endif

#endif

#define PASSED 0
#define FAILED 2

static jint result = PASSED;

static jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved);

JNIEXPORT
jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT
jint JNICALL Agent_OnAttach(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT
jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    return JNI_VERSION_9;
}

static void check_jvmti_error(jvmtiEnv *jvmti, char* fname, jvmtiError err) {
    if (err != JVMTI_ERROR_NONE) {
        printf("  ## %s error: %d\n", fname, err);
        exit(err);
    }
}

 static void deallocate(jvmtiEnv *jvmti, char* mem) {
    jvmtiError err = (*jvmti)->Deallocate(jvmti, (unsigned char*)mem);
    check_jvmti_error(jvmti, "Deallocate", err);
}

static void get_phase(jvmtiEnv *jvmti, jvmtiPhase *phase_ptr) {
    jvmtiError err = (*jvmti)->GetPhase(jvmti, phase_ptr);
    check_jvmti_error(jvmti, "GetPhase", err);
}

static jthread get_cur_thread(jvmtiEnv *jvmti) {
    jthread cur_thread = NULL;
    jvmtiError err = (*jvmti)->GetCurrentThread(jvmti, &cur_thread);
    check_jvmti_error(jvmti, "GetCurrentThread", err);
    return cur_thread;
}

static intptr_t get_thread_local(jvmtiEnv *jvmti, jthread thread) {
    void *val = NULL;
    jvmtiError err = (*jvmti)->GetThreadLocalStorage(jvmti, thread, &val);
    check_jvmti_error(jvmti, "GetThreadLocalStorage", err);
    return (intptr_t)val;
}

static void set_thread_local(jvmtiEnv *jvmti, jthread thread, intptr_t x) {
    void *val = (void*)x;
    jvmtiError err = (*jvmti)->SetThreadLocalStorage(jvmti, thread, val);
    check_jvmti_error(jvmti, "SetThreadLocalStorage", err);
}

static void print_class_status(jvmtiEnv *jvmti, jclass klass) {
    jint status = 0;
    jvmtiError err = (*jvmti)->GetClassStatus(jvmti, klass, &status);

    check_jvmti_error(jvmti, "GetClassStatus", err);
    // This function is only used in a ClassPrepare event context
    if ((status & JVMTI_CLASS_STATUS_VERIFIED)    == 0 ||
        (status & JVMTI_CLASS_STATUS_PREPARED)    == 0 ||
        (status & JVMTI_CLASS_STATUS_INITIALIZED) != 0 ||
        (status & JVMTI_CLASS_STATUS_ERROR)       != 0) {
        printf("  ## Error: unexpected class status: 0x%08x\n", status);
    }
    printf("    Class status: 0x%08x\n", status);
}

static void print_class_signature(jvmtiEnv *jvmti, jclass klass) {
    char* name = NULL;
    jvmtiError err = (*jvmti)->GetClassSignature(jvmti, klass, &name, NULL);

    check_jvmti_error(jvmti, "GetClassSignature", err);
    if (name != NULL) {
        printf(" class: '%s'\n", name);
        deallocate(jvmti, name);
    }
}

static void print_class_source_file_name(jvmtiEnv *jvmti, jclass klass) {
    char* name = NULL;
    jvmtiError err = (*jvmti)->GetSourceFileName(jvmti, klass, &name);

    check_jvmti_error(jvmti, "GetSourceFileName", err);
    if (name != NULL) {
        printf("    Class source file name: '%s'\n", name);
        deallocate(jvmti, name);
    }
}

static void print_class_info(jvmtiEnv *jvmti, jclass klass) {
    jint mods = 0;
    jboolean is_interface  = JNI_FALSE;
    jboolean is_array      = JNI_FALSE;
    jboolean is_modifiable = JNI_FALSE;
    jvmtiError err = (*jvmti)->GetClassModifiers(jvmti, klass, &mods);

    check_jvmti_error(jvmti, "GetClassModifiers", err);
    printf("    Class modifiers: 0x%08x\n", mods);

    err = (*jvmti)->IsInterface(jvmti, klass, &is_interface);
    check_jvmti_error(jvmti, "IsInterface", err);
    printf("    Class is interface: %d\n", is_interface);

    err = (*jvmti)->IsArrayClass(jvmti, klass, &is_array);
    check_jvmti_error(jvmti, "IsArrayClass", err);
    printf("    Class is array: %d\n", is_array);

    err = (*jvmti)->IsModifiableClass(jvmti, klass, &is_modifiable);
    check_jvmti_error(jvmti, "IsModifiableClass", err);
    printf("    Class is modifiable: %d\n", is_modifiable);
}

static jint get_class_methods(jvmtiEnv *jvmti, jclass klass, jmethodID** methods_ptr) {
    jint count = 0;
    jvmtiError err = (*jvmti)->GetClassMethods(jvmti, klass, &count, methods_ptr);
    check_jvmti_error(jvmti, "GetClassMethods", err);
    return count;
}

static jint get_class_fields(jvmtiEnv *jvmti, jclass klass, jfieldID** fields_ptr) {
    jint count = 0;
    jvmtiError err = (*jvmti)->GetClassFields(jvmti, klass, &count, fields_ptr);
    check_jvmti_error(jvmti, "GetClassFields", err);
    return count;
}

static void print_method_name_sign(jvmtiEnv *jvmti, jmethodID method) {
    char* name = NULL;
    char* sign = NULL;
    jvmtiError err = (*jvmti)->GetMethodName(jvmti, method, &name, &sign, NULL);

    check_jvmti_error(jvmti, "GetMethodName", err);
    printf("  Method: %s%s\n", name, sign);
    deallocate(jvmti, name);
    deallocate(jvmti, sign);
}

static void print_method_declaring_class(jvmtiEnv *jvmti, jmethodID method) {
    jclass dclass = NULL;
    jvmtiError err = (*jvmti)->GetMethodDeclaringClass(jvmti, method, &dclass);

    check_jvmti_error(jvmti, "GetMethodDeclaringClass", err);
    printf("    Method declaring");
    print_class_signature(jvmti, dclass);
}

static void print_method_info(jvmtiEnv *jvmti, jmethodID method) {
    jint mods = 0;
    jint locals_max = 0;
    jint args_size = 0;
    jboolean is_native   = JNI_FALSE;
    jboolean is_synth    = JNI_FALSE;
    jboolean is_obsolete = JNI_FALSE;
    jvmtiError err = (*jvmti)->GetMethodModifiers(jvmti, method, &mods);

    check_jvmti_error(jvmti, "GetMethodModifiers", err);
    printf("    Method modifiers: 0x%08x\n", mods);

    err = (*jvmti)->IsMethodNative(jvmti, method, &is_native);
    check_jvmti_error(jvmti, "IsMethodNative", err);
    printf("    Method is native: %d\n", is_native);

    if (is_native == JNI_FALSE) {
        err = (*jvmti)->GetMaxLocals(jvmti, method, &locals_max);
        check_jvmti_error(jvmti, "GetMaxLocals", err);
        printf("    Method max locals: %d\n", locals_max);

        err = (*jvmti)->GetArgumentsSize(jvmti, method, &args_size);
        check_jvmti_error(jvmti, "GetArgumentsSize", err);
        printf("    Method arguments size: %d\n", args_size);
    }

    err = (*jvmti)->IsMethodSynthetic(jvmti, method, &is_synth);
    check_jvmti_error(jvmti, "IsMethodSynthetic", err);
    printf("    Method is synthetic: %d\n", is_synth);

    err = (*jvmti)->IsMethodObsolete(jvmti, method, &is_obsolete);
    check_jvmti_error(jvmti, "IsMethodObsolete", err);
    printf("    Method is obsolete: %d\n", is_obsolete);
}

static void test_method_functions(jvmtiEnv *jvmti, jmethodID method) {
    print_method_name_sign(jvmti, method);
    print_method_declaring_class(jvmti, method);
    print_method_info(jvmti, method);
}

static void print_field_name_sign(jvmtiEnv *jvmti, jclass klass, jfieldID field) {
    char* name = NULL;
    char* sign = NULL;
    jvmtiError err = (*jvmti)->GetFieldName(jvmti, klass, field, &name, &sign, NULL);

    check_jvmti_error(jvmti, "GetFieldName", err);
    printf("  Field: %s %s\n", sign, name);
    deallocate(jvmti, name);
    deallocate(jvmti, sign);
}

static void print_field_declaring_class(jvmtiEnv *jvmti, jclass klass, jfieldID field) {
    jclass dclass = NULL;
    jvmtiError err = (*jvmti)->GetFieldDeclaringClass(jvmti, klass, field, &dclass);

    check_jvmti_error(jvmti, "GetFieldDeclaringClass", err);
    printf("    Field declaring");
    print_class_signature(jvmti, dclass);
}

static void print_field_info(jvmtiEnv *jvmti, jclass klass, jfieldID field) {
    jint mods = 0;
    jboolean is_synth = JNI_FALSE;
    jvmtiError err = (*jvmti)->GetFieldModifiers(jvmti, klass, field, &mods);

    check_jvmti_error(jvmti, "GetFieldModifiers", err);
    printf("    Field modifiers: 0x%08x\n", mods);

    err = (*jvmti)->IsFieldSynthetic(jvmti, klass, field, &is_synth);
    check_jvmti_error(jvmti, "IsFieldSynthetic", err);
    printf("    Field is synthetic: %d\n", is_synth);
}

static void test_field_functions(jvmtiEnv *jvmti, jclass klass, jfieldID field) {
    print_field_name_sign(jvmti, klass, field);
    print_field_declaring_class(jvmti, klass, field);
    print_field_info(jvmti, klass, field);
}

static void test_class_functions(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jclass klass) {
    jint count = 0;
    jint idx = 0;
    jmethodID* methods = NULL;
    jfieldID*  fields = NULL;

    print_class_signature(jvmti, klass);
    print_class_status(jvmti, klass);
    print_class_source_file_name(jvmti, klass);
    print_class_info(jvmti, klass);

    count = get_class_methods(jvmti, klass, &methods);
    for (idx = 0; idx < count; idx++) {
        test_method_functions(jvmti, methods[idx]);
    }
    if (methods != NULL) {
        deallocate(jvmti, (char*)methods);
    }
    count = get_class_fields(jvmti, klass, &fields);
    for (idx = 0; idx < count; idx++) {
        test_field_functions(jvmti, klass, fields[idx]);
    }
    if (fields != NULL) {
        deallocate(jvmti, (char*)fields);
    }
}

static void JNICALL
VMStart(jvmtiEnv *jvmti, JNIEnv* jni) {
    jvmtiPhase phase;

    printf("VMStart event\n");
    get_phase(jvmti, &phase);
    if (phase != JVMTI_PHASE_START && phase != JVMTI_PHASE_LIVE) {
        printf("  ## Error: unexpected phase: %d, expected: %d or %d\n",
               phase, JVMTI_PHASE_START, JVMTI_PHASE_LIVE);
        result = FAILED;
    }
}

static void JNICALL
VMInit(jvmtiEnv *jvmti, JNIEnv* jnii, jthread thread) {
    jvmtiPhase phase;

    printf("VMInit event\n");
    get_phase(jvmti, &phase);
    if (phase != JVMTI_PHASE_LIVE) {
        printf("  ## Error: unexpected phase: %d, expected: %d\n",
               phase, JVMTI_PHASE_LIVE);
        result = FAILED;
    }
}

static void JNICALL
ClassPrepare(jvmtiEnv *jvmti, JNIEnv *env, jthread thread, jclass klass) {
    static const jint EVENTS_LIMIT = 2;
    static       jint event_no = 0;
    jthread cur_thread = get_cur_thread(jvmti);
    jvmtiPhase phase;
    intptr_t exp_val = 777;
    intptr_t act_val;

    get_phase(jvmti, &phase);
    if (phase != JVMTI_PHASE_START && phase != JVMTI_PHASE_LIVE) {
        printf("  ## Error: unexpected phase: %d, expected: %d or %d\n",
               phase, JVMTI_PHASE_START, JVMTI_PHASE_LIVE);
        return;
    }
    if (phase == JVMTI_PHASE_START && event_no < EVENTS_LIMIT) {
        printf("\nClassPrepare event during the start phase: #%d\n", event_no);
        // Test the JVMTI class functions during the start phase
        test_class_functions(jvmti, env, thread, klass);

        set_thread_local(jvmti, thread, exp_val);
        act_val = get_thread_local(jvmti, cur_thread);
        if (act_val != exp_val) { // Actual value does not match the expected
            printf("  ## Unexpected thread-local: %ld, expected: %ld\n",
                   (long)act_val, (long)exp_val);
            result = FAILED;
        } else {
            printf("    Got expected thread-local: %ld\n", (long)exp_val);
        }
        event_no++;
    }
}

static
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jboolean with_early_vm_start_capability = JNI_FALSE;
    jvmtiEnv *jvmti = NULL;
    jvmtiError err;
    jint res, size;
    jvmtiCapabilities caps;
    jvmtiEventCallbacks callbacks;

    if (options != NULL && strstr(options, "with_early_vmstart") != NULL) {
        with_early_vm_start_capability = JNI_TRUE;
    }

    res = JNI_ENV_PTR(jvm)->GetEnv(JNI_ENV_ARG(jvm, (void **) &jvmti), JVMTI_VERSION_9);
    if (res != JNI_OK || jvmti == NULL) {
        printf("## Agent_Initialize: Error in GetEnv: res: %d, jvmti env: %p\n", res, jvmti);
        return JNI_ERR;
    }

    memset(&caps, 0, sizeof(caps));
    caps.can_get_source_file_name = 1;
    caps.can_get_synthetic_attribute = 1;

    if (with_early_vm_start_capability == JNI_TRUE) {
        caps.can_generate_early_vmstart = 1;
        printf("Capability enabled: can_generate_early_vmstart\n");
    } else {
        printf("Capability disabled: can_generate_early_vmstart\n");
    }
    err = (*jvmti)->AddCapabilities(jvmti, &caps);
    check_jvmti_error(jvmti, "## Agent_Initialize: AddCapabilites", err);

    size = (jint)sizeof(callbacks);
    memset(&callbacks, 0, size);
    callbacks.VMStart = VMStart;
    callbacks.VMInit = VMInit;
    callbacks.ClassPrepare = ClassPrepare;

    err = (*jvmti)->SetEventCallbacks(jvmti, &callbacks, size);
    check_jvmti_error(jvmti, "## Agent_Initialize: SetEventCallbacks", err);

    err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_VM_START, NULL);
    check_jvmti_error(jvmti, "## Agent_Initialize: SetEventNotificationMode VM_START", err);

    err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);
    check_jvmti_error(jvmti, "## Agent_Initialize: SetEventNotificationMode VM_INIT", err);

    err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE, NULL);
    check_jvmti_error(jvmti, "## Agent_Initialize: SetEventNotificationMode CLASS_PREPARE", err);
    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_AllowedFunctions_check(JNIEnv *env, jclass cls) {
    return result;
}

#ifdef __cplusplus
}
#endif
