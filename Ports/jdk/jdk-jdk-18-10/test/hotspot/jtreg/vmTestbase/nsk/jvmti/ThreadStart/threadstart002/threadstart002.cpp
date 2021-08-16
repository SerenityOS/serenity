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

#include "nsk_tools.h"
#include "JVMTITools.h"
#include "jvmti_tools.h"
#include "agent_common.h"
#include "jni_tools.h"

extern "C" {

#define PASSED 0
#define STATUS_FAILED 2
#define WAIT_TIME 20000

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
/* volatile variables */
static jrawMonitorID agent_start_lock, thr_start_lock, thr_resume_lock, thr_event_lock;
static volatile jthread agent_thread = NULL;
static volatile jboolean terminate_debug_agent = JNI_FALSE;
static volatile jboolean debug_agent_timed_out = JNI_FALSE;
static volatile jboolean debug_agent_started = JNI_FALSE;
static volatile jthread next_thread = NULL;
static jvmtiThreadInfo inf;
static volatile int eventsCount = 0;
static volatile jint result = PASSED;

/*
    The agent runs special debugger agent (debug_agent) in a separate thread
    that operates on behalf of other threads.
    Upon receiving ThreadStart event, the debugger agent:
    - suspends the new thread
    - calls jni_DeleteGlobalRef with a jnienv * for that new thread
    - resumes the new thread
    Then the thread suspend status is checked in ThreadStart callback.

    The following monitors are used to synchronize debugger thread with other
    threads:
    1. agent_start_lock
       used to notify VMInit callback as well as ThreadStart callback
       that agent thread has been started.
    2. thr_event_lock
       used to guarantee that only one ThreadStart event is proceeded at
       the time.
    3. thr_start_lock
       used to notify agent thread that new thread has been started.
    4. thr_resume_lock
       used to notify ThreadStart callback that agent thread finished
       suspending and resuming the thread.

    So, the threads behaves as following:

VMInit                  | debug_agent                 |   ThreadStart
-------------------------------------------------------------------------
                        |                             |
 agent_start_lock.enter |                             | agent_start_lock.enter
                        |                             |
 ... create debug_agent | ... start                   |  while (!debug_agent)
 agent_start_lock.wait  |                             |    agent_start_lock.wait
                        | agent_start_lock.enter      |
                        | agent_start_lock.notifyAll  |
                        | agent_start_lock.exit       |
 agent_start_lock.exit  |                             |  agent_start_lock.exit
                        |                             |
                        |                             |  thr_event_lock.enter
                        |                             |
                        | thr_start_lock.enter        |  thr_start_lock.enter
                        | if (!next_thread)           |  thr_resume_lock.enter
                        |     thr_start_lock.wait     |
                        |                             |  ... next_thread = ...
                        |                             |  thr_start_lock.notify
                        |                             |  thr_start_lock.exit
                        |                             |
                        | ... suspend new thread      |  thr_resume_lock.wait
                        | ... resume new thread       |
                        |                             |
                        | thr_resume_lock.enter       |
                        | thr_resume_lock.notify      |
                        | thr_resume_lock.exit        |
                        |                             |  ... check next_thread state
                        |                             |  thr_resume_lock.exit
                        | thr_start_lock.exit         |
                                                      | thr_event_lock.exit


*/

static void JNICALL
debug_agent(jvmtiEnv* jvmti, JNIEnv* jni, void *p) {
    JNIEnv *env = jni;
    jint thrStat;
    jobject temp;

    /* Notify VMInit callback as well as ThreadStart callback (if any)
     * that agent thread has been started
     */
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(agent_start_lock))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("[agent] failed to acquire agent_start_lock\n");
    }

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorNotifyAll(agent_start_lock))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("[agent] failed to notify about agent_start_lock\n");
    }

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(agent_start_lock))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("[agent] failed to release agent_start_lock\n");
    }

    NSK_DISPLAY0(">>> [agent] agent created\n");

    debug_agent_started = JNI_TRUE;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(thr_start_lock))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("[agent] failed to enter thr_start_lock\n");
    }

    while (terminate_debug_agent != JNI_TRUE) {

        if (next_thread == NULL) {
            /* wait till new thread will be created and started */
            if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorWait(thr_start_lock, (jlong)0))) {
                result = STATUS_FAILED;
                NSK_COMPLAIN0("[agent] Failed while waiting thr_start_lock\n");
            }
        }

        if (next_thread != NULL) {
            /* hmm, why NewGlobalRef is called one more time???
             * next_thread = env->NewGlobalRef(next_thread);
             */
            if (!NSK_JVMTI_VERIFY(jvmti->SuspendThread(next_thread))) {
                result = STATUS_FAILED;
                NSK_COMPLAIN1("[agent] Failed to suspend thread#%d\n", eventsCount);
            }

            NSK_DISPLAY2(">>> [agent] thread#%d %s suspended ...\n", eventsCount, inf.name);

            /* these dummy calls provoke VM to hang */
            temp = env->NewGlobalRef(next_thread);
            env->DeleteGlobalRef(temp);

            if (!NSK_JVMTI_VERIFY(jvmti->ResumeThread(next_thread))) {
                result = STATUS_FAILED;
                NSK_COMPLAIN1("[agent] Failed to resume thread#%d\n", eventsCount);
            }

            NSK_DISPLAY2(">>> [agent] thread#%d %s resumed ...\n", eventsCount, inf.name);

            if (!NSK_JVMTI_VERIFY(jvmti->GetThreadState(next_thread, &thrStat))) {
                result = STATUS_FAILED;
                NSK_COMPLAIN1("[agent] Failed to get thread state for thread#%d\n", eventsCount);
            }

            NSK_DISPLAY3(">>> [agent] %s threadState=%s (%x)\n",
                    inf.name, TranslateState(thrStat), thrStat);

            if (thrStat & JVMTI_THREAD_STATE_SUSPENDED) {
                NSK_COMPLAIN1("[agent] \"%s\" was not resumed\n", inf.name);
                env->FatalError("[agent] could not recover");
            }

            env->DeleteGlobalRef(next_thread);
            next_thread = NULL;

            /* Notify ThreadStart callback that thread has been resumed */
            if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(thr_resume_lock))) {
                NSK_COMPLAIN0("[agent] Failed to acquire thr_resume_lock\n");
                result = STATUS_FAILED;
            }

            debug_agent_timed_out = JNI_FALSE;

            if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorNotify(thr_resume_lock))) {
                NSK_COMPLAIN0("[agent] Failed to notifing about thr_resume_lock\n");
                result = STATUS_FAILED;
            }

            if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(thr_resume_lock))) {
                NSK_COMPLAIN0("[agent] Failed to release thr_resume_lock\n");
                result = STATUS_FAILED;
            }
        }
    }

    /*
     * We don't call RawMonitorExit(thr_start_lock) in the loop so we don't
     * lose any notify calls.
     */
    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(thr_start_lock))) {
        NSK_COMPLAIN0("[agent] Failed to release thr_start_lock\n");
        result = STATUS_FAILED;
    }

    NSK_DISPLAY0(">>> [agent] done.\n");
}

