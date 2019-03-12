#pragma once

#include "Assertions.h"
#include "Retainable.h"
#include "RetainPtr.h"
#include "StdLibExtras.h"
#include "kmalloc.h"

namespace AK {

template<typename T>
class Buffer : public Retainable<Buffer<T>> {
public:
    static Retained<Buffer> create_uninitialized(ssize_t count);
    static Retained<Buffer> create_zeroed(ssize_t count);
    static Retained<Buffer> copy(const T*, ssize_t count);
    static Retained<Buffer> wrap(T*, ssize_t count);
    static Retained<Buffer> adopt(T*, ssize_t count);

    ~Buffer() { clear(); }

    void clear()
    {
        if (!m_elements)
            return;
        if (m_owned)
            kfree(m_elements);
        m_elements = nullptr;
    }

    T& operator[](ssize_t i) { ASSERT(i < m_size); return m_elements[i]; }
    const T& operator[](ssize_t i) const { ASSERT(i < m_size); return m_elements[i]; }
    bool is_empty() const { return !m_size; }
    ssize_t size() const { return m_size; }

    T* pointer() { return m_elements; }
    const T* pointer() const { return m_elements; }

    T* offset_pointer(ssize_t offset) { return m_elements + offset; }
    const T* offset_pointer(ssize_t offset) const { return m_elements + offset; }

    const void* end_pointer() const { return m_elements + m_size; }

    // NOTE: trim() does not reallocate.
    void trim(ssize_t size)
    {
        ASSERT(size <= m_size);
        m_size = size;
    }

    void grow(ssize_t size);

private:
    enum ConstructionMode { Uninitialized, Copy, Wrap, Adopt };
    explicit Buffer(ssize_t); // For ConstructionMode=Uninitialized
    Buffer(const T*, ssize_t, ConstructionMode); // For ConstructionMode=Copy
    Buffer(T*, ssize_t, ConstructionMode); // For ConstructionMode=Wrap/Adopt
    Buffer() { }

    T* m_elements { nullptr };
    ssize_t m_size { 0 };
    bool m_owned { false };
};

template<typename T>
inline Buffer<T>::Buffer(ssize_t size)
    : m_size(size)
{
    m_elements = static_cast<T*>(kmalloc(size * sizeof(T)));
    m_owned = true;
}

template<typename T>
inline Buffer<T>::Buffer(const T* elements, ssize_t size, ConstructionMode mode)
    : m_size(size)
{
    ASSERT(mode == Copy);
    m_elements = static_cast<T*>(kmalloc(size * sizeof(T)));
    memcpy(m_elements, elements, size * sizeof(T));
    m_owned = true;
}

template<typename T>
inline Buffer<T>::Buffer(T* elements, ssize_t size, ConstructionMode mode)
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
inline void Buffer<T>::grow(ssize_t size)
{
    ASSERT(size > m_size);
    ASSERT(m_owned);
    T* new_elements = static_cast<T*>(kmalloc(size * sizeof(T)));
    memcpy(new_elements, m_elements, m_size * sizeof(T));
    T* old_elements = m_elements;
    m_elements = new_elements;
    m_size = size;
    kfree(old_elements);
}

template<typename T>
inline Retained<Buffer<T>> Buffer<T>::create_uninitialized(ssize_t size)
{
    return ::adopt(*new Buffer<T>(size));
}

template<typename T>
inline Retained<Buffer<T>> Buffer<T>::create_zeroed(ssize_t size)
{
    auto buffer = ::adopt(*new Buffer<T>(size));
    memset(buffer->pointer(), 0, size);
    return buffer;
}

template<typename T>
inline Retained<Buffer<T>> Buffer<T>::copy(const T* elements, ssize_t size)
{
    return ::adopt(*new Buffer<T>(elements, size, Copy));
}

template<typename T>
inline Retained<Buffer<T>> Buffer<T>::wrap(T* elements, ssize_t size)
{
    return ::adopt(*new Buffer<T>(elements, size, Wrap));
}

template<typename T>
inline Retained<Buffer<T>> Buffer<T>::adopt(T* elements, ssize_t size)
{
    return ::adopt(*new Buffer<T>(elements, size, Adopt));
}

}

using AK::Buffer;

