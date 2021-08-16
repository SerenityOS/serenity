/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* The test adds all capabilities suitable for profiling at OnLoad phase:
 *
 *   can_tag_objects
 *   can_get_owned_monitor_info
 *   can_get_current_contended_monitor
 *   can_get_monitor_info
 *   can_maintain_original_method_order
 *   can_get_current_thread_cpu_time
 *   can_get_thread_cpu_time
 *   can_generate_all_class_hook_events
 *   can_generate_compiled_method_load_events
 *   can_generate_monitor_events
 *   can_generate_vm_object_alloc_events
 *   can_generate_native_method_bind_events
 *   can_generate_garbage_collection_events
 *   can_generate_object_free_events
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
static jobject testedObject = NULL;
const jlong TESTED_TAG_VALUE = 5555555L;
static bool testedObjectNotified = false;


/* event counts */
static int ClassFileLoadHookEventsCount = 0;
static int CompiledMethodLoadEventsCount = 0;
static int CompiledMethodUnloadEventsCount = 0;
static int MonitorContendedEnterEventsCount = 0;
static int MonitorContendedEnteredEventsCount = 0;
static int MonitorWaitEventsCount = 0;
static int MonitorWaitedEventsCount = 0;
static int VMObjectAllocEventsCount = 0;
static int NativeMethodBindEventsCount = 0;
static int GarbageCollectionStartEventsCount = 0;
static int GarbageCollectionFinishEventsCount = 0;
static int ObjectFreeEventsCount = 0;

/* ========================================================================== */

/** callback functions **/

static void JNICALL
ClassFileLoadHook(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
        jclass class_beeing_redefined, jobject loader,
        const char* name, jobject protection_domain,
        jint class_data_len, const unsigned char* class_data,
        jint *new_class_data_len, unsigned char** new_class_data) {

    ClassFileLoadHookEventsCount++;
    NSK_DISPLAY1("ClassFileLoadHook event: %s\n", name);
}

static void JNICALL
CompiledMethodLoad(jvmtiEnv *jvmti_env, jmethodID method,
        jint code_size, const void* code_addr, jint map_length,
        const jvmtiAddrLocationMap* map, const void* compile_info) {
    char *name = NULL;
    char *signature = NULL;

    CompiledMethodLoadEventsCount++;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &name, &signature, NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY3("CompiledMethodLoad event: %s%s (0x%p)\n",
        name, signature, code_addr);
    if (name != NULL)
        jvmti_env->Deallocate((unsigned char*)name);
    if (signature != NULL)
        jvmti_env->Deallocate((unsigned char*)signature);
}

static void JNICALL
CompiledMethodUnload(jvmtiEnv *jvmti_env, jmethodID method,
        const void* code_addr) {
    char *name = NULL;
    char *sig = NULL;
    jvmtiError err;
    CompiledMethodUnloadEventsCount++;

    NSK_DISPLAY0("CompiledMethodUnload event received\n");
    // Check for the case that the class has been unloaded
    err = jvmti_env->GetMethodName(method, &name, &sig, NULL);
    if (err == JVMTI_ERROR_NONE) {
        NSK_DISPLAY3("for: \tmethod: name=\"%s\" signature=\"%s\"\n\tnative address=0x%p\n",
          name, sig, code_addr);
        jvmti_env->Deallocate((unsigned char*)name);
        jvmti_env->Deallocate((unsigned char*)sig);
    }
}

static void JNICALL
MonitorContendedEnter(jvmtiEnv *jvmti_env, JNIEnv* jni_env,
        jthread thread, jobject object) {
    jvmtiThreadInfo info;

    MonitorContendedEnterEventsCount++;

    /* get thread information */
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetThreadInfo(thread, &info))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY2("MonitorContendedEnter event: thread=\"%s\", object=0x%p\n",
        info.name, object);
}

static void JNICALL
MonitorContendedEntered(jvmtiEnv *jvmti_env, JNIEnv* jni_env,
        jthread thread, jobject object) {
    jvmtiThreadInfo info;

    MonitorContendedEnteredEventsCount++;

    /* get thread information */
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetThreadInfo(thread, &info))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY2("MonitorContendedEntered event: thread=\"%s\", object=0x%p\n",
        info.name, object);
}

