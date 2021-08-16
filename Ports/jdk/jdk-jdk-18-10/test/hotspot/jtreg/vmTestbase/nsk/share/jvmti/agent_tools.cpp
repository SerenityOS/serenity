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

#include <stdlib.h>
#include <string.h>

#include "native_thread.h"
#include "jni_tools.h"
#include "jvmti_tools.h"

extern "C" {

/* ============================================================================= */

/* Be careful: do not build shared library which will be linked with different
 * agent libs while global variables are used
 * Now the same source is used to build different agent libs, so these
 * variables are not shared between agents */

static jthread agentThread = NULL;
static jvmtiStartFunction agentThreadProc = NULL;
static void* agentThreadArg = NULL;


typedef enum { NEW, RUNNABLE, WAITING, SUSPENDED, TERMINATED } thread_state_t;

typedef struct agent_data_t {
    volatile thread_state_t thread_state;
    int last_debuggee_status;
    jrawMonitorID monitor;
} agent_data_t;

static agent_data_t agent_data;

static jvmtiEnv* jvmti_env = NULL;
static JavaVM* jvm = NULL;
static JNIEnv* jni_env = NULL;

static volatile int currentAgentStatus = NSK_STATUS_PASSED;

/* ============================================================================= */

void nsk_jvmti_setFailStatus() {
    currentAgentStatus = NSK_STATUS_FAILED;
}

int nsk_jvmti_isFailStatus() {
    return (nsk_jvmti_getStatus() != NSK_STATUS_PASSED);
}

jint nsk_jvmti_getStatus() {
    return currentAgentStatus;
}

/* ============================================================================= */
static jvmtiError init_agent_data(jvmtiEnv *jvmti_env, agent_data_t *data) {
    data->thread_state = NEW;
    data->last_debuggee_status = NSK_STATUS_PASSED;

    return jvmti_env->CreateRawMonitor("agent_data_monitor", &data->monitor);
}

/** Reset agent data to prepare for another run. */
void nsk_jvmti_resetAgentData() {
    rawMonitorEnter(jvmti_env, agent_data.monitor);
    /* wait for agentThreadWrapper() to finish */
    while (agent_data.thread_state != TERMINATED) {
        rawMonitorWait(jvmti_env, agent_data.monitor, 10);
    }
    agent_data.thread_state = NEW;
    agent_data.last_debuggee_status = NSK_STATUS_PASSED;
    rawMonitorExit(jvmti_env, agent_data.monitor);
}

static jvmtiError free_agent_data(jvmtiEnv *jvmti_env, agent_data_t *data) {
    return jvmti_env->DestroyRawMonitor(data->monitor);
}

/** Create JVMTI environment. */
jvmtiEnv* nsk_jvmti_createJVMTIEnv(JavaVM* javaVM, void* reserved) {
    jvm = javaVM;
    if (!NSK_VERIFY(javaVM->GetEnv((void **)&jvmti_env, JVMTI_VERSION_1_1) == JNI_OK)) {
        nsk_jvmti_setFailStatus();
        return NULL;
    }

    if (!NSK_JVMTI_VERIFY(init_agent_data(jvmti_env, &agent_data))) {
        nsk_jvmti_setFailStatus();
        return NULL;
    }

    return jvmti_env;
}

/** Dispose JVMTI environment */
static int nsk_jvmti_disposeJVMTIEnv(jvmtiEnv* jvmti_env) {
    if (jvmti_env != NULL) {
        if (!NSK_JVMTI_VERIFY(jvmti_env->DisposeEnvironment())) {
            nsk_jvmti_setFailStatus();
            return NSK_FALSE;
        }

        if (!NSK_JVMTI_VERIFY(free_agent_data(jvmti_env, &agent_data))) {
            nsk_jvmti_setFailStatus();
            return NSK_FALSE;
        }
    }
    return NSK_TRUE;
}

/** Get JNI environment for agent thread. */
JNIEnv* nsk_jvmti_getAgentJNIEnv() {
    return jni_env;
}

/** Get JVMTI environment for agent */
jvmtiEnv* nsk_jvmti_getAgentJVMTIEnv() {
    return jvmti_env;
}

/* ============================================================================= */
static void set_agent_thread_state(thread_state_t value) {
    rawMonitorEnter(jvmti_env, agent_data.monitor);
    agent_data.thread_state = value;
    rawMonitorNotify(jvmti_env, agent_data.monitor);
    rawMonitorExit(jvmti_env, agent_data.monitor);
}

/** Wrapper for user agent thread. */
static void JNICALL
agentThreadWrapper(jvmtiEnv* jvmti_env, JNIEnv* agentJNI, void* arg) {
    jni_env = agentJNI;

    /* run user agent proc */
    {
        set_agent_thread_state(RUNNABLE);

        NSK_TRACE((*agentThreadProc)(jvmti_env, agentJNI, agentThreadArg));

        set_agent_thread_state(TERMINATED);
    }

    /* finalize agent thread */
    {
        /* gelete global ref for agent thread */
        agentJNI->DeleteGlobalRef(agentThread);
        agentThread = NULL;
    }
}

/** Start wrapper for user agent thread. */
static jthread startAgentThreadWrapper(JNIEnv *jni_env, jvmtiEnv* jvmti_env) {
    const jint  THREAD_PRIORITY = JVMTI_THREAD_MAX_PRIORITY;
    const char* THREAD_NAME = "JVMTI agent thread";
    const char* THREAD_CLASS_NAME = "java/lang/Thread";
    const char* THREAD_CTOR_NAME = "<init>";
    const char* THREAD_CTOR_SIGNATURE = "(Ljava/lang/String;)V";

    jobject threadName = NULL;
    jclass threadClass = NULL;
    jmethodID threadCtor = NULL;
    jobject threadObject = NULL;
    jobject threadGlobalRef = NULL;

    if (!NSK_JNI_VERIFY(jni_env, (threadClass = jni_env->FindClass(THREAD_CLASS_NAME)) != NULL)) {
        return NULL;
    }

    if (!NSK_JNI_VERIFY(jni_env, (threadCtor =
            jni_env->GetMethodID(threadClass, THREAD_CTOR_NAME, THREAD_CTOR_SIGNATURE)) != NULL))
        return NULL;

    if (!NSK_JNI_VERIFY(jni_env, (threadName = jni_env->NewStringUTF(THREAD_NAME)) != NULL))
        return NULL;

    if (!NSK_JNI_VERIFY(jni_env, (threadObject =
            jni_env->NewObject(threadClass, threadCtor, threadName)) != NULL))
        return NULL;

    if (!NSK_JNI_VERIFY(jni_env, (threadGlobalRef =
            jni_env->NewGlobalRef(threadObject)) != NULL)) {
        jni_env->DeleteLocalRef(threadObject);
        return NULL;
    }
    agentThread = (jthread)threadGlobalRef;

    if (!NSK_JVMTI_VERIFY(
            jvmti_env->RunAgentThread(agentThread, &agentThreadWrapper, agentThreadArg, THREAD_PRIORITY))) {
        jni_env->DeleteGlobalRef(threadGlobalRef);
        jni_env->DeleteLocalRef(threadObject);
        return NULL;
    }
    return agentThread;
}

/** Register user agent thread with arg. */
int nsk_jvmti_setAgentProc(jvmtiStartFunction proc, void* arg) {
    agentThreadProc = proc;
    agentThreadArg = arg;
    return NSK_TRUE;
}

/** Get agent thread ref. */
jthread nsk_jvmti_getAgentThread() {
    return agentThread;
}

/** Run registered user agent thread via wrapper. */
static jthread nsk_jvmti_runAgentThread(JNIEnv *jni_env, jvmtiEnv* jvmti_env) {
    /* start agent thread wrapper */
    jthread thread = startAgentThreadWrapper(jni_env, jvmti_env);
    if (thread == NULL) {
        nsk_jvmti_setFailStatus();
        return NULL;
    }

    return thread;
}

/* ============================================================================= */

/** Sleep current thread. */
void nsk_jvmti_sleep(jlong timeout) {
    int seconds = (int)((timeout + 999) / 1000);
    THREAD_sleep(seconds);
}

/** Sync point called from Java code. */
static jint syncDebuggeeStatus(JNIEnv* jni_env, jvmtiEnv* jvmti_env, jint debuggeeStatus) {
    jint result = NSK_STATUS_FAILED;

    rawMonitorEnter(jvmti_env, agent_data.monitor);

    /* save last debugee status */
    agent_data.last_debuggee_status = debuggeeStatus;

    /* we don't enter if-stmt in second call */
    if (agent_data.thread_state == NEW) {
        if (nsk_jvmti_runAgentThread(jni_env, jvmti_env) == NULL)
            goto monitor_exit_and_return;

        /* SP2.2-w - wait for agent thread */
        while (agent_data.thread_state == NEW) {
            rawMonitorWait(jvmti_env, agent_data.monitor, 0);
        }
    }

    /* wait for sync permit */
    /* we don't enter loop in first call */
    while (agent_data.thread_state != WAITING && agent_data.thread_state != TERMINATED) {
        /* SP4.2-w - second wait for agent thread */
        rawMonitorWait(jvmti_env, agent_data.monitor, 0);
    }

    if (agent_data.thread_state != TERMINATED) {
        agent_data.thread_state = SUSPENDED;
        /* SP3.2-n - notify to start test */
        /* SP6.2-n - notify to end test */
        rawMonitorNotify(jvmti_env, agent_data.monitor);
    }
    else {
        NSK_COMPLAIN0("Debuggee status sync aborted because agent thread has finished\n");
        goto monitor_exit_and_return;
    }

    /* update status from debuggee */
    if (debuggeeStatus != NSK_STATUS_PASSED) {
        nsk_jvmti_setFailStatus();
    }

    while (agent_data.thread_state == SUSPENDED) {
        /* SP5.2-w - wait while testing */
        /* SP7.2 - wait for agent end */
        rawMonitorWait(jvmti_env, agent_data.monitor, 0);
    }

    agent_data.last_debuggee_status = nsk_jvmti_getStatus();
    result = agent_data.last_debuggee_status;

monitor_exit_and_return:
    rawMonitorExit(jvmti_env, agent_data.monitor);
    return result;
}

/** Wait for sync point with Java code. */
int nsk_jvmti_waitForSync(jlong timeout) {
    static const int inc_timeout = 1000;

    jlong t = 0;
    int result = NSK_TRUE;

    rawMonitorEnter(jvmti_env, agent_data.monitor);

    agent_data.thread_state = WAITING;

    /* SP2.2-n - notify agent is waiting and wait */
    /* SP4.1-n - notify agent is waiting and wait */
    rawMonitorNotify(jvmti_env, agent_data.monitor);

    while (agent_data.thread_state == WAITING) {
        /* SP3.2-w - wait to start test */
        /* SP6.2-w - wait to end test */
        rawMonitorWait(jvmti_env, agent_data.monitor, inc_timeout);

        if (timeout == 0) continue;

        t += inc_timeout;

        if (t >= timeout) break;
    }

    if (agent_data.thread_state == WAITING) {
        NSK_COMPLAIN1("No status sync occured for timeout: %" LL "d ms\n", timeout);
        nsk_jvmti_setFailStatus();
        result = NSK_FALSE;
    }

    rawMonitorExit(jvmti_env, agent_data.monitor);

    return result;
}

/** Resume java code suspended on sync point. */
int nsk_jvmti_resumeSync() {
    int result;
    rawMonitorEnter(jvmti_env, agent_data.monitor);

    if (agent_data.thread_state == SUSPENDED) {
        result = NSK_TRUE;
        agent_data.thread_state = RUNNABLE;
        /* SP5.2-n - notify suspend done */
        /* SP7.2-n - notify agent end */
        rawMonitorNotify(jvmti_env, agent_data.monitor);
    }
    else {
        NSK_COMPLAIN0("Debuggee was not suspended on status sync\n");
        nsk_jvmti_setFailStatus();
        result = NSK_FALSE;
    }

    rawMonitorExit(jvmti_env, agent_data.monitor);
    return NSK_TRUE;
}

/** Native function for Java code to provide sync point. */
JNIEXPORT jint JNICALL
Java_nsk_share_jvmti_DebugeeClass_checkStatus(JNIEnv* jni_env, jclass cls, jint debuggeeStatus) {
    jint status;
    NSK_TRACE(status = syncDebuggeeStatus(jni_env, jvmti_env, debuggeeStatus));
    return status;
}

/** Native function for Java code to reset agent data. */
JNIEXPORT void JNICALL
Java_nsk_share_jvmti_DebugeeClass_resetAgentData(JNIEnv* jni_env, jclass cls) {
    NSK_TRACE(nsk_jvmti_resetAgentData());
}

/* ============================================================================= */

/** Find loaded class by signature. */
jclass nsk_jvmti_classBySignature(const char signature[]) {
    jclass* classes = NULL;
    jint count = 0;
    jclass foundClass = NULL;
    int i;

    if (!NSK_VERIFY(signature != NULL)) {
        nsk_jvmti_setFailStatus();
        return NULL;
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetLoadedClasses(&count, &classes))) {
        nsk_jvmti_setFailStatus();
        return NULL;
    }

    for (i = 0; i < count; i++) {
        char* sig = NULL;
        char* generic = NULL;

        if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(classes[i], &sig, &generic))) {
            nsk_jvmti_setFailStatus();
            break;
        }

        if (sig != NULL && strcmp(signature, sig) == 0) {
            foundClass = classes[i];
        }

        if (!(NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)sig))
                && NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)generic)))) {
            nsk_jvmti_setFailStatus();
            break;
        }

        if (foundClass != NULL)
            break;
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)classes))) {
        nsk_jvmti_setFailStatus();
        return NULL;
    }

    if (!NSK_JNI_VERIFY(jni_env, (foundClass = (jclass)
                jni_env->NewGlobalRef(foundClass)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NULL;
    }

    return foundClass;
}

/** Find alive thread by name. */
jthread nsk_jvmti_threadByName(const char name[]) {
    jthread* threads = NULL;
    jint count = 0;
    jthread foundThread = NULL;
    int i;

    if (!NSK_VERIFY(name != NULL)) {
        nsk_jvmti_setFailStatus();
        return NULL;
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetAllThreads(&count, &threads))) {
        nsk_jvmti_setFailStatus();
        return NULL;
    }

    for (i = 0; i < count; i++) {
        jvmtiThreadInfo info;

        if (!NSK_JVMTI_VERIFY(jvmti_env->GetThreadInfo(threads[i], &info))) {
            nsk_jvmti_setFailStatus();
            break;
        }

        if (info.name != NULL && strcmp(name, info.name) == 0) {
            foundThread = threads[i];
            break;
        }
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)threads))) {
        nsk_jvmti_setFailStatus();
        return NULL;
    }

    if (!NSK_JNI_VERIFY(jni_env, (foundThread = (jthread)
                jni_env->NewGlobalRef(foundThread)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NULL;
    }

    return foundThread;
}


/* ============================================================================= */

/** Add all capabilities for finding line locations. */
int nsk_jvmti_addLocationCapabilities() {
    jvmtiCapabilities caps;

    memset(&caps, 0, sizeof(caps));
    caps.can_get_line_numbers = 1;
    if (!NSK_JVMTI_VERIFY(jvmti_env->AddCapabilities(&caps)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/** Add all capabilities for using breakpoints. */
int nsk_jvmti_addBreakpointCapabilities() {
    jvmtiCapabilities caps;

    if (!nsk_jvmti_addLocationCapabilities())
        return NSK_FALSE;

    memset(&caps, 0, sizeof(caps));
    caps.can_generate_breakpoint_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti_env->AddCapabilities(&caps)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/** Find line location. */
jlocation nsk_jvmti_getLineLocation(jclass cls, jmethodID method, int line) {
    jint count = 0;
    jvmtiLineNumberEntry* table = NULL;
    jlocation location = NSK_JVMTI_INVALID_JLOCATION;
    int i;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetLineNumberTable(method, &count, &table)))
        return NSK_JVMTI_INVALID_JLOCATION;

    for (i = 0; i < count; i++) {
        if (table[i].line_number == line) {
            location = table[i].start_location;
            break;
        }
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*)table)))
        return NSK_JVMTI_INVALID_JLOCATION;

    return location;
}

