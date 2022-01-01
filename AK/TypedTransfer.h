/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Traits.h>

namespace AK {

template<typename T>
class TypedTransfer {
private:
    template<typename U>
    static void move_impl(T* destination, U* source, size_t count, auto move_function)
    {
        if (count == 0)
            return;

        if constexpr (Traits<T>::is_trivial()) {
            if (count == 1)
                *destination = *source;
            else
                __builtin_memmove(destination, source, count * sizeof(T));
            return;
        }

        for (size_t i = 0; i < count; ++i) {
            if (destination <= source)
                move_function(&destination[i], &source[i]);
            else
                move_function(&destination[count - i - 1], &source[count - i - 1]);
        }
    }

public:
    static void uninitialized_copy(T* destination, auto const* source, size_t count)
    {
        move_impl(destination, source, count, [](T* destination, auto const* source) { ::new (destination) T(*source); });
    }

    static void move(T* destination, auto* source, size_t count)
    {
        move_impl(destination, source, count, [](T* destination, auto* source) { ::new (destination) T(std::move(*source)); });
    }

    static bool compare(const T* a, const T* b, size_t count)
    {
        if (count == 0)
            return true;

        if constexpr (Traits<T>::is_trivial())
            return !__builtin_memcmp(a, b, count * sizeof(T));

        for (size_t i = 0; i < count; ++i) {
            if (a[i] != b[i])
                return false;
        }

        return true;
    }
};

}
