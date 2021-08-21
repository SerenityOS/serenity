/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Locking/LockLocation.h>
#include <Kernel/Locking/MutexContendedResource.h>

namespace Kernel {

template<typename T>
class MutexProtected
    : private T
    , public MutexContendedResource {
    AK_MAKE_NONCOPYABLE(MutexProtected);
    AK_MAKE_NONMOVABLE(MutexProtected);

protected:
    using LockedShared = LockedResource<T const, LockMode::Shared>;
    using LockedExclusive = LockedResource<T, LockMode::Exclusive>;

    LockedShared lock_shared(LockLocation const& location) const { return LockedShared(this, this->MutexContendedResource::m_mutex, location); }
    LockedExclusive lock_exclusive(LockLocation const& location) { return LockedExclusive(this, this->MutexContendedResource::m_mutex, location); }

public:
    using T::T;

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
        with_shared([&](const auto& value) {
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
};

}
