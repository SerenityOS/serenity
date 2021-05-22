/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Atomic.h>
#include <AK/Types.h>

#ifdef __serenity__
#    include <unistd.h>
#else
#    include <pthread.h>
#endif

namespace Threading {

class Lock {
public:
    Lock() { }
    ~Lock() { }

    void lock();
    void unlock();

private:
#ifdef __serenity__
    using ThreadID = int;

    ALWAYS_INLINE static ThreadID self()
    {
        return gettid();
    }
#else
    using ThreadID = pthread_t;

    ALWAYS_INLINE static ThreadID self()
    {
        return pthread_self();
    }
#endif
    Atomic<ThreadID> m_holder { 0 };
    u32 m_level { 0 };
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
    ThreadID tid = self();
    if (m_holder == tid) {
        ++m_level;
        return;
    }
    for (;;) {
        ThreadID expected = 0;
        if (m_holder.compare_exchange_strong(expected, tid, AK::memory_order_acq_rel)) {
            m_level = 1;
            return;
        }
#ifdef __serenity__
        donate(expected);
#endif
    }
}

inline void Lock::unlock()
{
    VERIFY(m_holder == self());
    VERIFY(m_level);
    if (m_level == 1)
        m_holder.store(0, AK::memory_order_release);
    else
        --m_level;
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
