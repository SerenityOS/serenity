/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AllOf.h>
#include <AK/Array.h>
#include <AK/CharacterTypes.h>
#include <AK/Optional.h>
#include <AK/QuickSort.h>
#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/StringUtils.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>

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

// UnicodeData source: https://www.unicode.org/Public/13.0.0/ucd/UnicodeData.txt
// Field descriptions: https://www.unicode.org/reports/tr44/tr44-13.html#UnicodeData.txt
struct CodePointData {
    u32 index { 0 };
    u32 code_point { 0 };
    String name;
    String general_category;
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

// Some code points are excluded from UnicodeData.txt, and instead are part of a "range" of code
// points, as indicated by the "name" field. For example:
//     3400;<CJK Ideograph Extension A, First>;Lo;0;L;;;;;N;;;;;
//     4DBF;<CJK Ideograph Extension A, Last>;Lo;0;L;;;;;N;;;;;
struct CodePointRange {
    u32 index;
    u32 first;
    u32 last;
};

struct UnicodeData {
    Vector<SpecialCasing> special_casing;
    u32 largest_casing_transform_size { 0 };
    u32 largest_special_casing_size { 0 };
    Vector<String> locales;
    Vector<String> conditions;

    Vector<CodePointData> code_point_data;
    Vector<CodePointRange> code_point_ranges;
    Vector<String> general_categories;
    u32 last_contiguous_code_point { 0 };
};

static constexpr auto s_desired_fields = Array {
    "general_category"sv,
    "simple_uppercase_mapping"sv,
    "simple_lowercase_mapping"sv,
};

static void parse_special_casing(Core::File& file, UnicodeData& unicode_data)
{
    auto parse_code_point_list = [&](auto const& line) {
        Vector<u32> code_points;

        auto segments = line.split(' ');
        for (auto const& code_point : segments)
            code_points.append(AK::StringUtils::convert_to_uint_from_hex<u32>(code_point).value());

        return code_points;
    };

    while (file.can_read_line()) {
        auto line = file.read_line();
        if (line.is_empty() || line.starts_with('#'))
            continue;

        if (auto index = line.find('#'); index.has_value())
            line = line.substring(0, *index);

        auto segments = line.split(';', true);
        VERIFY(segments.size() == 5 || segments.size() == 6);

        SpecialCasing casing {};
        casing.index = static_cast<u32>(unicode_data.special_casing.size());
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

            casing.locale = casing.locale.to_uppercase();
            casing.condition.replace("_", "", true);

            if (!casing.locale.is_empty() && !unicode_data.locales.contains_slow(casing.locale))
                unicode_data.locales.append(casing.locale);
            if (!casing.condition.is_empty() && !unicode_data.conditions.contains_slow(casing.condition))
                unicode_data.conditions.append(casing.condition);
        }

        unicode_data.largest_casing_transform_size = max(unicode_data.largest_casing_transform_size, casing.lowercase_mapping.size());
        unicode_data.largest_casing_transform_size = max(unicode_data.largest_casing_transform_size, casing.titlecase_mapping.size());
        unicode_data.largest_casing_transform_size = max(unicode_data.largest_casing_transform_size, casing.uppercase_mapping.size());

        unicode_data.special_casing.append(move(casing));
    }

    quick_sort(unicode_data.locales);
    quick_sort(unicode_data.conditions);
}

