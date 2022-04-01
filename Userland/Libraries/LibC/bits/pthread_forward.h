/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    int (*pthread_cond_init)(pthread_cond_t*, pthread_condattr_t const*);
    int (*pthread_cond_signal)(pthread_cond_t*);
    int (*pthread_cond_wait)(pthread_cond_t*, pthread_mutex_t*);
    int (*pthread_cond_destroy)(pthread_cond_t*);
    int (*pthread_cond_timedwait)(pthread_cond_t*, pthread_mutex_t*, const struct timespec*);
};

void __init_pthread_forward(PthreadFunctions);
