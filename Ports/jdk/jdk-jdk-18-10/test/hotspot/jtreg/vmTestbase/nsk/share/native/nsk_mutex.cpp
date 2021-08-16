/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "nsk_mutex.h"

extern "C" {

#ifndef _WIN32

#include <pthread.h>

MUTEX* MUTEX_create()
{
        MUTEX* mutex = (MUTEX*)malloc(sizeof(pthread_mutex_t));
        if (mutex)
        {
                pthread_mutex_init((pthread_mutex_t*)mutex, NULL);
        }
        return mutex;
}

void MUTEX_acquire(MUTEX* mutex)
{
        pthread_mutex_lock((pthread_mutex_t*)mutex);
}

void MUTEX_release(MUTEX* mutex)
{
        pthread_mutex_unlock((pthread_mutex_t*)mutex);
}

void MUTEX_destroy(MUTEX* mutex)
{
        pthread_mutex_destroy((pthread_mutex_t*)mutex);

        free(mutex);
}

#else // _WIN32

#include <windows.h>

MUTEX* MUTEX_create()
{
        MUTEX* mutex = (MUTEX*)malloc(sizeof(CRITICAL_SECTION));
        if (mutex)
        {
                InitializeCriticalSection((PCRITICAL_SECTION)mutex);
        }
        return mutex;
}

void MUTEX_acquire(MUTEX* mutex)
{
        EnterCriticalSection((PCRITICAL_SECTION)mutex);
}

void MUTEX_release(MUTEX* mutex)
{
        LeaveCriticalSection((PCRITICAL_SECTION)mutex);
}

void MUTEX_destroy(MUTEX* mutex)
{
        DeleteCriticalSection((PCRITICAL_SECTION)mutex);

        free(mutex);
}

#endif // _WIN32

}
