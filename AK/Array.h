/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/Iterator.h>
#include <AK/Span.h>

namespace AK {

template<typename T, size_t Size>
struct Array {
    constexpr const T* data() const { return __data; }
    constexpr T* data() { return __data; }

    constexpr size_t size() const { return Size; }

    constexpr Span<const T> span() const { return { __data, Size }; }
    constexpr Span<T> span() { return { __data, Size }; }

    constexpr const T& at(size_t index) const
    {
        VERIFY(index < size());
        return (*this)[index];
    }
    constexpr T& at(size_t index)
    {
        VERIFY(index < size());
        return (*this)[index];
    }

    constexpr const T& front() const { return at(0); }
    constexpr T& front() { return at(0); }

    constexpr const T& back() const { return at(max(1, size()) - 1); }
    constexpr T& back() { return at(max(1, size()) - 1); }

    constexpr bool is_empty() const { return size() == 0; }

    constexpr const T& operator[](size_t index) const { return __data[index]; }
    constexpr T& operator[](size_t index) { return __data[index]; }

    template<typename T2, size_t Size2>
    constexpr bool operator==(const Array<T2, Size2>& other) const { return span() == other.span(); }

    using ConstIterator = SimpleIterator<const Array, const T>;
    using Iterator = SimpleIterator<Array, T>;

    constexpr ConstIterator begin() const { return ConstIterator::begin(*this); }
    constexpr Iterator begin() { return Iterator::begin(*this); }

    constexpr ConstIterator end() const { return ConstIterator::end(*this); }
    constexpr Iterator end() { return Iterator::end(*this); }

    constexpr operator Span<const T>() const { return span(); }
    constexpr operator Span<T>() { return span(); }

    constexpr size_t fill(const T& value)
    {
        for (size_t idx = 0; idx < Size; ++idx)
            __data[idx] = value;

        return Size;
    }

    T __data[Size];
};

template<typename T, typename... Types>
Array(T, Types...) -> Array<T, sizeof...(Types) + 1>;

namespace Detail {
template<typename T, unsigned long... Is>
constexpr auto integer_sequence_generate_array([[maybe_unused]] const T offset, IntegerSequence<T, Is...>) -> Array<T, sizeof...(Is)>
{
    return { { (offset + Is)... } };
}
}

template<typename T, T N>
constexpr static auto iota_array(const T offset = {})
{
    static_assert(N >= T {}, "Negative sizes not allowed in iota_array()");
    return Detail::integer_sequence_generate_array<T>(offset, MakeIntegerSequence<T, N>());
}

}

using AK::Array;
using AK::iota_array;
