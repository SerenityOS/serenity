/*
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BootPPMParser.h"

namespace Prekernel {

BootPPMParser::BootPPMParser(u8 const* buffer, u32 buffer_size)
{
    m_cursor = reinterpret_cast<char const*>(buffer);
    m_buffer_end = m_cursor + buffer_size;
}

bool BootPPMParser::parse()
{
    if (!check_position()) {
        return false;
    }
    if (!parse_magic()) {
        return false;
    }
    if (!parse_new_line()) {
        return false;
    }
    if (!parse_comment()) {
        return false;
    }
    if (!parse_integer(image.width)) {
        return false;
    }
    if (!parse_integer(image.height)) {
        return false;
    }
    u32 max_color_value;
    if (!parse_integer(max_color_value) || max_color_value != 255) {
        return false;
    }

    image.pixel_data = reinterpret_cast<u8 const*>(m_cursor);

    return true;
}

bool BootPPMParser::check_position() const
{
    if (m_cursor >= m_buffer_end) {
        return false;
    }
    return true;
}

bool BootPPMParser::parse_magic()
{
    if (m_cursor[0] != 'P' || m_cursor[1] != '6') {
        return false;
    }
    m_cursor += 2;

    return check_position();
}

bool BootPPMParser::parse_new_line()
{
    if (*m_cursor != '\n') {
        return false;
    }
    ++m_cursor;

    return check_position();
}

bool BootPPMParser::parse_comment()
{
    if (*m_cursor == '#') {
        // Skip to the next new line character
        while (check_position() && *m_cursor != '\n') {
            ++m_cursor;
        }
        ++m_cursor;
    }

    return check_position();
}

bool BootPPMParser::parse_integer(u32& value)
{
    auto begin = m_cursor;
    while (check_position() && *m_cursor != ' ' && *m_cursor != '\n') {
        ++m_cursor;
    }
    auto end = m_cursor;
    ++m_cursor;

    if (!check_position()) {
        return false;
    }

    value = 0;
    u32 multiplier = 1;
    while (--end >= begin) {
        value += multiplier * (*end - '0');
        multiplier *= 10;
    }

    return true;
}

}
