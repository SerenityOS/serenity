/*
 * Copyright (c) 2003, 2005, Oracle and/or its affiliates. All rights reserved.
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

/* Windows process id's and threads */

#include <process.h>
#include <time.h>
#include <Windows.h>

#define MUTEX_T         int
#define MUTEX_INIT      0
#define MUTEX_LOCK(x)           /* FIXUP? */
#define MUTEX_UNLOCK(x)         /* FIXUP? */
#define GET_THREAD_ID() GetCurrentThreadId()
#define THREAD_T        unsigned long
#define PID_T           int
#define GETPID()        getpid()
#define GETMILLSECS(millisecs) (millisecs=0)

#define popen   _popen
#define pclose  _pclose
#define sleep(s)  Sleep((s)*1000)