/** Set breakpoint to a line. */
jlocation nsk_jvmti_setLineBreakpoint(jclass cls, jmethodID method, int line) {
    jlocation location = NSK_JVMTI_INVALID_JLOCATION;

    if (!NSK_VERIFY((location =
            nsk_jvmti_getLineLocation(cls, method, line)) != NSK_JVMTI_INVALID_JLOCATION))
        return NSK_JVMTI_INVALID_JLOCATION;

    if (!NSK_JVMTI_VERIFY(jvmti_env->SetBreakpoint(method, location)))
        return NSK_JVMTI_INVALID_JLOCATION;

    return location;
}

/** Remove breakpoint from a line. */
jlocation nsk_jvmti_clearLineBreakpoint(jclass cls, jmethodID method, int line) {
    jlocation location = NSK_JVMTI_INVALID_JLOCATION;

    if (!NSK_VERIFY((location =
            nsk_jvmti_getLineLocation(cls, method, line)) != NSK_JVMTI_INVALID_JLOCATION))
        return NSK_JVMTI_INVALID_JLOCATION;

    if (!NSK_JVMTI_VERIFY(jvmti_env->ClearBreakpoint(method, location)))
        return NSK_JVMTI_INVALID_JLOCATION;

    return location;
}

/* ============================================================================= */

