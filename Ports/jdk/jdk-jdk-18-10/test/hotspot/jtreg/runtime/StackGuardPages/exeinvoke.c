/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* This code tests the fact that we actually remove stack guard page when calling
 * JavaThread::exit() i.e. when detaching from current thread.
 * We overflow the stack and check that we get access error because of a guard page.
 * Than we detach from vm thread and overflow stack once again. This time we shouldn't
 * get access error because stack guard page is removed
 *
 * Notice: due a complicated interaction of signal handlers, the test may crash.
 * It's OK - don't file a bug.
 */

#include <assert.h>
#include <jni.h>
#include <jvm.h>
#include <alloca.h>
#include <signal.h>
#include <string.h>
#include <sys/mman.h>
#include <stdlib.h>
#include <sys/ucontext.h>
#include <setjmp.h>
#include <unistd.h>
#include <sys/syscall.h>
#include <errno.h>

#include <pthread.h>

#define CLASS_PATH_OPT "-Djava.class.path="

JavaVM* _jvm;

static jmp_buf  context;

static int _last_si_code = -1;
static int _failures = 0;
static int _rec_count = 0;
static int _kp_rec_count = 0;

pid_t gettid() {
  return (pid_t) syscall(SYS_gettid);
}

static void handler(int sig, siginfo_t *si, void *unused) {
  _last_si_code = si->si_code;
  printf("Got SIGSEGV(%d) at address: 0x%lx\n",si->si_code, (long) si->si_addr);
  longjmp(context, 1);
}

void set_signal_handler() {
  static char altstack[SIGSTKSZ];

  stack_t ss = {
    .ss_size = SIGSTKSZ,
    .ss_flags = 0,
    .ss_sp = altstack
  };

  struct sigaction sa = {
    .sa_sigaction = handler,
    .sa_flags = SA_ONSTACK | SA_SIGINFO | SA_RESETHAND
  };

  _last_si_code = -1;

  sigaltstack(&ss, 0);
  sigemptyset(&sa.sa_mask);
  if (sigaction(SIGSEGV, &sa, NULL) == -1) {
    fprintf(stderr, "Test ERROR. Can't set sigaction (%d)\n", errno);
    exit(7);
  }
}

size_t get_java_stacksize () {
  pthread_attr_t attr;
  JDK1_1InitArgs jdk_args;

  memset(&jdk_args, 0, (sizeof jdk_args));

  jdk_args.version = JNI_VERSION_1_1;
  JNI_GetDefaultJavaVMInitArgs(&jdk_args);
  if (jdk_args.javaStackSize <= 0) {
    fprintf(stderr, "Test ERROR. Can't get a valid value for the default stacksize.\n");
    exit(7);
  }
  return jdk_args.javaStackSize;
}

void *run_java_overflow (void *p) {
  JNIEnv *env;
  jclass class_id;
  jmethodID method_id;
  int res;

  res = (*_jvm)->AttachCurrentThread(_jvm, (void**)&env, NULL);
  if (res != JNI_OK) {
    fprintf(stderr, "Test ERROR. Can't attach to current thread\n");
    exit(7);
  }

  class_id = (*env)->FindClass (env, "DoOverflow");
  if (class_id == NULL) {
    fprintf(stderr, "Test ERROR. Can't load class DoOverflow\n");
    exit(7);
  }

  method_id = (*env)->GetStaticMethodID(env, class_id, "printIt", "()V");
  if (method_id == NULL) {
    fprintf(stderr, "Test ERROR. Can't find method DoOverflow.printIt\n");
    exit(7);
  }

  (*env)->CallStaticVoidMethod(env, class_id, method_id, NULL);

  res = (*_jvm)->DetachCurrentThread(_jvm);
  if (res != JNI_OK) {
    fprintf(stderr, "Test ERROR. Can't call detach from current thread\n");
    exit(7);
  }
  return NULL;
}

void do_overflow(){
  int *p = alloca(sizeof(int));
  if (_kp_rec_count == 0 || _rec_count < _kp_rec_count) {
      _rec_count ++;
      do_overflow();
  }
}

