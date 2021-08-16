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

#define BOOL  0
#define BYTE  1
#define CHAR  2
#define SHORT 3
#define INT   4
#define LONG  5
#define FLOAT 6
#define DOUBLE 7

JNIEXPORT jobjectArray JNICALL
Java_nsk_stress_jni_JNIter003_jniInitArrays (JNIEnv *env, jobject jobj, jint size) {

  jarray *arrayArray;
  jboolean *boolBuf;
  jbyte *byteBuf;
  jchar *charBuf;
  jshort *shortBuf;
  jint *intBuf;
  jlong *longBuf;
  jfloat *floatBuf;
  jdouble *doubleBuf;
  jobjectArray objectsArray;

  int i;
  int SIZE = size;
  const char *classname = "java/lang/Object";
  jclass clazz = env->FindClass(classname); CE
  jobject obj = env->AllocObject(clazz); CE
  objectsArray = env->NewObjectArray(8, clazz, obj); CE

  arrayArray = (jarray *)c_malloc(env, 8*sizeof(jarray));
  arrayArray[BOOL] = env->NewBooleanArray(SIZE); CE
  arrayArray[BYTE] = env->NewByteArray(SIZE); CE
  arrayArray[CHAR] = env->NewCharArray(SIZE); CE
  arrayArray[SHORT] = env->NewShortArray(SIZE); CE
  arrayArray[INT] = env->NewIntArray(SIZE); CE
  arrayArray[LONG] = env->NewLongArray(SIZE); CE
  arrayArray[FLOAT] = env->NewFloatArray(SIZE); CE
  arrayArray[DOUBLE] = env->NewDoubleArray(SIZE); CE

  for (i = 0; i < 8; i++) {
    env->SetObjectArrayElement(objectsArray, i,arrayArray[i]); CE
  }

  boolBuf = (jboolean *)c_malloc(env, SIZE * sizeof(jboolean));
  byteBuf = (jbyte *)c_malloc(env, SIZE * sizeof(jbyte));
  charBuf = (jchar *)c_malloc(env, SIZE * sizeof(jchar));
  shortBuf = (jshort *)c_malloc(env, SIZE * sizeof(jshort));
  intBuf = (jint *)c_malloc(env, SIZE * sizeof(jint));
  longBuf = (jlong *)c_malloc(env, SIZE * sizeof(jlong));
  floatBuf = (jfloat *)c_malloc(env, SIZE * sizeof(jfloat));
  doubleBuf = (jdouble *)c_malloc(env, SIZE * sizeof(jdouble));

  for (i = 0; i < SIZE; i++) {
    boolBuf[i] = (i % 2 == 0) ? JNI_TRUE : JNI_FALSE;
    /*
      byteBuf[i] = (jbyte)random();
      charBuf[i] = (jchar)random();
      shortBuf[i] = (jshort)random();
      intBuf[i] = (jint)random();
      longBuf[i] = (jlong)random();
      floatBuf[i] = (jfloat)((random()));
      doubleBuf[i] = (jdouble)((random()));
    */
    byteBuf[i] = (jbyte)109;
    charBuf[i] = (jchar)214;
    shortBuf[i] = (jshort)9223;
    intBuf[i] = (jint)872634;
    longBuf[i] = (jlong)276458276;
    floatBuf[i] = (jfloat)235.4576284;
    doubleBuf[i] = (jdouble)98275.716253567;
  }
  env->SetBooleanArrayRegion((jbooleanArray) arrayArray[BOOL], 0, i, boolBuf); CE
  env->SetByteArrayRegion((jbyteArray) arrayArray[BYTE], 0, i, byteBuf); CE
  env->SetCharArrayRegion((jcharArray) arrayArray[CHAR], 0, i, charBuf); CE
  env->SetShortArrayRegion((jshortArray) arrayArray[SHORT], 0, i, shortBuf); CE
  env->SetIntArrayRegion((jintArray) arrayArray[INT], 0, i, intBuf); CE
  env->SetLongArrayRegion((jlongArray) arrayArray[LONG], 0, i, longBuf); CE
  env->SetFloatArrayRegion((jfloatArray) arrayArray[FLOAT], 0, i, floatBuf); CE
  env->SetDoubleArrayRegion((jdoubleArray) arrayArray[DOUBLE], 0, i, doubleBuf); CE

  free(doubleBuf);
  free(floatBuf);
  free(longBuf);
  free(intBuf);
  free(shortBuf);
  free(byteBuf);
  free(charBuf);
  free(boolBuf);
  free(arrayArray);

  return objectsArray;
}

