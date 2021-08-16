/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

//Default methods call test
JNIEXPORT void JNICALL
Java_DefaultMethods_callAndVerify(JNIEnv *env, jclass unused, jobject impl, jstring klass_name, jint expected_result, jint impl_expected_result) {

    jmethodID getOne_id = NULL;
    jint res = 0;
    jclass clazz = NULL;
    const char* class_name = NULL;

    class_name = (*env)->GetStringUTFChars(env, klass_name, NULL);

    clazz = (*env)->FindClass(env, class_name);
    (*env)->ReleaseStringUTFChars(env, klass_name, class_name);
    if (clazz == NULL) {
        (*env)->FatalError(env, "could not find class");
    }

    getOne_id = (*env)->GetMethodID(env, clazz, "getOne", "()I");
    if (getOne_id == NULL) {
        (*env)->FatalError(env, "could not find method");
    }

    res = (*env)->CallNonvirtualIntMethod(env, impl, clazz, getOne_id);

    if (res != expected_result) {
        (*env)->FatalError(env, "wrong return value");
    }

    res = (*env)->CallIntMethod(env, impl, getOne_id);

    if (res != impl_expected_result) {
        (*env)->FatalError(env, "wrong return value");
    }
}
