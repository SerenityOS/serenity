/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Locking/ContendedResource.h>

namespace Kernel {

template<typename T>
class ProtectedValue : private T
    , public ContendedResource {
    AK_MAKE_NONCOPYABLE(ProtectedValue);
    AK_MAKE_NONMOVABLE(ProtectedValue);

protected:
    using LockedShared = LockedResource<T const, LockMode::Shared>;
    using LockedExclusive = LockedResource<T, LockMode::Exclusive>;

    LockedShared lock_shared() const { return LockedShared(this, this->ContendedResource::m_mutex); }
    LockedExclusive lock_exclusive() { return LockedExclusive(this, this->ContendedResource::m_mutex); }

public:
    using T::T;

    ProtectedValue() = default;

    template<typename Callback>
    decltype(auto) with_shared(Callback callback) const
    {
        auto lock = lock_shared();
        return callback(*lock);
    }

    template<typename Callback>
    decltype(auto) with_exclusive(Callback callback)
    {
        auto lock = lock_exclusive();
        return callback(*lock);
    }

    template<typename Callback>
    void for_each_shared(Callback callback) const
    {
        with_shared([&](const auto& value) {
            for (auto& item : value)
                callback(item);
        });
    }

    template<typename Callback>
    void for_each_exclusive(Callback callback)
    {
        with_exclusive([&](auto& value) {
            for (auto& item : value)
                callback(item);
        });
    }
};

}
