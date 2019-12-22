#pragma once

#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>
#include <AK/Traits.h>
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

template<typename T, int inline_capacity>
class Vector;

template<typename VectorType, typename ElementType>
class VectorIterator {
public:
    bool operator!=(const VectorIterator& other) const { return m_index != other.m_index; }
    bool operator==(const VectorIterator& other) const { return m_index == other.m_index; }
    bool operator<(const VectorIterator& other) const { return m_index < other.m_index; }
    bool operator>(const VectorIterator& other) const { return m_index > other.m_index; }
    bool operator>=(const VectorIterator& other) const { return m_index >= other.m_index; }
    VectorIterator& operator++()
    {
        ++m_index;
        return *this;
    }
    VectorIterator& operator--()
    {
        --m_index;
        return *this;
    }
    VectorIterator operator-(int value) { return { m_vector, m_index - value }; }
    VectorIterator operator+(int value) { return { m_vector, m_index + value }; }
    VectorIterator& operator=(const VectorIterator& other)
    {
        m_index = other.m_index;
        return *this;
    }
    ElementType& operator*() { return m_vector[m_index]; }
    int operator-(const VectorIterator& other) { return m_index - other.m_index; }

    bool is_end() const { return m_index == m_vector.size(); }
    int index() const { return m_index; }

private:
    friend VectorType;
    VectorIterator(VectorType& vector, int index)
        : m_vector(vector)
        , m_index(index)
    {
    }
    VectorType& m_vector;
    int m_index { 0 };
};

template<typename T>
class TypedTransfer {
public:
    static void move(T* destination, T* source, size_t count)
    {
        if constexpr (Traits<T>::is_trivial()) {
            memmove(destination, source, count * sizeof(T));
            return;
        }
        for (size_t i = 0; i < count; ++i)
            new (&destination[i]) T(AK::move(source[i]));
    }

    static void copy(T* destination, const T* source, size_t count)
    {
        if constexpr (Traits<T>::is_trivial()) {
            memmove(destination, source, count * sizeof(T));
            return;
        }
        for (size_t i = 0; i < count; ++i)
            new (&destination[i]) T(source[i]);
    }

    static bool compare(const T* a, const T* b, size_t count)
    {
        if constexpr (Traits<T>::is_trivial())
            return !memcmp(a, b, count * sizeof(T));

        for (size_t i = 0; i < count; ++i) {
            if (a[i] != b[i])
                return false;
        }
        return true;
    }
};

template<typename T, int inline_capacity = 0>
class Vector {
public:
    Vector()
        : m_capacity(inline_capacity)
    {
    }

