/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <AK/Types.h>
#include <Kernel/Arch/x86/CPU.h>
#include <Kernel/Forward.h>

namespace Kernel {

template<typename BaseType = u32>
class SpinLock {
    AK_MAKE_NONCOPYABLE(SpinLock);
    AK_MAKE_NONMOVABLE(SpinLock);

public:
    SpinLock() = default;

    ALWAYS_INLINE u32 lock()
    {
        u32 prev_flags;
        Processor::enter_critical(prev_flags);
        cli();
        while (m_lock.exchange(1, AK::memory_order_acquire) != 0) {
            Processor::wait_check();
        }
        return prev_flags;
    }

    ALWAYS_INLINE void unlock(u32 prev_flags)
    {
        VERIFY(is_locked());
        m_lock.store(0, AK::memory_order_release);
        if (prev_flags & 0x200)
            sti();
        else
            cli();
        Processor::leave_critical(prev_flags);
    }

    [[nodiscard]] ALWAYS_INLINE bool is_locked() const
    {
        return m_lock.load(AK::memory_order_relaxed) != 0;
    }

    ALWAYS_INLINE void initialize()
    {
        m_lock.store(0, AK::memory_order_relaxed);
    }

private:
    Atomic<BaseType> m_lock { 0 };
};

class RecursiveSpinLock {
    AK_MAKE_NONCOPYABLE(RecursiveSpinLock);
    AK_MAKE_NONMOVABLE(RecursiveSpinLock);

public:
    RecursiveSpinLock() = default;

    ALWAYS_INLINE u32 lock()
    {
        u32 prev_flags;
        Processor::enter_critical(prev_flags);
        cli();
        auto& proc = Processor::current();
        FlatPtr cpu = FlatPtr(&proc);
        FlatPtr expected = 0;
        while (!m_lock.compare_exchange_strong(expected, cpu, AK::memory_order_acq_rel)) {
            if (expected == cpu)
                break;
            Processor::wait_check();
            expected = 0;
        }
        m_recursions++;
        return prev_flags;
    }

    ALWAYS_INLINE void unlock(u32 prev_flags)
    {
        VERIFY(m_recursions > 0);
        VERIFY(m_lock.load(AK::memory_order_relaxed) == FlatPtr(&Processor::current()));
        if (--m_recursions == 0)
            m_lock.store(0, AK::memory_order_release);
        if (prev_flags & 0x200)
            sti();
        else
            cli();
        Processor::leave_critical(prev_flags);
    }

    [[nodiscard]] ALWAYS_INLINE bool is_locked() const
    {
        return m_lock.load(AK::memory_order_relaxed) != 0;
    }

    [[nodiscard]] ALWAYS_INLINE bool own_lock() const
    {
        return m_lock.load(AK::memory_order_relaxed) == FlatPtr(&Processor::current());
    }

    [[nodiscard]] ALWAYS_INLINE u32 own_recursions() const
    {
        if (own_lock()) {
            atomic_thread_fence(AK::MemoryOrder::memory_order_acquire);
            return m_recursions;
        }
        return 0;
    }

    ALWAYS_INLINE void initialize()
    {
        m_lock.store(0, AK::memory_order_relaxed);
    }

private:
    Atomic<FlatPtr> m_lock { 0 };
    u32 m_recursions { 0 };
};

template<typename LockType>
class [[nodiscard]] ScopedSpinLock {

    AK_MAKE_NONCOPYABLE(ScopedSpinLock);

public:
    ScopedSpinLock() = delete;
    ScopedSpinLock& operator=(ScopedSpinLock&&) = delete;

    ScopedSpinLock(LockType& lock)
        : m_lock(&lock)
    {
        VERIFY(m_lock);
        m_prev_flags = m_lock->lock();
        m_have_lock = true;
    }

    ScopedSpinLock(ScopedSpinLock&& from)
        : m_lock(from.m_lock)
        , m_prev_flags(from.m_prev_flags)
        , m_have_lock(from.m_have_lock)
    {
        from.m_lock = nullptr;
        from.m_prev_flags = 0;
        from.m_have_lock = false;
    }

    ~ScopedSpinLock()
    {
        if (m_lock && m_have_lock) {
            m_lock->unlock(m_prev_flags);
        }
    }

    ALWAYS_INLINE void lock()
    {
        VERIFY(m_lock);
        VERIFY(!m_have_lock);
        m_prev_flags = m_lock->lock();
        m_have_lock = true;
    }

    ALWAYS_INLINE void unlock()
    {
        VERIFY(m_lock);
        VERIFY(m_have_lock);
        m_lock->unlock(m_prev_flags);
        m_prev_flags = 0;
        m_have_lock = false;
    }

