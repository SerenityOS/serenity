/*
 * Copyright (c) 2021, Mițca Dumitru <dumitru0mitca@gmail.com>
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

#include <AK/Format.h>
#include <bits/pthread_integration.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>
#include <unistd.h>

extern "C" {

#define STUB(return_type, function_name, signature)                                         \
    [[gnu::weak]] return_type function_name signature;                                      \
    [[gnu::weak]] return_type function_name signature                                       \
    {                                                                                       \
        dbgln("LibC stub for {} called. Did you forget to link pthreads?", #function_name); \
        dump_backtrace();                                                                   \
        abort();                                                                            \
    }

STUB(int, pthread_create, (pthread_t*, pthread_attr_t*, void* (*)(void*), void*))
STUB(void, pthread_exit, (void*))
STUB(int, pthread_kill, (pthread_t, int))
STUB(void, pthread_cleanup_push, (void (*)(void*), void*))
STUB(void, pthread_cleanup_pop, (int))
STUB(int, pthread_join, (pthread_t, void**))
STUB(int, pthread_mutex_lock, (pthread_mutex_t*))
STUB(int, pthread_mutex_trylock, (pthread_mutex_t*))
STUB(int, pthread_mutex_unlock, (pthread_mutex_t*))
STUB(int, pthread_mutex_init, (pthread_mutex_t*, const pthread_mutexattr_t*))
STUB(int, pthread_mutex_destroy, (pthread_mutex_t*))
STUB(int, pthread_attr_init, (pthread_attr_t*))
STUB(int, pthread_attr_destroy, (pthread_attr_t*))
STUB(int, pthread_attr_getdetachstate, (const pthread_attr_t*, int*))
STUB(int, pthread_attr_setdetachstate, (pthread_attr_t*, int))
STUB(int, pthread_attr_getguardsize, (const pthread_attr_t*, size_t*))
STUB(int, pthread_attr_setguardsize, (pthread_attr_t*, size_t))
STUB(int, pthread_attr_getschedparam, (const pthread_attr_t*, struct sched_param*))
STUB(int, pthread_attr_setschedparam, (pthread_attr_t*, const struct sched_param*))
STUB(int, pthread_attr_getstack, (const pthread_attr_t*, void**, size_t*))
STUB(int, pthread_attr_setstack, (pthread_attr_t*, void*, size_t))
STUB(int, pthread_attr_getstacksize, (const pthread_attr_t*, size_t*))
STUB(int, pthread_attr_setstacksize, (pthread_attr_t*, size_t))
STUB(int, pthread_once, (pthread_once_t*, void (*)()))
STUB(void*, pthread_getspecific, (pthread_key_t))
STUB(int, pthread_setspecific, (pthread_key_t, const void*))
STUB(int, pthread_getschedparam, (pthread_t, int*, struct sched_param*))
STUB(int, pthread_setschedparam, (pthread_t, int, const struct sched_param*))
STUB(int, pthread_key_create, (pthread_key_t*, void (*)(void*)))
STUB(int, pthread_key_delete, (pthread_key_t))
STUB(int, pthread_cond_broadcast, (pthread_cond_t*))
STUB(int, pthread_cond_init, (pthread_cond_t*, const pthread_condattr_t*))
STUB(int, pthread_cond_signal, (pthread_cond_t*))
STUB(int, pthread_cond_wait, (pthread_cond_t*, pthread_mutex_t*))
STUB(int, pthread_condattr_init, (pthread_condattr_t*))
STUB(int, pthread_condattr_setclock, (pthread_condattr_t*, clockid_t))
STUB(int, pthread_condattr_destroy, (pthread_condattr_t*))
STUB(int, pthread_cond_destroy, (pthread_cond_t*))
STUB(int, pthread_cond_timedwait, (pthread_cond_t*, pthread_mutex_t*, const struct timespec*))
STUB(int, pthread_cancel, (pthread_t))
STUB(int, pthread_setcancelstate, (int, int*))
STUB(int, pthread_setcanceltype, (int, int*))
STUB(void, pthread_testcancel, (void))
STUB(int, pthread_spin_destroy, (pthread_spinlock_t*))
STUB(int, pthread_spin_init, (pthread_spinlock_t*, int))
STUB(int, pthread_spin_lock, (pthread_spinlock_t*))
STUB(int, pthread_spin_trylock, (pthread_spinlock_t*))
STUB(int, pthread_spin_unlock, (pthread_spinlock_t*))
STUB(pthread_t, pthread_self, ())
STUB(int, pthread_detach, (pthread_t))
STUB(int, pthread_equal, (pthread_t, pthread_t))
STUB(int, pthread_mutexattr_init, (pthread_mutexattr_t*))
STUB(int, pthread_mutexattr_settype, (pthread_mutexattr_t*, int))
STUB(int, pthread_mutexattr_destroy, (pthread_mutexattr_t*))
STUB(int, pthread_setname_np, (pthread_t, const char*))
STUB(int, pthread_getname_np, (pthread_t, char*, size_t))
STUB(int, pthread_rwlock_destroy, (pthread_rwlock_t*))
STUB(int, pthread_rwlock_init, (pthread_rwlock_t * __restrict, const pthread_rwlockattr_t* __restrict))
STUB(int, pthread_rwlock_rdlock, (pthread_rwlock_t*))
STUB(int, pthread_rwlock_timedrdlock, (pthread_rwlock_t * __restrict, const struct timespec* __restrict))
STUB(int, pthread_rwlock_timedwrlock, (pthread_rwlock_t * __restrict, const struct timespec* __restrict))
STUB(int, pthread_rwlock_tryrdlock, (pthread_rwlock_t*))
STUB(int, pthread_rwlock_trywrlock, (pthread_rwlock_t*))
STUB(int, pthread_rwlock_unlock, (pthread_rwlock_t*))
STUB(int, pthread_rwlock_wrlock, (pthread_rwlock_t*))
STUB(int, pthread_rwlockattr_destroy, (pthread_rwlockattr_t*))
STUB(int, pthread_rwlockattr_getpshared, (const pthread_rwlockattr_t* __restrict, int* __restrict))
STUB(int, pthread_rwlockattr_init, (pthread_rwlockattr_t*))
STUB(int, pthread_rwlockattr_setpshared, (pthread_rwlockattr_t*, int))
STUB(int, pthread_atfork, (void (*)(), void (*)(), void (*)()))

#undef STUB
}
