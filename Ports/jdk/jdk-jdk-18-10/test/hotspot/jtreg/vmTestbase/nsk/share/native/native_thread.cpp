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

#include <stdlib.h>
#include <stdio.h>

/* testbase_nsk threads: */
#include <native_thread.h>

/***************************************************************/

#ifdef _WIN32

#define windows
#include <windows.h>
#include <process.h> /* for thread routines */
typedef  unsigned int  THREAD_ID;

#else // !_WIN32

#include <unistd.h>

#ifdef sun
#include <thread.h>
typedef  thread_t  THREAD_ID;
#else // !sun
#include <pthread.h>
typedef  pthread_t  THREAD_ID;
#endif // !sun

#endif // !_WIN32

/***************************************************************/

extern "C" {

/**
 * A mirror to control a thread.
 */
typedef
    struct STRUCT_THREAD {
        PROCEDURE procedure;
        void* context;
        /**/
        int started;
        int finished;
        int status;
        /**/
        THREAD_ID id;
    }
    THREAD;

/***************************************************************/

/**
 * Return a new thread mirror, or NULL if failed.
 */
void* THREAD_new(PROCEDURE procedure, void* context) {
    THREAD* thread = (THREAD*)malloc(sizeof(THREAD));
    if (thread == NULL)
        return NULL;
    thread->procedure = procedure;
    thread->context   = context;
    thread->started   = 0; /* No */
    thread->finished  = 0;
    thread->status   = -1; /* undefined */
    return thread;
}

/***************************************************************/

#ifdef windows
unsigned __stdcall procedure(void* t) {
#else // !windows
void* procedure(void* t) {
#endif
    THREAD* thread = (THREAD*)t;
    thread->started  = 1;
    thread->status   = thread->procedure(thread->context);
    thread->finished = 1;
    return 0;
}

/**
 * Return the thread if started OK, or NULL if failed.
 */
void* THREAD_start(void* t) {
    THREAD* thread = (THREAD*)t;
    if (thread == NULL)
        return NULL;
    if (thread->started != 0)
        return NULL;
/*  thread->started  = 0;      -- not yet */
    thread->finished = 0;
    thread->status   = 0;
    {

#ifdef windows
    uintptr_t result = _beginthreadex(NULL,0,procedure,thread,0,&(thread->id));
    if (result == 0) {
        perror("failed to create a native thread");
        return NULL;
    }
#elif sun
    int result = thr_create(NULL,0,procedure,thread,0,&(thread->id));
    if (result != 0) {
        perror("failed to create a native thread");
        return NULL;
    }
#else // !windows & !sun
    int result = pthread_create(&(thread->id),NULL,procedure,thread);
    if (result != 0) {
        perror("failed to create a native thread");
        return NULL;
    }
#endif // !windows & !sun
    };
    return thread;
}

/***************************************************************/

/**
 * Return 1 if the thread has been started, or 0 if not,
 * or -1 if thread == NULL.
 */
int THREAD_isStarted(void* t) {
    THREAD* thread = (THREAD*)t;
    if (thread == NULL)
        return -1;
    return (thread->started == 1);
}

/**
 * Return 1 if the thread has been started and already has finished,
 * or 0 if the thread hasn't finish (or even hasn't been started),
 * or -1 if thread == NULL.
 */
int THREAD_hasFinished(void* t) {
    THREAD* thread = (THREAD*)t;
    if (thread == NULL)
        return -1;
    return (thread->finished == 1);
}

/**
 * Return thread->status if thread has finished,
 * or return 0 if thread hasn't finished,
 * or retuen -1 if thread == NULL.
 */
int THREAD_status(void* t) {
    THREAD* thread = (THREAD*)t;
    if (thread == NULL)
        return -1;
    return thread->status;
}

/***************************************************************/

/**
 * Cycle with 1 second sleeps until the thread has finished;
 * or return immediately, if thread == NULL.
 */
void THREAD_waitFor(void* t) {
    THREAD* thread = (THREAD*)t;
    if (thread == NULL)
        return;
    while (thread->finished == 0)
        THREAD_sleep(1); /* yield for a second */
}

/***************************************************************/

/**
 * Current thread sleeps.
 */
void THREAD_sleep(int seconds) {
#ifdef windows
    Sleep(1000L * seconds);
#else
    sleep(seconds);
#endif
}

/***************************************************************/

}
