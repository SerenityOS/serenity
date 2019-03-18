#pragma once

#include "Assertions.h"
#include "OwnPtr.h"
#include "kmalloc.h"

namespace AK {

template<typename T> class Vector;

template<typename T>
class VectorImpl {
public:
    ~VectorImpl()
    {
        for (int i = 0; i < m_size; ++i)
            at(i).~T();
    }
    static OwnPtr<VectorImpl> create(int capacity)
    {
        int size = sizeof(VectorImpl) + sizeof(T) * capacity;
        void* slot = kmalloc(size);
        new (slot) VectorImpl(capacity);
        return OwnPtr<VectorImpl>((VectorImpl*)slot);
    }

    int size() const { return m_size; }
    int capacity() const { return m_capacity; }

    T& at(int i) { ASSERT(i < m_size); return *slot(i); }
    const T& at(int i) const { ASSERT(i < m_size); return *slot(i); }

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

private:
    friend class Vector<T>;

    VectorImpl(int capacity) : m_capacity(capacity) { }

    T* tail() { return reinterpret_cast<T*>(this + 1); }
    T* slot(int i) { return &tail()[i]; }

    const T* tail() const { return reinterpret_cast<const T*>(this + 1); }
    const T* slot(int i) const { return &tail()[i]; }

    int m_size { 0 };
    int m_capacity;
};

template<typename T>
class Vector {
public:
    Vector() { }
    ~Vector() { clear(); }

    Vector(Vector&& other)
        : m_impl(move(other.m_impl))
    {
    }

    Vector(const Vector& other)
    {
        ensure_capacity(other.size());
        for (int i = 0; i < other.size(); ++i)
            unchecked_append(other[i]);
    }

    Vector& operator=(Vector&& other)
    {
        if (this != &other)
            m_impl = move(other.m_impl);
        return *this;
    }

    void clear()
    {
        m_impl = nullptr;
    }

    void clear_with_capacity()
    {
        if (!m_impl)
            return;
        for (int i = 0; i < size(); ++i)
            at(i).~T();
        m_impl->m_size = 0;
    }

    bool contains_slow(const T& value) const
    {
        for (int i = 0; i < size(); ++i) {
            if (at(i) == value)
                return true;
        }
        return false;
    }

    bool is_empty() const { return size() == 0; }
    int size() const { return m_impl ? m_impl->size() : 0; }
    int capacity() const { return m_impl ? m_impl->capacity() : 0; }

    T* data() { return m_impl ? m_impl->slot(0) : nullptr; }
    const T* data() const { return m_impl ? m_impl->slot(0) : nullptr; }

    const T& at(int i) const { return m_impl->at(i); }
    T& at(int i) { return m_impl->at(i); }

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
        --m_impl->m_size;
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
        m_impl->remove(index);
    }

    void insert(int index, T&& value)
    {
        ASSERT(index <= size());
        if (index == size())
            return append(move(value));
        ensure_capacity(size() + 1);
        ++m_impl->m_size;
        for (int i = size() - 1; i > index; --i) {
            new (m_impl->slot(i)) T(move(m_impl->at(i - 1)));
            m_impl->at(i - 1).~T();
        }
        new (m_impl->slot(index)) T(move(value));
    }

    Vector& operator=(const Vector<T>& other)
    {
        if (this != &other) {
            clear();
            ensure_capacity(other.size());
            for (const auto& v : other)
                unchecked_append(v);
        }
        return *this;
    }

    void append(Vector<T>&& other)
    {
        if (!m_impl) {
            m_impl = move(other.m_impl);
            return;
        }
        Vector<T> tmp = move(other);
        ensure_capacity(size() + tmp.size());
        for (auto&& v : tmp) {
            unchecked_append(move(v));
        }
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
        new (m_impl->slot(m_impl->m_size)) T(move(value));
        ++m_impl->m_size;
    }

    void unchecked_append(const T& value)
    {
        new (m_impl->slot(m_impl->m_size)) T(value);
        ++m_impl->m_size;
    }

    void append(T&& value)
    {
        ensure_capacity(size() + 1);
        new (m_impl->slot(m_impl->m_size)) T(move(value));
        ++m_impl->m_size;
    }

    void append(const T& value)
    {
        ensure_capacity(size() + 1);
        new (m_impl->slot(m_impl->m_size)) T(value);
        ++m_impl->m_size;
    }

    void append(const T* values, int count)
    {
        if (!count)
            return;
        ensure_capacity(size() + count);
        for (int i = 0; i < count; ++i)
            new (m_impl->slot(m_impl->m_size + i)) T(values[i]);
        m_impl->m_size += count;
    }

    void ensure_capacity(int neededCapacity)
    {
        if (capacity() >= neededCapacity)
            return;
        int new_capacity = padded_capacity(neededCapacity);
        auto new_impl = VectorImpl<T>::create(new_capacity);
        if (m_impl) {
            new_impl->m_size = m_impl->m_size;
            for (int i = 0; i < size(); ++i) {
                new (new_impl->slot(i)) T(move(m_impl->at(i)));
                m_impl->at(i).~T();
            }
        }
        m_impl = move(new_impl);
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
                new (m_impl->slot(i)) T;
        } else {
            for (int i = new_size; i < size(); ++i)
                m_impl->at(i).~T();
        }
        m_impl->m_size = new_size;
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
    static int padded_capacity(int capacity)
    {
        return max(int(4), capacity + (capacity / 4) + 4);
    }

    OwnPtr<VectorImpl<T>> m_impl;
};

}

using AK::Vector;
