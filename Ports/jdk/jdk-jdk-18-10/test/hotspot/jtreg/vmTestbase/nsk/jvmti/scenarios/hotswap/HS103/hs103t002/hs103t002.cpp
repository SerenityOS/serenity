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
/*
 Periodically hotswap class(es) with a changed version in
 asynchronous manner from specified number of JVMTI agents. The VM
 works in default mode.
*/
#include <jni.h>
#include <jvmti.h>
#include "agent_common.h"
#include <string.h>
#include "jni_tools.h"
#include "jvmti_tools.h"
#include "JVMTITools.h"

extern "C" {

#define FILE_NAME "nsk/jvmti/scenarios/hotswap/HS103/hs103t002/MyThread"
#define SEARCH_NAME "nsk/jvmti/scenarios/hotswap/HS103/hs103t002/MyThread"
#define MAIN_CLASS  "nsk/jvmti/scenarios/hotswap/HS103/hs103t002/hs103t002"

static jvmtiEnv * jvmti;
static jthread testAgentThread;

JNIEXPORT void JNICALL doRedefineInNativeThread(jvmtiEnv * jvmti,
                                                JNIEnv * jni, void * arg) {
  jclass cla;
  int i = 0;
  int redefineNumber = 0;
  char fileName[512];

  jclass testClass;

  jmethodID setRedefinitionDone;
  jmethodID setRedefinitionFailed;

  testClass = jni->FindClass(MAIN_CLASS);

  if (!NSK_JNI_VERIFY(jni, (
    setRedefinitionFailed = jni->GetStaticMethodID(testClass, "setRedefinitionFailed", "()V")) != NULL))
  {
    jni->FatalError("TEST FAILED: while getting setRedefinitionFailed()\n");
  }

  if (!NSK_JNI_VERIFY(jni, (
    setRedefinitionDone = jni->GetStaticMethodID(testClass, "setRedefinitionDone", "()V")) != NULL))
  {
    jni->FatalError("TEST FAILED: while getting setRedefinitionDone()\n");
  }

  nsk_printf("doRedefineInNativeThread\n");
  cla = jni->FindClass(SEARCH_NAME);
  nsk_jvmti_getFileName(redefineNumber, FILE_NAME, fileName, sizeof(fileName)/sizeof(char));
  for (i = 0; i < 30; i++) {
    nsk_printf(" Inside the redefine method..\n");
    if (nsk_jvmti_redefineClass(jvmti, cla,fileName)) {
      nsk_printf("\nMyClass :: Successfully redefined..\n");
    } else {
      nsk_printf("\nMyClass :: Failed to redefine ..\n");

      if (!NSK_JNI_VERIFY_VOID(jni, jni->CallStaticVoidMethod(testClass, setRedefinitionFailed)))
      {
         jni->FatalError("TEST FAILED: while calling setRedefinitionFailed()\n");
      }
    }
  }

  if (!NSK_JNI_VERIFY_VOID(jni, jni->CallStaticVoidMethod(testClass, setRedefinitionDone)))
  {
    jni->FatalError("TEST FAILED: while calling setRedefinitionDone()\n");
  }

  nsk_printf(" All 30 redefinitions are done..\n");
}


#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_hs103t002(JavaVM *jvm, char *options, void *reserved) {
  return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_hs103t002(JavaVM *jvm, char *options, void *reserved) {
  return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_hs103t002(JavaVM *jvm, char *options, void *reserved) {
  return JNI_VERSION_1_8;
}
#endif

jint Agent_Initialize(JavaVM *vm, char *options, void *reserved) {
  jint rc;

  nsk_printf("Agent:: VM.. Started..\n");

  rc = vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_1);
  if (rc != JNI_OK) {
    nsk_printf("Agent:: Could not load JVMTI interface \n");
    return JNI_ERR;
  } else {
    jvmtiCapabilities caps;
    if (!nsk_jvmti_parseOptions(options)) {
      nsk_printf("# error agent Failed to parse options \n");
      return JNI_ERR;
    }
    memset(&caps, 0, sizeof(caps));
    caps.can_redefine_classes = 1;
    jvmti->AddCapabilities(&caps);
  }
  return JNI_OK;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS103_hs103t002_hs103t002_startAgentThread(JNIEnv * jni, jclass cls) {
  jvmtiError err ;
  jthread thread;
  jclass clas;
  jmethodID method;
  const char * threadName = "Agent Thread";
  jobject name;

  nsk_printf("hs103t002_startAgentThread\n");

  name = jni->NewStringUTF(threadName);
  clas = jni->FindClass("java/lang/Thread");

  if (!NSK_JNI_VERIFY(jni, (method = jni->GetMethodID(clas, "<init>", "(Ljava/lang/String;)V")) != NULL)) {
    jni->FatalError("failed to get ID for the java method\n");
  }

  thread = (jthread) jni->NewObject(clas,method,name);
  testAgentThread = jni->NewGlobalRef(thread);
  err = JVMTI_ERROR_NONE;
  err = jvmti->RunAgentThread(testAgentThread, &doRedefineInNativeThread, NULL,
                                 JVMTI_THREAD_NORM_PRIORITY);
  if (err == JVMTI_ERROR_INVALID_PRIORITY) {
    nsk_printf(" JVMTI_ERROR_INVALID_PRIORITY ..\n");
    return JNI_ERR;
  } else if (err == JVMTI_ERROR_INVALID_THREAD) {
    nsk_printf(" JVMTI_ERROR_INVALID_THREAD ..\n");
    return JNI_ERR;
  } else if (err == JVMTI_ERROR_NULL_POINTER) {
    nsk_printf(" JVMTI_ERROR_NULL_POINTER ..\n");
    return JNI_ERR;
  } else {
    nsk_printf(" Agent Thread Created.. \n");
  }
  return JNI_OK;
}

}
