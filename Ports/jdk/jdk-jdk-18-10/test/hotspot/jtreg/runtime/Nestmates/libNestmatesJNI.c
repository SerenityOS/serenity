/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

// boolean flag for static versus non-static
#define STATIC 1
#define NON_STATIC 0

// Helper methods to get class/method/field IDs

static void getClassID(JNIEnv *env,
                       jstring defining_class_name,
                       jclass* clazz) {
  const char* name = (*env)->GetStringUTFChars(env, defining_class_name, NULL);
  if (name != NULL) {
    *clazz = (*env)->FindClass(env, name);
    (*env)->ReleaseStringUTFChars(env, defining_class_name, name);
  }
}

static void getClassAndMethodID(JNIEnv *env,
                                int is_static,
                                jstring defining_class_name,
                                jstring method_name,
                                const char* sig,
                                jmethodID* m_id, jclass* clazz) {
  getClassID(env, defining_class_name, clazz);
  if (*clazz != NULL) {
    const char* name = (*env)->GetStringUTFChars(env, method_name, NULL);
    if (name != NULL) {
      if (is_static) {
        *m_id = (*env)->GetStaticMethodID(env, *clazz, name, sig);
      } else {
        *m_id = (*env)->GetMethodID(env, *clazz, name, sig);
      }
      (*env)->ReleaseStringUTFChars(env, method_name, name);
    }
  }
}

static void getClassAndFieldID(JNIEnv *env,
                                int is_static,
                                jstring defining_class_name,
                                jstring field_name,
                                const char* sig,
                                jfieldID* m_id, jclass* clazz) {
  getClassID(env, defining_class_name, clazz);
  if (*clazz != NULL) {
    const char* name = (*env)->GetStringUTFChars(env, field_name, NULL);
    if (name != NULL) {
      if (is_static) {
        *m_id = (*env)->GetStaticFieldID(env, *clazz, name, sig);
      } else {
        *m_id = (*env)->GetFieldID(env, *clazz, name, sig);
      }
      (*env)->ReleaseStringUTFChars(env, field_name, name);
    }
  }
}

JNIEXPORT void JNICALL
Java_NestmatesJNI_callVoidVoid(JNIEnv *env, jclass unused,
                               jobject target,
                               jstring defining_class_name,
                               jstring method_name,
                               jboolean virtual) {

  // Lookup "void method_name()" in defining_class_name, and if it exists
  // call target.method_name() using a virtual or non-virtual invocation

  jmethodID m_id = NULL;
  jclass clazz = NULL;
  getClassAndMethodID(env, NON_STATIC, defining_class_name, method_name,
                      "()V", &m_id, &clazz);
  if (m_id != NULL && clazz != NULL) {
    if (!virtual) {
      (*env)->CallNonvirtualVoidMethod(env, target, clazz, m_id);
    } else {
      (*env)->CallVoidMethod(env, target, m_id);
    }
  }
}

JNIEXPORT jobject JNICALL
Java_NestmatesJNI_callStringVoid(JNIEnv *env, jclass unused,
                                 jobject target,
                                 jstring defining_class_name,
                                 jstring method_name,
                                 jboolean virtual) {

  // Lookup "String method_name()" in defining_class_name, and if it exists
  // call target.method_name() using a virtual or non-virtual invocation

  jmethodID m_id = NULL;
  jclass clazz = NULL;
  getClassAndMethodID(env, NON_STATIC, defining_class_name, method_name,
                      "()Ljava/lang/String;", &m_id, &clazz);
  if (m_id != NULL && clazz != NULL) {
    if (!virtual) {
      return (*env)->CallNonvirtualObjectMethod(env, target, clazz, m_id);
    } else {
      return (*env)->CallObjectMethod(env, target, m_id);
    }
  }
  return NULL;
}

