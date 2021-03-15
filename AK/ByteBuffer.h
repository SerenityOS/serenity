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

#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Span.h>
#include <AK/Types.h>
#include <AK/kmalloc.h>

namespace AK {

class ByteBufferImpl : public RefCounted<ByteBufferImpl> {
public:
    static NonnullRefPtr<ByteBufferImpl> create_uninitialized(size_t size);
    static NonnullRefPtr<ByteBufferImpl> create_zeroed(size_t);
    static NonnullRefPtr<ByteBufferImpl> copy(const void*, size_t);

    ByteBufferImpl() = delete;
    ~ByteBufferImpl() { clear(); }

    void clear()
    {
        if (!m_data)
            return;
        kfree(m_data);
        m_data = nullptr;
    }

    u8& operator[](size_t i)
    {
        VERIFY(i < m_size);
        return m_data[i];
    }
    const u8& operator[](size_t i) const
    {
        VERIFY(i < m_size);
        return m_data[i];
    }
    bool is_empty() const { return !m_size; }
    size_t size() const { return m_size; }

    u8* data() { return m_data; }
    const u8* data() const { return m_data; }

    Bytes bytes() { return { data(), size() }; }
    ReadonlyBytes bytes() const { return { data(), size() }; }

    Span<u8> span() { return { data(), size() }; }
    Span<const u8> span() const { return { data(), size() }; }

    u8* offset_pointer(int offset) { return m_data + offset; }
    const u8* offset_pointer(int offset) const { return m_data + offset; }

    void* end_pointer() { return m_data + m_size; }
    const void* end_pointer() const { return m_data + m_size; }

    // NOTE: trim() does not reallocate.
    void trim(size_t size)
    {
        VERIFY(size <= m_size);
        m_size = size;
    }

    void grow(size_t size);

    void zero_fill();

private:
    explicit ByteBufferImpl(size_t);
    ByteBufferImpl(const void*, size_t);

    u8* m_data { nullptr };
    size_t m_size { 0 };
};

class ByteBuffer {
public:
    ByteBuffer() = default;
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
    static ByteBuffer copy(ReadonlyBytes bytes) { return ByteBuffer(ByteBufferImpl::copy(bytes.data(), bytes.size())); }

    ~ByteBuffer() { clear(); }
    void clear() { m_impl = nullptr; }

    operator bool() const { return !is_null(); }
    bool operator!() const { return is_null(); }
    bool is_null() const { return m_impl == nullptr; }

    // Disable default implementations that would use surprising integer promotion.
    bool operator==(const ByteBuffer& other) const;
    bool operator!=(const ByteBuffer& other) const { return !(*this == other); }
    bool operator<=(const ByteBuffer& other) const = delete;
    bool operator>=(const ByteBuffer& other) const = delete;
    bool operator<(const ByteBuffer& other) const = delete;
    bool operator>(const ByteBuffer& other) const = delete;

    u8& operator[](size_t i)
    {
        VERIFY(m_impl);
        return (*m_impl)[i];
    }
    u8 operator[](size_t i) const
    {
        VERIFY(m_impl);
        return (*m_impl)[i];
    }
    bool is_empty() const { return !m_impl || m_impl->is_empty(); }
    size_t size() const { return m_impl ? m_impl->size() : 0; }

    u8* data() { return m_impl ? m_impl->data() : nullptr; }
    const u8* data() const { return m_impl ? m_impl->data() : nullptr; }

    Bytes bytes()
    {
        if (m_impl) {
            return m_impl->bytes();
        }
        return {};
    }
    ReadonlyBytes bytes() const
    {
        if (m_impl) {
            return m_impl->bytes();
        }
        return {};
    }

    Span<u8> span()
    {
        if (m_impl) {
            return m_impl->span();
        }
        return {};
    }
    Span<const u8> span() const
    {
        if (m_impl) {
            return m_impl->span();
        }
        return {};
    }

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

    ByteBuffer slice(size_t offset, size_t size) const
    {
        if (is_null())
            return {};

        if (offset == 0 && size == this->size())
            return *this;

        // I cannot hand you a slice I don't have
        VERIFY(offset + size <= this->size());

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
        if (data_size == 0)
            return;
        VERIFY(data != nullptr);
        int old_size = size();
        grow(size() + data_size);
        __builtin_memcpy(this->data() + old_size, data, data_size);
    }

    void operator+=(const ByteBuffer& other)
    {
        append(other.data(), other.size());
    }

    void overwrite(size_t offset, const void* data, size_t data_size)
    {
        // make sure we're not told to write past the end
        VERIFY(offset + data_size <= size());
        __builtin_memcpy(this->data() + offset, data, data_size);
    }

    void zero_fill()
    {
        m_impl->zero_fill();
    }

    operator Bytes() { return bytes(); }
    operator ReadonlyBytes() const { return bytes(); }

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
    if (size != 0)
        m_data = static_cast<u8*>(kmalloc(size));
}

inline ByteBufferImpl::ByteBufferImpl(const void* data, size_t size)
    : m_size(size)
{
    if (size != 0) {
        m_data = static_cast<u8*>(kmalloc(size));
        __builtin_memcpy(m_data, data, size);
    }
}

inline void ByteBufferImpl::grow(size_t size)
{
    VERIFY(size > m_size);
    if (size == 0) {
        if (m_data)
            kfree(m_data);
        m_data = nullptr;
        m_size = 0;
        return;
    }
    u8* new_data = static_cast<u8*>(kmalloc(size));
    __builtin_memcpy(new_data, m_data, m_size);
    u8* old_data = m_data;
    m_data = new_data;
    m_size = size;
    if (old_data)
        kfree(old_data);
}

inline void ByteBufferImpl::zero_fill()
{
    __builtin_memset(m_data, 0, m_size);
}

inline NonnullRefPtr<ByteBufferImpl> ByteBufferImpl::create_uninitialized(size_t size)
{
    return ::adopt(*new ByteBufferImpl(size));
}

inline NonnullRefPtr<ByteBufferImpl> ByteBufferImpl::create_zeroed(size_t size)
{
    auto buffer = ::adopt(*new ByteBufferImpl(size));
    if (size != 0)
        __builtin_memset(buffer->data(), 0, size);
    return buffer;
}

inline NonnullRefPtr<ByteBufferImpl> ByteBufferImpl::copy(const void* data, size_t size)
{
    return ::adopt(*new ByteBufferImpl(data, size));
}

}

using AK::ByteBuffer;
