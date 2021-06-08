/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/Find.h>
#include <AK/Forward.h>
#include <AK/Iterator.h>
#include <AK/Optional.h>
#include <AK/Span.h>
#include <AK/StdLibExtras.h>
#include <AK/Traits.h>
#include <AK/TypedTransfer.h>
#include <AK/kmalloc.h>

// NOTE: We can't include <initializer_list> during the toolchain bootstrap,
//       since it's part of libstdc++, and libstdc++ depends on LibC.
//       For this reason, we don't support Vector(initializer_list) in LibC.
#ifndef SERENITY_LIBC_BUILD
#    include <initializer_list>
#endif

#ifndef __serenity__
#    include <new>
#endif

namespace AK {

template<typename T, size_t inline_capacity>
class Vector {
public:
    using ValueType = T;

    Vector()
        : m_capacity(inline_capacity)
    {
    }

#ifndef SERENITY_LIBC_BUILD
    Vector(std::initializer_list<T> list)
    {
        ensure_capacity(list.size());
        for (auto& item : list)
            unchecked_append(item);
    }
#endif

    Vector(Vector&& other)
        : m_size(other.m_size)
        , m_capacity(other.m_capacity)
        , m_outline_buffer(other.m_outline_buffer)
    {
        if constexpr (inline_capacity > 0) {
            if (!m_outline_buffer) {
                for (size_t i = 0; i < m_size; ++i) {
                    new (&inline_buffer()[i]) T(move(other.inline_buffer()[i]));
                    other.inline_buffer()[i].~T();
                }
            }
        }
        other.m_outline_buffer = nullptr;
        other.m_size = 0;
        other.reset_capacity();
    }

    Vector(Vector const& other)
    {
        ensure_capacity(other.size());
        TypedTransfer<T>::copy(data(), other.data(), other.size());
        m_size = other.size();
    }

    template<size_t other_inline_capacity>
    Vector(Vector<T, other_inline_capacity> const& other)
    {
        ensure_capacity(other.size());
        TypedTransfer<T>::copy(data(), other.data(), other.size());
        m_size = other.size();
    }

    ~Vector()
    {
        clear();
    }

    Span<T> span() { return { data(), size() }; }
    Span<T const> span() const { return { data(), size() }; }

    operator Span<T>() { return span(); }
    operator Span<T const>() const { return span(); }

    bool is_empty() const { return size() == 0; }
    ALWAYS_INLINE size_t size() const { return m_size; }
    size_t capacity() const { return m_capacity; }

    T* data()
    {
        if constexpr (inline_capacity > 0)
            return m_outline_buffer ? m_outline_buffer : inline_buffer();
        return m_outline_buffer;
    }

    T const* data() const
    {
        if constexpr (inline_capacity > 0)
            return m_outline_buffer ? m_outline_buffer : inline_buffer();
        return m_outline_buffer;
    }

    ALWAYS_INLINE T const& at(size_t i) const
    {
        VERIFY(i < m_size);
        return data()[i];
    }

    ALWAYS_INLINE T& at(size_t i)
    {
        VERIFY(i < m_size);
        return data()[i];
    }

    ALWAYS_INLINE T const& operator[](size_t i) const { return at(i); }
    ALWAYS_INLINE T& operator[](size_t i) { return at(i); }

    T const& first() const { return at(0); }
    T& first() { return at(0); }

    T const& last() const { return at(size() - 1); }
    T& last() { return at(size() - 1); }

    template<typename Callback>
    Optional<T> first_matching(Callback callback)
    {
        for (size_t i = 0; i < size(); ++i) {
            if (callback(at(i))) {
                return at(i);
            }
        }
        return {};
    }

    template<typename Callback>
    Optional<T> last_matching(Callback callback)
    {
        for (ssize_t i = size() - 1; i >= 0; --i) {
            if (callback(at(i))) {
                return at(i);
            }
        }
        return {};
    }

    template<typename V>
    bool operator==(V const& other) const
    {
        if (m_size != other.size())
            return false;
        return TypedTransfer<T>::compare(data(), other.data(), size());
    }

    bool contains_slow(T const& value) const
    {
        for (size_t i = 0; i < size(); ++i) {
            if (Traits<T>::equals(at(i), value))
                return true;
        }
        return false;
    }

    bool contains_in_range(T const& value, size_t const start, size_t const end) const
    {
        VERIFY(start <= end);
        VERIFY(end < size());
        for (size_t i = start; i <= end; ++i) {
            if (Traits<T>::equals(at(i), value))
                return true;
        }
        return false;
    }

    template<typename U = T>
    void insert(size_t index, U&& value)
    {
        auto did_allocate = try_insert<U>(index, forward<U>(value));
        VERIFY(did_allocate);
    }