static void JNICALL
MonitorWait(jvmtiEnv *jvmti_env, JNIEnv* jni_env,
        jthread thread, jobject object, jlong timeout) {
    jvmtiThreadInfo info;

    MonitorWaitEventsCount++;

    /* get thread information */
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetThreadInfo(thread, &info))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY2("MonitorWait event: thread=\"%s\", object=0x%p\n",
        info.name, object);
}

static void JNICALL
MonitorWaited(jvmtiEnv *jvmti_env, JNIEnv* jni_env,
        jthread thread, jobject object, jboolean timed_out) {
    jvmtiThreadInfo info;

    MonitorWaitedEventsCount++;

    /* get thread information */
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetThreadInfo(thread, &info))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY2("MonitorWaited event: thread=\"%s\", object=0x%p\n",
        info.name, object);
}

static void JNICALL
VMObjectAlloc(jvmtiEnv *jvmti_env, JNIEnv* jni_env,
              jthread thread, jobject object,
              jclass object_klass, jlong size) {
    char *signature;

    VMObjectAllocEventsCount++;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(object_klass, &signature, NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY2("VMObjectAlloc: \"%s\", size=%d\n", signature, size);
    if (signature != NULL)
        jvmti_env->Deallocate((unsigned char*)signature);
}

static void JNICALL
NativeMethodBind(jvmtiEnv* jvmti_env, JNIEnv *jni_env,
        jthread thread, jmethodID method, void* func, void** func_ptr) {
    jvmtiPhase phase;
    char *name = NULL;
    char *signature = NULL;

    NativeMethodBindEventsCount++;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetPhase(&phase))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (phase != JVMTI_PHASE_START && phase != JVMTI_PHASE_LIVE)
        return;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &name, &signature, NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    NSK_DISPLAY2("NativeMethodBind event: %s%s\n", name, signature);

    if (name != NULL)
        jvmti_env->Deallocate((unsigned char*)name);
    if (signature != NULL)
        jvmti_env->Deallocate((unsigned char*)signature);
}

static void JNICALL
GarbageCollectionStart(jvmtiEnv *jvmti_env) {
    GarbageCollectionStartEventsCount++;
    NSK_DISPLAY0("GarbageCollectionStart\n");
}

static void JNICALL
GarbageCollectionFinish(jvmtiEnv *jvmti_env) {
    GarbageCollectionFinishEventsCount++;
    NSK_DISPLAY0("GarbageCollectionFinish\n");
}

static void JNICALL
ObjectFree(jvmtiEnv *jvmti_env, jlong tag) {
    char buffer[32];

    ObjectFreeEventsCount++;
    NSK_DISPLAY1("ObjectFree event: tag=%s\n", jlong_to_string(tag, buffer));

    if (tag == TESTED_TAG_VALUE) {
      testedObjectNotified = true;
    }
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
    if (!NSK_VERIFY(caps.can_tag_objects))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_get_owned_monitor_info))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_get_current_contended_monitor))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_get_monitor_info))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_maintain_original_method_order))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_get_current_thread_cpu_time))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_get_thread_cpu_time))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_generate_all_class_hook_events))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_generate_compiled_method_load_events))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_generate_monitor_events))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_generate_vm_object_alloc_events))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_generate_native_method_bind_events))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_generate_garbage_collection_events))
        return NSK_FALSE;
    if (!NSK_VERIFY(caps.can_generate_object_free_events))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ========================================================================== */

/* Check "can_get_owned_monitor_info" function
 */
