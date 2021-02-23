/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
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

#include <AK/Assertions.h>
#include <AK/Iterator.h>
#include <AK/TypedTransfer.h>
#include <AK/Types.h>

namespace AK {

namespace Detail {

template<typename T>
class Span {
public:
    ALWAYS_INLINE constexpr Span() = default;

    ALWAYS_INLINE constexpr Span(T* values, size_t size)
        : m_values(values)
        , m_size(size)
    {
    }

    template<size_t size>
    ALWAYS_INLINE constexpr Span(T (&values)[size])
        : m_values(values)
        , m_size(size)
    {
    }

protected:
    T* m_values { nullptr };
    size_t m_size { 0 };
};

template<>
class Span<u8> {
public:
    ALWAYS_INLINE constexpr Span() = default;

    ALWAYS_INLINE constexpr Span(u8* values, size_t size)
        : m_values(values)
        , m_size(size)
    {
    }
    ALWAYS_INLINE Span(void* values, size_t size)
        : m_values(reinterpret_cast<u8*>(values))
        , m_size(size)
    {
    }

protected:
    u8* m_values { nullptr };
    size_t m_size { 0 };
};

template<>
class Span<const u8> {
public:
    ALWAYS_INLINE constexpr Span() = default;

    ALWAYS_INLINE constexpr Span(const u8* values, size_t size)
        : m_values(values)
        , m_size(size)
    {
    }
    ALWAYS_INLINE Span(const void* values, size_t size)
        : m_values(reinterpret_cast<const u8*>(values))
        , m_size(size)
    {
    }
    ALWAYS_INLINE Span(const char* values, size_t size)
        : m_values(reinterpret_cast<const u8*>(values))
        , m_size(size)
    {
    }

protected:
    const u8* m_values { nullptr };
    size_t m_size { 0 };
};

}

template<typename T>
class Span : public Detail::Span<T> {
public:
    using Detail::Span<T>::Span;

    constexpr Span() = default;

    ALWAYS_INLINE constexpr Span(const Span& other)
        : Span(other.m_values, other.m_size)
    {
    }

    ALWAYS_INLINE constexpr const T* data() const { return this->m_values; }
    ALWAYS_INLINE constexpr T* data() { return this->m_values; }

    ALWAYS_INLINE constexpr const T* offset_pointer(size_t offset) const { return this->m_values + offset; }
    ALWAYS_INLINE constexpr T* offset_pointer(size_t offset) { return this->m_values + offset; }

    using ConstIterator = SimpleIterator<const Span, const T>;
    using Iterator = SimpleIterator<Span, T>;

    constexpr ConstIterator begin() const { return ConstIterator::begin(*this); }
    constexpr Iterator begin() { return Iterator::begin(*this); }

    constexpr ConstIterator end() const { return ConstIterator::end(*this); }
    constexpr Iterator end() { return Iterator::end(*this); }

    ALWAYS_INLINE constexpr size_t size() const { return this->m_size; }
    ALWAYS_INLINE constexpr bool is_null() const { return this->m_values == nullptr; }
    ALWAYS_INLINE constexpr bool is_empty() const { return this->m_size == 0; }

    [[nodiscard]] ALWAYS_INLINE constexpr Span slice(size_t start, size_t length) const
    {
        VERIFY(start + length <= size());
        return { this->m_values + start, length };
    }
    [[nodiscard]] ALWAYS_INLINE constexpr Span slice(size_t start) const
    {
        VERIFY(start <= size());
        return { this->m_values + start, size() - start };
    }

    [[nodiscard]] ALWAYS_INLINE constexpr Span trim(size_t length) const
    {
        return { this->m_values, min(size(), length) };
    }

    ALWAYS_INLINE constexpr T* offset(size_t start) const
    {
        VERIFY(start < this->m_size);
        return this->m_values + start;
    }

    ALWAYS_INLINE constexpr void overwrite(size_t offset, const void* data, size_t data_size)
    {
        // make sure we're not told to write past the end
        VERIFY(offset + data_size <= size());
        __builtin_memcpy(this->data() + offset, data, data_size);
    }

    ALWAYS_INLINE constexpr size_t copy_to(Span<typename RemoveConst<T>::Type> other) const
    {
        VERIFY(other.size() >= size());
        return TypedTransfer<typename RemoveConst<T>::Type>::copy(other.data(), data(), size());
    }

    ALWAYS_INLINE constexpr size_t copy_trimmed_to(Span<typename RemoveConst<T>::Type> other) const
    {
        const auto count = min(size(), other.size());
        return TypedTransfer<typename RemoveConst<T>::Type>::copy(other.data(), data(), count);
    }

    ALWAYS_INLINE constexpr size_t fill(const T& value)
    {
        for (size_t idx = 0; idx < size(); ++idx)
            data()[idx] = value;

        return size();
    }

    bool constexpr contains_slow(const T& value) const
    {
        for (size_t i = 0; i < size(); ++i) {
            if (at(i) == value)
                return true;
        }
        return false;
    }

    ALWAYS_INLINE constexpr const T& at(size_t index) const
    {
        VERIFY(index < this->m_size);
        return this->m_values[index];
    }
    ALWAYS_INLINE constexpr T& at(size_t index)
    {
        VERIFY(index < this->m_size);
        return this->m_values[index];
    }

    ALWAYS_INLINE constexpr T& operator[](size_t index) const
    {
        return at(index);
    }
    ALWAYS_INLINE constexpr T& operator[](size_t index)
    {
        return at(index);
    }

    ALWAYS_INLINE constexpr Span& operator=(const Span<T>& other)
    {
        this->m_size = other.m_size;
        this->m_values = other.m_values;
        return *this;
    }

    constexpr bool operator==(Span<const T> other) const
    {
        if (size() != other.size())
            return false;

        return TypedTransfer<T>::compare(data(), other.data(), size());
    }

    ALWAYS_INLINE constexpr operator Span<const T>() const
    {
        return { data(), size() };
    }
};

using ReadonlyBytes = Span<const u8>;
using Bytes = Span<u8>;

}

using AK::Bytes;
using AK::ReadonlyBytes;
using AK::Span;
