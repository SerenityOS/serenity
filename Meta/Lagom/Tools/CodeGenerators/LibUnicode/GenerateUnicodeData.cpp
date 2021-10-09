/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AllOf.h>
#include <AK/Array.h>
#include <AK/CharacterTypes.h>
#include <AK/Find.h>
#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/QuickSort.h>
#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/StringUtils.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>

// Some code points are excluded from UnicodeData.txt, and instead are part of a "range" of code
// points, as indicated by the "name" field. For example:
//     3400;<CJK Ideograph Extension A, First>;Lo;0;L;;;;;N;;;;;
//     4DBF;<CJK Ideograph Extension A, Last>;Lo;0;L;;;;;N;;;;;
struct CodePointRange {
    u32 first;
    u32 last;
};

// SpecialCasing source: https://www.unicode.org/Public/13.0.0/ucd/SpecialCasing.txt
// Field descriptions: https://www.unicode.org/reports/tr44/tr44-13.html#SpecialCasing.txt
struct SpecialCasing {
    u32 index { 0 };
    u32 code_point { 0 };
    Vector<u32> lowercase_mapping;
    Vector<u32> uppercase_mapping;
    Vector<u32> titlecase_mapping;
    String locale;
    String condition;
};

// PropList source: https://www.unicode.org/Public/13.0.0/ucd/PropList.txt
// Property descriptions: https://www.unicode.org/reports/tr44/tr44-13.html#PropList.txt
using PropList = HashMap<String, Vector<CodePointRange>>;

// PropertyAliases source: https://www.unicode.org/Public/13.0.0/ucd/PropertyAliases.txt
struct Alias {
    String property;
    String alias;
};

// Normalization source: https://www.unicode.org/Public/13.0.0/ucd/DerivedNormalizationProps.txt
// Normalization descriptions: https://www.unicode.org/reports/tr44/#DerivedNormalizationProps.txt
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

using NormalizationProps = HashMap<String, Vector<Normalization>>;

// UnicodeData source: https://www.unicode.org/Public/13.0.0/ucd/UnicodeData.txt
// Field descriptions: https://www.unicode.org/reports/tr44/tr44-13.html#UnicodeData.txt
//                     https://www.unicode.org/reports/tr44/#General_Category_Values
struct CodePointData {
    u32 code_point { 0 };
    String name;
    u8 canonical_combining_class { 0 };
    String bidi_class;
    String decomposition_type;
    Optional<i8> numeric_value_decimal;
    Optional<i8> numeric_value_digit;
    Optional<i8> numeric_value_numeric;
    bool bidi_mirrored { false };
    String unicode_1_name;
    String iso_comment;
    Optional<u32> simple_uppercase_mapping;
    Optional<u32> simple_lowercase_mapping;
    Optional<u32> simple_titlecase_mapping;
    Vector<u32> special_casing_indices;
};

struct UnicodeData {
    u32 simple_uppercase_mapping_size { 0 };
    u32 simple_lowercase_mapping_size { 0 };

    Vector<SpecialCasing> special_casing;
    u32 largest_casing_transform_size { 0 };
    u32 largest_special_casing_size { 0 };
    Vector<String> conditions;

    Vector<CodePointData> code_point_data;
    Vector<CodePointRange> code_point_ranges;

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

    // FIXME: We are not yet doing anything with this data. It will be needed for String.prototype.normalize.
    NormalizationProps normalization_props;
};

static constexpr auto s_desired_fields = Array {
    "canonical_combining_class"sv,
    "simple_uppercase_mapping"sv,
    "simple_lowercase_mapping"sv,
};

static Vector<u32> parse_code_point_list(StringView const& list)
{
    Vector<u32> code_points;

    auto segments = list.split_view(' ');
    for (auto const& code_point : segments)
        code_points.append(AK::StringUtils::convert_to_uint_from_hex<u32>(code_point).value());

    return code_points;
}

static CodePointRange parse_code_point_range(StringView const& list)
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

