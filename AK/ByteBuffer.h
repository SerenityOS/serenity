/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/LogStream.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <AK/kmalloc.h>

namespace AK {

class ByteBufferImpl : public RefCounted<ByteBufferImpl> {
public:
    static NonnullRefPtr<ByteBufferImpl> create_uninitialized(size_t size);
    static NonnullRefPtr<ByteBufferImpl> create_zeroed(size_t);
    static NonnullRefPtr<ByteBufferImpl> copy(const void*, size_t);
    static NonnullRefPtr<ByteBufferImpl> wrap(void*, size_t);
    static NonnullRefPtr<ByteBufferImpl> wrap(const void*, size_t);
    static NonnullRefPtr<ByteBufferImpl> adopt(void*, size_t);

    ~ByteBufferImpl() { clear(); }

    void clear()
    {
        if (!m_data)
            return;
        if (m_owned)
            kfree(m_data);
        m_data = nullptr;
    }

    u8& operator[](size_t i)
    {
        ASSERT(i < m_size);
        return m_data[i];
    }
    const u8& operator[](size_t i) const
    {
        ASSERT(i < m_size);
        return m_data[i];
    }
    bool is_empty() const { return !m_size; }
    size_t size() const { return m_size; }

    u8* data() { return m_data; }
    const u8* data() const { return m_data; }

    u8* offset_pointer(int offset) { return m_data + offset; }
    const u8* offset_pointer(int offset) const { return m_data + offset; }

    void* end_pointer() { return m_data + m_size; }
    const void* end_pointer() const { return m_data + m_size; }

    // NOTE: trim() does not reallocate.
    void trim(size_t size)
    {
        ASSERT(size <= m_size);
        m_size = size;
    }

    void grow(size_t size);

private:
    enum ConstructionMode {
        Uninitialized,
        Copy,
        Wrap,
        Adopt
    };
    explicit ByteBufferImpl(size_t);                       // For ConstructionMode=Uninitialized
    ByteBufferImpl(const void*, size_t, ConstructionMode); // For ConstructionMode=Copy
    ByteBufferImpl(void*, size_t, ConstructionMode);       // For ConstructionMode=Wrap/Adopt
    ByteBufferImpl() {}

    u8* m_data { nullptr };
    size_t m_size { 0 };
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

    static ByteBuffer create_uninitialized(size_t size) { return ByteBuffer(ByteBufferImpl::create_uninitialized(size)); }
    static ByteBuffer create_zeroed(size_t size) { return ByteBuffer(ByteBufferImpl::create_zeroed(size)); }
    static ByteBuffer copy(const void* data, size_t size) { return ByteBuffer(ByteBufferImpl::copy(data, size)); }
    static ByteBuffer wrap(const void* data, size_t size) { return ByteBuffer(ByteBufferImpl::wrap(data, size)); }
    static ByteBuffer wrap(void* data, size_t size) { return ByteBuffer(ByteBufferImpl::wrap(data, size)); }
    static ByteBuffer adopt(void* data, size_t size) { return ByteBuffer(ByteBufferImpl::adopt(data, size)); }

    ~ByteBuffer() { clear(); }
    void clear() { m_impl = nullptr; }

    operator bool() const { return !is_null(); }
    bool operator!() const { return is_null(); }
    bool is_null() const { return m_impl == nullptr; }

    u8& operator[](size_t i)
    {
        ASSERT(m_impl);
        return (*m_impl)[i];
    }
    u8 operator[](size_t i) const
    {
        ASSERT(m_impl);
        return (*m_impl)[i];
    }
    bool is_empty() const { return !m_impl || m_impl->is_empty(); }
    size_t size() const { return m_impl ? m_impl->size() : 0; }

    u8* data() { return m_impl ? m_impl->data() : nullptr; }
    const u8* data() const { return m_impl ? m_impl->data() : nullptr; }

    u8* offset_pointer(int offset) { return m_impl ? m_impl->offset_pointer(offset) : nullptr; }
    const u8* offset_pointer(int offset) const { return m_impl ? m_impl->offset_pointer(offset) : nullptr; }

