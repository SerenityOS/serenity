/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Coroutine.h>
#include <AK/Error.h>
#include <AK/FixedArray.h>

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

class StreamSeekbackBuffer {
public:
    StreamSeekbackBuffer(size_t max_seekback_distance, size_t max_back_reference_length, double optimization_factor = 2.0)
        : m_seekback(MUST(FixedArray<u8>::create((max_seekback_distance + max_back_reference_length) * optimization_factor)))
        , m_max_seekback_distance(max_seekback_distance)
    {
    }

    ReadonlyBytes data() const
    {
        return m_buffer.data();
    }

    void dequeue(size_t bytes)
    {
        m_buffer.dequeue(bytes);
    }

    void write(ReadonlyBytes bytes)
    {
        m_buffer.append(bytes);
        write_to_seekback(bytes);
    }

    void write(u8 byte)
    {
        m_buffer.append(byte);
        write_to_seekback(byte);
    }

    void copy_from_seekback(size_t distance, size_t length)
    {
        VERIFY(distance <= max_seekback_distance());

        auto buffer_bytes = m_buffer.get_bytes_for_writing(length);

        while (length > 0) {
            auto write = [&](ReadonlyBytes bytes) {
                write_to_seekback(bytes, false);
                buffer_bytes = buffer_bytes.slice(bytes.copy_to(buffer_bytes));
            };

            size_t to_copy = min(distance, length);
            if (distance <= m_head) {
                write(m_seekback.span().slice(m_head - distance, to_copy));
            } else if (distance - to_copy > m_head) {
                write(m_seekback.span().slice(m_seekback.size() - (distance - m_head), to_copy));
            } else {
                auto first_part = m_seekback.span().slice_from_end(distance - m_head);
                auto second_part = m_seekback.span().slice(0, to_copy - distance + m_head);
                write(first_part);
                write(second_part);
            }

            distance += to_copy;
            length -= to_copy;
        }

        VERIFY(buffer_bytes.is_empty());
    }

    size_t max_seekback_distance() const
    {
        return min(m_max_seekback_distance, m_seekback_length);
    }

private:
    void write_to_seekback(ReadonlyBytes bytes, bool may_discard_prefix = true)
    {
        if (may_discard_prefix && bytes.size() > m_max_seekback_distance) {
            bytes = bytes.slice_from_end(m_max_seekback_distance);
            m_head = 0;
        }

        if (m_head + bytes.size() > m_seekback.size()) {
            size_t first_part_size = m_seekback.size() - m_head;
            size_t new_head = bytes.size() - first_part_size;

            memcpy(m_seekback.data() + m_head, bytes.data(), first_part_size);
            memcpy(m_seekback.data(), bytes.slice(first_part_size).data(), new_head);
            m_head = new_head;
        } else {
            memcpy(m_seekback.data() + m_head, bytes.data(), bytes.size());
            m_head += bytes.size();
            if (m_head == m_seekback.size())
                m_head = 0;
        }
        m_seekback_length += bytes.size();
    }

    void write_to_seekback(u8 byte)
    {
        m_seekback[m_head] = byte;
        m_head = (m_head + 1 == m_seekback.size() ? 0 : m_head + 1);
        ++m_seekback_length;
    }

    StreamBuffer m_buffer;
    FixedArray<u8> m_seekback;
    size_t m_head { 0 };
    u64 m_seekback_length { 0 };
    size_t m_max_seekback_distance { 0 };
};

}

#ifdef USING_AK_GLOBALLY
using AK::StreamBuffer;
using AK::StreamSeekbackBuffer;
#endif
