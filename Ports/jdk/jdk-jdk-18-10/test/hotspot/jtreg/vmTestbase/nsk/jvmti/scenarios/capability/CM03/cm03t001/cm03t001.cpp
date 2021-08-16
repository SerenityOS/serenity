/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "jni_tools.h"
#include "agent_common.h"
#include "jvmti_tools.h"

extern "C" {

/* The test adds all capabilities suitable for debugging at OnLoad phase:
 *
 *   can_get_bytecodes
 *   can_get_synthetic_attribute
 *   can_pop_frame
 *   can_redefine_classes
 *   can_signal_thread
 *   can_get_source_file_name
 *   can_get_line_numbers
 *   can_get_source_debug_extension
 *   can_access_local_variables
 *   can_suspend
 *   can_generate_field_modification_events
 *   can_generate_field_access_events
 *   can_generate_single_step_events
 *   can_generate_exception_events
 *   can_generate_frame_pop_events
 *   can_generate_breakpoint_events
 *   can_generate_method_entry_events
 *   can_generate_method_exit_events
 *
 * and sets calbacks and enables events correspondent to capabilities above.
 * Then checks that GetCapabilities returns correct list of possessed
 * capabilities in Live phase, and checks that correspondent possessed
 * functions works and requested events are generated.
 */

/* ========================================================================== */

/* scaffold objects */
static jlong timeout = 0;

/* test objects */
static jthread thread = NULL;
static jclass klass = NULL;
static jmethodID method = NULL;
static jfieldID field = NULL;
static jint klass_byte_count = 0;
static unsigned char *klass_bytes = NULL;

/* event counts */
static int FieldAccessEventsCount = 0;
static int FieldModificationEventsCount = 0;
static int SingleStepEventsCount = 0;
static int ExceptionEventsCount = 0;
static int ExceptionCatchEventsCount = 0;
static int BreakpointEventsCount = 0;
static int FramePopEventsCount = 0;
static int MethodEntryEventsCount = 0;
static int MethodExitEventsCount = 0;

/* ========================================================================== */

/** callback functions **/

/* to get class file data for RedefineClasses */
const char* CLASS_NAME = "nsk/jvmti/scenarios/capability/CM03/cm03t001Thread";
static void JNICALL
ClassFileLoadHook(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
        jclass class_beeing_redefined, jobject loader,
        const char* name, jobject protection_domain,
        jint class_data_len, const unsigned char* class_data,
        jint *new_class_data_len, unsigned char** new_class_data) {

    if (name != NULL && (strcmp(name, CLASS_NAME) == 0)) {
        NSK_DISPLAY1("ClassFileLoadHook: %s\n", name);
        if (!NSK_JVMTI_VERIFY(jvmti_env->Allocate(class_data_len, &klass_bytes)))
            nsk_jvmti_setFailStatus();
        else {
            memcpy(klass_bytes, class_data, class_data_len);
            klass_byte_count = class_data_len;
        }
        NSK_JVMTI_VERIFY(
            jvmti_env->SetEventNotificationMode(
                JVMTI_DISABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL));
    }
}

static void JNICALL
FieldAccess(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
        jthread thread, jmethodID method, jlocation location,
        jclass field_klass, jobject object, jfieldID field) {
    char *name = NULL;
    char *signature = NULL;

    FieldAccessEventsCount++;

    if (!NSK_JVMTI_VERIFY(jvmti_env->ClearFieldAccessWatch(klass, field)))
        return;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetFieldName(field_klass, field, &name, &signature, NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY2("FieldAccess event: %s:%s\n", name, signature);
    if (name != NULL)
        jvmti_env->Deallocate((unsigned char*)name);
    if (signature != NULL)
        jvmti_env->Deallocate((unsigned char*)signature);
}

static void JNICALL
FieldModification(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
        jthread thread, jmethodID method, jlocation location,
        jclass field_klass, jobject object,
        jfieldID field, char sig, jvalue new_value) {
    char *name = NULL;
    char *signature = NULL;

    FieldModificationEventsCount++;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetFieldName(field_klass, field, &name, &signature, NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY2("FieldModification event: %s:%s\n", name, signature);
    if (name != NULL)
        jvmti_env->Deallocate((unsigned char*)name);
    if (signature != NULL)
        jvmti_env->Deallocate((unsigned char*)signature);
}

static void JNICALL
SingleStep(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
        jmethodID method, jlocation location) {
    char *name = NULL;
    char *signature = NULL;

    SingleStepEventsCount++;

    NSK_JVMTI_VERIFY(
        jvmti_env->SetEventNotificationMode(
            JVMTI_DISABLE, JVMTI_EVENT_SINGLE_STEP, NULL));

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &name, &signature, NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY2("SingleStep event: %s%s\n", name, signature);
    if (name != NULL)
        jvmti_env->Deallocate((unsigned char*)name);
    if (signature != NULL)
        jvmti_env->Deallocate((unsigned char*)signature);
}

static void JNICALL
Exception(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread,
        jmethodID method, jlocation location, jobject exception,
        jmethodID catch_method, jlocation catch_location) {
    jclass klass = NULL;
    char *signature = NULL;

    ExceptionEventsCount++;

    if (!NSK_JNI_VERIFY(jni_env, (klass = jni_env->GetObjectClass(exception)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(klass, &signature, NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY1("Exception event: %s\n", signature);
    if (signature != NULL)
        jvmti_env->Deallocate((unsigned char*)signature);
}

void JNICALL
ExceptionCatch(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread,
        jmethodID method, jlocation location, jobject exception) {
    jclass klass = NULL;
    char *signature = NULL;

    ExceptionCatchEventsCount++;

    if (!NSK_JNI_VERIFY(jni_env, (klass = jni_env->GetObjectClass(exception)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(klass, &signature, NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY1("ExceptionCatch event: %s\n", signature);
    if (signature != NULL)
        jvmti_env->Deallocate((unsigned char*)signature);
}

static void JNICALL
Breakpoint(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
        jthread thread, jmethodID method, jlocation location) {
    char *name = NULL;
    char *signature = NULL;

    BreakpointEventsCount++;
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &name, &signature, NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY2("Breakpoint event: %s%s\n", name, signature);
    if (name != NULL)
        jvmti_env->Deallocate((unsigned char*)name);
    if (signature != NULL)
        jvmti_env->Deallocate((unsigned char*)signature);

    jvmti_env->NotifyFramePop(thread, 0);
}

static void JNICALL
FramePop(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
        jthread thread, jmethodID method,
        jboolean wasPopedByException) {
    char *name = NULL;
    char *signature = NULL;

    FramePopEventsCount++;
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &name, &signature, NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY2("FramePop event: %s%s\n", name, signature);
    if (name != NULL)
        jvmti_env->Deallocate((unsigned char*)name);
    if (signature != NULL)
        jvmti_env->Deallocate((unsigned char*)signature);
}

static void JNICALL
MethodEntry(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
        jthread thread, jmethodID method) {
    char *name = NULL;
    char *signature = NULL;

    MethodEntryEventsCount++;
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &name, &signature, NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY2("MethodEntry event: %s%s\n", name, signature);
    if (name != NULL)
        jvmti_env->Deallocate((unsigned char*)name);
    if (signature != NULL)
        jvmti_env->Deallocate((unsigned char*)signature);
}

static void JNICALL
MethodExit(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
        jthread thread, jmethodID method,
        jboolean was_poped_by_exception, jvalue return_value) {
    char *name = NULL;
    char *signature = NULL;

    MethodExitEventsCount++;
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &name, &signature, NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY2("MethodExit event: %s%s\n", name, signature);
    if (name != NULL)
        jvmti_env->Deallocate((unsigned char*)name);
    if (signature != NULL)
        jvmti_env->Deallocate((unsigned char*)signature);
}

/* ========================================================================== */

static int prepare(jvmtiEnv* jvmti, JNIEnv* jni) {
    const char* THREAD_NAME = "Debuggee Thread";
    jvmtiThreadInfo info;
    jthread *threads = NULL;
    jint threads_count = 0;
    int i;

    NSK_DISPLAY0("Prepare: find tested thread\n");

    /* get all live threads */
    if (!NSK_JVMTI_VERIFY(jvmti->GetAllThreads(&threads_count, &threads)))
        return NSK_FALSE;

    if (!NSK_VERIFY(threads_count > 0 && threads != NULL))
        return NSK_FALSE;

    /* find tested thread */
    for (i = 0; i < threads_count; i++) {
        if (!NSK_VERIFY(threads[i] != NULL))
            return NSK_FALSE;

        /* get thread information */
        if (!NSK_JVMTI_VERIFY(jvmti->GetThreadInfo(threads[i], &info)))
            return NSK_FALSE;

        NSK_DISPLAY3("    thread #%d (%s): %p\n", i, info.name, threads[i]);

        /* find by name */
        if (info.name != NULL && (strcmp(info.name, THREAD_NAME) == 0)) {
            thread = threads[i];
        }
    }

    if (!NSK_JNI_VERIFY(jni, (thread = jni->NewGlobalRef(thread)) != NULL))
        return NSK_FALSE;

    /* deallocate threads list */
    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)threads)))
        return NSK_FALSE;

    /* get tested thread class */
    if (!NSK_JNI_VERIFY(jni, (klass = jni->GetObjectClass(thread)) != NULL))
        return NSK_FALSE;

    /* klass is used by other threads - convert to global handle */
    if (!NSK_JNI_VERIFY(jni, (klass = (jclass)jni->NewGlobalRef(klass)) != NULL))
        return NSK_FALSE;

    /* get tested thread method 'delay' */
    if (!NSK_JNI_VERIFY(jni, (method = jni->GetMethodID(klass, "delay", "()V")) != NULL))
        return NSK_FALSE;

    /* get tested thread field 'waitingFlag' */
    if (!NSK_JNI_VERIFY(jni, (field = jni->GetFieldID(klass, "waitingFlag", "Z")) != NULL))
        return NSK_FALSE;

    return NSK_TRUE;
}

static int prepareEvents(jvmtiEnv* jvmti, JNIEnv* jni) {
    NSK_DISPLAY0("Prepare events ...\n");

    /* get tested thread method 'letItGo' */
    if (!NSK_JNI_VERIFY(jni, (method = jni->GetMethodID(klass, "letItGo", "()V")) != NULL))
        return NSK_FALSE;

    /* get tested thread field 'waitingFlag' */
    if (!NSK_JNI_VERIFY(jni, (field = jni->GetFieldID(klass, "waitingFlag", "Z")) != NULL))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(jvmti->SetFieldAccessWatch(klass, field)))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(jvmti->SetFieldModificationWatch(klass, field)))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(jvmti->SetBreakpoint(method, 0)))
        return NSK_FALSE;

    /* enable events */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_SINGLE_STEP, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_EXCEPTION, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_EXCEPTION_CATCH, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_ENTRY, thread)))
        return NSK_FALSE;
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_EXIT, thread)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ========================================================================== */

/* Check GetCapabilities function
 */
static int checkGetCapabilities(jvmtiEnv* jvmti) {
    jvmtiCapabilities caps;

    memset(&caps, 0, sizeof(caps));
    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_get_bytecodes))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_get_synthetic_attribute))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_pop_frame))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_redefine_classes))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_signal_thread))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_get_source_file_name))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_get_line_numbers))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_get_source_debug_extension))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_access_local_variables))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_suspend))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_generate_field_modification_events))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_generate_field_access_events))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_generate_single_step_events))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_generate_exception_events))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_generate_frame_pop_events))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_generate_breakpoint_events))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_generate_method_entry_events))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_generate_method_exit_events))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ========================================================================== */

