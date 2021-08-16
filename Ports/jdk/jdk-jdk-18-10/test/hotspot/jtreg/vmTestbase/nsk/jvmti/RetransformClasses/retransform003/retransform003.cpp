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
#include <string.h>
#include <stdlib.h>
#include <jvmti.h>
#include "agent_common.h"
#include <jvmti_tools.h>
#include "JVMTITools.h"

extern "C" {

/* ============================================================================= */

static jvmtiEnv *jvmti = NULL;

/* Class, which contains the following callback method:
 *    static public void callback(String className, int agentID) */
static const char *CALLBACK_CLASS_NAME = "nsk.jvmti.RetransformClasses.retransform003";

/* Classes, belonging to the package defined in the TRIGGER variable will be retransformed */
static const char *TRIGGER = "nsk/share/jvmti/RetransformClasses"; //

/* Agent identifier */
static jint agent_id = -1;


/* ============================================================================= */

/* Used to explicitly initiate class retransformation process from Java code
 *
 * Parameters:
 *    jclass klass                      - class, which possess this native method
 *                                        (nsk.jvmti.RetransformClasses.retransform003)
 *
 *    jclass class_for_retransformation - class, which should be retransformed
 */
JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_RetransformClasses_retransform003_forceLoadedClassesRetransformation(
        JNIEnv *jni
        , jclass klass
        , jclass class_for_retransformation
        )
{
    if (!NSK_JVMTI_VERIFY(jvmti->RetransformClasses(1, &class_for_retransformation)))
        return JNI_FALSE;

    return JNI_TRUE;
}


/* ============================================================================= */

/* Class retransformation hook */
static void JNICALL
ClassFileLoadHook (
        jvmtiEnv *jvmti
        , JNIEnv *jni
        , jclass class_being_redefined
        , jobject loader
        , const char* name
        , jobject protection_domain
        , jint class_data_len
        , const unsigned char* class_data
        , jint *new_class_data_len
        , unsigned char** new_class_data
        )
{
    jclass loader_class;
    jclass callback_class;
    jmethodID method_id;
    jstring class_name_string;

    // Check whether currently retransformed class belongs to the package we are interested in
    if (name == NULL || strncmp(TRIGGER, name,strlen(name) < strlen(TRIGGER) ? strlen(name) : strlen(TRIGGER)))
    {
        return;
    }

    // Get ant the invoke callback function
    if (!NSK_VERIFY((loader_class = jni->GetObjectClass(loader)) != NULL))
        return;

    if (!NSK_VERIFY((method_id = jni->GetMethodID(
            loader_class, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;")) != NULL))
        return;

    if (!NSK_VERIFY((class_name_string = jni->NewStringUTF(CALLBACK_CLASS_NAME)) != NULL))
        return;

    if (!NSK_VERIFY((callback_class = (jclass) jni->CallObjectMethod(
            loader, method_id, class_name_string)) != NULL))
        return;

    if (!NSK_VERIFY((method_id = jni->GetStaticMethodID(
            callback_class, "callback", "(Ljava/lang/String;I)V")) != NULL))
        return;

    if (!NSK_VERIFY((class_name_string = jni->NewStringUTF(name)) != NULL))
        return;

    jni->CallStaticObjectMethod(callback_class, method_id, class_name_string, agent_id);
}


/* ============================================================================= */

/* Agent initialization procedure */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_retransform003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_retransform003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_retransform003(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *vm, char *options, void *reserved)
{
    jvmtiEventCallbacks callbacks;
    jvmtiCapabilities caps;

    if (!NSK_VERIFY(
                nsk_jvmti_parseOptions(options)
                )
       )
        return JNI_ERR;

    agent_id= nsk_jvmti_findOptionIntValue("id", -1);

    if (!NSK_VERIFY((jvmti = nsk_jvmti_createJVMTIEnv(vm, reserved)) != NULL))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    if (nsk_jvmti_findOptionIntValue("can_retransform_classes", 1)) {
        caps.can_retransform_classes = 1;
    } else {
        caps.can_retransform_classes = 0;
    }


    // Register all necessary JVM capabilities
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    // Register all necessary event callbacks
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.ClassFileLoadHook = &ClassFileLoadHook;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    // Enable class retransformation
    if (!NSK_JVMTI_VERIFY(
                jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                                JVMTI_EVENT_CLASS_FILE_LOAD_HOOK,
                                                NULL)))
        return JNI_ERR;

    return JNI_OK;
}

}

/* ============================================================================= */
