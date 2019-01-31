#pragma once

#include "Buffer.h"
#include "Types.h"
#include "StdLibExtras.h"

namespace AK {

class ByteBuffer {
public:
    ByteBuffer() { }
    ByteBuffer(std::nullptr_t) { }
    ByteBuffer(const ByteBuffer& other)
        : m_impl(other.m_impl.copy_ref())
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
        m_impl = other.m_impl.copy_ref();
        return *this;
    }

    static ByteBuffer create_uninitialized(size_t size) { return ByteBuffer(Buffer<byte>::create_uninitialized(size)); }
    static ByteBuffer copy(const byte* data, size_t size) { return ByteBuffer(Buffer<byte>::copy(data, size)); }
    static ByteBuffer wrap(byte* data, size_t size) { return ByteBuffer(Buffer<byte>::wrap(data, size)); }
    static ByteBuffer adopt(byte* data, size_t size) { return ByteBuffer(Buffer<byte>::adopt(data, size)); }

    ~ByteBuffer() { clear(); }
    void clear() { m_impl = nullptr; }

    operator bool() const { return !is_null(); }
    bool operator!() const { return is_null(); }
    bool is_null() const { return m_impl == nullptr; }

    byte& operator[](size_t i) { ASSERT(m_impl); return (*m_impl)[i]; }
    byte operator[](size_t i) const { ASSERT(m_impl); return (*m_impl)[i]; }
    bool is_empty() const { return !m_impl || m_impl->is_empty(); }
    size_t size() const { return m_impl ? m_impl->size() : 0; }

    byte* pointer() { return m_impl ? m_impl->pointer() : nullptr; }
    const byte* pointer() const { return m_impl ? m_impl->pointer() : nullptr; }

    byte* offset_pointer(size_t offset) { return m_impl ? m_impl->offset_pointer(offset) : nullptr; }
    const byte* offset_pointer(size_t offset) const { return m_impl ? m_impl->offset_pointer(offset) : nullptr; }

    const void* end_pointer() const { return m_impl ? m_impl->end_pointer() : nullptr; }

    // NOTE: trim() does not reallocate.
    void trim(size_t size)
    {
        if (m_impl)
            m_impl->trim(size);
    }

    ByteBuffer slice(size_t offset, size_t size) const
    {
        if (is_null())
            return { };
        if (offset >= this->size())
            return { };
        if (offset + size >= this->size())
            size = this->size() - offset;
        return copy(offset_pointer(offset), size);
    }

    void grow(size_t size)
    {
        if (!m_impl)
            m_impl = Buffer<byte>::create_uninitialized(size);
        else
            m_impl->grow(size);
    }

private:
    explicit ByteBuffer(RetainPtr<Buffer<byte>>&& impl)
        : m_impl(move(impl))
    {
    }

    RetainPtr<Buffer<byte>> m_impl;
};

}

using AK::ByteBuffer;

