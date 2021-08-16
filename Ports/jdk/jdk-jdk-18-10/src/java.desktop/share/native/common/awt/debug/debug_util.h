/*
 * Copyright (c) 1999, 2012, Oracle and/or its affiliates. All rights reserved.
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

#if !defined(_DEBUG_UTIL_H)
#define _DEBUG_UTIL_H

#if defined(__cplusplus)
extern "C" {
#endif

typedef int dbool_t;

#if !defined(TRUE)
#define TRUE 1
#endif
#if !defined(FALSE)
#define FALSE 0
#endif

typedef void * dmutex_t;

#include "jvm.h"
#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include <stddef.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <limits.h>

/* keep these after the other headers */
#include "debug_mem.h"
#include "debug_assert.h"
#include "debug_trace.h"

#if defined(DEBUG)

/* Mutex object mainly for internal debug code use only */
dmutex_t DMutex_Create();
void DMutex_Destroy(dmutex_t);
void DMutex_Enter(dmutex_t);
void DMutex_Exit(dmutex_t);

#endif /* DEBUG */

#if defined(__cplusplus)
} /* extern "C" */
#endif

#endif /* defined(_DEBUG_UTIL_H) */