    template<typename C, typename U = T>
    void insert_before_matching(U&& value, C callback, size_t first_index = 0, size_t* inserted_index = nullptr)
    {
        auto did_allocate = try_insert_before_matching(forward<U>(value), callback, first_index, inserted_index);
        VERIFY(did_allocate);
    }

    void append(Vector&& other)
    {
        auto did_allocate = try_append(move(other));
        VERIFY(did_allocate);
    }

    void append(Vector const& other)
    {
        auto did_allocate = try_append(other);
        VERIFY(did_allocate);
    }

    ALWAYS_INLINE void append(T&& value)
    {
        auto did_allocate = try_append(move(value));
        VERIFY(did_allocate);
    }

    ALWAYS_INLINE void append(T const& value)
    {
        auto did_allocate = try_append(T(value));
        VERIFY(did_allocate);
    }

    void append(T const* values, size_t count)
    {
        auto did_allocate = try_append(values, count);
        VERIFY(did_allocate);
    }

    template<typename U = T>
    ALWAYS_INLINE void unchecked_append(U&& value)
    {
        VERIFY((size() + 1) <= capacity());
        new (slot(m_size)) T(forward<U>(value));
        ++m_size;
    }

    template<class... Args>
    void empend(Args&&... args)
    {
        auto did_allocate = try_empend(forward<Args>(args)...);
        VERIFY(did_allocate);
    }

    template<typename U = T>
    void prepend(U&& value)
    {
        auto did_allocate = try_insert(0, forward<U>(value));
        VERIFY(did_allocate);
    }

    void prepend(Vector&& other)
    {
        auto did_allocate = try_prepend(move(other));
        VERIFY(did_allocate);
    }

    void prepend(T const* values, size_t count)
    {
        auto did_allocate = try_prepend(values, count);
        VERIFY(did_allocate);
    }

    // FIXME: What about assigning from a vector with lower inline capacity?
    Vector& operator=(Vector&& other)
    {
        if (this != &other) {
            clear();
            m_size = other.m_size;
            m_capacity = other.m_capacity;
            m_outline_buffer = other.m_outline_buffer;
            if constexpr (inline_capacity > 0) {
                if (!m_outline_buffer) {
                    for (size_t i = 0; i < m_size; ++i) {
                        new (&inline_buffer()[i]) T(move(other.inline_buffer()[i]));
                        other.inline_buffer()[i].~T();
                    }
                }
            }
            other.m_outline_buffer = nullptr;
            other.m_size = 0;
            other.reset_capacity();
        }
        return *this;
    }

    Vector& operator=(Vector const& other)
    {
        if (this != &other) {
            clear();
            ensure_capacity(other.size());
            TypedTransfer<T>::copy(data(), other.data(), other.size());
            m_size = other.size();
        }
        return *this;
    }

    template<size_t other_inline_capacity>
    Vector& operator=(Vector<T, other_inline_capacity> const& other)
    {
        clear();
        ensure_capacity(other.size());
        TypedTransfer<T>::copy(data(), other.data(), other.size());
        m_size = other.size();
        return *this;
    }

    void clear()
    {
        clear_with_capacity();
        if (m_outline_buffer) {
            kfree(m_outline_buffer);
            m_outline_buffer = nullptr;
        }
        reset_capacity();
    }

    void clear_with_capacity()
    {
        for (size_t i = 0; i < m_size; ++i)
            data()[i].~T();
        m_size = 0;
    }

    void remove(size_t index)
    {
        VERIFY(index < m_size);

        if constexpr (Traits<T>::is_trivial()) {
            TypedTransfer<T>::copy(slot(index), slot(index + 1), m_size - index - 1);
        } else {
            at(index).~T();
            for (size_t i = index + 1; i < m_size; ++i) {
                new (slot(i - 1)) T(move(at(i)));
                at(i).~T();
            }
        }

        --m_size;
    }

    void remove(size_t index, size_t count)
    {
        if (count == 0)
            return;
        VERIFY(index + count > index);
        VERIFY(index + count <= m_size);

        if constexpr (Traits<T>::is_trivial()) {
            TypedTransfer<T>::copy(slot(index), slot(index + count), m_size - index - count);
        } else {
            for (size_t i = index; i < index + count; i++)
                at(i).~T();
            for (size_t i = index + count; i < m_size; ++i) {
                new (slot(i - count)) T(move(at(i)));
                at(i).~T();
            }
        }

        m_size -= count;
    }

    template<typename Callback>
    bool remove_first_matching(Callback callback)
    {
        for (size_t i = 0; i < size(); ++i) {
            if (callback(at(i))) {
                remove(i);
                return true;
            }
        }
        return false;
    }

