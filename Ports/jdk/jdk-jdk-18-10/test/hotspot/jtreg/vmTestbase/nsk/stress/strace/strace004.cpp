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

static const char *Stest_cn="nsk/stress/strace/strace004";
static const char *Slongparam="(J)V";

JNIEXPORT void JNICALL
Java_nsk_stress_strace_strace004Thread_recursiveMethod(JNIEnv *env, jobject obj)
{
    jfieldID field;
    jmethodID method;
    jint currDepth;
    jclass testClass, threadClass, objClass;
    jint maxDepth;
    jboolean isLocked;
    jint achivedCount;
    jobject testField, waitStart;

    FIND_CLASS(testClass, Stest_cn);
    FIND_CLASS(objClass, "java/lang/Object");
    GET_OBJECT_CLASS(threadClass, obj);

    /* currDepth++ */
    GET_INT_FIELD(currDepth, obj, threadClass, "currentDepth");
    currDepth++;
    SET_INT_FIELD(obj, threadClass, "currentDepth", currDepth);

    if (currDepth == 1)
    {
        GET_OBJ_FIELD(testField, obj, threadClass, "test",
                                "Lnsk/stress/strace/strace004;");

        GET_STATIC_OBJ_FIELD(waitStart, testClass, "waitStart",
                                "Ljava/lang/Object;");

        MONITOR_ENTER(testField);
        GET_INT_FIELD(achivedCount, testField, testClass, "achivedCount");
        achivedCount++;
        SET_INT_FIELD(testField, testClass, "achivedCount", achivedCount);
        MONITOR_EXIT(testField);

        GET_STATIC_BOOL_FIELD(isLocked, testClass, "isLocked");
        while (isLocked != JNI_TRUE)
        {
            MONITOR_ENTER(waitStart);
            CALL_VOID(waitStart, objClass, "wait", Slongparam, 1LL);
            MONITOR_EXIT(waitStart);
            GET_STATIC_BOOL_FIELD(isLocked, testClass, "isLocked");
        }
    }

    GET_STATIC_INT_FIELD(maxDepth, testClass, "DEPTH");

    if (maxDepth - currDepth > 0)
    {
        CALL_STATIC_VOID_NOPARAM(threadClass, "yield");
        CALL_VOID_NOPARAM(obj, threadClass, "recursiveMethod");
    }

    currDepth--;
    GET_OBJECT_CLASS(threadClass, obj);
    SET_INT_FIELD(obj, threadClass, "currentDepth", currDepth);
}

}
