/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Iterator.h>
#include <AK/Span.h>
#include <AK/kmalloc.h>

namespace AK {

template<typename T>
class FixedArray {
public:
    FixedArray() = default;
    explicit FixedArray(size_t size)
        : m_size(size)
    {
        if (m_size != 0) {
            m_elements = static_cast<T*>(kmalloc_array(m_size, sizeof(T)));
            for (size_t i = 0; i < m_size; ++i)
                new (&m_elements[i]) T();
        }
    }
    ~FixedArray()
    {
        clear();
    }

    FixedArray(FixedArray const& other)
        : m_size(other.m_size)
    {
        if (m_size != 0) {
            m_elements = static_cast<T*>(kmalloc_array(m_size, sizeof(T)));
            for (size_t i = 0; i < m_size; ++i)
                new (&m_elements[i]) T(other[i]);
        }
    }

    FixedArray& operator=(FixedArray const& other)
    {
        FixedArray array(other);
        swap(array);
        return *this;
    }

    FixedArray(FixedArray&&) = delete;
    FixedArray& operator=(FixedArray&&) = delete;

    void clear()
    {
        if (!m_elements)
            return;
        for (size_t i = 0; i < m_size; ++i)
            m_elements[i].~T();
        kfree_sized(m_elements, sizeof(T) * m_size);
        m_elements = nullptr;
        m_size = 0;
    }

    size_t size() const { return m_size; }
    T* data() { return m_elements; }
    T const* data() const { return m_elements; }

    T& operator[](size_t index)
    {
        VERIFY(index < m_size);
        return m_elements[index];
    }

    T const& operator[](size_t index) const
    {
        VERIFY(index < m_size);
        return m_elements[index];
    }

    bool contains_slow(T const& value) const
    {
        for (size_t i = 0; i < m_size; ++i) {
            if (m_elements[i] == value)
                return true;
        }
        return false;
    }

    void swap(FixedArray& other)
    {
        ::swap(m_elements, other.m_elements);
        ::swap(m_size, other.m_size);
    }

    using ConstIterator = SimpleIterator<FixedArray const, T const>;
    using Iterator = SimpleIterator<FixedArray, T>;

    ConstIterator begin() const { return ConstIterator::begin(*this); }
    Iterator begin() { return Iterator::begin(*this); }

    ConstIterator end() const { return ConstIterator::end(*this); }
    Iterator end() { return Iterator::end(*this); }

    Span<T const> span() const { return { data(), size() }; }
    Span<T> span() { return { data(), size() }; }

private:
    size_t m_size { 0 };
    T* m_elements { nullptr };
};

}

using AK::FixedArray;
