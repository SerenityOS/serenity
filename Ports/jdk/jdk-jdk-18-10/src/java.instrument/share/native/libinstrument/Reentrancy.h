/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _REENTRANCY_H_
#define _REENTRANCY_H_

#include    <jni.h>

/*
 *  This module provides some utility functions to support the "same thread" re-entrancy management.
 *  Uses JVMTI TLS to store a single bit per thread.
 *  Non-zero means the thread is already inside; zero means the thread is not inside.
 */

#ifdef __cplusplus
extern "C" {
#endif

/* returns true if the token is acquired by this call,
 * false if we already hold it and do not have to acquire it
 */
extern jboolean
tryToAcquireReentrancyToken(    jvmtiEnv *  jvmtienv,
                                jthread     thread);

/* release the token; assumes we already hold it */
extern void
releaseReentrancyToken(         jvmtiEnv *  jvmtienv,
                                jthread     thread);


#ifdef __cplusplus
} /* extern "C" */
#endif /* __cplusplus */


#endif
