/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#ifdef __serenity__

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <AK/Atomic.h>
#include <unistd.h>

namespace LibThread {

class Lock {
public:
    Lock() {}
    ~Lock() {}

    void lock();
    void unlock();

private:
    AK::Atomic<bool> m_lock { false };
    u32 m_level { 0 };
    int m_holder { -1 };
};

class Locker {
public:
    [[gnu::always_inline]] inline explicit Locker(Lock& l)
        : m_lock(l)
    {
        lock();
    }
    [[gnu::always_inline]] inline ~Locker() { unlock(); }
    [[gnu::always_inline]] inline void unlock() { m_lock.unlock(); }
    [[gnu::always_inline]] inline void lock() { m_lock.lock(); }

private:
    Lock& m_lock;
};

[[gnu::always_inline]] inline void Lock::lock()
{
    int tid = gettid();
    for (;;) {
        bool expected = false;
        if (m_lock.compare_exchange_strong(expected, true, AK::memory_order_acq_rel)) {
            if (m_holder == -1 || m_holder == tid) {
                m_holder = tid;
                ++m_level;
                m_lock.store(false, AK::memory_order_release);
                return;
            }
            m_lock.store(false, AK::memory_order_release);
        }
        donate(m_holder);
    }
}

inline void Lock::unlock()
{
    for (;;) {
        bool expected = false;
        if (m_lock.compare_exchange_strong(expected, true, AK::memory_order_acq_rel)) {
            ASSERT(m_holder == gettid());
            ASSERT(m_level);
            --m_level;
            if (m_level) {
                m_lock.store(false, AK::memory_order_release);
                return;
            }
            m_holder = -1;
            m_lock.store(false, AK::memory_order_release);
            return;
        }
        donate(m_holder);
    }
}

#define LOCKER(lock) LibThread::Locker locker(lock)

template<typename T>
class Lockable {
public:
    Lockable() {}
    Lockable(T&& resource)
        : m_resource(move(resource))
    {
    }
    Lock& lock() { return m_lock; }
    T& resource() { return m_resource; }

    T lock_and_copy()
    {
        LOCKER(m_lock);
        return m_resource;
    }

private:
    T m_resource;
    Lock m_lock;
};

}

#else

namespace LibThread {

class Lock {
public:
    Lock() { }
    ~Lock() { }
};

}

#define LOCKER(x)

#endif
