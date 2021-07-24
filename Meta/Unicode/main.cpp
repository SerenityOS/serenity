/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/LexicalPath.h>
#include <AK/Optional.h>
#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/StringUtils.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>

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
    Vector<CodePointData> code_point_data;
    Vector<CodePointRange> code_point_ranges;
    u32 last_contiguous_code_point { 0 };
};

static constexpr auto s_desired_fields = Array {
    "simple_uppercase_mapping"sv,
    "simple_lowercase_mapping"sv,
};

UnicodeData parse_unicode_data(Core::File& file)
{
    UnicodeData unicode_data;

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

        previous_code_point = data.code_point;
        unicode_data.code_point_data.append(move(data));
    }

    unicode_data.last_contiguous_code_point = *last_contiguous_code_point;
    return unicode_data;
}

void generate_unicode_data_header(String header_path)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once

#include <AK/Optional.h>
#include <AK/Types.h>

namespace AK {

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
    append_field("char const*"sv, "general_category"sv);
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

    builder.append(R"~~~(
};

Optional<UnicodeData> unicode_data_for_code_point(u32 code_point);

}
)~~~");

    auto file_or_error = Core::File::open(header_path, Core::OpenMode::WriteOnly);
    if (file_or_error.is_error()) {
        warnln("Failed to open {}: {}", header_path, file_or_error.release_error());
        return;
    }

    bool success = file_or_error.value()->write(generator.as_string_view());
    VERIFY(success);
}

void generate_unicode_data_implementation(UnicodeData unicode_data, String implementation_path)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.set("size", String::formatted("{}", unicode_data.code_point_data.size()));
    generator.set("last_contiguous_code_point", String::formatted("0x{:x}", unicode_data.last_contiguous_code_point));

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/CharacterTypes.h>
#include <AK/Find.h>
#include <AK/UnicodeData.h>

namespace AK {

static constexpr Array<UnicodeData, @size@> s_unicode_data { {)~~~");

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
        append_field("general_category", String::formatted("\"{}\"", data.general_category));
        append_field("canonical_combining_class", String::formatted("{}", data.canonical_combining_class));
        append_field("bidi_class", String::formatted("\"{}\"", data.bidi_class));
        append_field("decomposition_type", String::formatted("\"{}\"", data.decomposition_type));
        append_field("numeric_value_decimal", String::formatted("{}", data.numeric_value_decimal.value_or(-1)));
        append_field("numeric_value_digit", String::formatted("{}", data.numeric_value_digit.value_or(-1)));
        append_field("numeric_value_numeric", String::formatted("{}", data.numeric_value_numeric.value_or(-1)));
        append_field("bidi_mirrored", String::formatted("{}", data.bidi_mirrored));
        append_field("unicode_1_name", String::formatted("\"{}\"", data.unicode_1_name));
        append_field("iso_comment", String::formatted("\"{}\"", data.iso_comment));
        append_field("simple_uppercase_mapping", String::formatted("{:#x}", data.simple_uppercase_mapping.value_or(data.code_point)));
        append_field("simple_lowercase_mapping", String::formatted("{:#x}", data.simple_lowercase_mapping.value_or(data.code_point)));
        append_field("simple_titlecase_mapping", String::formatted("{:#x}", data.simple_titlecase_mapping.value_or(data.code_point)));

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

}
)~~~");

    auto file_or_error = Core::File::open(implementation_path, Core::OpenMode::WriteOnly);
    if (file_or_error.is_error()) {
        warnln("Failed to open {}: {}", implementation_path, file_or_error.release_error());
        return;
    }

    bool success = file_or_error.value()->write(generator.as_string_view());
    VERIFY(success);
}

int main(int argc, char** argv)
{
    char const* unicode_path = nullptr;
    char const* ak_path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(unicode_path, "Path to UnicodeData.txt file", "unicode-data-path", 'u', "unicode-data-path");
    args_parser.add_option(ak_path, "Path to the AK source folder", "ak-path", 'a', "ak-path");
    args_parser.parse(argc, argv);

    if (!unicode_path) {
        warnln("-u/--unicode-data-path is required");
        args_parser.print_usage(stderr, argv[0]);
        return 1;
    }
    if (!ak_path) {
        warnln("-a/--ak-path is required");
        args_parser.print_usage(stderr, argv[0]);
        return 1;
    }

    auto file_or_error = Core::File::open(unicode_path, Core::OpenMode::ReadOnly);
    if (file_or_error.is_error()) {
        warnln("Failed to open {}: {}", unicode_path, file_or_error.release_error());
        return 1;
    }

    auto unicode_data = parse_unicode_data(file_or_error.value());

    LexicalPath ak(ak_path);
    auto header_path = ak.append("UnicodeData.h");
    auto implementation_path = ak.append("UnicodeData.cpp");

    generate_unicode_data_header(header_path.string());
    generate_unicode_data_implementation(move(unicode_data), implementation_path.string());

    return 0;
}
