/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <stdlib.h>

#include "jni.h"

static jint count = 0;
static jclass test_class;
static jint current_jni_version = JNI_VERSION_10;

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    jclass cl;

    (*vm)->GetEnv(vm, (void **) &env, current_jni_version);

    cl = (*env)->FindClass(env, "NativeLibraryTest");
    test_class = (*env)->NewGlobalRef(env, cl);

    // increment the count when JNI_OnLoad is called
    count++;

    return current_jni_version;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    jmethodID mid;
    jclass cl;

    (*vm)->GetEnv(vm, (void **) &env, current_jni_version);
    mid = (*env)->GetStaticMethodID(env, test_class, "nativeLibraryUnloaded", "()V");
    (*env)->CallStaticVoidMethod(env, test_class, mid);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->FatalError(env, "Exception thrown");
    }

    cl = (*env)->FindClass(env, "p/Test");
    if (cl != NULL) {
        (*env)->FatalError(env, "p/Test class should not be found");
    }
}

JNIEXPORT jint JNICALL
Java_p_Test_count
(JNIEnv *env, jclass cls) {
    return count;
}
