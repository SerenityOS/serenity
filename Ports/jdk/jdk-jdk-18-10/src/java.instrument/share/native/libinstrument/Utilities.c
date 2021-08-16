/*
 * Copyright (c) 2003, 2008, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/*
 * Copyright 2003 Wily Technology, Inc.
 */

#include    <stdlib.h>
#include    <stdio.h>

#include    "JPLISAssert.h"
#include    "Utilities.h"
#include    "JavaExceptions.h"

/*
 *  This module provides various simple JNI and JVMTI utility functionality.
 */

void *
allocate(jvmtiEnv * jvmtienv, size_t bytecount) {
    void *          resultBuffer    = NULL;
    jvmtiError      error           = JVMTI_ERROR_NONE;

    error = (*jvmtienv)->Allocate(jvmtienv,
                                  bytecount,
                                  (unsigned char**) &resultBuffer);
    /* may be called from any phase */
    jplis_assert(error == JVMTI_ERROR_NONE);
    if ( error != JVMTI_ERROR_NONE ) {
        resultBuffer = NULL;
    }
    return resultBuffer;
}

/**
 * Convenience method that deallocates memory.
 * Throws assert on error.
 * JVMTI Deallocate can only fail due to internal error, that is, this
 * agent has done something wrong or JVMTI has done something wrong.  These
 * errors aren't interesting to a JPLIS agent and so are not returned.
 */
void
deallocate(jvmtiEnv * jvmtienv, void * buffer) {
    jvmtiError  error = JVMTI_ERROR_NONE;

    error = (*jvmtienv)->Deallocate(jvmtienv,
                                    (unsigned char*)buffer);
    /* may be called from any phase */
    jplis_assert_msg(error == JVMTI_ERROR_NONE, "Can't deallocate memory");
    return;
}

/**
 *  Returns whether the passed exception is an instance of the given classname
 *  Clears any JNI exceptions before returning
 */
jboolean
isInstanceofClassName(  JNIEnv *        jnienv,
                        jobject         instance,
                        const char *    className) {
    jboolean    isInstanceof        = JNI_FALSE;
    jboolean    errorOutstanding    = JNI_FALSE;
    jclass      classHandle         = NULL;

    jplis_assert(isSafeForJNICalls(jnienv));

    /* get an instance of unchecked exception for instanceof comparison */
    classHandle = (*jnienv)->FindClass(jnienv, className);
    errorOutstanding = checkForAndClearThrowable(jnienv);
    jplis_assert(!errorOutstanding);

    if (!errorOutstanding) {
        isInstanceof = (*jnienv)->IsInstanceOf(jnienv, instance, classHandle);
        errorOutstanding = checkForAndClearThrowable(jnienv);
        jplis_assert(!errorOutstanding);
    }

    jplis_assert(isSafeForJNICalls(jnienv));
    return isInstanceof;
}

/* We don't come back from this
*/
void
abortJVM(   JNIEnv *        jnienv,
            const char *    message) {
    (*jnienv)->FatalError(jnienv, message);
}
