/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>

namespace AK {

class Utf8View;

class Utf8CodePointIterator {
    friend class Utf8View;

public:
    Utf8CodePointIterator() = default;
    ~Utf8CodePointIterator() = default;

    bool operator==(Utf8CodePointIterator const&) const = default;
    bool operator!=(Utf8CodePointIterator const&) const = default;
    Utf8CodePointIterator& operator++();
    u32 operator*() const;
    // NOTE: This returns {} if the peek is at or past EOF.
    Optional<u32> peek(size_t offset = 0) const;

    ssize_t operator-(const Utf8CodePointIterator& other) const
    {
        return m_ptr - other.m_ptr;
    }

    // Note : These methods return the information about the underlying UTF-8 bytes.
    // If the UTF-8 string encoding is not valid at the iterator's position, then the underlying bytes might be different from the
    // decoded character's re-encoded bytes (which will be an `0xFFFD REPLACEMENT CHARACTER` with an UTF-8 length of three bytes).
    // If your code relies on the decoded character being equivalent to the re-encoded character, use the `UTF8View::validate()`
    // method on the view prior to using its iterator.
    size_t underlying_code_point_length_in_bytes() const;
    ReadonlyBytes underlying_code_point_bytes() const;
    bool done() const { return m_length == 0; }

private:
    Utf8CodePointIterator(u8 const* ptr, size_t length)
        : m_ptr(ptr)
        , m_length(length)
    {
    }

    u8 const* m_ptr { nullptr };
    size_t m_length { 0 };
};

class Utf8View {
public:
    using Iterator = Utf8CodePointIterator;

    Utf8View() = default;

    explicit Utf8View(String& string)
        : m_string(string.view())
    {
    }

    explicit Utf8View(StringView string)
        : m_string(string)
    {
    }

    ~Utf8View() = default;

    explicit Utf8View(String&&) = delete;

    StringView as_string() const { return m_string; }

    Utf8CodePointIterator begin() const { return { begin_ptr(), m_string.length() }; }
    Utf8CodePointIterator end() const { return { end_ptr(), 0 }; }
    Utf8CodePointIterator iterator_at_byte_offset(size_t) const;

    const unsigned char* bytes() const { return begin_ptr(); }
    size_t byte_length() const { return m_string.length(); }
    size_t byte_offset_of(const Utf8CodePointIterator&) const;
    size_t byte_offset_of(size_t code_point_offset) const;

    Utf8View substring_view(size_t byte_offset, size_t byte_length) const { return Utf8View { m_string.substring_view(byte_offset, byte_length) }; }
    Utf8View substring_view(size_t byte_offset) const { return substring_view(byte_offset, byte_length() - byte_offset); }
    Utf8View unicode_substring_view(size_t code_point_offset, size_t code_point_length) const;
    Utf8View unicode_substring_view(size_t code_point_offset) const { return unicode_substring_view(code_point_offset, length() - code_point_offset); }

    bool is_empty() const { return m_string.is_empty(); }
    bool is_null() const { return m_string.is_null(); }
    bool starts_with(const Utf8View&) const;
    bool contains(u32) const;

    Utf8View trim(const Utf8View& characters, TrimMode mode = TrimMode::Both) const;

    size_t iterator_offset(const Utf8CodePointIterator& it) const
    {
        return byte_offset_of(it);
    }

    bool validate(size_t& valid_bytes) const;
    bool validate() const
    {
        size_t valid_bytes;
        return validate(valid_bytes);
    }

    size_t length() const
    {
        if (!m_have_length) {
            m_length = calculate_length();
            m_have_length = true;
        }
        return m_length;
    }

private:
    u8 const* begin_ptr() const { return (u8 const*)m_string.characters_without_null_termination(); }
    u8 const* end_ptr() const { return begin_ptr() + m_string.length(); }
    size_t calculate_length() const;

    StringView m_string;
    mutable size_t m_length { 0 };
    mutable bool m_have_length { false };
};

}

using AK::Utf8CodePointIterator;
using AK::Utf8View;
