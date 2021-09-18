/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <AK/Utf8View.h>

namespace AK {

Utf8CodePointIterator Utf8View::iterator_at_byte_offset(size_t byte_offset) const
{
    size_t current_offset = 0;
    for (auto iterator = begin(); !iterator.done(); ++iterator) {
        if (current_offset >= byte_offset)
            return iterator;
        current_offset += iterator.underlying_code_point_length_in_bytes();
    }
    return end();
}

size_t Utf8View::byte_offset_of(const Utf8CodePointIterator& it) const
{
    VERIFY(it.m_ptr >= begin_ptr());
    VERIFY(it.m_ptr <= end_ptr());

    return it.m_ptr - begin_ptr();
}

size_t Utf8View::byte_offset_of(size_t code_point_offset) const
{
    size_t byte_offset = 0;

    for (auto it = begin(); !it.done(); ++it) {
        if (code_point_offset == 0)
            return byte_offset;

        byte_offset += it.underlying_code_point_length_in_bytes();
        --code_point_offset;
    }

    return byte_offset;
}

Utf8View Utf8View::unicode_substring_view(size_t code_point_offset, size_t code_point_length) const
{
    if (code_point_length == 0)
        return {};

    size_t code_point_index = 0, offset_in_bytes = 0;
    for (auto iterator = begin(); !iterator.done(); ++iterator) {
        if (code_point_index == code_point_offset)
            offset_in_bytes = byte_offset_of(iterator);
        if (code_point_index == code_point_offset + code_point_length - 1) {
            size_t length_in_bytes = byte_offset_of(++iterator) - offset_in_bytes;
            return substring_view(offset_in_bytes, length_in_bytes);
        }
        ++code_point_index;
    }

    VERIFY_NOT_REACHED();
}

static inline bool decode_first_byte(
    unsigned char byte,
    size_t& out_code_point_length_in_bytes,
    u32& out_value)
{
    if ((byte & 128) == 0) {
        out_value = byte;
        out_code_point_length_in_bytes = 1;
        return true;
    }
    if ((byte & 64) == 0) {
        return false;
    }
    if ((byte & 32) == 0) {
        out_value = byte & 31;
        out_code_point_length_in_bytes = 2;
        return true;
    }
    if ((byte & 16) == 0) {
        out_value = byte & 15;
        out_code_point_length_in_bytes = 3;
        return true;
    }
    if ((byte & 8) == 0) {
        out_value = byte & 7;
        out_code_point_length_in_bytes = 4;
        return true;
    }

    return false;
}

bool Utf8View::validate(size_t& valid_bytes) const
{
    valid_bytes = 0;
    for (auto ptr = begin_ptr(); ptr < end_ptr(); ptr++) {
        size_t code_point_length_in_bytes;
        u32 value;
        bool first_byte_makes_sense = decode_first_byte(*ptr, code_point_length_in_bytes, value);
        if (!first_byte_makes_sense)
            return false;

        for (size_t i = 1; i < code_point_length_in_bytes; i++) {
            ptr++;
            if (ptr >= end_ptr())
                return false;
            if (*ptr >> 6 != 2)
                return false;
        }

        valid_bytes += code_point_length_in_bytes;
    }

    return true;
}

size_t Utf8View::calculate_length() const
{
    size_t length = 0;
    for ([[maybe_unused]] auto code_point : *this) {
        ++length;
    }
    return length;
}

bool Utf8View::starts_with(const Utf8View& start) const
{
    if (start.is_empty())
        return true;
    if (is_empty())
        return false;
    if (start.length() > length())
        return false;
    if (begin_ptr() == start.begin_ptr())
        return true;

    for (auto k = begin(), l = start.begin(); l != start.end(); ++k, ++l) {
        if (*k != *l)
            return false;
    }
    return true;
}

bool Utf8View::contains(u32 needle) const
{
    for (u32 code_point : *this) {
        if (code_point == needle)
            return true;
    }
    return false;
}

Utf8View Utf8View::trim(const Utf8View& characters, TrimMode mode) const
{
    size_t substring_start = 0;
    size_t substring_length = byte_length();

    if (mode == TrimMode::Left || mode == TrimMode::Both) {
        for (auto code_point = begin(); code_point != end(); ++code_point) {
            if (substring_length == 0)
                return {};
            if (!characters.contains(*code_point))
                break;
            substring_start += code_point.underlying_code_point_length_in_bytes();
            substring_length -= code_point.underlying_code_point_length_in_bytes();
        }
    }

    if (mode == TrimMode::Right || mode == TrimMode::Both) {
        size_t seen_whitespace_length = 0;
        for (auto code_point = begin(); code_point != end(); ++code_point) {
            if (characters.contains(*code_point))
                seen_whitespace_length += code_point.underlying_code_point_length_in_bytes();
            else
                seen_whitespace_length = 0;
        }
        if (seen_whitespace_length >= substring_length)
            return {};
        substring_length -= seen_whitespace_length;
    }

    return substring_view(substring_start, substring_length);
}

Utf8CodePointIterator& Utf8CodePointIterator::operator++()
{
    VERIFY(m_length > 0);

    size_t code_point_length_in_bytes = underlying_code_point_length_in_bytes();
    if (code_point_length_in_bytes > m_length) {
        // We don't have enough data for the next code point. Skip one character and try again.
        // The rest of the code will output replacement characters as needed for any eventual extension bytes we might encounter afterwards.
        dbgln("Expected code point size {} is too big for the remaining length {}. Moving forward one byte.", code_point_length_in_bytes, m_length);
        m_ptr += 1;
        m_length -= 1;
        return *this;
    }

    m_ptr += code_point_length_in_bytes;
    m_length -= code_point_length_in_bytes;
    return *this;
}

size_t Utf8CodePointIterator::underlying_code_point_length_in_bytes() const
{
    VERIFY(m_length > 0);
    size_t code_point_length_in_bytes = 0;
    u32 value;
    bool first_byte_makes_sense = decode_first_byte(*m_ptr, code_point_length_in_bytes, value);

    // If any of these tests fail, we will output a replacement character for this byte and treat it as a code point of size 1.
    if (!first_byte_makes_sense)
        return 1;

    if (code_point_length_in_bytes > m_length)
        return 1;

    for (size_t offset = 1; offset < code_point_length_in_bytes; offset++) {
        if (m_ptr[offset] >> 6 != 2)
            return 1;
    }

    return code_point_length_in_bytes;
}

ReadonlyBytes Utf8CodePointIterator::underlying_code_point_bytes() const
{
    return { m_ptr, underlying_code_point_length_in_bytes() };
}

u32 Utf8CodePointIterator::operator*() const
{
    VERIFY(m_length > 0);

    u32 code_point_value_so_far = 0;
    size_t code_point_length_in_bytes = 0;

    bool first_byte_makes_sense = decode_first_byte(m_ptr[0], code_point_length_in_bytes, code_point_value_so_far);

    if (!first_byte_makes_sense) {
        // The first byte of the code point doesn't make sense: output a replacement character
        dbgln("First byte doesn't make sense: {:#02x}.", m_ptr[0]);
        return 0xFFFD;
    }

    if (code_point_length_in_bytes > m_length) {
        // There is not enough data left for the full code point: output a replacement character
        dbgln("Not enough bytes (need {}, have {}), first byte is: {:#02x}.", code_point_length_in_bytes, m_length, m_ptr[0]);
        return 0xFFFD;
    }

    for (size_t offset = 1; offset < code_point_length_in_bytes; offset++) {
        if (m_ptr[offset] >> 6 != 2) {
            // One of the extension bytes of the code point doesn't make sense: output a replacement character
            dbgln("Extension byte {:#02x} in {} position after first byte {:#02x} doesn't make sense.", m_ptr[offset], offset, m_ptr[0]);
            return 0xFFFD;
        }

        code_point_value_so_far <<= 6;
        code_point_value_so_far |= m_ptr[offset] & 63;
    }

    return code_point_value_so_far;
}

Optional<u32> Utf8CodePointIterator::peek(size_t offset) const
{
    if (offset == 0) {
        if (this->done())
            return {};
        return this->operator*();
    }

    auto new_iterator = *this;
    for (size_t index = 0; index < offset; ++index) {
        ++new_iterator;
        if (new_iterator.done())
            return {};
    }
    return *new_iterator;
}

}