void *run_native_overflow(void *p) {
  // Test that stack guard page is correctly set for initial and non initial thread
  // and correctly removed for the initial thread
  JNIEnv *env;
  jclass class_id;
  jmethodID method_id;
  int res;

  printf("run_native_overflow %ld\n", (long) gettid());

  res = (*_jvm)->AttachCurrentThread(_jvm, (void **)&env, NULL);
  if (res != JNI_OK) {
    fprintf(stderr, "Test ERROR. Can't attach to current thread\n");
    exit(7);
  }

  class_id = (*env)->FindClass (env, "DoOverflow");
  if (class_id == NULL) {
    fprintf(stderr, "Test ERROR. Can't load class DoOverflow\n");
    exit(7);
  }

  method_id = (*env)->GetStaticMethodID (env, class_id, "printAlive", "()V");
  if (method_id == NULL) {
    fprintf(stderr, "Test ERROR. Can't find method DoOverflow.printAlive\n");
    exit(7);
  }

  (*env)->CallStaticVoidMethod (env, class_id, method_id, NULL);

  // Initialize statics used in do_overflow
  _kp_rec_count = 0;
  _rec_count = 0;

  set_signal_handler();
  if (! setjmp(context)) {
    do_overflow();
  }

  if (_last_si_code == SEGV_ACCERR) {
    printf("Test PASSED. Got access violation accessing guard page at %d\n", _rec_count);
  }

  res = (*_jvm)->DetachCurrentThread(_jvm);
  if (res != JNI_OK) {
    fprintf(stderr, "Test ERROR. Can't call detach from current thread\n");
    exit(7);
  }

  if (getpid() != gettid()) {
    // For non-initial thread we don't unmap the region but call os::uncommit_memory and keep PROT_NONE
    // so if host has enough swap space we will get the same SEGV with code SEGV_ACCERR(2) trying
    // to access it as if the guard page is present.
    // We have no way to check this, so bail out, marking test as succeeded
    printf("Test PASSED. Not initial thread\n");
    return NULL;
  }

  // Limit depth of recursion for second run. It can't exceed one for first run.
  _kp_rec_count = _rec_count;
  _rec_count = 0;

  set_signal_handler();
  if (! setjmp(context)) {
    do_overflow();
  }

  if (_last_si_code == SEGV_ACCERR) {
      ++ _failures;
      fprintf(stderr,"Test FAILED. Stack guard page is still there at %d\n", _rec_count);
  } else if (_last_si_code == -1) {
      printf("Test PASSED. No stack guard page is present. Maximum recursion level reached at %d\n", _rec_count);
  }
  else{
      printf("Test PASSED. No stack guard page is present. SIGSEGV(%d) at %d\n", _last_si_code, _rec_count);
  }

  return NULL;
}

void usage() {
  fprintf(stderr, "Usage: invoke test_java_overflow\n");
  fprintf(stderr, "       invoke test_native_overflow\n");
}


int main (int argc, const char** argv) {
  JavaVMInitArgs vm_args;
  JavaVMOption options[3];
  JNIEnv* env;
  int optlen;
  char *javaclasspath = NULL;
  char javaclasspathopt[4096];

  printf("Test started with pid: %ld\n", (long) getpid());

  /* set the java class path so the DoOverflow class can be found */
  javaclasspath = getenv("CLASSPATH");

  if (javaclasspath == NULL) {
    fprintf(stderr, "Test ERROR. CLASSPATH is not set\n");
    exit(7);
  }
  optlen = strlen(CLASS_PATH_OPT) + strlen(javaclasspath) + 1;
  if (optlen > 4096) {
    fprintf(stderr, "Test ERROR. CLASSPATH is too long\n");
    exit(7);
  }
  snprintf(javaclasspathopt, sizeof(javaclasspathopt), "%s%s",
      CLASS_PATH_OPT, javaclasspath);

  options[0].optionString = "-Xint";
  options[1].optionString = "-Xss1M";
  options[2].optionString = javaclasspathopt;

  vm_args.version = JNI_VERSION_1_2;
  vm_args.ignoreUnrecognized = JNI_TRUE;
  vm_args.options = options;
  vm_args.nOptions = 3;

  if (JNI_CreateJavaVM (&_jvm, (void **)&env, &vm_args) < 0 ) {
    fprintf(stderr, "Test ERROR. Can't create JavaVM\n");
    exit(7);
  }

  size_t stack_size = get_java_stacksize();
  pthread_t thr;
  pthread_attr_t thread_attr;

  pthread_attr_init(&thread_attr);
  pthread_attr_setstacksize(&thread_attr, stack_size);

  if (argc > 1 && strcmp(argv[1], "test_java_overflow") == 0) {
    printf("\nTesting JAVA_OVERFLOW\n");

    printf("Testing stack guard page behaviour for other thread\n");

    pthread_create(&thr, &thread_attr, run_java_overflow, NULL);
    pthread_join(thr, NULL);

    printf("Testing stack guard page behaviour for initial thread\n");
    run_java_overflow(NULL);
    // This test crash on error
    exit(0);
  }

  if (argc > 1 && strcmp(argv[1], "test_native_overflow") == 0) {
    printf("\nTesting NATIVE_OVERFLOW\n");

    printf("Testing stack guard page behaviour for other thread\n");
    pthread_create(&thr, &thread_attr, run_native_overflow, NULL);
    pthread_join(thr, NULL);

    printf("Testing stack guard page behaviour for initial thread\n");
    run_native_overflow(NULL);

    exit((_failures > 0) ? 1 : 0);
  }

  fprintf(stderr, "Test ERROR. Unknown parameter %s\n", ((argc > 1) ? argv[1] : "none"));
  usage();
  exit(7);
}
