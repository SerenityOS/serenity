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

#ifndef NSK_SHARE_NATIVE_NATIVE_THREAD_H
#define NSK_SHARE_NATIVE_NATIVE_THREAD_H

extern "C" {

/**
 * A thread procedure with a void* argument and returning
 * a status.
 */
typedef int(*PROCEDURE)(void*);

/**
 * Return a thread mirror, or NULL if failed.
 */
void* THREAD_new(PROCEDURE procedure, void* context);

/**
 * Return the thread if started OK, or NULL if failed.
 */
void* THREAD_start(void* thread);

/**
 * Return 1 if the thread has been started, or 0 if not,
 * or -1 if thread==NULL.
 */
int THREAD_isStarted(void* thread);

/**
 * Return 1 if the thread has been started and already has finished,
 * or 0 if the thread hasn't finish (or even hasn't been started),
 * or -1 if thread==NULL.
 */
int THREAD_hasFinished(void* thread);

/**
 * Return thread->status if thread has finished,
 * or return 0 if thread hasn't finished,
 * or retuen -1 if thread==NULL.
 */
int THREAD_status(void* thread);

/**
 * Cycle with 1 second sleeps until the thread has finished;
 * or return immediately, if thread==NULL.
 */
void THREAD_waitFor(void* thread);

/**
 * Current thread sleeps.
 */
void THREAD_sleep(int seconds);

}

#endif