JNIEXPORT jboolean JNICALL
Java_nsk_stress_jni_JNIter003_jniBodyChangeArray (JNIEnv *env, jobject jobj,
                                                  jobjectArray orig, jobjectArray clone, jint limit) {

#define SWAP(param1, param2) tmparr = param2; param2 = param1; param1 = tmparr;
#define SIZE(type) env->GetArrayLength(arrayClone[type])

  static volatile long count = 0;
  jarray *arrayOrig, *arrayClone;
  jboolean *boolOrig, *boolClone;
  jbyte *byteOrig, *byteClone;
  jchar *charOrig, *charClone;
  jshort *shortOrig, *shortClone;
  jint *intOrig, *intClone;
  jlong *longOrig, *longClone;
  jfloat *floatOrig, *floatClone;
  jdouble *doubleOrig, *doubleClone;
  int i;

  if ((orig == NULL) || (clone == NULL)) {
    fprintf(stderr, "JNI received a NULL array from Java\n");
    return JNI_FALSE;
  }
  if (count == limit) {
    jclass clazz;
    const char *classname = "nsk/stress/jni/JNIter003";
    const char *setdone = "halt";
    const char *setdonesig = "()V";
    jmethodID jmethod;

    fprintf(stderr, "Count and limit are: %ld\t%d cons.\n", count, limit);
    clazz = env->FindClass(classname); CE
    jmethod = env->GetMethodID(clazz, setdone, setdonesig); CE
    env->CallVoidMethod(jobj, jmethod); CE

    return JNI_TRUE;
  }
  CHECK(env->MonitorEnter(jobj));
  ++count;
  CHECK(env->MonitorExit(jobj));
  arrayOrig = (jarray *)c_malloc(env, 8 * sizeof(jarray));
  arrayClone = (jarray *)c_malloc(env, 8 * sizeof(jarray));
  for (i = 0; i < 8; i++) {
    arrayOrig[i] = (jarray) env->GetObjectArrayElement(orig, i); CE
    arrayClone[i] = (jarray) env->GetObjectArrayElement(clone, i); CE
  }

  /* Take the elements from Java arrays into native buffers */
  /* Use Get<Type>ArrayElements */
  boolOrig = env->GetBooleanArrayElements((jbooleanArray) arrayOrig[BOOL], 0); CE
  byteOrig = env->GetByteArrayElements((jbyteArray) arrayOrig[BYTE], 0); CE
  charOrig = env->GetCharArrayElements((jcharArray) arrayOrig[CHAR], 0); CE
  shortOrig = env->GetShortArrayElements((jshortArray) arrayOrig[SHORT], 0); CE
  intOrig = env->GetIntArrayElements((jintArray) arrayOrig[INT], 0); CE
  longOrig = env->GetLongArrayElements((jlongArray) arrayOrig[LONG], 0); CE
  floatOrig = env->GetFloatArrayElements((jfloatArray) arrayOrig[FLOAT], 0); CE
  doubleOrig = env->GetDoubleArrayElements((jdoubleArray) arrayOrig[DOUBLE], 0); CE

  /* Alloc some memory for cloned arrays buffers */
  boolClone = (jboolean *)c_malloc(env, SIZE(BOOL) * sizeof(jboolean));
  byteClone = (jbyte *)c_malloc(env, SIZE(BYTE) * sizeof(jbyte));
  charClone = (jchar *)c_malloc(env, SIZE(CHAR) * sizeof(jchar));
  shortClone = (jshort *)c_malloc(env, SIZE(SHORT) * sizeof(jshort));
  intClone = (jint *)c_malloc(env, SIZE(INT) * sizeof(jint));
  longClone = (jlong *)c_malloc(env, SIZE(LONG) * sizeof(jlong));
  floatClone = (jfloat *)c_malloc(env, SIZE(FLOAT) * sizeof(jfloat));
  doubleClone = (jdouble *)c_malloc(env, SIZE(DOUBLE) * sizeof(jdouble));

  /* Take the elements from cloned Java arrays into native buffers */
  /* Use Get<Type>ArrayRegion */
  env->GetBooleanArrayRegion((jbooleanArray) arrayClone[BOOL], 0, SIZE(BOOL), boolClone); CE
  env->GetByteArrayRegion((jbyteArray) arrayClone[BYTE], 0, SIZE(BYTE), byteClone); CE
  env->GetCharArrayRegion((jcharArray) arrayClone[CHAR], 0, SIZE(CHAR), charClone); CE
  env->GetShortArrayRegion((jshortArray) arrayClone[SHORT], 0, SIZE(SHORT), shortClone); CE
  env->GetIntArrayRegion((jintArray) arrayClone[INT], 0, SIZE(INT), intClone); CE
  env->GetLongArrayRegion((jlongArray) arrayClone[LONG], 0, SIZE(LONG), longClone); CE
  env->GetFloatArrayRegion((jfloatArray) arrayClone[FLOAT], 0, SIZE(FLOAT), floatClone); CE
  env->GetDoubleArrayRegion((jdoubleArray) arrayClone[DOUBLE], 0, SIZE(DOUBLE), doubleClone); CE

  /* In this code section I should make some changes for elements into both */
  /* (original and cloned) arrays and than copied new values back to Java */

  /*
    Can't change the pointer into the Java structure.  It's illegal JNI.
    SWAP(boolOrig, boolClone)
    SWAP(byteOrig, byteClone)
    SWAP(charOrig, charClone)
    SWAP(shortOrig, shortClone)
    SWAP(intOrig, intClone)
    SWAP(longOrig, longClone)
    SWAP(floatOrig, floatClone)
    SWAP(doubleOrig, doubleClone)
  */

  /* Coping new values of elements back to Java and releasing native buffers */
  /* Use Set<Type>ArrayRegion */
  /*
    Use Orig pointers to get the original effect of the test.
    env->SetBooleanArrayRegion(arrayClone[BOOL], 0,SIZE(BOOL), boolClone);
    env->SetByteArrayRegion(arrayClone[BYTE], 0,SIZE(BYTE), byteClone);
    env->SetCharArrayRegion(arrayClone[CHAR], 0,SIZE(CHAR), charClone);
    env->SetShortArrayRegion(arrayClone[SHORT], 0,SIZE(SHORT), shortClone);
    env->SetIntArrayRegion(arrayClone[INT], 0,SIZE(INT), intClone);
    env->SetLongArrayRegion(arrayClone[LONG], 0,SIZE(LONG), longClone);
    env->SetFloatArrayRegion(arrayClone[FLOAT], 0,SIZE(FLOAT), floatClone);
    env->SetDoubleArrayRegion(arrayClone[DOUBLE], 0,SIZE(DOUBLE), doubleClone);
  */
  env->SetBooleanArrayRegion((jbooleanArray) arrayClone[BOOL], 0, SIZE(BOOL), boolOrig); CE
  env->SetByteArrayRegion((jbyteArray) arrayClone[BYTE], 0, SIZE(BYTE), byteOrig); CE
  env->SetCharArrayRegion((jcharArray) arrayClone[CHAR], 0, SIZE(CHAR), charOrig); CE
  env->SetShortArrayRegion((jshortArray) arrayClone[SHORT], 0, SIZE(SHORT), shortOrig); CE
  env->SetIntArrayRegion((jintArray) arrayClone[INT], 0, SIZE(INT), intOrig); CE
  env->SetLongArrayRegion((jlongArray) arrayClone[LONG], 0, SIZE(LONG), longOrig); CE
  env->SetFloatArrayRegion((jfloatArray) arrayClone[FLOAT], 0, SIZE(FLOAT), floatOrig); CE
  env->SetDoubleArrayRegion((jdoubleArray) arrayClone[DOUBLE], 0, SIZE(DOUBLE), doubleOrig); CE

  /* Use Release<Type>ArrayElements */
  env->ReleaseDoubleArrayElements((jdoubleArray) arrayOrig[DOUBLE], doubleOrig, 0); CE
  env->ReleaseFloatArrayElements((jfloatArray) arrayOrig[FLOAT], floatOrig, 0); CE
  env->ReleaseLongArrayElements((jlongArray) arrayOrig[LONG], longOrig, 0); CE
  env->ReleaseIntArrayElements((jintArray) arrayOrig[INT], intOrig, 0); CE
  env->ReleaseShortArrayElements((jshortArray) arrayOrig[SHORT], shortOrig, 0); CE
  env->ReleaseCharArrayElements((jcharArray) arrayOrig[CHAR], charOrig, 0); CE
  env->ReleaseByteArrayElements((jbyteArray) arrayOrig[BYTE], byteOrig, 0); CE
  env->ReleaseBooleanArrayElements((jbooleanArray) arrayOrig[BOOL], boolOrig, 0); CE
  free(arrayOrig);

  free(doubleClone);
  free(floatClone);
  free(longClone);
  free(intClone);
  free(shortClone);
  free(byteClone);
  free(charClone);
  free(boolClone);
  free(arrayClone);

  return JNI_TRUE;
}

}