static void parse_unicode_data(Core::File& file, UnicodeData& unicode_data)
{
    Optional<u32> code_point_range_start;
    Optional<u32> code_point_range_index;

    Optional<u32> last_contiguous_code_point;
    u32 previous_code_point = 0;

    while (file.can_read_line()) {
        auto line = file.read_line();
        if (line.is_empty())
            continue;

        auto segments = line.split(';', true);
        VERIFY(segments.size() == 15);

        CodePointData data {};
        data.index = static_cast<u32>(unicode_data.code_point_data.size());
        data.code_point = AK::StringUtils::convert_to_uint_from_hex<u32>(segments[0]).value();
        data.name = move(segments[1]);
        data.general_category = move(segments[2]);
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

        if (data.name.starts_with("<"sv) && data.name.ends_with(", First>")) {
            VERIFY(!code_point_range_start.has_value() && !code_point_range_index.has_value());

            code_point_range_start = data.code_point;
            code_point_range_index = data.index;

            data.name = data.name.substring(1, data.name.length() - 9);
        } else if (data.name.starts_with("<"sv) && data.name.ends_with(", Last>")) {
            VERIFY(code_point_range_start.has_value() && code_point_range_index.has_value());

            unicode_data.code_point_ranges.append({ *code_point_range_index, *code_point_range_start, data.code_point });
            data.name = data.name.substring(1, data.name.length() - 8);

            code_point_range_start.clear();
            code_point_range_index.clear();
        } else if ((data.code_point > 0) && (data.code_point - previous_code_point) != 1) {
            if (!last_contiguous_code_point.has_value())
                last_contiguous_code_point = previous_code_point;
        }

        for (auto const& casing : unicode_data.special_casing) {
            if (casing.code_point == data.code_point)
                data.special_casing_indices.append(casing.index);
        }

        unicode_data.largest_special_casing_size = max(unicode_data.largest_special_casing_size, data.special_casing_indices.size());

        if (!unicode_data.general_categories.contains_slow(data.general_category))
            unicode_data.general_categories.append(data.general_category);

        previous_code_point = data.code_point;
        unicode_data.code_point_data.append(move(data));
    }

    quick_sort(unicode_data.general_categories);
    unicode_data.last_contiguous_code_point = *last_contiguous_code_point;
}

static void generate_unicode_data_header(UnicodeData const& unicode_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.set("casing_transform_size", String::number(unicode_data.largest_casing_transform_size));
    generator.set("special_casing_size", String::number(unicode_data.largest_special_casing_size));

    generator.append(R"~~~(
#pragma once

#include <AK/Optional.h>
#include <AK/Types.h>

namespace Unicode {

enum class Locale {
    None,)~~~");

    for (auto const& locale : unicode_data.locales) {
        generator.set("locale", locale);
        generator.append(R"~~~(
    @locale@,)~~~");
    }

    generator.append(R"~~~(
};

enum class Condition {
    None,)~~~");

    for (auto const& condition : unicode_data.conditions) {
        generator.set("condition", condition);
        generator.append(R"~~~(
    @condition@,)~~~");
    }

    generator.append(R"~~~(
};

// https://www.unicode.org/reports/tr44/#General_Category_Values
enum class GeneralCategory {)~~~");

    for (auto const& general_category : unicode_data.general_categories) {
        generator.set("general_category", general_category);
        generator.append(R"~~~(
    @general_category@,)~~~");
    }

    generator.append(R"~~~(
};

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
    append_field("GeneralCategory"sv, "general_category"sv);
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

Optional<UnicodeData> unicode_data_for_code_point(u32 code_point);

})~~~");

    outln("{}", generator.as_string_view());
}

