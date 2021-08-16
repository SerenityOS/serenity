/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include <stdlib.h>
#include <jvmti.h>
#include "agent_common.h"
#include <jni.h>
#include <string.h>
#include "jvmti_tools.h"
#include "JVMTITools.h"
#include "jni_tools.h"

extern "C" {
#define FILE_NAME "nsk/jvmti/scenarios/hotswap/HS203/hs203t004/MyThread"
#define CLASS_NAME "Lnsk/jvmti/scenarios/hotswap/HS203/hs203t004/MyThread;"
#define METHOD_NAME "doTask2"

static jint redefineNumber;
static jvmtiEnv * jvmti;

JNIEXPORT void JNICALL callbackClassPrepare(jvmtiEnv *jvmti_env,
                                        JNIEnv* jni,
                                        jthread thread,
                                        jclass klass) {
    char * className;
    className=NULL;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(klass, &className, NULL))) {
        NSK_COMPLAIN0("#error Agent :: while getting classname.\n");
        nsk_jvmti_agentFailed();
    } else {
        if (strcmp(className, CLASS_NAME) == 0) {
            if (nsk_jvmti_enableNotification(jvmti_env, JVMTI_EVENT_COMPILED_METHOD_LOAD, NULL)) {
                NSK_DISPLAY0(" Agent :: notification enabled for COMPILED_METHOD_LOAD.\n");
                if (!NSK_JVMTI_VERIFY(jvmti_env->GenerateEvents(JVMTI_EVENT_COMPILED_METHOD_LOAD))) {
                    NSK_COMPLAIN0("#error Agent :: occured while enabling compiled method events.\n");
                    nsk_jvmti_agentFailed();
                }
            }
        }

        if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char *)className))) {
            NSK_COMPLAIN1("#error Agent :: failed to Deallocate className = %s.", className);
            nsk_jvmti_agentFailed();
        }
    }
}


