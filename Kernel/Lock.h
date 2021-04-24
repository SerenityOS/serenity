/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Atomic.h>
#include <AK/HashMap.h>
#include <AK/Types.h>
#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/Forward.h>
#include <Kernel/LockMode.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class Lock {
    AK_MAKE_NONCOPYABLE(Lock);
    AK_MAKE_NONMOVABLE(Lock);

public:
    using Mode = LockMode;

    Lock(const char* name = nullptr)
        : m_name(name)
    {
    }
    ~Lock() = default;

#if LOCK_DEBUG
    void lock(Mode mode = Mode::Exclusive, const SourceLocation& location = SourceLocation::current());
    void restore_lock(Mode, u32, const SourceLocation& location = SourceLocation::current());
#else
    void lock(Mode = Mode::Exclusive);
    void restore_lock(Mode, u32);
#endif

    void unlock();
    [[nodiscard]] Mode force_unlock_if_locked(u32&);
    [[nodiscard]] bool is_locked() const { return m_mode != Mode::Unlocked; }
    void clear_waiters();

    [[nodiscard]] const char* name() const { return m_name; }

    static const char* mode_to_string(Mode mode)
    {
        switch (mode) {
        case Mode::Unlocked:
            return "unlocked";
        case Mode::Exclusive:
            return "exclusive";
        case Mode::Shared:
            return "shared";
        default:
            return "invalid";
        }
    }

private:
    Atomic<bool> m_lock { false };
    const char* m_name { nullptr };
    WaitQueue m_queue;
    Atomic<Mode, AK::MemoryOrder::memory_order_relaxed> m_mode { Mode::Unlocked };

    // When locked exclusively, only the thread already holding the lock can
    // lock it again. When locked in shared mode, any thread can do that.
    u32 m_times_locked { 0 };

    // One of the threads that hold this lock, or nullptr. When locked in shared
    // mode, this is stored on best effort basis: nullptr value does *not* mean
    // the lock is unlocked, it just means we don't know which threads hold it.
    // When locked exclusively, this is always the one thread that holds the
    // lock.
    RefPtr<Thread> m_holder;
    HashMap<Thread*, u32> m_shared_holders;
};

class Locker {
public:
#if LOCK_DEBUG
    ALWAYS_INLINE explicit Locker(Lock& l, Lock::Mode mode = Lock::Mode::Exclusive, const SourceLocation& location = SourceLocation::current())
#else
    ALWAYS_INLINE explicit Locker(Lock& l, Lock::Mode mode = Lock::Mode::Exclusive)
#endif
        : m_lock(l)
    {
#if LOCK_DEBUG
        m_lock.lock(mode, location);
#else
        m_lock.lock(mode);
#endif
    }

    ALWAYS_INLINE ~Locker()
    {
        if (m_locked)
            unlock();
    }
    ALWAYS_INLINE void unlock()
    {
        VERIFY(m_locked);
        m_locked = false;
        m_lock.unlock();
    }

#if LOCK_DEBUG
    ALWAYS_INLINE void lock(Lock::Mode mode = Lock::Mode::Exclusive, const SourceLocation& location = SourceLocation::current())
#else
    ALWAYS_INLINE void lock(Lock::Mode mode = Lock::Mode::Exclusive)
#endif
    {
        VERIFY(!m_locked);
        m_locked = true;

#if LOCK_DEBUG
        m_lock.lock(mode, location);
#else
        m_lock.lock(mode);
#endif
    }

private:
    Lock& m_lock;
    bool m_locked { true };
};

template<typename T>
class Lockable {
public:
    Lockable() = default;
    Lockable(T&& resource)
        : m_resource(move(resource))
    {
    }
    [[nodiscard]] Lock& lock() { return m_lock; }
    [[nodiscard]] T& resource() { return m_resource; }

    [[nodiscard]] T lock_and_copy()
    {
        Locker locker(m_lock);
        return m_resource;
    }

private:
    T m_resource;
    Lock m_lock;
};

}