    template<typename Callback>
    void remove_all_matching(Callback callback)
    {
        for (size_t i = 0; i < size();) {
            if (callback(at(i))) {
                remove(i);
            } else {
                ++i;
            }
        }
    }

    T take_last()
    {
        VERIFY(!is_empty());
        T value = move(last());
        last().~T();
        --m_size;
        return value;
    }

    T take_first()
    {
        VERIFY(!is_empty());
        T value = move(first());
        remove(0);
        return value;
    }

    T take(size_t index)
    {
        T value = move(at(index));
        remove(index);
        return value;
    }

    T unstable_take(size_t index)
    {
        VERIFY(index < m_size);
        swap(at(index), at(m_size - 1));
        return take_last();
    }

    template<typename U = T>
    [[nodiscard]] bool try_insert(size_t index, U&& value)
    {
        if (index > size())
            return false;
        if (index == size())
            return try_append(forward<U>(value));
        if (!try_grow_capacity(size() + 1))
            return false;
        ++m_size;
        if constexpr (Traits<T>::is_trivial()) {
            TypedTransfer<T>::move(slot(index + 1), slot(index), m_size - index - 1);
        } else {
            for (size_t i = size() - 1; i > index; --i) {
                new (slot(i)) T(move(at(i - 1)));
                at(i - 1).~T();
            }
        }
        new (slot(index)) T(forward<U>(value));
        return true;
    }

    template<typename C, typename U = T>
    [[nodiscard]] bool try_insert_before_matching(U&& value, C callback, size_t first_index = 0, size_t* inserted_index = nullptr)
    {
        for (size_t i = first_index; i < size(); ++i) {
            if (callback(at(i))) {
                if (!try_insert(i, forward<U>(value)))
                    return false;
                if (inserted_index)
                    *inserted_index = i;
                return true;
            }
        }
        if (!try_append(forward<U>(value)))
            return false;
        if (inserted_index)
            *inserted_index = size() - 1;
        return true;
    }

    [[nodiscard]] bool try_append(Vector&& other)
    {
        if (is_empty()) {
            *this = move(other);
            return true;
        }
        auto other_size = other.size();
        Vector tmp = move(other);
        if (!try_grow_capacity(size() + other_size))
            return false;
        TypedTransfer<T>::move(data() + m_size, tmp.data(), other_size);
        m_size += other_size;
        return true;
    }

    [[nodiscard]] bool try_append(Vector const& other)
    {
        if (!try_grow_capacity(size() + other.size()))
            return false;
        TypedTransfer<T>::copy(data() + m_size, other.data(), other.size());
        m_size += other.m_size;
        return true;
    }

    [[nodiscard]] ALWAYS_INLINE bool try_append(T&& value)
    {
        if (!try_grow_capacity(size() + 1))
            return false;
        new (slot(m_size)) T(move(value));
        ++m_size;
        return true;
    }

    [[nodiscard]] ALWAYS_INLINE bool try_append(T const& value)
    {
        return try_append(T(value));
    }

    [[nodiscard]] bool try_append(T const* values, size_t count)
    {
        if (!count)
            return true;
        if (!try_grow_capacity(size() + count))
            return false;
        TypedTransfer<T>::copy(slot(m_size), values, count);
        m_size += count;
        return true;
    }

    template<class... Args>
    [[nodiscard]] bool try_empend(Args&&... args)
    {
        if (!try_grow_capacity(m_size + 1))
            return false;
        new (slot(m_size)) T { forward<Args>(args)... };
        ++m_size;
        return true;
    }

    template<typename U = T>
    [[nodiscard]] bool try_prepend(U&& value)
    {
        return try_insert(0, forward<U>(value));
    }

    [[nodiscard]] bool try_prepend(Vector&& other)
    {
        if (other.is_empty())
            return true;

        if (is_empty()) {
            *this = move(other);
            return true;
        }

        auto other_size = other.size();
        if (!try_grow_capacity(size() + other_size))
            return false;

        for (size_t i = size() + other_size - 1; i >= other.size(); --i) {
            new (slot(i)) T(move(at(i - other_size)));
            at(i - other_size).~T();
        }

        Vector tmp = move(other);
        TypedTransfer<T>::move(slot(0), tmp.data(), tmp.size());
        m_size += other_size;
        return true;
    }

    [[nodiscard]] bool try_prepend(T const* values, size_t count)
    {
        if (!count)
            return true;
        if (!try_grow_capacity(size() + count))
            return false;
        TypedTransfer<T>::move(slot(count), slot(0), m_size);
        TypedTransfer<T>::copy(slot(0), values, count);
        m_size += count;
        return true;
    }

