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
#include <stdlib.h>
#include <string.h>
#include <jvmti.h>
#include "agent_common.h"
#include <jni.h>
#include "jvmti_tools.h"
#include "jni_tools.h"
#include "JVMTITools.h"

extern "C" {

#define FILE_NAME "nsk/jvmti/scenarios/hotswap/HS204/hs204t003/MyThread"
#define CLASS_NAME  "Lnsk/jvmti/scenarios/hotswap/HS204/hs204t003/MyThread;"
#define FIELDNAME "intState"
#define TYPE "I"

static jint redefineNumber;
static jvmtiEnv * jvmti;
static jclass watchFieldClass;

JNIEXPORT void JNICALL callbackClassPrepare(jvmtiEnv *jvmti_env,
        JNIEnv* jni,
        jthread thread,
        jclass klass) {
    char * className;
    char * generic;

    className = NULL;
    generic   = NULL;
    redefineNumber=0;
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(klass, &className, &generic))) {
        NSK_DISPLAY0(" Agent :: Failed get class signature.\n");
        nsk_jvmti_agentFailed();
    } else {
        if ((strcmp(className, CLASS_NAME) == 0)) {
            jfieldID fieldId;
            if (!NSK_JNI_VERIFY(jni, (fieldId = jni->GetStaticFieldID(klass, FIELDNAME, TYPE)) != NULL)) {
                    NSK_DISPLAY0(" Agent :: Failed to get FieldId.\n");
                    nsk_jvmti_agentFailed();
            } else {
                if (!NSK_JVMTI_VERIFY(jvmti_env->SetFieldAccessWatch(klass, fieldId))) {
                    NSK_DISPLAY0(" Agent :: Failed to set watch point on a field.\n");
                    nsk_jvmti_agentFailed();
                } else {
                    nsk_jvmti_enableNotification(jvmti_env, JVMTI_EVENT_FIELD_ACCESS, NULL);
                    if (!NSK_JNI_VERIFY(jni, (watchFieldClass = (jclass) jni->NewGlobalRef(klass)) != NULL)) {
                        NSK_DISPLAY0(" Agent :: Failed to get global reference for class.\n");
                        nsk_jvmti_agentFailed();
                    }
                    NSK_DISPLAY0(" Agent :: SetFieldAccessWatch.\n");
                }
            }
            NSK_DISPLAY1(" Agent :: Leaving callbackClassPrepare for class = %s .\n", className);
        }
    }

    if (className != NULL) {
        if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char *)className))) {
            NSK_DISPLAY1(" Agent :: #error failed to Deallocate className = %s.", className);
            nsk_jvmti_agentFailed();
        }
    }

    if (generic != NULL) {
        if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char *)generic))) {
            NSK_DISPLAY1(" Agent :: #error failed to Deallocate class signature = %s.", generic);
            nsk_jvmti_agentFailed();
        }
    }
    return;
}

