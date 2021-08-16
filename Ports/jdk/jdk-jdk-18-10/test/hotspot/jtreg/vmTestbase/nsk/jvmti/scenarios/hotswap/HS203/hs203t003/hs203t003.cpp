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
#include "jni_tools.h"
#include "JVMTITools.h"
/*
   hs203T003:
   1. Set FieldAccessWatch, FieldModificatoinWatch for a field.
   2. Upon access/modification of the field within a method, redefine
   a class with the changed field version, and pop a currently executed
   frame within FieldAccess/FieldModification callback.

*/
extern "C" {
#define DIR_NAME "newclass"
#define PATH_FORMAT "%s%02d/%s"

#define FILE_NAME "nsk/jvmti/scenarios/hotswap/HS203/hs203t003/MyThread"
#define CLASS_NAME "Lnsk/jvmti/scenarios/hotswap/HS203/hs203t003/MyThread;"
#define SEARCH_NAME "nsk/jvmti/scenarios/hotswap/HS203/hs203t003/MyThread"
#define FIELDNAME "threadState"
#define TYPE "I"

static jint redefineNumber;
static jvmtiEnv * jvmti;
static int redefineCnt=0;

JNIEXPORT void JNICALL callbackClassPrepare(jvmtiEnv *jvmti_env,
                                        JNIEnv* jni,
                                        jthread thread,
                                        jclass klass) {
    char * className;
    char * generic;
    redefineNumber=0;
    className=NULL;
    generic=NULL;
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(klass, &className, &generic))) {
        nsk_printf("#error Agent :: while getting classname Signature.\n");
        nsk_jvmti_agentFailed();
    } else {
        if (strcmp(className,CLASS_NAME) == 0) {
            jfieldID field;
            /* get the field id and set watch on that .*/
            if (!NSK_JNI_VERIFY(jni, (field = jni->GetFieldID(klass, FIELDNAME, TYPE)) != NULL)) {
                nsk_printf(" Agent :: (*JNI)->GetFieldID(jni, ...) returns `null`.\n");
                nsk_jvmti_agentFailed();
            } else  if (!NSK_JVMTI_VERIFY(jvmti_env->SetFieldAccessWatch(klass, field))) {
                nsk_printf("#error Agent :: occured while jvmti->SetFieldAccessWatch(...) .\n");
                nsk_jvmti_agentFailed();
            }
        }
    }
}

