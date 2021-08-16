/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "ExceptionCheckingJniEnv.hpp"
#include "jni_tools.h"
#include "jvmti_tools.h"

extern "C" {

/* scaffold objects */
static jvmtiEnv *jvmti = NULL;
static jlong timeout = 0;

#define TESTED_CLASS_NAME   "nsk/jvmti/scenarios/bcinstr/BI01/bi01t002a"
#define TOTAL_INSTRUMENTED_CLASSES 2

static int clsLoadedIdx=0;
static jint newClassSize[TOTAL_INSTRUMENTED_CLASSES];
static unsigned char* newClassBytes[TOTAL_INSTRUMENTED_CLASSES];
static jvmtiClassDefinition oldClassDef[TOTAL_INSTRUMENTED_CLASSES];

/* ============================================================================= */
/*
 * Class:     nsk_jvmti_scenarios_bcinstr_BI01_bi01t002
 * Method:    setNewByteCode
 * Signature: (I[B)Z
 */
JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_bcinstr_BI01_bi01t002_setNewByteCode(JNIEnv *jni_env,
                        jobject o, jint ind, jbyteArray byteCode) {

    ExceptionCheckingJniEnvPtr ec_jni(jni_env);
    jbyte* elements;
    jboolean isCopy;

    newClassSize[ind] = ec_jni->GetArrayLength(byteCode, TRACE_JNI_CALL);
    if (!NSK_VERIFY(newClassSize[ind] > 0)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("\t... got array size: %d\n", newClassSize[ind]);

    elements = ec_jni->GetByteArrayElements(byteCode, &isCopy, TRACE_JNI_CALL);
    NSK_DISPLAY1("\t... got elements list: 0x%p\n", (void*)elements);

    if (!NSK_JVMTI_VERIFY(jvmti->Allocate(newClassSize[ind], &newClassBytes[ind]))) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("\t... created bytes array: 0x%p\n", (void*)newClassBytes[ind]);

    {
        int j;
        for (j = 0; j < newClassSize[ind]; j++)
            newClassBytes[ind][j] = (unsigned char)elements[j];
    }
    NSK_DISPLAY1("\t... copied bytecode: %d bytes\n", (int)newClassSize[ind]);

    NSK_DISPLAY1("\t... release elements list: 0x%p\n", (void*)elements);
    NSK_TRACE(ec_jni->ReleaseByteArrayElements(byteCode, elements, JNI_ABORT, TRACE_JNI_CALL));
    NSK_DISPLAY0("\t... released\n");
    return NSK_TRUE;
}

/* ============================================================================= */
/*
 * Class:     nsk_jvmti_scenarios_bcinstr_BI01_bi01t002
 * Method:    setClass
 * Signature: (ILjava/lang/Class;)V
 */
JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_bcinstr_BI01_bi01t002_setClass(JNIEnv *jni_env,
                        jobject o, jint ind, jclass cls) {

    ExceptionCheckingJniEnvPtr ec_jni(jni_env);
    oldClassDef[ind].klass = (jclass) ec_jni->NewGlobalRef(cls, TRACE_JNI_CALL);
}

/* ============================================================================= */

/** Callback function for ClassFileLoadHook event. */
JNIEXPORT void JNICALL
cbClassFileLoadHook(jvmtiEnv *jvmti_env, JNIEnv* jni_env,
            jclass class_being_redefined, jobject loader, const char* name,
            jobject protection_domain, jint class_data_len,
            const unsigned char* class_data, jint* new_class_data_len,
            unsigned char** new_class_data) {

    if (name == NULL || strcmp(name, TESTED_CLASS_NAME)) {
        return;
    }

    NSK_DISPLAY3("CLASS_FILE_LOAD_HOOK event: %s\n\treceived bytecode: 0x%p:%d\n",
                        name, (void *)class_data, class_data_len);
    if (nsk_getVerboseMode()) {
        nsk_printHexBytes("   ", 16, class_data_len, class_data);
    }

    {
        /*store original byte code, it will be used to do final redefinition*/
        int j;
        unsigned char *arr;

        oldClassDef[clsLoadedIdx].class_byte_count = class_data_len;
        if (!NSK_JVMTI_VERIFY(jvmti_env->Allocate(class_data_len, &arr))) {
            nsk_jvmti_setFailStatus();
            return;
        }
        for (j = 0; j < class_data_len; j++) {
            arr[j] = class_data[j];
        }
        oldClassDef[clsLoadedIdx].class_bytes = arr;
    }

    *new_class_data_len = newClassSize[clsLoadedIdx];
    *new_class_data = newClassBytes[clsLoadedIdx];

    NSK_DISPLAY2("Replace with new bytecode: 0x%p:%d\n",
                                (void*)newClassBytes[clsLoadedIdx],
                                (int)newClassSize[clsLoadedIdx]);
    if (nsk_getVerboseMode()) {
        nsk_printHexBytes("   ", 16, newClassSize[clsLoadedIdx],
                                newClassBytes[clsLoadedIdx]);
    }
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* agentJNI, void* arg) {

    int i;

    /*Wait for debuggee to read new byte codes nsk_jvmti_waitForSync#1*/
    NSK_DISPLAY0("Wait for debuggee to read new byte codes nsk_jvmti_waitForSync#1\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    for (clsLoadedIdx=0; clsLoadedIdx < TOTAL_INSTRUMENTED_CLASSES; clsLoadedIdx++) {

        if (!nsk_jvmti_resumeSync())
            return;

        NSK_DISPLAY1("Wait for debuggee to load tested class by classLoader[%d]\n",
                            (clsLoadedIdx+1));
        /*Wait for debuggee to load next class nsk_jvmti_waitForSync#2*/
        if (!nsk_jvmti_waitForSync(timeout))
            return;
    }

    if (!nsk_jvmti_resumeSync())
        return;

    /*Wait for debuggee to check instrumentation code works nsk_jvmti_waitForSync#3*/
    NSK_DISPLAY0("Wait for debuggee to check instrumentation code works nsk_jvmti_waitForSync#3\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    NSK_DISPLAY0("Notification disabled for CLASS_FILE_LOAD_HOOK event\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_DISABLE,
                                                          JVMTI_EVENT_CLASS_FILE_LOAD_HOOK,
                                                          NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (!nsk_jvmti_resumeSync())
        return;

    /*Wait for debuggee to set classes to be redefined nsk_jvmti_waitForSync#4*/
    NSK_DISPLAY0("Wait for debuggee to set classes to be redefined nsk_jvmti_waitForSync#4\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    NSK_DISPLAY0("Redfine class with old byte code\n");
    for (i = 0; i < TOTAL_INSTRUMENTED_CLASSES; i++) {
        NSK_DISPLAY4("class definition %d:\n\t0x%p, 0x%p:%d\n",
                        i,
                        oldClassDef[i].klass,
                        oldClassDef[i].class_bytes,
                        oldClassDef[i].class_byte_count);
        if (nsk_getVerboseMode()) {
            nsk_printHexBytes("   ", 16, oldClassDef[i].class_byte_count,
                                    oldClassDef[i].class_bytes);
        }
    }
    if (!NSK_JVMTI_VERIFY(jvmti->RedefineClasses(TOTAL_INSTRUMENTED_CLASSES, oldClassDef))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (!nsk_jvmti_resumeSync())
        return;

    /*Wait for debuggee to check old byte code works nsk_jvmti_waitForSync#5*/
    NSK_DISPLAY0("Wait for debuggee to check old byte code works nsk_jvmti_waitForSync#5\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    for (i = 0; i < TOTAL_INSTRUMENTED_CLASSES; i++) {
        agentJNI->DeleteGlobalRef(oldClassDef[i].klass);
    }

    NSK_DISPLAY0("Let debuggee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;

}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_bi01t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_bi01t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_bi01t002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    jvmti = nsk_jvmti_createJVMTIEnv(jvm, reserved);
    if (!NSK_VERIFY(jvmti != NULL))
        return JNI_ERR;

    {
        jvmtiCapabilities caps;
        memset(&caps, 0, sizeof(caps));

        caps.can_redefine_classes = 1;
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
            return JNI_ERR;
    }

    NSK_DISPLAY0("Set callback for CLASS_FILE_LOAD_HOOK event\n");
    {
        jvmtiEventCallbacks callbacks;
        jint size = (jint)sizeof(callbacks);

        memset(&callbacks, 0, size);
        callbacks.ClassFileLoadHook = cbClassFileLoadHook;
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, size))) {
            return JNI_ERR;
        }
    }

    NSK_DISPLAY0("Set notification enabled for CLASS_FILE_LOAD_HOOK event\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                                          JVMTI_EVENT_CLASS_FILE_LOAD_HOOK,
                                                          NULL))) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */


}
