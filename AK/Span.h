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

#include <AK/Assertions.h>
#include <AK/Iterator.h>
#include <AK/TypedTransfer.h>
#include <AK/Types.h>

namespace AK {

namespace Detail {

template<typename T>
class Span {
public:
    ALWAYS_INLINE Span() = default;

    ALWAYS_INLINE Span(T* values, size_t size)
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
    ALWAYS_INLINE Span() = default;

    ALWAYS_INLINE Span(u8* values, size_t size)
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
    ALWAYS_INLINE Span() = default;

    ALWAYS_INLINE Span(const u8* values, size_t size)
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

    ALWAYS_INLINE Span(std::nullptr_t)
        : Span()
    {
    }

    ALWAYS_INLINE Span(const Span& other)
        : Span(other.m_values, other.m_size)
    {
    }

    ALWAYS_INLINE const T* data() const { return this->m_values; }
    ALWAYS_INLINE T* data() { return this->m_values; }

    using ConstIterator = SimpleIterator<const Span, const T>;
    using Iterator = SimpleIterator<Span, T>;

    constexpr ConstIterator begin() const { return ConstIterator::begin(*this); }
    constexpr Iterator begin() { return Iterator::begin(*this); }

    constexpr ConstIterator end() const { return ConstIterator::end(*this); }
    constexpr Iterator end() { return Iterator::end(*this); }

    ALWAYS_INLINE size_t size() const { return this->m_size; }

    ALWAYS_INLINE bool is_empty() const { return this->m_size == 0; }

    ALWAYS_INLINE Span slice(size_t start, size_t length) const
    {
        ASSERT(start + length <= size());
        return { this->m_values + start, length };
    }
    ALWAYS_INLINE Span slice(size_t start) const
    {
        ASSERT(start <= size());
        return { this->m_values + start, size() - start };
    }

    ALWAYS_INLINE Span trim(size_t length) const
    {
        return { this->m_values, min(size(), length) };
    }

    ALWAYS_INLINE T* offset(size_t start) const
    {
        ASSERT(start < this->m_size);
        return this->m_values + start;
    }

    ALWAYS_INLINE size_t copy_to(Span<typename RemoveConst<T>::Type> other) const
    {
        ASSERT(other.size() >= size());
        return TypedTransfer<typename RemoveConst<T>::Type>::copy(other.data(), data(), size());
    }

    ALWAYS_INLINE size_t copy_trimmed_to(Span<typename RemoveConst<T>::Type> other) const
    {
        const auto count = min(size(), other.size());
        return TypedTransfer<typename RemoveConst<T>::Type>::copy(other.data(), data(), count);
    }

    ALWAYS_INLINE size_t fill(const T& value)
    {
        for (size_t idx = 0; idx < size(); ++idx)
            data()[idx] = value;

        return size();
    }

    bool contains_slow(const T& value) const
    {
        for (size_t i = 0; i < size(); ++i) {
            if (at(i) == value)
                return true;
        }
        return false;
    }

    ALWAYS_INLINE const T& at(size_t index) const
    {
        ASSERT(index < this->m_size);
        return this->m_values[index];
    }
    ALWAYS_INLINE T& at(size_t index)
    {
        ASSERT(index < this->m_size);
        return this->m_values[index];
    }

    ALWAYS_INLINE T& operator[](size_t index) const
    {
        return this->m_values[index];
    }
    ALWAYS_INLINE T& operator[](size_t index)
    {
        return this->m_values[index];
    }

    ALWAYS_INLINE Span& operator=(const Span<T>& other)
    {
        this->m_size = other.m_size;
        this->m_values = other.m_values;
        return *this;
    }

    bool operator==(Span<const T> other) const
    {
        if (size() != other.size())
            return false;

        return TypedTransfer<T>::compare(data(), other.data(), size());
    }

    ALWAYS_INLINE operator Span<const T>() const
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