static int checkGetOwnedMonitorInfo(jvmtiEnv* jvmti) {
    jint count;
    jobject *monitors = NULL;

    NSK_DISPLAY0("Checking positive: GetOwnedMonitorInfo\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetOwnedMonitorInfo(thread, &count, &monitors)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_get_current_contended_monitor" function
 */
static int checkGetCurrentContendedMonitor(jvmtiEnv* jvmti) {
    jobject monitor = NULL;

    NSK_DISPLAY0("Checking positive: GetCurrentContendedMonitor\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetCurrentContendedMonitor(thread, &monitor)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_tag_objects" functions
 */

static jvmtiIterationControl JNICALL
HeapObject(jlong class_tag, jlong size, jlong *tag_ptr, void *user_data) {
    return JVMTI_ITERATION_CONTINUE;
}

static jvmtiIterationControl JNICALL
HeapRoot(jvmtiHeapRootKind root_kind, jlong class_tag, jlong size,
        jlong *tag_ptr, void *user_data) {
    return JVMTI_ITERATION_CONTINUE;
}

static jvmtiIterationControl JNICALL
StackReference(jvmtiHeapRootKind root_kind, jlong class_tag, jlong size,
        jlong *tag_ptr, jlong thread_tag, jint depth, jmethodID method,
        jint slot, void *user_data) {
    return JVMTI_ITERATION_CONTINUE;
}

static jvmtiIterationControl JNICALL
ObjectReference(jvmtiObjectReferenceKind reference_kind, jlong class_tag,
        jlong size, jlong *tag_ptr, jlong referrer_tag,
        jint referrer_index, void *user_data) {
    return JVMTI_ITERATION_CONTINUE;
}

static jvmtiIterationControl JNICALL
ThreadObjectReference(jvmtiObjectReferenceKind reference_kind, jlong class_tag,
        jlong size, jlong *tag_ptr, jlong referrer_tag,
        jint referrer_index, void *user_data) {
    static jlong ThreadObjectReferenceTagCount;
    *tag_ptr = ++ThreadObjectReferenceTagCount;
    return JVMTI_ITERATION_CONTINUE;
}

// Create the jni local ref in a new frame so it
// doesn't stay alive.
class NewFrame {
  JNIEnv* _jni;
 public:
  NewFrame(JNIEnv* jni) : _jni(jni) {
    _jni->PushLocalFrame(16);
  }
  ~NewFrame() {
    _jni->PopLocalFrame(NULL);
  }
};

static int checkObjectTagEvent(jvmtiEnv* jvmti, JNIEnv* jni) {
    jlong tag = TESTED_TAG_VALUE;
    jint count;
    jobject *res_objects = NULL;
    jlong *res_tags = NULL;

    NewFrame local_frame(jni);

    // Create a tested object to tag.
    if (!NSK_JNI_VERIFY(jni, (testedObject = jni->NewStringUTF("abcde")) != NULL))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: SetTag\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(testedObject, TESTED_TAG_VALUE)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: GetObjectsWithTags\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetObjectsWithTags(1, &tag, &count, &res_objects, &res_tags)))
        return NSK_FALSE;

    if (!NSK_VERIFY(count == 1))
        return NSK_FALSE;

    return NSK_TRUE;
}


// Test that after GC, the object was removed from the tag map table.
static int checkObjectFreeEvent(jvmtiEnv* jvmti) {
    jlong tag = TESTED_TAG_VALUE;
    jint count;
    jobject *res_objects = NULL;
    jlong *res_tags = NULL;

    // Make some GCs happen
    for (int i = 0; i < 5; i++) {
        if (!NSK_JVMTI_VERIFY(jvmti->ForceGarbageCollection()))
            return NSK_FALSE;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->GetObjectsWithTags(1, &tag, &count, &res_objects, &res_tags)))
        return NSK_FALSE;

    if (!NSK_VERIFY(count == 0))
        return NSK_FALSE;

    if (!NSK_VERIFY(testedObjectNotified))
        return NSK_FALSE;

    return NSK_TRUE;
}

static int checkHeapFunctions(jvmtiEnv* jvmti) {
    const jlong TAG_VALUE = (123456789L);
    jlong tag;
    jint count;
    jobject *res_objects = NULL;
    jlong *res_tags = NULL;
    jint dummy_user_data = 0;

    NSK_DISPLAY0("Checking positive: SetTag\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(thread, TAG_VALUE)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: GetTag\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetTag(thread, &tag)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: GetObjectsWithTags\n");
    tag = TAG_VALUE;
    if (!NSK_JVMTI_VERIFY(jvmti->GetObjectsWithTags(1, &tag, &count, &res_objects, &res_tags)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: IterateOverHeap\n");
    if (!NSK_JVMTI_VERIFY(
            jvmti->IterateOverHeap(JVMTI_HEAP_OBJECT_TAGGED, HeapObject, &dummy_user_data)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: IterateOverInstancesOfClass\n");
    if (!NSK_JVMTI_VERIFY(
            jvmti->IterateOverInstancesOfClass(klass,
                                               JVMTI_HEAP_OBJECT_UNTAGGED,
                                               HeapObject,
                                               &dummy_user_data)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: IterateOverObjectsReachableFromObject\n");
    if (!NSK_JVMTI_VERIFY(
            jvmti->IterateOverObjectsReachableFromObject(thread,
                                                         ThreadObjectReference,
                                                         &dummy_user_data)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: IterateOverReachableObjects\n");
    if (!NSK_JVMTI_VERIFY(
            jvmti->IterateOverReachableObjects(HeapRoot,
                                               StackReference,
                                               ObjectReference,
                                               &dummy_user_data)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_get_monitor_info" function
 */
static int checkGetObjectMonitorUsage(jvmtiEnv* jvmti) {
    jvmtiMonitorUsage monitor_info;

    NSK_DISPLAY0("Checking positive: GetObjectMonitorUsage\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetObjectMonitorUsage(thread, &monitor_info)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_get_current_thread_cpu_time" function
 */
static int checkGetCurrentThreadCpuTime(jvmtiEnv* jvmti) {
    jvmtiTimerInfo info;
    jlong nanos;

    NSK_DISPLAY0("Checking positive: GetCurrentThreadCpuTimerInfo\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetCurrentThreadCpuTimerInfo(&info)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: GetCurrentThreadCpuTime\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetCurrentThreadCpuTime(&nanos)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* Check "can_get_thread_cpu_time" function
 */
static int checkGetThreadCpuTime(jvmtiEnv* jvmti) {
    jvmtiTimerInfo info;
    jlong nanos;

    NSK_DISPLAY0("Checking positive: GetThreadCpuTimerInfo\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetThreadCpuTimerInfo(&info)))
        return NSK_FALSE;

    NSK_DISPLAY0("Checking positive: checkGetThreadCpuTime\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GetThreadCpuTime(thread, &nanos)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ========================================================================== */

/* Check generated events
 */
static int checkGeneratedEvents() {
    int result = NSK_TRUE;

    NSK_DISPLAY1("ClassFileLoadHook events received: %d\n",
        ClassFileLoadHookEventsCount);
    if (!NSK_VERIFY(ClassFileLoadHookEventsCount != 0))
        result = NSK_FALSE;

    NSK_DISPLAY1("CompiledMethodLoad events received: %d\n",
        CompiledMethodLoadEventsCount);
    if (CompiledMethodLoadEventsCount == 0) {
        NSK_DISPLAY0("# WARNING: no CompiledMethodLoad events\n");
        NSK_DISPLAY0("#    (VM might not compile any methods at all)\n");
    }

    NSK_DISPLAY1("CompiledMethodUnload events received: %d\n",
        CompiledMethodUnloadEventsCount);
    if (CompiledMethodUnloadEventsCount == 0) {
        NSK_DISPLAY0("# WARNING: no CompiledMethodUnload events\n");
        NSK_DISPLAY0("#    (VM might not compile any methods at all)\n");
    }

    NSK_DISPLAY1("MonitorContendedEnter events received: %d\n",
        MonitorContendedEnterEventsCount);
    if (!NSK_VERIFY(MonitorContendedEnterEventsCount != 0))
        result = NSK_FALSE;

    NSK_DISPLAY1("MonitorContendedEntered events received: %d\n",
        MonitorContendedEnteredEventsCount);
    if (!NSK_VERIFY(MonitorContendedEnteredEventsCount != 0))
        result = NSK_FALSE;

    NSK_DISPLAY1("MonitorWait events received: %d\n",
        MonitorWaitEventsCount);
    if (!NSK_VERIFY(MonitorWaitEventsCount != 0))
        result = NSK_FALSE;

    NSK_DISPLAY1("MonitorWaited events received: %d\n",
        MonitorWaitedEventsCount);
    if (!NSK_VERIFY(MonitorWaitedEventsCount != 0))
        result = NSK_FALSE;

    NSK_DISPLAY1("VMObjectAlloc events received: %d\n",
        VMObjectAllocEventsCount);
    if (!NSK_VERIFY(VMObjectAllocEventsCount != 0))
        result = NSK_FALSE;

    NSK_DISPLAY1("NativeMethodBind events received: %d\n",
        NativeMethodBindEventsCount);
    if (!NSK_VERIFY(NativeMethodBindEventsCount != 0))
        result = NSK_FALSE;

    NSK_DISPLAY1("GarbageCollectionStart events received: %d\n",
        GarbageCollectionStartEventsCount);
    if (!NSK_VERIFY(GarbageCollectionStartEventsCount != 0))
        result = NSK_FALSE;

    NSK_DISPLAY1("GarbageCollectionFinish events received: %d\n",
        GarbageCollectionFinishEventsCount);
    if (!NSK_VERIFY(GarbageCollectionFinishEventsCount != 0))
        result = NSK_FALSE;

    NSK_DISPLAY1("ObjectFree events received: %d\n",
        ObjectFreeEventsCount);
    if (!NSK_VERIFY(ObjectFreeEventsCount != 0))
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

    NSK_DISPLAY0("Testcase #2: check if all correspondent functions work\n");
    if (!checkGetOwnedMonitorInfo(jvmti))
        nsk_jvmti_setFailStatus();
    if (!checkGetCurrentContendedMonitor(jvmti))
        nsk_jvmti_setFailStatus();
    if (!checkHeapFunctions(jvmti))
        nsk_jvmti_setFailStatus();
    if (!checkGetObjectMonitorUsage(jvmti))
        nsk_jvmti_setFailStatus();
    if (!checkGetCurrentThreadCpuTime(jvmti))
        nsk_jvmti_setFailStatus();
    if (!checkGetThreadCpuTime(jvmti))
        nsk_jvmti_setFailStatus();

    if (!checkObjectTagEvent(jvmti, jni))
        nsk_jvmti_setFailStatus();

    NSK_TRACE(jni->DeleteGlobalRef(thread));

    /* resume debugee and wait for sync */
    if (!nsk_jvmti_resumeSync())
        return;
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    /* this will also flush any pending ObjectFree events for event check */
    NSK_DISPLAY0("Testcase #3: check if the object is freed in the tag map\n");
    if (!checkObjectFreeEvent(jvmti)) {
        nsk_jvmti_setFailStatus();
    }

    NSK_DISPLAY0("Testcase #4: check if the events are generated\n");
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
JNIEXPORT jint JNICALL Agent_OnLoad_cm02t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_cm02t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_cm02t001(JavaVM *jvm, char *options, void *reserved) {
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
    caps.can_tag_objects = 1;
    caps.can_get_owned_monitor_info = 1;
    caps.can_get_current_contended_monitor = 1;
    caps.can_get_monitor_info = 1;
    caps.can_maintain_original_method_order = 1;
    caps.can_get_current_thread_cpu_time = 1;
    caps.can_get_thread_cpu_time = 1;
    caps.can_generate_all_class_hook_events = 1;
    caps.can_generate_compiled_method_load_events = 1;
    caps.can_generate_monitor_events = 1;
    caps.can_generate_vm_object_alloc_events = 1;
    caps.can_generate_native_method_bind_events = 1;
    caps.can_generate_garbage_collection_events = 1;
    caps.can_generate_object_free_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    /* set event callbacks */
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.ClassFileLoadHook = &ClassFileLoadHook;
    callbacks.CompiledMethodLoad = &CompiledMethodLoad;
    callbacks.CompiledMethodUnload = &CompiledMethodUnload;
    callbacks.MonitorContendedEnter = &MonitorContendedEnter;
    callbacks.MonitorContendedEntered = &MonitorContendedEntered;
    callbacks.MonitorWait = &MonitorWait;
    callbacks.MonitorWaited = &MonitorWaited;
    callbacks.VMObjectAlloc = &VMObjectAlloc;
    callbacks.NativeMethodBind = &NativeMethodBind;
    callbacks.GarbageCollectionStart = &GarbageCollectionStart;
    callbacks.GarbageCollectionFinish = &GarbageCollectionFinish;
    callbacks.ObjectFree = &ObjectFree;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    /* enable events */
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(
            JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(
            JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_LOAD, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(
            JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_UNLOAD, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(
            JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTER, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(
            JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(
            JVMTI_ENABLE, JVMTI_EVENT_MONITOR_WAIT, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(
            JVMTI_ENABLE, JVMTI_EVENT_MONITOR_WAITED, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(
            JVMTI_ENABLE, JVMTI_EVENT_VM_OBJECT_ALLOC, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(
            JVMTI_ENABLE, JVMTI_EVENT_NATIVE_METHOD_BIND, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(
            JVMTI_ENABLE, JVMTI_EVENT_GARBAGE_COLLECTION_START, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(
            JVMTI_ENABLE, JVMTI_EVENT_GARBAGE_COLLECTION_FINISH, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(
            JVMTI_ENABLE, JVMTI_EVENT_OBJECT_FREE, NULL)))
        return JNI_ERR;

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

}
