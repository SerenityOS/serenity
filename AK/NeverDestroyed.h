/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/Types.h>

namespace AK {

template<typename T>
class NeverDestroyed {
    AK_MAKE_NONCOPYABLE(NeverDestroyed);
    AK_MAKE_NONMOVABLE(NeverDestroyed);

public:
    template<typename... Args>
    NeverDestroyed(Args&&... args)
    {
        new (storage) T(forward<Args>(args)...);
    }

    ~NeverDestroyed() = default;

    T* operator->() { return &get(); }
    const T* operator->() const { return &get(); }

    T& operator*() { return get(); }
    const T& operator*() const { return get(); }

    T& get() { return reinterpret_cast<T&>(storage); }
    const T& get() const { return reinterpret_cast<T&>(storage); }

private:
    alignas(T) u8 storage[sizeof(T)];
};

}

using AK::NeverDestroyed;
