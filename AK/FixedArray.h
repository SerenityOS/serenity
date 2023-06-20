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
#include <initializer_list>

namespace AK {

// FixedArray is an Array with a size only known at run-time.
// It guarantees to only allocate when being constructed, and to only deallocate when being destructed.
template<typename T>
class FixedArray {
public:
    FixedArray() = default;

    static ErrorOr<FixedArray<T>> create(std::initializer_list<T> initializer)
    {
        auto array = TRY(create(initializer.size()));
        auto it = initializer.begin();
        for (size_t i = 0; i < array.size(); ++i) {
            array[i] = move(*it);
            ++it;
        }
        return array;
    }

    static ErrorOr<FixedArray<T>> create(size_t size)
    {
        if (size == 0)
            return FixedArray<T>();
        auto* new_storage = static_cast<Storage*>(kmalloc(storage_allocation_size(size)));
        if (!new_storage)
            return Error::from_errno(ENOMEM);
        new_storage->size = size;
        for (size_t i = 0; i < size; ++i)
            new (&new_storage->elements[i]) T();
        return FixedArray<T>(new_storage);
    }

    static FixedArray<T> must_create_but_fixme_should_propagate_errors(size_t size)
    {
        return MUST(create(size));
    }

    template<size_t N>
    static ErrorOr<FixedArray<T>> create(T (&&array)[N])
    {
        return create(Span(array, N));
    }

    template<typename U>
    static ErrorOr<FixedArray<T>> create(Span<U> span)
    {
        if (span.size() == 0)
            return FixedArray<T>();
        auto* new_storage = static_cast<Storage*>(kmalloc(storage_allocation_size(span.size())));
        if (!new_storage)
            return Error::from_errno(ENOMEM);
        new_storage->size = span.size();
        for (size_t i = 0; i < span.size(); ++i)
            new (&new_storage->elements[i]) T(span[i]);
        return FixedArray<T>(new_storage);
    }

    ErrorOr<FixedArray<T>> clone() const
    {
        return create(span());
    }

    static size_t storage_allocation_size(size_t size)
    {
        return sizeof(Storage) + size * sizeof(T);
    }

    // Nobody can ever use these functions, since it would be impossible to make them OOM-safe due to their signatures. We just explicitly delete them.
    FixedArray(FixedArray<T> const&) = delete;
    FixedArray<T>& operator=(FixedArray<T> const&) = delete;

    FixedArray(FixedArray<T>&& other)
        : m_storage(exchange(other.m_storage, nullptr))
    {
    }

    FixedArray<T>& operator=(FixedArray<T>&& other)
    {
        m_storage = other.m_storage;
        other.m_storage = nullptr;
        return *this;
    }

    ~FixedArray()
    {
        if (!m_storage)
            return;
        for (size_t i = 0; i < m_storage->size; ++i)
            m_storage->elements[i].~T();
        kfree_sized(m_storage, storage_allocation_size(m_storage->size));
        m_storage = nullptr;
    }

    size_t size() const { return m_storage ? m_storage->size : 0; }
    bool is_empty() const { return size() == 0; }
    T* data() { return m_storage ? m_storage->elements : nullptr; }
    T const* data() const { return m_storage ? m_storage->elements : nullptr; }

    T& at(size_t index)
    {
        VERIFY(index < m_storage->size);
        return m_storage->elements[index];
    }

    T& unchecked_at(size_t index)
    {
        return m_storage->elements[index];
    }

    T const& at(size_t index) const
    {
        VERIFY(index < m_storage->size);
        return m_storage->elements[index];
    }

    T& operator[](size_t index)
    {
        return at(index);
    }

    T const& operator[](size_t index) const
    {
        return at(index);
    }

    bool contains_slow(T const& value) const
    {
        if (!m_storage)
            return false;
        for (size_t i = 0; i < m_storage->size; ++i) {
            if (m_storage->elements[i] == value)
                return true;
        }
        return false;
    }

    void swap(FixedArray<T>& other)
    {
        AK::swap(m_storage, other.m_storage);
    }

    void fill_with(T const& value)
    {
        if (!m_storage)
            return;
        for (size_t i = 0; i < m_storage->size; ++i)
            m_storage->elements[i] = value;
    }

    using Iterator = SimpleIterator<FixedArray, T>;
    using ConstIterator = SimpleIterator<FixedArray const, T const>;

    Iterator begin() { return Iterator::begin(*this); }
    ConstIterator begin() const { return ConstIterator::begin(*this); }

    Iterator end() { return Iterator::end(*this); }
    ConstIterator end() const { return ConstIterator::end(*this); }

    Span<T> span() { return { data(), size() }; }
    ReadonlySpan<T> span() const { return { data(), size() }; }

private:
    struct Storage {
        size_t size { 0 };
        T elements[0];
    };

    FixedArray(Storage* storage)
        : m_storage(storage)
    {
    }

    Storage* m_storage { nullptr };
};

}

#if USING_AK_GLOBALLY
using AK::FixedArray;
#endif
