/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Locking/Spinlock.h>

namespace Kernel {

class WaitQueue;

template<typename T, typename Lock>
class SpinlockProtectedBase {
    AK_MAKE_NONCOPYABLE(SpinlockProtectedBase);
    AK_MAKE_NONMOVABLE(SpinlockProtectedBase);
    friend class WaitQueue;

public:
    template<typename... Args>
    SpinlockProtectedBase(Args&&... args)
        : m_value(forward<Args>(args)...)
    {
    }

    template<typename Callback>
    decltype(auto) with(Callback callback) const
    {
        SpinlockLocker<Lock> m_spinlock_locker(m_spinlock); 
        return callback(m_value);
    }

    template<typename Callback>
    decltype(auto) with(Callback callback)
    {
        SpinlockLocker<Lock> m_spinlock_locker(m_spinlock); 
        return callback(m_value);
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
    Lock mutable m_spinlock;
};

}
