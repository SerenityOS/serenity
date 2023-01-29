/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Locking/Spinlock.h>

namespace Kernel {

template<typename T, LockRank Rank>
class SpinlockProtected {
    AK_MAKE_NONCOPYABLE(SpinlockProtected);
    AK_MAKE_NONMOVABLE(SpinlockProtected);

private:
    template<typename U>
    class Locked {
        AK_MAKE_NONCOPYABLE(Locked);
        AK_MAKE_NONMOVABLE(Locked);

    public:
        Locked(U& value, RecursiveSpinlock<Rank>& spinlock)
            : m_value(value)
            , m_locker(spinlock)
        {
        }

        ALWAYS_INLINE U const* operator->() const { return &m_value; }
        ALWAYS_INLINE U const& operator*() const { return m_value; }

        ALWAYS_INLINE U* operator->() { return &m_value; }
        ALWAYS_INLINE U& operator*() { return m_value; }

        ALWAYS_INLINE U const& get() const { return m_value; }
        ALWAYS_INLINE U& get() { return m_value; }

    private:
        U& m_value;
        SpinlockLocker<RecursiveSpinlock<Rank>> m_locker;
    };

    auto lock_const() const { return Locked<T const>(m_value, m_spinlock); }
    auto lock_mutable() { return Locked<T>(m_value, m_spinlock); }

public:
    template<typename... Args>
    SpinlockProtected(Args&&... args)
        : m_value(forward<Args>(args)...)
    {
    }

    template<typename Callback>
    decltype(auto) with(Callback callback) const
    {
        auto lock = lock_const();
        return callback(*lock);
    }

    template<typename Callback>
    decltype(auto) with(Callback callback)
    {
        auto lock = lock_mutable();
        return callback(*lock);
    }

    template<typename Callback>
    void for_each_const(Callback callback) const
    {
        with([&](auto const& value) {
            for (auto& item : value)
                callback(item);
        });
    }

    template<typename Callback>
    void for_each(Callback callback)
    {
        with([&](auto& value) {
            for (auto& item : value)
                callback(item);
        });
    }

private:
    T m_value;
    RecursiveSpinlock<Rank> mutable m_spinlock;
    static constexpr LockRank const m_rank { Rank };
};

}
