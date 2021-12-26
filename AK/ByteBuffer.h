#pragma once

#include "StdLibExtras.h"
#include "Types.h"
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/kmalloc.h>

namespace AK {

class ByteBufferImpl : public RefCounted<ByteBufferImpl> {
public:
    static NonnullRefPtr<ByteBufferImpl> create_uninitialized(int size);
    static NonnullRefPtr<ByteBufferImpl> create_zeroed(int);
    static NonnullRefPtr<ByteBufferImpl> copy(const void*, int);
    static NonnullRefPtr<ByteBufferImpl> wrap(void*, int);
    static NonnullRefPtr<ByteBufferImpl> wrap(const void*, int);
    static NonnullRefPtr<ByteBufferImpl> adopt(void*, int);

    ~ByteBufferImpl() { clear(); }

    void clear()
    {
        if (!m_data)
            return;
        if (m_owned)
            kfree(m_data);
        m_data = nullptr;
    }

    u8& operator[](int i)
    {
        ASSERT(i < m_size);
        return m_data[i];
    }
    const u8& operator[](int i) const
    {
        ASSERT(i < m_size);
        return m_data[i];
    }
    bool is_empty() const { return !m_size; }
    int size() const { return m_size; }

    u8* pointer() { return m_data; }
    const u8* pointer() const { return m_data; }

    u8* offset_pointer(int offset) { return m_data + offset; }
    const u8* offset_pointer(int offset) const { return m_data + offset; }

    void* end_pointer() { return m_data + m_size; }
    const void* end_pointer() const { return m_data + m_size; }

    // NOTE: trim() does not reallocate.
    void trim(int size)
    {
        ASSERT(size <= m_size);
        m_size = size;
    }

    void grow(int size);

private:
    enum ConstructionMode {
        Uninitialized,
        Copy,
        Wrap,
        Adopt
    };
    explicit ByteBufferImpl(int);                       // For ConstructionMode=Uninitialized
    ByteBufferImpl(const void*, int, ConstructionMode); // For ConstructionMode=Copy
    ByteBufferImpl(void*, int, ConstructionMode);       // For ConstructionMode=Wrap/Adopt
    ByteBufferImpl() {}

    u8* m_data { nullptr };
    int m_size { 0 };
    bool m_owned { false };
};

class ByteBuffer {
public:
    ByteBuffer() {}
    ByteBuffer(std::nullptr_t) {}
    ByteBuffer(const ByteBuffer& other)
        : m_impl(other.m_impl)
    {
    }
    ByteBuffer(ByteBuffer&& other)
        : m_impl(move(other.m_impl))
    {
    }
    ByteBuffer& operator=(ByteBuffer&& other)
    {
        if (this != &other)
            m_impl = move(other.m_impl);
        return *this;
    }
    ByteBuffer& operator=(const ByteBuffer& other)
    {
        if (this != &other)
            m_impl = other.m_impl;
        return *this;
    }

    static ByteBuffer create_uninitialized(int size) { return ByteBuffer(ByteBufferImpl::create_uninitialized(size)); }
    static ByteBuffer create_zeroed(int size) { return ByteBuffer(ByteBufferImpl::create_zeroed(size)); }
    static ByteBuffer copy(const void* data, int size) { return ByteBuffer(ByteBufferImpl::copy(data, size)); }
    static ByteBuffer wrap(const void* data, int size) { return ByteBuffer(ByteBufferImpl::wrap(data, size)); }
    static ByteBuffer wrap(void* data, int size) { return ByteBuffer(ByteBufferImpl::wrap(data, size)); }
    static ByteBuffer adopt(void* data, int size) { return ByteBuffer(ByteBufferImpl::adopt(data, size)); }

    ~ByteBuffer() { clear(); }
    void clear() { m_impl = nullptr; }

    operator bool() const { return !is_null(); }
    bool operator!() const { return is_null(); }
    bool is_null() const { return m_impl == nullptr; }

    u8& operator[](int i)
    {
        ASSERT(m_impl);
        return (*m_impl)[i];
    }
    u8 operator[](int i) const
    {
        ASSERT(m_impl);
        return (*m_impl)[i];
    }
    bool is_empty() const { return !m_impl || m_impl->is_empty(); }
    int size() const { return m_impl ? m_impl->size() : 0; }