void JNICALL ThreadStart(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread) {
    jint thrStat;
    jvmtiPhase phase;

    NSK_DISPLAY0(">>> [ThreadStart hook] start\n");

    /* skip if thread is 'agent thread' */
    if (env->IsSameObject(agent_thread, thread) == JNI_TRUE) {
        NSK_DISPLAY0(">>> [ThreadStart hook] skip agent thread\n");
        NSK_DISPLAY0(">>> [ThreadStart hook] end\n");
        return;
    }

    /* wait till agent thread is started
     * (otherwise can fail while waiting on thr_resume_thread due to timeout)
     */
    if (debug_agent_started != JNI_TRUE) {
        if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorEnter(agent_start_lock))) {
            NSK_COMPLAIN0("[ThreadStart hook] Failed to acquire agent_start_lock\n");
            result = STATUS_FAILED;
        }

        while (debug_agent_started != JNI_TRUE) {
            NSK_DISPLAY1(">>> [ThreadStart hook] waiting %dms for agent thread to start\n", WAIT_TIME);

            if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorWait(agent_start_lock, (jlong)WAIT_TIME))) {
                NSK_COMPLAIN0("[ThreadStart hook] Failed to wait for agent_start_lock\n");
                result = STATUS_FAILED;
            }
        }

        if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorExit(agent_start_lock))) {
            NSK_COMPLAIN0("[ThreadStart hook] Failed to release agent_start_lock\n");
            result = STATUS_FAILED;
        }
    }


    /* get JVMTI phase */
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetPhase(&phase))) {
        NSK_COMPLAIN0("[ThreadStart hook] Failed to get JVMTI phase\n");
        result = STATUS_FAILED;
    }

    /* Acquire event lock,
     * so only one StartThread callback could be proceeded at the time
     */
    if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorEnter(thr_event_lock))) {
        NSK_COMPLAIN0("[ThreadStart hook] Failed to acquire thr_event_lock\n");
        result = STATUS_FAILED;
    }

    {
        /* Get thread name */
        inf.name = (char*) "UNKNOWN";
        if (phase == JVMTI_PHASE_LIVE) {
            /* GetThreadInfo may only be called during the live phase */
            if (!NSK_JVMTI_VERIFY(jvmti_env->GetThreadInfo(thread, &inf))) {
                NSK_COMPLAIN1("[ThreadStart hook] Failed to get thread infor for thread#%d\n", eventsCount);
                result = STATUS_FAILED;
            }
        }

        NSK_DISPLAY2(">>> [ThreadStart hook] thread#%d: %s\n", eventsCount, inf.name);

        /* Acquire thr_start_lock */
        if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorEnter(thr_start_lock))) {
            NSK_COMPLAIN1("[ThreadStart hook] thread#%d failed to acquire thr_start_lock\n", eventsCount);
            result = STATUS_FAILED;
        }

            /* Acquire thr_resume_lock before we release thr_start_lock to prevent
             * debug agent from notifying us before we are ready.
         */
        if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorEnter(thr_resume_lock))) {
            NSK_COMPLAIN1("[ThreadStart hook] thread#%d failed to acquire thr_resume_lock\n", eventsCount);
            result = STATUS_FAILED;
        }

        /* Store thread */
        next_thread = env->NewGlobalRef(thread);
        debug_agent_timed_out = JNI_TRUE;

        /* Notify agent thread about new started thread and let agent thread to work with it */
        if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorNotify(thr_start_lock))) {
            NSK_COMPLAIN1("[ThreadStart hook] thread#%d failed to notify about thr_start_lock\n", eventsCount);
            result = STATUS_FAILED;
        }

        if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorExit(thr_start_lock))) {
            NSK_COMPLAIN1("[ThreadStart hook] thread#%d failed to release thr_start_lock\n", eventsCount);
            result = STATUS_FAILED;
        }

        /* Wait till this started thread will be resumed by agent thread */
        if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorWait(thr_resume_lock, (jlong)WAIT_TIME))) {
            NSK_COMPLAIN1("[ThreadStart hook] thread#%d failed while waiting for thr_resume_lock\n", eventsCount);
            result = STATUS_FAILED;
        }

        if (debug_agent_timed_out == JNI_TRUE) {
            NSK_COMPLAIN1("[ThreadStart hook] \"%s\": debug agent timed out\n", inf.name);
            env->FatalError("[ThreadStart hook] could not recover");
        }

        /* Release thr_resume_lock lock */
        if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorExit(thr_resume_lock))) {
            NSK_COMPLAIN1("[ThreadStart hook] thread#%d failed to release thr_resume_lock\n", eventsCount);
            result = STATUS_FAILED;
        }

        /* check that thread is not in SUSPENDED state */
        if (!NSK_JVMTI_VERIFY(jvmti_env->GetThreadState(thread, &thrStat))) {
            NSK_COMPLAIN1("[ThreadStart hook] Failed to get thread state for thread#%d\n", eventsCount);
            result = STATUS_FAILED;
        }

        NSK_DISPLAY2(">>> [ThreadStart hook] threadState=%s (%x)\n",
                TranslateState(thrStat), thrStat);

        if (thrStat & JVMTI_THREAD_STATE_SUSPENDED) {
            NSK_COMPLAIN1("[ThreadStart hook] \"%s\" was self-suspended\n", inf.name);
            env->FatalError("[ThreadStart hook] could not recover");
        }

        eventsCount++;
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorExit(thr_event_lock))) {
        NSK_COMPLAIN0("[ThreadStart hook] Failed to release thr_event_lock\n");
        result = STATUS_FAILED;
    }

    NSK_DISPLAY0(">>> [ThreadStart hook] end\n");
}

