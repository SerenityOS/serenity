/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#ifdef __APPLE__
#  include <dlfcn.h>
#endif

#ifdef _WIN32
#include <windows.h>
#else
#include <pthread.h>
#endif

#include "jni.h"
#include "unittest.hpp"

#include "runtime/thread.inline.hpp"

// Default value for -new-thread option: true on AIX because we run into
// problems when attempting to initialize the JVM on the primordial thread.
#ifdef _AIX
const static bool DEFAULT_SPAWN_IN_NEW_THREAD = true;
#else
const static bool DEFAULT_SPAWN_IN_NEW_THREAD = false;
#endif

static bool is_prefix(const char* prefix, const char* str) {
  return strncmp(str, prefix, strlen(prefix)) == 0;
}

static bool is_suffix(const char* suffix, const char* str) {
  size_t suffix_len = strlen(suffix);
  size_t str_len = strlen(str);
  if (str_len < suffix_len) {
      return false;
  }
  return strncmp(str + (str_len - suffix_len), suffix, suffix_len) == 0;
}

static int init_jvm(int argc, char **argv, bool disable_error_handling, JavaVM** jvm_ptr) {
  // don't care about the program name
  argc--;
  argv++;

  int extra_jvm_args = disable_error_handling ? 4 : 2;
  int num_jvm_options = argc + extra_jvm_args;

  JavaVMOption* options = new JavaVMOption[num_jvm_options];
  options[0].optionString = (char*) "-Dsun.java.launcher.is_altjvm=true";
  options[1].optionString = (char*) "-XX:+ExecutingUnitTests";

  if (disable_error_handling) {
    // don't create core files or hs_err files executing assert tests
    options[2].optionString = (char*) "-XX:+SuppressFatalErrorMessage";
    options[3].optionString = (char*) "-XX:-CreateCoredumpOnCrash";
  }

  for (int i = 0; i < argc; i++) {
    options[extra_jvm_args + i].optionString = argv[i];
  }

  JavaVMInitArgs args;
  args.version = JNI_VERSION_1_8;
  args.nOptions = num_jvm_options;
  args.options = options;
  args.ignoreUnrecognized = JNI_FALSE;

  JNIEnv* env;

  int ret = JNI_CreateJavaVM(jvm_ptr, (void**)&env, &args);
  if (ret == JNI_OK) {
    // CreateJavaVM leaves WXExec context, while gtests
    // calls internal functions assuming running in WXWwrite.
    // Switch to WXWrite once for all test cases.
    MACOS_AARCH64_ONLY(Thread::current()->enable_wx(WXWrite));
  }
  return ret;
}

static bool is_same_vm_test(const char* name) {
  return is_suffix("_vm", name) && !is_suffix("_other_vm", name);
}

class JVMInitializerListener : public ::testing::EmptyTestEventListener {
 private:
  int _argc;
  char** _argv;
  JavaVM* _jvm;

 public:
  JVMInitializerListener(int argc, char** argv) :
    _argc(argc), _argv(argv), _jvm(nullptr) {
  }

  virtual void OnTestStart(const ::testing::TestInfo& test_info) {
    const char* name = test_info.name();
    if (_jvm == nullptr && is_same_vm_test(name)) {
      // we want to have hs_err and core files when we execute regular tests
      int ret_val = init_jvm(_argc, _argv, false, &_jvm);
      if (ret_val != 0) {
        ADD_FAILURE() << "Could not initialize the JVM: " << ret_val;
        exit(1);
      }
    }
  }

  void destroy_jvm() {
    if (_jvm != NULL) {
      int ret = _jvm->DestroyJavaVM();
      if (ret != 0) {
        fprintf(stderr, "Warning: DestroyJavaVM error %d\n", ret);
      }
    }
  }
};