JNIEXPORT void JNICALL callbackCompiledMethodLoad(jvmtiEnv *jvmti_env,
        jmethodID method,
        jint code_size,
        const void* code_addr,
        jint map_length,
        const jvmtiAddrLocationMap* map,
        const void* compile_info) {
    jclass threadClass;
    if (redefineNumber == 0) {
        if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodDeclaringClass(method, &threadClass))) {
            NSK_COMPLAIN0("#error Agent :: while geting the declaring class.\n");
            nsk_jvmti_agentFailed();
        } else {
            char *className;
            char *methodName;

            className = NULL;
            methodName = NULL;

            if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(threadClass, &className, NULL))) {
                NSK_COMPLAIN0("#error Agent :: while getting classname.\n");
                nsk_jvmti_agentFailed();
                return;
            }

            if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &methodName, NULL, NULL))) {
                NSK_COMPLAIN0("#error Agent :: while getting methodname.\n");
                nsk_jvmti_agentFailed();
                return;
            }

            if ((strcmp(className, CLASS_NAME) == 0) && (strcmp(methodName, METHOD_NAME) == 0)) {
                char fileName[512];

                NSK_DISPLAY2(" Agent :: Got CompiledMethodLoadEvent for class: %s, method: %s.\n", className, methodName);
                NSK_DISPLAY0(" Agent :: redefining class.\n");

                nsk_jvmti_getFileName(redefineNumber, FILE_NAME, fileName, sizeof(fileName)/sizeof(char));

                if (nsk_jvmti_redefineClass(jvmti_env, threadClass, fileName)) {
                    NSK_DISPLAY0(" Agent :: Successfully redefined.\n");
                    redefineNumber++;
                } else {
                    NSK_COMPLAIN0("#error Agent :: Failed to redefine.\n");
                    nsk_jvmti_agentFailed();
                }
            }

            if (className != NULL) {
                if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char *)className))) {
                    NSK_COMPLAIN1("#error Agent :: failed to Deallocate className = %s.", className);
                    nsk_jvmti_agentFailed();
                }
            }
            if (methodName != NULL) {
                if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char *)methodName))) {
                    NSK_COMPLAIN1("#error Agent :: failed to Deallocate methodName = %s.", methodName);
                    nsk_jvmti_agentFailed();
                }
            }
        }
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_hs203t004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_hs203t004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_hs203t004(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *vm, char *options, void *reserved) {
    redefineNumber=0;
    if (!NSK_VERIFY(JNI_OK == vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_1))) {
        NSK_DISPLAY0("#error Agent :: Could not load JVMTI interface.\n");
        return JNI_ERR;
        } else {
        jvmtiCapabilities caps;
        jvmtiEventCallbacks eventCallbacks;
        memset(&caps, 0, sizeof(caps));
        if (!nsk_jvmti_parseOptions(options)) {
            NSK_DISPLAY0("#error Agent ::  Failed to parse options.\n");
            return JNI_ERR;
        }
        caps.can_redefine_classes = 1;
        caps.can_suspend = 1;
        caps.can_pop_frame = 1;
        caps.can_generate_all_class_hook_events = 1;
        caps.can_generate_compiled_method_load_events = 1;
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
            NSK_DISPLAY0("#error Agent :: occured while adding capabilities.\n");
            return JNI_ERR;
        }
        memset(&eventCallbacks, 0, sizeof(eventCallbacks));
        eventCallbacks.ClassPrepare =callbackClassPrepare;
        eventCallbacks.CompiledMethodLoad=callbackCompiledMethodLoad;
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks)))) {
            NSK_COMPLAIN0("#error Agent :: occured while setting event callback.\n");
            return JNI_ERR;
        }
        if (nsk_jvmti_enableNotification(jvmti,JVMTI_EVENT_CLASS_PREPARE, NULL)) {
            NSK_DISPLAY0(" Agent :: Notifications are enabled.\n");
        } else {
            NSK_COMPLAIN0("#error Agent :: Error in enableing Notifications.\n");
            return JNI_ERR;
        }
    }
    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS203_hs203t004_hs203t004_suspendThread(JNIEnv * jni,
        jobject clas,
        jthread thread) {
    NSK_DISPLAY0(" Agent :: Suspending Thread.\n");
    if (NSK_JVMTI_VERIFY(jvmti->SuspendThread(thread))) {
        NSK_DISPLAY0(" Agent :: Succeded in suspending.\n");
    } else {
        NSK_COMPLAIN0("#error Agent :: occured while suspending thread.\n");
        nsk_jvmti_agentFailed();
    }
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS203_hs203t004_hs203t004_popThreadFrame(JNIEnv * jni,
                                                                                   jobject clas,
                                                                                   jthread thread) {
    jboolean retvalue;
    jint state;

    NSK_DISPLAY0(" Agent :: nsk.jvmti.scenarios.hotswap.HS203.hs203t004.popThreadFrame(...).\n");
    retvalue = JNI_FALSE;
    if (!NSK_JVMTI_VERIFY(jvmti->GetThreadState(thread, &state))) {
        NSK_COMPLAIN0("#error Agent :: while getting thread's state.\n");
        nsk_jvmti_agentFailed();
    } else {
        if (state & JVMTI_THREAD_STATE_SUSPENDED) {
            if (!NSK_JVMTI_VERIFY(jvmti->PopFrame(thread))) {
                NSK_DISPLAY0("#error Agent :: occured while poping thread's frame.\n");
                nsk_jvmti_agentFailed();
            } else {
                if (NSK_JVMTI_VERIFY(
                        jvmti->SetEventNotificationMode(JVMTI_DISABLE,
                                                        JVMTI_EVENT_COMPILED_METHOD_LOAD, NULL))) {
                    NSK_DISPLAY0(" Agent :: Disabled JVMTI_EVENT_COMPILED_METHOD_LOAD.\n");
                    retvalue = JNI_TRUE;
                } else {
                    NSK_COMPLAIN0("#error Agent :: Failed to disable JVMTI_EVENT_COMPILED_METHOD_LOAD.\n");
                    nsk_jvmti_agentFailed();
                }
            }
        } else {
            NSK_COMPLAIN0("#error Agent :: Thread was not suspened.\n");
            nsk_jvmti_agentFailed();
        }
    }
    return retvalue;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS203_hs203t004_hs203t004_resumeThread(JNIEnv * jni,
        jclass clas,
        jthread thread) {
    jboolean retvalue;

    retvalue = JNI_FALSE;
    if (NSK_JVMTI_VERIFY(jvmti->ResumeThread(thread))) {
        NSK_DISPLAY0(" Agent :: Thread resumed.\n");
        retvalue= JNI_TRUE;
    } else {
        NSK_COMPLAIN0("#error Agent :: Failed to resume the thread.\n");
        nsk_jvmti_agentFailed();
    }
    return retvalue;
}

}
