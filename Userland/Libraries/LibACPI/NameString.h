/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Vector.h>
#ifdef KERNEL
#    include <Kernel/Library/KString.h>
#else
#    include <AK/String.h>
#endif

namespace ACPI::AML {

class TableStream;

class NameSegment {
public:
    NameSegment() = default;

    static ErrorOr<NameSegment> from_string_view(StringView view);
    static ErrorOr<NameSegment> from_bytes(ReadonlyBytes span);

    friend bool operator==(NameSegment const& l, NameSegment const& r) { return l.m_data == r.m_data; }
    friend bool operator!=(NameSegment const& l, NameSegment const& r) { return l.m_data != r.m_data; }

    StringView to_string_view() const;

private:
    NameSegment(Array<char, 4> value);

    Array<char, 4> m_data { '_', '_', '_', '_' };
};

class NameString {
public:
    enum class Type : u8 {
        Absolute,
        Relative,
    };

    static ErrorOr<NameString> from_string_view(StringView input);

    NameString(Type type, size_t depth)
        : m_type(type)
        , m_parent_index(depth)
    {
    }

    NameString(Type type, size_t depth, Vector<NameSegment, 4> segments)
        : m_type(type)
        , m_parent_index(depth)
        , m_segments(move(segments))
    {
    }

    ErrorOr<NameString> clone() const;

    ErrorOr<NameSegment> basename() const;

#ifdef KERNEL
    ErrorOr<NonnullOwnPtr<Kernel::KString>> to_string() const;
#else
    ErrorOr<String> to_string() const;
#endif

    ErrorOr<NameSegment> segment(size_t index) const { return m_segments[index]; }
    size_t segment_count() const { return m_segments.size(); }

    Type type() const { return m_type; }
    size_t parent_index() const { return m_parent_index; }

private:
    Type m_type { Type::Relative };
    size_t m_parent_index { 0 };

    Vector<NameSegment, 4> m_segments;
};

}
