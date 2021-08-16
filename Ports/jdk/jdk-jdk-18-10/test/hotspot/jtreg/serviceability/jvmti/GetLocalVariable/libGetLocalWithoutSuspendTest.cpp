/*
 * Copyright (c) 2020 SAP SE. All rights reserved.
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
#include "jvmti.h"
#include "jni.h"


//
// Please also read the @comment in GetLocalWithoutSuspendTest.java
//

extern "C" {

////////////////////////////////////////////////////
// BEGIN: Shared Variables
// The following variables are shared between agent and target thread

// glws_monitor is used to synchronize access to shared variables.
static jrawMonitorID glws_monitor;

// Target thread for agent operations.
static jthread target_thread = NULL;

// Depth of the frame for GetLocalObject() call by the agent thread. It is set by the target thread.
// -1 is the signal to shut down.
static int depth_for_get_local = 0;

enum TestState {
  Initial,

  TargetInNative,            // The agent waits for the target thread to reach
                             // the native method notifyAgentToGetLocal. Then it
                             // reads depth_for_get_local and changes the state
                             // to AgentInGetLocal. After that it
                             // calls GetLocalObject().

  AgentInGetLocal,           // The target thread waits for the agent to call
                             // GetLocalObject(). When this state is reached it
                             // resets the state to Initial and returns from
                             // native after a short spin wait racing the agent
                             // thread doing the unsafe stack walk.

  ShutDown,

  Terminated
};

// Current test state. It is used to synchronize agent and target thread execution.
static TestState test_state;

// END: Shared Variables
////////////////////////////////////////////////////


// Dummy counter used in spin wait. It is declared volatile to prevent the compiler
// from eliminating the whole spin loop.
static volatile int dummy_counter   = 0;

// Makes a string of the argument (which is not macro-expanded)
#define STR(a)  #a

// Makes a string of the macro expansion of a
#define XSTR(a) STR(a)

#define AT_LINE " ERROR at line " XSTR(__LINE__)

static jvmtiEnv* jvmti = NULL;

static jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved);

static char* GetErrorMessage(jvmtiEnv *jvmti, jvmtiError errCode) {
  char *errMsg;
  jvmtiError result = jvmti->GetErrorName(errCode, &errMsg);
  return result == JVMTI_ERROR_NONE ? errMsg : NULL;
}

static void ShowErrorMessage(jvmtiEnv *jvmti, jvmtiError errCode, const char *message) {
  char* errMsg = GetErrorMessage(jvmti, errCode);

  if (errMsg != NULL) {
    fprintf(stderr, "AGENT: %s: %s (%d)\n", message, errMsg, errCode);
    jvmti->Deallocate((unsigned char *)errMsg);
  } else {
    fprintf(stderr, "AGENT: %s (%d)\n", message, errCode);
  }
}

static void monitor_enter(jvmtiEnv* jvmti, JNIEnv* env, const char* loc) {
  jvmtiError err = jvmti->RawMonitorEnter(glws_monitor);
  if (err != JVMTI_ERROR_NONE) {
    ShowErrorMessage(jvmti, err, loc);
    env->FatalError(loc);
  }
}

static void monitor_exit(jvmtiEnv* jvmti, JNIEnv* env, const char* loc) {
  jvmtiError err = jvmti->RawMonitorExit(glws_monitor);
  if (err != JVMTI_ERROR_NONE) {
    ShowErrorMessage(jvmti, err, loc);
    env->FatalError(loc);
  }
}

static void monitor_wait(jvmtiEnv* jvmti, JNIEnv* env, const char* loc) {
  jvmtiError err = jvmti->RawMonitorWait(glws_monitor, 0);
  if (err != JVMTI_ERROR_NONE) {
    ShowErrorMessage(jvmti, err, loc);
    env->FatalError(loc);
  }
}

static void monitor_notify(jvmtiEnv* jvmti, JNIEnv* env, const char* loc) {
  jvmtiError err = jvmti->RawMonitorNotify(glws_monitor);
  if (err != JVMTI_ERROR_NONE) {
    ShowErrorMessage(jvmti, err, loc);
    env->FatalError(loc);
  }
}

static void monitor_destroy(jvmtiEnv* jvmti, JNIEnv* env, const char* loc) {
  jvmtiError err = jvmti->DestroyRawMonitor(glws_monitor);
  if (err != JVMTI_ERROR_NONE) {
    ShowErrorMessage(jvmti, err, loc);
    env->FatalError(loc);
  }
}

// Perform GetLocalObject() at the requested depth while target thread is running.
// Note that the JVMTI spec does not require to suspend the target thread.
void test_GetLocalObject(jvmtiEnv* jvmti, JNIEnv* env, int depth) {
  jvmtiError err;
  jobject obj;
  char* errMsg;

  printf("AGENT: calling GetLocalObject() with depth %d\n", depth);
  err = jvmti->GetLocalObject(target_thread, depth, 0, &obj);
  errMsg = GetErrorMessage(jvmti, err);
  printf("AGENT: GetLocalObject() result code %s (%d)\n", errMsg != NULL ? errMsg : "N/A", err);
  if (errMsg != NULL) {
    jvmti->Deallocate((unsigned char *)errMsg);
  }
  fflush(stdout);

  // If the target thread wins the race we can get errors because we
  // don't find a frame at the given depth or we find a non-java frame
  // there (e.g. native frame). This is expected.
  // JVMTI_ERROR_INVALID_SLOT can occur also because the target thread is
  // running and the GetLocalObject() call might coincidentally refer to the
  // frame of a static method without parameters.
  if (err != JVMTI_ERROR_NONE &&
      err != JVMTI_ERROR_NO_MORE_FRAMES &&
      err != JVMTI_ERROR_OPAQUE_FRAME &&
      err != JVMTI_ERROR_INVALID_SLOT) {
    ShowErrorMessage(jvmti, err, "AgentThreadLoop: error in JVMTI GetLocalObject");
    env->FatalError("AgentThreadLoop: error in JVMTI GetLocalObject\n");
  }
}

// Function holding the main loop for the test agent thread.
//
// The agent does the following in each loop iteration:
//
// - Wait for the target thread either to start a new test iteration or to
//   signal shutdown.
//
//     Shutdown is signalled by setting test_state to ShutDown. The agent reacts
//     to it by changing test_state to Terminated and then exits.
//
//     In the case of a new test iteration the target thread builds a deep call
//     stack and then calls the native method notifyAgentToGetLocal(). While in
//     native code its stack is walkable. It sets the shared variable test_state
//     to TargetInNative and then uses the glws_monitor to send the
//     notification to the agent thread.
//
// - Read the shared variable depth_for_get_local which was set by the target
//   thread before sending the notification.
//
// - Set test_state to AgentInGetLocal and notify the target thread.
//
// - Perform the JVMTI GetLocal call at depth_for_get_local racing the target
//   thread returning from the native call making its stack not walkable. The VM
//   will crash if this happens while the stack is walked to find the frame for
//   the GetLocal operation. The deeper the frame the more likely the crash
//   because the stack walk takes longer.
//
JNIEXPORT void JNICALL
AgentThreadLoop(jvmtiEnv * jvmti, JNIEnv* env, void * arg) {
  jvmtiError err;
  jvmtiThreadInfo thread_info;

  // Wait until target_thread is set by target thread.
  monitor_enter(jvmti, env, AT_LINE);
  while (target_thread == NULL) {
    monitor_wait(jvmti, env, AT_LINE);
  }
  monitor_exit(jvmti, env, AT_LINE);

  err = jvmti->GetThreadInfo(target_thread, &thread_info);
  if (err != JVMTI_ERROR_NONE) {
    ShowErrorMessage(jvmti, err, "AgentThreadLoop: error in JVMTI GetThreadInfo");
    env->FatalError("AgentThreadLoop: error in JVMTI GetThreadInfo\n");
  }

  printf("AGENT: AgentThreadLoop thread started. Polling thread '%s' for local variables\n",
         thread_info.name);
  jvmti->Deallocate((unsigned char *) thread_info.name);

  do {
    int depth;

    monitor_enter(jvmti, env, AT_LINE);

    // Wait for java part to build large stack and then become stack walk
    // save by calling the native method notifyAgentToGetLocal or to signal
    // shutdown.
    while (test_state != TargetInNative) {
      if (test_state == ShutDown) {
        test_state = Terminated;
        monitor_notify(jvmti, env, AT_LINE);
        monitor_exit(jvmti, env, AT_LINE);
        return;
      }
      monitor_wait(jvmti, env, AT_LINE);
    }
    depth = depth_for_get_local;

    // Notify target thread that this thread is about to query the local value.
    test_state = AgentInGetLocal;
    monitor_notify(jvmti, env, AT_LINE);

    monitor_exit(jvmti, env, AT_LINE);

    // Now get the local object from the target thread's stack.
    test_GetLocalObject(jvmti, env, depth);
  } while (true);

  printf("AGENT: AgentThreadLoop thread: exiting\n");
}

// Called by target thread after building a large stack.
// By calling this native method, the thread's stack becomes walkable.
// It notifies the agent to do the GetLocalObject() call and then races
// it to make its stack not walkable by returning from the native call.
JNIEXPORT void JNICALL
Java_GetLocalWithoutSuspendTest_notifyAgentToGetLocal(JNIEnv *env, jclass cls, jint depth, jint waitCycles) {
  monitor_enter(jvmti, env, AT_LINE);

  // Set depth_for_get_local and notify agent that the target thread is ready for the GetLocalObject() call
  depth_for_get_local = depth;
  test_state = TargetInNative;

  monitor_notify(jvmti, env, AT_LINE);

  // Wait for agent thread to read depth_for_get_local and do the GetLocalObject() call
  while (test_state != AgentInGetLocal) {
    monitor_wait(jvmti, env, AT_LINE);
  }

  // Reset state to Initial
  test_state = Initial;

  monitor_exit(jvmti, env, AT_LINE);

  // Wait a little until agent thread is in unsafe stack walk.
  // This needs to be a spin wait or sleep because we cannot get a notification
  // from there.
  while (--waitCycles > 0) {
    dummy_counter++;
  }
}

// Called by target thread to signal shutdown. The target thread waits for the
// agent's acknowledge by changing test_state to Terminated.
JNIEXPORT void JNICALL
Java_GetLocalWithoutSuspendTest_shutDown(JNIEnv *env, jclass cls) {
  monitor_enter(jvmti, env, AT_LINE);

  // Notify agent thread to shut down
  test_state = ShutDown;
  monitor_notify(jvmti, env, AT_LINE);

  // Wait for agent to terminate
  while (test_state != Terminated) {
    monitor_wait(jvmti, env, AT_LINE);
  }

  monitor_exit(jvmti, env, AT_LINE);

  // Destroy glws_monitor
  monitor_destroy(jvmti, env, AT_LINE);
}

// Called by target thread to provide agent with its thread object
JNIEXPORT void JNICALL
Java_GetLocalWithoutSuspendTest_setTargetThread(JNIEnv *env, jclass cls, jthread target) {
  monitor_enter(jvmti, env, AT_LINE);

  target_thread = env->NewGlobalRef(target);

  monitor_notify(jvmti, env, AT_LINE);

  monitor_exit(jvmti, env, AT_LINE);
}

void JNICALL VMInit(jvmtiEnv *jvmti, JNIEnv *env, jthread thr) {
  jvmtiError err;
  jobject agent_thread_name;
  jclass thread_clas;
  jmethodID thread_ctro;
  jthread agent_thread;

  printf("AGENT: VM init event\n");
  printf("AGENT: Start new thread that performs GetLocalObject calls on a running target thread\n");

  agent_thread_name = env->NewStringUTF("GetLocalWithoutSuspendTest Agent Thread");
  if (agent_thread_name == NULL) {
    env->FatalError("VMInit: NewStringUTF failed\n");
  }

  thread_clas = env->FindClass("java/lang/Thread");
  if (agent_thread_name == NULL) {
    env->FatalError("VMInit: java.lang.Thread class not found\n");
  }

  thread_ctro = env->GetMethodID(thread_clas, "<init>", "(Ljava/lang/String;)V");
  if (thread_ctro == NULL) {
    env->FatalError("VMInit: failed to get ID for the Thread ctor\n");
  }

  agent_thread = (jthread) env->NewObject(thread_clas, thread_ctro, agent_thread_name);
  if (agent_thread == NULL) {
    env->FatalError("VMInit: Failed to allocate thread object\n");
  }

  err = jvmti->RunAgentThread(agent_thread, &AgentThreadLoop, NULL,
                              JVMTI_THREAD_NORM_PRIORITY);
  if (err != JVMTI_ERROR_NONE) {
    ShowErrorMessage(jvmti, err, "VMInit: failed to start GetLocalWithoutSuspendTest thread");
    return;
  }
}

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
  printf("AGENT: Agent_OnLoad started.\n");
  return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT jint JNICALL
Agent_OnAttach(JavaVM *jvm, char *options, void *reserved) {
  printf("AGENT: Agent_OnAttach started.");
  return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *jvm, void *reserved) {
  jint res;
  JNIEnv *env;

  printf("AGENT: JNI_OnLoad started.");
  res = jvm->GetEnv((void **) &env, JNI_VERSION_9);
  if (res != JNI_OK || env == NULL) {
    fprintf(stderr, "Error: GetEnv call failed(%d)!\n", res);
    return JNI_ERR;
  }

  return JNI_VERSION_9;
}

static
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
  jint res;
  jvmtiError err;
  jvmtiCapabilities caps;
  jvmtiEventCallbacks callbacks;

  printf("AGENT: Agent_Initialize started\n");

  memset(&caps, 0, sizeof(caps));
  memset(&callbacks, 0, sizeof(callbacks));

  res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_9);
  if (res != JNI_OK || jvmti == NULL) {
    fprintf(stderr, "Error: GetEnv(JVMTI_VERSION_9) call failed(%d)!\n", res);
    return JNI_ERR;
  }

  caps.can_access_local_variables = 1;

  err = jvmti->AddCapabilities(&caps);
  if (err != JVMTI_ERROR_NONE) {
    ShowErrorMessage(jvmti, err, "Agent_OnLoad: error in JVMTI AddCapabilities");
    return JNI_ERR;
  }

  err = jvmti->GetCapabilities(&caps);
  if (err != JVMTI_ERROR_NONE) {
    ShowErrorMessage(jvmti, err, "Agent_OnLoad: error in JVMTI GetCapabilities");
    return JNI_ERR;
  }

  if (!caps.can_access_local_variables) {
    fprintf(stderr, "Warning: Access to local variables is not implemented\n");
    return JNI_ERR;
  }

  callbacks.VMInit = &VMInit;
  err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
  if (err != JVMTI_ERROR_NONE) {
    ShowErrorMessage(jvmti, err, "Agent_OnLoad: error in JVMTI SetEventCallbacks");
    return JNI_ERR;
  }

  err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);
  if (err != JVMTI_ERROR_NONE) {
    ShowErrorMessage(jvmti, err, "Agent_OnLoad: error in JVMTI SetEventNotificationMode");
    return JNI_ERR;
  }

  err = jvmti->CreateRawMonitor("GetLocalWithoutSuspend Test Monitor", &glws_monitor);

  printf("AGENT: Agent_Initialize finished\n");
  return JNI_OK;
}

}
