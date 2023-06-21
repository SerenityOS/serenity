/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <AK/StdLibExtraDetails.h>

namespace AK {

class BinaryBufferWriter {
public:
    BinaryBufferWriter(Bytes target)
        : m_target(target)
    {
    }

    template<typename T>
    requires(IsTriviallyConstructible<T>) T& append_structure()
    {
        VERIFY((reinterpret_cast<FlatPtr>(m_target.data()) + m_offset) % alignof(T) == 0);
        VERIFY(m_offset + sizeof(T) <= m_target.size());
        T* allocated = new (m_target.data() + m_offset) T;
        m_offset += sizeof(T);
        return *allocated;
    }

    void skip_bytes(size_t num_bytes)
    {
        VERIFY(m_offset + num_bytes <= m_target.size());
        m_offset += num_bytes;
    }

    [[nodiscard]] size_t current_offset() const
    {
        return m_offset;
    }

private:
    Bytes m_target;
    size_t m_offset { 0 };
};

}
