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
    using ConstPeekType = T;
    static constexpr bool is_trivial() { return false; }
    static constexpr bool equals(const T& a, const T& b) { return a == b; }
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