/** Enable or disable given events. */
int nsk_jvmti_enableEvents(jvmtiEventMode enable, int size, jvmtiEvent list[], jthread thread) {
    int i;

    for (i = 0; i < size; i++) {
        if (!NSK_JVMTI_VERIFY(jvmti_env->SetEventNotificationMode(enable, list[i], thread))) {
            nsk_jvmti_setFailStatus();
            return NSK_FALSE;
        }
    }
    return NSK_TRUE;
}

/* ============================================================================= */

typedef jint (JNICALL *checkStatus_type)(JNIEnv* jni_env, jclass cls, jint debuggeeStatus);

static checkStatus_type checkStatus_func = NULL;

/**
 * Proxy function to gain sequential access to checkStatus of each agent
 */
JNIEXPORT jint JNICALL
MA_checkStatus(JNIEnv* jni_env, jclass cls, jint debuggeeStatus) {
    jint status;

    NSK_TRACE(status = syncDebuggeeStatus(jni_env, jvmti_env, debuggeeStatus));
    return (*checkStatus_func)(jni_env, cls, status);
}

/**
 * nativeMethodBind callback:
 *      if needed, redirects checkStatus native method call
 */
static void JNICALL nativeMethodBind(jvmtiEnv* jvmti_env, JNIEnv *jni_env,
                              jthread thread, jmethodID mid,
                              void* address, void** new_address_ptr) {
    const char* BIND_CLASS_NAME = "Lnsk/share/jvmti/DebugeeClass;";
    const char* BIND_METHOD_NAME = "checkStatus";
    const char* BIND_METHOD_SIGNATURE = "(I)I";

    jvmtiPhase phase;
    jclass cls;
    char *class_sig = NULL;
    char *name = NULL;
    char *sig = NULL;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetPhase(&phase))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (phase != JVMTI_PHASE_START && phase != JVMTI_PHASE_LIVE)
        return;

    if (NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(mid, &name, &sig, NULL))) {
        if (strcmp(name, BIND_METHOD_NAME) == 0 &&
                strcmp(sig, BIND_METHOD_SIGNATURE) == 0) {

            if (NSK_JVMTI_VERIFY(jvmti_env->GetMethodDeclaringClass(mid, &cls))
             && NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(cls, &class_sig, NULL))
             && strcmp(class_sig, BIND_CLASS_NAME) == 0
             && address != (void*)Java_nsk_share_jvmti_DebugeeClass_checkStatus) {
                checkStatus_func = (checkStatus_type)address;
                NSK_TRACE(*new_address_ptr = (void*)MA_checkStatus);
            }
        }
    }

    if (name != NULL)
        jvmti_env->Deallocate((unsigned char*)name);

    if (sig != NULL)
        jvmti_env->Deallocate((unsigned char*)sig);

    if (class_sig != NULL)
        jvmti_env->Deallocate((unsigned char*)class_sig);
}

