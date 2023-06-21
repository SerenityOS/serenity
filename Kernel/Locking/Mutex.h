/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
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
#include <Kernel/Tasks/WaitQueue.h>

namespace Kernel {

class Mutex {
    friend class Thread;

    AK_MAKE_NONCOPYABLE(Mutex);
    AK_MAKE_NONMOVABLE(Mutex);

public:
    using Mode = LockMode;

    // FIXME: remove this after annihilating Process::m_big_lock
    enum class MutexBehavior {
        Regular,
        BigLock,
    };

    Mutex(StringView name = {}, MutexBehavior behavior = MutexBehavior::Regular)
        : m_name(name)
        , m_behavior(behavior)
    {
    }
    ~Mutex() = default;

    void lock(Mode mode = Mode::Exclusive, LockLocation const& location = LockLocation::current());
    void restore_exclusive_lock(u32, LockLocation const& location = LockLocation::current());

    void unlock();
    [[nodiscard]] Mode force_unlock_exclusive_if_locked(u32&);
    [[nodiscard]] bool is_locked() const
    {
        SpinlockLocker lock(m_lock);
        return m_mode != Mode::Unlocked;
    }

    [[nodiscard]] bool is_exclusively_locked_by_current_thread() const
    {
        SpinlockLocker lock(m_lock);
        VERIFY(m_mode != Mode::Shared); // This method should only be used on exclusively-held locks
        if (m_mode == Mode::Unlocked)
            return false;
        return m_holder == bit_cast<uintptr_t>(Thread::current());
    }

    [[nodiscard]] StringView name() const { return m_name; }

    static StringView mode_to_string(Mode mode)
    {
        switch (mode) {
        case Mode::Unlocked:
            return "unlocked"sv;
        case Mode::Exclusive:
            return "exclusive"sv;
        case Mode::Shared:
            return "shared"sv;
        default:
            return "invalid"sv;
        }
    }

private:
    using BlockedThreadList = IntrusiveList<&Thread::m_blocked_threads_list_node>;

    // FIXME: remove this after annihilating Process::m_big_lock
    using BigLockBlockedThreadList = IntrusiveList<&Thread::m_big_lock_blocked_threads_list_node>;

    // FIXME: Allow any lock rank.
    void block(Thread&, Mode, SpinlockLocker<Spinlock<LockRank::None>>&, u32);
    void unblock_waiters(Mode);

    StringView m_name;
    Mode m_mode { Mode::Unlocked };

    // FIXME: remove this after annihilating Process::m_big_lock
    MutexBehavior m_behavior;

    // When locked exclusively, only the thread already holding the lock can
    // lock it again. When locked in shared mode, any thread can do that.
    u32 m_times_locked { 0 };

    // The address of one of the threads that hold this lock, or 0.
    // When locked in shared mode, this is stored on best effort basis: 0 does *not* mean
    // the lock is unlocked, it just means we don't know which threads hold it.
    // When locked exclusively, this is always the one thread that holds the lock.
    uintptr_t m_holder { 0 };
    size_t m_shared_holders { 0 };

    struct BlockedThreadLists {
        BlockedThreadList exclusive;
        BlockedThreadList shared;

        // FIXME: remove this after annihilating Process::m_big_lock
        BigLockBlockedThreadList exclusive_big_lock;

        ALWAYS_INLINE BlockedThreadList& list_for_mode(Mode mode)
        {
            VERIFY(mode == Mode::Exclusive || mode == Mode::Shared);
            return mode == Mode::Exclusive ? exclusive : shared;
        }
    };
    // FIXME: Use a specific lock rank passed by constructor.
    SpinlockProtected<BlockedThreadLists, LockRank::None> m_blocked_thread_lists {};

    // FIXME: See above.
    mutable Spinlock<LockRank::None> m_lock {};

#if LOCK_SHARED_UPGRADE_DEBUG
    HashMap<uintptr_t, u32> m_shared_holders_map;
#endif
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

}
