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
#include <stdio.h>

extern "C" {

JNIEXPORT void JNICALL
Java_gc_gctests_nativeGC05_nativeGC05_kickOffRefillers
(JNIEnv *env, jobject obj, jobject matrix, jobject stack) {
        jclass matrixClass, stackClass, pairClass = 0;
        jmethodID stack_pop_mid, stack_empty_mid, matrix_repopulate_mid, pair_geti_mid = 0, pair_getj_mid = 0;
        jobject pair;
        jint i, j;
        jboolean b;

        /* Get class objects associated with the objects passed in */
        matrixClass    = env->GetObjectClass(matrix);
        stackClass = env->GetObjectClass(stack);

        /* GetMethodID's for the pop() and Repopulate() methods */
        stack_pop_mid  = env->GetMethodID(stackClass, "pop", "()Ljava/lang/Object;");
        if (stack_pop_mid == 0) {
                printf("could not get a methodID for Stack::pop()\n");
                return;
        }
        stack_empty_mid = env->GetMethodID(stackClass, "empty", "()Z");
        if (stack_empty_mid == 0) {
                printf("could not get a methodID for Stack::empty()\n");
                return;
        }

        matrix_repopulate_mid = env->GetMethodID(matrixClass, "repopulate", "(II)V");
        if (matrix_repopulate_mid == 0) {
                printf("could not get a methodID for Matrix::repopulate(int, int)\n");
                return;
        }

        /** b = stack.empty(); */
        b = env->CallBooleanMethod(stack, stack_empty_mid);
        while (b == JNI_FALSE) {
                /** pair = stack.pop() */
                pair = env->CallObjectMethod(stack, stack_pop_mid);

                if (pairClass == 0) {
                        pairClass = env->GetObjectClass(pair);
                        pair_geti_mid = env->GetMethodID(pairClass, "getI", "()I");
                        if (pair_geti_mid == 0) {
                                printf("could not get a methodID for IndexPair::getI()\n");
                                return;
                        }
                        pair_getj_mid = env->GetMethodID(pairClass, "getJ", "()I");
                        if (pair_getj_mid == 0) {
                                printf("could not get a methodID for IndexPair::getJ()\n");
                                return;
                        }
                }

                /** i = pair.getI(); */
                i = env->CallIntMethod(pair, pair_geti_mid);
                /** j = pair.getJ(); */
                j = env->CallIntMethod(pair, pair_getj_mid);

                /*  matrix.repopulate(i, j); */
                env->CallVoidMethod(matrix, matrix_repopulate_mid, i, j);

                /** b = stack.empty(); */
                b = env->CallBooleanMethod(stack, stack_empty_mid);
        }
}

}
