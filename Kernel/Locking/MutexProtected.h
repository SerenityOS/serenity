/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Locking/Mutex.h>

namespace Kernel {

template<typename T>
class MutexProtected {
    AK_MAKE_NONCOPYABLE(MutexProtected);
    AK_MAKE_NONMOVABLE(MutexProtected);

private:
    template<typename U, LockMode lock_mode>
    class Locked {
        AK_MAKE_NONCOPYABLE(Locked);
        AK_MAKE_NONMOVABLE(Locked);

    public:
        Locked(U& value, Mutex& mutex, LockLocation const& location)
            : m_value(value)
            , m_locker(mutex, lock_mode, location)
        {
        }

        ALWAYS_INLINE U const* operator->() const { return &m_value; }
        ALWAYS_INLINE U const& operator*() const { return m_value; }

        ALWAYS_INLINE U* operator->()
        requires(!IsConst<U>)
        {
            return &m_value;
        }
        ALWAYS_INLINE U& operator*()
        requires(!IsConst<U>)
        {
            return m_value;
        }

        ALWAYS_INLINE U const& get() const { return &m_value; }
        ALWAYS_INLINE U& get()
        requires(!IsConst<U>)
        {
            return &m_value;
        }

    private:
        U& m_value;
        MutexLocker m_locker;
    };

    auto lock_shared(LockLocation const& location) const { return Locked<T const, LockMode::Shared>(m_value, m_mutex, location); }
    auto lock_exclusive(LockLocation const& location) { return Locked<T, LockMode::Exclusive>(m_value, m_mutex, location); }

public:
    MutexProtected() = default;

    template<typename Callback>
    decltype(auto) with_shared(Callback callback, LockLocation const& location = LockLocation::current()) const
    {
        auto lock = lock_shared(location);
        return callback(*lock);
    }

    template<typename Callback>
    decltype(auto) with_exclusive(Callback callback, LockLocation const& location = LockLocation::current())
    {
        auto lock = lock_exclusive(location);
        return callback(*lock);
    }

    template<typename Callback>
    void for_each_shared(Callback callback, LockLocation const& location = LockLocation::current()) const
    {
        with_shared([&](auto const& value) {
            for (auto& item : value)
                callback(item);
        },
            location);
    }

    template<typename Callback>
    void for_each_exclusive(Callback callback, LockLocation const& location = LockLocation::current())
    {
        with_exclusive([&](auto& value) {
            for (auto& item : value)
                callback(item);
        },
            location);
    }

private:
    T m_value;
    Mutex mutable m_mutex;
};

}
