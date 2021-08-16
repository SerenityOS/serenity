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

extern "C" {

#define MAX_DEPTH 1024

/* ========================================================================== */

static const int NUMBER_OF_SAMPLES = 1000;
static const int DISPLAYING_FREQUENCY = 100;
static const jlong SAMPLING_INTERVAL = 10;

/* scaffold objects */
static jlong timeout = 0;

/* test objects */
static jthread thread = NULL;
static jrawMonitorID waitLock = NULL;
static jfieldID field = NULL;
static jmethodID methodRun = NULL;
static jmethodID methodCatcher = NULL;
static jmethodID methodThrower = NULL;
static jint MAX_LADDER = 0;
static int sampleCount = 0;
static jint frameCount = 0;
static jvmtiFrameInfo frameBuffer[MAX_DEPTH];

/* ========================================================================== */

static int prepare(jvmtiEnv* jvmti, JNIEnv* jni) {
    const char* THREAD_NAME = "Debuggee Thread";
    jvmtiThreadInfo info;
    jthread *threads = NULL;
    jint threads_count = 0;
    jclass klass = NULL;
    jfieldID fid = NULL;
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

        if (info.name != NULL) {
            if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)info.name)))
                return NSK_FALSE;
        }
    }

    /* deallocate threads list */
    if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)threads)))
        return NSK_FALSE;

    if (thread == NULL) {
        NSK_COMPLAIN0("Debuggee thread not found");
        return NSK_FALSE;
    }

    /* get tested thread class */
    if (!NSK_JNI_VERIFY(jni, (klass = jni->GetObjectClass(thread)) != NULL))
        return NSK_FALSE;

    /* get tested thread field 'MAX_LADDER' */
    if (!NSK_JNI_VERIFY(jni, (fid = jni->GetStaticFieldID(klass, "MAX_LADDER", "I")) != NULL))
        return NSK_FALSE;

    MAX_LADDER = jni->GetStaticIntField(klass, fid);
    NSK_DISPLAY1("MAX_LADDER: %d\n", MAX_LADDER);

    /* get tested thread field 'depth' */
    if (!NSK_JNI_VERIFY(jni, (field = jni->GetFieldID(klass, "depth", "I")) != NULL))
        return NSK_FALSE;

    /* get tested thread method 'run' */
    if (!NSK_JNI_VERIFY(jni, (methodRun = jni->GetMethodID(klass, "run", "()V")) != NULL))
        return NSK_FALSE;

    /* get tested thread method 'catcher' */
    if (!NSK_JNI_VERIFY(jni, (methodCatcher =
            jni->GetMethodID(klass, "catcher", "(II)V")) != NULL))
        return NSK_FALSE;

    /* get tested thread method 'thrower' */
    if (!NSK_JNI_VERIFY(jni, (methodThrower= jni->GetMethodID(klass, "thrower", "(I)V")) != NULL))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("waitLock", &waitLock)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ============================================================================= */

static int wait_for(jvmtiEnv* jvmti, jlong millis) {

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(waitLock)))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorWait(waitLock, millis)))
        nsk_jvmti_setFailStatus();

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(waitLock)))
        return NSK_FALSE;

    return NSK_TRUE;
}

static int displayFrameInfo(jvmtiEnv* jvmti, jint i) {
    char buffer[32];
    char *name = NULL;
    char *signature = NULL;

    if (!NSK_JVMTI_VERIFY(jvmti->GetMethodName(frameBuffer[frameCount-1-i].method, &name, &signature, NULL)))
        return NSK_FALSE;

    NSK_DISPLAY4("    [%d] method: %s%s, location: %s\n", i, name,
        signature, jlong_to_string(frameBuffer[frameCount-1-i].location, buffer));
    if (name != NULL)
        jvmti->Deallocate((unsigned char*)name);
    if (signature != NULL)
        jvmti->Deallocate((unsigned char*)signature);

    return NSK_TRUE;
}

static int complainFrameInfo(jvmtiEnv* jvmti, jint i, jmethodID method) {
    char buffer[32];
    char *name = NULL;
    char *signature = NULL;

    if (!NSK_JVMTI_VERIFY(jvmti->GetMethodName(frameBuffer[frameCount-1-i].method, &name, &signature, NULL)))
        return NSK_FALSE;

    NSK_COMPLAIN3("    got method: %s%s, location: %s\n", name, signature,
        jlong_to_string(frameBuffer[frameCount-1-i].location, buffer));
    if (name != NULL)
        jvmti->Deallocate((unsigned char*)name);
    if (signature != NULL)
        jvmti->Deallocate((unsigned char*)signature);

    if (!NSK_JVMTI_VERIFY(jvmti->GetMethodName(method, &name, &signature, NULL)))
        return NSK_FALSE;

    NSK_COMPLAIN2("    expected method: %s%s\n", name, signature);

    return NSK_TRUE;
}

static int checkStackTrace(jvmtiEnv* jvmti, JNIEnv* jni) {
    int res = NSK_TRUE;
    jmethodID method = NULL;
    jint depth;
    jint i;
    int displayFlag =
        (nsk_getVerboseMode() && (sampleCount % DISPLAYING_FREQUENCY) == 0);

    if (!NSK_JVMTI_VERIFY(jvmti->SuspendThread(thread)))
        return NSK_FALSE;

    depth = jni->GetIntField(thread, field);

    /* get stack trace */
    if (!NSK_JVMTI_VERIFY(jvmti->GetStackTrace(thread, 0, MAX_DEPTH, frameBuffer, &frameCount))) {
        res = NSK_FALSE;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->ResumeThread(thread)))
        res = NSK_FALSE;

    if (res) {
        if (displayFlag) {
            NSK_DISPLAY3("Sample #%d, frameCount: %d, depth: %d\n",
                sampleCount, frameCount, depth);
        }
        for (i = 0; i < frameCount; i++) {
            if (displayFlag && !displayFrameInfo(jvmti, i))
                res = NSK_FALSE;
            if (i == 0) {
                method = methodRun;
            } else if (i <= (depth + 1)) {
                method = methodCatcher;
            } else if (i <= (MAX_LADDER + 2)) {
                method = methodThrower;
            } else {
                continue;
            }
            if (!NSK_VERIFY(frameBuffer[frameCount-1-i].method == method)) {
                NSK_COMPLAIN3("Sample #%d, depth=%d, wrong frame [%d]:\n",
                    sampleCount, depth, i);
                complainFrameInfo(jvmti, i, method);
                res = NSK_FALSE;
            }
        }
    }

    return res;
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

    /* sample stack of the tested thread */
    for (sampleCount = 0;
         sampleCount < NUMBER_OF_SAMPLES && !nsk_jvmti_isFailStatus();
         sampleCount++) {
        wait_for(jvmti, SAMPLING_INTERVAL);
        if (!checkStackTrace(jvmti, jni))
            nsk_jvmti_setFailStatus();
    }

    /* resume debugee after last sync */
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/* agent library initialization */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_sp07t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_sp07t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_sp07t002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;
    jvmtiCapabilities caps;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60000;
    NSK_DISPLAY1("Timeout: %d msc\n", (int)timeout);

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    memset(&caps, 0, sizeof(caps));
    caps.can_suspend = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

}
