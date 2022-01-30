/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/Forward.h>
#include <AK/HashFunctions.h>
#include <AK/StringHash.h>
#include <string.h>

namespace AK {

template<typename T>
struct GenericTraits {
    using PeekType = T;
    using ConstPeekType = T;
    static constexpr bool is_trivial() { return false; }
    static constexpr bool equals(const T& a, const T& b) { return a == b; }
    template<Concepts::HashCompatible<T> U>
    static bool equals(U const& a, T const& b) { return a == b; }
};

template<typename T>
struct Traits : public GenericTraits<T> {
};

template<typename T>
requires(IsIntegral<T>) struct Traits<T> : public GenericTraits<T> {
    static constexpr bool is_trivial() { return true; }
    static constexpr unsigned hash(T value)
    {
        if constexpr (sizeof(T) < 8)
            return int_hash(value);
        else
            return u64_hash(value);
    }
};

template<typename T>
requires(IsPointer<T> && !Detail::IsPointerOfType<char, T>) struct Traits<T> : public GenericTraits<T> {
    static unsigned hash(T p) { return ptr_hash((FlatPtr)p); }
    static constexpr bool is_trivial() { return true; }
};

template<Enum T>
struct Traits<T> : public GenericTraits<T> {
    static unsigned hash(T value) { return Traits<UnderlyingType<T>>::hash(to_underlying(value)); }
    static constexpr bool is_trivial() { return Traits<UnderlyingType<T>>::is_trivial(); }
};

template<typename T>
requires(Detail::IsPointerOfType<char, T>) struct Traits<T> : public GenericTraits<T> {
    static unsigned hash(T const value) { return string_hash(value, strlen(value)); }
    static constexpr bool equals(T const a, T const b) { return strcmp(a, b); }
    static constexpr bool is_trivial() { return true; }
};

}

using AK::GenericTraits;
using AK::Traits;
