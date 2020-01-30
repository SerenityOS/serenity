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

#include <AK/Vector.h>

namespace AK {

template<typename T>
class FixedArray {
public:
    FixedArray() {}
    explicit FixedArray(size_t size)
        : m_size(size)
    {
        m_elements = (T*)kmalloc(sizeof(T) * m_size);
        for (size_t i = 0; i < m_size; ++i)
            new (&m_elements[i]) T();
    }
    ~FixedArray()
    {
        clear();
    }

    FixedArray(const FixedArray& other)
        : m_size(other.m_size)
    {
        m_elements = (T*)kmalloc(sizeof(T) * m_size);
        for (size_t i = 0; i < m_size; ++i)
            new (&m_elements[i]) T(other[i]);
    }

    FixedArray& operator=(const FixedArray&) = delete;
    FixedArray(FixedArray&&) = delete;
    FixedArray& operator=(FixedArray&&) = delete;

    void clear()
    {
        if (!m_elements)
            return;
        for (size_t i = 0; i < m_size; ++i)
            m_elements[i].~T();
        kfree(m_elements);
        m_elements = nullptr;
        m_size = 0;
    }

    size_t size() const { return m_size; }

    T* data()
    {
        return m_elements;
    }
    const T* data() const
    {
        return m_elements;
    }

    T& operator[](size_t index)
    {
        ASSERT(index < m_size);
        return m_elements[index];
    }

    const T& operator[](size_t index) const
    {
        ASSERT(index < m_size);
        return m_elements[index];
    }

    void resize(size_t new_size)
    {
        if (new_size == m_size)
            return;
        auto* new_elements = (T*)kmalloc(new_size * sizeof(T));
        for (size_t i = 0; i < min(new_size, m_size); ++i)
            new (&new_elements[i]) T(move(m_elements[i]));
        for (size_t i = min(new_size, m_size); i < new_size; ++i)
            new (&new_elements[i]) T();
        for (size_t i = 0; i < m_size; ++i)
            m_elements[i].~T();
        if (m_elements)
            kfree(m_elements);
        m_elements = new_elements;
        m_size = new_size;
    }

    using Iterator = VectorIterator<FixedArray, T>;
    Iterator begin() { return Iterator(*this, 0); }
    Iterator end() { return Iterator(*this, size()); }

    using ConstIterator = VectorIterator<const FixedArray, const T>;
    ConstIterator begin() const { return ConstIterator(*this, 0); }
    ConstIterator end() const { return ConstIterator(*this, size()); }

private:
    size_t m_size { 0 };
    T* m_elements { nullptr };
};

}

using AK::FixedArray;
