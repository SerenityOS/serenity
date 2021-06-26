/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BitStream.h"

namespace Video::VP9 {

u8 BitStream::read_byte()
{
    VERIFY(m_bytes_remaining >= 1);
    m_bytes_remaining--;
    return *(m_data_ptr++);
}

bool BitStream::read_bit()
{
    if (!m_current_byte.has_value()) {
        m_current_byte = read_byte();
        m_current_bit_position = 7;
    }

    bool bit_value = m_current_byte.value() & (1u << m_current_bit_position);
    if (--m_current_bit_position < 0)
        m_current_byte.clear();
    return bit_value;
}

u8 BitStream::read_f(size_t n)
{
    u8 result = 0;
    for (size_t i = 0; i < n; i++) {
        result = (2 * result) + read_bit();
    }
    return result;
}

u8 BitStream::read_f8()
{
    if (!m_current_byte.has_value())
        return read_byte();

    auto high_bits = m_current_byte.value() & ((1u << m_current_bit_position) - 1);
    u8 remaining_bits = 7 - m_current_bit_position;
    m_current_byte = read_byte();
    m_current_bit_position = 7;
    auto low_bits = (m_current_byte.value() >> (8u - remaining_bits)) & ((1u << remaining_bits) - 1);
    m_current_bit_position -= remaining_bits;
    return (high_bits << remaining_bits) | low_bits;
}

u16 BitStream::read_f16()
{
    return (read_f8() << 8u) | read_f8();
}

/* 9.2.1 */
bool BitStream::init_bool(size_t bytes)
{
    if (bytes < 1)
        return false;
    m_bool_value = read_f8();
    m_bool_range = 255;
    m_bool_max_bits = (8 * bytes) - 8;
    return !read_bool(128);
}

/* 9.2.2 */
bool BitStream::read_bool(u8 probability)
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
            new_bit = read_bit();
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
bool BitStream::exit_bool()
{
    // FIXME: I'm not sure if this call to min is spec compliant, or if there is an issue elsewhere earlier in the parser.
    auto padding_element = read_f(min(m_bool_max_bits, (u64)bits_remaining()));

    // FIXME: It is a requirement of bitstream conformance that enough padding bits are inserted to ensure that the final coded byte of a frame is not equal to a superframe marker.
    //  A byte b is equal to a superframe marker if and only if (b & 0xe0)is equal to 0xc0, i.e. if the most significant 3 bits are equal to 0b110.
    return padding_element == 0;
}

u8 BitStream::read_literal(size_t n)
{
    u8 return_value = 0;
    for (size_t i = 0; i < n; i++) {
        return_value = (2 * return_value) + read_bool(128);
    }
    return return_value;
}

i8 BitStream::read_s(size_t n)
{
    auto value = read_f(n);
    auto sign = read_bit();
    return sign ? -value : value;
}

u64 BitStream::get_position()
{
    return (m_bytes_read * 8) + (7 - m_current_bit_position);
}

size_t BitStream::bytes_remaining()
{
    return m_bytes_remaining;
}

size_t BitStream::bits_remaining()
{
    return (bytes_remaining() * 8) + m_current_bit_position + 1;
}

}
