/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/AllOf.h>
#include <AK/Array.h>
#include <AK/CharacterTypes.h>
#include <AK/DeprecatedString.h>
#include <AK/Error.h>
#include <AK/Find.h>
#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/QuickSort.h>
#include <AK/SourceGenerator.h>
#include <AK/StringUtils.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>

// Some code points are excluded from UnicodeData.txt, and instead are part of a "range" of code
// points, as indicated by the "name" field. For example:
//     3400;<CJK Ideograph Extension A, First>;Lo;0;L;;;;;N;;;;;
//     4DBF;<CJK Ideograph Extension A, Last>;Lo;0;L;;;;;N;;;;;
struct CodePointRange {
    u32 first;
    u32 last;
};

// https://www.unicode.org/reports/tr44/#SpecialCasing.txt
struct SpecialCasing {
    u32 index { 0 };
    u32 code_point { 0 };
    Vector<u32> lowercase_mapping;
    Vector<u32> uppercase_mapping;
    Vector<u32> titlecase_mapping;
    DeprecatedString locale;
    DeprecatedString condition;
};

// https://www.unicode.org/reports/tr44/#CaseFolding.txt
struct CaseFolding {
    u32 code_point { 0 };
    StringView status { "Common"sv };
    Vector<u32> mapping { 0 };
};

// https://www.unicode.org/reports/tr44/#Character_Decomposition_Mappings
struct CodePointDecomposition {
    // `tag` is a string since it's used for codegen as an enum value.
    DeprecatedString tag { "Canonical"sv };
    size_t decomposition_index { 0 };
    size_t decomposition_size { 0 };
};

// https://www.unicode.org/reports/tr44/#PropList.txt
using PropList = HashMap<DeprecatedString, Vector<CodePointRange>>;

// https://www.unicode.org/reports/tr44/#DerivedNormalizationProps.txt
enum class QuickCheck {
    Yes,
    No,
    Maybe,
};

struct Normalization {
    CodePointRange code_point_range;
    Vector<u32> value;
    QuickCheck quick_check { QuickCheck::Yes };
};

using NormalizationProps = HashMap<DeprecatedString, Vector<Normalization>>;

struct CodePointName {
    CodePointRange code_point_range;
    size_t name { 0 };
};

// https://www.unicode.org/reports/tr44/#UnicodeData.txt
struct CodePointData {
    u32 code_point { 0 };
    DeprecatedString name;
    Optional<size_t> abbreviation;
    u8 canonical_combining_class { 0 };
    DeprecatedString bidi_class;
    Optional<CodePointDecomposition> decomposition_mapping;
    Optional<i8> numeric_value_decimal;
    Optional<i8> numeric_value_digit;
    Optional<i8> numeric_value_numeric;
    bool bidi_mirrored { false };
    DeprecatedString unicode_1_name;
    DeprecatedString iso_comment;
    Optional<u32> simple_uppercase_mapping;
    Optional<u32> simple_lowercase_mapping;
    Optional<u32> simple_titlecase_mapping;
    Vector<u32> special_casing_indices;
    Vector<u32> case_folding_indices;
};

struct BlockName {
    CodePointRange code_point_range;
    size_t name { 0 };
};

struct UnicodeData {
    UniqueStringStorage unique_strings;

    u32 code_points_with_non_zero_combining_class { 0 };

    u32 code_points_with_decomposition_mapping { 0 };
    Vector<u32> decomposition_mappings;
    Vector<DeprecatedString> compatibility_tags;

    u32 simple_uppercase_mapping_size { 0 };
    u32 simple_lowercase_mapping_size { 0 };
    u32 simple_titlecase_mapping_size { 0 };

    Vector<SpecialCasing> special_casing;
    u32 code_points_with_special_casing { 0 };
    u32 largest_special_casing_mapping_size { 0 };
    u32 largest_special_casing_size { 0 };
    Vector<DeprecatedString> conditions;
    Vector<DeprecatedString> locales;

    Vector<CaseFolding> case_folding;
    u32 code_points_with_case_folding { 0 };
    u32 largest_case_folding_mapping_size { 0 };
    u32 largest_case_folding_size { 0 };
    Vector<StringView> statuses;

    Vector<CodePointData> code_point_data;

    HashMap<u32, size_t> code_point_abbreviations;
    HashMap<u32, size_t> code_point_display_name_aliases;
    Vector<CodePointName> code_point_display_names;

    // https://www.unicode.org/reports/tr44/#General_Category_Values
    PropList general_categories;
    Vector<Alias> general_category_aliases;

    // The Unicode standard defines additional properties (Any, Assigned, ASCII) which are not in
    // any UCD file. Assigned code point ranges are derived as this generator is executed.
    // https://unicode.org/reports/tr18/#General_Category_Property
    PropList prop_list {
        { "Any"sv, { { 0, 0x10ffff } } },
        { "Assigned"sv, {} },
        { "ASCII"sv, { { 0, 0x7f } } },
    };
    Vector<Alias> prop_aliases;

    PropList script_list {
        { "Unknown"sv, {} },
    };
    Vector<Alias> script_aliases;
    PropList script_extensions;

    Vector<BlockName> block_display_names;

    // FIXME: We are not yet doing anything with this data. It will be needed for String.prototype.normalize.
    NormalizationProps normalization_props;

    PropList grapheme_break_props;
    PropList word_break_props;
    PropList sentence_break_props;
};

static DeprecatedString sanitize_entry(DeprecatedString const& entry)
{
    auto sanitized = entry.replace("-"sv, "_"sv, ReplaceMode::All);
    sanitized = sanitized.replace(" "sv, "_"sv, ReplaceMode::All);

    StringBuilder builder;
    bool next_is_upper = true;
    for (auto ch : sanitized) {
        if (next_is_upper)
            builder.append_code_point(to_ascii_uppercase(ch));
        else
            builder.append_code_point(ch);
        next_is_upper = ch == '_';
    }

    return builder.to_deprecated_string();
}

static Vector<u32> parse_code_point_list(StringView list)
{
    Vector<u32> code_points;

    auto segments = list.split_view(' ');
    for (auto const& code_point : segments)
        code_points.append(AK::StringUtils::convert_to_uint_from_hex<u32>(code_point).value());

    return code_points;
}

static CodePointRange parse_code_point_range(StringView list)
{
    CodePointRange code_point_range {};

    if (list.contains(".."sv)) {
        auto segments = list.split_view(".."sv);
        VERIFY(segments.size() == 2);

        auto begin = AK::StringUtils::convert_to_uint_from_hex<u32>(segments[0]).value();
        auto end = AK::StringUtils::convert_to_uint_from_hex<u32>(segments[1]).value();
        code_point_range = { begin, end };
    } else {
        auto code_point = AK::StringUtils::convert_to_uint_from_hex<u32>(list).value();
        code_point_range = { code_point, code_point };
    }

    return code_point_range;
}

