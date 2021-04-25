/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
        return __data[index];
    }
    constexpr T& at(size_t index)
    {
        VERIFY(index < size());
        return __data[index];
    }

    constexpr const T& front() const { return at(0); }
    constexpr T& front() { return at(0); }

    constexpr const T& back() const { return at(max(1, size()) - 1); }
    constexpr T& back() { return at(max(1, size()) - 1); }

    constexpr bool is_empty() const { return size() == 0; }

    constexpr const T& operator[](size_t index) const { return at(index); }
    constexpr T& operator[](size_t index) { return at(index); }

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

    constexpr T max() requires(requires(T x, T y) { x < y; })
    {
        static_assert(Size > 0, "No values to max() over");

        T value = __data[0];
        for (size_t i = 1; i < Size; ++i)
            value = AK::max(__data[i], value);
        return value;
    }

    constexpr T min() requires(requires(T x, T y) { x > y; })
    {
        static_assert(Size > 0, "No values to min() over");

        T value = __data[0];
        for (size_t i = 1; i < Size; ++i)
            value = AK::min(__data[i], value);
        return value;
    }

    T __data[Size];
};

template<typename T, typename... Types>
Array(T, Types...) -> Array<T, sizeof...(Types) + 1>;

namespace Detail {
template<typename T, size_t... Is>
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
using AK::Detail::integer_sequence_generate_array;
