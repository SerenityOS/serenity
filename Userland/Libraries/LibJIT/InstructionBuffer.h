/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>

namespace JIT {

TYPEDEF_DISTINCT_NUMERIC_GENERAL(size_t, true, true, false, false, false, true, JITLabel);
TYPEDEF_DISTINCT_NUMERIC_GENERAL(size_t, true, true, false, false, false, true, JITPatchLocation);

class InstructionBuffer {
public:
    InstructionBuffer() = delete;
    InstructionBuffer(String&& region_name, size_t num_pages = 4);
    ~InstructionBuffer();

    void finalize();
    void append_bytes(Bytes data);
    void append_bytes(Vector<u8> data) { append_bytes(Bytes(data.data(), data.size())); }
    template<typename T>
    void write_le(JITPatchLocation offset, T value)
    {
        for (size_t i = 0; i < sizeof(T); ++i) {
            m_memory_region[offset.value() + i] = value & 0xff;
            value >>= 8;
        }
    }
    template<typename T>
    void append_le(T value)
    {
        ensure_tail_capacity(sizeof(T));
        write_le(m_used_space, value);
        m_used_space += sizeof(value);
    }
    JITLabel get_current_offset() const { return m_used_space; }
    JITPatchLocation get_relative_patch_location(size_t offset) const { return m_used_space + offset; }
    void* offset_to_address(JITLabel offset)
    {
        m_can_grow = false;
        return &m_memory_region[offset.value()];
    }
    void enter_at_offset(JITLabel offset) const;
    void dump_encoded_instructions();

private:
    void apply_relocations();
    void ensure_tail_capacity(size_t extra_space);
    void grow();

    String m_region_name;
    u8* m_memory_region {};
    size_t m_region_size {};
    size_t m_used_space {};
    bool m_region_is_executable { false };
    bool m_can_grow { true };
};

}
