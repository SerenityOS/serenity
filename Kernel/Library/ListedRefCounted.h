/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>

namespace Kernel {

// ListedRefCounted<T> is a slot-in replacement for RefCounted<T> to use in classes
// that add themselves to a {Spinlock, Mutex}Protected<IntrusiveList> when constructed.
// The custom unref() implementation here ensures that the list is locked during
// unref(), and that the T is removed from the list before ~T() is invoked.

enum class LockType {
    Spinlock,
    Mutex,
};

template<typename T, LockType Lock>
class ListedRefCounted : public RefCountedBase {
public:
    bool unref() const
    {
        auto callback = [&](auto& list) {
            auto new_ref_count = deref_base();
            if (new_ref_count == 0)
                list.remove(const_cast<T&>(static_cast<T const&>(*this)));
            return new_ref_count;
        };

        RefCountType new_ref_count;
        if constexpr (Lock == LockType::Spinlock)
            new_ref_count = T::all_instances().with(callback);
        else if constexpr (Lock == LockType::Mutex)
            new_ref_count = T::all_instances().with_exclusive(callback);
        if (new_ref_count == 0) {
            call_will_be_destroyed_if_present(static_cast<const T*>(this));
            delete const_cast<T*>(static_cast<T const*>(this));
        } else if (new_ref_count == 1) {
            call_one_ref_left_if_present(static_cast<T const*>(this));
        }
        return new_ref_count == 0;
    }
};

}
