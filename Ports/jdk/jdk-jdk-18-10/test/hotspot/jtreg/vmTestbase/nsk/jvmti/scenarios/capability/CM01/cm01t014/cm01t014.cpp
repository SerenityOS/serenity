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
#include "jni_tools.h"
#include "agent_common.h"
#include "jvmti_tools.h"

extern "C" {

#define CAPABILITY can_get_bytecodes
#define CAPABILITY_STR "can_get_bytecodes"

/* The test checks capability can_get_bytecodes
 * and correspondent function GetBytecodes.
 *
 * Testcases:
 *   1. Check if GetPotentialCapabilities returns the capability
 *   2. Add the capability during Live phase
 *   3. Check if GetCapabilities returns the capability
 *   4. Check that only correspondent function work and functions of
 *      other capabilities return JVMTI_ERROR_MUST_POSSESS_CAPABILITY
 *   5. Relinquish the capability during Live phase
 *   6. Check if GetCapabilities does not return the capability
 *   7. Check that correspondent to relinquished capability function
 *      returns JVMTI_ERROR_MUST_POSSESS_CAPABILITY
 *   8. Add back the capability and check with GetCapabilities
 *   9. Check if VM exits well with the capability has not been relinquished
 */

/* ========================================================================== */

/* scaffold objects */
static JNIEnv* jni = NULL;
static jvmtiEnv *jvmti = NULL;
static jlong timeout = 0;

/* test objects */
static jthread thread = NULL;
static jclass klass = NULL;
static jmethodID method = NULL;
static jfieldID field = NULL;

/* ========================================================================== */

static int prepare() {
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

    /* deallocate threads list */
    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)threads)))
        return NSK_FALSE;

    /* get tested thread class */
    if (!NSK_JNI_VERIFY(jni, (klass = jni->GetObjectClass(thread)) != NULL))
        return NSK_FALSE;

    /* get tested thread method 'run' */
    if (!NSK_JNI_VERIFY(jni, (method = jni->GetMethodID(klass, "run", "()V")) != NULL))
        return NSK_FALSE;

    /* get tested thread field 'waitingMonitor' */
    if (!NSK_JNI_VERIFY(jni, (field =
            jni->GetFieldID(klass, "waitingMonitor", "Ljava/lang/Object;")) != NULL))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ========================================================================== */

/* Check GetPotentialCapabilities function
 */
static int checkGetPotentialCapabilities() {
    jvmtiCapabilities caps;

    if (!NSK_JVMTI_VERIFY(jvmti->GetPotentialCapabilities(&caps)))
        return NSK_FALSE;
    if (!caps.CAPABILITY) {
        NSK_COMPLAIN1("GetPotentialCapabilities does not return \"%s\" capability\n",
            CAPABILITY_STR);
        return NSK_FALSE;
    }

    return NSK_TRUE;
}

/* Check AddCapabilities function
 */
