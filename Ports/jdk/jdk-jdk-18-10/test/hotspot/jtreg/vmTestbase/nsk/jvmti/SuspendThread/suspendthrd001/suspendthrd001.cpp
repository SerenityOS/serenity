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

#include <string.h>
#include "jvmti.h"
#include "agent_common.h"
#include "jni_tools.h"
#include "jvmti_tools.h"

extern "C" {

/* ============================================================================= */

/* scaffold objects */
static jlong timeout = 0;

/* constant names */
#define THREAD_NAME     "TestedThread"

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    NSK_DISPLAY0("Wait for thread to start\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    /* perform testing */
    {
        jthread testedThread = NULL;

        NSK_DISPLAY1("Find thread: %s\n", THREAD_NAME);
        if (!NSK_VERIFY((testedThread =
                nsk_jvmti_threadByName(THREAD_NAME)) != NULL))
            return;
        NSK_DISPLAY1("  ... found thread: %p\n", (void*)testedThread);

        NSK_DISPLAY1("Suspend thread: %p\n", (void*)testedThread);
        if (!NSK_JVMTI_VERIFY(jvmti->SuspendThread(testedThread))) {
            nsk_jvmti_setFailStatus();
            return;
        }

        NSK_DISPLAY0("Let thread to run and finish\n");
        if (!nsk_jvmti_resumeSync())
            return;

        NSK_DISPLAY1("Get state vector for thread: %p\n", (void*)testedThread);
        {
            jint state = 0;

            if (!NSK_JVMTI_VERIFY(jvmti->GetThreadState(testedThread, &state))) {
                nsk_jvmti_setFailStatus();
            }
            NSK_DISPLAY2("  ... got state vector: %s (%d)\n",
                            TranslateState(state), (int)state);

            if ((state & JVMTI_THREAD_STATE_SUSPENDED) == 0) {
                NSK_COMPLAIN2("SuspendThread() does not turn on flag SUSPENDED:\n"
                              "#   state: %s (%d)\n",
                              TranslateState(state), (int)state);
                nsk_jvmti_setFailStatus();
            }
        }

        NSK_DISPLAY1("Resume thread: %p\n", (void*)testedThread);
        if (!NSK_JVMTI_VERIFY(jvmti->ResumeThread(testedThread))) {
            nsk_jvmti_setFailStatus();
        }

        NSK_DISPLAY0("Wait for thread to finish\n");
        if (!nsk_jvmti_waitForSync(timeout))
            return;

        NSK_DISPLAY0("Delete thread reference\n");
        NSK_TRACE(jni->DeleteGlobalRef(testedThread));
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_suspendthrd001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_suspendthrd001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_suspendthrd001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* add specific capabilities for suspending thread */
    {
        jvmtiCapabilities suspendCaps;
        memset(&suspendCaps, 0, sizeof(suspendCaps));
        suspendCaps.can_suspend = 1;
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&suspendCaps)))
            return JNI_ERR;
    }

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
