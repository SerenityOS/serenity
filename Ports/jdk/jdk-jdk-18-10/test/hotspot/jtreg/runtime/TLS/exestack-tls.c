/*
 * Copyright (c) 2019, Google Inc. All rights reserved.
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef __GLIBC__
#include <gnu/libc-version.h>
#endif

// Declare the thread local variable(s) in the main executable. This can be
// used to demonstrate the issues associated with the on-stack static TLS blocks
// that may cause insufficient stack space. The dynamic TLS blocks for shared
// objects (such as a JNI library) loaded via dlopen are not allocated on stack.
__thread int tls[128 * 1024];

JNIEnv* create_vm(JavaVM **jvm, char* argTLS) {
    JNIEnv* env;
    JavaVMInitArgs args;
    JavaVMOption options[3];
    args.version = JNI_VERSION_1_8;
    args.nOptions = 3;
    char classpath[4096];
    snprintf(classpath, sizeof classpath,
             "-Djava.class.path=%s", getenv("CLASSPATH"));
    options[0].optionString = classpath;
    options[1].optionString = "-Xlog:os+thread=info";
    options[2].optionString = argTLS;
    args.options = &options[0];
    args.ignoreUnrecognized = 0;
    int rv;
    rv = JNI_CreateJavaVM(jvm, (void**)&env, &args);
    if (rv < 0) return NULL;
    return env;
}

#ifdef __GLIBC__
// glibc 2.15 introduced __pthread_get_minstack
int glibc_has_pthread_get_minstack() {
  const char* glibc_vers = gnu_get_libc_version();
  const int glibc_vers_major = atoi(glibc_vers);
  const int glibc_vers_minor = atoi(strchr(glibc_vers, '.') + 1);;
  printf("GNU libc version: %s\n", glibc_vers);
  if ((glibc_vers_major > 2) || ((glibc_vers_major == 2) && (glibc_vers_minor >= 15))) {
    return 1;
  }
  printf("This version does not provide __pthread_get_minstack\n");
  return 0;
}
#else
int glibc_has_pthread_get_minstack() {
  return 0;
}
#endif

int run(jboolean addTLS) {
    JavaVM *jvm;
    jclass testClass;
    jmethodID runMethod;
    char* argTLS;
    int res = -1;

    if (addTLS) {
      if (!glibc_has_pthread_get_minstack()) {
        printf("Skipping the test.\n");
        return 0;
      }
      argTLS = "-XX:+AdjustStackSizeForTLS";
    } else {
      argTLS = "-XX:-AdjustStackSizeForTLS"; // default
    }
    printf("Running test with %s ...\n", argTLS);
    JNIEnv *env = create_vm(&jvm, argTLS);

    // Run T.run() and check result:
    // - Expect T.run() to return 'true' when stack size is adjusted for TLS,
    //   return 0 if so
    // - Expect T.run() to return 'false' if stack size is not adjusted for
    //   TLS, return 0 if so
    // Return -1 (fail) for other cases
    testClass = (*env)->FindClass(env, "T");
    runMethod = (*env)->GetStaticMethodID(env, testClass, "run", "()Z");
    if ((*env)->CallStaticBooleanMethod(env, testClass, runMethod, NULL)) {
      if (addTLS) {
        // expect T.run() to return 'true'
        res = 0;
      }
    } else {
      if (!addTLS) {
        // expect T.run() to return 'false'
        res = 0;
      }
    }

    if (res == 0) {
      printf("Test passed with %s\n", argTLS);
    } else {
      printf("Test failed with %s\n", argTLS);
    }
    return res;
}

int main(int argc, char **argv) {
    if (argc == 2 && strcmp(argv[1], "-add_tls") == 0) {
      return run(JNI_TRUE);
    } else {
      return run(JNI_FALSE);
    }
}