static ErrorOr<void> parse_special_casing(Core::InputBufferedFile& file, UnicodeData& unicode_data)
{
    Array<u8, 1024> buffer;

    while (TRY(file.can_read_line())) {
        auto line = TRY(file.read_line(buffer));

        if (line.is_empty() || line.starts_with('#'))
            continue;

        if (auto index = line.find('#'); index.has_value())
            line = line.substring_view(0, *index);

        auto segments = line.split_view(';', SplitBehavior::KeepEmpty);
        VERIFY(segments.size() == 5 || segments.size() == 6);

        SpecialCasing casing {};
        casing.code_point = AK::StringUtils::convert_to_uint_from_hex<u32>(segments[0]).value();
        casing.lowercase_mapping = parse_code_point_list(segments[1]);
        casing.titlecase_mapping = parse_code_point_list(segments[2]);
        casing.uppercase_mapping = parse_code_point_list(segments[3]);

        if (auto condition = segments[4].trim_whitespace(); !condition.is_empty()) {
            auto conditions = condition.split_view(' ', SplitBehavior::KeepEmpty);
            VERIFY(conditions.size() == 1 || conditions.size() == 2);

            if (conditions.size() == 2) {
                casing.locale = conditions[0];
                casing.condition = conditions[1];
            } else if (all_of(conditions[0], is_ascii_lower_alpha)) {
                casing.locale = conditions[0];
            } else {
                casing.condition = conditions[0];
            }

            if (!casing.locale.is_empty()) {
                casing.locale = DeprecatedString::formatted("{:c}{}", to_ascii_uppercase(casing.locale[0]), casing.locale.substring_view(1));

                if (!unicode_data.locales.contains_slow(casing.locale))
                    unicode_data.locales.append(casing.locale);
            }

            casing.condition = casing.condition.replace("_"sv, ""sv, ReplaceMode::All);

            if (!casing.condition.is_empty() && !unicode_data.conditions.contains_slow(casing.condition))
                unicode_data.conditions.append(casing.condition);
        }

        unicode_data.largest_special_casing_mapping_size = max(unicode_data.largest_special_casing_mapping_size, casing.lowercase_mapping.size());
        unicode_data.largest_special_casing_mapping_size = max(unicode_data.largest_special_casing_mapping_size, casing.titlecase_mapping.size());
        unicode_data.largest_special_casing_mapping_size = max(unicode_data.largest_special_casing_mapping_size, casing.uppercase_mapping.size());

        unicode_data.special_casing.append(move(casing));
    }

    quick_sort(unicode_data.special_casing, [](auto const& lhs, auto const& rhs) {
        if (lhs.code_point != rhs.code_point)
            return lhs.code_point < rhs.code_point;
        if (lhs.locale.is_empty() && !rhs.locale.is_empty())
            return false;
        if (!lhs.locale.is_empty() && rhs.locale.is_empty())
            return true;
        return lhs.locale < rhs.locale;
    });

    for (u32 i = 0; i < unicode_data.special_casing.size(); ++i)
        unicode_data.special_casing[i].index = i;

    return {};
}

static ErrorOr<void> parse_case_folding(Core::InputBufferedFile& file, UnicodeData& unicode_data)
{
    Array<u8, 1024> buffer;

    while (TRY(file.can_read_line())) {
        auto line = TRY(file.read_line(buffer));
        if (line.is_empty() || line.starts_with('#'))
            continue;

        auto segments = line.split_view(';', SplitBehavior::KeepEmpty);
        VERIFY(segments.size() == 4);

        CaseFolding folding {};
        folding.code_point = AK::StringUtils::convert_to_uint_from_hex<u32>(segments[0]).value();
        folding.mapping = parse_code_point_list(segments[2]);

        switch (segments[1].trim_whitespace()[0]) {
        case 'C':
            folding.status = "Common"sv;
            break;
        case 'F':
            folding.status = "Full"sv;
            break;
        case 'S':
            folding.status = "Simple"sv;
            break;
        case 'T':
            folding.status = "Special"sv;
            break;
        }

        unicode_data.largest_case_folding_mapping_size = max(unicode_data.largest_case_folding_mapping_size, folding.mapping.size());

        if (!unicode_data.statuses.contains_slow(folding.status))
            unicode_data.statuses.append(folding.status);

        unicode_data.case_folding.append(move(folding));
    }

    quick_sort(unicode_data.case_folding, [](auto const& lhs, auto const& rhs) {
        if (lhs.code_point != rhs.code_point)
            return lhs.code_point < rhs.code_point;
        return lhs.status < rhs.status;
    });

    return {};
}

static ErrorOr<void> parse_prop_list(Core::InputBufferedFile& file, PropList& prop_list, bool multi_value_property = false, bool sanitize_property = false)
{
    Array<u8, 1024> buffer;

    while (TRY(file.can_read_line())) {
        auto line = TRY(file.read_line(buffer));

        if (line.is_empty() || line.starts_with('#'))
            continue;

        if (auto index = line.find('#'); index.has_value())
            line = line.substring_view(0, *index);

        auto segments = line.split_view(';', SplitBehavior::KeepEmpty);
        VERIFY(segments.size() == 2);

        auto code_point_range = parse_code_point_range(segments[0].trim_whitespace());
        Vector<StringView> properties;

        if (multi_value_property)
            properties = segments[1].trim_whitespace().split_view(' ');
        else
            properties = { segments[1].trim_whitespace() };

        for (auto& property : properties) {
            auto& code_points = prop_list.ensure(sanitize_property ? sanitize_entry(property).trim_whitespace().view() : property.trim_whitespace());
            code_points.append(code_point_range);
        }
    }

    return {};
}

static ErrorOr<void> parse_alias_list(Core::InputBufferedFile& file, PropList const& prop_list, Vector<Alias>& prop_aliases)
{
    DeprecatedString current_property;
    Array<u8, 1024> buffer;

    auto append_alias = [&](auto alias, auto property) {
        // Note: The alias files contain lines such as "Hyphen = Hyphen", which we should just skip.
        if (alias == property)
            return;

        // FIXME: We will, eventually, need to find where missing properties are located and parse them.
        if (!prop_list.contains(property))
            return;

        prop_aliases.append({ property, alias });
    };

    while (TRY(file.can_read_line())) {
        auto line = TRY(file.read_line(buffer));

        if (line.is_empty() || line.starts_with('#')) {
            if (line.ends_with("Properties"sv))
                current_property = line.substring_view(2);
            continue;
        }

        // Note: For now, we only care about Binary Property aliases for Unicode property escapes.
        if (current_property != "Binary Properties"sv)
            continue;

        auto segments = line.split_view(';', SplitBehavior::KeepEmpty);
        VERIFY((segments.size() == 2) || (segments.size() == 3));

        auto alias = segments[0].trim_whitespace();
        auto property = segments[1].trim_whitespace();
        append_alias(alias, property);

        if (segments.size() == 3) {
            alias = segments[2].trim_whitespace();
            append_alias(alias, property);
        }
    }

    return {};
}

static ErrorOr<void> parse_name_aliases(Core::InputBufferedFile& file, UnicodeData& unicode_data)
{
    Array<u8, 1024> buffer;

    while (TRY(file.can_read_line())) {
        auto line = TRY(file.read_line(buffer));

        if (line.is_empty() || line.starts_with('#'))
            continue;

        auto segments = line.split_view(';', SplitBehavior::KeepEmpty);
        VERIFY(segments.size() == 3);

        auto code_point = AK::StringUtils::convert_to_uint_from_hex<u32>(segments[0].trim_whitespace());
        auto alias = segments[1].trim_whitespace();
        auto reason = segments[2].trim_whitespace();

        if (reason == "abbreviation"sv) {
            auto index = unicode_data.unique_strings.ensure(alias);
            unicode_data.code_point_abbreviations.set(*code_point, index);
        } else if (reason.is_one_of("correction"sv, "control"sv)) {
            if (!unicode_data.code_point_display_name_aliases.contains(*code_point)) {
                auto index = unicode_data.unique_strings.ensure(alias);
                unicode_data.code_point_display_name_aliases.set(*code_point, index);
            }
        }
    }

    return {};
}

static ErrorOr<void> parse_value_alias_list(Core::InputBufferedFile& file, StringView desired_category, Vector<DeprecatedString> const& value_list, Vector<Alias>& prop_aliases, bool primary_value_is_first = true, bool sanitize_alias = false)
{
    TRY(file.seek(0, SeekMode::SetPosition));
    Array<u8, 1024> buffer;

    auto append_alias = [&](auto alias, auto value) {
        // Note: The value alias file contains lines such as "Ahom = Ahom", which we should just skip.
        if (alias == value)
            return;

        // FIXME: We will, eventually, need to find where missing properties are located and parse them.
        if (!value_list.contains_slow(value))
            return;

        prop_aliases.append({ value, alias });
    };

    while (TRY(file.can_read_line())) {
        auto line = TRY(file.read_line(buffer));

        if (line.is_empty() || line.starts_with('#'))
            continue;

        if (auto index = line.find('#'); index.has_value())
            line = line.substring_view(0, *index);

        auto segments = line.split_view(';', SplitBehavior::KeepEmpty);
        auto category = segments[0].trim_whitespace();

        if (category != desired_category)
            continue;

        VERIFY((segments.size() == 3) || (segments.size() == 4));
        auto value = primary_value_is_first ? segments[1].trim_whitespace() : segments[2].trim_whitespace();
        auto alias = primary_value_is_first ? segments[2].trim_whitespace() : segments[1].trim_whitespace();
        append_alias(sanitize_alias ? sanitize_entry(alias).view() : alias, value);

        if (segments.size() == 4) {
            alias = segments[3].trim_whitespace();
            append_alias(sanitize_alias ? sanitize_entry(alias).view() : alias, value);
        }
    }

    return {};
}

