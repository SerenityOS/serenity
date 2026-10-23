/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Result.h>
#include <AK/StringView.h>
#ifndef KERNEL
#    include <AK/String.h>
#else
#    include <Kernel/Library/KString.h>
#endif

namespace ACPI::AML {

class TableStream;

class NameSegment {
public:
    NameSegment()
        : m_data({ '_', '_', '_', '_' })
    {
    }

    static ErrorOr<NameSegment> from_string_view(StringView view);
    static ErrorOr<NameSegment> from_readonly_bytes(ReadonlyBytes span);

    friend bool operator==(NameSegment const& l, NameSegment const& r) { return l.m_data == r.m_data; }
    friend bool operator!=(NameSegment const& l, NameSegment const& r) { return l.m_data != r.m_data; }

    StringView to_string_view() const;

protected:
    friend class NameString;
    NameSegment(Array<char, 4> value);
    Array<char, 4> m_data;
};

class NameString {
public:
    enum class Type : u8 {
        Absolute,
        Relative
    };

    static ErrorOr<NameString> from_table_stream(TableStream& stream);
    static ErrorOr<NameString> from_string_view(StringView input);

    ErrorOr<NameString> dirname() const;
    ErrorOr<NameSegment> basename() const;

#ifndef KERNEL
    ErrorOr<String> to_string() const;
#else
    ErrorOr<NonnullOwnPtr<Kernel::KString>> to_string() const;
#endif

    ErrorOr<NameSegment> segment(size_t index) const;
    Type type() const { return m_type; }
    size_t depth() const { return m_depth; }
    size_t count() const { return m_count; }

    // Clone namestring and its underlying data
    // into a heap allocated ByteBuffer.
    NameString clone(ByteBuffer& buf);

protected:
    NameString(Type type, size_t depth)
        : m_type(type)
        , m_depth(depth)
        , m_count(0)
        , m_additional_unit_bytes(0)
    {
    }
    NameString(Type type, size_t depth, int additional_unit_bytes, size_t count, ReadonlyBytes sequence)
        : m_type(type)
        , m_depth(depth)
        , m_count(count)
        , m_additional_unit_bytes(additional_unit_bytes)
        , m_name_sequence(sequence)
    {
    }

    Type m_type { Type::Relative };
    size_t m_depth { 0 };
    size_t m_count { 0 };

    int m_additional_unit_bytes { 0 };
    ReadonlyBytes m_name_sequence;
};

}
