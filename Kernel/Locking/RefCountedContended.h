/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <Kernel/Locking/ContendedResource.h>
#include <Kernel/Locking/LockLocation.h>
#include <Kernel/Locking/Mutex.h>

namespace Kernel {

template<typename T>
class RefCountedContended : public ContendedResource
    , public AK::RefCountedBase {
    AK_MAKE_NONCOPYABLE(RefCountedContended);
    AK_MAKE_NONMOVABLE(RefCountedContended);

protected:
    using LockedShared = LockedResource<T const, LockMode::Shared>;
    using LockedExclusive = LockedResource<T, LockMode::Exclusive>;

    LockedShared lock_shared(LockLocation const& location) const { return LockedShared(static_cast<T const*>(this), this->ContendedResource::m_mutex, location); }
    LockedExclusive lock_exclusive(LockLocation const& location) { return LockedExclusive(static_cast<T*>(this), this->ContendedResource::m_mutex, location); }

public:
    RefCountedContended() = default;

    bool unref() const
    {
        auto new_ref_count = deref_base();
        if (new_ref_count == 0) {
            AK::call_will_be_destroyed_if_present(static_cast<T*>(const_cast<RefCountedContended*>(this)->lock_exclusive().get()));
            delete static_cast<const T*>(this);
            return true;
        } else if (new_ref_count == 1) {
            AK::call_one_ref_left_if_present(static_cast<T*>(const_cast<RefCountedContended*>(this)->lock_exclusive().get()));
        }
        return false;
    }

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
