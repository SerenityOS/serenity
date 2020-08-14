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

#    include <AK/Assertions.h>
#    include <AK/Atomic.h>
#    include <AK/Types.h>
#    include <unistd.h>

namespace LibThread {

class Lock {
public:
    Lock() { }
    ~Lock() { }

    void lock();
    void unlock();

private:
    Atomic<pid_t> m_holder { 0 };
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
    pid_t tid = gettid();
    if (m_holder == tid) {
        ++m_level;
        return;
    }
    for (;;) {
        int expected = 0;
        if (m_holder.compare_exchange_strong(expected, tid, AK::memory_order_acq_rel)) {
            m_level = 1;
            return;
        }
        donate(expected);
    }
}

inline void Lock::unlock()
{
    ASSERT(m_holder == gettid());
    ASSERT(m_level);
    if (m_level == 1)
        m_holder.store(0, AK::memory_order_release);
    else
        --m_level;
}

#    define LOCKER(lock) LibThread::Locker locker(lock)

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

#    define LOCKER(x)

#endif