JNIEXPORT jobject JNICALL
Java_NestmatesJNI_newInstance0(JNIEnv *env, jclass unused,
                              jstring defining_class_name,
                              jstring method_name,
                              jstring sig,
                              jobject outerThis) {

  // Lookup the no-user-arg constructor in defining_class_name using sig,
  // and use it to create an instance of the class, and return it. For
  // inner classes we need an outerThis reference to pass to the constructor.

  jmethodID m_id = NULL;
  jclass clazz = NULL;
  const char* _sig = (*env)->GetStringUTFChars(env, sig, NULL);
  getClassAndMethodID(env, NON_STATIC, defining_class_name, method_name,
                      _sig, &m_id, &clazz);
  (*env)->ReleaseStringUTFChars(env, sig, _sig);
  if (m_id != NULL && clazz != NULL) {
    return (*env)->NewObject(env, clazz, m_id, outerThis);
  }
  return NULL;
}

JNIEXPORT void JNICALL
Java_NestmatesJNI_callStaticVoidVoid(JNIEnv *env, jclass unused,
                                     jstring defining_class_name,
                                     jstring method_name) {

  // Lookup "static void method_name()" in defining_class_name, and if it exists
  // invoke it.

  jmethodID m_id = NULL;
  jclass clazz = NULL;
  getClassAndMethodID(env, STATIC, defining_class_name, method_name,
                        "()V", &m_id, &clazz);
  if (m_id != NULL && clazz != NULL) {
    (*env)->CallStaticVoidMethod(env, clazz, m_id);
  }
}

JNIEXPORT jint JNICALL
Java_NestmatesJNI_getIntField(JNIEnv *env, jclass unused,
                              jobject target,
                              jstring defining_class_name,
                              jstring field_name) {

  // Lookup field field_name in defining_class_name, and if it exists
  // return its value.

  jfieldID f_id = NULL;
  jclass clazz = NULL;
  getClassAndFieldID(env, NON_STATIC, defining_class_name, field_name,
                     "I", &f_id, &clazz);
  if (f_id != NULL && clazz != NULL) {
    return (*env)->GetIntField(env, target, f_id);
  }
  return -1;
}

JNIEXPORT void JNICALL
Java_NestmatesJNI_setIntField(JNIEnv *env, jclass unused,
                              jobject target,
                              jstring defining_class_name,
                              jstring field_name,
                              jint newVal) {

  // Lookup field field_name in defining_class_name, and if it exists
  // set it to newVal.

  jfieldID f_id = NULL;
  jclass clazz = NULL;
  getClassAndFieldID(env, NON_STATIC, defining_class_name, field_name,
                     "I", &f_id, &clazz);
  if (f_id != NULL && clazz != NULL) {
    (*env)->SetIntField(env, target, f_id, newVal);
  }
}

JNIEXPORT jint JNICALL
Java_NestmatesJNI_getStaticIntField(JNIEnv *env, jclass unused,
                                    jstring defining_class_name,
                                    jstring field_name) {

  // Lookup field field_name in defining_class_name, and if it exists
  // return its value.

  jfieldID f_id = NULL;
  jclass clazz = NULL;
  getClassAndFieldID(env, STATIC, defining_class_name, field_name,
                     "I", &f_id, &clazz);
  if (f_id != NULL && clazz != NULL) {
    return (*env)->GetStaticIntField(env, clazz, f_id);
  }
  return -1;
}

JNIEXPORT void JNICALL
Java_NestmatesJNI_setStaticIntField(JNIEnv *env, jclass unused,
                                    jstring defining_class_name,
                                    jstring field_name,
                                    jint newVal) {

  // Lookup field field_name in defining_class_name, and if it exists
  // set it to newVal.

  jfieldID f_id = NULL;
  jclass clazz = NULL;
  getClassAndFieldID(env, STATIC, defining_class_name, field_name,
                       "I", &f_id, &clazz);
  if (f_id != NULL && clazz != NULL) {
    (*env)->SetStaticIntField(env, clazz, f_id, newVal);
  }
}
