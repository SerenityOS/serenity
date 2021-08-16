/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Class:     CallWithJNIWeak
 * Method:    testJNIFieldAccessors
 * Signature: (LCallWithJNIWeak;)V
 */
JNIEXPORT void JNICALL
Java_CallWithJNIWeak_testJNIFieldAccessors(JNIEnv *env, jclass clazz, jobject this) {
  // Make sure that we have a weak reference to the receiver

  jweak self = (*env)->NewWeakGlobalRef(env, this);

  jclass this_class = (*env)->GetObjectClass(env, self);

  jclass exception = (*env)->FindClass(env, "java/lang/RuntimeException");

  jfieldID id_i = (*env)->GetFieldID(env, this_class, "i", "I");
  jfieldID id_j = (*env)->GetFieldID(env, this_class, "j", "J");
  jfieldID id_z = (*env)->GetFieldID(env, this_class, "z", "Z");
  jfieldID id_c = (*env)->GetFieldID(env, this_class, "c", "C");
  jfieldID id_s = (*env)->GetFieldID(env, this_class, "s", "S");
  jfieldID id_f = (*env)->GetFieldID(env, this_class, "f", "F");
  jfieldID id_d = (*env)->GetFieldID(env, this_class, "d", "D");
  jfieldID id_l = (*env)->GetFieldID(env, this_class, "l", "Ljava/lang/Object;");
  jvalue v;

#define CHECK(variable, expected)                                   \
  do {                                                              \
    if ((variable) != (expected)) {                                 \
      (*env)->ThrowNew(env, exception,  #variable" != " #expected); \
      return;                                                       \
    }                                                               \
  } while(0)

  // The values checked below must be kept in sync with the Java source file.

  v.i = (*env)->GetIntField(env, self, id_i);
  CHECK(v.i, 1);

  v.j = (*env)->GetLongField(env, self, id_j);
  CHECK(v.j, 2);

  v.z = (*env)->GetBooleanField(env, self, id_z);
  CHECK(v.z, JNI_TRUE);

  v.c = (*env)->GetCharField(env, self, id_c);
  CHECK(v.c, 'a');

  v.s = (*env)->GetShortField(env, self, id_s);
  CHECK(v.s, 3);

  v.f = (*env)->GetFloatField(env, self, id_f);
  CHECK(v.f, 1.0f);

  v.d = (*env)->GetDoubleField(env, self, id_d);
  CHECK(v.d, 2.0);

#undef CHECK

  v.l = (*env)->GetObjectField(env, self, id_l);
  if (v.l == NULL) {
    (*env)->ThrowNew(env, exception, "Object field was null");
    return;
  }
  {
    jclass clz = (*env)->GetObjectClass(env, v.l);
    if (!(*env)->IsSameObject(env, clazz, clz)) {
      (*env)->ThrowNew(env, exception, "Bad object class");
    }
  }

  (*env)->DeleteWeakGlobalRef(env, self);
}

/*
 * Class:     CallWithJNIWeak
 * Method:    runTests
 * Signature: (LCallWithJNIWeak;)V
 */
JNIEXPORT void JNICALL
Java_CallWithJNIWeak_runTests(JNIEnv *env, jclass clazz, jobject this) {
  jweak that = (*env)->NewWeakGlobalRef(env, this);
  {
    jmethodID method = (*env)->GetStaticMethodID(env,
        clazz, "testJNIFieldAccessors", "(LCallWithJNIWeak;)V");
    (*env)->CallStaticVoidMethod(env, clazz, method, that);
    if ((*env)->ExceptionCheck(env)) {
      return;
    }
  }

  {
    jmethodID method = (*env)->GetMethodID(env, clazz, "weakReceiverTest", "()V");
    (*env)->CallVoidMethod(env, that, method);
    if ((*env)->ExceptionCheck(env)) {
      return;
    }
  }

  {
    jmethodID method = (*env)->GetMethodID(env, clazz, "synchonizedWeakReceiverTest", "()V");
    (*env)->CallVoidMethod(env, that, method);
    if ((*env)->ExceptionCheck(env)) {
      return;
    }
  }
  (*env)->DeleteWeakGlobalRef(env, that);
}

/*
 * Class:     CallWithJNIWeak
 * Method:    weakReceiverTest0
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_CallWithJNIWeak_weakReceiverTest0(JNIEnv *env, jobject obj) {
  (*env)->GetObjectClass(env, obj);
}
