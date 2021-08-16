/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "jni_tools.h"

extern "C" {

#define GET_OBJECT_CLASS(_class, _obj)\
    if (!NSK_JNI_VERIFY(env, (_class = \
            env->GetObjectClass(_obj)) != NULL))\
        return 2

#define CALL_STATIC_VOID_NOPARAM(_class, _methodName)\
    GET_STATIC_METHOD_ID(method, _class, _methodName, "()V");\
    if (!NSK_JNI_VERIFY_VOID(env, env->CallStaticVoidMethod(_class, method)))\
        return 2

#define GET_STATIC_METHOD_ID(_methodID, _class, _methodName, _sig)\
    if (!NSK_JNI_VERIFY(env, (_methodID = \
            env->GetStaticMethodID(_class, _methodName, _sig)) != NULL))\
        return 2

#define GET_METHOD_ID(_methodID, _class, _methodName, _sig)\
    if (!NSK_JNI_VERIFY(env, (_methodID = \
            env->GetMethodID(_class, _methodName, _sig)) != NULL))\
        return 2

#define CALL_VOID_NOPARAM(_obj, _class, _methodName)\
    GET_METHOD_ID(method, _class, _methodName, "()V");\
    if (!NSK_JNI_VERIFY_VOID(env, env->CallVoidMethod(_obj, method)))\
        return 2

JNIEXPORT jint JNICALL
Java_nsk_monitoring_stress_thread_RunningThread_recursionNative(JNIEnv *env,
         jobject obj, jint maxDepth, jint currentDepth, jboolean returnToJava) {
    jmethodID method;
    jclass threadClass;

    GET_OBJECT_CLASS(threadClass, obj);
    currentDepth++;

    if (maxDepth > currentDepth) {
        CALL_STATIC_VOID_NOPARAM(threadClass, "yield");

        if (returnToJava) {
            GET_METHOD_ID(method, threadClass, "recursionJava", "(II)V");
            if (!NSK_JNI_VERIFY_VOID(env,
                                     env->CallIntMethod(obj, method, maxDepth, currentDepth))) {
                return 1;
            }
        } else {
            GET_METHOD_ID(method, threadClass, "recursionNative", "(IIZ)I");
            if (!NSK_JNI_VERIFY_VOID(env,
                                     env->CallIntMethod(obj, method, maxDepth, currentDepth, returnToJava))) {
                return 1;
            }
        }
    }
    CALL_VOID_NOPARAM(obj, threadClass, "waitForSign");
    return 0;
}

}
