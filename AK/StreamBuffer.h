/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Coroutine.h>
#include <AK/Error.h>

namespace AK {

class StreamBuffer {
    AK_MAKE_NONCOPYABLE(StreamBuffer);

public:
    StreamBuffer()
    {
        m_capacity = min_capacity;
        m_data = reinterpret_cast<u8*>(kmalloc(m_capacity));
    }

    StreamBuffer(StreamBuffer&& other)
        : m_read_head(exchange(other.m_read_head, 0))
        , m_peek_head(exchange(other.m_peek_head, 0))
        , m_capacity(exchange(other.m_capacity, 0))
        , m_data(exchange(other.m_data, nullptr))
    {
    }

    StreamBuffer& operator=(StreamBuffer&& buffer)
    {
        if (this != &buffer) {
            this->~StreamBuffer();
            new (this) StreamBuffer(move(buffer));
        }
        return *this;
    }

    ~StreamBuffer()
    {
        if (m_data)
            kfree_sized(m_data, m_capacity);
    }

    bool is_empty() const
    {
        return m_read_head == m_peek_head;
    }

    ReadonlyBytes data() const
    {
        return { m_data + m_read_head, m_peek_head - m_read_head };
    }

    void dequeue(size_t bytes)
    {
        m_read_head += bytes;
    }

    template<typename Func>
    Coroutine<ErrorOr<size_t>> enqueue(size_t preferred_capacity_for_writing, Func&& func)
    {
        allocate_enough_space_for(preferred_capacity_for_writing);
        size_t nread = CO_TRY(co_await func(Bytes { m_data + m_peek_head, m_capacity - m_peek_head }));
        m_peek_head += nread;
        co_return nread;
    }

    void append(ReadonlyBytes bytes)
    {
        if (m_peek_head + bytes.size() > m_capacity)
            allocate_enough_space_for(bytes.size());
        memcpy(m_data + m_peek_head, bytes.data(), bytes.size());
        m_peek_head += bytes.size();
    }

    void append(u8 byte)
    {
        if (m_peek_head == m_capacity)
            allocate_enough_space_for(1);
        m_data[m_peek_head++] = byte;
    }

    Bytes get_bytes_for_writing(size_t length)
    {
        if (m_peek_head + length > m_capacity)
            allocate_enough_space_for(length);
        m_peek_head += length;
        return { m_data + m_peek_head - length, length };
    }

private:
    static constexpr size_t min_capacity = 32;

    void allocate_enough_space_for(size_t length)
    {
        if (m_read_head != 0) {
            if (m_capacity - (m_peek_head - m_read_head) >= length) {
                memmove(m_data, m_data + m_read_head, m_peek_head - m_read_head);
                m_peek_head -= m_read_head;
                m_read_head = 0;
                return;
            }
        }

        VERIFY(m_capacity < NumericLimits<size_t>::max() / 3);
        size_t new_capacity = max(m_capacity * 3 / 2, m_capacity + length);

        u8* new_data = (u8*)kmalloc(new_capacity);
        memcpy(new_data, m_data + m_read_head, m_peek_head - m_read_head);
        kfree_sized(m_data, m_capacity);

        m_data = new_data;
        m_capacity = new_capacity;
        m_peek_head -= m_read_head;
        m_read_head = 0;
    }

    size_t m_read_head { 0 };
    size_t m_peek_head { 0 };
    size_t m_capacity { 0 };
    u8* m_data { nullptr };
};

}

#ifdef USING_AK_GLOBALLY
using AK::StreamBuffer;
#endif