static int checkAddCapabilities() {
    jvmtiCapabilities caps;

    memset(&caps, 0, sizeof(caps));
    caps.CAPABILITY = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check GetCapabilities function
 */
static int checkGetCapabilities(int owe) {
    jvmtiCapabilities caps;

    memset(&caps, 0, sizeof(caps));
    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return NSK_FALSE;
    if (owe && !caps.CAPABILITY) {
        NSK_COMPLAIN1("GetCapabilities does not return \"%s\" capability\n",
            CAPABILITY_STR);
        return NSK_FALSE;
    } else if (!owe && caps.CAPABILITY) {
        NSK_COMPLAIN1("GetCapabilities returns relinquished \"%s\" capability\n",
            CAPABILITY_STR);
        return NSK_FALSE;
    }

    return NSK_TRUE;
}

/* Check RelinquishCapabilities function
 */
static int checkRelinquishCapabilities() {
    jvmtiCapabilities caps;

    memset(&caps, 0, sizeof(caps));
    caps.CAPABILITY = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->RelinquishCapabilities(&caps)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ========================================================================== */

/* Check "can_suspend" functions
 */
static int checkSuspend() {
    jvmtiError err;

    NSK_DISPLAY0("Checking negative: SuspendThread\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY, jvmti->SuspendThread(thread)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: ResumeThread\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY, jvmti->ResumeThread(thread)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: SuspendThreadList\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->SuspendThreadList(1, &thread, &err)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: ResumeThreadList\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->ResumeThreadList(1, &thread, &err)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_signal_thread" functions
 */
static int checkSignalThread() {
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

    NSK_DISPLAY0("Checking negative: StopThread\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->StopThread(thread, exception)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: InterruptThread\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY, jvmti->InterruptThread(thread)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_get_owned_monitor_info" function
 */
static int checkGetOwnedMonitorInfo() {
    jint count;
    jobject *monitors = NULL;

    NSK_DISPLAY0("Checking negative: GetOwnedMonitorInfo\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->GetOwnedMonitorInfo(thread, &count, &monitors)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_get_current_contended_monitor" function
 */
static int checkGetCurrentContendedMonitor() {
    jobject monitor = NULL;

    NSK_DISPLAY0("Checking negative: GetCurrentContendedMonitor\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->GetCurrentContendedMonitor(thread, &monitor)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_pop_frame" function
 */
static int checkPopFrame() {
    NSK_DISPLAY0("Checking negative: PopFrame\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY, jvmti->PopFrame(thread)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_tag_objects" functions
 */

static jvmtiIterationControl JNICALL
HeapObject(jlong class_tag, jlong size, jlong *tag_ptr, void *user_data) {
    return JVMTI_ITERATION_ABORT;
}

static jvmtiIterationControl JNICALL
HeapRoot(jvmtiHeapRootKind root_kind, jlong class_tag, jlong size,
        jlong *tag_ptr, void *user_data) {
    return JVMTI_ITERATION_ABORT;
}

static jvmtiIterationControl JNICALL
StackReference(jvmtiHeapRootKind root_kind, jlong class_tag, jlong size,
        jlong *tag_ptr, jlong thread_tag, jint depth, jmethodID method,
        jint slot, void *user_data) {
    return JVMTI_ITERATION_ABORT;
}

static jvmtiIterationControl JNICALL
ObjectReference(jvmtiObjectReferenceKind reference_kind, jlong class_tag,
        jlong size, jlong *tag_ptr, jlong referrer_tag,
        jint referrer_index, void *user_data) {
    return JVMTI_ITERATION_ABORT;
}

static int checkHeapFunctions() {
    const jlong TAG_VALUE = (123456789L);
    jlong tag;
    jint count;
    jobject *res_objects = NULL;
    jlong *res_tags = NULL;
    jint dummy_user_data = 0;

    NSK_DISPLAY0("Checking negative: SetTag\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->SetTag(thread, TAG_VALUE)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: GetTag\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY, jvmti->GetTag(thread, &tag)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: GetObjectsWithTags\n");
    tag = TAG_VALUE;
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->GetObjectsWithTags(1, &tag, &count, &res_objects, &res_tags)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: IterateOverHeap\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->IterateOverHeap(JVMTI_HEAP_OBJECT_TAGGED, HeapObject, &dummy_user_data)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: IterateOverInstancesOfClass\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->IterateOverInstancesOfClass(klass,
                                               JVMTI_HEAP_OBJECT_UNTAGGED,
                                               HeapObject,
                                               &dummy_user_data)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: IterateOverObjectsReachableFromObject\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->IterateOverObjectsReachableFromObject(thread,
                                                         ObjectReference,
                                                         &dummy_user_data)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: IterateOverReachableObjects\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->IterateOverReachableObjects(HeapRoot,
                                               StackReference,
                                               ObjectReference,
                                               &dummy_user_data)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_access_local_variables" functions
 */
static int checkLocalVariableFunctions() {
    jint count;
    jvmtiLocalVariableEntry *local_variable_table = NULL;
    jobject object_value;
    jint int_value;
    jlong long_value;
    jfloat float_value;
    jdouble double_value;

    NSK_DISPLAY0("Checking negative: GetLocalVariableTable\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->GetLocalVariableTable(method, &count, &local_variable_table)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: GetLocalObject\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->GetLocalObject(thread, 0, 0, &object_value)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: GetLocalInt\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->GetLocalInt(thread, 0, 0, &int_value)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: GetLocalLong\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->GetLocalLong(thread, 0, 0, &long_value)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: GetLocalFloat\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->GetLocalFloat(thread, 0, 0, &float_value)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: GetLocalDouble\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->GetLocalDouble(thread, 0, 0, &double_value)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: SetLocalObject\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->SetLocalObject(thread, 0, 0, thread)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: SetLocalInt\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->SetLocalInt(thread, 0, 0, (jint)0)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: SetLocalLong\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->SetLocalLong(thread, 0, 0, (jlong)0)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: SetLocalFloat\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->SetLocalFloat(thread, 0, 0, (jfloat)0.0)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: SetLocalDouble\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->SetLocalDouble(thread, 0, 0, (jdouble)0.0)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_get_source_info" functions
 */
static int checkSourceInfoFunctions() {
    char *name;
    jint count;
    jvmtiLineNumberEntry *line_number_table = NULL;

    NSK_DISPLAY0("Checking negative: GetSourceFileName\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->GetSourceFileName(klass, &name)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: GetSourceDebugExtension\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->GetSourceDebugExtension(klass, &name)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: GetLineNumberTable\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->GetLineNumberTable(method, &count, &line_number_table)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_redefine_classes" functions
 */
static int checkRedefineClasses() {
    jvmtiClassDefinition class_def;

    NSK_DISPLAY0("Checking negative: RedefineClasses\n");
    class_def.klass = klass;
    class_def.class_byte_count = 0;
    class_def.class_bytes = NULL;
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->RedefineClasses(1, &class_def)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_get_monitor_info" function
 */
static int checkGetObjectMonitorUsage() {
    jvmtiMonitorUsage monitor_info;

    NSK_DISPLAY0("Checking negative: GetObjectMonitorUsage\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->GetObjectMonitorUsage(thread, &monitor_info)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_get_synthetic_attribute" functions
 */
static int checkIsSyntheticFunctions() {
    jboolean is_synthetic;

    NSK_DISPLAY0("Checking negative: IsFieldSynthetic\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->IsFieldSynthetic(klass, field, &is_synthetic)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: IsMethodSynthetic\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->IsMethodSynthetic(method, &is_synthetic)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_get_bytecodes" function
 */
static int checkGetBytecodes(int positive) {
    jint count;
    unsigned char *bytecodes;

    if (positive) {
        NSK_DISPLAY0("Checking positive: GetBytecodes\n");
        if (!NSK_JVMTI_VERIFY(jvmti->GetBytecodes(method, &count, &bytecodes)))
            return NSK_FALSE;
        if (!NSK_JVMTI_VERIFY(jvmti->Deallocate(bytecodes)))
            return NSK_FALSE;
    } else {
        NSK_DISPLAY0("Checking negative: GetBytecodes\n");
        if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
                jvmti->GetBytecodes(method, &count, &bytecodes)))
            return NSK_FALSE;
    }

    return NSK_TRUE;
}

/* Check "can_get_current_thread_cpu_time" function
 */
static int checkGetCurrentThreadCpuTime() {
    jvmtiTimerInfo info;
    jlong nanos;

    NSK_DISPLAY0("Checking negative: GetCurrentThreadCpuTimerInfo\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->GetCurrentThreadCpuTimerInfo(&info)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: GetCurrentThreadCpuTime\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->GetCurrentThreadCpuTime(&nanos)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_get_thread_cpu_time" function
 */
static int checkGetThreadCpuTime() {
    jvmtiTimerInfo info;
    jlong nanos;

    NSK_DISPLAY0("Checking negative: GetThreadCpuTimerInfo\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->GetThreadCpuTimerInfo(&info)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking negative: GetThreadCpuTime\n");
    if (!NSK_JVMTI_VERIFY_CODE(JVMTI_ERROR_MUST_POSSESS_CAPABILITY,
            jvmti->GetThreadCpuTime(thread, &nanos)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ========================================================================== */

/* agent algorithm
 */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* agentJNI, void* arg) {
    jni = agentJNI;

    /* wait for initial sync */
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!prepare()) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* testcase #1: check GetPotentialCapabilities */
    NSK_DISPLAY0("Testcase #1: check if GetPotentialCapabilities returns the capability\n");
    if (!checkGetPotentialCapabilities()) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* testcase #2: add the capability during Live phase */
    NSK_DISPLAY0("Testcase #2: add the capability during Live phase\n");
    if (!checkAddCapabilities()) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* testcase #3: check if GetCapabilities returns the capability */
    NSK_DISPLAY0("Testcase #3: check if GetCapabilities returns the capability\n");
    if (!checkGetCapabilities(NSK_TRUE)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* testcase #4: check that only correspondent function work */
    NSK_DISPLAY0("Testcase #4: check that only correspondent function work but not others\n");
    if (!checkSuspend())
        nsk_jvmti_setFailStatus();
    if (!checkSignalThread())
        nsk_jvmti_setFailStatus();
    if (!checkGetOwnedMonitorInfo())
        nsk_jvmti_setFailStatus();
    if (!checkGetCurrentContendedMonitor())
        nsk_jvmti_setFailStatus();
    if (!checkPopFrame())
        nsk_jvmti_setFailStatus();
    if (!checkHeapFunctions())
        nsk_jvmti_setFailStatus();
    if (!checkLocalVariableFunctions())
        nsk_jvmti_setFailStatus();
    if (!checkSourceInfoFunctions())
        nsk_jvmti_setFailStatus();
    if (!checkRedefineClasses())
        nsk_jvmti_setFailStatus();
    if (!checkGetObjectMonitorUsage())
        nsk_jvmti_setFailStatus();
    if (!checkIsSyntheticFunctions())
        nsk_jvmti_setFailStatus();
    if (!checkGetBytecodes(NSK_TRUE))
        nsk_jvmti_setFailStatus();
    if (!checkGetCurrentThreadCpuTime())
        nsk_jvmti_setFailStatus();
    if (!checkGetThreadCpuTime())
        nsk_jvmti_setFailStatus();

    /* testcase #5: relinquish the capability during Live phase */
    NSK_DISPLAY0("Testcase #5: relinquish the capability during Live phase\n");
    if (!checkRelinquishCapabilities()) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* testcase #6: check if GetCapabilities does not return the capability */
    NSK_DISPLAY0("Testcase #6: check if GetCapabilities does not return the capability\n");
    if (!checkGetCapabilities(NSK_FALSE)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* testcase #7: check that the capability function does not work */
    if (!checkGetBytecodes(NSK_FALSE))
        nsk_jvmti_setFailStatus();

    /* testcase #8: add back the capability and check with GetCapabilities */
    NSK_DISPLAY0("Testcase #8: add back the capability and check with GetCapabilities\n");
    if (!checkAddCapabilities()) {
        nsk_jvmti_setFailStatus();
        return;
    }
    if (!checkGetCapabilities(NSK_TRUE)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* testcase #9: exits with the capability has not been relinquished */
    NSK_DISPLAY0("Testcase #9: check if VM exits well with the capability has not been relinquished\n");

    /* resume debugee after last sync */
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/* agent library initialization
 */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_cm01t014(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_cm01t014(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_cm01t014(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60000;
    NSK_DISPLAY1("Timeout: %d msc\n", (int)timeout);

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

}
