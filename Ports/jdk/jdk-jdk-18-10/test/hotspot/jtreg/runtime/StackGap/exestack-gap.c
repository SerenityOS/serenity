/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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

JNIEnv* create_vm(JavaVM **jvm, char *extra_option)
{
    JNIEnv* env;
    JavaVMInitArgs args;
    JavaVMOption options[4];
    args.version = JNI_VERSION_1_8;
    args.nOptions = 3 + (extra_option != NULL);
    options[0].optionString = "-Xss2048k";
    char classpath[4096];
    snprintf(classpath, sizeof classpath,
             "-Djava.class.path=%s", getenv("CLASSPATH"));
    options[1].optionString = classpath;
    options[2].optionString = "-XX:+UnlockExperimentalVMOptions";
    if (extra_option) {
      options[3].optionString = extra_option;
    }
    args.options = &options[0];
    args.ignoreUnrecognized = 0;
    int rv;
    rv = JNI_CreateJavaVM(jvm, (void**)&env, &args);
    if (rv < 0) return NULL;
    return env;
}

void run(char *extra_arg) {
  JavaVM *jvm;
  jclass T_class;
  jmethodID test_method;
  JNIEnv *env = create_vm(&jvm, extra_arg);
  if (env == NULL)
    exit(1);
  T_class = (*env)->FindClass(env, "T");
  test_method = (*env)->GetStaticMethodID(env, T_class, "test", "(I)V");
  (*env)->CallStaticVoidMethod(env, T_class, test_method, 1000);
}


int main(int argc, char **argv)
{
  if (argc > 1) {
    run(argv[1]);
  } else {
    run(NULL);
  }

  return 0;
}
