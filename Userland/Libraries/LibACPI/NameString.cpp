/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/StringBuilder.h>
#include <Kernel/Library/KString.h>
#include <LibACPI/Definitions.h>
#include <LibACPI/NameString.h>
#include <LibACPI/OpcodeFilters.h>
#include <LibACPI/TableStream.h>

#ifdef KERNEL
#    define warnln dbgln
#endif

namespace ACPI::AML {

NameSegment::NameSegment(Array<char, 4> value)
    : m_data(value)
{
}

ErrorOr<NameSegment> NameSegment::from_string_view(StringView view)
{
    return NameSegment::from_bytes(view.bytes());
}

ErrorOr<NameSegment> NameSegment::from_bytes(ReadonlyBytes span)
{
    if (span.size() != 4)
        return Error::from_string_view_or_print_error_and_return_errno("Name segment has invalid length"sv, EINVAL);

    if (!is_lead_name_char(span[0]) || !is_name_char(span[1]) || !is_name_char(span[2]) || !is_name_char(span[3])) {
        warnln("ACPI: Invalid character found inside name segment: '{}'", span);
        return Error::from_string_view_or_print_error_and_return_errno("Invalid character found inside name segment."sv, EINVAL);
    }

    return NameSegment({ (char)span[0], (char)span[1], (char)span[2], (char)span[3] });
}

StringView NameSegment::to_string_view() const
{
    StringView view(m_data.data(), 4);
    return view;
}

// https://uefi.org/specs/ACPI/6.6/19_ASL_Reference.html#asl-name-and-pathname-terms
ErrorOr<NameString> NameString::from_string_view(StringView input)
{
    Type type = Type::Relative;
    size_t depth = 0;

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
    Vector<NameSegment, 4> segments;
    for (; i < input.length(); i += 5) {
        if (i + 4 > input.length())
            return Error::from_string_view_or_print_error_and_return_errno("Incomplete name segment."sv, EINVAL);

        TRY(segments.try_append(TRY(NameSegment::from_string_view(input.substring_view(i, 4)))));

        if (i + 4 < input.length()) {
            if (input[i + 4] != '.')
                return Error::from_string_view_or_print_error_and_return_errno("Invalid name segment continuation."sv, EINVAL);
        }
    }

    return NameString(type, depth, move(segments));
}

ErrorOr<NameString> NameString::clone() const
{
    return NameString(m_type, m_parent_index, TRY(m_segments.clone()));
}

ErrorOr<NameSegment> NameString::basename() const
{
    if (m_segments.is_empty())
        return Error::from_string_view_or_print_error_and_return_errno("NullNameString has no base name!"sv, EINVAL);

    return segment(m_segments.size() - 1);
}

#ifdef KERNEL
ErrorOr<NonnullOwnPtr<Kernel::KString>> NameString::to_string() const
#else
ErrorOr<String> NameString::to_string() const
#endif
{
    StringBuilder builder;
    if (m_type == NameString::Type::Absolute) {
        TRY(builder.try_append('\\'));
    } else if (m_parent_index > 0) {
        TRY(builder.try_append_repeated('^', m_parent_index));
    }

    for (size_t i = 0; i < m_segments.size(); i++) {
        TRY(builder.try_append(TRY(segment(i)).to_string_view()));

        if (i != m_segments.size() - 1) {
            TRY(builder.try_append('.'));
        }
    }

#ifdef KERNEL
    return Kernel::KString::try_create(builder.string_view());
#else
    return builder.to_string();
#endif
}

}
