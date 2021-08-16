/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

// Private interface methods call test
JNIEXPORT jint JNICALL
Java_PrivateInterfaceMethods_callIntVoid(JNIEnv *env, jclass unused, jobject impl, jstring defining_class_name,
                                         jstring method_name, jboolean virtual) {

    // Lookup int method_name() in defining_class_name, and if it exists call impl.method_name()
    // using a virtual or non-virtual invocation as indicated

    jmethodID m_id = NULL;
    jclass clazz = NULL;
    const char* name = NULL;

    name = (*env)->GetStringUTFChars(env, defining_class_name, NULL);
    if (name == NULL) return -1;
    clazz = (*env)->FindClass(env, name);
    (*env)->ReleaseStringUTFChars(env, defining_class_name, name);
    if ((*env)->ExceptionCheck(env)) return -1;

    name = (*env)->GetStringUTFChars(env, method_name, NULL);
    if (name == NULL) return -1;
    m_id = (*env)->GetMethodID(env, clazz, name, "()I");
    (*env)->ReleaseStringUTFChars(env, method_name, name);
    if ((*env)->ExceptionCheck(env)) return -1;

    if (!virtual)
        return (*env)->CallNonvirtualIntMethod(env, impl, clazz, m_id);
    else
        return (*env)->CallIntMethod(env, impl, m_id);
}

// Private interface methods lookup test
JNIEXPORT void JNICALL
Java_PrivateInterfaceMethods_lookupIntVoid(JNIEnv *env, jclass unused,
                                           jstring defining_class_name, jstring method_name) {

    // Lookup int method_name() in defining_class_name

    jmethodID m_id = NULL;
    jclass clazz = NULL;
    const char* name = NULL;

    name = (*env)->GetStringUTFChars(env, defining_class_name, NULL);
    if (name == NULL) return;
    clazz = (*env)->FindClass(env, name);
    (*env)->ReleaseStringUTFChars(env, defining_class_name, name);
    if ((*env)->ExceptionCheck(env)) return;

    name = (*env)->GetStringUTFChars(env, method_name, NULL);
    if (name == NULL) return;
    m_id = (*env)->GetMethodID(env, clazz, name, "()I");
    (*env)->ReleaseStringUTFChars(env, method_name, name);
}

