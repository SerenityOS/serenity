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
#include <Kernel/Forward.h>
#include <Kernel/Locking/LockLocation.h>
#include <Kernel/Locking/LockMode.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class Mutex {
    friend class Thread;

    AK_MAKE_NONCOPYABLE(Mutex);
    AK_MAKE_NONMOVABLE(Mutex);

public:
    using Mode = LockMode;

    Mutex(const char* name = nullptr)
        : m_name(name)
    {
    }
    ~Mutex() = default;

    void lock(Mode mode = Mode::Exclusive, LockLocation const& location = LockLocation::current());
    void restore_lock(Mode, u32, LockLocation const& location = LockLocation::current());

    void unlock();
    [[nodiscard]] Mode force_unlock_if_locked(u32&);
    [[nodiscard]] bool is_locked() const
    {
        ScopedSpinlock lock(m_lock);
        return m_mode != Mode::Unlocked;
    }
    [[nodiscard]] bool own_lock() const
    {
        ScopedSpinlock lock(m_lock);
        if (m_mode == Mode::Exclusive)
            return m_holder == Thread::current();
        if (m_mode == Mode::Shared)
            return m_shared_holders.contains(Thread::current());
        return false;
    }

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
    typedef IntrusiveList<Thread, RawPtr<Thread>, &Thread::m_blocked_threads_list_node> BlockedThreadList;

    ALWAYS_INLINE BlockedThreadList& thread_list_for_mode(Mode mode)
    {
        VERIFY(mode == Mode::Exclusive || mode == Mode::Shared);
        return mode == Mode::Exclusive ? m_blocked_threads_list_exclusive : m_blocked_threads_list_shared;
    }

    void block(Thread&, Mode, ScopedSpinlock<Spinlock<u8>>&, u32);
    void unblock_waiters(Mode);

    const char* m_name { nullptr };
    Mode m_mode { Mode::Unlocked };

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

    BlockedThreadList m_blocked_threads_list_exclusive;
    BlockedThreadList m_blocked_threads_list_shared;

    mutable Spinlock<u8> m_lock;
};

class MutexLocker {
    AK_MAKE_NONCOPYABLE(MutexLocker);

public:
    ALWAYS_INLINE explicit MutexLocker()
        : m_lock(nullptr)
        , m_locked(false)
    {
    }

    ALWAYS_INLINE explicit MutexLocker(Mutex& l, Mutex::Mode mode = Mutex::Mode::Exclusive, LockLocation const& location = LockLocation::current())
        : m_lock(&l)
    {
        m_lock->lock(mode, location);
    }

    ALWAYS_INLINE ~MutexLocker()
    {
        if (m_locked)
            unlock();
    }

    ALWAYS_INLINE void unlock()
    {
        VERIFY(m_lock);
        VERIFY(m_locked);
        m_locked = false;
        m_lock->unlock();
    }

    ALWAYS_INLINE void attach_and_lock(Mutex& lock, Mutex::Mode mode = Mutex::Mode::Exclusive, LockLocation const& location = LockLocation::current())
    {
        VERIFY(!m_locked);
        m_lock = &lock;
        m_locked = true;

        m_lock->lock(mode, location);
    }

    ALWAYS_INLINE void lock(Mutex::Mode mode = Mutex::Mode::Exclusive, LockLocation const& location = LockLocation::current())
    {
        VERIFY(m_lock);
        VERIFY(!m_locked);
        m_locked = true;

        m_lock->lock(mode, location);
    }

private:
    Mutex* m_lock;
    bool m_locked { true };
};

class ScopedLockRelease {
    AK_MAKE_NONCOPYABLE(ScopedLockRelease);

public:
    ScopedLockRelease& operator=(ScopedLockRelease&&) = delete;

    ScopedLockRelease(Mutex& lock)
        : m_lock(&lock)
        , m_previous_mode(lock.force_unlock_if_locked(m_previous_recursions))
    {
    }

    ScopedLockRelease(ScopedLockRelease&& from)
        : m_lock(exchange(from.m_lock, nullptr))
        , m_previous_mode(exchange(from.m_previous_mode, Mutex::Mode::Unlocked))
        , m_previous_recursions(exchange(from.m_previous_recursions, 0))
    {
    }

    ~ScopedLockRelease()
    {
        if (m_lock && m_previous_mode != Mutex::Mode::Unlocked)
            m_lock->restore_lock(m_previous_mode, m_previous_recursions);
    }

    void restore_lock()
    {
        VERIFY(m_lock);
        if (m_previous_mode != Mutex::Mode::Unlocked) {
            m_lock->restore_lock(m_previous_mode, m_previous_recursions);
            m_previous_mode = Mutex::Mode::Unlocked;
            m_previous_recursions = 0;
        }
    }

    void do_not_restore()
    {
        VERIFY(m_lock);
        m_previous_mode = Mutex::Mode::Unlocked;
        m_previous_recursions = 0;
    }

private:
    Mutex* m_lock;
    Mutex::Mode m_previous_mode;
    u32 m_previous_recursions;
};

}