static char* get_java_home_arg(int argc, char** argv) {
  for (int i = 0; i < argc; i++) {
    if (strncmp(argv[i], "-jdk", strlen(argv[i])) == 0) {
      return argv[i+1];
    }
    if (is_prefix("--jdk=", argv[i])) {
      return argv[i] + strlen("--jdk=");
    }
    if (is_prefix("-jdk:", argv[i])) {
      return argv[i] + strlen("-jdk:");
    }
  }
  return NULL;
}

static bool get_spawn_new_main_thread_arg(int argc, char** argv) {
  // -new-thread[=(true|false)]
  for (int i = 0; i < argc; i++) {
    if (is_prefix("-new-thread", argv[i])) {
      const char* v = argv[i] + strlen("-new-thread");
      if (strlen(v) == 0) {
        return true;
      } else {
        if (strcmp(v, "=true") == 0) {
          return true;
        } else if (strcmp(v, "=false") == 0) {
          return false;
        } else {
          fprintf(stderr, "Invalid value for -new-thread (%s)", v);
        }
      }
    }
  }
  return DEFAULT_SPAWN_IN_NEW_THREAD;
}

static int num_args_to_skip(char* arg) {
  if (strcmp(arg, "-jdk") == 0) {
    return 2; // skip the argument after -jdk as well
  }
  if (is_prefix("--jdk=", arg)) {
    return 1;
  }
  if (is_prefix("-jdk:", arg)) {
    return 1;
  }
  if (is_prefix("-new-thread", arg)) {
    return 1;
  }
  return 0;
}

static char** remove_test_runner_arguments(int* argcp, char **argv) {
  int argc = *argcp;
  char** new_argv = (char**) malloc(sizeof(char*) * argc);
  int new_argc = 0;

  int i = 0;
  while (i < argc) {
    int args_to_skip = num_args_to_skip(argv[i]);
    if (args_to_skip == 0) {
      new_argv[new_argc] = argv[i];
      i++;
      new_argc++;
    } else {
      i += num_args_to_skip(argv[i]);
    }
  }

  *argcp = new_argc;
  return new_argv;
}

