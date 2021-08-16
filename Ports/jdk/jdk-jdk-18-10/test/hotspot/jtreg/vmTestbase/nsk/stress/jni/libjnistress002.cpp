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
#include "jnihelper.h"

extern "C" {

jobject NewObjectWrapper(JNIEnv *env, jclass clazz, jmethodID methodID, ...) {
  va_list ap;
  jobject tmp;

  va_start(ap, methodID);
  tmp = env->NewObjectV(clazz, methodID, ap);
  va_end(ap);
  return(tmp);
}

JNIEXPORT jobjectArray JNICALL
Java_nsk_stress_jni_JNIter002_jniobjects (JNIEnv *env, jobject jobj, jstring jstr, jint intgr,
                                          jlong lng, jcharArray jChArr, jfloat flt, jdouble dbl) {

  static int classCount = 0;
  jobjectArray obj;
  jobject element;
  jclass clazz, clazzUp;
  jmethodID methodID;
  jfieldID fieldID;
  const char *classname = "nsk/stress/jni/objectsJNI";
  const char *name = "<init>";
  const char *sig = "(Ljava/lang/String;IJ[CFD)V";
  const char *upperClassName = "nsk/stress/jni/jnistress002";
  const char *fieldName = "jniStringAllocSize";
  const char *fieldSig = "I";
  const char *setpass = "halt";
  const char *setpassSig = "()V";
  jvalue paramArr [6];

  CHECK(env->MonitorEnter(jobj));
  ++classCount;
  CHECK(env->MonitorExit(jobj));
  paramArr[0].l = jstr;
  paramArr[1].i = intgr;
  paramArr[2].j = lng;
  paramArr[3].l = jChArr;
  paramArr[4].f = flt;
  paramArr[5].d = dbl;

  clazz = env->FindClass(classname); CE
  element = env->AllocObject(clazz); CE
  obj = env->NewObjectArray((jsize)3, clazz, element); CE

  methodID = env->GetMethodID(clazz, name, sig); CE

  element = env->NewObject(clazz, methodID,
                           jstr, intgr, lng, jChArr, flt, dbl); CE
  env->SetObjectArrayElement(obj, 0, element); CE
  element = env->NewObjectA(clazz, methodID, paramArr); CE
  env->SetObjectArrayElement(obj, 1, element); CE
  element= NewObjectWrapper(env, clazz, methodID,
                            jstr, intgr, lng, jChArr, flt, dbl); CE
  env->SetObjectArrayElement(obj, 2, element); CE

  clazzUp = env->FindClass(upperClassName); CE
  fieldID = env->GetStaticFieldID(clazzUp, fieldName, fieldSig); CE
  if (classCount == env->GetStaticIntField(clazzUp, fieldID)) {
    classname = "nsk/stress/jni/JNIter002";
    clazz = env->FindClass(classname); CE
    methodID = env->GetStaticMethodID(clazz, setpass, setpassSig); CE
    env->CallStaticVoidMethod(clazz, methodID); CE
  }

  CE

  return obj;
}

}
