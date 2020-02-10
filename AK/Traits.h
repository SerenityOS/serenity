/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/HashFunctions.h>

namespace AK {

template<typename T>
struct GenericTraits {
    using PeekType = T;
    static constexpr bool is_trivial() { return false; }
    static bool equals(const T& a, const T& b) { return a == b; }
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
struct Traits<unsigned> : public GenericTraits<unsigned> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(unsigned u) { return int_hash(u); }
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

template<>
struct Traits<char> : public GenericTraits<char> {
    static constexpr bool is_trivial() { return true; }
    static unsigned hash(char c) { return int_hash(c); }
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
