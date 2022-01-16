/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Iterator.h>
#include <AK/Span.h>
#include <AK/kmalloc.h>

namespace AK {

// FixedArray is an Array with a size only known at run-time.
// It guarantees to only allocate when being constructed, and to only deallocate when being destructed.
template<typename T>
class FixedArray {
public:
    FixedArray() = default;

    static ErrorOr<FixedArray<T>> try_create(size_t size)
    {
        if (size == 0)
            return FixedArray<T>();
        T* elements = static_cast<T*>(kmalloc_array(size, sizeof(T)));
        if (!elements)
            return Error::from_errno(ENOMEM);
        for (size_t i = 0; i < size; ++i)
            new (&elements[i]) T();
        return FixedArray<T>(size, elements);
    }

    static FixedArray<T> must_create_but_fixme_should_propagate_errors(size_t size)
    {
        return MUST(try_create(size));
    }

    // NOTE:
    // Even though it may look like there will be a template instantiation of this function for every size,
    // the compiler will inline this anyway and therefore not generate any duplicate code.

    template<size_t N>
    static ErrorOr<FixedArray<T>> try_create(T(&&array)[N])
    {
        if (N == 0)
            return FixedArray<T>();
        T* elements = static_cast<T*>(kmalloc_array(N, sizeof(T)));
        if (!elements)
            return Error::from_errno(ENOMEM);
        for (size_t i = 0; i < N; ++i)
            new (&elements[i]) T(move(array[i]));
        return FixedArray<T>(N, elements);
    }

    template<typename U>
    static ErrorOr<FixedArray<T>> try_create(Span<U> span)
    {
        if (span.size() == 0)
            return FixedArray<T>();
        T* elements = static_cast<T*>(kmalloc_array(span.size(), sizeof(T)));
        if (!elements)
            return Error::from_errno(ENOMEM);
        for (size_t i = 0; i < span.size(); ++i)
            new (&elements[i]) T(span[i]);
        return FixedArray<T>(span.size(), elements);
    }

    ErrorOr<FixedArray<T>> try_clone() const
    {
        if (m_size == 0)
            return FixedArray<T>();
        T* elements = static_cast<T*>(kmalloc_array(m_size, sizeof(T)));
        if (!elements)
            return Error::from_errno(ENOMEM);
        for (size_t i = 0; i < m_size; ++i)
            new (&elements[i]) T(m_elements[i]);
        return FixedArray<T>(m_size, elements);
    }

    FixedArray<T> must_clone_but_fixme_should_propagate_errors() const
    {
        return MUST(try_clone());
    }

    // Nobody can ever use these functions, since it would be impossible to make them OOM-safe due to their signatures. We just explicitly delete them.
    FixedArray(FixedArray<T> const&) = delete;
    FixedArray<T>& operator=(FixedArray<T> const&) = delete;

    FixedArray(FixedArray<T>&& other)
        : m_size(other.m_size)
        , m_elements(other.m_elements)
    {
        other.m_size = 0;
        other.m_elements = nullptr;
    }
    // This function would violate the contract, as it would need to deallocate this FixedArray. As it also has no use case, we delete it.
    FixedArray<T>& operator=(FixedArray<T>&&) = delete;

    ~FixedArray()
    {
        if (!m_elements)
            return;
        for (size_t i = 0; i < m_size; ++i)
            m_elements[i].~T();
        kfree_sized(m_elements, sizeof(T) * m_size);
        // NOTE: should prevent use-after-free early
        m_size = 0;
        m_elements = nullptr;
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

    void swap(FixedArray<T>& other)
    {
        ::swap(m_size, other.m_size);
        ::swap(m_elements, other.m_elements);
    }

    using Iterator = SimpleIterator<FixedArray, T>;
    using ConstIterator = SimpleIterator<FixedArray const, T const>;

    Iterator begin() { return Iterator::begin(*this); }
    ConstIterator begin() const { return ConstIterator::begin(*this); }

    Iterator end() { return Iterator::end(*this); }
    ConstIterator end() const { return ConstIterator::end(*this); }

    Span<T> span() { return { data(), size() }; }
    Span<T const> span() const { return { data(), size() }; }

private:
    FixedArray(size_t size, T* elements)
        : m_size(size)
        , m_elements(elements)
    {
    }

    size_t m_size { 0 };
    T* m_elements { nullptr };
};

}

using AK::FixedArray;