    void* end_pointer() { return m_impl ? m_impl->end_pointer() : nullptr; }
    const void* end_pointer() const { return m_impl ? m_impl->end_pointer() : nullptr; }

    ByteBuffer isolated_copy() const
    {
        if (!m_impl)
            return {};
        return copy(m_impl->data(), m_impl->size());
    }

    // NOTE: trim() does not reallocate.
    void trim(size_t size)
    {
        if (m_impl)
            m_impl->trim(size);
    }

    ByteBuffer slice_view(size_t offset, size_t size) const
    {
        if (is_null())
            return {};
        if (offset >= this->size())
            return {};
        if (offset + size >= this->size())
            size = this->size() - offset;
        return wrap(offset_pointer(offset), size);
    }

    ByteBuffer slice(size_t offset, size_t size) const
    {
        if (is_null())
            return {};
        if (offset >= this->size())
            return {};
        if (offset + size >= this->size())
            size = this->size() - offset;
        return copy(offset_pointer(offset), size);
    }

    void grow(size_t size)
    {
        if (!m_impl)
            m_impl = ByteBufferImpl::create_uninitialized(size);
        else
            m_impl->grow(size);
    }

    void append(const void* data, size_t data_size)
    {
        int old_size = size();
        grow(size() + data_size);
        memcpy(this->data() + old_size, data, data_size);
    }

private:
    explicit ByteBuffer(RefPtr<ByteBufferImpl>&& impl)
        : m_impl(move(impl))
    {
    }

    RefPtr<ByteBufferImpl> m_impl;
};

inline ByteBufferImpl::ByteBufferImpl(size_t size)
    : m_size(size)
{
    m_data = static_cast<u8*>(kmalloc(size));
    m_owned = true;
}

inline ByteBufferImpl::ByteBufferImpl(const void* data, size_t size, ConstructionMode mode)
    : m_size(size)
{
    ASSERT(mode == Copy);
    m_data = static_cast<u8*>(kmalloc(size));
    memcpy(m_data, data, size);
    m_owned = true;
}

inline ByteBufferImpl::ByteBufferImpl(void* data, size_t size, ConstructionMode mode)
    : m_data(static_cast<u8*>(data))
    , m_size(size)
{
    if (mode == Adopt) {
        m_owned = true;
    } else if (mode == Wrap) {
        m_owned = false;
    }
}

inline void ByteBufferImpl::grow(size_t size)
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

inline NonnullRefPtr<ByteBufferImpl> ByteBufferImpl::create_uninitialized(size_t size)
{
    return ::adopt(*new ByteBufferImpl(size));
}

inline NonnullRefPtr<ByteBufferImpl> ByteBufferImpl::create_zeroed(size_t size)
{
    auto buffer = ::adopt(*new ByteBufferImpl(size));
    memset(buffer->data(), 0, size);
    return buffer;
}

inline NonnullRefPtr<ByteBufferImpl> ByteBufferImpl::copy(const void* data, size_t size)
{
    return ::adopt(*new ByteBufferImpl(data, size, Copy));
}

inline NonnullRefPtr<ByteBufferImpl> ByteBufferImpl::wrap(void* data, size_t size)
{
    return ::adopt(*new ByteBufferImpl(data, size, Wrap));
}

inline NonnullRefPtr<ByteBufferImpl> ByteBufferImpl::wrap(const void* data, size_t size)
{
    return ::adopt(*new ByteBufferImpl(const_cast<void*>(data), size, Wrap));
}

inline NonnullRefPtr<ByteBufferImpl> ByteBufferImpl::adopt(void* data, size_t size)
{
    return ::adopt(*new ByteBufferImpl(data, size, Adopt));
}

inline const LogStream& operator<<(const LogStream& stream, const ByteBuffer& value)
{
    stream.write((const char*)value.data(), value.size());
    return stream;
}

}

using AK::ByteBuffer;
