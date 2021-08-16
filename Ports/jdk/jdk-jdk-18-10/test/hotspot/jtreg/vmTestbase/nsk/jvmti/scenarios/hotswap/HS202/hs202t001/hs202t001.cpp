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
#include <jni.h>
#include <jvmti.h>
#include "agent_common.h"
#include <string.h>
#include "jvmti_tools.h"
#include "JVMTITools.h"
/*
    hs202t001:
        1. Set breakpoints in several methods when Object.wait(),
        Object.notify(), Object.notifyAll() are in use in these methods.
        2. Upon reaching a breakpoint, enable SingleStep.
        3. Redefine an java.lang.Object class within SingleStep callback
        when one of its methods is called by the tested method.
        4. Pop a currently executed frame.

*/
extern "C" {
#define FILE_NAME "nsk/jvmti/scenarios/hotswap/HS202/hs203t001/MyObject"
#define CLASS_NAME "Lnsk/jvmti/scenarios/hotswap/HS202/hs203t001/MyObject;"
#define METHOD_NAME "leaveMonitor"
#define METHOD_SIGN "()V"
#define METHOD_NOTIFYALL "notifyAll"

static jvmtiEnv * jvmti;


JNIEXPORT void JNICALL callbackClassPrepare(jvmtiEnv *jvmti,
                                        JNIEnv* jni,
                                        jthread thread,
                                        jclass klass) {
    char * className;
    char * generic;
    jvmti->GetClassSignature(klass, &className, &generic);
    if (strcmp(className,CLASS_NAME) == 0) {
        jmethodID method;
        method = jni->GetMethodID(klass, METHOD_NAME, METHOD_SIGN);
        if (method == NULL) {
            nsk_printf("Agent:: Method is null ");
        } else {
            jlocation start;
            jlocation end;
            jvmtiError err ;
            err=jvmti->GetMethodLocation(method, &start, &end);
            if (err != JVMTI_ERROR_NONE) {
                nsk_printf(" ## Error occured %s \n",TranslateError(err));
            }else {
                nsk_printf("\n Start = %d and end = %d ", start , end);
                nsk_printf(" setting break points..");
                err= jvmti->SetBreakpoint(method, start);
                if (err != JVMTI_ERROR_NONE) {
                    nsk_printf(" ## Error occured %s \n",TranslateError(err));
                } else  {
                    nsk_printf(" NO ERRORS ");
                    if (nsk_jvmti_enableNotification(jvmti,JVMTI_EVENT_BREAKPOINT, NULL)) {
                        nsk_printf(" Enabled.. notification event ..\n");
                    }
                }
            }
        }
    }
}

/*
 *This event call back will be called when a filed
 *threadState is beeing upodated or beeing used in
 *the program java flow.
 *In this current example the code will be called
 */
void JNICALL callbackSingleStep(jvmtiEnv *jvmti_env,
        JNIEnv* jni,
        jthread thread,
        jmethodID method,
        jlocation location) {
    jvmtiError err;
    char * name;
    char * signature;
    char * generic ;
    err = JVMTI_ERROR_NONE;
    jvmti_env->GetMethodName(method, &name, &signature, &generic);
    if (strcmp(name,METHOD_NAME) == 0) { /* same method */
        jclass cls;
        jmethodID mem ;
        jvmti_env->GetMethodDeclaringClass(method, &cls);
        mem=jni->GetMethodID(cls,METHOD_NOTIFYALL,"()V");
        jni->CallVoidMethod(thread,mem);
    }

}

void JNICALL callbackBreakpoint(jvmtiEnv *jvmti_env,
        JNIEnv* jni_env,
        jthread thread,
        jmethodID method,
        jlocation location) {
    jvmtiError err;
    err = JVMTI_ERROR_NONE;
    if (nsk_jvmti_enableNotification(jvmti,JVMTI_EVENT_SINGLE_STEP, NULL)) {
        nsk_printf(" Enabled.. notification event ..");
    }
    err= jvmti->SetEventNotificationMode(JVMTI_DISABLE,
            JVMTI_EVENT_BREAKPOINT, NULL);
    if (err == JVMTI_ERROR_NONE) {
        nsk_printf(" Disabled notification..");
    }

}


#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_hs202t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_hs202t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_hs202t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *vm, char *options, void *reserved) {
    jvmtiError rc;
    jint code;
    nsk_printf("Agent:: VM.. Started..\n");
    code = vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_1);
    if (code != JNI_OK) {
        nsk_printf("Agent:: Could not load JVMTI interface \n");
        return JNI_ERR;
    } else {
        jvmtiCapabilities caps;
        jvmtiEventCallbacks eventCallbacks;
        memset(&caps, 0, sizeof(caps));
        if (!nsk_jvmti_parseOptions(options)) {
            nsk_printf("# error agent Failed to parse options \n");
            return JNI_ERR;
        }
        caps.can_redefine_classes = 1;
        caps.can_suspend = 1;
        caps.can_pop_frame = 1;
        caps.can_generate_all_class_hook_events = 1;
        caps.can_generate_compiled_method_load_events = 1;
        caps.can_generate_breakpoint_events=1;
        caps.can_generate_single_step_events=1;
        jvmti->AddCapabilities(&caps);
        memset(&eventCallbacks, 0, sizeof(eventCallbacks));
        eventCallbacks.ClassPrepare =callbackClassPrepare;
        eventCallbacks.SingleStep =callbackSingleStep;
        eventCallbacks.Breakpoint =callbackBreakpoint;
        rc = jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks));
        if (rc != JVMTI_ERROR_NONE) {
            nsk_printf(" ## Error occured %s \n",TranslateError(rc));
            return JNI_ERR;
        }
        if (nsk_jvmti_enableNotification(jvmti,JVMTI_EVENT_CLASS_PREPARE, NULL)) {
            nsk_printf("Agent :: NOTIFICATIONS ARE ENABLED \n");
        } else {
            nsk_printf(" Error in Eanableing Notifications..");
        }
    }
    return JNI_OK;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS202_hs202t001_hs202t001_popThreadFrame(JNIEnv * jni,
        jclass clas,
        jthread thread) {
    jvmtiError err ;
    jboolean retvalue;
    jint state;
    nsk_printf("Agent:: POPING THE FRAME....\n");
    retvalue = JNI_FALSE;
    jvmti->GetThreadState(thread, &state);
    if (state & JVMTI_THREAD_STATE_SUSPENDED) {
        err = jvmti->PopFrame(thread);
        if (err == JVMTI_ERROR_NONE) {
            nsk_printf("Agent:: NO Errors poped very well ..\n");
            retvalue=JNI_OK;
            return retvalue;
        } else if (err != JVMTI_ERROR_NONE) {
            nsk_printf(" ## Error occured %s \n",TranslateError(err));
        }
        nsk_printf("Agent:: some other error ..\n");
    } else {
        nsk_printf("Agent:: Thread was not suspened.. check for capabilities, and java method signature ");
    }
    return retvalue;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS202_hs202t001_hs202t001_resumeThread(JNIEnv * jni,
                                                                           jclass clas,
                                                                           jthread thread) {
    jvmtiError err ;
    jboolean retvalue;
    retvalue = JNI_FALSE;
    err = jvmti->ResumeThread(thread);
    if (err == JVMTI_ERROR_NONE) {
        nsk_printf(" Agent:: Thread Resumed.. \n");
        retvalue=JNI_OK;
    } else {
        nsk_printf(" Agent:: Failed.. to Resume the thread.\n");
    }
    return retvalue;
}

}
