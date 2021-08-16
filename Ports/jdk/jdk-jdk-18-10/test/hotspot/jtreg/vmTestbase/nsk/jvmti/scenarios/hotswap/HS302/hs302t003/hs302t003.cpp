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

extern "C" {

#define FILE_NAME "nsk/jvmti/scenarios/hotswap/HS302/hs302t003/MyClass"
#define CLASS_NAME "Lnsk/jvmti/scenarios/hotswap/HS302/hs302t003/MyClass;"

void JNICALL callbackClassPrepare(jvmtiEnv *jvmti_env,
                               JNIEnv* jni_env,
                               jthread thread,
                               jclass klass) {
  char * className;
  char * generic;
  int redefineNumber=0;
  jvmti_env->GetClassSignature(klass, &className, &generic);
  if (strcmp(className,CLASS_NAME) == 0) {
      char fileName[512];

      nsk_jvmti_getFileName(redefineNumber, FILE_NAME, fileName, sizeof(fileName)/sizeof(char));
      nsk_jvmti_disableNotification(jvmti_env, JVMTI_EVENT_CLASS_PREPARE, NULL);
      if (nsk_jvmti_redefineClass(jvmti_env, klass, fileName)) {
          nsk_printf("Redefine successful ..\n");
      } else {
          nsk_printf("Redefine failed ..     \n");
      }
  }
}


#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_hs302t003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_hs302t003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_hs302t003(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *vm, char *options, void *reserved) {
    jint rc ;
    jvmtiEnv * jvmti;

    nsk_printf("Agent:: VM.. Started..\n");
    rc=vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_1);
    if (rc != JNI_OK) {
        nsk_printf("Agent:: Could not load JVMTI interface \n");
        return JNI_ERR;
    } else {
        jvmtiCapabilities caps;
        jvmtiEventCallbacks eventCallbacks;

        if (!nsk_jvmti_parseOptions(options)) {
            nsk_printf("# error agent Failed to parse options \n");
            return JNI_ERR;
        }

        memset(&caps, 0, sizeof(caps));
        caps.can_redefine_classes = 1;
        caps.can_generate_all_class_hook_events=1;
        jvmti->AddCapabilities(&caps);
        memset(&eventCallbacks, 0, sizeof(eventCallbacks));
        eventCallbacks.ClassPrepare = &callbackClassPrepare;
        rc=jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks));
        if (rc != JVMTI_ERROR_NONE) {
            nsk_printf(" Agent:: Error occured while setting event call back \n");
            return JNI_ERR;
        }
        if (nsk_jvmti_enableNotification(jvmti, JVMTI_EVENT_CLASS_PREPARE, NULL)) {
            nsk_printf(" Enabled. noftification..");
        } else {
            nsk_printf(" Failed to Enable ..");
        }
    }
    return JNI_OK;
}


}
