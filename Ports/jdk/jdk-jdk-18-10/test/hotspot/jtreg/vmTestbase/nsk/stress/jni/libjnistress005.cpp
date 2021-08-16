/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jnihelper.h"

extern "C" {

JNIEXPORT void JNICALL
Java_nsk_stress_jni_JNIter005_except (JNIEnv *env, jobject jobj, jthrowable tobj) {
  jclass clazz;
  static int Exceptcalls = 0;
  const char *name;
  const char *mess;
  jmethodID jmethod;
  const char *iter = "nsk/stress/jni/JNIter005";
  //    const char *inc = "nsk/stress/jni/jnistress005/incCount";
  const char *incSig = "()V";
  const char *fldName = "counts";
  const char *fldSig = "I";

  /*     incClazz = env->FindClass(iter); */
  /*     jmethod = env->GetStaticMethodID(incClazz, inc, incSig); */
  /*     env->CallStaticVoidMethod(incClazz, jmethod); */
  /*     jfld = env->GetFieldID(incClazz, fldName, fldSig); */
  /*     printf("JNI: Count is %d\n", env->GetIntField(jobj, jfld)); */

  CHECK(env->MonitorEnter(jobj));

  if (env->Throw(tobj) == 0) {
    if (env->ExceptionOccurred()) {
      if (Exceptcalls % 1000 == 0) {
        fprintf(stderr, "NATIVE: Throw has been caught in native\n");
      }
    }
    env->ExceptionClear();
    ++Exceptcalls;
  } else {
    fprintf(stderr, "Throw failed\n");
  }

  CHECK(env->MonitorExit(jobj));

  switch (Exceptcalls % 23) {
  case 0: name = "java/lang/ArithmeticException"; break;
  case 1: name = "java/lang/ArrayIndexOutOfBoundsException"; break;
  case 2: name = "java/lang/ArrayStoreException"; break;
  case 3: name = "java/lang/ClassCastException"; break;
  case 4: name = "java/lang/ClassNotFoundException"; break;
  case 5: name = "java/lang/CloneNotSupportedException"; break;
  case 6: name = "java/lang/IllegalAccessException"; break;
  case 7: name = "java/lang/IllegalArgumentException"; break;
  case 8: name = "java/lang/IllegalMonitorStateException"; break;
  case 9: name = "java/lang/IllegalStateException"; break;
  case 10: name = "java/lang/IllegalThreadStateException"; break;
  case 11: name = "java/lang/IndexOutOfBoundsException"; break;
  case 12: name = "java/lang/InstantiationException"; break;
  case 13: name = "java/lang/InterruptedException"; break;
  case 14: name = "java/lang/NegativeArraySizeException"; break;
  case 15: name = "java/lang/NoSuchFieldException"; break;
  case 16: name = "java/lang/NoSuchMethodException"; break;
  case 17: name = "java/lang/NullPointerException"; break;
  case 18: name = "java/lang/NumberFormatException"; break;
  case 19: name = "java/lang/RuntimeException"; break;
  case 20: name = "java/lang/SecurityException"; break;
  case 21: name = "java/lang/StringIndexOutOfBoundsException"; break;
  case 22: name = "java/lang/UnsupportedOperationException"; break;
  default: name = "java/lang/Exception";
  }
  mess = name;

  clazz = env->FindClass(name); CE
  if (env->ThrowNew(clazz, mess) != 0) {
    const char *pass = "setpass";
    const char *passSig = "(Z)V";
    jclass incClazz;
    fprintf(stderr, "ThrowNew failed\n");
    CE;
    // ThrowNew failed but didn't itself raise an exception
    incClazz = env->FindClass(iter); CE
    jmethod=env->GetStaticMethodID(incClazz, pass, passSig); CE
    env->CallStaticVoidMethod(incClazz, jmethod, JNI_FALSE); CE
  }
  /*     printf("JNI: count %d\n", Exceptcalls); */
}

}