    u8* data() { return pointer(); }
    const u8* data() const { return pointer(); }

    u8* pointer() { return m_impl ? m_impl->pointer() : nullptr; }
    const u8* pointer() const { return m_impl ? m_impl->pointer() : nullptr; }

    u8* offset_pointer(int offset) { return m_impl ? m_impl->offset_pointer(offset) : nullptr; }
    const u8* offset_pointer(int offset) const { return m_impl ? m_impl->offset_pointer(offset) : nullptr; }

    void* end_pointer() { return m_impl ? m_impl->end_pointer() : nullptr; }
    const void* end_pointer() const { return m_impl ? m_impl->end_pointer() : nullptr; }

    ByteBuffer isolated_copy() const
    {
        if (!m_impl)
            return {};
        return copy(m_impl->pointer(), m_impl->size());
    }

    // NOTE: trim() does not reallocate.
    void trim(int size)
    {
        if (m_impl)
            m_impl->trim(size);
    }

    ByteBuffer slice(int offset, int size) const
    {
        if (is_null())
            return {};
        if (offset >= this->size())
            return {};
        if (offset + size >= this->size())
            size = this->size() - offset;
        return copy(offset_pointer(offset), size);
    }

    void grow(int size)
    {
        if (!m_impl)
            m_impl = ByteBufferImpl::create_uninitialized(size);
        else
            m_impl->grow(size);
    }

    void append(const void* data, int data_size)
    {
        int old_size = size();
        grow(size() + data_size);
        memcpy(pointer() + old_size, data, data_size);
    }

private:
    explicit ByteBuffer(RefPtr<ByteBufferImpl>&& impl)
        : m_impl(move(impl))
    {
    }

    RefPtr<ByteBufferImpl> m_impl;
};

inline ByteBufferImpl::ByteBufferImpl(int size)
    : m_size(size)
{
    m_data = static_cast<u8*>(kmalloc(size));
    m_owned = true;
}

inline ByteBufferImpl::ByteBufferImpl(const void* data, int size, ConstructionMode mode)
    : m_size(size)
{
    ASSERT(mode == Copy);
    m_data = static_cast<u8*>(kmalloc(size));
    memcpy(m_data, data, size);
    m_owned = true;
}

inline ByteBufferImpl::ByteBufferImpl(void* data, int size, ConstructionMode mode)
    : m_data(static_cast<u8*>(data))
    , m_size(size)
{
    if (mode == Adopt) {
        m_owned = true;
    } else if (mode == Wrap) {
        m_owned = false;
    }
}

inline void ByteBufferImpl::grow(int size)
{
    ASSERT(size > m_size);
    ASSERT(m_owned);
    u8* new_data = static_cast<u8*>(kmalloc(size));
    memcpy(new_data, m_data, m_size);
    u8* old_data = m_data;
    m_data = new_data;
    m_size = size;
    kfree(old_data);
}

inline NonnullRefPtr<ByteBufferImpl> ByteBufferImpl::create_uninitialized(int size)
{
    return ::adopt(*new ByteBufferImpl(size));
}

inline NonnullRefPtr<ByteBufferImpl> ByteBufferImpl::create_zeroed(int size)
{
    auto buffer = ::adopt(*new ByteBufferImpl(size));
    memset(buffer->pointer(), 0, size);
    return buffer;
}

inline NonnullRefPtr<ByteBufferImpl> ByteBufferImpl::copy(const void* data, int size)
{
    return ::adopt(*new ByteBufferImpl(data, size, Copy));
}

inline NonnullRefPtr<ByteBufferImpl> ByteBufferImpl::wrap(void* data, int size)
{
    return ::adopt(*new ByteBufferImpl(data, size, Wrap));
}

inline NonnullRefPtr<ByteBufferImpl> ByteBufferImpl::wrap(const void* data, int size)
{
    return ::adopt(*new ByteBufferImpl(const_cast<void*>(data), size, Wrap));
}

inline NonnullRefPtr<ByteBufferImpl> ByteBufferImpl::adopt(void* data, int size)
{
    return ::adopt(*new ByteBufferImpl(data, size, Adopt));
}

}

using AK::ByteBuffer;
