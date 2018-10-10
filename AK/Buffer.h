#pragma once

#include "Assertions.h"
#include "Retainable.h"
#include "RetainPtr.h"
#include <cstdlib>
#include <cstring>
#include "kmalloc.h"

namespace AK {

template<typename T>
class Buffer : public Retainable<Buffer<T>> {
public:
    static RetainPtr<Buffer> createUninitialized(size_t count);
    static RetainPtr<Buffer> copy(const T*, size_t count);
    static RetainPtr<Buffer> wrap(T*, size_t count);
    static RetainPtr<Buffer> adopt(T*, size_t count);

    ~Buffer() { clear(); }

    void clear()
    {
        if (!m_elements)
            return;
        kfree(m_elements);
        m_elements = nullptr;
    }

    T& operator[](size_t i) { ASSERT(i < m_size); return m_elements[i]; }
    const T& operator[](size_t i) const { ASSERT(i < m_size); return m_elements[i]; }
    bool isEmpty() const { return !m_size; }
    size_t size() const { return m_size; }

    T* pointer() { return m_elements; }
    const T* pointer() const { return m_elements; }

    T* offsetPointer(size_t offset) { return m_elements + offset; }
    const T* offsetPointer(size_t offset) const { return m_elements + offset; }

    const void* endPointer() const { return m_elements + m_size; }

    // NOTE: trim() does not reallocate.
    void trim(size_t size)
    {
        ASSERT(size <= m_size);
        m_size = size;
    }

private:
    enum ConstructionMode { Uninitialized, Copy, Wrap, Adopt };
    explicit Buffer(size_t); // For ConstructionMode=Uninitialized
    Buffer(const T*, size_t, ConstructionMode); // For ConstructionMode=Copy
    Buffer(T*, size_t, ConstructionMode); // For ConstructionMode=Wrap/Adopt
    Buffer() { }

    T* m_elements { nullptr };
    size_t m_size { 0 };
    bool m_owned { false };
};

template<typename T>
inline Buffer<T>::Buffer(size_t size)
    : m_size(size)
{
    m_elements = static_cast<T*>(kmalloc(size * sizeof(T)));
    m_owned = true;
}

template<typename T>
inline Buffer<T>::Buffer(const T* elements, size_t size, ConstructionMode mode)
    : m_size(size)
{
    ASSERT(mode == Copy);
    m_elements = static_cast<T*>(kmalloc(size * sizeof(T)));
    memcpy(m_elements, elements, size * sizeof(T));
    m_owned = true;
}

template<typename T>
inline Buffer<T>::Buffer(T* elements, size_t size, ConstructionMode mode)
    : m_elements(elements)
    , m_size(size)
{
    if (mode == Adopt) {
        m_owned = true;
    } else if (mode == Wrap) {
        m_owned = false;
    }

}

template<typename T>
inline RetainPtr<Buffer<T>> Buffer<T>::createUninitialized(size_t size)
{
    return ::adopt(*new Buffer<T>(size));
}

template<typename T>
inline RetainPtr<Buffer<T>> Buffer<T>::copy(const T* elements, size_t size)
{
    return ::adopt(*new Buffer<T>(elements, size, Copy));
}

template<typename T>
inline RetainPtr<Buffer<T>> Buffer<T>::wrap(T* elements, size_t size)
{
    return ::adopt(*new Buffer<T>(elements, size, Wrap));
}

template<typename T>
inline RetainPtr<Buffer<T>> Buffer<T>::adopt(T* elements, size_t size)
{
    return ::adopt(*new Buffer<T>(elements, size, Adopt));
}

}

using AK::Buffer;

