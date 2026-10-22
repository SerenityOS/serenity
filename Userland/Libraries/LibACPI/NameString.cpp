/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NameString.h"
#include "Definitions.h"
#include "OpcodeFilters.h"
#include "TableStream.h"
#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/StringBuilder.h>

namespace ACPI::AML {

NameSegment::NameSegment(Array<char, 4> value)
    : m_data(value)
{
}

ErrorOr<NameSegment> NameSegment::from_string_view(StringView view)
{
    return NameSegment::from_readonly_bytes(view.bytes());
}

ErrorOr<NameSegment> NameSegment::from_readonly_bytes(ReadonlyBytes span)
{
    if (span.size() != 4)
        return Error::from_string_view_or_print_error_and_return_errno("Name segment has invalid length"sv, EINVAL);

    if (!is_lead_name_char(span[0]) || !is_name_char(span[1]) || !is_name_char(span[2]) || !is_name_char(span[3])) {
        dbgln("[ACPI] Invalid character found inside name segment: '{}'", span);
        return Error::from_string_view_or_print_error_and_return_errno("Invalid character found inside name segment."sv, EINVAL);
    }

    return NameSegment({ (char)span[0], (char)span[1], (char)span[2], (char)span[3] });
}

StringView NameSegment::to_string_view() const
{
    StringView view(m_data.data(), 4);
    return view;
}

ErrorOr<NameString> NameString::from_table_stream(TableStream& stream)
{
    Type type = Type::Relative;
    size_t depth = 0;
    size_t count = 0;
    u8 initial = 0;

    // Figure out path type.
    initial = TRY(stream.read_value<u8>());
    if (initial == '\\') {
        type = Type::Absolute;
    } else if (initial == '^') {
        while (initial == '^') {
            depth++;
            if (stream.is_eof())
                return Error::from_string_view_or_print_error_and_return_errno("Unexpected EOF in NameString."sv, EINVAL);
            initial = TRY(stream.read_value<u8>());
        }

        TRY(stream.seek(stream.offset() - 1));
    } else {
        TRY(stream.seek(stream.offset() - 1));
    }

    // Figure out how many segments are in the path.
    // NamePath := NameSeg | DualNamePath | MultiNamePath | NullName
    initial = TRY(stream.read_value<u8>());
    switch (initial) {
    case 0:
        return NameString(type, depth);

    case static_cast<u8>(Prefix::MultiNamePrefix):
        count = TRY(stream.read_value<u8>());

        if (count == 0) {
            return Error::from_string_view_or_print_error_and_return_errno("Multiname path must have at least 1 item."sv, EINVAL);
        }
        break;

    case static_cast<u8>(Prefix::DualNamePrefix):
        count = 2;
        break;

    default:
        if (is_lead_name_char(initial)) {
            TRY(stream.seek(stream.offset() - 1));
            count = 1;
            break;
        }

        dbgln("[ACPI] Invalid character: '{:X}' at offset {:X}", (char)initial, stream.offset());
        return Error::from_string_view_or_print_error_and_return_errno("Invalid character found."sv, EINVAL);
    }

    // Validate items inside remaining path..
    auto span = TRY(stream.read_in_place<u8 const>(count * 4));
    for (size_t i = 0; i < count; i++) {
        auto item = span.slice(i * 4, 4);
        if (!is_lead_name_char(item[0]) || !is_name_char(item[1]) || !is_name_char(item[2]) || !is_name_char(item[3])) {
            dbgln("[ACPI] Invalid character found inside name segment: '{}'", item);
            return Error::from_string_view_or_print_error_and_return_errno("Invalid character found inside name segment."sv, EINVAL);
        }
    }
    return NameString(type, depth, 0, count, span);
}

ErrorOr<NameString> NameString::from_string_view(StringView input)
{
    Type type = Type::Relative;
    size_t depth = 0;
    size_t count = 0;
    size_t i = 0;

    if (input[i] == '\\') {
        type = NameString::Type::Absolute;
        if (i >= input.length())
            return NameString(type, depth);

        i++;
    } else if (input[i] == '^') {
        type = NameString::Type::Relative;
        while (i < input.length() && input[i] == '^') {
            depth++;
            i++;
        }
    }

    if (i >= input.length())
        return NameString(type, depth);

    // Validate segments, alongside separators.
    size_t segment_start = i;
    for (; i < input.length(); i += 5) {
        if (i + 4 > input.length())
            return Error::from_string_view_or_print_error_and_return_errno("Incomplete name segment."sv, EINVAL);

        if (!is_lead_name_char(input[i]) || !is_name_char(input[i + 1]) || !is_name_char(input[i + 2]) || !is_name_char(input[i + 3]))
            return Error::from_string_view_or_print_error_and_return_errno("Invalid character found inside name segment."sv, EINVAL);

        if (i + 4 < input.length()) {
            if (input[i + 4] != '.' && input[i + 4] != 0)
                return Error::from_string_view_or_print_error_and_return_errno("Invalid name segment continuation."sv, EINVAL);
        }

        count++;
    }

    auto view = input.substring_view(segment_start, (count * 5) - 1);
    return NameString(type, depth, 1, count, view.bytes());
}

ErrorOr<NameSegment> NameString::segment(std::size_t index) const
{
    if (index >= m_count)
        return Error::from_string_view_or_print_error_and_return_errno("Segment index out of bounds!"sv, EINVAL);

    return NameSegment::from_readonly_bytes(m_name_sequence.slice(index * (4 + m_additional_unit_bytes), 4));
}

#ifndef KERNEL
ErrorOr<String> NameString::to_string() const
#else
ErrorOr<NonnullOwnPtr<Kernel::KString>> NameString::to_string() const
#endif
{
    StringBuilder builder;
    if (m_type == NameString::Type::Absolute) {
        TRY(builder.try_append('\\'));
    } else if (m_depth > 0) {
        TRY(builder.try_append_repeated('^', m_depth));
    }

    for (size_t i = 0; i < m_count; i++) {
        TRY(builder.try_append(TRY(segment(i)).to_string_view()));
        if (i != m_count - 1) {
            TRY(builder.try_append('.'));
        }
    }

#ifndef KERNEL
    return builder.to_string();
#else
    return TRY(Kernel::KString::try_create(builder.string_view()));
#endif
}

ErrorOr<NameString> NameString::dirname() const
{
    if (m_count == 0)
        return Error::from_string_view_or_print_error_and_return_errno("NullNameString has no dir name!"sv, EINVAL);

    return NameString(m_type, m_depth, m_additional_unit_bytes, m_count - 1, m_name_sequence);
}

ErrorOr<NameSegment> NameString::basename() const
{
    if (m_count == 0)
        return Error::from_string_view_or_print_error_and_return_errno("NullNameString has no base name!"sv, EINVAL);

    return segment(m_count - 1);
}

NameString NameString::clone(ByteBuffer& buf)
{
    buf.resize(m_name_sequence.size());
    m_name_sequence.copy_to(buf);
    return NameString(m_type, m_depth, m_additional_unit_bytes, m_count, buf.bytes());
}

}