static ErrorOr<void> parse_normalization_props(Core::InputBufferedFile& file, UnicodeData& unicode_data)
{
    Array<u8, 1024> buffer;

    while (TRY(file.can_read_line())) {
        auto line = TRY(file.read_line(buffer));

        if (line.is_empty() || line.starts_with('#'))
            continue;

        if (auto index = line.find('#'); index.has_value())
            line = line.substring_view(0, *index);

        auto segments = line.split_view(';', SplitBehavior::KeepEmpty);
        VERIFY((segments.size() == 2) || (segments.size() == 3));

        auto code_point_range = parse_code_point_range(segments[0].trim_whitespace());
        auto property = segments[1].trim_whitespace().to_deprecated_string();

        Vector<u32> value;
        QuickCheck quick_check = QuickCheck::Yes;

        if (segments.size() == 3) {
            auto value_or_quick_check = segments[2].trim_whitespace();

            if ((value_or_quick_check == "N"sv))
                quick_check = QuickCheck::No;
            else if ((value_or_quick_check == "M"sv))
                quick_check = QuickCheck::Maybe;
            else
                value = parse_code_point_list(value_or_quick_check);
        }

        auto& normalizations = unicode_data.normalization_props.ensure(property);
        normalizations.append({ code_point_range, move(value), quick_check });

        auto& prop_list = unicode_data.prop_list.ensure(property);
        prop_list.append(move(code_point_range));
    }

    return {};
}

static void add_canonical_code_point_name(CodePointRange range, StringView name, UnicodeData& unicode_data)
{
    // https://www.unicode.org/versions/Unicode15.0.0/ch04.pdf#G142981
    // FIXME: Implement the NR1 rules for Hangul syllables.

    struct CodePointNameFormat {
        CodePointRange code_point_range;
        StringView name;
    };

    // These code point ranges are the NR2 set of name replacements defined by Table 4-8.
    constexpr Array<CodePointNameFormat, 16> s_ideographic_replacements { {
        { { 0x3400, 0x4DBF }, "CJK UNIFIED IDEOGRAPH-{:X}"sv },
        { { 0x4E00, 0x9FFF }, "CJK UNIFIED IDEOGRAPH-{:X}"sv },
        { { 0xF900, 0xFA6D }, "CJK COMPATIBILITY IDEOGRAPH-{:X}"sv },
        { { 0xFA70, 0xFAD9 }, "CJK COMPATIBILITY IDEOGRAPH-{:X}"sv },
        { { 0x17000, 0x187F7 }, "TANGUT IDEOGRAPH-{:X}"sv },
        { { 0x18B00, 0x18CD5 }, "KHITAN SMALL SCRIPT CHARACTER-{:X}"sv },
        { { 0x18D00, 0x18D08 }, "TANGUT IDEOGRAPH-{:X}"sv },
        { { 0x1B170, 0x1B2FB }, "NUSHU CHARACTER-{:X}"sv },
        { { 0x20000, 0x2A6DF }, "CJK UNIFIED IDEOGRAPH-{:X}"sv },
        { { 0x2A700, 0x2B739 }, "CJK UNIFIED IDEOGRAPH-{:X}"sv },
        { { 0x2B740, 0x2B81D }, "CJK UNIFIED IDEOGRAPH-{:X}"sv },
        { { 0x2B820, 0x2CEA1 }, "CJK UNIFIED IDEOGRAPH-{:X}"sv },
        { { 0x2CEB0, 0x2EBE0 }, "CJK UNIFIED IDEOGRAPH-{:X}"sv },
        { { 0x2F800, 0x2FA1D }, "CJK COMPATIBILITY IDEOGRAPH-{:X}"sv },
        { { 0x30000, 0x3134A }, "CJK UNIFIED IDEOGRAPH-{:X}"sv },
        { { 0x31350, 0x323AF }, "CJK UNIFIED IDEOGRAPH-{:X}"sv },
    } };

    auto it = find_if(s_ideographic_replacements.begin(), s_ideographic_replacements.end(),
        [&](auto const& replacement) {
            return replacement.code_point_range.first == range.first;
        });

    if (it != s_ideographic_replacements.end()) {
        auto index = unicode_data.unique_strings.ensure(it->name);
        unicode_data.code_point_display_names.append({ it->code_point_range, index });
        return;
    }

    it = find_if(s_ideographic_replacements.begin(), s_ideographic_replacements.end(),
        [&](auto const& replacement) {
            return (replacement.code_point_range.first <= range.first) && (range.first <= replacement.code_point_range.last);
        });

    if (it != s_ideographic_replacements.end()) {
        // Drop code points that will have been captured by a range defined by the ideographic replacements.
        return;
    }

    if (auto alias = unicode_data.code_point_display_name_aliases.get(range.first); alias.has_value()) {
        // NR4 states that control code points have a null string as their name. Our implementation
        // uses the control code's alias as its display name.
        unicode_data.code_point_display_names.append({ range, *alias });
        return;
    }

    auto index = unicode_data.unique_strings.ensure(name);
    unicode_data.code_point_display_names.append({ range, index });
}

static Optional<CodePointDecomposition> parse_decomposition_mapping(StringView string, UnicodeData& unicode_data)
{
    if (string.is_empty())
        return {};

    CodePointDecomposition mapping;

    auto parts = string.split_view(' ');

    VERIFY(parts.size() > 0);

    if (parts.first().starts_with('<')) {
        auto const tag = parts.take_first().trim("<>"sv);

        mapping.tag = DeprecatedString::formatted("{:c}{}", to_ascii_uppercase(tag[0]), tag.substring_view(1));

        if (!unicode_data.compatibility_tags.contains_slow(mapping.tag))
            unicode_data.compatibility_tags.append(mapping.tag);
    }

    mapping.decomposition_index = unicode_data.decomposition_mappings.size();
    mapping.decomposition_size = parts.size();
    for (auto part : parts) {
        unicode_data.decomposition_mappings.append(AK::StringUtils::convert_to_uint_from_hex<u32>(part).value());
    }

    return mapping;
}

static ErrorOr<void> parse_block_display_names(Core::InputBufferedFile& file, UnicodeData& unicode_data)
{
    Array<u8, 1024> buffer;
    while (TRY(file.can_read_line())) {
        auto line = TRY(file.read_line(buffer));
        if (line.is_empty() || line.starts_with('#'))
            continue;

        auto segments = line.split_view(';', SplitBehavior::KeepEmpty);
        VERIFY(segments.size() == 2);

        auto code_point_range = parse_code_point_range(segments[0].trim_whitespace());
        auto display_name = segments[1].trim_whitespace();

        auto index = unicode_data.unique_strings.ensure(display_name);
        unicode_data.block_display_names.append({ code_point_range, index });
    }

    TRY(file.seek(0, SeekMode::SetPosition));

    return {};
}

