/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <Kernel/ACPI/Bytecode/NameString.h>

namespace Kernel::ACPI {

struct NamePathResult {
    NameString::NameMultiplyPrefix prefix;
    Vector<StringView> paths;
};

static Optional<NamePathResult> parse_name_path(Span<u8 const> encoded_name_string)
{
    if (encoded_name_string[0] == 0)
        return {};

    // FIXME: Add safety checks to ensure we can actually read from the encoded_name_string
    // given that we parse multi names sometimes.
    Vector<StringView> string_views;
    NameString::NameMultiplyPrefix prefix = NameString::NameMultiplyPrefix::None;
    switch (encoded_name_string[0]) {
    case 0x2E: // Dual Name
        prefix = NameString::NameMultiplyPrefix::Dual;
        string_views.append({ encoded_name_string.slice(1, 4).data(), 4 });
        string_views.append({ encoded_name_string.slice(5, 4).data(), 4 });
        break;
    case 0x2F: // Multi Name
        prefix = NameString::NameMultiplyPrefix::Multiple;
        // Note: encoded_name_string[1] holds the Segment count
        // Note: We slice everything with a start of 2, because we have the 0x2F Multi Name Prefix and ByteData of SegCount.
        for (size_t string_index = 0; string_index < encoded_name_string[1]; string_index++) {
            string_views.append(StringView { encoded_name_string.slice(2 + (4 * string_index), 4) });
        }
        break;
    default:
        string_views.append({ encoded_name_string.trim(4).data(), 4 });
    }
    return NamePathResult { prefix, string_views };
}

NameString::NameString(Speciality speciality, size_t enumerated_prefix_paths_count)
    : m_speciality(speciality)
    , m_prefix_paths_count(enumerated_prefix_paths_count)
{
    if (m_prefix_paths_count > 0) {
        VERIFY(speciality == Speciality::HasPrefixPath);
    }
}

NameString::NameString(const Vector<StringView>& string_views, Speciality speciality, NameMultiplyPrefix multiply_prefix, size_t enumerated_prefix_paths_count)
    : m_speciality(speciality)
    , m_prefix(multiply_prefix)
    , m_prefix_paths_count(enumerated_prefix_paths_count)
{
    if (m_prefix_paths_count > 0) {
        VERIFY(speciality == Speciality::HasPrefixPath);
    }
    for (auto& string_view : string_views) {
        m_name_segments.append(KString::try_create(string_view));
    }
}

NameString::NameString(StringView name_segment)
    : m_speciality(Speciality::None)
    , m_prefix_paths_count(0)
{
    // Note: Name segment must be 4 characters long.
    VERIFY(name_segment.length() == 4);
    m_name_segments.append(KString::try_create(name_segment));
}

size_t NameString::encoded_length() const
{
    size_t count = 0;
    if (m_speciality == Speciality::HasRootChar) {
        count++;
    } else if (m_speciality == Speciality::HasPrefixPath) {
        for (size_t prefix_path_index = 0; prefix_path_index < m_prefix_paths_count; prefix_path_index++)
            count++;
    }
    if (m_name_segments.is_empty()) {
        // Note: In case NamePath has (RootChar or PrefixPath) and a NullName, this is the correct encoded length.
        return count + 1;
    }
    for (auto& name_segment : m_name_segments) {
        count += name_segment->view().length();
    }
    if (m_prefix == NameMultiplyPrefix::Multiple) {
        // Note: 2 for SegCount + 0x2F Multiple Name Prefix
        count += 2;
    } else if (m_prefix == NameMultiplyPrefix::Dual) {
        // Note: 1 for 0x2E Dual Name Prefix
        count += 1;
    }
    return count;
}

String NameString::full_name() const
{
    StringBuilder builder;
    if (m_speciality == Speciality::HasRootChar) {
        builder.append("\\");
    } else if (m_speciality == Speciality::HasPrefixPath) {
        for (size_t prefix_path_index = 0; prefix_path_index < m_prefix_paths_count; prefix_path_index++)
            builder.append("^");
    }
    for (auto& name_segment : m_name_segments) {
        VERIFY(name_segment);
        builder.append(name_segment->view());
    }
    return builder.build();
}

RefPtr<NameString> NameString::try_to_create_with_string_view(StringView name_segment)
{
    return adopt_ref_if_nonnull(new (nothrow) NameString(name_segment));
}

RefPtr<NameString> NameString::try_to_evaluate_with_validation(Span<u8 const> encoded_strings)
{
    auto name_string = try_to_create(encoded_strings);
    if (!name_string)
        return {};
    for (auto& name_segment : name_string->name_segments()) {
        VERIFY(name_segment);
        auto name_segment_view = name_segment->view();
        dbgln_if(ACPI_AML_DEBUG, "Name String {}", name_segment_view);
        VERIFY(name_segment_view.length() == 4);
        for (size_t char_index = 0; char_index < 4; char_index++) {
            if (name_segment_view[char_index] >= 'A' || name_segment_view[char_index] <= 'Z')
                continue;
            if (name_segment_view[char_index] >= 'a' || name_segment_view[char_index] <= 'z')
                continue;
            if (name_segment_view[char_index] >= '0' || name_segment_view[char_index] <= '9')
                continue;
            if (name_segment_view[char_index] == '_')
                continue;
            return {};
        }
    }
    return name_string;
}

RefPtr<NameString> NameString::try_to_create(Span<u8 const> encoded_name_string)
{
    bool has_prefix_paths = false;
    bool has_root_char = false;
    switch (encoded_name_string[0]) {
    case 0x5E:
        has_prefix_paths = true;
        break;
    case 0x5C:
        has_root_char = true;
        break;
    case 0x0:
        return {};
    }

    // Note: These flags are "mutually exclusive", which means they can't be both true at the same time.
    VERIFY(!(has_root_char && has_prefix_paths));
    if (has_root_char) {

        auto name_paths = parse_name_path(encoded_name_string.slice(1));

        if (name_paths.has_value())
            return adopt_ref_if_nonnull(new (nothrow) NameString(name_paths.value().paths, Speciality::HasRootChar, name_paths.value().prefix, 0));
        return adopt_ref_if_nonnull(new (nothrow) NameString(Speciality::HasRootChar, 0));
    }

    if (has_prefix_paths) {
        size_t enumerated_prefix_paths_count = 1;
        // Note: We start from char at the second slot, because we already know there's at least one Prefix Path.
        for (size_t char_index = 1; char_index < encoded_name_string.size(); char_index++) {
            if (encoded_name_string[char_index] != 0x5E)
                break;
            enumerated_prefix_paths_count++;
        }
        auto name_paths = parse_name_path(encoded_name_string.slice(enumerated_prefix_paths_count));

        if (name_paths.has_value())
            return adopt_ref_if_nonnull(new (nothrow) NameString(name_paths.value().paths, Speciality::HasPrefixPath, name_paths.value().prefix, enumerated_prefix_paths_count));
        return adopt_ref_if_nonnull(new (nothrow) NameString(Speciality::HasPrefixPath, enumerated_prefix_paths_count));
    }

    auto name_paths = parse_name_path(encoded_name_string);
    if (name_paths.has_value())
        return adopt_ref_if_nonnull(new (nothrow) NameString(name_paths.value().paths, Speciality::None, name_paths.value().prefix, 0));
    return adopt_ref_if_nonnull(new (nothrow) NameString(Speciality::None, 0));
}

}
