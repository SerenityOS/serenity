#pragma once

#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>
#include <AK/kmalloc.h>

namespace AK {

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
        for (int i = 0; i < other.size(); ++i)
            unchecked_append(other[i]);
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

    const T& at(int i) const { ASSERT(i >= 0 && i < m_size); return data()[i]; }
    T& at(int i) { ASSERT(i >= 0 && i < m_size); return data()[i]; }

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

    void remove(int index)
    {
        ASSERT(index < m_size);
        at(index).~T();
        for (int i = index + 1; i < m_size; ++i) {
            new (slot(i - 1)) T(move(at(i)));
            at(i).~T();
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

    Vector& operator=(const Vector& other)
    {
        if (this != &other) {
            clear();
            ensure_capacity(other.size());
            for (const auto& v : other)
                unchecked_append(v);
        }
        return *this;
    }

    void append(Vector&& other)
    {
        if (is_empty()) {
            *this = move(other);
            return;
        }
        Vector tmp = move(other);
        grow_capacity(size() + tmp.size());
        for (auto&& v : tmp)
            unchecked_append(move(v));
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

    void unchecked_append(T&& value)
    {
        ASSERT((size() + 1) <= capacity());
        new (slot(m_size)) T(move(value));
        ++m_size;
    }

    void unchecked_append(const T& value)
    {
        new (slot(m_size)) T(value);
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
        grow_capacity(size() + 1);
        new (slot(m_size)) T(value);
        ++m_size;
    }

    void prepend(const T& value)
    {
        grow_capacity(size() + 1);
        for (int i = size(); i > 0; --i) {
            new (slot(i)) T(move(at(i - 1)));
            at(i - 1).~T();
        }
        new (slot(0)) T(value);
        ++m_size;
    }

    void append(const T* values, int count)
    {
        if (!count)
            return;
        grow_capacity(size() + count);
        for (int i = 0; i < count; ++i)
            new (slot(m_size + i)) T(values[i]);
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
        for (int i = 0; i < m_size; ++i) {
            new (&new_buffer[i]) T(move(at(i)));
            at(i).~T();
        }
        if (m_outline_buffer)
            kfree(m_outline_buffer);
        m_outline_buffer = new_buffer;
        m_capacity = new_capacity;
    }

    void resize(int new_size)
    {
        if (new_size == size())
            return;

        if (!new_size) {
            clear();
            return;
        }

        if (new_size > size()) {
            ensure_capacity(new_size);
            for (int i = size(); i < new_size; ++i)
                new (slot(i)) T;
        } else {
            for (int i = new_size; i < size(); ++i)
                at(i).~T();
        }
        m_size = new_size;
    }

    class Iterator {
    public:
        bool operator!=(const Iterator& other) { return m_index != other.m_index; }
        bool operator==(const Iterator& other) { return m_index == other.m_index; }
        bool operator<(const Iterator& other) { return m_index < other.m_index; }
        Iterator& operator++() { ++m_index; return *this; }
        Iterator operator-(int value) { return { m_vector, m_index - value }; }
        Iterator operator+(int value) { return { m_vector, m_index + value }; }
        T& operator*() { return m_vector[m_index]; }
    private:
        friend class Vector;
        Iterator(Vector& vector, int index) : m_vector(vector), m_index(index) { }
        Vector& m_vector;
        int m_index { 0 };
    };

    Iterator begin() { return Iterator(*this, 0); }
    Iterator end() { return Iterator(*this, size()); }

    class ConstIterator {
    public:
        bool operator!=(const ConstIterator& other) { return m_index != other.m_index; }
        bool operator==(const ConstIterator& other) { return m_index == other.m_index; }
        bool operator<(const ConstIterator& other) { return m_index < other.m_index; }
        ConstIterator& operator++() { ++m_index; return *this; }
        ConstIterator operator-(int value) { return { m_vector, m_index - value }; }
        ConstIterator operator+(int value) { return { m_vector, m_index + value }; }
        const T& operator*() const { return m_vector[m_index]; }
    private:
        friend class Vector;
        ConstIterator(const Vector& vector, const int index) : m_vector(vector), m_index(index) { }
        const Vector& m_vector;
        int m_index { 0 };
    };

    ConstIterator begin() const { return ConstIterator(*this, 0); }
    ConstIterator end() const { return ConstIterator(*this, size()); }

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

    T* inline_buffer() { static_assert(inline_capacity > 0); return reinterpret_cast<T*>(m_inline_buffer_storage); }
    const T* inline_buffer() const { static_assert(inline_capacity > 0); return reinterpret_cast<const T*>(m_inline_buffer_storage); }

    int m_size { 0 };
    int m_capacity { 0 };

    alignas(T) byte m_inline_buffer_storage[sizeof(T) * inline_capacity];
    T* m_outline_buffer { nullptr };
};

}

using AK::Vector;