/**
 * Initialize multiple agent:
 *      establish processing of nativeMethodBind events
 */
int nsk_jvmti_init_MA(jvmtiEventCallbacks* callbacks) {

    if (callbacks == NULL) {
        NSK_COMPLAIN0("callbacks should not be NULL\n");
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }

    if (callbacks->NativeMethodBind != NULL) {
        NSK_COMPLAIN0("callbacks.NativeMethodBind should be NULL\n");
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }

    {
        jvmtiCapabilities caps;
        memset(&caps, 0, sizeof(caps));
        caps.can_generate_native_method_bind_events = 1;
        if (!NSK_JVMTI_VERIFY(jvmti_env->AddCapabilities(&caps)))
            return NSK_FALSE;
    }

    callbacks->NativeMethodBind = nativeMethodBind;
    if (!NSK_JVMTI_VERIFY(jvmti_env->SetEventCallbacks(callbacks, sizeof(jvmtiEventCallbacks))))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(
            jvmti_env->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_NATIVE_METHOD_BIND, NULL)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ============================================================================= */

int nsk_jvmti_isOptionalEvent(jvmtiEvent event) {

    return (event == JVMTI_EVENT_EXCEPTION)
        || (event == JVMTI_EVENT_EXCEPTION_CATCH)
        || (event == JVMTI_EVENT_SINGLE_STEP)
        || (event == JVMTI_EVENT_FRAME_POP)
        || (event == JVMTI_EVENT_BREAKPOINT)
        || (event == JVMTI_EVENT_FIELD_ACCESS)
        || (event == JVMTI_EVENT_FIELD_MODIFICATION)
        || (event == JVMTI_EVENT_METHOD_ENTRY)
        || (event == JVMTI_EVENT_METHOD_EXIT)
        || (event == JVMTI_EVENT_NATIVE_METHOD_BIND)
        || (event == JVMTI_EVENT_COMPILED_METHOD_LOAD)
        || (event == JVMTI_EVENT_COMPILED_METHOD_UNLOAD)
        || (event == JVMTI_EVENT_MONITOR_WAIT)
        || (event == JVMTI_EVENT_MONITOR_WAITED)
        || (event == JVMTI_EVENT_MONITOR_CONTENDED_ENTER)
        || (event == JVMTI_EVENT_MONITOR_CONTENDED_ENTERED)
        || (event == JVMTI_EVENT_GARBAGE_COLLECTION_START)
        || (event == JVMTI_EVENT_GARBAGE_COLLECTION_FINISH)
        || (event == JVMTI_EVENT_OBJECT_FREE)
        || (event == JVMTI_EVENT_VM_OBJECT_ALLOC);
}

/* ============================================================================= */

void nsk_jvmti_showPossessedCapabilities(jvmtiEnv *jvmti_env) {

    jvmtiCapabilities caps;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetCapabilities(&caps))) {
        return;
    }

    NSK_DISPLAY0("\n");
    NSK_DISPLAY0("Possessed capabilities:\n");
    NSK_DISPLAY0("-----------------------\n");
    if (caps.can_tag_objects)
        NSK_DISPLAY0("\tcan_tag_objects\n");
    if (caps.can_generate_field_modification_events)
        NSK_DISPLAY0("\tcan_generate_field_modification_events\n");
    if (caps.can_generate_field_access_events)
        NSK_DISPLAY0("\tcan_generate_field_access_events\n");
    if (caps.can_get_bytecodes)
        NSK_DISPLAY0("\tcan_get_bytecodes\n");
    if (caps.can_get_synthetic_attribute)
        NSK_DISPLAY0("\tcan_get_synthetic_attribute\n");
    if (caps.can_get_owned_monitor_info)
        NSK_DISPLAY0("\tcan_get_owned_monitor_info\n");
    if (caps.can_get_current_contended_monitor)
        NSK_DISPLAY0("\tcan_get_current_contended_monitor\n");
    if (caps.can_get_monitor_info)
        NSK_DISPLAY0("\tcan_get_monitor_info\n");
    if (caps.can_pop_frame)
        NSK_DISPLAY0("\tcan_pop_frame\n");
    if (caps.can_redefine_classes)
        NSK_DISPLAY0("\tcan_redefine_classes\n");
    if (caps.can_signal_thread)
        NSK_DISPLAY0("\tcan_signal_thread\n");
    if (caps.can_get_source_file_name)
        NSK_DISPLAY0("\tcan_get_source_file_name\n");
    if (caps.can_get_line_numbers)
        NSK_DISPLAY0("\tcan_get_line_numbers\n");
    if (caps.can_get_source_debug_extension)
        NSK_DISPLAY0("\tcan_get_source_debug_extension\n");
    if (caps.can_access_local_variables)
        NSK_DISPLAY0("\tcan_access_local_variables\n");
    if (caps.can_maintain_original_method_order)
        NSK_DISPLAY0("\tcan_maintain_original_method_order\n");
    if (caps.can_generate_single_step_events)
        NSK_DISPLAY0("\tcan_generate_single_step_events\n");
    if (caps.can_generate_exception_events)
        NSK_DISPLAY0("\tcan_generate_exception_events\n");
    if (caps.can_generate_frame_pop_events)
        NSK_DISPLAY0("\tcan_generate_frame_pop_events\n");
    if (caps.can_generate_breakpoint_events)
        NSK_DISPLAY0("\tcan_generate_breakpoint_events\n");
    if (caps.can_suspend)
        NSK_DISPLAY0("\tcan_suspend\n");
    if (caps.can_get_current_thread_cpu_time)
        NSK_DISPLAY0("\tcan_get_current_thread_cpu_time\n");
    if (caps.can_get_thread_cpu_time)
        NSK_DISPLAY0("\tcan_get_thread_cpu_time\n");
    if (caps.can_generate_method_entry_events)
        NSK_DISPLAY0("\tcan_generate_method_entry_events\n");
    if (caps.can_generate_method_exit_events)
        NSK_DISPLAY0("\tcan_generate_method_exit_events\n");
    if (caps.can_generate_all_class_hook_events)
        NSK_DISPLAY0("\tcan_generate_all_class_hook_events\n");
    if (caps.can_generate_compiled_method_load_events)
        NSK_DISPLAY0("\tcan_generate_compiled_method_load_events\n");
    if (caps.can_generate_monitor_events)
        NSK_DISPLAY0("\tcan_generate_monitor_events\n");
    if (caps.can_generate_vm_object_alloc_events)
        NSK_DISPLAY0("\tcan_generate_vm_object_alloc_events\n");
    if (caps.can_generate_native_method_bind_events)
        NSK_DISPLAY0("\tcan_generate_native_method_bind_events\n");
    if (caps.can_generate_garbage_collection_events)
        NSK_DISPLAY0("\tcan_generate_garbage_collection_events\n");
    if (caps.can_generate_object_free_events)
        NSK_DISPLAY0("\tcan_generate_object_free_events\n");

    NSK_DISPLAY0("\n");
}

/* ============================================================================= */

}
