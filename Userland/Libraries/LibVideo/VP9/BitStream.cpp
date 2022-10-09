/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BitStream.h"

namespace Video::VP9 {

ErrorOr<void> BitStream::fill_reservoir()
{
    VERIFY(m_reservoir_bits_remaining == 0);
    if (m_data_ptr == m_end_ptr)
        return Error::from_string_literal("Stream is out of data");
    VERIFY(m_data_ptr < m_end_ptr);
    m_reservoir = 0;

    size_t byte_index;
    for (byte_index = 0; m_data_ptr < m_end_ptr && byte_index < sizeof(m_reservoir); byte_index++) {
        m_reservoir = (m_reservoir << 8) | *m_data_ptr;
        m_data_ptr++;
    }

    m_reservoir_bits_remaining = byte_index * 8;
    m_reservoir <<= (sizeof(m_reservoir) - byte_index) * 8;
    return {};
}

ErrorOr<u64> BitStream::read_bits(u8 bit_count)
{
    if (bit_count > sizeof(u64) * 8)
        return Error::from_string_literal("Requested read is too large");
    u64 result = 0;

    while (bit_count > 0) {
        if (m_reservoir_bits_remaining == 0)
            TRY(fill_reservoir());

        u64 batch_bits = min(bit_count, m_reservoir_bits_remaining);
        u64 bit_shift = (sizeof(m_reservoir) * 8u) - batch_bits;

        result = (result << batch_bits) | m_reservoir >> bit_shift;
        m_reservoir <<= batch_bits;
        bit_count -= batch_bits;
        m_reservoir_bits_remaining -= batch_bits;
        m_bits_read += batch_bits;
    }

    return result;
}

ErrorOr<bool> BitStream::read_bit()
{
    auto value = TRY(read_bits(1));
    VERIFY(value <= 2);
    return value != 0;
}

ErrorOr<u8> BitStream::read_f8()
{
    return TRY(read_bits(8));
}

ErrorOr<u16> BitStream::read_f16()
{
    return TRY(read_bits(16));
}

/* 9.2.1 */
ErrorOr<void> BitStream::init_bool(size_t bytes)
{
    if (bytes > bytes_remaining())
        return Error::from_string_literal("Available data is too small for range decoder");
    m_bool_value = TRY(read_f8());
    m_bool_range = 255;
    m_bool_max_bits = (8 * bytes) - 8;
    if (TRY(read_bool(128)))
        return Error::from_string_literal("Range decoder marker was non-zero");
    return {};
}

/* 9.2.2 */
ErrorOr<bool> BitStream::read_bool(u8 probability)
{
    auto split = 1u + (((m_bool_range - 1u) * probability) >> 8u);
    bool return_bool;

    if (m_bool_value < split) {
        m_bool_range = split;
        return_bool = false;
    } else {
        m_bool_range -= split;
        m_bool_value -= split;
        return_bool = true;
    }

    while (m_bool_range < 128) {
        bool new_bit;
        if (m_bool_max_bits) {
            new_bit = TRY(read_bit());
            m_bool_max_bits--;
        } else {
            new_bit = false;
        }
        m_bool_range *= 2;
        m_bool_value = (m_bool_value << 1u) + new_bit;
    }

    return return_bool;
}

/* 9.2.3 */
ErrorOr<void> BitStream::exit_bool()
{
    while (m_bool_max_bits > 0) {
        auto padding_read_size = min(m_bool_max_bits, sizeof(m_reservoir) * 8);
        auto padding_bits = TRY(read_bits(padding_read_size));
        m_bool_max_bits -= padding_read_size;

        if (padding_bits != 0)
            return Error::from_string_literal("Range decoder has non-zero padding element");
    }

    // FIXME: It is a requirement of bitstream conformance that enough padding bits are inserted to ensure that the final coded byte of a frame is not equal to a superframe marker.
    //  A byte b is equal to a superframe marker if and only if (b & 0xe0)is equal to 0xc0, i.e. if the most significant 3 bits are equal to 0b110.
    return {};
}

size_t BitStream::range_coding_bits_remaining()
{
    return m_bool_max_bits;
}

ErrorOr<u8> BitStream::read_literal(u8 n)
{
    u8 return_value = 0;
    for (size_t i = 0; i < n; i++) {
        return_value = (2 * return_value) + TRY(read_bool(128));
    }
    return return_value;
}

ErrorOr<i8> BitStream::read_s(size_t n)
{
    auto value = TRY(read_bits(n));
    auto sign = TRY(read_bit());
    return sign ? -value : value;
}

u64 BitStream::get_position()
{
    return m_bits_read;
}

size_t BitStream::bytes_remaining()
{
    return (m_end_ptr - m_data_ptr) + (m_reservoir_bits_remaining / 8);
}

size_t BitStream::bits_remaining()
{
    return ((m_end_ptr - m_data_ptr) * sizeof(m_data_ptr) * 8) + m_reservoir_bits_remaining;
}

}
