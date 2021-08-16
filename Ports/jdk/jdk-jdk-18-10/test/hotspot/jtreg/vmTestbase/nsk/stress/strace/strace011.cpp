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

static const char *Stest_cn="nsk/stress/strace/strace011";
static const char *SthreadName_mn="getName";
static const char *SthreadName_s="()Ljava/lang/String;";
static const char *Slongparam="(J)V";

JNIEXPORT void JNICALL
Java_nsk_stress_strace_strace011Thread_recursiveMethod(JNIEnv *env, jobject obj)
{
    jclass testClass, threadClass;
    jint currDepth, achivedCount, maxDepth;
    jobject testField, lockedObject;
    jboolean isLocked;

    jfieldID field;
    jmethodID method;

    FIND_CLASS(testClass, Stest_cn);
    GET_OBJECT_CLASS(threadClass, obj);

    /* currDepth++ */
    GET_INT_FIELD(currDepth, obj, threadClass, "currentDepth");
    currDepth++;
    SET_INT_FIELD(obj, threadClass, "currentDepth", currDepth);

    GET_STATIC_INT_FIELD(maxDepth, testClass, "DEPTH");


    if (maxDepth - currDepth > 0)
    {
        CALL_VOID_NOPARAM(obj, threadClass, "recursiveMethod");
    }


    if (maxDepth == currDepth)
    {
        int alltime;

        GET_OBJ_FIELD(testField, obj, threadClass, "test",
                                "Lnsk/stress/strace/strace011;");

        MONITOR_ENTER(testField);
        GET_STATIC_INT_FIELD(achivedCount, testClass, "achivedCount");
        achivedCount++;
        SET_STATIC_INT_FIELD(testClass, "achivedCount", achivedCount);
        MONITOR_EXIT(testField);

        alltime = 0;
        GET_STATIC_BOOL_FIELD(isLocked, testClass, "isLocked");

        while (isLocked != JNI_TRUE)
        {
            MONITOR_ENTER(testField);
            CALL_VOID(testField, threadClass, "wait", Slongparam, 1LL);
            alltime++;

            MONITOR_EXIT(testField);

            GET_STATIC_BOOL_FIELD(isLocked, testClass, "isLocked");
        }

        GET_STATIC_OBJ_FIELD(lockedObject, testClass, "lockedObject",
                                "Ljava/lang/Object;");

        MONITOR_ENTER(testField);
        GET_STATIC_INT_FIELD(achivedCount, testClass, "achivedCount");
        achivedCount++;
        SET_STATIC_INT_FIELD(testClass, "achivedCount", achivedCount);
        MONITOR_EXIT(testField);


/*      printf("entering to monitor\n");    */

        MONITOR_ENTER(lockedObject);

/*      printf("exiting from monitor\n");   */

        MONITOR_EXIT(lockedObject);
    }

    currDepth--;
    GET_OBJECT_CLASS(threadClass, obj);
    SET_INT_FIELD(obj, threadClass, "currentDepth", currDepth);
}

}