void JNICALL VMInit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr) {
    jclass cls = NULL;
    jmethodID mid = NULL;

    NSK_DISPLAY0(">>> VMInit event: start\n");

    if (!NSK_JVMTI_VERIFY(jvmti_env->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_START, NULL))) {
        NSK_COMPLAIN0("TEST FAILED: failed to enable JVMTI_EVENT_THREAD_START\n");
        return;
    }

    /* Start agent thread */
    if (!NSK_VERIFY((cls = env->FindClass("java/lang/Thread")) != NULL)) {
        result = STATUS_FAILED;
            NSK_COMPLAIN0("TEST FAILED: Cannot start agent thread: FindClass() failed\n");
        return;
    }


    if (!NSK_VERIFY((mid = env->GetMethodID(cls, "<init>", "()V")) != NULL)) {
        result = STATUS_FAILED;
            NSK_COMPLAIN0("TEST FAILED: Cannot start agent thread: GetMethodID() failed\n");
        return;
    }


    if (!NSK_VERIFY((agent_thread = env->NewObject(cls, mid)) != NULL)) {
        result = STATUS_FAILED;
            NSK_COMPLAIN0("Cannot start agent thread: NewObject() failed\n");
        return;
    }

    agent_thread = (jthread) env->NewGlobalRef(agent_thread);
    if (agent_thread == NULL) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("Cannot create global reference for agent_thread\n");
        return;
    }

    /*
     * Grab agent_start_lock before launching debug_agent to prevent
     * debug_agent from notifying us before we are ready.
     */

    if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorEnter(agent_start_lock))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILED: failed to enter agent_start_lock\n");
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->RunAgentThread(agent_thread, debug_agent, NULL, JVMTI_THREAD_NORM_PRIORITY))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILED: failed to create agent thread\n");
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorWait(agent_start_lock, (jlong)0))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILED: failed to wait agent_start_lock\n");
    }

    if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorExit(agent_start_lock))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILED: failed to exit agent_start_lock\n");
    }

    NSK_DISPLAY0(">>> VMInit event: end\n");
}

