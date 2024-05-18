/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2024, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/Span.h>
#include <AK/Vector.h>

namespace Java {

class Reader {
public:
    explicit Reader(ReadonlyBytes bytes)
        : m_bytes(bytes)
    {
    }

    ALWAYS_INLINE ReadonlyBytes bytes() const { return m_bytes; }
    ALWAYS_INLINE size_t offset() const { return m_offset; }

    bool done() const
    {
        return offset() >= bytes().size();
    }

    size_t remaining() const
    {
        if (done())
            return 0;

        return bytes().size() - offset();
    }

    void move_by(ssize_t count)
    {
        m_offset += count;
    }

    template<typename T = u8>
    T read()
    {
        T value = reinterpret_cast<T const*>(m_bytes.offset(m_offset))[0];
        move_by(sizeof(T));
        return AK::convert_between_host_and_big_endian(value);
    }

    template<typename T = u8>
    void move_to(size_t offset)
    {
        VERIFY(offset <= m_bytes.size());
        m_offset = static_cast<ssize_t>(offset);
    }

private:
    ReadonlyBytes m_bytes;
    ssize_t m_offset { 0 };
};

}
