/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibC/bits/pthread_forward.h>

static const PthreadFunctions s_functions = {
    .pthread_mutex_trylock = pthread_mutex_trylock,
    .pthread_mutex_destroy = pthread_mutex_destroy,

    .pthread_mutexattr_init = pthread_mutexattr_init,
    .pthread_mutexattr_settype = pthread_mutexattr_settype,
    .pthread_mutexattr_destroy = pthread_mutexattr_destroy,

    .pthread_once = pthread_once,

    .pthread_cond_broadcast = pthread_cond_broadcast,
    .pthread_cond_init = pthread_cond_init,
    .pthread_cond_signal = pthread_cond_signal,
    .pthread_cond_wait = pthread_cond_wait,
    .pthread_cond_destroy = pthread_cond_destroy,
    .pthread_cond_timedwait = pthread_cond_timedwait,
};

[[gnu::constructor]] static void forward_pthread_functions()
{
    __init_pthread_forward(s_functions);
}
