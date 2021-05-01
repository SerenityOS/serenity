/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Types.h>

namespace AK {

class Utf8View;

class Utf8CodepointIterator {
    friend class Utf8View;

public:
    Utf8CodepointIterator() = default;
    ~Utf8CodepointIterator() = default;

    bool operator==(const Utf8CodepointIterator&) const;
    bool operator!=(const Utf8CodepointIterator&) const;
    Utf8CodepointIterator& operator++();
    u32 operator*() const;

    ssize_t operator-(const Utf8CodepointIterator& other) const
    {
        return m_ptr - other.m_ptr;
    }

    size_t code_point_length_in_bytes() const;
    bool done() const { return m_length == 0; }

private:
    Utf8CodepointIterator(const unsigned char*, size_t);
    const unsigned char* m_ptr { nullptr };
    size_t m_length;
};

class Utf8View {
public:
    using Iterator = Utf8CodepointIterator;

    Utf8View() = default;
    explicit Utf8View(const String&);
    explicit Utf8View(const StringView&);
    explicit Utf8View(const char*);
    ~Utf8View() = default;

    const StringView& as_string() const { return m_string; }

    Utf8CodepointIterator begin() const;
    Utf8CodepointIterator end() const;

    const unsigned char* bytes() const { return begin_ptr(); }
    int byte_length() const { return m_string.length(); }
    size_t byte_offset_of(const Utf8CodepointIterator&) const;
    Utf8View substring_view(int byte_offset, int byte_length) const;
    bool is_empty() const { return m_string.is_empty(); }

    bool starts_with(const Utf8View&) const;

    size_t iterator_offset(const Utf8CodepointIterator& it) const
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
    const unsigned char* begin_ptr() const;
    const unsigned char* end_ptr() const;
    size_t calculate_length() const;

    StringView m_string;
    mutable size_t m_length { 0 };
    mutable bool m_have_length { false };
};

}

using AK::Utf8CodepointIterator;
using AK::Utf8View;