    [[nodiscard]] bool try_grow_capacity(size_t needed_capacity)
    {
        if (m_capacity >= needed_capacity)
            return true;
        return try_ensure_capacity(padded_capacity(needed_capacity));
    }

    [[nodiscard]] bool try_ensure_capacity(size_t needed_capacity)
    {
        if (m_capacity >= needed_capacity)
            return true;
        size_t new_capacity = kmalloc_good_size(needed_capacity * sizeof(T)) / sizeof(T);
        auto* new_buffer = (T*)kmalloc(new_capacity * sizeof(T));
        if (new_buffer == nullptr)
            return false;

        if constexpr (Traits<T>::is_trivial()) {
            TypedTransfer<T>::copy(new_buffer, data(), m_size);
        } else {
            for (size_t i = 0; i < m_size; ++i) {
                new (&new_buffer[i]) T(move(at(i)));
                at(i).~T();
            }
        }
        if (m_outline_buffer)
            kfree(m_outline_buffer);
        m_outline_buffer = new_buffer;
        m_capacity = new_capacity;
        return true;
    }

    [[nodiscard]] bool try_resize(size_t new_size, bool keep_capacity = false)
    {
        if (new_size <= size()) {
            shrink(new_size, keep_capacity);
            return true;
        }

        if (!try_ensure_capacity(new_size))
            return false;

        for (size_t i = size(); i < new_size; ++i)
            new (slot(i)) T {};
        m_size = new_size;
        return true;
    }

    [[nodiscard]] bool try_resize_and_keep_capacity(size_t new_size)
    {
        return try_resize(new_size, true);
    }

    void grow_capacity(size_t needed_capacity)
    {
        auto did_allocate = try_grow_capacity(needed_capacity);
        VERIFY(did_allocate);
    }

    void ensure_capacity(size_t needed_capacity)
    {
        auto did_allocate = try_ensure_capacity(needed_capacity);
        VERIFY(did_allocate);
    }

    void shrink(size_t new_size, bool keep_capacity = false)
    {
        VERIFY(new_size <= size());
        if (new_size == size())
            return;

        if (!new_size) {
            if (keep_capacity)
                clear_with_capacity();
            else
                clear();
            return;
        }

        for (size_t i = new_size; i < size(); ++i)
            at(i).~T();
        m_size = new_size;
    }

    void resize(size_t new_size, bool keep_capacity = false)
    {
        auto did_allocate = try_resize(new_size, keep_capacity);
        VERIFY(did_allocate);
    }

    void resize_and_keep_capacity(size_t new_size)
    {
        auto did_allocate = try_resize_and_keep_capacity(new_size);
        VERIFY(did_allocate);
    }

    using ConstIterator = SimpleIterator<Vector const, T const>;
    using Iterator = SimpleIterator<Vector, T>;

    ConstIterator begin() const { return ConstIterator::begin(*this); }
    Iterator begin() { return Iterator::begin(*this); }

    ConstIterator end() const { return ConstIterator::end(*this); }
    Iterator end() { return Iterator::end(*this); }

    template<typename TUnaryPredicate>
    ConstIterator find_if(TUnaryPredicate&& finder) const
    {
        return AK::find_if(begin(), end(), forward<TUnaryPredicate>(finder));
    }

    template<typename TUnaryPredicate>
    Iterator find_if(TUnaryPredicate&& finder)
    {
        return AK::find_if(begin(), end(), forward<TUnaryPredicate>(finder));
    }

    ConstIterator find(T const& value) const
    {
        return AK::find(begin(), end(), value);
    }

    Iterator find(T const& value)
    {
        return AK::find(begin(), end(), value);
    }

    Optional<size_t> find_first_index(T const& value) const
    {
        if (auto const index = AK::find_index(begin(), end(), value);
            index < size()) {
            return index;
        }
        return {};
    }

private:
    void reset_capacity()
    {
        m_capacity = inline_capacity;
    }

    static size_t padded_capacity(size_t capacity)
    {
        return max(static_cast<size_t>(4), capacity + (capacity / 4) + 4);
    }

    T* slot(size_t i) { return &data()[i]; }
    T const* slot(size_t i) const { return &data()[i]; }

    T* inline_buffer()
    {
        static_assert(inline_capacity > 0);
        return reinterpret_cast<T*>(m_inline_buffer_storage);
    }
    T const* inline_buffer() const
    {
        static_assert(inline_capacity > 0);
        return reinterpret_cast<T const*>(m_inline_buffer_storage);
    }

    size_t m_size { 0 };
    size_t m_capacity { 0 };

    alignas(T) unsigned char m_inline_buffer_storage[sizeof(T) * inline_capacity];
    T* m_outline_buffer { nullptr };
};

}

using AK::Vector;
