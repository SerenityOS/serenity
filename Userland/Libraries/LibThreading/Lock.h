/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <pthread.h>

namespace Threading {

class Lock {
public:
    Lock()
    {
#ifndef __serenity__
        pthread_mutexattr_t attr;
        pthread_mutexattr_init(&attr);
        pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_RECURSIVE);
        pthread_mutex_init(&m_mutex, &attr);
#endif
    }
    ~Lock() { }

    void lock();
    void unlock();

private:
#ifdef __serenity__
    pthread_mutex_t m_mutex = PTHREAD_RECURSIVE_MUTEX_INITIALIZER_NP;
#else
    pthread_mutex_t m_mutex;
#endif
};

class Locker {
public:
    ALWAYS_INLINE explicit Locker(Lock& l)
        : m_lock(l)
    {
        lock();
    }
    ALWAYS_INLINE ~Locker() { unlock(); }
    ALWAYS_INLINE void unlock() { m_lock.unlock(); }
    ALWAYS_INLINE void lock() { m_lock.lock(); }

private:
    Lock& m_lock;
};

ALWAYS_INLINE void Lock::lock()
{
    pthread_mutex_lock(&m_mutex);
}

inline void Lock::unlock()
{
    pthread_mutex_unlock(&m_mutex);
}

}
