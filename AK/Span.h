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
#include <AK/Checked.h>
#include <AK/Types.h>

namespace AK {

template<typename T>
class Span {
public:
    using Iterator = T*;
    using ConstIterator = const T*;

    static_assert(!IsPointer<T>::value);

    ALWAYS_INLINE Span() = default;
    ALWAYS_INLINE Span(T* values, size_t size)
        : m_values(values)
        , m_size(size)
    {
        ASSERT(!Checked<uintptr_t>::addition_would_overflow((uintptr_t)values, size * sizeof(T)));
    }
    ALWAYS_INLINE Span(const Span& other)
        : m_values(other.m_values)
        , m_size(other.m_size)
    {
    }

    ALWAYS_INLINE const T* data() const { return m_values; }
    ALWAYS_INLINE T* data() { return m_values; }

    ALWAYS_INLINE ConstIterator begin() const
    {
        return m_values;
    }
    ALWAYS_INLINE ConstIterator end() const
    {
        return begin() + m_size;
    }

    ALWAYS_INLINE Iterator begin()
    {
        return m_values;
    }
    ALWAYS_INLINE Iterator end()
    {
        return begin() + m_size;
    }

    ALWAYS_INLINE size_t size() const { return m_size; }

    ALWAYS_INLINE bool is_empty() const { return m_size == 0; }

    ALWAYS_INLINE Span<T> subspan(size_t start, size_t size) const
    {
        ASSERT(start + size <= m_size);
        return { m_values + start, size };
    }

    ALWAYS_INLINE const T& at(size_t index) const
    {
        ASSERT(index < m_size);
        return m_values[index];
    }
    ALWAYS_INLINE T& at(size_t index)
    {
        ASSERT(index < m_size);
        return m_values[index];
    }

    ALWAYS_INLINE T& operator[](size_t index) const
    {
        return m_values[index];
    }
    ALWAYS_INLINE T& operator[](size_t index)
    {
        return m_values[index];
    }

    ALWAYS_INLINE T& operator=(const T& other)
    {
        m_size = other.m_size;
        m_values = other.m_values;
    }

    ALWAYS_INLINE operator Span<const T>() const
    {
        return { data(), size() };
    }

protected:
    T* m_values { nullptr };
    size_t m_size { 0 };
};

using ReadonlyBytes = Span<const u8>;
using Bytes = Span<u8>;

}

using AK::Bytes;
using AK::ReadonlyBytes;
using AK::Span;
