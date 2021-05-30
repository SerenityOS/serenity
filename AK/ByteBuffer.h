/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <AK/Types.h>
#include <AK/kmalloc.h>

namespace AK {
namespace Detail {

template<size_t inline_capacity>
class ByteBuffer {
public:
    ByteBuffer() = default;

    ~ByteBuffer()
    {
        clear();
    }

    ByteBuffer(ByteBuffer const& other)
    {
        resize(other.size());
        VERIFY(m_size == other.size());
        __builtin_memcpy(data(), other.data(), other.size());
    }

    ByteBuffer(ByteBuffer&& other)
    {
        move_from(move(other));
    }

    ByteBuffer& operator=(ByteBuffer&& other)
    {
        if (this != &other) {
            if (!m_inline)
                kfree(m_outline_buffer);
            move_from(move(other));
        }
        return *this;
    }

    ByteBuffer& operator=(ByteBuffer const& other)
    {
        if (this != &other) {
            if (m_size > other.size())
                trim(other.size(), true);
            else
                resize(other.size());
            __builtin_memcpy(data(), other.data(), other.size());
        }
        return *this;
    }

    [[nodiscard]] static ByteBuffer create_uninitialized(size_t size)
    {
        return ByteBuffer(size);
    }

    [[nodiscard]] static ByteBuffer create_zeroed(size_t size)
    {
        auto buffer = create_uninitialized(size);
        buffer.zero_fill();
        VERIFY(size == 0 || (buffer[0] == 0 && buffer[size - 1] == 0));
        return buffer;
    }

    [[nodiscard]] static ByteBuffer copy(void const* data, size_t size)
    {
        auto buffer = create_uninitialized(size);
        if (size != 0)
            __builtin_memcpy(buffer.data(), data, size);
        return buffer;
    }

    [[nodiscard]] static ByteBuffer copy(ReadonlyBytes bytes)
    {
        return copy(bytes.data(), bytes.size());
    }

    template<size_t other_inline_capacity>
    bool operator==(ByteBuffer<other_inline_capacity> const& other) const
    {
        if (size() != other.size())
            return false;

        // So they both have data, and the same length.
        return !__builtin_memcmp(data(), other.data(), size());
    }

    bool operator!=(ByteBuffer const& other) const { return !(*this == other); }

    [[nodiscard]] u8& operator[](size_t i)
    {
        VERIFY(i < m_size);
        return data()[i];
    }

    [[nodiscard]] u8 const& operator[](size_t i) const
    {
        VERIFY(i < m_size);
        return data()[i];
    }

    [[nodiscard]] bool is_empty() const { return !m_size; }
    [[nodiscard]] size_t size() const { return m_size; }

    [[nodiscard]] u8* data() { return m_inline ? m_inline_buffer : m_outline_buffer; }
    [[nodiscard]] u8 const* data() const { return m_inline ? m_inline_buffer : m_outline_buffer; }

    [[nodiscard]] Bytes bytes() { return { data(), size() }; }
    [[nodiscard]] ReadonlyBytes bytes() const { return { data(), size() }; }

    [[nodiscard]] AK::Span<u8> span() { return { data(), size() }; }
    [[nodiscard]] AK::Span<const u8> span() const { return { data(), size() }; }

    [[nodiscard]] u8* offset_pointer(int offset) { return data() + offset; }
    [[nodiscard]] u8 const* offset_pointer(int offset) const { return data() + offset; }

    [[nodiscard]] void* end_pointer() { return data() + m_size; }
    [[nodiscard]] void const* end_pointer() const { return data() + m_size; }

    [[nodiscard]] ByteBuffer slice(size_t offset, size_t size) const
    {
        // I cannot hand you a slice I don't have
        VERIFY(offset + size <= this->size());

        return copy(offset_pointer(offset), size);
    }

    void clear()
    {
        if (!m_inline) {
            kfree(m_outline_buffer);
            m_inline = true;
        }
        m_size = 0;
    }

    ALWAYS_INLINE void resize(size_t new_size)
    {
        if (new_size <= m_size) {
            trim(new_size, false);
            return;
        }
        ensure_capacity(new_size);
        m_size = new_size;
    }

    ALWAYS_INLINE void ensure_capacity(size_t new_capacity)
    {
        if (new_capacity <= capacity())
            return;
        ensure_capacity_slowpath(new_capacity);
    }

    void append(void const* data, size_t data_size)
    {
        if (data_size == 0)
            return;
        VERIFY(data != nullptr);
        int old_size = size();
        resize(size() + data_size);
        __builtin_memcpy(this->data() + old_size, data, data_size);
    }

    void operator+=(ByteBuffer const& other)
    {
        append(other.data(), other.size());
    }

    void overwrite(size_t offset, void const* data, size_t data_size)
    {
        // make sure we're not told to write past the end
        VERIFY(offset + data_size <= size());
        __builtin_memcpy(this->data() + offset, data, data_size);
    }

    void zero_fill()
    {
        __builtin_memset(data(), 0, m_size);
    }

    operator Bytes() { return bytes(); }
    operator ReadonlyBytes() const { return bytes(); }

    ALWAYS_INLINE size_t capacity() const { return m_inline ? inline_capacity : m_outline_capacity; }

private:
    ByteBuffer(size_t size)
    {
        resize(size);
        VERIFY(m_size == size);
    }

    void move_from(ByteBuffer&& other)
    {
        m_size = other.m_size;
        m_inline = other.m_inline;
        if (!other.m_inline) {
            m_outline_buffer = other.m_outline_buffer;
            m_outline_capacity = other.m_outline_capacity;
        } else
            __builtin_memcpy(m_inline_buffer, other.m_inline_buffer, other.m_size);
        other.m_size = 0;
        other.m_inline = true;
    }

    void trim(size_t size, bool may_discard_existing_data)
    {
        VERIFY(size <= m_size);
        if (!m_inline && size <= inline_capacity)
            shrink_into_inline_buffer(size, may_discard_existing_data);
        m_size = size;
    }

    NEVER_INLINE void shrink_into_inline_buffer(size_t size, bool may_discard_existing_data)
    {
        // m_inline_buffer and m_outline_buffer are part of a union, so save the pointer
        auto outline_buffer = m_outline_buffer;
        if (!may_discard_existing_data)
            __builtin_memcpy(m_inline_buffer, outline_buffer, size);
        kfree(outline_buffer);
        m_inline = true;
    }

    NEVER_INLINE void ensure_capacity_slowpath(size_t new_capacity)
    {
        u8* new_buffer;
        new_capacity = kmalloc_good_size(new_capacity);
        if (!m_inline) {
            new_buffer = (u8*)krealloc(m_outline_buffer, new_capacity);
            VERIFY(new_buffer);
        } else {
            new_buffer = (u8*)kmalloc(new_capacity);
            VERIFY(new_buffer);
            __builtin_memcpy(new_buffer, data(), m_size);
        }
        m_outline_buffer = new_buffer;
        m_outline_capacity = new_capacity;
        m_inline = false;
    }

    union {
        u8 m_inline_buffer[inline_capacity];
        struct {
            u8* m_outline_buffer;
            size_t m_outline_capacity;
        };
    };
    size_t m_size { 0 };
    bool m_inline { true };
};

}
}