JNIEXPORT void JNICALL callbackFieldAccess(jvmtiEnv *jvmti_env,
                                                 JNIEnv* jni,
                                                 jthread thread,
                                                 jmethodID method,
                                                 jlocation location,
                                                 jclass field_klass,
                                                 jobject object,
                                                 jfieldID field) {
    jclass clas;
    char fileName[512];
    if (redefineCnt < 10) {
        redefineCnt++;
        return;
    }
    redefineNumber=0;
    if (!NSK_JNI_VERIFY(jni, (clas = jni->FindClass(SEARCH_NAME)) != NULL)) {
        nsk_printf(" Agent :: (*JNI)->FindClass(jni, %s) returns `null`.\n",SEARCH_NAME);
        nsk_jvmti_agentFailed();
    } else  {
        nsk_jvmti_getFileName(redefineNumber, FILE_NAME, fileName,
                                sizeof(fileName)/sizeof(char));
        if (!nsk_jvmti_redefineClass(jvmti_env, clas, fileName)) {
            nsk_printf(" Agent :: Failed to redefine.\n");
            nsk_jvmti_agentFailed();
        } else {
            nsk_printf(" Agent :: Redefined.\n");
            nsk_printf(" Agent :: Suspendeding thread.\n");
            /* pop the current working frame. */
            if (!NSK_JVMTI_VERIFY(jvmti_env->SuspendThread(thread))) {
                nsk_printf("#error Agent :: occured suspending Thread.\n");
                nsk_jvmti_agentFailed();
            } else {
                nsk_printf(" Agent :: Succeded in suspending.\n");
            }
        }
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_hs203t003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_hs203t003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_hs203t003(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *vm, char *options, void *reserved) {
    if (!NSK_VERIFY(JNI_OK == vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_1))) {
        nsk_printf(" Agent :: Could not load JVMTI interface.\n");
        return JNI_ERR;
    } else {
        jvmtiCapabilities caps;
        jvmtiEventCallbacks eventCallbacks;
        if (!nsk_jvmti_parseOptions(options)) {
            nsk_printf("#error Agent :: Failed to parse options.\n");
            return JNI_ERR;
        }
        memset(&caps, 0, sizeof(caps));
        caps.can_redefine_classes = 1;
        caps.can_suspend=1;
        caps.can_pop_frame=1;
        caps.can_generate_all_class_hook_events=1;
        caps.can_generate_field_access_events=1;
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
            nsk_printf("#error Agent :: while adding capabilities.\n");
            return JNI_ERR;
        }
        memset(&eventCallbacks, 0, sizeof(eventCallbacks));
        eventCallbacks.ClassPrepare =callbackClassPrepare;
        eventCallbacks.FieldAccess= callbackFieldAccess;
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks)))) {
            nsk_printf("#error Agent :: while setting event callbacks.\n");
            return JNI_ERR;
        }
        if (nsk_jvmti_enableNotification(jvmti,JVMTI_EVENT_CLASS_PREPARE, NULL) &&
                nsk_jvmti_enableNotification(jvmti,JVMTI_EVENT_FIELD_ACCESS, NULL)) {
            nsk_printf(" Agent :: Notifications are enabled.\n");
        } else {
            nsk_printf("#error Agent :: Eanableing Notifications.\n");
            return JNI_ERR;
        }
    }
    return JNI_OK;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS203_hs203t003_hs203t003_isSuspended(JNIEnv * jni,
        jclass clas,
        jthread thread) {
    jboolean retvalue;
    jint state;
    retvalue = JNI_FALSE;
    if (!NSK_JVMTI_VERIFY(jvmti->GetThreadState(thread, &state))) {
        nsk_printf(" Agent :: Error while getting thread state.\n");
        nsk_jvmti_agentFailed();
    } else {
        if (state & JVMTI_THREAD_STATE_SUSPENDED) {
          retvalue = JNI_TRUE;
        }
    }
    return retvalue;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS203_hs203t003_hs203t003_popThreadFrame(JNIEnv * jni,
        jclass clas,
        jthread thread) {
    jboolean retvalue;
    jint state;
    retvalue = JNI_FALSE;
    if (!NSK_JVMTI_VERIFY(jvmti->GetThreadState(thread, &state))) {
        nsk_printf(" Agent :: Error while getting thread state.\n");
        nsk_jvmti_agentFailed();
    } else {
        if (state & JVMTI_THREAD_STATE_SUSPENDED) {
            if (!NSK_JVMTI_VERIFY(jvmti->PopFrame(thread))) {
                nsk_printf("#error Agent :: while poping thread's frame.\n");
                nsk_jvmti_agentFailed();
            } else {
                nsk_printf(" Agent :: poped thread frame.\n");
                if (!NSK_JVMTI_VERIFY(
                        jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_FIELD_ACCESS, NULL))) {
                    nsk_printf("#error Agent :: failed to disable notification JVMTI_EVENT_FIELD ACCESS.\n");
                    nsk_jvmti_agentFailed();
                } else {
                    nsk_printf(" Agent :: Disabled notification JVMTI_EVENT_FIELD ACCESS. \n");
                    retvalue = JNI_TRUE;
                }
            }
        } else {
            nsk_printf("#error Agent :: Thread was not suspened.");
            nsk_jvmti_agentFailed();
        }
    }
    return retvalue;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS203_hs203t003_hs203t003_resumeThread(JNIEnv * jni,
        jclass clas,
        jthread thread) {
    jboolean retvalue;
    retvalue = JNI_FALSE;
    if (!NSK_JVMTI_VERIFY(jvmti->ResumeThread(thread))) {
        nsk_printf("#error Agent :: while resuming thread.\n");
        nsk_jvmti_agentFailed();
    } else {
        nsk_printf(" Agent :: Thread resumed.\n");
        retvalue= JNI_TRUE;
    }
    return retvalue;
}

}
