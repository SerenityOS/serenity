/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibPthread/pthread.h>

struct PthreadFunctions {
    int (*pthread_mutex_trylock)(pthread_mutex_t* mutex);
    int (*pthread_mutex_destroy)(pthread_mutex_t*);

    int (*pthread_mutexattr_init)(pthread_mutexattr_t*);
    int (*pthread_mutexattr_settype)(pthread_mutexattr_t*, int);
    int (*pthread_mutexattr_destroy)(pthread_mutexattr_t*);

    int (*pthread_once)(pthread_once_t*, void (*)(void));

    int (*pthread_cond_broadcast)(pthread_cond_t*);
    int (*pthread_cond_init)(pthread_cond_t*, const pthread_condattr_t*);
    int (*pthread_cond_signal)(pthread_cond_t*);
    int (*pthread_cond_wait)(pthread_cond_t*, pthread_mutex_t*);
    int (*pthread_cond_destroy)(pthread_cond_t*);
    int (*pthread_cond_timedwait)(pthread_cond_t*, pthread_mutex_t*, const struct timespec*);
};

void __init_pthread_forward(PthreadFunctions);