static ErrorOr<void> parse_unicode_data(Core::InputBufferedFile& file, UnicodeData& unicode_data)
{
    Optional<u32> code_point_range_start;

    auto& assigned_code_points = unicode_data.prop_list.find("Assigned"sv)->value;
    Optional<u32> assigned_code_point_range_start = 0;
    u32 previous_code_point = 0;

    Array<u8, 1024> buffer;

    while (TRY(file.can_read_line())) {
        auto line = TRY(file.read_line(buffer));

        if (line.is_empty())
            continue;

        auto segments = line.split_view(';', SplitBehavior::KeepEmpty);
        VERIFY(segments.size() == 15);

        CodePointData data {};
        data.code_point = AK::StringUtils::convert_to_uint_from_hex<u32>(segments[0]).value();
        data.name = segments[1];
        data.canonical_combining_class = AK::StringUtils::convert_to_uint<u8>(segments[3]).value();
        data.bidi_class = segments[4];
        data.decomposition_mapping = parse_decomposition_mapping(segments[5], unicode_data);
        data.numeric_value_decimal = AK::StringUtils::convert_to_int<i8>(segments[6]);
        data.numeric_value_digit = AK::StringUtils::convert_to_int<i8>(segments[7]);
        data.numeric_value_numeric = AK::StringUtils::convert_to_int<i8>(segments[8]);
        data.bidi_mirrored = segments[9] == "Y"sv;
        data.unicode_1_name = segments[10];
        data.iso_comment = segments[11];
        data.simple_uppercase_mapping = AK::StringUtils::convert_to_uint_from_hex<u32>(segments[12]);
        data.simple_lowercase_mapping = AK::StringUtils::convert_to_uint_from_hex<u32>(segments[13]);
        data.simple_titlecase_mapping = AK::StringUtils::convert_to_uint_from_hex<u32>(segments[14]);

        if (auto abbreviation = unicode_data.code_point_abbreviations.get(data.code_point); abbreviation.has_value())
            data.abbreviation = *abbreviation;

        if (!assigned_code_point_range_start.has_value())
            assigned_code_point_range_start = data.code_point;

        if (data.name.starts_with("<"sv) && data.name.ends_with(", First>"sv)) {
            VERIFY(!code_point_range_start.has_value() && assigned_code_point_range_start.has_value());
            code_point_range_start = data.code_point;

            data.name = data.name.substring(1, data.name.length() - 9);

            assigned_code_points.append({ *assigned_code_point_range_start, previous_code_point });
            assigned_code_point_range_start.clear();
        } else if (data.name.starts_with("<"sv) && data.name.ends_with(", Last>"sv)) {
            VERIFY(code_point_range_start.has_value());

            CodePointRange code_point_range { *code_point_range_start, data.code_point };
            assigned_code_points.append(code_point_range);

            data.name = data.name.substring(1, data.name.length() - 8);
            code_point_range_start.clear();

            add_canonical_code_point_name(code_point_range, data.name, unicode_data);
        } else {
            add_canonical_code_point_name({ data.code_point, data.code_point }, data.name, unicode_data);

            if ((data.code_point > 0) && (data.code_point - previous_code_point) != 1) {
                VERIFY(assigned_code_point_range_start.has_value());

                assigned_code_points.append({ *assigned_code_point_range_start, previous_code_point });
                assigned_code_point_range_start = data.code_point;
            }
        }

        bool has_special_casing { false };
        for (auto const& casing : unicode_data.special_casing) {
            if (casing.code_point == data.code_point) {
                data.special_casing_indices.append(casing.index);
                has_special_casing = true;
            }
        }

        bool has_case_folding { false };
        for (size_t i = 0; i < unicode_data.case_folding.size(); ++i) {
            if (auto const& folding = unicode_data.case_folding[i]; folding.code_point == data.code_point) {
                data.case_folding_indices.append(i);
                has_case_folding = true;
            }
        }

        unicode_data.code_points_with_non_zero_combining_class += data.canonical_combining_class != 0;
        unicode_data.simple_uppercase_mapping_size += data.simple_uppercase_mapping.has_value();
        unicode_data.simple_lowercase_mapping_size += data.simple_lowercase_mapping.has_value();
        unicode_data.simple_titlecase_mapping_size += data.simple_titlecase_mapping.has_value();
        unicode_data.code_points_with_decomposition_mapping += data.decomposition_mapping.has_value();

        unicode_data.code_points_with_special_casing += has_special_casing;
        unicode_data.largest_special_casing_size = max(unicode_data.largest_special_casing_size, data.special_casing_indices.size());

        unicode_data.code_points_with_case_folding += has_case_folding;
        unicode_data.largest_case_folding_size = max(unicode_data.largest_case_folding_size, data.case_folding_indices.size());

        previous_code_point = data.code_point;
        unicode_data.code_point_data.append(move(data));
    }

    return {};
}

