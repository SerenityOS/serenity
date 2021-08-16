/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _UTILITIES_H_
#define _UTILITIES_H_

#include    <jni.h>
#include    <jvmti.h>
#include    "jni_util.h"

#ifdef STATIC_BUILD
#define allocate instAllocate
#define deallocate instDeallocate
#endif


#ifdef __cplusplus
extern "C" {
#endif

/*
 *  This module provides various simple JNI and JVMTI utility functionality.
 */

/*
 *  This allocate must be paired with this deallocate. Used for our own working buffers.
 *  Implementation may vary.
 */
extern void *
allocate(jvmtiEnv * jvmtienv, size_t bytecount);

extern void
deallocate(jvmtiEnv * jvmtienv, void * buffer);


/*
 *  Misc. JNI support
 */
/* convenience wrapper around JNI instanceOf */
extern jboolean
isInstanceofClassName(  JNIEnv*     jnienv,
                        jobject     instance,
                        const char* className);


/* calling this stops the JVM and does not return */
extern void
abortJVM(   JNIEnv *        jnienv,
            const char *    message);


#ifdef __cplusplus
} /* extern "C" */
#endif /* __cplusplus */


#endif
