#pragma once

#include "Buffer.h"
#include "Types.h"
#include <cstdlib>
#include <cstring>
#include <cstdio>

namespace AK {

class ByteBuffer {
public:
    ByteBuffer() { }
    ByteBuffer(std::nullptr_t) { }
    ByteBuffer(const ByteBuffer& other)
        : m_impl(other.m_impl.copyRef())
    {
    }
    ByteBuffer(ByteBuffer&& other)
        : m_impl(std::move(other.m_impl))
    {
    }
    ByteBuffer& operator=(ByteBuffer&& other)
    {
        if (this != &other)
            m_impl = std::move(other.m_impl);
        return *this;
    }

    static ByteBuffer createEmpty() { return ByteBuffer(Buffer<byte>::createUninitialized(0)); }
    static ByteBuffer createUninitialized(size_t size) { return ByteBuffer(Buffer<byte>::createUninitialized(size)); }
    static ByteBuffer copy(const byte* data, size_t size) { return ByteBuffer(Buffer<byte>::copy(data, size)); }
    static ByteBuffer wrap(byte* data, size_t size) { return ByteBuffer(Buffer<byte>::wrap(data, size)); }
    static ByteBuffer adopt(byte* data, size_t size) { return ByteBuffer(Buffer<byte>::adopt(data, size)); }

    ~ByteBuffer() { clear(); }
    void clear() { m_impl = nullptr; }

    operator bool() const { return !isNull(); }
    bool operator!() const { return isNull(); }
    bool isNull() const { return m_impl == nullptr; }

    byte& operator[](size_t i) { ASSERT(m_impl); return (*m_impl)[i]; }
    byte operator[](size_t i) const { ASSERT(m_impl); return (*m_impl)[i]; }
    bool isEmpty() const { return !m_impl || m_impl->isEmpty(); }
    size_t size() const { return m_impl ? m_impl->size() : 0; }

    byte* pointer() { return m_impl ? m_impl->pointer() : nullptr; }
    const byte* pointer() const { return m_impl ? m_impl->pointer() : nullptr; }

    byte* offsetPointer(size_t offset) { return m_impl ? m_impl->offsetPointer(offset) : nullptr; }
    const byte* offsetPointer(size_t offset) const { return m_impl ? m_impl->offsetPointer(offset) : nullptr; }

    const void* endPointer() const { return m_impl ? m_impl->endPointer() : nullptr; }

    // NOTE: trim() does not reallocate.
    void trim(size_t size)
    {
        if (m_impl)
            m_impl->trim(size);
    }

    ByteBuffer slice(size_t offset, size_t size) const
    {
        if (isNull())
            return { };
        if (offset >= this->size())
            return { };
        if (offset + size >= this->size())
            size = this->size() - offset;
        return copy(offsetPointer(offset), size);
    }

private:
    explicit ByteBuffer(RetainPtr<Buffer<byte>>&& impl)
        : m_impl(std::move(impl))
    {
    }

    RetainPtr<Buffer<byte>> m_impl;
};

}

using AK::ByteBuffer;