static void parse_special_casing(Core::File& file, UnicodeData& unicode_data)
{
    while (file.can_read_line()) {
        auto line = file.read_line();
        if (line.is_empty() || line.starts_with('#'))
            continue;

        if (auto index = line.find('#'); index.has_value())
            line = line.substring(0, *index);

        auto segments = line.split(';', true);
        VERIFY(segments.size() == 5 || segments.size() == 6);

        SpecialCasing casing {};
        casing.code_point = AK::StringUtils::convert_to_uint_from_hex<u32>(segments[0]).value();
        casing.lowercase_mapping = parse_code_point_list(segments[1]);
        casing.titlecase_mapping = parse_code_point_list(segments[2]);
        casing.uppercase_mapping = parse_code_point_list(segments[3]);

        if (auto condition = segments[4].trim_whitespace(); !condition.is_empty()) {
            auto conditions = condition.split(' ', true);
            VERIFY(conditions.size() == 1 || conditions.size() == 2);

            if (conditions.size() == 2) {
                casing.locale = move(conditions[0]);
                casing.condition = move(conditions[1]);
            } else if (all_of(conditions[0], is_ascii_lower_alpha)) {
                casing.locale = move(conditions[0]);
            } else {
                casing.condition = move(conditions[0]);
            }

            if (!casing.locale.is_empty())
                casing.locale = String::formatted("{:c}{}", to_ascii_uppercase(casing.locale[0]), casing.locale.substring_view(1));
            casing.condition = casing.condition.replace("_", "", true);

            if (!casing.condition.is_empty() && !unicode_data.conditions.contains_slow(casing.condition))
                unicode_data.conditions.append(casing.condition);
        }

        unicode_data.largest_casing_transform_size = max(unicode_data.largest_casing_transform_size, casing.lowercase_mapping.size());
        unicode_data.largest_casing_transform_size = max(unicode_data.largest_casing_transform_size, casing.titlecase_mapping.size());
        unicode_data.largest_casing_transform_size = max(unicode_data.largest_casing_transform_size, casing.uppercase_mapping.size());

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
}

static void parse_prop_list(Core::File& file, PropList& prop_list, bool multi_value_property = false)
{
    while (file.can_read_line()) {
        auto line = file.read_line();
        if (line.is_empty() || line.starts_with('#'))
            continue;

        if (auto index = line.find('#'); index.has_value())
            line = line.substring(0, *index);

        auto segments = line.split_view(';', true);
        VERIFY(segments.size() == 2);

        auto code_point_range = parse_code_point_range(segments[0].trim_whitespace());
        Vector<StringView> properties;

        if (multi_value_property)
            properties = segments[1].trim_whitespace().split_view(' ');
        else
            properties = { segments[1].trim_whitespace() };

        for (auto const& property : properties) {
            auto& code_points = prop_list.ensure(property.trim_whitespace());
            code_points.append(code_point_range);
        }
    }
}

static void parse_alias_list(Core::File& file, PropList const& prop_list, Vector<Alias>& prop_aliases)
{
    String current_property;

    auto append_alias = [&](auto alias, auto property) {
        // Note: The alias files contain lines such as "Hyphen = Hyphen", which we should just skip.
        if (alias == property)
            return;

        // FIXME: We will, eventually, need to find where missing properties are located and parse them.
        if (!prop_list.contains(property))
            return;

        prop_aliases.append({ property, alias });
    };

    while (file.can_read_line()) {
        auto line = file.read_line();
        if (line.is_empty() || line.starts_with('#')) {
            if (line.ends_with("Properties"sv))
                current_property = line.substring(2);
            continue;
        }

        // Note: For now, we only care about Binary Property aliases for Unicode property escapes.
        if (current_property != "Binary Properties"sv)
            continue;

        auto segments = line.split_view(';', true);
        VERIFY((segments.size() == 2) || (segments.size() == 3));

        auto alias = segments[0].trim_whitespace();
        auto property = segments[1].trim_whitespace();
        append_alias(alias, property);

        if (segments.size() == 3) {
            alias = segments[2].trim_whitespace();
            append_alias(alias, property);
        }
    }
}

static void parse_value_alias_list(Core::File& file, StringView desired_category, Vector<String> const& value_list, Vector<Alias>& prop_aliases, bool primary_value_is_first = true)
{
    VERIFY(file.seek(0));

    auto append_alias = [&](auto alias, auto value) {
        // Note: The value alias file contains lines such as "Ahom = Ahom", which we should just skip.
        if (alias == value)
            return;

        // FIXME: We will, eventually, need to find where missing properties are located and parse them.
        if (!value_list.contains_slow(value))
            return;

        prop_aliases.append({ value, alias });
    };

    while (file.can_read_line()) {
        auto line = file.read_line();
        if (line.is_empty() || line.starts_with('#'))
            continue;

        if (auto index = line.find('#'); index.has_value())
            line = line.substring(0, *index);

        auto segments = line.split_view(';', true);
        auto category = segments[0].trim_whitespace();

        if (category != desired_category)
            continue;

        VERIFY((segments.size() == 3) || (segments.size() == 4));
        auto value = primary_value_is_first ? segments[1].trim_whitespace() : segments[2].trim_whitespace();
        auto alias = primary_value_is_first ? segments[2].trim_whitespace() : segments[1].trim_whitespace();
        append_alias(alias, value);

        if (segments.size() == 4) {
            alias = segments[3].trim_whitespace();
            append_alias(alias, value);
        }
    }
}

static void parse_normalization_props(Core::File& file, UnicodeData& unicode_data)
{
    while (file.can_read_line()) {
        auto line = file.read_line();
        if (line.is_empty() || line.starts_with('#'))
            continue;

        if (auto index = line.find('#'); index.has_value())
            line = line.substring(0, *index);

        auto segments = line.split_view(';', true);
        VERIFY((segments.size() == 2) || (segments.size() == 3));

        auto code_point_range = parse_code_point_range(segments[0].trim_whitespace());
        auto property = segments[1].trim_whitespace().to_string();

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
}

static void parse_unicode_data(Core::File& file, UnicodeData& unicode_data)
{
    Optional<u32> code_point_range_start;

    auto& assigned_code_points = unicode_data.prop_list.find("Assigned"sv)->value;
    Optional<u32> assigned_code_point_range_start = 0;
    u32 previous_code_point = 0;

    while (file.can_read_line()) {
        auto line = file.read_line();
        if (line.is_empty())
            continue;

        auto segments = line.split(';', true);
        VERIFY(segments.size() == 15);

        CodePointData data {};
        data.code_point = AK::StringUtils::convert_to_uint_from_hex<u32>(segments[0]).value();
        data.name = move(segments[1]);
        data.canonical_combining_class = AK::StringUtils::convert_to_uint<u8>(segments[3]).value();
        data.bidi_class = move(segments[4]);
        data.decomposition_type = move(segments[5]);
        data.numeric_value_decimal = AK::StringUtils::convert_to_int<i8>(segments[6]);
        data.numeric_value_digit = AK::StringUtils::convert_to_int<i8>(segments[7]);
        data.numeric_value_numeric = AK::StringUtils::convert_to_int<i8>(segments[8]);
        data.bidi_mirrored = segments[9] == "Y"sv;
        data.unicode_1_name = move(segments[10]);
        data.iso_comment = move(segments[11]);
        data.simple_uppercase_mapping = AK::StringUtils::convert_to_uint_from_hex<u32>(segments[12]);
        data.simple_lowercase_mapping = AK::StringUtils::convert_to_uint_from_hex<u32>(segments[13]);
        data.simple_titlecase_mapping = AK::StringUtils::convert_to_uint_from_hex<u32>(segments[14]);

        if (!assigned_code_point_range_start.has_value())
            assigned_code_point_range_start = data.code_point;

        if (data.name.starts_with("<"sv) && data.name.ends_with(", First>")) {
            VERIFY(!code_point_range_start.has_value() && assigned_code_point_range_start.has_value());
            code_point_range_start = data.code_point;

            data.name = data.name.substring(1, data.name.length() - 9);

            assigned_code_points.append({ *assigned_code_point_range_start, previous_code_point });
            assigned_code_point_range_start.clear();
        } else if (data.name.starts_with("<"sv) && data.name.ends_with(", Last>")) {
            VERIFY(code_point_range_start.has_value());

            CodePointRange code_point_range { *code_point_range_start, data.code_point };
            unicode_data.code_point_ranges.append(code_point_range);
            assigned_code_points.append(code_point_range);

            data.name = data.name.substring(1, data.name.length() - 8);
            code_point_range_start.clear();
        } else if ((data.code_point > 0) && (data.code_point - previous_code_point) != 1) {
            VERIFY(assigned_code_point_range_start.has_value());

            assigned_code_points.append({ *assigned_code_point_range_start, previous_code_point });
            assigned_code_point_range_start = data.code_point;
        }

        for (auto const& casing : unicode_data.special_casing) {
            if (casing.code_point == data.code_point)
                data.special_casing_indices.append(casing.index);
        }

        unicode_data.simple_uppercase_mapping_size += data.simple_uppercase_mapping.has_value();
        unicode_data.simple_lowercase_mapping_size += data.simple_lowercase_mapping.has_value();

        unicode_data.largest_special_casing_size = max(unicode_data.largest_special_casing_size, data.special_casing_indices.size());
        previous_code_point = data.code_point;

        unicode_data.code_point_data.append(move(data));
    }
}

static void generate_unicode_data_header(Core::File& file, UnicodeData& unicode_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.set("casing_transform_size", String::number(unicode_data.largest_casing_transform_size));
    generator.set("special_casing_size", String::number(unicode_data.largest_special_casing_size));

    auto generate_enum = [&](StringView name, StringView default_, Vector<String> values, Vector<Alias> aliases = {}) {
        quick_sort(values);
        quick_sort(aliases, [](auto& alias1, auto& alias2) { return alias1.alias < alias2.alias; });

        generator.set("name", name);
        generator.set("underlying", String::formatted("{}UnderlyingType", name));

        generator.append(R"~~~(
using @underlying@ = u8;

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
            generator.set("value", alias.property);
            generator.append(R"~~~(
    @alias@ = @value@,)~~~");
        }

        generator.append(R"~~~(
};
)~~~");
    };

    generator.append(R"~~~(
#pragma once

#include <AK/Optional.h>
#include <AK/Types.h>
#include <LibUnicode/Forward.h>
#include <LibUnicode/UnicodeLocale.h>

namespace Unicode {
)~~~");

    generate_enum("Condition"sv, "None"sv, move(unicode_data.conditions));
    generate_enum("GeneralCategory"sv, {}, unicode_data.general_categories.keys(), unicode_data.general_category_aliases);
    generate_enum("Property"sv, {}, unicode_data.prop_list.keys(), unicode_data.prop_aliases);
    generate_enum("Script"sv, {}, unicode_data.script_list.keys(), unicode_data.script_aliases);

    generator.append(R"~~~(
struct SpecialCasing {
    u32 code_point { 0 };

    u32 lowercase_mapping[@casing_transform_size@];
    u32 lowercase_mapping_size { 0 };

    u32 uppercase_mapping[@casing_transform_size@];
    u32 uppercase_mapping_size { 0 };

    u32 titlecase_mapping[@casing_transform_size@];
    u32 titlecase_mapping_size { 0 };

    Locale locale { Locale::None };
    Condition condition { Condition::None };
};

struct UnicodeData {
    u32 code_point;)~~~");

    auto append_field = [&](StringView type, StringView name) {
        if (!s_desired_fields.span().contains_slow(name))
            return;

        generator.set("type", type);
        generator.set("name", name);
        generator.append(R"~~~(
    @type@ @name@;)~~~");
    };

    // Note: For compile-time performance, only primitive types are used.
    append_field("char const*"sv, "name"sv);
    append_field("u8"sv, "canonical_combining_class"sv);
    append_field("char const*"sv, "bidi_class"sv);
    append_field("char const*"sv, "decomposition_type"sv);
    append_field("i8"sv, "numeric_value_decimal"sv);
    append_field("i8"sv, "numeric_value_digit"sv);
    append_field("i8"sv, "numeric_value_numeric"sv);
    append_field("bool"sv, "bidi_mirrored"sv);
    append_field("char const*"sv, "unicode_1_name"sv);
    append_field("char const*"sv, "iso_comment"sv);
    append_field("u32"sv, "simple_uppercase_mapping"sv);
    append_field("u32"sv, "simple_lowercase_mapping"sv);
    append_field("u32"sv, "simple_titlecase_mapping"sv);

    generator.append(R"~~~(

    SpecialCasing const* special_casing[@special_casing_size@] {};
    u32 special_casing_size { 0 };
};

namespace Detail {

Optional<UnicodeData> unicode_data_for_code_point(u32 code_point);

u32 simple_uppercase_mapping(u32 code_point);
u32 simple_lowercase_mapping(u32 code_point);

bool code_point_has_general_category(u32 code_point, GeneralCategory general_category);
Optional<GeneralCategory> general_category_from_string(StringView const& general_category);

bool code_point_has_property(u32 code_point, Property property);
Optional<Property> property_from_string(StringView const& property);

bool code_point_has_script(u32 code_point, Script script);
bool code_point_has_script_extension(u32 code_point, Script script);
Optional<Script> script_from_string(StringView const& script);

}

}
)~~~");

    file.write(generator.as_string_view());
}

static void generate_unicode_data_implementation(Core::File& file, UnicodeData const& unicode_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("special_casing_size", String::number(unicode_data.special_casing.size()));
    generator.set("code_point_data_size", String::number(unicode_data.code_point_data.size()));

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/BinarySearch.h>
#include <AK/CharacterTypes.h>
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <LibUnicode/UnicodeData.h>

namespace Unicode {
)~~~");

    auto append_list_and_size = [&](auto const& list, StringView format) {
        if (list.is_empty()) {
            generator.append(", {}, 0");
            return;
        }

        bool first = true;
        generator.append(", {");
        for (auto const& item : list) {
            generator.append(first ? " " : ", ");
            generator.append(String::formatted(format, item));
            first = false;
        }
        generator.append(String::formatted(" }}, {}", list.size()));
    };

    generator.append(R"~~~(
static constexpr Array<SpecialCasing, @special_casing_size@> s_special_casing { {)~~~");

    for (auto const& casing : unicode_data.special_casing) {
        generator.set("code_point", String::formatted("{:#x}", casing.code_point));
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

static constexpr Array<UnicodeData, @code_point_data_size@> s_unicode_data { {)~~~");

    auto append_field = [&](StringView name, String value) {
        if (!s_desired_fields.span().contains_slow(name))
            return;

        generator.set("value", move(value));
        generator.append(", @value@");
    };

    for (auto const& data : unicode_data.code_point_data) {
        generator.set("code_point", String::formatted("{:#x}", data.code_point));
        generator.append(R"~~~(
    { @code_point@)~~~");

        append_field("name", String::formatted("\"{}\"", data.name));
        append_field("canonical_combining_class", String::number(data.canonical_combining_class));
        append_field("bidi_class", String::formatted("\"{}\"", data.bidi_class));
        append_field("decomposition_type", String::formatted("\"{}\"", data.decomposition_type));
        append_field("numeric_value_decimal", String::number(data.numeric_value_decimal.value_or(-1)));
        append_field("numeric_value_digit", String::number(data.numeric_value_digit.value_or(-1)));
        append_field("numeric_value_numeric", String::number(data.numeric_value_numeric.value_or(-1)));
        append_field("bidi_mirrored", String::formatted("{}", data.bidi_mirrored));
        append_field("unicode_1_name", String::formatted("\"{}\"", data.unicode_1_name));
        append_field("iso_comment", String::formatted("\"{}\"", data.iso_comment));
        append_field("simple_uppercase_mapping", String::formatted("{:#x}", data.simple_uppercase_mapping.value_or(data.code_point)));
        append_field("simple_lowercase_mapping", String::formatted("{:#x}", data.simple_lowercase_mapping.value_or(data.code_point)));
        append_field("simple_titlecase_mapping", String::formatted("{:#x}", data.simple_titlecase_mapping.value_or(data.code_point)));
        append_list_and_size(data.special_casing_indices, "&s_special_casing[{}]"sv);
        generator.append(" },");
    }

    generator.append(R"~~~(
} };

struct CodePointMapping {
    u32 code_point { 0 };
    u32 mapping { 0 };
};

struct CodePointComparator {
    constexpr int operator()(u32 code_point, CodePointMapping const& mapping)
    {
        return code_point - mapping.code_point;
    }
};
)~~~");

    auto append_code_point_mappings = [&](StringView name, u32 size, auto mapping_getter) {
        generator.set("name", name);
        generator.set("size", String::number(size));

        generator.append(R"~~~(
static constexpr Array<CodePointMapping, @size@> s_@name@_mappings { {
    )~~~");

        constexpr size_t max_mappings_per_row = 20;
        size_t mappings_in_current_row = 0;

        for (auto const& data : unicode_data.code_point_data) {
            auto mapping = mapping_getter(data);
            if (!mapping.has_value())
                continue;

            if (mappings_in_current_row++ > 0)
                generator.append(" ");

            generator.set("code_point", String::formatted("{:#x}", data.code_point));
            generator.set("mapping", String::formatted("{:#x}", *mapping));
            generator.append("{ @code_point@, @mapping@ },");

            if (mappings_in_current_row == max_mappings_per_row) {
                mappings_in_current_row = 0;
                generator.append("\n    ");
            }
        }
        generator.append(R"~~~(
} };
)~~~");
    };

    append_code_point_mappings("uppercase"sv, unicode_data.simple_uppercase_mapping_size, [](auto const& data) { return data.simple_uppercase_mapping; });
    append_code_point_mappings("lowercase"sv, unicode_data.simple_lowercase_mapping_size, [](auto const& data) { return data.simple_lowercase_mapping; });

    generator.append(R"~~~(
struct CodePointRange {
    u32 first { 0 };
    u32 last { 0 };
};

struct CodePointRangeComparator {
    constexpr int operator()(u32 code_point, CodePointRange const& range)
    {
        return (code_point > range.last) - (code_point < range.first);
    }
};

)~~~");

    auto append_code_point_range_list = [&](String name, Vector<CodePointRange> const& ranges) {
        generator.set("name", name);
        generator.set("size", String::number(ranges.size()));
        generator.append(R"~~~(
static constexpr Array<CodePointRange, @size@> @name@ { {
    )~~~");

        constexpr size_t max_ranges_per_row = 20;
        size_t ranges_in_current_row = 0;

        for (auto const& range : ranges) {
            if (ranges_in_current_row++ > 0)
                generator.append(" ");

            generator.set("first", String::formatted("{:#x}", range.first));
            generator.set("last", String::formatted("{:#x}", range.last));
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
            auto name = String::formatted(property_format, property.key);
            append_code_point_range_list(move(name), property.value);
        }

        auto property_names = property_list.keys();
        quick_sort(property_names);

        generator.set("name", collection_name);
        generator.set("size", String::number(property_names.size()));
        generator.append(R"~~~(
static constexpr Array<Span<CodePointRange const>, @size@> @name@ { {)~~~");

        for (auto const& property_name : property_names) {
            generator.set("name", String::formatted(property_format, property_name));
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

    generator.append(R"~~~(
static HashMap<u32, UnicodeData const*> const& ensure_code_point_map()
{
    static HashMap<u32, UnicodeData const*> code_point_to_data_map;
    code_point_to_data_map.ensure_capacity(s_unicode_data.size());

    for (auto const& unicode_data : s_unicode_data)
        code_point_to_data_map.set(unicode_data.code_point, &unicode_data);

    return code_point_to_data_map;
}

static Optional<u32> index_of_code_point_in_range(u32 code_point)
{)~~~");

    for (auto const& range : unicode_data.code_point_ranges) {
        generator.set("first", String::formatted("{:#x}", range.first));
        generator.set("last", String::formatted("{:#x}", range.last));

        generator.append(R"~~~(
    if ((code_point > @first@) && (code_point < @last@))
        return @first@;)~~~");
    }

    generator.append(R"~~~(
    return {};
}

namespace Detail {

Optional<UnicodeData> unicode_data_for_code_point(u32 code_point)
{
    static auto const& code_point_to_data_map = ensure_code_point_map();
    VERIFY(is_unicode(code_point));

    if (auto data = code_point_to_data_map.get(code_point); data.has_value())
        return *(data.value());

    if (auto index = index_of_code_point_in_range(code_point); index.has_value()) {
        auto data_for_range = *(code_point_to_data_map.get(*index).value());
        data_for_range.simple_uppercase_mapping = code_point;
        data_for_range.simple_lowercase_mapping = code_point;
        return data_for_range;
    }

    return {};
}
)~~~");

    auto append_code_point_mapping_search = [&](StringView method, StringView mappings) {
        generator.set("method", method);
        generator.set("mappings", mappings);
        generator.append(R"~~~(
u32 @method@(u32 code_point)
{
    auto const* mapping = binary_search(@mappings@, code_point, nullptr, CodePointComparator {});
    return mapping ? mapping->mapping : code_point;
}
)~~~");
    };

    append_code_point_mapping_search("simple_uppercase_mapping"sv, "s_uppercase_mappings"sv);
    append_code_point_mapping_search("simple_lowercase_mapping"sv, "s_lowercase_mappings"sv);

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

    auto append_from_string = [&](StringView enum_title, StringView enum_snake, PropList const& prop_list, Vector<Alias> const& aliases) {
        generator.set("enum_title", enum_title);
        generator.set("enum_snake", enum_snake);

        auto properties = prop_list.keys();
        for (auto const& alias : aliases)
            properties.append(alias.alias);
        quick_sort(properties);

        generator.append(R"~~~(
Optional<@enum_title@> @enum_snake@_from_string(StringView const& @enum_snake@)
{
    static HashMap<String, @enum_title@> @enum_snake@_values { {)~~~");

        for (auto const& property : properties) {
            generator.set("property", property);
            generator.append(R"~~~(
        { "@property@"sv, @enum_title@::@property@ },)~~~");
        }

        generator.append(R"~~~(
    } };

    if (auto value = @enum_snake@_values.get(@enum_snake@); value.has_value())
        return value.value();
    return {};
}
)~~~");
    };

    append_prop_search("GeneralCategory"sv, "general_category"sv, "s_general_categories"sv);
    append_from_string("GeneralCategory"sv, "general_category"sv, unicode_data.general_categories, unicode_data.general_category_aliases);

    append_prop_search("Property"sv, "property"sv, "s_properties"sv);
    append_from_string("Property"sv, "property"sv, unicode_data.prop_list, unicode_data.prop_aliases);

    append_prop_search("Script"sv, "script"sv, "s_scripts"sv);
    append_prop_search("Script"sv, "script_extension"sv, "s_script_extensions"sv);
    append_from_string("Script"sv, "script"sv, unicode_data.script_list, unicode_data.script_aliases);

    generator.append(R"~~~(
}

}
)~~~");

    file.write(generator.as_string_view());
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

static void normalize_script_extensions(PropList& script_extensions, PropList const& script_list, Vector<Alias> const& script_aliases)
{
    // The ScriptExtensions UCD file lays out its code point ranges rather uniquely compared to
    // other files. The Script listed on each line may either be a full Script string or an aliased
    // abbreviation. Further, the extensions may or may not include the base Script list. Normalize
    // the extensions here to be keyed by the full Script name and always include the base list.
    auto extensions = move(script_extensions);
    script_extensions = script_list;

    for (auto const& extension : extensions) {
        auto it = find_if(script_aliases.begin(), script_aliases.end(), [&](auto const& alias) { return extension.key == alias.alias; });
        auto const& key = (it == script_aliases.end()) ? extension.key : it->property;

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
}

int main(int argc, char** argv)
{
    char const* generated_header_path = nullptr;
    char const* generated_implementation_path = nullptr;
    char const* unicode_data_path = nullptr;
    char const* special_casing_path = nullptr;
    char const* derived_general_category_path = nullptr;
    char const* prop_list_path = nullptr;
    char const* derived_core_prop_path = nullptr;
    char const* derived_binary_prop_path = nullptr;
    char const* prop_alias_path = nullptr;
    char const* prop_value_alias_path = nullptr;
    char const* scripts_path = nullptr;
    char const* script_extensions_path = nullptr;
    char const* emoji_data_path = nullptr;
    char const* normalization_path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the Unicode Data header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the Unicode Data implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(unicode_data_path, "Path to UnicodeData.txt file", "unicode-data-path", 'u', "unicode-data-path");
    args_parser.add_option(special_casing_path, "Path to SpecialCasing.txt file", "special-casing-path", 's', "special-casing-path");
    args_parser.add_option(derived_general_category_path, "Path to DerivedGeneralCategory.txt file", "derived-general-category-path", 'g', "derived-general-category-path");
    args_parser.add_option(prop_list_path, "Path to PropList.txt file", "prop-list-path", 'p', "prop-list-path");
    args_parser.add_option(derived_core_prop_path, "Path to DerivedCoreProperties.txt file", "derived-core-prop-path", 'd', "derived-core-prop-path");
    args_parser.add_option(derived_binary_prop_path, "Path to DerivedBinaryProperties.txt file", "derived-binary-prop-path", 'b', "derived-binary-prop-path");
    args_parser.add_option(prop_alias_path, "Path to PropertyAliases.txt file", "prop-alias-path", 'a', "prop-alias-path");
    args_parser.add_option(prop_value_alias_path, "Path to PropertyValueAliases.txt file", "prop-value-alias-path", 'v', "prop-value-alias-path");
    args_parser.add_option(scripts_path, "Path to Scripts.txt file", "scripts-path", 'r', "scripts-path");
    args_parser.add_option(script_extensions_path, "Path to ScriptExtensions.txt file", "script-extensions-path", 'x', "script-extensions-path");
    args_parser.add_option(emoji_data_path, "Path to emoji-data.txt file", "emoji-data-path", 'e', "emoji-data-path");
    args_parser.add_option(normalization_path, "Path to DerivedNormalizationProps.txt file", "normalization-path", 'n', "normalization-path");
    args_parser.parse(argc, argv);

    auto open_file = [&](StringView path, StringView flags, Core::OpenMode mode = Core::OpenMode::ReadOnly) {
        if (path.is_empty()) {
            warnln("{} is required", flags);
            args_parser.print_usage(stderr, argv[0]);
            exit(1);
        }

        auto file_or_error = Core::File::open(path, mode);
        if (file_or_error.is_error()) {
            warnln("Failed to open {}: {}", path, file_or_error.release_error());
            exit(1);
        }

        return file_or_error.release_value();
    };

    auto generated_header_file = open_file(generated_header_path, "-h/--generated-header-path", Core::OpenMode::ReadWrite);
    auto generated_implementation_file = open_file(generated_implementation_path, "-c/--generated-implementation-path", Core::OpenMode::ReadWrite);
    auto unicode_data_file = open_file(unicode_data_path, "-u/--unicode-data-path");
    auto derived_general_category_file = open_file(derived_general_category_path, "-g/--derived-general-category-path");
    auto special_casing_file = open_file(special_casing_path, "-s/--special-casing-path");
    auto prop_list_file = open_file(prop_list_path, "-p/--prop-list-path");
    auto derived_core_prop_file = open_file(derived_core_prop_path, "-d/--derived-core-prop-path");
    auto derived_binary_prop_file = open_file(derived_binary_prop_path, "-b/--derived-binary-prop-path");
    auto prop_alias_file = open_file(prop_alias_path, "-a/--prop-alias-path");
    auto prop_value_alias_file = open_file(prop_value_alias_path, "-v/--prop-value-alias-path");
    auto scripts_file = open_file(scripts_path, "-r/--scripts-path");
    auto script_extensions_file = open_file(script_extensions_path, "-x/--script-extensions-path");
    auto emoji_data_file = open_file(emoji_data_path, "-e/--emoji-data-path");
    auto normalization_file = open_file(normalization_path, "-n/--normalization-path");

    UnicodeData unicode_data {};
    parse_special_casing(special_casing_file, unicode_data);
    parse_prop_list(derived_general_category_file, unicode_data.general_categories);
    parse_prop_list(prop_list_file, unicode_data.prop_list);
    parse_prop_list(derived_core_prop_file, unicode_data.prop_list);
    parse_prop_list(derived_binary_prop_file, unicode_data.prop_list);
    parse_prop_list(emoji_data_file, unicode_data.prop_list);
    parse_normalization_props(normalization_file, unicode_data);
    parse_alias_list(prop_alias_file, unicode_data.prop_list, unicode_data.prop_aliases);
    parse_prop_list(scripts_file, unicode_data.script_list);
    parse_prop_list(script_extensions_file, unicode_data.script_extensions, true);

    populate_general_category_unions(unicode_data.general_categories);
    parse_unicode_data(unicode_data_file, unicode_data);
    parse_value_alias_list(prop_value_alias_file, "gc"sv, unicode_data.general_categories.keys(), unicode_data.general_category_aliases);
    parse_value_alias_list(prop_value_alias_file, "sc"sv, unicode_data.script_list.keys(), unicode_data.script_aliases, false);
    normalize_script_extensions(unicode_data.script_extensions, unicode_data.script_list, unicode_data.script_aliases);

    generate_unicode_data_header(generated_header_file, unicode_data);
    generate_unicode_data_implementation(generated_implementation_file, unicode_data);

    return 0;
}