    [[nodiscard]] ALWAYS_INLINE bool have_lock() const
    {
        return m_have_lock;
    }

private:
    LockType* m_lock { nullptr };
    u32 m_prev_flags { 0 };
    bool m_have_lock { false };
};

enum class SharedSpinLockMode {
    Shared = 0,
    Exclusive
};

class SharedSpinLock {
    AK_MAKE_NONCOPYABLE(SharedSpinLock);
    AK_MAKE_NONMOVABLE(SharedSpinLock);
    // This spin lock allows multiple shared readers simultaneously while allowing
    // only one exclusive writer at a time, which also blocks readers until the writer
    // is done.

public:
    SharedSpinLock() = default;

    template<SharedSpinLockMode Mode>
    ALWAYS_INLINE u32 lock()
    {
        u32 prev_flags;
        Processor::enter_critical(prev_flags);
        cli();
        u32 expected = 0;
        if constexpr (Mode == SharedSpinLockMode::Shared) {
            while (!m_lock.compare_exchange_strong(expected, expected + 2, AK::memory_order_acquire)) {
                if (expected & 1) {
                    expected &= ~1;
                    // Write pending or in progress
                    Processor::wait_check();
                }
            }
        } else {
            while (!m_lock.compare_exchange_strong(expected, expected | 1, AK::memory_order_acquire)) {
                if (expected & 1) {
                    expected &= ~1;
                    // Another writer is in progress
                    Processor::wait_check();
                }
            }
            while (expected != 1) {
                // A read is still in progress
                Processor::wait_check();
                expected = m_lock.load(AK::memory_order_relaxed);
                VERIFY(expected & 1);
            }
        }
        return prev_flags;
    }

    template<SharedSpinLockMode Mode>
    ALWAYS_INLINE void unlock(u32 prev_flags)
    {
        if constexpr (Mode == SharedSpinLockMode::Shared)
            m_lock.fetch_sub(2, AK::memory_order_release);
        else
            m_lock.store(0, AK::memory_order_release);
        if (prev_flags & 0x200)
            sti();
        else
            cli();
        Processor::leave_critical(prev_flags);
    }

private:
    Atomic<u32> m_lock { 0 };
};

class RecursiveSharedSpinLock {
    AK_MAKE_NONCOPYABLE(RecursiveSharedSpinLock);
    AK_MAKE_NONMOVABLE(RecursiveSharedSpinLock);
    // This spin lock allows multiple shared readers simultaneously while allowing
    // only one exclusive writer at a time, which also blocks readers until the writer
    // is done. The exclusive writer is allowed to acquire additional shared or exclusive
    // locks recursively while holding the exclusive lock.

public:
    RecursiveSharedSpinLock() = default;

    [[nodiscard]] ALWAYS_INLINE bool own_exclusive() const
    {
        if (!(m_lock.load(AK::memory_order_relaxed) & 1))
            return false;
        // m_exclusive_owner may be stale, but if it matches our current
        // processor it must be a recursive lock!
        return m_exclusive_owner == FlatPtr(&Processor::current());
    }

    [[nodiscard]] ALWAYS_INLINE u32 own_recursions() const
    {
        return own_exclusive() ? m_recursions : 0;
    }

    template<SharedSpinLockMode Mode>
    ALWAYS_INLINE u32 lock()
    {
        u32 prev_flags;
        Processor::enter_critical(prev_flags);
        cli();
        u32 expected = 0;
        if constexpr (Mode == SharedSpinLockMode::Shared) {
            while (!m_lock.compare_exchange_strong(expected, expected + 2, AK::memory_order_acquire)) {
                if (expected & 1) {
                    // m_exclusive_owner may be stale, but if it matches our current
                    // processor it must be a recursive lock!
                    if (m_exclusive_owner == FlatPtr(&Processor::current())) {
                        // We already own an exclusive lock, add a reference.
                        // Note that we can't use m_lock for this purpose as
                        // we need to be able to distinguish other shared
                        // readers from recursive references.
                        m_recursions++;
                        break;
                    }
                    expected &= ~1;
                    // Write pending or in progress
                    Processor::wait_check();
                }
            }
        } else {
            while (!m_lock.compare_exchange_strong(expected, expected | 1, AK::memory_order_acquire)) {
                if (expected & 1) {
                    // m_exclusive_owner may be stale, but if it matches our current
                    // processor it must be a recursive lock!
                    if (m_exclusive_owner == FlatPtr(&Processor::current())) {
                        // We already own an exclusive lock, add a reference
                        // Note that we can't use m_lock for this purpose as
                        // we need to be able to distinguish other shared
                        // readers from recursive references.
                        m_recursions++;
                        expected = 0;
                        break;
                    }
                    expected &= ~1;
                    // Another writer is in progress
                    Processor::wait_check();
                }
            }
            if (!m_exclusive_owner) {
                m_exclusive_owner = FlatPtr(&Processor::current());
                VERIFY(m_recursions == 0);
                m_recursions = 1;
            } else {
                VERIFY(m_recursions > 1);
            }
            while (expected != 0) {
                // A read is still in progress
                Processor::wait_check();
                expected = m_lock.load(AK::memory_order_relaxed);
                VERIFY(expected & 1);
            }
        }
        return prev_flags;
    }

