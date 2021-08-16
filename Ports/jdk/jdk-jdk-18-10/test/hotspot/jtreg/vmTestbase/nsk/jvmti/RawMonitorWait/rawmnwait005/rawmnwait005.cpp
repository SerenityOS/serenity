/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2
#define MILLIS_PER_MINUTE (60 * 1000)

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jint result = PASSED;
static jboolean printdump = JNI_TRUE;
static jrawMonitorID monitor;
static jrawMonitorID wait_lock;
static jlong wait_time;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_rawmnwait005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_rawmnwait005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_rawmnwait005(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

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

    if (!caps.can_signal_thread) {
        printf("Warning: InterruptThread is not implemented\n");
    }

    return JNI_OK;
}

static void JNICALL
test_thread(jvmtiEnv* jvmti, JNIEnv* jni, void *unused) {
    jvmtiError err;
    const char* const thread_name = "test thread";

    // Once we hold this monitor we know we can't get interrupted
    // until we have called wait().
    err = jvmti->RawMonitorEnter(monitor);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorEnter#test) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    if (printdump == JNI_TRUE) {
        printf(">>> [%s] acquired lock for 'monitor' ...\n", thread_name);
    }

    // We can't get this monitor until the main thread has called wait() on it.
    err = jvmti->RawMonitorEnter(wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorEnter#wait) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> [%s] acquired lock for 'wait_lock' ...\n", thread_name);
        printf(">>> [%s] notifying main thread (wait_lock.notify) ...\n", thread_name);
    }

    err = jvmti->RawMonitorNotify(wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorWait#wait) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti->RawMonitorExit(wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorExit#wait) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> [%s] waiting for interrupt ...\n", thread_name);
    }

    err = jvmti->RawMonitorWait(monitor, wait_time);
    if (err != JVMTI_ERROR_INTERRUPT) {
        printf("Error expected: JVMTI_ERROR_INTERRUPT,\n");
        printf("\tactual: %s (%d)\n", TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti->RawMonitorExit(monitor);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorExit#test) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    // We can't reacquire this monitor until the main thread is waiting for us to
    // complete.
    err = jvmti->RawMonitorEnter(wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorEnter#wait) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> [%s] acquired lock for 'wait_lock' ...\n", thread_name);
        printf(">>> [%s] notifying main thread we are done ...\n", thread_name);
    }

    err = jvmti->RawMonitorNotify(wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorWait#wait) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti->RawMonitorExit(wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorExit#wait) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> [%s] all done\n", thread_name);
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RawMonitorWait_rawmnwait005_check(JNIEnv *env,
        jclass cls, jthread thr, jint wtime) {
    jvmtiError err;
    const char* const thread_name = "main thread";

    if (!caps.can_signal_thread) {
        return result;
    }

    wait_time = wtime * MILLIS_PER_MINUTE;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    err = jvmti->CreateRawMonitor("test monitor", &monitor);
    if (err != JVMTI_ERROR_NONE) {
        printf("(CreateRawMonitor#test) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return STATUS_FAILED;
    }

    // 'wait_lock' is used to notify current thread when child thread ('test_thread')
    //  is ready. This in particular means 'test_thread' is waiting for notification
    //  of the raw monitor 'monitor' and current thread can now interrupt 'test_thread'.
    //
    err = jvmti->CreateRawMonitor("wait lock", &wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(CreateRawMonitor#wait) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return STATUS_FAILED;
    }

    // get exclusive ownership of 'wait_lock' monitor before
    //  starting 'test_thread' to avoid following race condition:
    //   'test_thread'     |   current thread
    //   -------------------------------------
    //                     | RunAgentThread(..., test_thread, ...)
    //    wait_lock.enter  |
    //    wait_lock.notify |
    //                     | wait_lock.enter
    //                     | wait_lock.wait(0)
    //    ...              |
    //                     |  ... will wait forever ...
    //
    // See also 6399368 test bug.
    //
    err = jvmti->RawMonitorEnter(wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorEnter#wait) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return STATUS_FAILED;
    }
    if (printdump == JNI_TRUE) {
        printf(">>> [%s] acquired lock for 'wait_lock' ... \n", thread_name);
    }

    if (printdump == JNI_TRUE) {
        printf(">>> [%s] starting test thread ...\n", thread_name);
    }

    // This starts a daemon thread, so we need to synchronize with it
    // before we terminate - else the test will end before it checks
    // it was interrupted!

    err = jvmti->RunAgentThread(thr, test_thread, NULL,
                                JVMTI_THREAD_NORM_PRIORITY);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RunDebugThread) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> [%s] waiting for test thread to run (do wait_lock.wait)...\n", thread_name);
    }
    err = jvmti->RawMonitorWait(wait_lock, (jlong)0);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorWait#wait) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return STATUS_FAILED;
    }
    if (printdump == JNI_TRUE) {
        printf(">>> [%s] got notification from test thread ...\n", thread_name);
    }

    // Keep the wait_lock so we can wait again at the end.

    err = jvmti->RawMonitorEnter(monitor);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorEnter#test) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return STATUS_FAILED;
    }
    if (printdump == JNI_TRUE) {
        printf(">>> [%s] acquired lock for 'monitor' ... \n", thread_name);
    }

    if (printdump == JNI_TRUE) {
        printf(">>> [%s] interrupting test thread ...\n", thread_name);
    }

    err = jvmti->InterruptThread(thr);
    if (err != JVMTI_ERROR_NONE) {
        printf("(InterruptThread) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti->RawMonitorExit(monitor);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorExit#test) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> [%s] waiting for test thread to complete its wait and notify us ...\n", thread_name);
    }
    err = jvmti->RawMonitorWait(wait_lock, (jlong)0);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorWait#wait) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return STATUS_FAILED;
    }
    if (printdump == JNI_TRUE) {
        printf(">>> [%s] got final notification from test thread ...\n", thread_name);
    }

    err = jvmti->RawMonitorExit(wait_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorExit#wait) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> [%s] all done\n", thread_name);
    }

    return result;
}

}