// This is generally run once for a set of tests. But if that set includes a vm_assert or
// other_vm test, then a new process is forked, and runUnitTestsInner is called, passing
// just that test as the one to be executed.
//
// When we execute a vm_assert or other_vm test we create and initialize the JVM below.
//
// A vm_assert test crashes the VM so no cleanup is needed, but for other_vm we call
// DestroyJavaVM via the TEST_OTHER_VM macro prior to the call to exit().
//
// For same_vm tests we use an event listener to create the JVM when the first same_vm
// test is executed. Once all tests are completed we can then call DestroyJavaVM on that
// JVM directly.
static void runUnitTestsInner(int argc, char** argv) {
  ::testing::InitGoogleMock(&argc, argv);
  ::testing::GTEST_FLAG(death_test_style) = "threadsafe";

  bool is_vmassert_test = false;
  bool is_othervm_test = false;
  // death tests facility is used for both regular death tests, other vm and vmassert tests
  if (::testing::internal::GTEST_FLAG(internal_run_death_test).length() > 0) {
    // when we execute death test, filter value equals to test name
    const char* test_name = ::testing::GTEST_FLAG(filter).c_str();
    const char* const othervm_suffix = "_other_vm"; // TEST_OTHER_VM
    const char* const vmassert_suffix = "_vm_assert"; // TEST_VM_ASSERT(_MSG)
    if (is_suffix(othervm_suffix, test_name)) {
      is_othervm_test = true;
    } else if (is_suffix(vmassert_suffix, test_name)) {
      is_vmassert_test = true;
    }
  }

  char* java_home = get_java_home_arg(argc, argv);
  if (java_home == NULL) {
    fprintf(stderr, "ERROR: You must specify a JDK to use for running the unit tests.\n");
    exit(1);
  }
#ifndef _WIN32
  int overwrite = 1; // overwrite an eventual existing value for JAVA_HOME
  setenv("JAVA_HOME", java_home, overwrite);

// workaround for JDK-7131356
#ifdef __APPLE__
  size_t len = strlen(java_home) + strlen("/lib/jli/libjli.dylib") + 1;
  char* path = new char[len];
  snprintf(path, len, "%s/lib/jli/libjli.dylib", java_home);
  dlopen(path, RTLD_NOW | RTLD_GLOBAL);
#endif // __APPLE__

#else  // _WIN32
  const char* java_home_var = "_ALT_JAVA_HOME_DIR";
  size_t len = strlen(java_home) + strlen(java_home_var) + 2;
  char * envString = new char[len];
  sprintf_s(envString, len, "%s=%s", java_home_var, java_home);
  _putenv(envString);
#endif // _WIN32
  argv = remove_test_runner_arguments(&argc, argv);


  JVMInitializerListener* jvm_listener = NULL;

  if (is_vmassert_test || is_othervm_test) {
    JavaVM* jvm = NULL;
    // both vmassert and other vm tests require inited jvm
    // but only vmassert tests disable hs_err and core file generation
    int ret;
    if ((ret = init_jvm(argc, argv, is_vmassert_test, &jvm)) != 0) {
      fprintf(stderr, "ERROR: JNI_CreateJavaVM failed: %d\n", ret);
      abort();
    }
  } else {
    ::testing::TestEventListeners& listeners = ::testing::UnitTest::GetInstance()->listeners();
    jvm_listener = new JVMInitializerListener(argc, argv);
    listeners.Append(jvm_listener);
  }

  int result = RUN_ALL_TESTS();

  // vm_assert and other_vm tests never reach this point as they either abort, or call
  // exit() - see TEST_OTHER_VM macro. We will reach here when all same_vm tests have
  // completed for this run, so we can terminate the VM used for that case.

  if (result != 0) {
    fprintf(stderr, "ERROR: RUN_ALL_TESTS() failed. Error %d\n", result);
    exit(2);
  }

  if (jvm_listener != NULL) {
    jvm_listener->destroy_jvm();
  }
}

// Thread support for -new-thread option

struct args_t {
  int argc; char** argv;
};

#define STACK_SIZE 0x200000

#ifdef _WIN32

static DWORD WINAPI thread_wrapper(void* p) {
  const args_t* const p_args = (const args_t*) p;
  runUnitTestsInner(p_args->argc, p_args->argv);
  return 0;
}

static void run_in_new_thread(const args_t* args) {
  HANDLE hdl;
  hdl = CreateThread(NULL, STACK_SIZE, thread_wrapper, (void*)args, 0, NULL);
  if (hdl == NULL) {
    fprintf(stderr, "Failed to create main thread\n");
    exit(2);
  }
  WaitForSingleObject(hdl, INFINITE);
}

#else

extern "C" void* thread_wrapper(void* p) {
  const args_t* const p_args = (const args_t*) p;
  runUnitTestsInner(p_args->argc, p_args->argv);
  return 0;
}

static void run_in_new_thread(const args_t* args) {
  pthread_t tid;
  pthread_attr_t attr;

  pthread_attr_init(&attr);
  pthread_attr_setstacksize(&attr, STACK_SIZE);

  if (pthread_create(&tid, &attr, thread_wrapper, (void*)args) != 0) {
    fprintf(stderr, "Failed to create main thread\n");
    exit(2);
  }

  if (pthread_join(tid, NULL) != 0) {
    fprintf(stderr, "Failed to join main thread\n");
    exit(2);
  }
}

#endif

extern "C"
JNIEXPORT void JNICALL runUnitTests(int argc, char** argv) {
  const bool spawn_new_main_thread = get_spawn_new_main_thread_arg(argc, argv);
  if (spawn_new_main_thread) {
    args_t args;
    args.argc = argc;
    args.argv = argv;
    run_in_new_thread(&args);
  } else {
    runUnitTestsInner(argc, argv);
  }
}
