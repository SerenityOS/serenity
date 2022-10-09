/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Optional.h>
#include <AK/Types.h>

namespace Video::VP9 {

class BitStream {
public:
    BitStream(u8 const* data, size_t size)
        : m_data_ptr(data)
        , m_end_ptr(data + size)
    {
    }

    ErrorOr<bool> read_bit();

    ErrorOr<u64> read_bits(u8 bit_count);
    /* (9.1) */
    ErrorOr<u8> read_f8();
    ErrorOr<u16> read_f16();

    /* (9.2) */
    ErrorOr<void> init_bool(size_t bytes);
    ErrorOr<bool> read_bool(u8 probability);
    ErrorOr<void> exit_bool();
    ErrorOr<u8> read_literal(u8 bit_count);
    size_t range_coding_bits_remaining();

    /* (4.9.2) */
    ErrorOr<i8> read_s(size_t n);

    u64 get_position();
    size_t bytes_remaining();
    size_t bits_remaining();

private:
    ErrorOr<void> fill_reservoir();

    u8 const* m_data_ptr { nullptr };
    u8 const* m_end_ptr { nullptr };
    u64 m_reservoir;
    u8 m_reservoir_bits_remaining { 0 };
    size_t m_bits_read { 0 };

    u8 m_bool_value { 0 };
    u8 m_bool_range { 0 };
    u64 m_bool_max_bits { 0 };
};

}
