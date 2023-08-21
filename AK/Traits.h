/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BitCast.h>
#include <AK/Concepts.h>
#include <AK/Forward.h>
#include <AK/HashFunctions.h>
#include <AK/StringHash.h>

namespace AK {

template<typename T>
struct GenericTraits {
    using PeekType = T&;
    using ConstPeekType = T const&;
    static constexpr bool is_trivial() { return false; }
    static constexpr bool is_trivially_serializable() { return false; }
    static constexpr bool equals(T const& a, T const& b) { return a == b; }
    template<Concepts::HashCompatible<T> U>
    static bool equals(T const& self, U const& other) { return self == other; }
};

template<typename T>
struct Traits : public GenericTraits<T> {
};

template<Integral T>
struct Traits<T> : public GenericTraits<T> {
    static constexpr bool is_trivial() { return true; }
    static constexpr bool is_trivially_serializable() { return true; }
    static constexpr unsigned hash(T value)
    {
        if constexpr (sizeof(T) < 8)
            return int_hash(value);
        else
            return u64_hash(value);
    }
};

#ifndef KERNEL
template<FloatingPoint T>
struct Traits<T> : public GenericTraits<T> {
    static constexpr bool is_trivial() { return true; }
    static constexpr bool is_trivially_serializable() { return true; }
    static constexpr unsigned hash(T value)
    {
        if constexpr (sizeof(T) < 8)
            return int_hash(bit_cast<u32>(value));
        else
            return u64_hash(bit_cast<u64>(value));
    }
};
#endif

template<typename T>
requires(IsPointer<T> && !Detail::IsPointerOfType<char, T>) struct Traits<T> : public GenericTraits<T> {
    static unsigned hash(T p) { return ptr_hash(p); }
    static constexpr bool is_trivial() { return true; }
};

template<Enum T>
struct Traits<T> : public GenericTraits<T> {
    static unsigned hash(T value) { return Traits<UnderlyingType<T>>::hash(to_underlying(value)); }
    static constexpr bool is_trivial() { return Traits<UnderlyingType<T>>::is_trivial(); }
    static constexpr bool is_trivially_serializable() { return Traits<UnderlyingType<T>>::is_trivially_serializable(); }
};

template<typename T>
requires(Detail::IsPointerOfType<char, T>) struct Traits<T> : public GenericTraits<T> {
    static unsigned hash(T const value) { return string_hash(value, strlen(value)); }
    static constexpr bool equals(T const a, T const b) { return strcmp(a, b); }
    static constexpr bool is_trivial() { return true; }
};

}

#if USING_AK_GLOBALLY
using AK::GenericTraits;
using AK::Traits;
#endif