static void generate_unicode_data_implementation(UnicodeData unicode_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("special_casing_size", String::number(unicode_data.special_casing.size()));
    generator.set("code_point_data_size", String::number(unicode_data.code_point_data.size()));
    generator.set("last_contiguous_code_point", String::formatted("0x{:x}", unicode_data.last_contiguous_code_point));

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/CharacterTypes.h>
#include <AK/Find.h>
#include <LibUnicode/UnicodeData.h>

namespace Unicode {
)~~~");

    auto append_numeric_list = [&](auto const& code_points, StringView format) {
        if (code_points.is_empty()) {
            generator.append(", {}, 0");
            return;
        }

        bool first = true;
        generator.append(", {");
        for (auto code_point : code_points) {
            generator.append(first ? " " : ", ");
            generator.append(String::formatted(format, code_point));
            first = false;
        }
        generator.append(String::formatted(" }}, {}", code_points.size()));
    };

    generator.append(R"~~~(
static constexpr Array<SpecialCasing, @special_casing_size@> s_special_casing { {)~~~");

    for (auto const& casing : unicode_data.special_casing) {
        generator.set("code_point", String::formatted("{:#x}", casing.code_point));
        generator.append(R"~~~(
    { @code_point@)~~~");

        constexpr auto format = "0x{:x}"sv;
        append_numeric_list(casing.lowercase_mapping, format);
        append_numeric_list(casing.uppercase_mapping, format);
        append_numeric_list(casing.titlecase_mapping, format);

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
        append_field("general_category", String::formatted("GeneralCategory::{}", data.general_category));
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
        append_numeric_list(data.special_casing_indices, "&s_special_casing[{}]"sv);
        generator.append(" },");
    }

    generator.append(R"~~~(
} };

static Optional<u32> index_of_code_point_in_range(u32 code_point)
{)~~~");

    for (auto const& range : unicode_data.code_point_ranges) {
        generator.set("index", String::formatted("{}", range.index));
        generator.set("first", String::formatted("{:#x}", range.first));
        generator.set("last", String::formatted("{:#x}", range.last));

        generator.append(R"~~~(
    if ((code_point > @first@) && (code_point < @last@))
        return @index@;)~~~");
    }

    generator.append(R"~~~(
    return {};
}

Optional<UnicodeData> unicode_data_for_code_point(u32 code_point)
{
    VERIFY(is_unicode(code_point));

    if (code_point <= @last_contiguous_code_point@)
        return s_unicode_data[code_point];

    if (auto index = index_of_code_point_in_range(code_point); index.has_value()) {
        auto data_for_range = s_unicode_data[*index];
        data_for_range.simple_uppercase_mapping = code_point;
        data_for_range.simple_lowercase_mapping = code_point;
        return data_for_range;
    }

    auto it = AK::find_if(s_unicode_data.begin(), s_unicode_data.end(), [code_point](auto const& data) { return data.code_point == code_point; });
    if (it != s_unicode_data.end())
        return *it;

    return {};
}

})~~~");

    outln("{}", generator.as_string_view());
}

int main(int argc, char** argv)
{
    bool generate_header = false;
    bool generate_implementation = false;
    char const* unicode_data_path = nullptr;
    char const* special_casing_path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(generate_header, "Generate the Unicode Data header file", "generate-header", 'h');
    args_parser.add_option(generate_implementation, "Generate the Unicode Data implementation file", "generate-implementation", 'c');
    args_parser.add_option(unicode_data_path, "Path to UnicodeData.txt file", "unicode-data-path", 'u', "unicode-data-path");
    args_parser.add_option(special_casing_path, "Path to SpecialCasing.txt file", "special-casing-path", 's', "special-casing-path");
    args_parser.parse(argc, argv);

    if (!generate_header && !generate_implementation) {
        warnln("At least one of -h/--generate-header or -c/--generate-implementation is required");
        args_parser.print_usage(stderr, argv[0]);
        return 1;
    }
    if (!unicode_data_path) {
        warnln("-u/--unicode-data-path is required");
        args_parser.print_usage(stderr, argv[0]);
        return 1;
    }
    if (!special_casing_path) {
        warnln("-s/--special-casing-path is required");
        args_parser.print_usage(stderr, argv[0]);
        return 1;
    }

    auto unicode_data_file_or_error = Core::File::open(unicode_data_path, Core::OpenMode::ReadOnly);
    if (unicode_data_file_or_error.is_error()) {
        warnln("Failed to open {}: {}", unicode_data_path, unicode_data_file_or_error.release_error());
        return 1;
    }

    auto special_casing_file_or_error = Core::File::open(special_casing_path, Core::OpenMode::ReadOnly);
    if (special_casing_file_or_error.is_error()) {
        warnln("Failed to open {}: {}", special_casing_path, special_casing_file_or_error.release_error());
        return 1;
    }

    UnicodeData unicode_data {};
    parse_special_casing(special_casing_file_or_error.value(), unicode_data);
    parse_unicode_data(unicode_data_file_or_error.value(), unicode_data);

    if (generate_header)
        generate_unicode_data_header(unicode_data);
    if (generate_implementation)
        generate_unicode_data_implementation(move(unicode_data));

    return 0;
}