/* Check "can_get_bytecodes" function
 */
static int checkGetBytecodes(jvmtiEnv* jvmti) {
    jint count;
    unsigned char *bytecodes;

    NSK_DISPLAY0("Checking positive: GetBytecodes\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetBytecodes(method, &count, &bytecodes)))
        return NSK_FALSE;
    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate(bytecodes)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_get_synthetic_attribute" functions
 */
static int checkIsSyntheticFunctions(jvmtiEnv* jvmti) {
    jboolean is_synthetic;

    NSK_DISPLAY0("Checking positive: IsFieldSynthetic\n");
    if (!NSK_JVMTI_VERIFY(jvmti->IsFieldSynthetic(klass, field, &is_synthetic)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: IsMethodSynthetic\n");
    if (!NSK_JVMTI_VERIFY(jvmti->IsMethodSynthetic(method, &is_synthetic)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_redefine_classes" functions
 */
static int checkRedefineClasses(jvmtiEnv* jvmti) {
    jvmtiClassDefinition class_def;
    jboolean is_obsolete;

    if (!NSK_VERIFY(klass_byte_count != 0 && klass_bytes != NULL))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: RedefineClasses\n");
    class_def.klass = klass;
    class_def.class_byte_count = klass_byte_count;
    class_def.class_bytes = klass_bytes;
    if (!NSK_JVMTI_VERIFY(jvmti->RedefineClasses(1, &class_def)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: IsMethodObsolete\n");
    if (!NSK_JVMTI_VERIFY(jvmti->IsMethodObsolete(method, &is_obsolete)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_get_source_file_name" function
 */
static int checkGetSourceFileName(jvmtiEnv* jvmti) {
    char *name;

    NSK_DISPLAY0("Checking positive: GetSourceFileName\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetSourceFileName(klass, &name)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_get_line_numbers" function
 */
static int checkGetLineNumberTable(jvmtiEnv* jvmti) {
    jint count;
    jvmtiLineNumberEntry *line_number_table = NULL;

    NSK_DISPLAY0("Checking positive: GetLineNumberTable\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetLineNumberTable(method, &count, &line_number_table)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_get_source_debug_extension" function
 */
static int checkGetSourceDebugExtension(jvmtiEnv* jvmti) {
    char *name;

    NSK_DISPLAY0("Checking positive: GetSourceDebugExtension\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_ABSENT_INFORMATION,
            jvmti->GetSourceDebugExtension(klass, &name)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_access_local_variables" functions
 */
static int checkLocalVariableFunctions(jvmtiEnv* jvmti) {
    jint count;
    jvmtiLocalVariableEntry *local_variable_table = NULL;
    jobject object_value;
    jint int_value;
    jlong long_value;
    jfloat float_value;
    jdouble double_value;
    int i;

    NSK_DISPLAY0("Checking positive: GetLocalVariableTable\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetLocalVariableTable(method, &count, &local_variable_table)))
        return NSK_FALSE;

/* DEBUG -- while 4913796 bug not fixed thread should be suspended
 */
    if (!NSK_JVMTI_VERIFY(jvmti->SuspendThread(thread)))
        return NSK_FALSE;

    for (i = 0; i < count; i++) {
        if (strcmp(local_variable_table[i].name, "o") == 0) {
            NSK_DISPLAY0("Checking positive: GetLocalObject\n");
            if (!NSK_JVMTI_VERIFY(
                    jvmti->GetLocalObject(thread, 1, local_variable_table[i].slot, &object_value)))
                return NSK_FALSE;

            NSK_DISPLAY0("Checking positive: SetLocalObject\n");
            if (!NSK_JVMTI_VERIFY(
                    jvmti->SetLocalObject(thread, 1, local_variable_table[i].slot, object_value)))
                return NSK_FALSE;
        } else if (strcmp(local_variable_table[i].name, "i") == 0) {
            NSK_DISPLAY0("Checking positive: GetLocalInt\n");
            if (!NSK_JVMTI_VERIFY(
                    jvmti->GetLocalInt(thread, 1, local_variable_table[i].slot, &int_value)))
                return NSK_FALSE;

            NSK_DISPLAY0("Checking positive: SetLocalInt\n");
            if (!NSK_JVMTI_VERIFY(
                    jvmti->SetLocalInt(thread, 1, local_variable_table[i].slot, int_value)))
                return NSK_FALSE;
        } else if (strcmp(local_variable_table[i].name, "l") == 0) {
            NSK_DISPLAY0("Checking positive: GetLocalLong\n");
            if (!NSK_JVMTI_VERIFY(
                    jvmti->GetLocalLong(thread, 1, local_variable_table[i].slot, &long_value)))
                return NSK_FALSE;

            NSK_DISPLAY0("Checking positive: SetLocalLong\n");
            if (!NSK_JVMTI_VERIFY(
                    jvmti->SetLocalLong(thread, 1, local_variable_table[i].slot, long_value)))
                return NSK_FALSE;
        } else if (strcmp(local_variable_table[i].name, "f") == 0) {
            NSK_DISPLAY0("Checking positive: GetLocalFloat\n");
            if (!NSK_JVMTI_VERIFY(
                    jvmti->GetLocalFloat(thread, 1, local_variable_table[i].slot, &float_value)))
                return NSK_FALSE;

            NSK_DISPLAY0("Checking positive: SetLocalFloat\n");
            if (!NSK_JVMTI_VERIFY(
                    jvmti->SetLocalFloat(thread, 1, local_variable_table[i].slot, float_value)))
                return NSK_FALSE;
        } else if (strcmp(local_variable_table[i].name, "d") == 0) {
            NSK_DISPLAY0("Checking positive: GetLocalDouble\n");
            if (!NSK_JVMTI_VERIFY(
                    jvmti->GetLocalDouble(thread, 1, local_variable_table[i].slot, &double_value)))
                return NSK_FALSE;

            NSK_DISPLAY0("Checking positive: SetLocalDouble\n");
            if (!NSK_JVMTI_VERIFY(
                    jvmti->SetLocalDouble(thread, 1, local_variable_table[i].slot, double_value)))
                return NSK_FALSE;
        }
    }

/* DEBUG -- while 4913796 bug not fixed thread should be suspended
 */
    if (!NSK_JVMTI_VERIFY(jvmti->ResumeThread(thread)))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)local_variable_table)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_suspend" functions
 */
static int checkSuspend(jvmtiEnv* jvmti) {
    jvmtiError err;

    NSK_DISPLAY0("Checking positive: SuspendThread\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SuspendThread(thread)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: ResumeThread\n");
    if (!NSK_JVMTI_VERIFY(jvmti->ResumeThread(thread)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: SuspendThreadList\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SuspendThreadList(1, &thread, &err)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: ResumeThreadList\n");
    if (!NSK_JVMTI_VERIFY(jvmti->ResumeThreadList(1, &thread, &err)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_pop_frame" function
 */
static int checkPopFrame(jvmtiEnv* jvmti) {
    int result = NSK_TRUE;
    jvmtiError err;

    NSK_DISPLAY0("Checking positive: PopFrame\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SuspendThread(thread)))
        return NSK_FALSE;

    // PopFrame is allowed to fail with JVMTI_ERROR_OPAQUE_FRAME.
    // That will happen if we are in a native function,
    // for example while waiting for a Condition.
    // See JCK-5020108.
    err = jvmti->PopFrame(thread);
    if (err != JVMTI_ERROR_NONE && err != JVMTI_ERROR_OPAQUE_FRAME) {
      result = NSK_FALSE;
      NSK_DISPLAY1("jvmti error from PopFrame: %d\n", err);
    }

    if (!NSK_JVMTI_VERIFY(jvmti->ResumeThread(thread)))
        result = NSK_FALSE;

    return result;
}

/* Check "can_signal_thread" functions
 */
static int checkSignalThread(jvmtiEnv* jvmti, JNIEnv* jni) {
    const char* THREAD_DEATH_CLASS_NAME = "java/lang/ThreadDeath";
    const char* THREAD_DEATH_CTOR_NAME = "<init>";
    const char* THREAD_DEATH_CTOR_SIGNATURE = "()V";
    jclass cls = NULL;
    jmethodID ctor = NULL;
    jobject exception = NULL;

    if (!NSK_JNI_VERIFY(jni, (cls = jni->FindClass(THREAD_DEATH_CLASS_NAME)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (ctor =
            jni->GetMethodID(cls, THREAD_DEATH_CTOR_NAME, THREAD_DEATH_CTOR_SIGNATURE)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (exception = jni->NewObject(cls, ctor)) != NULL))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: InterruptThread\n");
    if (!NSK_JVMTI_VERIFY(jvmti->InterruptThread(thread)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: StopThread\n");
    if (!NSK_JVMTI_VERIFY(jvmti->StopThread(thread, exception)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ========================================================================== */

/* Check generated events
 */
static int checkGeneratedEvents() {
    int result = NSK_TRUE;

    NSK_DISPLAY1("FieldAccess events received: %d\n",
        FieldAccessEventsCount);
    if (!NSK_VERIFY(FieldAccessEventsCount != 0))
        result = NSK_FALSE;

    NSK_DISPLAY1("FieldModification events received: %d\n",
        FieldModificationEventsCount);
    if (!NSK_VERIFY(FieldModificationEventsCount != 0))
        result = NSK_FALSE;

    NSK_DISPLAY1("SingleStep events received: %d\n",
        SingleStepEventsCount);
    if (!NSK_VERIFY(SingleStepEventsCount != 0))
        result = NSK_FALSE;

    NSK_DISPLAY1("Exception events received: %d\n",
        ExceptionEventsCount);
    if (!NSK_VERIFY(ExceptionEventsCount != 0))
        result = NSK_FALSE;

    NSK_DISPLAY1("ExceptionCatch events received: %d\n",
        ExceptionCatchEventsCount);
    if (!NSK_VERIFY(ExceptionCatchEventsCount != 0))
        result = NSK_FALSE;

    NSK_DISPLAY1("Breakpoint events received: %d\n",
        BreakpointEventsCount);
    if (!NSK_VERIFY(BreakpointEventsCount != 0))
        result = NSK_FALSE;

    NSK_DISPLAY1("FramePop events received: %d\n",
        FramePopEventsCount);
    if (!NSK_VERIFY(FramePopEventsCount != 0))
        result = NSK_FALSE;

    NSK_DISPLAY1("MethodEntry events received: %d\n",
        MethodEntryEventsCount);
    if (!NSK_VERIFY(MethodEntryEventsCount != 0))
        result = NSK_FALSE;

    NSK_DISPLAY1("MethodExit events received: %d\n",
        MethodExitEventsCount);
    if (!NSK_VERIFY(MethodExitEventsCount != 0))
        result = NSK_FALSE;

    return result;
}

/* ========================================================================== */

/* agent algorithm */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    /* wait for initial sync */
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!prepare(jvmti, jni)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    NSK_DISPLAY0("Testcase #1: check if GetCapabilities returns the capabilities\n");
    if (!checkGetCapabilities(jvmti)) {
        nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY0("Testcase #2: check if correspondent functions work\n");
    if (!checkGetBytecodes(jvmti))
        nsk_jvmti_setFailStatus();
    if (!checkIsSyntheticFunctions(jvmti))
        nsk_jvmti_setFailStatus();
    if (!checkRedefineClasses(jvmti))
        nsk_jvmti_setFailStatus();
    if (!checkGetSourceFileName(jvmti))
        nsk_jvmti_setFailStatus();
    if (!checkGetLineNumberTable(jvmti))
        nsk_jvmti_setFailStatus();
    if (!checkGetSourceDebugExtension(jvmti))
        nsk_jvmti_setFailStatus();
    if (!checkLocalVariableFunctions(jvmti))
        nsk_jvmti_setFailStatus();
    if (!checkSuspend(jvmti))
        nsk_jvmti_setFailStatus();

    if (!prepareEvents(jvmti, jni)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* resume debugee and wait for sync */
    if (!nsk_jvmti_resumeSync())
        return;
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!checkPopFrame(jvmti))
        nsk_jvmti_setFailStatus();
    if (!checkSignalThread(jvmti, jni))
        nsk_jvmti_setFailStatus();

    NSK_TRACE(jni->DeleteGlobalRef(thread));
    NSK_TRACE(jni->DeleteGlobalRef(klass));

    /* resume debugee and wait for sync */
    if (!nsk_jvmti_resumeSync())
        return;
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    NSK_DISPLAY0("Testcase #3: check if the events are generated\n");
    if (!checkGeneratedEvents()) {
        nsk_jvmti_setFailStatus();
    }

    /* resume debugee after last sync */
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/* agent library initialization */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_cm03t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_cm03t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_cm03t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;
    jvmtiCapabilities caps;
    jvmtiEventCallbacks callbacks;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60000;
    NSK_DISPLAY1("Timeout: %d msc\n", (int)timeout);

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* add capabilities */
    memset(&caps, 0, sizeof(caps));
    caps.can_get_bytecodes = 1;
    caps.can_get_synthetic_attribute = 1;
    caps.can_pop_frame = 1;
    caps.can_redefine_classes = 1;
    caps.can_signal_thread = 1;
    caps.can_get_source_file_name = 1;
    caps.can_get_line_numbers = 1;
    caps.can_get_source_debug_extension = 1;
    caps.can_access_local_variables = 1;
    caps.can_suspend = 1;
    caps.can_generate_field_modification_events = 1;
    caps.can_generate_field_access_events = 1;
    caps.can_generate_single_step_events = 1;
    caps.can_generate_exception_events = 1;
    caps.can_generate_frame_pop_events = 1;
    caps.can_generate_breakpoint_events = 1;
    caps.can_generate_method_entry_events = 1;
    caps.can_generate_method_exit_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    /* set event callbacks */
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.ClassFileLoadHook = &ClassFileLoadHook;
    callbacks.FieldAccess = &FieldAccess;
    callbacks.FieldModification = &FieldModification;
    callbacks.SingleStep = &SingleStep;
    callbacks.Exception = &Exception;
    callbacks.ExceptionCatch = &ExceptionCatch;
    callbacks.Breakpoint = &Breakpoint;
    callbacks.FramePop = &FramePop;
    callbacks.MethodEntry = &MethodEntry;
    callbacks.MethodExit = &MethodExit;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    /* enable events */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_FIELD_ACCESS, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_FIELD_MODIFICATION, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_BREAKPOINT, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_FRAME_POP, NULL)))
        return JNI_ERR;

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

}