    template<SharedSpinLockMode Mode>
    ALWAYS_INLINE void unlock(u32 prev_flags)
    {
        if constexpr (Mode == SharedSpinLockMode::Shared) {
            // m_exclusive_owner may be stale, but if it matches our current
            // processor it must be a recursive lock!
            if (m_exclusive_owner == FlatPtr(&Processor::current())) {
                VERIFY(m_recursions > 1);
                m_recursions--;
            }
            m_lock.fetch_sub(2, AK::memory_order_release);
        } else {
            VERIFY(m_recursions > 0);
            VERIFY(m_exclusive_owner != 0);
            VERIFY(m_exclusive_owner == FlatPtr(&Processor::current()));
            if (--m_recursions == 0) {
                m_exclusive_owner = 0;
                m_lock.store(0, AK::memory_order_release);
            }
        }
        if (prev_flags & 0x200)
            sti();
        else
            cli();
        Processor::leave_critical(prev_flags);
    }

private:
    Atomic<u32> m_lock { 0 };
    FlatPtr m_exclusive_owner { 0 };
    u32 m_recursions { 0 };
};

template<SharedSpinLockMode Mode, typename LockType>
class ScopedSharedSpinLockBase {

    AK_MAKE_NONCOPYABLE(ScopedSharedSpinLockBase);

public:
    ScopedSharedSpinLockBase() = delete;
    ScopedSharedSpinLockBase& operator=(ScopedSharedSpinLockBase&&) = delete;

    ScopedSharedSpinLockBase(LockType& lock)
        : m_lock(&lock)
    {
        VERIFY(m_lock);
        m_prev_flags = m_lock->template lock<Mode>();
        m_have_lock = true;
    }

    ScopedSharedSpinLockBase(ScopedSharedSpinLockBase&& from)
        : m_lock(from.m_lock)
        , m_prev_flags(from.m_prev_flags)
        , m_have_lock(from.m_have_lock)
    {
        from.m_lock = nullptr;
        from.m_prev_flags = 0;
        from.m_have_lock = false;
    }

    ~ScopedSharedSpinLockBase()
    {
        if (m_lock && m_have_lock) {
            m_lock->template unlock<Mode>(m_prev_flags);
        }
    }

    ALWAYS_INLINE void lock()
    {
        VERIFY(m_lock);
        VERIFY(!m_have_lock);
        m_prev_flags = m_lock->template lock<Mode>();
        m_have_lock = true;
    }

    ALWAYS_INLINE void unlock()
    {
        VERIFY(m_lock);
        VERIFY(m_have_lock);
        m_lock->template unlock<Mode>(m_prev_flags);
        m_prev_flags = 0;
        m_have_lock = false;
    }

    [[nodiscard]] ALWAYS_INLINE bool own_lock() const requires(Mode == SharedSpinLockMode::Exclusive)
    {
        if (!m_have_lock)
            return false;
        return m_lock->own_exclusive();
    }

private:
    LockType* m_lock { nullptr };
    u32 m_prev_flags { 0 };
    bool m_have_lock { false };
};

template<typename LockType>
class [[nodiscard]] ScopedExclusiveSpinLock : public ScopedSharedSpinLockBase<SharedSpinLockMode::Exclusive, LockType> {
public:
    ScopedExclusiveSpinLock(LockType& lock)
        : ScopedSharedSpinLockBase<SharedSpinLockMode::Exclusive, LockType>(lock)
    {
    }
    ScopedExclusiveSpinLock(ScopedExclusiveSpinLock&& from)
        : ScopedSharedSpinLockBase<SharedSpinLockMode::Exclusive, LockType>(from)
    {
    }
};

template<typename LockType>
class [[nodiscard]] ScopedSharedSpinLock : public ScopedSharedSpinLockBase<SharedSpinLockMode::Shared, LockType> {
public:
    ScopedSharedSpinLock(LockType& lock)
        : ScopedSharedSpinLockBase<SharedSpinLockMode::Shared, LockType>(lock)
    {
    }
    ScopedSharedSpinLock(ScopedSharedSpinLock&& from)
        : ScopedSharedSpinLockBase<SharedSpinLockMode::Exclusive, LockType>(from)
    {
    }
};

}
