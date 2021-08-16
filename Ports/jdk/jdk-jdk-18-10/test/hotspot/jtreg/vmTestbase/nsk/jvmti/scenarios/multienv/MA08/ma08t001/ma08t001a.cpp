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

#include <stdlib.h>
#include <string.h>
#include "jni_tools.h"
#include "agent_common.h"
#include "jvmti_tools.h"

#define PASSED 0
#define STATUS_FAILED 2

extern "C" {

/* ========================================================================== */

/* scaffold objects */
static jlong timeout = 0;

/* test objects */
static jthread threadForStop = NULL;
static jthread threadForInterrupt = NULL;
static int ThreadDeathFlag = 0;
static int InterruptedExceptionFlag = 0;

/* ========================================================================== */

/** callback functions **/

const char* THREAD_DEATH_CLASS_SIG = "Ljava/lang/ThreadDeath;";
const char* INTERRUPTED_EXCEPTION_CLASS_SIG = "Ljava/lang/InterruptedException;";
static void JNICALL
Exception(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread,
        jmethodID method, jlocation location, jobject exception,
        jmethodID catch_method, jlocation catch_location) {
    jclass klass = NULL;
    char *signature = NULL;

    if (!NSK_JNI_VERIFY(jni_env, (klass = jni_env->GetObjectClass(exception)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(klass, &signature, NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (!NSK_VERIFY(signature != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    NSK_DISPLAY1("Exception event: %s\n", signature);

    if (jni_env->IsSameObject(threadForInterrupt, thread)) {
        if (strcmp(signature, INTERRUPTED_EXCEPTION_CLASS_SIG) == 0) {
            InterruptedExceptionFlag++;
        } else {
            NSK_COMPLAIN1("Unexpected exception in DebuggeeThreadForInterrupt: %s\n",
                signature);
            nsk_jvmti_setFailStatus();
        }
    } else if (jni_env->IsSameObject(threadForStop, thread)) {
        if (strcmp(signature, THREAD_DEATH_CLASS_SIG) == 0) {
            ThreadDeathFlag++;
        } else {
            NSK_COMPLAIN1("Unexpected exception in DebuggeeThreadForStop: %s\n",
                signature);
            nsk_jvmti_setFailStatus();
        }
    }

    jvmti_env->Deallocate((unsigned char*)signature);
}

/* ========================================================================== */

static int prepare(jvmtiEnv* jvmti, JNIEnv* jni) {
    const char* STOP_THREAD_NAME = "DebuggeeThreadForStop";
    const char* INTERRUPT_THREAD_NAME = "DebuggeeThreadForInterrupt";
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
        if (info.name != NULL) {
            if (strcmp(info.name, STOP_THREAD_NAME) == 0) {
                threadForStop = threads[i];
            } else if (strcmp(info.name, INTERRUPT_THREAD_NAME) == 0) {
                threadForInterrupt = threads[i];
            }
        }
    }

    /* deallocate threads list */
    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)threads)))
        return NSK_FALSE;

    if (threadForStop == NULL) {
        NSK_COMPLAIN0("DebuggeeThreadForStop not found");
        return NSK_FALSE;
    }

    if (threadForInterrupt == NULL) {
        NSK_COMPLAIN0("DebuggeeThreadForInterrupt not found");
        return NSK_FALSE;
    }

    if (!NSK_JNI_VERIFY(jni, (threadForStop = jni->NewGlobalRef(threadForStop)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (threadForInterrupt = jni->NewGlobalRef(threadForInterrupt)) != NULL))
        return NSK_FALSE;

    /* enable event */
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_EXCEPTION, NULL)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ========================================================================== */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!prepare(jvmti, jni)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* resume debugee and wait for sync */
    if (!nsk_jvmti_resumeSync())
        return;
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    NSK_DISPLAY1("ThreadDeath received: %d\n", ThreadDeathFlag);
    if (!NSK_VERIFY(ThreadDeathFlag))
        nsk_jvmti_setFailStatus();

    NSK_DISPLAY1("InterruptedException received: %d\n",
        InterruptedExceptionFlag);
    if (!NSK_VERIFY(InterruptedExceptionFlag))
        nsk_jvmti_setFailStatus();

    /* disable event */
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_EXCEPTION, NULL)))
        nsk_jvmti_setFailStatus();

    NSK_TRACE(jni->DeleteGlobalRef(threadForStop));
    NSK_TRACE(jni->DeleteGlobalRef(threadForInterrupt));

    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ma08t001a(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ma08t001a(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ma08t001a(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;
    jvmtiCapabilities caps;
    jvmtiEventCallbacks callbacks;

    NSK_DISPLAY0("Agent_OnLoad\n");

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    memset(&caps, 0, sizeof(caps));
    caps.can_generate_exception_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
        return JNI_ERR;
    }

    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.Exception = &Exception;
    if (!NSK_VERIFY(nsk_jvmti_init_MA(&callbacks)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

}