JNIEXPORT void JNICALL callbackFieldAccess(jvmtiEnv *jvmti_env,
        JNIEnv* jni,
        jthread thread,
        jmethodID method,
        jlocation location,
        jclass field_klass,
        jobject object,
        jfieldID field) {
    char fileName[512];
    char * className;
    char * generic;

    className = NULL;
    generic   = NULL;
    if (redefineNumber != 0) {
        return;
    }
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(field_klass, &className, &generic))) {
        NSK_DISPLAY0(" Agent :: Failed get class signature.\n");
        nsk_jvmti_agentFailed();
    } else {
        if ((strcmp(className, CLASS_NAME) == 0)) {
            jvmtiThreadInfo info;
            nsk_jvmti_getFileName(redefineNumber, FILE_NAME, fileName,
                    sizeof(fileName)/sizeof(char));
            if (nsk_jvmti_redefineClass(jvmti_env, field_klass, fileName)) {
                NSK_DISPLAY0(" Agent :: Successfully redefined.\n");
                redefineNumber++;
            } else {
                NSK_DISPLAY0(" Agent :: Failed to redefine.\n");
                nsk_jvmti_agentFailed();
            }
            NSK_DISPLAY0(" Agent :: Before attempting thread suspend.\n");
            if (!NSK_JVMTI_VERIFY(jvmti_env->GetThreadInfo(thread, &info))) {
                NSK_DISPLAY0(" Agent :: error getting thread info ");
                nsk_jvmti_agentFailed();
            } else {
                NSK_DISPLAY1(" Agent :: Thread Name = %s .\n", info.name);
            }
            if (!NSK_JVMTI_VERIFY(jvmti_env->SuspendThread(thread))) {
                NSK_DISPLAY0(" Agent :: Failed to suspend thread.\n");
                nsk_jvmti_agentFailed();
            }
        }
    }

    if (className != NULL) {
        if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char *)className))) {
            NSK_DISPLAY1(" Agent :: #error failed to Deallocate className = %s.", className);
            nsk_jvmti_agentFailed();
        }
    }

    if (generic != NULL) {
        if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char *)generic))) {
            NSK_DISPLAY1(" Agent :: #error failed to Deallocate class signature = %s.", generic);
            nsk_jvmti_agentFailed();
        }
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_hs204t003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_hs204t003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_hs204t003(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *vm, char *options, void *reserved) {
    if (!NSK_VERIFY (JNI_OK == vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_1))) {
        NSK_DISPLAY0("Agent :: Could not load JVMTI interface \n");
        return JNI_ERR;
    } else {
        jvmtiCapabilities caps;
        jvmtiEventCallbacks eventCallbacks;
        memset(&caps, 0, sizeof(caps));
        if (!nsk_jvmti_parseOptions(options)) {
            NSK_DISPLAY0(" NSK Failed to parse..");
            return JNI_ERR;
        }
        caps.can_redefine_classes             = 1;
        caps.can_generate_field_access_events = 1;
        caps.can_pop_frame                    = 1;
        caps.can_suspend                      = 1;
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
            NSK_DISPLAY0(" Agent :: Failed add required capabilities\n.");
            return JNI_ERR;
        }
        memset(&eventCallbacks, 0, sizeof(eventCallbacks));
        eventCallbacks.ClassPrepare = callbackClassPrepare;
        eventCallbacks.FieldAccess  = callbackFieldAccess;
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks)))) {
            NSK_DISPLAY0(" Agent :: Error occured while setting event call back \n");
            return JNI_ERR;
        }
        nsk_jvmti_enableNotification(jvmti, JVMTI_EVENT_CLASS_PREPARE, NULL);
    }
    return JNI_OK;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS204_hs204t003_hs204t003_popFrame(JNIEnv * jni,
        jobject object,
        jthread thread) {
    jboolean retvalue;
    jint state;
    retvalue = JNI_FALSE;
    if (!NSK_JVMTI_VERIFY(jvmti->GetThreadState(thread, &state))) {
        NSK_DISPLAY0(" Agent :: Error getting thread state.\n");
        nsk_jvmti_agentFailed();
    } else {
        if (state & JVMTI_THREAD_STATE_SUSPENDED) {
            NSK_DISPLAY0(" Agent :: Thread state = JVMTI_THREAD_STATE_SUSPENDED.\n");
            if (!NSK_JVMTI_VERIFY (jvmti->PopFrame(thread))) {
                NSK_DISPLAY0("#error Agent :: Jvmti failed to do popFrame.\n");
                nsk_jvmti_agentFailed();
            } else {
                if (!NSK_JVMTI_VERIFY (jvmti->ResumeThread(thread))) {
                    NSK_DISPLAY0(" Agent :: Error occured in resuming a thread.\n");
                    nsk_jvmti_agentFailed();
                } else {
                    jfieldID fieldId = jni->GetStaticFieldID(watchFieldClass, FIELDNAME, TYPE);
                    if (!NSK_JNI_VERIFY(jni, fieldId != NULL)) {
                        NSK_DISPLAY0(" Agent :: Failed to get FieldId before droping watchers.\n");
                        nsk_jvmti_agentFailed();
                    } else {
                        if (!NSK_JVMTI_VERIFY (jvmti->ClearFieldAccessWatch(watchFieldClass, fieldId))) {
                            NSK_DISPLAY0(" Agent :: failed to drop field watces.\n");
                            nsk_jvmti_agentFailed();
                        } else {
                            NSK_DISPLAY0(" Agent :: Sucessfully droped watches.\n");
                            retvalue = JNI_TRUE;
                        }
                    }
                }
            }
        } else {
            NSK_DISPLAY0(" Agent :: Thread should be suspended to its pop frame.\n");
        }
    }
    return retvalue;
}
}
