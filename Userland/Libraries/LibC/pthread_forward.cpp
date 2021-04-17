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

#include <LibC/assert.h>
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
