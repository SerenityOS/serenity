/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

extern "C" {

JNIEXPORT void JNICALL
Java_gc_gctests_nativeGC03_nativeGC03_nativeMethod03
(JNIEnv *env, jobject obj, jobjectArray listHolder) {
        jsize len;
        int i, count;
        jmethodID mid;
        jclass clss;

        len = env->GetArrayLength(listHolder);
        i = 0;
        count = 0;
        /*Trash all the linked lists */
        while (count < 10) {
                while (i < len) {
                        env->SetObjectArrayElement(listHolder, i, NULL);
                        i++;
                }

                /* Invoke a callback that will refill the array */

                clss = env->GetObjectClass(obj);
                mid = env->GetMethodID(clss, "fillArray", "()V");
                if (mid == 0) {
                        return;
                }
                env->CallVoidMethod(obj, mid);
                count++;
        }

}

}
