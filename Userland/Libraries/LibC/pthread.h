/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Includes essentially mandated by POSIX:
// https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/pthread.h.html
#include <sched.h>
#include <time.h>

#include <bits/pthread_integration.h>
#include <stdint.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

int pthread_create(pthread_t*, pthread_attr_t const*, void* (*)(void*), void*);
void pthread_exit(void*) __attribute__((noreturn));
int pthread_kill(pthread_t, int);
void pthread_cleanup_push(void (*)(void*), void*);
void pthread_cleanup_pop(int);
int pthread_join(pthread_t, void**);
int pthread_mutex_lock(pthread_mutex_t*);
int pthread_mutex_trylock(pthread_mutex_t* mutex);
int pthread_mutex_unlock(pthread_mutex_t*);
int pthread_mutex_init(pthread_mutex_t*, pthread_mutexattr_t const*);
int pthread_mutex_destroy(pthread_mutex_t*);

int pthread_attr_init(pthread_attr_t*);
int pthread_attr_destroy(pthread_attr_t*);

#define PTHREAD_CREATE_JOINABLE 0
#define PTHREAD_CREATE_DETACHED 1

#define PTHREAD_CANCELED ((void*)-1)

int pthread_attr_getdetachstate(pthread_attr_t const*, int*);
int pthread_attr_setdetachstate(pthread_attr_t*, int);

int pthread_attr_getguardsize(pthread_attr_t const*, size_t*);
int pthread_attr_setguardsize(pthread_attr_t*, size_t);

int pthread_attr_getschedparam(pthread_attr_t const*, struct sched_param*);
int pthread_attr_setschedparam(pthread_attr_t*, const struct sched_param*);

int pthread_attr_getstack(pthread_attr_t const*, void**, size_t*);
int pthread_attr_setstack(pthread_attr_t* attr, void*, size_t);

int pthread_attr_getstacksize(pthread_attr_t const*, size_t*);
int pthread_attr_setstacksize(pthread_attr_t*, size_t);

#define PTHREAD_SCOPE_SYSTEM 0
#define PTHREAD_SCOPE_PROCESS 1

int pthread_attr_getscope(pthread_attr_t const*, int*);
int pthread_attr_setscope(pthread_attr_t*, int);

int pthread_once(pthread_once_t*, void (*)(void));
#define PTHREAD_ONCE_INIT 0
void* pthread_getspecific(pthread_key_t key);
int pthread_setspecific(pthread_key_t key, void const* value);

int pthread_getschedparam(pthread_t thread, int* policy, struct sched_param* param);
int pthread_setschedparam(pthread_t thread, int policy, const struct sched_param* param);

#define PTHREAD_MUTEX_NORMAL __PTHREAD_MUTEX_NORMAL
#define PTHREAD_MUTEX_RECURSIVE __PTHREAD_MUTEX_RECURSIVE
#define PTHREAD_MUTEX_DEFAULT PTHREAD_MUTEX_NORMAL
#define PTHREAD_MUTEX_INITIALIZER __PTHREAD_MUTEX_INITIALIZER
#define PTHREAD_RECURSIVE_MUTEX_INITIALIZER_NP __PTHREAD_RECURSIVE_MUTEX_INITIALIZER_NP

#define PTHREAD_PROCESS_PRIVATE 1
#define PTHREAD_PROCESS_SHARED 2

#define PTHREAD_COND_INITIALIZER     \
    {                                \
        0, 0, CLOCK_MONOTONIC_COARSE \
    }

// FIXME: Actually implement this!
#define PTHREAD_RWLOCK_INITIALIZER \
    NULL

#define PTHREAD_KEYS_MAX 64
#define PTHREAD_DESTRUCTOR_ITERATIONS 4

int pthread_key_create(pthread_key_t* key, void (*destructor)(void*));
int pthread_key_delete(pthread_key_t key);
int pthread_cond_broadcast(pthread_cond_t*);
int pthread_cond_init(pthread_cond_t*, pthread_condattr_t const*);
int pthread_cond_signal(pthread_cond_t*);
int pthread_cond_wait(pthread_cond_t*, pthread_mutex_t*);
int pthread_condattr_init(pthread_condattr_t*);
int pthread_condattr_getclock(pthread_condattr_t* attr, clockid_t* clock);
int pthread_condattr_setclock(pthread_condattr_t*, clockid_t);
int pthread_condattr_destroy(pthread_condattr_t*);
int pthread_cond_destroy(pthread_cond_t*);
int pthread_cond_timedwait(pthread_cond_t*, pthread_mutex_t*, const struct timespec*);

#define PTHREAD_CANCEL_ENABLE 1
#define PTHREAD_CANCEL_DISABLE 2

#define PTHREAD_CANCEL_DEFERRED 1
#define PTHREAD_CANCEL_ASYNCHRONOUS 2

int pthread_cancel(pthread_t);
int pthread_setcancelstate(int state, int* oldstate);
int pthread_setcanceltype(int type, int* oldtype);
void pthread_testcancel(void);

int pthread_spin_destroy(pthread_spinlock_t*);
int pthread_spin_init(pthread_spinlock_t*, int);
int pthread_spin_lock(pthread_spinlock_t*);
int pthread_spin_trylock(pthread_spinlock_t*);
int pthread_spin_unlock(pthread_spinlock_t*);
pthread_t pthread_self(void);
int pthread_detach(pthread_t);
int pthread_equal(pthread_t, pthread_t);
int pthread_mutexattr_init(pthread_mutexattr_t*);
int pthread_mutexattr_settype(pthread_mutexattr_t*, int);
int pthread_mutexattr_gettype(pthread_mutexattr_t*, int*);
int pthread_mutexattr_destroy(pthread_mutexattr_t*);

int pthread_setname_np(pthread_t, char const*);
int pthread_getname_np(pthread_t, char*, size_t);

int pthread_equal(pthread_t t1, pthread_t t2);

int pthread_rwlock_destroy(pthread_rwlock_t*);
int pthread_rwlock_init(pthread_rwlock_t* __restrict, pthread_rwlockattr_t const* __restrict);
int pthread_rwlock_rdlock(pthread_rwlock_t*);
int pthread_rwlock_timedrdlock(pthread_rwlock_t* __restrict, const struct timespec* __restrict);
int pthread_rwlock_timedwrlock(pthread_rwlock_t* __restrict, const struct timespec* __restrict);
int pthread_rwlock_tryrdlock(pthread_rwlock_t*);
int pthread_rwlock_trywrlock(pthread_rwlock_t*);
int pthread_rwlock_unlock(pthread_rwlock_t*);
int pthread_rwlock_wrlock(pthread_rwlock_t*);
int pthread_rwlockattr_destroy(pthread_rwlockattr_t*);
int pthread_rwlockattr_getpshared(pthread_rwlockattr_t const* __restrict, int* __restrict);
int pthread_rwlockattr_init(pthread_rwlockattr_t*);
int pthread_rwlockattr_setpshared(pthread_rwlockattr_t*, int);

int pthread_atfork(void (*prepare)(void), void (*parent)(void), void (*child)(void));

__END_DECLS
