/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/HashFunctions.h>

namespace AK {

template<typename T>
struct GenericTraits {
    using PeekType = T;
    static constexpr bool is_trivial() { return false; }
    static constexpr bool equals(const T& a, const T& b) { return a == b; }
};

template<typename T>
struct Traits : public GenericTraits<T> {
};

template<>
struct Traits<int> : public GenericTraits<int> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(int i) { return int_hash(i); }
};

template<>
struct Traits<char> : public GenericTraits<char> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(char c) { return int_hash(c); }
};

template<>
struct Traits<i16> : public GenericTraits<i16> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(i16 i) { return int_hash(i); }
};

template<>
struct Traits<i64> : public GenericTraits<i64> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(i64 i) { return u64_hash(i); }
};

template<>
struct Traits<unsigned> : public GenericTraits<unsigned> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(unsigned u) { return int_hash(u); }
};

template<>
struct Traits<u8> : public GenericTraits<u8> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(u8 u) { return int_hash(u); }
};

template<>
struct Traits<u16> : public GenericTraits<u16> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(u16 u) { return int_hash(u); }
};

template<>
struct Traits<u64> : public GenericTraits<u64> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(u64 u) { return u64_hash(u); }
};

template<typename T>
struct Traits<T*> : public GenericTraits<T*> {
    static unsigned hash(const T* p)
    {
        return int_hash((unsigned)(__PTRDIFF_TYPE__)p);
    }
    static constexpr bool is_trivial() { return true; }
    static bool equals(const T* a, const T* b) { return a == b; }
};

}

using AK::GenericTraits;
using AK::Traits;
