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
    Lock() { }
    ~Lock() { }

    void lock();
    void unlock();

private:
    pthread_mutex_t m_mutex = PTHREAD_RECURSIVE_MUTEX_INITIALIZER_NP;
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

template<typename T>
class Lockable {
public:
    Lockable() { }

    template<typename... Args>
    Lockable(Args&&... args)
        : m_resource(forward(args)...)
    {
    }

    Lockable(T&& resource)
        : m_resource(move(resource))
    {
    }
    Lock& lock() { return m_lock; }
    T& resource() { return m_resource; }

    T lock_and_copy()
    {
        Locker locker(m_lock);
        return m_resource;
    }

private:
    T m_resource;
    Lock m_lock;
};

}
