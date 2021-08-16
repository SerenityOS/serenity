/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>

namespace Kernel {

template<typename T>
class ListedRefCounted : public RefCountedBase {
public:
    bool unref() const
    {
        bool did_hit_zero = T::all_instances().with([&](auto& list) {
            if (deref_base())
                return false;
            list.remove(const_cast<T&>(static_cast<T const&>(*this)));
            return true;
        });
        if (did_hit_zero)
            delete const_cast<T*>(static_cast<T const*>(this));
        return did_hit_zero;
    }
};

}
