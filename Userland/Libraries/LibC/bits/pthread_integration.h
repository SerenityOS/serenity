/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

void __pthread_fork_prepare(void);
void __pthread_fork_child(void);
void __pthread_fork_parent(void);
void __pthread_fork_atfork_register_prepare(void (*)(void));
void __pthread_fork_atfork_register_parent(void (*)(void));
void __pthread_fork_atfork_register_child(void (*)(void));

int __pthread_mutex_init(pthread_mutex_t*, const pthread_mutexattr_t*);
int __pthread_mutex_lock(pthread_mutex_t*);
int __pthread_mutex_trylock(pthread_mutex_t*);
int __pthread_mutex_lock_pessimistic_np(pthread_mutex_t*);
int __pthread_mutex_unlock(pthread_mutex_t*);

typedef void (*KeyDestructor)(void*);

int __pthread_key_create(pthread_key_t*, KeyDestructor);
int __pthread_key_delete(pthread_key_t);
void* __pthread_getspecific(pthread_key_t);
int __pthread_setspecific(pthread_key_t, const void*);

int __pthread_self(void);

void __pthread_key_destroy_for_current_thread(void);

#define __PTHREAD_MUTEX_NORMAL 0
#define __PTHREAD_MUTEX_RECURSIVE 1
#define __PTHREAD_MUTEX_INITIALIZER     \
    {                                   \
        0, 0, 0, __PTHREAD_MUTEX_NORMAL \
    }

#define __PTHREAD_RECURSIVE_MUTEX_INITIALIZER_NP \
    {                                            \
        0, 0, 0, __PTHREAD_MUTEX_RECURSIVE       \
    }

__END_DECLS
