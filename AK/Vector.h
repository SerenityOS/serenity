#pragma once

#include "Assertions.h"
#include "OwnPtr.h"
#include <new>
#include <stdlib.h>
#include <stdio.h>
#include <algorithm>

namespace AK {

template<typename T> class Vector;

template<typename T>
class VectorImpl {
public:
    ~VectorImpl() { }
    static OwnPtr<VectorImpl> create(unsigned capacity)
    {
        size_t size = sizeof(VectorImpl) + sizeof(T) * capacity;
        void* slot = kmalloc(size);
        return OwnPtr<VectorImpl>(new (slot) VectorImpl(capacity));
    }

    unsigned size() const { return m_size; }
    unsigned capacity() const { return m_capacity; }

    T& at(unsigned i) { return *slot(i); }
    const T& at(unsigned i) const { return *slot(i); }

private:
    friend class Vector<T>;

    VectorImpl(unsigned capacity) : m_capacity(capacity) { }

    T* tail() { return reinterpret_cast<T*>(this + 1); }
    T* slot(unsigned i) { return &tail()[i]; }

    const T* tail() const { return reinterpret_cast<const T*>(this + 1); }
    const T* slot(unsigned i) const { return &tail()[i]; }

    unsigned m_size { 0 };
    unsigned m_capacity;
};

template<typename T>
class Vector {
public:
    Vector() { }
    ~Vector() { clear(); }

    Vector(Vector&& other)
        : m_impl(std::move(other.m_impl))
    {
    }

    Vector& operator=(Vector&& other)
    {
        if (this != &other)
            m_impl = std::move(other.m_impl);
        return *this;
    }

    void clear()
    {
        for (unsigned i = 0; i < size(); ++i) {
            at(i).~T();
        }
        m_impl = nullptr;
    }

    bool isEmpty() const { return size() == 0; }
    unsigned size() const { return m_impl ? m_impl->size() : 0; }
    unsigned capacity() const { return m_impl ? m_impl->capacity() : 0; }

    const T& at(unsigned i) const { return m_impl->at(i); }
    T& at(unsigned i) { return m_impl->at(i); }

    const T& operator[](unsigned i) const { return at(i); }
    T& operator[](unsigned i) { return at(i); }

    const T& first() const { return at(0); }
    T& first() { return at(0); }

    const T& last() const { return at(size() - 1); }
    T& last() { return at(size() - 1); }

    T takeLast()
    {
        ASSERT(!isEmpty());
        T value = std::move(last());
        last().~T();
        --m_impl->m_size;
        return value;
    }

    void append(T&& value)
    {
        ensureCapacity(size() + 1);
        new (m_impl->slot(m_impl->m_size)) T(std::move(value));
        ++m_impl->m_size;
    }

    void append(const T& value)
    {
        ensureCapacity(size() + 1);
        new (m_impl->slot(m_impl->m_size)) T(value);
        ++m_impl->m_size;
    }

    void ensureCapacity(unsigned neededCapacity)
    {
        if (capacity() >= neededCapacity)
            return;
        size_t newCapacity = paddedCapacity(neededCapacity);
        auto newImpl = VectorImpl<T>::create(newCapacity);
        if (m_impl) {
            newImpl->m_size = m_impl->m_size;
            for (unsigned i = 0; i < size(); ++i) {
                new (newImpl->slot(i)) T(std::move(m_impl->at(i)));
                m_impl->at(i).~T();
            }
        }
        m_impl = std::move(newImpl);
    }

    class Iterator {
    public:
        bool operator!=(const Iterator& other) { return m_index != other.m_index; }
        Iterator& operator++() { ++m_index; return *this; }
        T& operator*() { return m_vector[m_index]; }
    private:
        friend class Vector;
        Iterator(Vector& vector, unsigned index) : m_vector(vector), m_index(index) { }
        Vector& m_vector;
        unsigned m_index { 0 };
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
        ConstIterator(const Vector& vector, const unsigned index) : m_vector(vector), m_index(index) { }
        const Vector& m_vector;
        unsigned m_index { 0 };
    };

    ConstIterator begin() const { return Iterator(*this, 0); }
    ConstIterator end() const { return Iterator(*this, size()); }

private:
    static unsigned paddedCapacity(unsigned capacity)
    {
        return std::max(4u, capacity + (capacity / 4) + 4);
    }

    OwnPtr<VectorImpl<T>> m_impl;
};

}

using AK::Vector;

