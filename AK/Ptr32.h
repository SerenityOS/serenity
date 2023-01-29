/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Types.h>

namespace AK {

template<typename T>
class Ptr32 {
public:
    constexpr Ptr32() = default;
    Ptr32(T* const ptr)
        : m_ptr((u32) reinterpret_cast<FlatPtr>(ptr))
    {
        VERIFY((reinterpret_cast<FlatPtr>(ptr) & 0xFFFFFFFFULL) == static_cast<FlatPtr>(m_ptr));
    }
    T& operator*() { return *static_cast<T*>(*this); }
    T const& operator*() const { return *static_cast<T const*>(*this); }

    T* operator->() { return *this; }
    T const* operator->() const { return *this; }

    operator T*() { return reinterpret_cast<T*>(static_cast<FlatPtr>(m_ptr)); }
    operator T const*() const { return reinterpret_cast<T const*>(static_cast<FlatPtr>(m_ptr)); }

    T& operator[](size_t index) { return static_cast<T*>(*this)[index]; }
    T const& operator[](size_t index) const { return static_cast<T const*>(*this)[index]; }

    constexpr explicit operator bool() { return m_ptr; }
    template<typename U>
    constexpr bool operator==(Ptr32<U> other) { return m_ptr == other.m_ptr; }

    constexpr Ptr32<T> operator+(u32 other) const
    {
        Ptr32<T> ptr {};
        ptr.m_ptr = m_ptr + other;
        return ptr;
    }
    constexpr Ptr32<T> operator-(u32 other) const
    {
        Ptr32<T> ptr {};
        ptr.m_ptr = m_ptr - other;
        return ptr;
    }

private:
    u32 m_ptr { 0 };
};

}

#if USING_AK_GLOBALLY
using AK::Ptr32;
#endif
