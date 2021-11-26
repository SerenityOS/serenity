/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <LibC/bits/pthread_forward.h>

static PthreadFunctions s_pthread_functions;

void __init_pthread_forward(PthreadFunctions funcs)
{
    s_pthread_functions = funcs;
}

int pthread_mutex_trylock(pthread_mutex_t* mutex)
{
    VERIFY(s_pthread_functions.pthread_mutex_trylock);
    return s_pthread_functions.pthread_mutex_trylock(mutex);
}

int pthread_mutex_destroy(pthread_mutex_t* mutex)
{
    VERIFY(s_pthread_functions.pthread_mutex_destroy);
    return s_pthread_functions.pthread_mutex_destroy(mutex);
}

int pthread_mutexattr_init(pthread_mutexattr_t* attr)
{
    VERIFY(s_pthread_functions.pthread_mutexattr_init);
    return s_pthread_functions.pthread_mutexattr_init(attr);
}

int pthread_mutexattr_settype(pthread_mutexattr_t* attr, int type)
{
    VERIFY(s_pthread_functions.pthread_mutexattr_settype);
    return s_pthread_functions.pthread_mutexattr_settype(attr, type);
}

int pthread_mutexattr_destroy(pthread_mutexattr_t* attr)
{
    VERIFY(s_pthread_functions.pthread_mutexattr_destroy);
    return s_pthread_functions.pthread_mutexattr_destroy(attr);
}

int pthread_once(pthread_once_t* self, void (*callback)(void))
{
    VERIFY(s_pthread_functions.pthread_once);
    return s_pthread_functions.pthread_once(self, callback);
}

int pthread_cond_broadcast(pthread_cond_t* cond)
{
    VERIFY(s_pthread_functions.pthread_cond_broadcast);
    return s_pthread_functions.pthread_cond_broadcast(cond);
}

int pthread_cond_init(pthread_cond_t* cond, const pthread_condattr_t* attr)
{
    VERIFY(s_pthread_functions.pthread_cond_init);
    return s_pthread_functions.pthread_cond_init(cond, attr);
}

int pthread_cond_signal(pthread_cond_t* cond)
{
    VERIFY(s_pthread_functions.pthread_cond_signal);
    return s_pthread_functions.pthread_cond_signal(cond);
}

int pthread_cond_wait(pthread_cond_t* cond, pthread_mutex_t* mutex)
{
    VERIFY(s_pthread_functions.pthread_cond_wait);
    return s_pthread_functions.pthread_cond_wait(cond, mutex);
}

int pthread_cond_destroy(pthread_cond_t* cond)
{
    VERIFY(s_pthread_functions.pthread_cond_destroy);
    return s_pthread_functions.pthread_cond_destroy(cond);
}

int pthread_cond_timedwait(pthread_cond_t* cond, pthread_mutex_t* mutex, const struct timespec* abstime)
{
    VERIFY(s_pthread_functions.pthread_cond_timedwait);
    return s_pthread_functions.pthread_cond_timedwait(cond, mutex, abstime);
}
