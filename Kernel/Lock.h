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

#include <AK/Assertions.h>
#include <AK/Atomic.h>
#include <AK/Types.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Forward.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class Lock {
public:
    Lock(const char* name = nullptr)
        : m_name(name)
    {
    }
    ~Lock() { }

    enum class Mode {
        Unlocked,
        Shared,
        Exclusive
    };

    void lock(Mode = Mode::Exclusive);
    void unlock();
    bool force_unlock_if_locked();
    bool is_locked() const { return m_holder; }
    void clear_waiters();

    const char* name() const { return m_name; }

private:
    Atomic<bool> m_lock { false };
    const char* m_name { nullptr };
    WaitQueue m_queue;
    Mode m_mode { Mode::Unlocked };

    // When locked exclusively, only the thread already holding the lock can
    // lock it again. When locked in shared mode, any thread can do that.
    u32 m_times_locked { 0 };

    // One of the threads that hold this lock, or nullptr. When locked in shared
    // mode, this is stored on best effort basis: nullptr value does *not* mean
    // the lock is unlocked, it just means we don't know which threads hold it.
    // When locked exclusively, this is always the one thread that holds the
    // lock.
    Thread* m_holder { nullptr };
};

class Locker {
public:
    ALWAYS_INLINE explicit Locker(Lock& l, Lock::Mode mode = Lock::Mode::Exclusive)
        : m_lock(l)
    {
        m_lock.lock(mode);
    }
    ALWAYS_INLINE ~Locker() { unlock(); }
    ALWAYS_INLINE void unlock() { m_lock.unlock(); }
    ALWAYS_INLINE void lock(Lock::Mode mode = Lock::Mode::Exclusive) { m_lock.lock(mode); }

private:
    Lock& m_lock;
};

#define LOCKER(...) Locker locker(__VA_ARGS__)

template<typename T>
class Lockable {
public:
    Lockable() { }
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
