#pragma once

#include "Assertions.h"
#include "kmalloc.h"

#define SANITIZE_VECTOR

template<typename T>
class Vector {
public:
    Vector() { }
    ~Vector();

    Vector(const Vector&&);
    Vector& operator=(const Vector&&);

    bool isEmpty() const { return m_size == 0; }
    size_t size() const { return m_size; }
    size_t capacity() const { return m_capacity; }

    void append(T&&);
    void clear();

    const T& operator[](size_t i) const { return m_elements[i]; }
    T& operator[](size_t i) { return m_elements[i]; }

private:
    Vector(const Vector&) = delete;
    Vector& operator=(const Vector&) = delete;

    void ensureCapacity(size_t);

    T* m_elements { nullptr };
    size_t m_size { 0 };
    size_t m_capacity { 0 };
};

template<typename T>
Vector<T>::~Vector()
{
    clear();
#ifdef SANITIZE_VECTOR
    m_elements = (T*)0xdddddddd;
    m_size = 0x8a8a8a8a;
    m_capacity = 0xa8a8a8a8;
#endif
}

template<typename T>
void Vector<T>::clear()
{
    if (!m_elements)
        return;
    for (size_t i = 0; i < m_size; ++i) {
        m_elements[i].~T();
    }
    kfree(m_elements);
    m_elements = nullptr;
    m_size = 0;
    m_capacity = 0;
}

template<typename T>
void Vector<T>::append(T&& element)
{
    ensureCapacity(m_size + 1);
    new (&m_elements[m_size]) T(move(element));
    ++m_size;
}

template<typename T>
void Vector<T>::ensureCapacity(size_t neededCapacity)
{
    if (neededCapacity <= m_capacity)
        return;
    size_t newCapacity = (neededCapacity + 8) & ~7;
    // FIXME: We need to support further growth here, jeez...
    ASSERT(m_capacity == 0);
    ASSERT(!m_elements);
    m_capacity = newCapacity;
    T* newElements = (T*)kmalloc(m_capacity * sizeof(T));
#ifdef SANITIZE_VECTOR
    memset(newElements, 0x66, m_capacity * sizeof(T));
#endif
    if (m_elements) {
        memcpy(newElements, m_elements, m_size * sizeof(T));
        kfree(m_elements);
    }
    m_elements = newElements;
}
