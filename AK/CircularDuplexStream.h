/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CircularQueue.h>
#include <AK/Stream.h>

namespace AK {

// FIXME: There are a lot of raw loops here, that's not necessary an issue but it
//        has to be verified that the optimizer is able to insert memcpy instead.
template<size_t Capacity>
class CircularDuplexStream : public AK::DuplexStream {
public:
    size_t write(ReadonlyBytes bytes) override
    {
        const auto nwritten = min(bytes.size(), Capacity - m_queue.size());

        for (size_t idx = 0; idx < nwritten; ++idx)
            m_queue.enqueue(bytes[idx]);

        m_total_written += nwritten;
        return nwritten;
    }

    bool write_or_error(ReadonlyBytes bytes) override
    {
        if (Capacity - m_queue.size() < bytes.size()) {
            set_recoverable_error();
            return false;
        }

        const auto nwritten = write(bytes);
        VERIFY(nwritten == bytes.size());
        return true;
    }

    size_t read(Bytes bytes) override
    {
        if (has_any_error())
            return 0;

        const auto nread = min(bytes.size(), m_queue.size());

        for (size_t idx = 0; idx < nread; ++idx)
            bytes[idx] = m_queue.dequeue();

        return nread;
    }

    size_t read(Bytes bytes, size_t seekback)
    {
        if (seekback > Capacity || seekback > m_total_written) {
            set_recoverable_error();
            return 0;
        }

        const auto nread = min(bytes.size(), seekback);

        for (size_t idx = 0; idx < nread; ++idx) {
            const auto index = (m_total_written - seekback + idx) % Capacity;
            bytes[idx] = m_queue.m_storage[index];
        }

        return nread;
    }

    bool read_or_error(Bytes bytes) override
    {
        if (m_queue.size() < bytes.size()) {
            set_recoverable_error();
            return false;
        }

        read(bytes);
        return true;
    }

    bool discard_or_error(size_t count) override
    {
        if (m_queue.size() < count) {
            set_recoverable_error();
            return false;
        }

        for (size_t idx = 0; idx < count; ++idx)
            m_queue.dequeue();

        return true;
    }

    bool unreliable_eof() const override { return eof(); }
    bool eof() const { return m_queue.size() == 0; }

    size_t remaining_contiguous_space() const
    {
        return min(Capacity - m_queue.size(), m_queue.capacity() - (m_queue.head_index() + m_queue.size()) % Capacity);
    }

    Bytes reserve_contiguous_space(size_t count)
    {
        VERIFY(count <= remaining_contiguous_space());

        Bytes bytes { m_queue.m_storage + (m_queue.head_index() + m_queue.size()) % Capacity, count };

        m_queue.m_size += count;
        m_total_written += count;

        return bytes;
    }

private:
    CircularQueue<u8, Capacity> m_queue;
    size_t m_total_written { 0 };
};

}

using AK::CircularDuplexStream;