    ~Vector()
    {
        clear();
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
                for (int i = 0; i < m_size; ++i) {
                    new (&inline_buffer()[i]) T(move(other.inline_buffer()[i]));
                    other.inline_buffer()[i].~T();
                }
            }
        }
        other.m_outline_buffer = nullptr;
        other.m_size = 0;
        other.reset_capacity();
    }

    Vector(const Vector& other)
    {
        ensure_capacity(other.size());
        TypedTransfer<T>::copy(data(), other.data(), other.size());
        m_size = other.size();
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
                    for (int i = 0; i < m_size; ++i) {
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
        for (int i = 0; i < m_size; ++i)
            data()[i].~T();
        m_size = 0;
    }

    bool operator==(const Vector& other) const
    {
        if (m_size != other.m_size)
            return false;
        return TypedTransfer<T>::compare(data(), other.data(), size());
    }

    bool operator!=(const Vector& other) const
    {
        return !(*this == other);
    }

    bool contains_slow(const T& value) const
    {
        for (int i = 0; i < size(); ++i) {
            if (at(i) == value)
                return true;
        }
        return false;
    }

    // NOTE: Vector::is_null() exists for the benefit of String::copy().
    bool is_null() const { return is_empty(); }
    bool is_empty() const { return size() == 0; }
    int size() const { return m_size; }
    int capacity() const { return m_capacity; }

    T* data()
    {
        if constexpr (inline_capacity > 0)
            return m_outline_buffer ? m_outline_buffer : inline_buffer();
        return m_outline_buffer;
    }
    const T* data() const
    {
        if constexpr (inline_capacity > 0)
            return m_outline_buffer ? m_outline_buffer : inline_buffer();
        return m_outline_buffer;
    }

    const T& at(int i) const
    {
        ASSERT(i >= 0 && i < m_size);
        return data()[i];
    }
    T& at(int i)
    {
        ASSERT(i >= 0 && i < m_size);
        return data()[i];
    }

    const T& operator[](int i) const { return at(i); }
    T& operator[](int i) { return at(i); }

    const T& first() const { return at(0); }
    T& first() { return at(0); }

    const T& last() const { return at(size() - 1); }
    T& last() { return at(size() - 1); }

    T take_last()
    {
        ASSERT(!is_empty());
        T value = move(last());
        last().~T();
        --m_size;
        return value;
    }

    T take_first()
    {
        ASSERT(!is_empty());
        T value = move(first());
        remove(0);
        return value;
    }

    T take(int index)
    {
        T value = move(at(index));
        remove(index);
        return value;
    }

    void remove(int index)
    {
        ASSERT(index < m_size);

        if constexpr (Traits<T>::is_trivial()) {
            TypedTransfer<T>::copy(slot(index), slot(index + 1), m_size - index - 1);
        } else {
            at(index).~T();
            for (int i = index + 1; i < m_size; ++i) {
                new (slot(i - 1)) T(move(at(i)));
                at(i).~T();
            }
        }

        --m_size;
    }

    void insert(int index, T&& value)
    {
        ASSERT(index <= size());
        if (index == size())
            return append(move(value));
        grow_capacity(size() + 1);
        ++m_size;
        for (int i = size() - 1; i > index; --i) {
            new (slot(i)) T(move(at(i - 1)));
            at(i - 1).~T();
        }
        new (slot(index)) T(move(value));
    }

    void insert(int index, const T& value)
    {
        insert(index, T(value));
    }

    template<typename C>
    void insert_before_matching(T&& value, C callback)
    {
        for (int i = 0; i < size(); ++i) {
            if (callback(at(i))) {
                insert(i, move(value));
                return;
            }
        }
        append(move(value));
    }

    Vector& operator=(const Vector& other)
    {
        if (this != &other) {
            clear();
            ensure_capacity(other.size());
            TypedTransfer<T>::copy(data(), other.data(), other.size());
            m_size = other.size();
        }
        return *this;
    }

    void append(Vector&& other)
    {
        if (is_empty()) {
            *this = move(other);
            return;
        }
        auto other_size = other.size();
        Vector tmp = move(other);
        grow_capacity(size() + other_size);
        TypedTransfer<T>::move(data() + m_size, tmp.data(), other_size);
        m_size += other_size;
    }

    void append(const Vector& other)
    {
        grow_capacity(size() + other.size());
        TypedTransfer<T>::copy(data() + m_size, other.data(), other.size());
        m_size += other.m_size;
    }

    template<typename Callback>
    void remove_first_matching(Callback callback)
    {
        for (int i = 0; i < size(); ++i) {
            if (callback(at(i))) {
                remove(i);
                return;
            }
        }
    }

    template<typename Callback>
    void remove_all_matching(Callback callback)
    {
        for (int i = 0; i < size();) {
            if (callback(at(i))) {
                remove(i);
            } else {
                ++i;
            }
        }
    }

    void unchecked_append(T&& value)
    {
        ASSERT((size() + 1) <= capacity());
        new (slot(m_size)) T(move(value));
        ++m_size;
    }

    void unchecked_append(const T& value)
    {
        unchecked_append(T(value));
    }

    template<class... Args>
    void empend(Args&&... args)
    {
        grow_capacity(m_size + 1);
        new (slot(m_size)) T(forward<Args>(args)...);
        ++m_size;
    }

    void append(T&& value)
    {
        grow_capacity(size() + 1);
        new (slot(m_size)) T(move(value));
        ++m_size;
    }

    void append(const T& value)
    {
        append(T(value));
    }

    void prepend(T&& value)
    {
        insert(0, move(value));
    }

    void prepend(const T& value)
    {
        insert(0, value);
    }

    void prepend(Vector&& other)
    {
        if (other.is_empty())
            return;

        if (is_empty()) {
            *this = move(other);
            return;
        }

        auto other_size = other.size();
        grow_capacity(size() + other_size);

        for (int i = size() + other_size - 1; i >= other.size(); --i) {
            new (slot(i)) T(move(at(i - other_size)));
            at(i - other_size).~T();
        }

        Vector tmp = move(other);
        TypedTransfer<T>::move(slot(0), tmp.data(), tmp.size());
        m_size += other_size;
    }

    void append(const T* values, int count)
    {
        if (!count)
            return;
        grow_capacity(size() + count);
        TypedTransfer<T>::copy(slot(m_size), values, count);
        m_size += count;
    }

    void grow_capacity(int needed_capacity)
    {
        if (m_capacity >= needed_capacity)
            return;
        ensure_capacity(padded_capacity(needed_capacity));
    }

    void ensure_capacity(int needed_capacity)
    {
        if (m_capacity >= needed_capacity)
            return;
        int new_capacity = needed_capacity;
        auto* new_buffer = (T*)kmalloc(new_capacity * sizeof(T));

        if constexpr (Traits<T>::is_trivial()) {
            TypedTransfer<T>::copy(new_buffer, data(), m_size);
        } else {
            for (int i = 0; i < m_size; ++i) {
                new (&new_buffer[i]) T(move(at(i)));
                at(i).~T();
            }
        }
        if (m_outline_buffer)
            kfree(m_outline_buffer);
        m_outline_buffer = new_buffer;
        m_capacity = new_capacity;
    }

    void shrink(int new_size)
    {
        ASSERT(new_size <= size());
        if (new_size == size())
            return;

        if (!new_size) {
            clear();
            return;
        }

        for (int i = new_size; i < size(); ++i)
            at(i).~T();
        m_size = new_size;
    }

    void resize(int new_size)
    {
        if (new_size <= size())
            return shrink(new_size);

        ensure_capacity(new_size);
        for (int i = size(); i < new_size; ++i)
            new (slot(i)) T;
        m_size = new_size;
    }

    using Iterator = VectorIterator<Vector, T>;
    Iterator begin() { return Iterator(*this, 0); }
    Iterator end() { return Iterator(*this, size()); }

    using ConstIterator = VectorIterator<const Vector, const T>;
    ConstIterator begin() const { return ConstIterator(*this, 0); }
    ConstIterator end() const { return ConstIterator(*this, size()); }

    template<typename Finder>
    ConstIterator find(Finder finder) const
    {
        for (int i = 0; i < m_size; ++i) {
            if (finder(at(i)))
                return ConstIterator(*this, i);
        }
        return end();
    }

    template<typename Finder>
    Iterator find(Finder finder)
    {
        for (int i = 0; i < m_size; ++i) {
            if (finder(at(i)))
                return Iterator(*this, i);
        }
        return end();
    }

    ConstIterator find(const T& value) const
    {
        return find([&](auto& other) { return value == other; });
    }

    Iterator find(const T& value)
    {
        return find([&](auto& other) { return value == other; });
    }

    int find_first_index(const T& value)
    {
        for (int i = 0; i < m_size; ++i) {
            if (value == at(i))
                return i;
        }
        return -1;
    }

private:
    void reset_capacity()
    {
        m_capacity = inline_capacity;
    }

    static int padded_capacity(int capacity)
    {
        return max(int(4), capacity + (capacity / 4) + 4);
    }

    T* slot(int i) { return &data()[i]; }
    const T* slot(int i) const { return &data()[i]; }

    T* inline_buffer()
    {
        static_assert(inline_capacity > 0);
        return reinterpret_cast<T*>(m_inline_buffer_storage);
    }
    const T* inline_buffer() const
    {
        static_assert(inline_capacity > 0);
        return reinterpret_cast<const T*>(m_inline_buffer_storage);
    }

    int m_size { 0 };
    int m_capacity { 0 };

    alignas(T) u8 m_inline_buffer_storage[sizeof(T) * inline_capacity];
    T* m_outline_buffer { nullptr };
};

}

using AK::Vector;
