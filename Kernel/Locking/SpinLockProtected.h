/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Locking/SpinLockResource.h>

namespace Kernel {

template<typename T>
class SpinLockProtected
    : private T
    , public SpinLockContendedResource {
    AK_MAKE_NONCOPYABLE(SpinLockProtected);
    AK_MAKE_NONMOVABLE(SpinLockProtected);

protected:
    using LockedConst = SpinLockLockedResource<T const>;
    using LockedMutable = SpinLockLockedResource<T>;

    LockedConst lock_const() const { return LockedConst(static_cast<T const*>(this), this->SpinLockContendedResource::m_spinlock); }
    LockedMutable lock_mutable() { return LockedMutable(static_cast<T*>(this), this->SpinLockContendedResource::m_spinlock); }

public:
    using T::T;

    SpinLockProtected() = default;

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
        with([&](const auto& value) {
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
};

}
