#pragma once

#include "Assertions.h"
#include "OwnPtr.h"
#include "kmalloc.h"

namespace AK {

template<typename T, typename Allocator> class Vector;

struct KmallocAllocator {
    static void* allocate(ssize_t size) { return kmalloc(size); }
    static void deallocate(void* ptr) { kfree(ptr); }
};

struct KmallocEternalAllocator {
    static void* allocate(ssize_t size) { return kmalloc_eternal(size); }
    static void deallocate(void*) { }
};

template<typename T, typename Allocator>
class VectorImpl {
public:
    ~VectorImpl() { }
    static VectorImpl* create(ssize_t capacity)
    {
        ssize_t size = sizeof(VectorImpl) + sizeof(T) * capacity;
        void* slot = Allocator::allocate(size);
        new (slot) VectorImpl(capacity);
        return (VectorImpl*)slot;
    }

    ssize_t size() const { return m_size; }
    ssize_t capacity() const { return m_capacity; }

    T& at(ssize_t i) { ASSERT(i < m_size); return *slot(i); }
    const T& at(ssize_t i) const { ASSERT(i < m_size); return *slot(i); }

    void remove(ssize_t index)
    {
        ASSERT(index < m_size);
        at(index).~T();
        for (ssize_t i = index + 1; i < m_size; ++i) {
            new (slot(i - 1)) T(move(at(i)));
            at(i).~T();
        }

        --m_size;
    }

//private:
    friend class Vector<T, Allocator>;

    VectorImpl(ssize_t capacity) : m_capacity(capacity) { }

    T* tail() { return reinterpret_cast<T*>(this + 1); }
    T* slot(ssize_t i) { return &tail()[i]; }

    const T* tail() const { return reinterpret_cast<const T*>(this + 1); }
    const T* slot(ssize_t i) const { return &tail()[i]; }

    ssize_t m_size { 0 };
    ssize_t m_capacity;
};

template<typename T, typename Allocator = KmallocAllocator>
class Vector {
public:
    Vector() { }
    ~Vector() { clear(); }

    Vector(Vector&& other)
        : m_impl(other.m_impl)
    {
        other.m_impl = nullptr;
    }

    Vector(const Vector& other)
    {
        ensure_capacity(other.size());
        for (ssize_t i = 0; i < other.size(); ++i)
            unchecked_append(other[i]);
    }

    Vector& operator=(Vector&& other)
    {
        if (this != &other) {
            m_impl = other.m_impl;
            other.m_impl = nullptr;
        }
        return *this;
    }

    void clear()
    {
        for (ssize_t i = 0; i < size(); ++i) {
            at(i).~T();
        }
        Allocator::deallocate(m_impl);
        m_impl = nullptr;
    }

    void clear_with_capacity()
    {
        if (!m_impl)
            return;
        for (ssize_t i = 0; i < size(); ++i)
            at(i).~T();
        m_impl->m_size = 0;
    }

    bool contains_slow(const T& value) const
    {
        for (ssize_t i = 0; i < size(); ++i) {
            if (at(i) == value)
                return true;
        }
        return false;
    }

    bool is_empty() const { return size() == 0; }
    ssize_t size() const { return m_impl ? m_impl->size() : 0; }
    ssize_t capacity() const { return m_impl ? m_impl->capacity() : 0; }

    T* data() { return m_impl ? m_impl->slot(0) : nullptr; }
    const T* data() const { return m_impl ? m_impl->slot(0) : nullptr; }

    const T& at(ssize_t i) const { return m_impl->at(i); }
    T& at(ssize_t i) { return m_impl->at(i); }

    const T& operator[](ssize_t i) const { return at(i); }
    T& operator[](ssize_t i) { return at(i); }

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

    void remove(ssize_t index)
    {
        m_impl->remove(index);
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
            m_impl = other.m_impl;
            other.m_impl = nullptr;
            return;
        }
        Vector<T> tmp = move(other);
        ensure_capacity(size() + tmp.size());
        for (auto&& v : tmp) {
            unchecked_append(move(v));
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

    void append(const T* values, ssize_t count)
    {
        ensure_capacity(size() + count);
        for (ssize_t i = 0; i < count; ++i)
            new (m_impl->slot(m_impl->m_size + i)) T(values[i]);
        m_impl->m_size += count;
    }

    void ensure_capacity(ssize_t neededCapacity)
    {
        if (capacity() >= neededCapacity)
            return;
        ssize_t new_capacity = padded_capacity(neededCapacity);
        auto new_impl = VectorImpl<T, Allocator>::create(new_capacity);
        if (m_impl) {
            new_impl->m_size = m_impl->m_size;
            for (ssize_t i = 0; i < size(); ++i) {
                new (new_impl->slot(i)) T(move(m_impl->at(i)));
                m_impl->at(i).~T();
            }
            Allocator::deallocate(m_impl);
        }
        m_impl = new_impl;
    }

    void resize(ssize_t new_size)
    {
        ASSERT(new_size >= size());
        if (!new_size)
            return;
        ensure_capacity(new_size);
        for (ssize_t i = size(); i < new_size; ++i)
            new (m_impl->slot(i)) T;
        m_impl->m_size = new_size;
    }

    class Iterator {
    public:
        bool operator!=(const Iterator& other) { return m_index != other.m_index; }
        Iterator& operator++() { ++m_index; return *this; }
        T& operator*() { return m_vector[m_index]; }
    private:
        friend class Vector;
        Iterator(Vector& vector, ssize_t index) : m_vector(vector), m_index(index) { }
        Vector& m_vector;
        ssize_t m_index { 0 };
    };

    Iterator begin() { return Iterator(*this, 0); }
    Iterator end() { return Iterator(*this, size()); }

    class ConstIterator {
    public:
        bool operator!=(const ConstIterator& other) { return m_index != other.m_index; }
        ConstIterator& operator++() { ++m_index; return *this; }
        const T& operator*() const { return m_vector[m_index]; }
    private:
        friend class Vector;
        ConstIterator(const Vector& vector, const ssize_t index) : m_vector(vector), m_index(index) { }
        const Vector& m_vector;
        ssize_t m_index { 0 };
    };

    ConstIterator begin() const { return ConstIterator(*this, 0); }
    ConstIterator end() const { return ConstIterator(*this, size()); }

//private:
    static ssize_t padded_capacity(ssize_t capacity)
    {
        return max(ssize_t(4), capacity + (capacity / 4) + 4);
    }

    VectorImpl<T, Allocator>* m_impl { nullptr };
};

}

using AK::Vector;
using AK::KmallocEternalAllocator;
using AK::KmallocAllocator;
