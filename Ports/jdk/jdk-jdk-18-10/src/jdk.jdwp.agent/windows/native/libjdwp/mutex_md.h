/*
 * Copyright (c) 1998, 2005, Oracle and/or its affiliates. All rights reserved.
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

/* Copied from JDK 1.2: mutex_md.h      1.13 98/09/21 */

/*
 * Win32 implementation of mutexes. Here we use critical sections as
 * our mutexes. We could have used mutexes, but mutexes are heavier
 * weight than critical sections. Mutexes and critical sections are
 * semantically identical, the only difference being that mutexes
 * can operate between processes (i.e. address spaces).
 *
 * It's worth noting that the Win32 functions supporting critical
 * sections do not provide any error information whatsoever (i.e.
 * all critical section routines return (void)).
 */

#ifndef _JAVASOFT_WIN32_MUTEX_MD_H_
#define _JAVASOFT_WIN32_MUTEX_MD_H_

#include <windows.h>

typedef CRITICAL_SECTION mutex_t;

#define mutexInit(m)    InitializeCriticalSection(m)
#define mutexDestroy(m) DeleteCriticalSection(m)
#define mutexLock(m)    EnterCriticalSection(m)
#define mutexUnlock(m)  LeaveCriticalSection(m)

#endif /* !_JAVASOFT_WIN32_MUTEX_MD_H_ */
