/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include <jvmti.h>
#include "agent_common.h"
#include <jni.h>
#include <string.h>

#include "jni_tools.h"
#include "jvmti_tools.h"


extern "C" {
static JNIEnv* jni = NULL;
static jvmtiEnv *jvmti = NULL;
static jlong timeout = 0;
static jint testStep;
static jint redefineNumber;
static unsigned char* newClassBytes;
static unsigned char* path;
static jthread testedThread;
static jclass testClass;
static jclass myTestClass;

#define NAME "nsk/jvmti/scenarios/hotswap/HS204/hs204t001/hs204t001R"
#define CLASS_NAME "Lnsk/jvmti/scenarios/hotswap/HS204/hs204t001/hs204t001R;"
#define PATH_TO_NEW_BYTECODE "pathToNewByteCode"
#define FILE_NAME "nsk/jvmti/scenarios/hotswap/HS204/hs204t001/hs204t001R"
static jint newClassSize;

char *getClassName(jvmtiEnv *jvmti, jclass  klass) {
    char * className;
    char * generic;
    if (!NSK_JVMTI_VERIFY(jvmti->GetClassSignature(klass, &className, &generic))) {
        nsk_jvmti_setFailStatus();
    }
    return className;
}

JNIEXPORT void JNICALL
callbackClassLoad(jvmtiEnv *jvmti_env,
        JNIEnv* jni_env,
        jthread thread,
        jclass klass) {
    char * name;
    name = getClassName(jvmti_env,klass);
    if ((strcmp(name,CLASS_NAME) == 0) && (redefineNumber == 1)) {
       char fileName[512];
        nsk_jvmti_getFileName(redefineNumber, FILE_NAME, fileName,
                        sizeof(fileName)/sizeof(char));
        NSK_DISPLAY1(">>>>>>CallbackClassLoad ... Name=%s...  >>\n",name);
        if (nsk_jvmti_redefineClass(jvmti, klass, fileName)) {
            NSK_DISPLAY0("\nMyClass :: Successfully redefined..\n");
            redefineNumber++;
        } else {
            NSK_COMPLAIN0("\nMyClass :: Failed to redefine ..\n");
        }
        /* if ((myTestClass = jni_env->NewGlobalRef(klass)) == NULL) {
           NSK_COMPLAIN0("Failed to create global ref...");
           }
         */
    }
}

JNIEXPORT void JNICALL
callbackClassPrepare(jvmtiEnv *jvmti_env,
        JNIEnv* jni_env,
        jthread thread,
        jclass klass) {
    char *  name;
    name = getClassName(jvmti_env, klass);
    if ((strcmp(name, CLASS_NAME) == 0) && (redefineNumber == 0)) {
        char fileName[512];
        nsk_jvmti_getFileName(redefineNumber, FILE_NAME, fileName,
                        sizeof(fileName)/sizeof(char));
        NSK_DISPLAY1(">>>>>>callbackClassPrepare ... Name=%s...  >>\n",name);
        if (nsk_jvmti_redefineClass(jvmti, klass, fileName)) {
            NSK_DISPLAY0("\nMyClass :: Successfully redefined..\n");
            redefineNumber++;
        } else {
            NSK_COMPLAIN0("\nMyClass :: Failed to redefine ..\n");
        }
        myTestClass = (jclass) jni_env->NewGlobalRef(klass);
        if (myTestClass == NULL) {
            NSK_COMPLAIN0("Failed to create global ref...");
        }
    }
}

JNIEXPORT void JNICALL
callbackClassFileLoadHock(jvmtiEnv *jvmti_env,
        JNIEnv* jni_env,
        jclass class_being_redefined,
        jobject loader,
        const char* name,
        jobject protection_domain,
        jint class_data_len,
        const unsigned char* class_data,
        jint* new_class_data_len,
        unsigned char** new_class_data) {
    if (name != NULL && strcmp(name, NAME) == 0 && (redefineNumber == 1)) {
        NSK_DISPLAY1(">>>>>>callbackClassFileLoadHock ... Name=%s...  >>\n",name);
        /*redefineClass(jvmti_env, myTestClass);*/
    }
}

JNIEXPORT void JNICALL
#ifdef STATIC_BUILD
Agent_OnUnload_hs204t001(JavaVM *jvm)
#else
Agent_OnUnload(JavaVM *jvm)
#endif
{
    NSK_DISPLAY0(" VM ... Going Down.. (C/C++) \n");
    return;
}

static void JNICALL agentProc(jvmtiEnv* jvmti, JNIEnv* agentJNI, void* arg) {
    redefineNumber = 0;
    jni = agentJNI;
    testStep=1;
    NSK_DISPLAY0("\n\n>>>> Debugge started, waiting for class loading \n");
    jni->DeleteGlobalRef(testClass);
    jni->DeleteGlobalRef(testedThread);
    NSK_DISPLAY0("Waiting for debuggee to become ready\n");
    if (!nsk_jvmti_waitForSync(timeout)) {
        return;
    }
    testStep = 1;
    NSK_DISPLAY0("\n\n>>>> Debugge started, waiting for class loading \n");
    if (!nsk_jvmti_resumeSync())
        return;
    NSK_DISPLAY0("Waiting for debuggee's threads to finish\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;
    jni->DeleteGlobalRef(testClass);
    jni->DeleteGlobalRef(testedThread);
    NSK_DISPLAY0("Let debuggee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_hs204t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_hs204t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_hs204t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *vm, char *options, void *reserved) {
    jint rc ;
    NSK_DISPLAY0(" VM.. Started..\n");
    rc=vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_1);
    if (rc != JNI_OK) {
        NSK_COMPLAIN0(" Could not load JVMTI interface \n");
    } else {
        /* Open simple block for better memor usage. */
        jvmtiCapabilities caps;
        memset(&caps, 0, sizeof(caps));

        /*
           set capabilities of
           1.ClassFileLoadHock,
           2.ClassLoad(doesn;t require any capabilities to set).
           3.ClassPrepare (doesn;t require any capabilitiesto set).
           4.Redefine (default).
           5.PopFrame.
         */
        caps.can_generate_all_class_hook_events=1;
        caps.can_access_local_variables = 1;
        caps.can_generate_single_step_events=1;
        caps.can_redefine_classes = 1;
        caps.can_suspend = 1;
        caps.can_pop_frame=1;
        caps.can_generate_all_class_hook_events=1;
        jvmti->AddCapabilities(&caps);
        /*
           set the method and other functions..
         */
        {
            jvmtiEventCallbacks eventCallbacks;
            memset(&eventCallbacks, 0, sizeof(eventCallbacks));
            eventCallbacks.ClassLoad = callbackClassLoad;
            eventCallbacks.ClassPrepare=callbackClassPrepare;
            eventCallbacks.ClassFileLoadHook=callbackClassFileLoadHock;
            rc=jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks));
            if (rc != JVMTI_ERROR_NONE) {
                NSK_COMPLAIN0("Error setting event callbacks");
                return JNI_ERR;
            }
        }
        {
            nsk_jvmti_enableNotification(jvmti, JVMTI_EVENT_SINGLE_STEP, testedThread);
            nsk_jvmti_enableNotification(jvmti, JVMTI_EVENT_CLASS_LOAD, testedThread);
            nsk_jvmti_enableNotification(jvmti, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, testedThread);
            nsk_jvmti_enableNotification(jvmti, JVMTI_EVENT_CLASS_PREPARE, testedThread);
            nsk_jvmti_enableNotification(jvmti, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK,testedThread);
        }

        if (!nsk_jvmti_setAgentProc(agentProc, NULL)) {
            NSK_COMPLAIN0("setAgentProc failed");
        }
        if (!nsk_jvmti_parseOptions(options)) {
            NSK_COMPLAIN0("Cannot parse options");
        }
        NSK_DISPLAY1("Wait time: %d\n",nsk_jvmti_getWaitTime());
        timeout=nsk_jvmti_getWaitTime();
        NSK_DISPLAY1(" returning back.. enter timeout-->%d  \n",timeout);
        return JNI_OK;
    }
    // TODO: shouldn't we return JNI_ERR if GetEnv failed?
    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS204_hs204t001_hs204t001_setThread(JNIEnv * env,
                         jclass klass,
             jobject thread) {
    NSK_DISPLAY0(" Inside the setThread Method");
    if (!NSK_JNI_VERIFY(env, (testClass = (jclass) env->NewGlobalRef(klass)) != NULL))
        nsk_jvmti_setFailStatus();
    if (!NSK_JNI_VERIFY(env, (testedThread = env->NewGlobalRef(thread)) != NULL))
        nsk_jvmti_setFailStatus();
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS204_hs204t001_hs204t001_suspendThread(JNIEnv * env,
        jclass klass,
        jobject thread) {
    jint state;
    NSK_DISPLAY0("---suspend thread .. \n");
    if (jvmti->GetThreadState(thread, &state) == JVMTI_ERROR_NONE) {
        NSK_DISPLAY0(" No Errors in finding state of the thread.\n");
        if (state & JVMTI_THREAD_STATE_ALIVE) {
            NSK_DISPLAY0(" Thread state is alive .. So can be suspend should be possible ..\n");
            nsk_jvmti_disableNotification(jvmti, JVMTI_EVENT_SINGLE_STEP, thread);
            if (!NSK_JVMTI_VERIFY(jvmti->SuspendThread(thread))) {
                NSK_COMPLAIN0("TEST FAILED: unable to suspend the thread \n");
                nsk_jvmti_setFailStatus();
                return NSK_FALSE;
            } else {
                NSK_DISPLAY0(" Sucessfully suspended Thread..\n");
            }
        } else {
            NSK_COMPLAIN0("Was not able to suspend a thread..\n");
            return NSK_FALSE;
        }
    }
    return NSK_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS204_hs204t001_hs204t001_popFrame(JNIEnv * env,
        jclass klass,
        jthread thread) {
    jint state;
    NSK_DISPLAY0("Inside pop_Frame method.....\n");
    if (jvmti->GetThreadState(thread, &state) == JVMTI_ERROR_NONE) {
        NSK_DISPLAY0(" Got the state of thread \n");
        if (state & JVMTI_THREAD_STATE_SUSPENDED) {
            NSK_DISPLAY0(" Thread is already in suspended mode..\n");
            if (!NSK_JVMTI_VERIFY(jvmti->PopFrame(thread))) {
                NSK_COMPLAIN0(" TEST FAILED: UNABLE TO POP FRAME \n");
                nsk_jvmti_setFailStatus();
                return NSK_FALSE;
            } else {
                NSK_DISPLAY0(" Poped frame safely..");
            }
            /* We should resume that thread for next execution.. */
            if (!NSK_JVMTI_VERIFY(jvmti->ResumeThread(thread))) {
                NSK_COMPLAIN0(" TEST FAILED: UNABLE TO Resume thread \n");
                nsk_jvmti_setFailStatus();
                return NSK_FALSE;
            } else {
                NSK_DISPLAY0(" Resumed.. thread for next set of executions...");
            }
        } else {
            NSK_DISPLAY0(" Thread is not in Suspened State for poping its status..");
        }
    }
    return NSK_TRUE;
}
}
