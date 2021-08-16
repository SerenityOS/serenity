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

#include <stdio.h>
#include "nsk_strace.h"

extern "C" {

static const char *Stest_cn="nsk/stress/strace/strace006";

static jclass testClass, threadClass;
static jint DEPTH;
static jclass stackOverflowErrorClass;

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv *env;

    if (vm->GetEnv((void **) &env, JNI_VERSION) != JNI_OK) {
        printf("%s:%d: Failed to call GetEnv\n", __FILE__, __LINE__);
        return 0;
    }

    FIND_CLASS(stackOverflowErrorClass, "java/lang/StackOverflowError");
    stackOverflowErrorClass = (jclass) env->NewGlobalRef(stackOverflowErrorClass);
    if (stackOverflowErrorClass == NULL) {
        printf("Can't create global ref for stack overflow class\n");
        return 0;
    }

    return JNI_VERSION;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved)
{
    JNIEnv *env;

    if (vm->GetEnv((void **) &env, JNI_VERSION) != JNI_OK) {
        if (stackOverflowErrorClass != NULL) {
            env->DeleteGlobalRef(stackOverflowErrorClass);
        }
    } else {
        printf("%s:%d: Failed to call GetEnv\n", __FILE__, __LINE__);
    }

}

JNIEXPORT void JNICALL
Java_nsk_stress_strace_strace006Thread_recursiveMethod2(JNIEnv *env, jobject obj)
{
    jfieldID field;
    jmethodID method;
    jint currDepth;
    jclass testClass, threadClass;
    jint maxDepth;

    FIND_CLASS(testClass, Stest_cn);
    GET_OBJECT_CLASS(threadClass, obj);

    GET_STATIC_INT_FIELD(maxDepth, testClass, "DEPTH");

    /* currDepth++ */
    GET_INT_FIELD(currDepth, obj, threadClass, "currentDepth");
    currDepth++;
    SET_INT_FIELD(obj, threadClass, "currentDepth", currDepth);

    if (maxDepth - currDepth > 0)
    {
        GET_STATIC_METHOD_ID(method, threadClass, "yield", "()V");
        env->CallStaticVoidMethod(threadClass, method);
        EXCEPTION_CHECK(stackOverflowErrorClass, currDepth);

        GET_METHOD_ID(method, threadClass, "recursiveMethod1", "()V");
        env->CallVoidMethod(obj, method);
        EXCEPTION_CHECK(stackOverflowErrorClass, currDepth);
    }

    currDepth--;
    GET_OBJECT_CLASS(threadClass, obj);
    SET_INT_FIELD(obj, threadClass, "currentDepth", currDepth);
}

}