static ErrorOr<void> generate_unicode_data_header(Core::InputBufferedFile& file, UnicodeData& unicode_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.set("special_casing_mapping_size", DeprecatedString::number(unicode_data.largest_special_casing_mapping_size));
    generator.set("case_folding_mapping_size", DeprecatedString::number(unicode_data.largest_case_folding_mapping_size));

    auto generate_enum = [&](StringView name, StringView default_, auto values, Vector<Alias> aliases = {}) {
        quick_sort(values);
        quick_sort(aliases, [](auto& alias1, auto& alias2) { return alias1.alias < alias2.alias; });

        generator.set("name", name);
        generator.set("underlying", DeprecatedString::formatted("{}UnderlyingType", name));
        generator.set("type", ((values.size() + !default_.is_empty()) < 256) ? "u8"sv : "u16"sv);

        generator.append(R"~~~(
using @underlying@ = @type@;

enum class @name@ : @underlying@ {)~~~");

        if (!default_.is_empty()) {
            generator.set("default", default_);
            generator.append(R"~~~(
    @default@,)~~~");
        }

        for (auto const& value : values) {
            generator.set("value", value);
            generator.append(R"~~~(
    @value@,)~~~");
        }

        for (auto const& alias : aliases) {
            generator.set("alias", alias.alias);
            generator.set("value", alias.name);
            generator.append(R"~~~(
    @alias@ = @value@,)~~~");
        }

        generator.append(R"~~~(
};
)~~~");
    };

    generator.append(R"~~~(
#pragma once

#include <AK/Types.h>
#include <LibUnicode/Forward.h>

namespace Unicode {
)~~~");

    generate_enum("Locale"sv, "None"sv, unicode_data.locales);
    generate_enum("Condition"sv, "None"sv, move(unicode_data.conditions));
    generate_enum("CaseFoldingStatus"sv, {}, move(unicode_data.statuses));
    generate_enum("GeneralCategory"sv, {}, unicode_data.general_categories.keys(), unicode_data.general_category_aliases);
    generate_enum("Property"sv, {}, unicode_data.prop_list.keys(), unicode_data.prop_aliases);
    generate_enum("Script"sv, {}, unicode_data.script_list.keys(), unicode_data.script_aliases);
    generate_enum("GraphemeBreakProperty"sv, {}, unicode_data.grapheme_break_props.keys());
    generate_enum("WordBreakProperty"sv, {}, unicode_data.word_break_props.keys());
    generate_enum("SentenceBreakProperty"sv, {}, unicode_data.sentence_break_props.keys());
    generate_enum("CompatibilityFormattingTag"sv, "Canonical"sv, unicode_data.compatibility_tags);

    generator.append(R"~~~(
struct SpecialCasing {
    u32 code_point { 0 };

    u32 lowercase_mapping[@special_casing_mapping_size@];
    u32 lowercase_mapping_size { 0 };

    u32 uppercase_mapping[@special_casing_mapping_size@];
    u32 uppercase_mapping_size { 0 };

    u32 titlecase_mapping[@special_casing_mapping_size@];
    u32 titlecase_mapping_size { 0 };

    Locale locale { Locale::None };
    Condition condition { Condition::None };
};

struct CaseFolding {
    u32 code_point { 0 };
    CaseFoldingStatus status { CaseFoldingStatus::Common };

    u32 mapping[@case_folding_mapping_size@];
    u32 mapping_size { 0 };
};

struct CodePointDecompositionRaw {
    u32 code_point { 0 };
    CompatibilityFormattingTag tag { CompatibilityFormattingTag::Canonical };
    size_t decomposition_index { 0 };
    size_t decomposition_count { 0 };
};

struct CodePointDecomposition {
    u32 code_point { 0 };
    CompatibilityFormattingTag tag { CompatibilityFormattingTag::Canonical };
    ReadonlySpan<u32> decomposition;
};

Optional<Locale> locale_from_string(StringView locale);

ReadonlySpan<SpecialCasing const*> special_case_mapping(u32 code_point);
ReadonlySpan<CaseFolding const*> case_folding_mapping(u32 code_point);

}
)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

static ErrorOr<void> generate_unicode_data_implementation(Core::InputBufferedFile& file, UnicodeData const& unicode_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("string_index_type"sv, unicode_data.unique_strings.type_that_fits());
    generator.set("largest_special_casing_size", DeprecatedString::number(unicode_data.largest_special_casing_size));
    generator.set("special_casing_size", DeprecatedString::number(unicode_data.special_casing.size()));
    generator.set("largest_case_folding_size", DeprecatedString::number(unicode_data.largest_case_folding_size));
    generator.set("case_folding_size", DeprecatedString::number(unicode_data.case_folding.size()));

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/BinarySearch.h>
#include <AK/CharacterTypes.h>
#include <AK/Optional.h>
#include <AK/Span.h>
#include <AK/DeprecatedString.h>
#include <AK/StringView.h>
#include <LibUnicode/CharacterTypes.h>
#include <LibUnicode/UnicodeData.h>
#include <LibUnicode/Normalize.h>

namespace Unicode {
)~~~");

    unicode_data.unique_strings.generate(generator);

    auto append_list_and_size = [&](auto const& list, StringView format) {
        if (list.is_empty()) {
            generator.append(", {}, 0");
            return;
        }

        bool first = true;
        generator.append(", {");
        for (auto const& item : list) {
            generator.append(first ? " "sv : ", "sv);
            generator.append(DeprecatedString::formatted(format, item));
            first = false;
        }
        generator.append(DeprecatedString::formatted(" }}, {}", list.size()));
    };

    generator.append(R"~~~(
static constexpr Array<SpecialCasing, @special_casing_size@> s_special_case { {)~~~");

    for (auto const& casing : unicode_data.special_casing) {
        generator.set("code_point", DeprecatedString::formatted("{:#x}", casing.code_point));
        generator.append(R"~~~(
    { @code_point@)~~~");

        constexpr auto format = "0x{:x}"sv;
        append_list_and_size(casing.lowercase_mapping, format);
        append_list_and_size(casing.uppercase_mapping, format);
        append_list_and_size(casing.titlecase_mapping, format);

        generator.set("locale", casing.locale.is_empty() ? "None" : casing.locale);
        generator.append(", Locale::@locale@");

        generator.set("condition", casing.condition.is_empty() ? "None" : casing.condition);
        generator.append(", Condition::@condition@");

        generator.append(" },");
    }

    generator.append(R"~~~(
} };

static constexpr Array<CaseFolding, @case_folding_size@> s_case_folding { {)~~~");

    for (auto const& folding : unicode_data.case_folding) {
        generator.set("code_point", DeprecatedString::formatted("{:#x}", folding.code_point));
        generator.set("status", folding.status);
        generator.append(R"~~~(
    { @code_point@, CaseFoldingStatus::@status@)~~~");

        append_list_and_size(folding.mapping, "0x{:x}"sv);
        generator.append(" },");
    }

    generator.append(R"~~~(
} };

struct CodePointMapping {
    u32 code_point { 0 };
    u32 mapping { 0 };
};

struct SpecialCaseMapping {
    u32 code_point { 0 };
    Array<SpecialCasing const*, @largest_special_casing_size@> special_casing {};
    u32 special_casing_size { 0 };
};

struct CaseFoldingMapping {
    u32 code_point { 0 };
    Array<CaseFolding const*, @largest_case_folding_size@> case_folding {};
    u32 case_folding_size { 0 };
};

struct CodePointAbbreviation {
    u32 code_point { 0 };
    @string_index_type@ abbreviation { 0 };
};

template<typename MappingType>
struct CodePointComparator {
    constexpr int operator()(u32 code_point, MappingType const& mapping)
    {
        return code_point - mapping.code_point;
    }
};

struct BlockNameData {
    CodePointRange code_point_range {};
    @string_index_type@ display_name { 0 };
};

struct BlockNameComparator : public CodePointRangeComparator {
    constexpr int operator()(u32 code_point, BlockNameData const& name)
    {
        return CodePointRangeComparator::operator()(code_point, name.code_point_range);
    }
};

struct CodePointName {
    CodePointRange code_point_range {};
    @string_index_type@ display_name { 0 };
};

struct CodePointNameComparator : public CodePointRangeComparator {
    constexpr int operator()(u32 code_point, CodePointName const& name)
    {
        return CodePointRangeComparator::operator()(code_point, name.code_point_range);
    }
};
)~~~");

    generator.set("decomposition_mappings_size", DeprecatedString::number(unicode_data.decomposition_mappings.size()));
    generator.append("\nstatic constexpr Array<u32, @decomposition_mappings_size@> s_decomposition_mappings_data { ");
    generator.append(DeprecatedString::join(", "sv, unicode_data.decomposition_mappings, "{:#x}"sv));
    generator.append(" };\n");

    auto append_code_point_mappings = [&](StringView name, StringView mapping_type, u32 size, auto mapping_getter) {
        generator.set("name", name);
        generator.set("mapping_type", mapping_type);
        generator.set("size", DeprecatedString::number(size));

        generator.append(R"~~~(
static constexpr Array<@mapping_type@, @size@> s_@name@_mappings { {
    )~~~");

        constexpr size_t max_mappings_per_row = 20;
        size_t mappings_in_current_row = 0;

        for (auto const& data : unicode_data.code_point_data) {
            auto mapping = mapping_getter(data);

            if constexpr (requires { mapping.has_value(); }) {
                if (!mapping.has_value())
                    continue;
            } else {
                if (mapping.is_empty())
                    continue;
            }

            if (mappings_in_current_row++ > 0)
                generator.append(" ");

            generator.set("code_point", DeprecatedString::formatted("{:#x}", data.code_point));
            generator.append("{ @code_point@");

            if constexpr (IsSame<decltype(mapping), Optional<u32>> || IsSame<decltype(mapping), Optional<size_t>>) {
                generator.set("mapping", DeprecatedString::formatted("{:#x}", *mapping));
                generator.append(", @mapping@ },");
            } else if constexpr (IsSame<decltype(mapping), Optional<CodePointDecomposition>>) {
                generator.set("tag", mapping->tag);
                generator.set("start", DeprecatedString::number(mapping->decomposition_index));
                generator.set("size", DeprecatedString::number(mapping->decomposition_size));
                generator.append(", CompatibilityFormattingTag::@tag@, @start@, @size@ },");
            } else {
                append_list_and_size(mapping, "&s_@name@[{}]"sv);
                generator.append(" },");
            }

            if (mappings_in_current_row == max_mappings_per_row) {
                mappings_in_current_row = 0;
                generator.append("\n    ");
            }
        }
        generator.append(R"~~~(
} };
)~~~");
    };

    append_code_point_mappings("combining_class"sv, "CodePointMapping"sv, unicode_data.code_points_with_non_zero_combining_class,
        [](auto const& data) -> Optional<u32> {
            if (data.canonical_combining_class == 0)
                return {};
            return data.canonical_combining_class;
        });
    append_code_point_mappings("uppercase"sv, "CodePointMapping"sv, unicode_data.simple_uppercase_mapping_size, [](auto const& data) { return data.simple_uppercase_mapping; });
    append_code_point_mappings("lowercase"sv, "CodePointMapping"sv, unicode_data.simple_lowercase_mapping_size, [](auto const& data) { return data.simple_lowercase_mapping; });
    append_code_point_mappings("titlecase"sv, "CodePointMapping"sv, unicode_data.simple_titlecase_mapping_size, [](auto const& data) { return data.simple_titlecase_mapping; });
    append_code_point_mappings("special_case"sv, "SpecialCaseMapping"sv, unicode_data.code_points_with_special_casing, [](auto const& data) { return data.special_casing_indices; });
    append_code_point_mappings("case_folding"sv, "CaseFoldingMapping"sv, unicode_data.code_points_with_case_folding, [](auto const& data) { return data.case_folding_indices; });
    append_code_point_mappings("abbreviation"sv, "CodePointAbbreviation"sv, unicode_data.code_point_abbreviations.size(), [](auto const& data) { return data.abbreviation; });

    append_code_point_mappings("decomposition"sv, "CodePointDecompositionRaw"sv, unicode_data.code_points_with_decomposition_mapping,
        [](auto const& data) {
            return data.decomposition_mapping;
        });

    auto append_code_point_range_list = [&](DeprecatedString name, Vector<CodePointRange> const& ranges) {
        generator.set("name", name);
        generator.set("size", DeprecatedString::number(ranges.size()));
        generator.append(R"~~~(
static constexpr Array<CodePointRange, @size@> @name@ { {
    )~~~");

        constexpr size_t max_ranges_per_row = 20;
        size_t ranges_in_current_row = 0;

        for (auto const& range : ranges) {
            if (ranges_in_current_row++ > 0)
                generator.append(" ");

            generator.set("first", DeprecatedString::formatted("{:#x}", range.first));
            generator.set("last", DeprecatedString::formatted("{:#x}", range.last));
            generator.append("{ @first@, @last@ },");

            if (ranges_in_current_row == max_ranges_per_row) {
                ranges_in_current_row = 0;
                generator.append("\n    ");
            }
        }

        generator.append(R"~~~(
} };
)~~~");
    };

    auto append_prop_list = [&](StringView collection_name, StringView property_format, PropList const& property_list) {
        for (auto const& property : property_list) {
            auto name = DeprecatedString::formatted(property_format, property.key);
            append_code_point_range_list(move(name), property.value);
        }

        auto property_names = property_list.keys();
        quick_sort(property_names);

        generator.set("name", collection_name);
        generator.set("size", DeprecatedString::number(property_names.size()));
        generator.append(R"~~~(
static constexpr Array<ReadonlySpan<CodePointRange>, @size@> @name@ { {)~~~");

        for (auto const& property_name : property_names) {
            generator.set("name", DeprecatedString::formatted(property_format, property_name));
            generator.append(R"~~~(
    @name@.span(),)~~~");
        }

        generator.append(R"~~~(
} };
)~~~");
    };

    append_prop_list("s_general_categories"sv, "s_general_category_{}"sv, unicode_data.general_categories);
    append_prop_list("s_properties"sv, "s_property_{}"sv, unicode_data.prop_list);
    append_prop_list("s_scripts"sv, "s_script_{}"sv, unicode_data.script_list);
    append_prop_list("s_script_extensions"sv, "s_script_extension_{}"sv, unicode_data.script_extensions);
    append_prop_list("s_grapheme_break_properties"sv, "s_grapheme_break_property_{}"sv, unicode_data.grapheme_break_props);
    append_prop_list("s_word_break_properties"sv, "s_word_break_property_{}"sv, unicode_data.word_break_props);
    append_prop_list("s_sentence_break_properties"sv, "s_sentence_break_property_{}"sv, unicode_data.sentence_break_props);

    auto append_code_point_display_names = [&](StringView type, StringView name, auto const& display_names) {
        constexpr size_t max_values_per_row = 30;
        size_t values_in_current_row = 0;

        generator.set("type", type);
        generator.set("name", name);
        generator.set("size", DeprecatedString::number(display_names.size()));

        generator.append(R"~~~(
static constexpr Array<@type@, @size@> @name@ { {
    )~~~");
        for (auto const& display_name : display_names) {
            if (values_in_current_row++ > 0)
                generator.append(", ");

            generator.set("first", DeprecatedString::formatted("{:#x}", display_name.code_point_range.first));
            generator.set("last", DeprecatedString::formatted("{:#x}", display_name.code_point_range.last));
            generator.set("name", DeprecatedString::number(display_name.name));
            generator.append("{ { @first@, @last@ }, @name@ }");

            if (values_in_current_row == max_values_per_row) {
                values_in_current_row = 0;
                generator.append(",\n    ");
            }
        }
        generator.append(R"~~~(
} };
)~~~");
    };

    append_code_point_display_names("BlockNameData"sv, "s_block_display_names"sv, unicode_data.block_display_names);
    append_code_point_display_names("CodePointName"sv, "s_code_point_display_names"sv, unicode_data.code_point_display_names);

    generator.append(R"~~~(
Optional<StringView> code_point_block_display_name(u32 code_point)
{
    if (auto const* entry = binary_search(s_block_display_names, code_point, nullptr, BlockNameComparator {}))
        return decode_string(entry->display_name);

    return {};
}

ReadonlySpan<BlockName> block_display_names()
{
    static auto display_names = []() {
        Array<BlockName, s_block_display_names.size()> display_names;

        for (size_t i = 0; i < s_block_display_names.size(); ++i) {
            auto const& display_name = s_block_display_names[i];
            display_names[i] = { display_name.code_point_range, decode_string(display_name.display_name) };
        }

        return display_names;
    }();

    return display_names.span();
}

Optional<DeprecatedString> code_point_display_name(u32 code_point)
{
    if (auto const* entry = binary_search(s_code_point_display_names, code_point, nullptr, CodePointNameComparator {})) {
        auto display_name = decode_string(entry->display_name);

        if (display_name.ends_with("{:X}"sv))
            return DeprecatedString::formatted(display_name, code_point);

        return display_name;
    }

    return {};
}
)~~~");

    auto append_code_point_mapping_search = [&](StringView method, StringView mappings, StringView fallback) {
        generator.set("method", method);
        generator.set("mappings", mappings);
        generator.set("fallback", fallback);
        generator.append(R"~~~(
u32 @method@(u32 code_point)
{
    auto const* mapping = binary_search(@mappings@, code_point, nullptr, CodePointComparator<CodePointMapping> {});
    return mapping ? mapping->mapping : @fallback@;
}
)~~~");
    };

    append_code_point_mapping_search("canonical_combining_class"sv, "s_combining_class_mappings"sv, "0"sv);
    append_code_point_mapping_search("to_unicode_uppercase"sv, "s_uppercase_mappings"sv, "code_point"sv);
    append_code_point_mapping_search("to_unicode_lowercase"sv, "s_lowercase_mappings"sv, "code_point"sv);
    append_code_point_mapping_search("to_unicode_titlecase"sv, "s_titlecase_mappings"sv, "code_point"sv);

    generator.append(R"~~~(
ReadonlySpan<SpecialCasing const*> special_case_mapping(u32 code_point)
{
    auto const* mapping = binary_search(s_special_case_mappings, code_point, nullptr, CodePointComparator<SpecialCaseMapping> {});
    if (mapping == nullptr)
        return {};

    return mapping->special_casing.span().slice(0, mapping->special_casing_size);
}

ReadonlySpan<CaseFolding const*> case_folding_mapping(u32 code_point)
{
    auto const* mapping = binary_search(s_case_folding_mappings, code_point, nullptr, CodePointComparator<CaseFoldingMapping> {});
    if (mapping == nullptr)
        return {};

    return mapping->case_folding.span().slice(0, mapping->case_folding_size);
}

Optional<StringView> code_point_abbreviation(u32 code_point)
{
    auto const* mapping = binary_search(s_abbreviation_mappings, code_point, nullptr, CodePointComparator<CodePointAbbreviation> {});
    if (mapping == nullptr)
        return {};
    if (mapping->abbreviation == 0)
        return {};

    return decode_string(mapping->abbreviation);
}

Optional<CodePointDecomposition const> code_point_decomposition(u32 code_point)
{
    auto const* mapping = binary_search(s_decomposition_mappings, code_point, nullptr, CodePointComparator<CodePointDecompositionRaw> {});
    if (mapping == nullptr)
        return {};
    return CodePointDecomposition { mapping->code_point, mapping->tag, ReadonlySpan<u32> { s_decomposition_mappings_data.data() + mapping->decomposition_index, mapping->decomposition_count } };
}

Optional<CodePointDecomposition const> code_point_decomposition_by_index(size_t index)
{
    if (index >= s_decomposition_mappings.size())
        return {};
    auto const& mapping = s_decomposition_mappings[index];
    return CodePointDecomposition { mapping.code_point, mapping.tag, ReadonlySpan<u32> { s_decomposition_mappings_data.data() + mapping.decomposition_index, mapping.decomposition_count } };
}
)~~~");

    auto append_prop_search = [&](StringView enum_title, StringView enum_snake, StringView collection_name) {
        generator.set("enum_title", enum_title);
        generator.set("enum_snake", enum_snake);
        generator.set("collection_name", collection_name);
        generator.append(R"~~~(
bool code_point_has_@enum_snake@(u32 code_point, @enum_title@ @enum_snake@)
{
    auto index = static_cast<@enum_title@UnderlyingType>(@enum_snake@);
    auto const& ranges = @collection_name@.at(index);

    auto const* range = binary_search(ranges, code_point, nullptr, CodePointRangeComparator {});
    return range != nullptr;
}
)~~~");
    };

    auto append_from_string = [&](StringView enum_title, StringView enum_snake, auto const& prop_list, Vector<Alias> const& aliases) -> ErrorOr<void> {
        HashValueMap<StringView> hashes;
        TRY(hashes.try_ensure_capacity(prop_list.size() + aliases.size()));

        ValueFromStringOptions options {};

        for (auto const& prop : prop_list) {
            if constexpr (IsSame<RemoveCVReference<decltype(prop)>, DeprecatedString>) {
                hashes.set(CaseInsensitiveASCIIStringViewTraits::hash(prop), prop);
                options.sensitivity = CaseSensitivity::CaseInsensitive;
            } else {
                hashes.set(prop.key.hash(), prop.key);
            }
        }

        for (auto const& alias : aliases)
            hashes.set(alias.alias.hash(), alias.alias);

        generate_value_from_string(generator, "{}_from_string"sv, enum_title, enum_snake, move(hashes), options);

        return {};
    };

    TRY(append_from_string("Locale"sv, "locale"sv, unicode_data.locales, {}));

    append_prop_search("GeneralCategory"sv, "general_category"sv, "s_general_categories"sv);
    TRY(append_from_string("GeneralCategory"sv, "general_category"sv, unicode_data.general_categories, unicode_data.general_category_aliases));

    append_prop_search("Property"sv, "property"sv, "s_properties"sv);
    TRY(append_from_string("Property"sv, "property"sv, unicode_data.prop_list, unicode_data.prop_aliases));

    append_prop_search("Script"sv, "script"sv, "s_scripts"sv);
    append_prop_search("Script"sv, "script_extension"sv, "s_script_extensions"sv);
    TRY(append_from_string("Script"sv, "script"sv, unicode_data.script_list, unicode_data.script_aliases));

    append_prop_search("GraphemeBreakProperty"sv, "grapheme_break_property"sv, "s_grapheme_break_properties"sv);
    append_prop_search("WordBreakProperty"sv, "word_break_property"sv, "s_word_break_properties"sv);
    append_prop_search("SentenceBreakProperty"sv, "sentence_break_property"sv, "s_sentence_break_properties"sv);

    generator.append(R"~~~(
}
)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

static Vector<u32> flatten_code_point_ranges(Vector<CodePointRange> const& code_points)
{
    Vector<u32> flattened;

    for (auto const& range : code_points) {
        flattened.grow_capacity(range.last - range.first);
        for (u32 code_point = range.first; code_point <= range.last; ++code_point)
            flattened.append(code_point);
    }

    return flattened;
}

static Vector<CodePointRange> form_code_point_ranges(Vector<u32> code_points)
{
    Vector<CodePointRange> ranges;

    u32 range_start = code_points[0];
    u32 range_end = range_start;

    for (size_t i = 1; i < code_points.size(); ++i) {
        u32 code_point = code_points[i];

        if ((code_point - range_end) == 1) {
            range_end = code_point;
        } else {
            ranges.append({ range_start, range_end });
            range_start = code_point;
            range_end = code_point;
        }
    }

    ranges.append({ range_start, range_end });
    return ranges;
}

static void sort_and_merge_code_point_ranges(Vector<CodePointRange>& code_points)
{
    quick_sort(code_points, [](auto const& range1, auto const& range2) {
        return range1.first < range2.first;
    });

    for (size_t i = 0; i < code_points.size() - 1;) {
        if (code_points[i].last >= code_points[i + 1].first) {
            code_points[i].last = max(code_points[i].last, code_points[i + 1].last);
            code_points.remove(i + 1);
        } else {
            ++i;
        }
    }

    auto all_code_points = flatten_code_point_ranges(code_points);
    code_points = form_code_point_ranges(all_code_points);
}

static void populate_general_category_unions(PropList& general_categories)
{
    // The Unicode standard defines General Category values which are not in any UCD file. These
    // values are simply unions of other values.
    // https://www.unicode.org/reports/tr44/#GC_Values_Table
    auto populate_union = [&](auto alias, auto categories) {
        auto& code_points = general_categories.ensure(alias);
        for (auto const& category : categories)
            code_points.extend(general_categories.find(category)->value);

        sort_and_merge_code_point_ranges(code_points);
    };

    populate_union("LC"sv, Array { "Ll"sv, "Lu"sv, "Lt"sv });
    populate_union("L"sv, Array { "Lu"sv, "Ll"sv, "Lt"sv, "Lm"sv, "Lo"sv });
    populate_union("M"sv, Array { "Mn"sv, "Mc"sv, "Me"sv });
    populate_union("N"sv, Array { "Nd"sv, "Nl"sv, "No"sv });
    populate_union("P"sv, Array { "Pc"sv, "Pd"sv, "Ps"sv, "Pe"sv, "Pi"sv, "Pf"sv, "Po"sv });
    populate_union("S"sv, Array { "Sm"sv, "Sc"sv, "Sk"sv, "So"sv });
    populate_union("Z"sv, Array { "Zs"sv, "Zl"sv, "Zp"sv });
    populate_union("C"sv, Array { "Cc"sv, "Cf"sv, "Cs"sv, "Co"sv, "Cn"sv });
}

static ErrorOr<void> normalize_script_extensions(PropList& script_extensions, PropList const& script_list, Vector<Alias> const& script_aliases)
{
    // The ScriptExtensions UCD file lays out its code point ranges rather uniquely compared to
    // other files. The Script listed on each line may either be a full Script string or an aliased
    // abbreviation. Further, the extensions may or may not include the base Script list. Normalize
    // the extensions here to be keyed by the full Script name and always include the base list.
    auto extensions = move(script_extensions);
    script_extensions = TRY(script_list.clone());

    for (auto const& extension : extensions) {
        auto it = find_if(script_aliases.begin(), script_aliases.end(), [&](auto const& alias) { return extension.key == alias.alias; });
        auto const& key = (it == script_aliases.end()) ? extension.key : it->name;

        auto& code_points = script_extensions.find(key)->value;
        code_points.extend(extension.value);

        sort_and_merge_code_point_ranges(code_points);
    }

    // Lastly, the Common and Inherited script extensions are special. They must not contain any
    // code points which appear in other script extensions. The ScriptExtensions UCD file does not
    // list these extensions, therefore this peculiarity must be handled programmatically.
    // https://www.unicode.org/reports/tr24/#Assignment_ScriptX_Values
    auto code_point_has_other_extension = [&](StringView key, u32 code_point) {
        for (auto const& extension : extensions) {
            if (extension.key == key)
                continue;
            if (any_of(extension.value, [&](auto const& r) { return (r.first <= code_point) && (code_point <= r.last); }))
                return true;
        }

        return false;
    };

    auto get_code_points_without_other_extensions = [&](StringView key) {
        auto code_points = flatten_code_point_ranges(script_list.find(key)->value);
        code_points.remove_all_matching([&](u32 c) { return code_point_has_other_extension(key, c); });
        return code_points;
    };

    auto common_code_points = get_code_points_without_other_extensions("Common"sv);
    script_extensions.set("Common"sv, form_code_point_ranges(common_code_points));

    auto inherited_code_points = get_code_points_without_other_extensions("Inherited"sv);
    script_extensions.set("Inherited"sv, form_code_point_ranges(inherited_code_points));
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView unicode_data_path;
    StringView special_casing_path;
    StringView case_folding_path;
    StringView derived_general_category_path;
    StringView prop_list_path;
    StringView derived_core_prop_path;
    StringView derived_binary_prop_path;
    StringView prop_alias_path;
    StringView prop_value_alias_path;
    StringView name_alias_path;
    StringView scripts_path;
    StringView script_extensions_path;
    StringView blocks_path;
    StringView emoji_data_path;
    StringView normalization_path;
    StringView grapheme_break_path;
    StringView word_break_path;
    StringView sentence_break_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the Unicode Data header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the Unicode Data implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(unicode_data_path, "Path to UnicodeData.txt file", "unicode-data-path", 'u', "unicode-data-path");
    args_parser.add_option(special_casing_path, "Path to SpecialCasing.txt file", "special-casing-path", 's', "special-casing-path");
    args_parser.add_option(case_folding_path, "Path to CaseFolding.txt file", "case-folding-path", 'o', "case-folding-path");
    args_parser.add_option(derived_general_category_path, "Path to DerivedGeneralCategory.txt file", "derived-general-category-path", 'g', "derived-general-category-path");
    args_parser.add_option(prop_list_path, "Path to PropList.txt file", "prop-list-path", 'p', "prop-list-path");
    args_parser.add_option(derived_core_prop_path, "Path to DerivedCoreProperties.txt file", "derived-core-prop-path", 'd', "derived-core-prop-path");
    args_parser.add_option(derived_binary_prop_path, "Path to DerivedBinaryProperties.txt file", "derived-binary-prop-path", 'b', "derived-binary-prop-path");
    args_parser.add_option(prop_alias_path, "Path to PropertyAliases.txt file", "prop-alias-path", 'a', "prop-alias-path");
    args_parser.add_option(prop_value_alias_path, "Path to PropertyValueAliases.txt file", "prop-value-alias-path", 'v', "prop-value-alias-path");
    args_parser.add_option(name_alias_path, "Path to NameAliases.txt file", "name-alias-path", 'm', "name-alias-path");
    args_parser.add_option(scripts_path, "Path to Scripts.txt file", "scripts-path", 'r', "scripts-path");
    args_parser.add_option(script_extensions_path, "Path to ScriptExtensions.txt file", "script-extensions-path", 'x', "script-extensions-path");
    args_parser.add_option(blocks_path, "Path to Blocks.txt file", "blocks-path", 'k', "blocks-path");
    args_parser.add_option(emoji_data_path, "Path to emoji-data.txt file", "emoji-data-path", 'e', "emoji-data-path");
    args_parser.add_option(normalization_path, "Path to DerivedNormalizationProps.txt file", "normalization-path", 'n', "normalization-path");
    args_parser.add_option(grapheme_break_path, "Path to GraphemeBreakProperty.txt file", "grapheme-break-path", 'f', "grapheme-break-path");
    args_parser.add_option(word_break_path, "Path to WordBreakProperty.txt file", "word-break-path", 'w', "word-break-path");
    args_parser.add_option(sentence_break_path, "Path to SentenceBreakProperty.txt file", "sentence-break-path", 'i', "sentence-break-path");
    args_parser.parse(arguments);

    auto generated_header_file = TRY(open_file(generated_header_path, Core::File::OpenMode::Write));
    auto generated_implementation_file = TRY(open_file(generated_implementation_path, Core::File::OpenMode::Write));
    auto unicode_data_file = TRY(open_file(unicode_data_path, Core::File::OpenMode::Read));
    auto derived_general_category_file = TRY(open_file(derived_general_category_path, Core::File::OpenMode::Read));
    auto special_casing_file = TRY(open_file(special_casing_path, Core::File::OpenMode::Read));
    auto case_folding_file = TRY(open_file(case_folding_path, Core::File::OpenMode::Read));
    auto prop_list_file = TRY(open_file(prop_list_path, Core::File::OpenMode::Read));
    auto derived_core_prop_file = TRY(open_file(derived_core_prop_path, Core::File::OpenMode::Read));
    auto derived_binary_prop_file = TRY(open_file(derived_binary_prop_path, Core::File::OpenMode::Read));
    auto prop_alias_file = TRY(open_file(prop_alias_path, Core::File::OpenMode::Read));
    auto prop_value_alias_file = TRY(open_file(prop_value_alias_path, Core::File::OpenMode::Read));
    auto name_alias_file = TRY(open_file(name_alias_path, Core::File::OpenMode::Read));
    auto scripts_file = TRY(open_file(scripts_path, Core::File::OpenMode::Read));
    auto script_extensions_file = TRY(open_file(script_extensions_path, Core::File::OpenMode::Read));
    auto blocks_file = TRY(open_file(blocks_path, Core::File::OpenMode::Read));
    auto emoji_data_file = TRY(open_file(emoji_data_path, Core::File::OpenMode::Read));
    auto normalization_file = TRY(open_file(normalization_path, Core::File::OpenMode::Read));
    auto grapheme_break_file = TRY(open_file(grapheme_break_path, Core::File::OpenMode::Read));
    auto word_break_file = TRY(open_file(word_break_path, Core::File::OpenMode::Read));
    auto sentence_break_file = TRY(open_file(sentence_break_path, Core::File::OpenMode::Read));

    UnicodeData unicode_data {};
    TRY(parse_special_casing(*special_casing_file, unicode_data));
    TRY(parse_case_folding(*case_folding_file, unicode_data));
    TRY(parse_prop_list(*derived_general_category_file, unicode_data.general_categories));
    TRY(parse_prop_list(*prop_list_file, unicode_data.prop_list));
    TRY(parse_prop_list(*derived_core_prop_file, unicode_data.prop_list));
    TRY(parse_prop_list(*derived_binary_prop_file, unicode_data.prop_list));
    TRY(parse_prop_list(*emoji_data_file, unicode_data.prop_list));
    TRY(parse_normalization_props(*normalization_file, unicode_data));
    TRY(parse_alias_list(*prop_alias_file, unicode_data.prop_list, unicode_data.prop_aliases));
    TRY(parse_prop_list(*scripts_file, unicode_data.script_list));
    TRY(parse_prop_list(*script_extensions_file, unicode_data.script_extensions, true));
    TRY(parse_block_display_names(*blocks_file, unicode_data));
    TRY(parse_name_aliases(*name_alias_file, unicode_data));
    TRY(parse_prop_list(*grapheme_break_file, unicode_data.grapheme_break_props));
    TRY(parse_prop_list(*word_break_file, unicode_data.word_break_props));
    TRY(parse_prop_list(*sentence_break_file, unicode_data.sentence_break_props));

    populate_general_category_unions(unicode_data.general_categories);
    TRY(parse_unicode_data(*unicode_data_file, unicode_data));
    TRY(parse_value_alias_list(*prop_value_alias_file, "gc"sv, unicode_data.general_categories.keys(), unicode_data.general_category_aliases));
    TRY(parse_value_alias_list(*prop_value_alias_file, "sc"sv, unicode_data.script_list.keys(), unicode_data.script_aliases, false));
    TRY(normalize_script_extensions(unicode_data.script_extensions, unicode_data.script_list, unicode_data.script_aliases));

    TRY(generate_unicode_data_header(*generated_header_file, unicode_data));
    TRY(generate_unicode_data_implementation(*generated_implementation_file, unicode_data));

    return 0;
}
