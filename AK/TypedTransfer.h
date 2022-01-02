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
    static void for_each(T* elements, size_t count, auto unary_function)
    {
        for (; count > 0; ++elements, --count)
            unary_function(elements);
    }

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
    template<typename... Args>
    static void construct(T* destination, size_t count, Args&&... args)
    {
        for_each(destination, count, [&](T* destination) { construct_at(destination, forward(args)...); });
    }

    template<typename... Args>
    static void construct_at(T* destination, Args&&... args)
    {
        ::new (destination) T { forward<Args>(args)... };
    }

    static void uninitialized_copy(T* destination, auto const* source, size_t count)
    {
        move_impl(destination, source, count, [](T* destination, auto const* source) { construct_at(destination, *source); });
    }

    static void uninitialized_move(T* destination, auto* source, size_t count)
    {
        move_impl(destination, source, count, [](T* destination, auto* source) { construct_at(destination, std::move(*source)); });
    }

    static void destroy(T* destination, size_t count)
    {
        for_each(destination, count, destroy_at);
    }

    static void destroy_at(T* destination)
    {
        if constexpr (!Traits<T>::is_trivial()) // FIXME: is_trivially_destructible
            destination->~T();
    }

    static bool equals(const T* a, const T* b, size_t count)
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
