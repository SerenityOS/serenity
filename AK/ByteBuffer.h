/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

    [[nodiscard]] static ByteBuffer create_uninitialized(size_t size) { return ByteBuffer(ByteBufferImpl::create_uninitialized(size)); }
    [[nodiscard]] static ByteBuffer create_zeroed(size_t size) { return ByteBuffer(ByteBufferImpl::create_zeroed(size)); }
    [[nodiscard]] static ByteBuffer copy(const void* data, size_t size) { return ByteBuffer(ByteBufferImpl::copy(data, size)); }
    [[nodiscard]] static ByteBuffer copy(ReadonlyBytes bytes) { return ByteBuffer(ByteBufferImpl::copy(bytes.data(), bytes.size())); }

    ~ByteBuffer() { clear(); }
    void clear() { m_impl = nullptr; }

    operator bool() const { return !is_null(); }
    bool operator!() const { return is_null(); }
    [[nodiscard]] bool is_null() const { return m_impl == nullptr; }

    // Disable default implementations that would use surprising integer promotion.
    bool operator==(const ByteBuffer& other) const;
    bool operator!=(const ByteBuffer& other) const { return !(*this == other); }
    bool operator<=(const ByteBuffer& other) const = delete;
    bool operator>=(const ByteBuffer& other) const = delete;
    bool operator<(const ByteBuffer& other) const = delete;
    bool operator>(const ByteBuffer& other) const = delete;

    [[nodiscard]] u8& operator[](size_t i)
    {
        VERIFY(m_impl);
        return (*m_impl)[i];
    }
    [[nodiscard]] u8 operator[](size_t i) const
    {
        VERIFY(m_impl);
        return (*m_impl)[i];
    }
    [[nodiscard]] bool is_empty() const { return !m_impl || m_impl->is_empty(); }
    [[nodiscard]] size_t size() const { return m_impl ? m_impl->size() : 0; }

    [[nodiscard]] u8* data() { return m_impl ? m_impl->data() : nullptr; }
    [[nodiscard]] const u8* data() const { return m_impl ? m_impl->data() : nullptr; }

    [[nodiscard]] Bytes bytes()
    {
        if (m_impl) {
            return m_impl->bytes();
        }
        return {};
    }
    [[nodiscard]] ReadonlyBytes bytes() const
    {
        if (m_impl) {
            return m_impl->bytes();
        }
        return {};
    }

    [[nodiscard]] Span<u8> span()
    {
        if (m_impl) {
            return m_impl->span();
        }
        return {};
    }
    [[nodiscard]] Span<const u8> span() const
    {
        if (m_impl) {
            return m_impl->span();
        }
        return {};
    }

    [[nodiscard]] u8* offset_pointer(int offset) { return m_impl ? m_impl->offset_pointer(offset) : nullptr; }
    [[nodiscard]] const u8* offset_pointer(int offset) const { return m_impl ? m_impl->offset_pointer(offset) : nullptr; }

    [[nodiscard]] void* end_pointer() { return m_impl ? m_impl->end_pointer() : nullptr; }
    [[nodiscard]] const void* end_pointer() const { return m_impl ? m_impl->end_pointer() : nullptr; }

    [[nodiscard]] ByteBuffer isolated_copy() const
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

    [[nodiscard]] ByteBuffer slice(size_t offset, size_t size) const
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
    return ::adopt_ref(*new ByteBufferImpl(size));
}

inline NonnullRefPtr<ByteBufferImpl> ByteBufferImpl::create_zeroed(size_t size)
{
    auto buffer = ::adopt_ref(*new ByteBufferImpl(size));
    if (size != 0)
        __builtin_memset(buffer->data(), 0, size);
    return buffer;
}

inline NonnullRefPtr<ByteBufferImpl> ByteBufferImpl::copy(const void* data, size_t size)
{
    return ::adopt_ref(*new ByteBufferImpl(data, size));
}

}

using AK::ByteBuffer;