void JNICALL VMDeath(jvmtiEnv *jvmti_env, JNIEnv *env) {
    NSK_DISPLAY0(">>> VMDeath event\n");

    terminate_debug_agent = JNI_TRUE;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_threadstart002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_threadstart002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_threadstart002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL)) {
        NSK_COMPLAIN0("TEST FAILED: failed to create JVMTIEnv\n");
        return JNI_ERR;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->GetPotentialCapabilities(&caps))) {
        NSK_COMPLAIN0("TEST FAILED: failed to get potential capabilities\n");
        return JNI_ERR;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
        NSK_COMPLAIN0("TEST FAILED: failed to add capabilities during agent load\n");
        return JNI_ERR;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps))) {
        NSK_COMPLAIN0("TEST FAILED: failed to get capabilities\n");
        return JNI_ERR;
    }

    if (!caps.can_suspend) {
        NSK_DISPLAY0("WARNING: suspend/resume is not implemented\n");
    }

    /* create raw monitors */
    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("_agent_start_lock", &agent_start_lock))) {
        NSK_COMPLAIN0("TEST FAILED: failed to create agent_start_lock\n");
        return JNI_ERR;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("_thr_event_lock", &thr_event_lock))) {
        NSK_COMPLAIN0("TEST FAILED: failed to create thr_event_lock\n");
        return JNI_ERR;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("_thr_start_lock", &thr_start_lock))) {
        NSK_COMPLAIN0("TEST FAILED: failed to create thr_start_lock\n");
        return JNI_ERR;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("_thr_resume_lock", &thr_resume_lock))) {
        NSK_COMPLAIN0("TEST FAILED: failed to create thr_resume_lock\n");
        return JNI_ERR;
    }

    callbacks.VMInit = &VMInit;
    callbacks.VMDeath = &VMDeath;
    callbacks.ThreadStart = &ThreadStart;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks)))) {
        NSK_COMPLAIN0("TEST FAILED: failed to set event callbacks\n");
        return JNI_ERR;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL))) {
        NSK_COMPLAIN0("TEST FAILED: failed to enable JVMTI_EVENT_VM_INIT\n");
        return JNI_ERR;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL))) {
        NSK_COMPLAIN0("TEST FAILED: failed to enable JVMTI_EVENT_VM_DEATH\n");
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_ThreadStart_threadstart002_check(JNIEnv *env, jclass cls) {
    if (eventsCount == 0) {
        NSK_COMPLAIN0("None of thread start events!\n");
        result = STATUS_FAILED;
    }

    NSK_DISPLAY1(">>> total of thread start events: %d\n", eventsCount);

    return result;
}

}
