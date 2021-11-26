/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/Types.h>

namespace Video::VP9 {

class BitStream {
public:
    BitStream(u8 const* data, size_t size)
        : m_data_ptr(data)
        , m_bytes_remaining(size)
    {
    }

    u8 read_byte();
    bool read_bit();

    /* (9.1) */
    u8 read_f(size_t n);
    u8 read_f8();
    u16 read_f16();

    /* (9.2) */
    bool init_bool(size_t bytes);
    bool read_bool(u8 probability);
    bool exit_bool();
    u8 read_literal(size_t n);

    /* (4.9.2) */
    i8 read_s(size_t n);

    u64 get_position();
    size_t bytes_remaining();
    size_t bits_remaining();

private:
    u8 const* m_data_ptr { nullptr };
    size_t m_bytes_remaining { 0 };
    Optional<u8> m_current_byte;
    i8 m_current_bit_position { 0 };
    u64 m_bytes_read { 0 };

    u8 m_bool_value { 0 };
    u8 m_bool_range { 0 };
    u64 m_bool_max_bits { 0 };
};

}
