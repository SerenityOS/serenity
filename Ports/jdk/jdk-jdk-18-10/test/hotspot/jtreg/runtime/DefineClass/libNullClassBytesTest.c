/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

JNIEXPORT void JNICALL
Java_NullClassBytesTest_nativeDefineClass(JNIEnv *env, jclass klass, jstring className, jobject ldr,
                                          jbyte* class_bytes, jint length) {
    if (class_bytes == NULL) {
        jclass cls = (*env)->FindClass(env, "java/lang/NullPointerException");

        if (cls != 0) {
            (*env)->ThrowNew(env, cls, "class_bytes are null");
        }
        return;
    }
    const char* c_name = (*env)->GetStringUTFChars(env, className, NULL);
    (*env)->DefineClass(env, c_name, ldr, class_bytes, length);
}
