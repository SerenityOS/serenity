/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/Format.h>
#include <AK/HashMap.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibUnicode/Locale.h>
#include <LibUnicode/RelativeTimeFormat.h>

using StringIndexType = u16;
constexpr auto s_string_index_type = "u16"sv;

using RelativeTimeFormatIndexType = u16;
constexpr auto s_relative_time_format_index_type = "u16"sv;

struct RelativeTimeFormat {
    unsigned hash() const
    {
        auto hash = time_unit.hash();
        hash = pair_int_hash(hash, style.hash());
        hash = pair_int_hash(hash, plurality.hash());
        hash = pair_int_hash(hash, tense_or_number);
        hash = pair_int_hash(hash, pattern);
        return hash;
    }

    bool operator==(RelativeTimeFormat const& other) const
    {
        return (time_unit == other.time_unit)
            && (plurality == other.plurality)
            && (style == other.style)
            && (tense_or_number == other.tense_or_number)
            && (pattern == other.pattern);
    }

    String time_unit;
    String style;
    String plurality;
    StringIndexType tense_or_number { 0 };
    StringIndexType pattern { 0 };
};

template<>
struct AK::Formatter<RelativeTimeFormat> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, RelativeTimeFormat const& format)
    {
        return Formatter<FormatString>::format(builder,
            "{{ TimeUnit::{}, Style::{}, RelativeTimeFormat::Plurality::{}, {}, {} }}",
            format.time_unit,
            format.style,
            format.plurality,
            format.tense_or_number,
            format.pattern);
    }
};

template<>
struct AK::Traits<RelativeTimeFormat> : public GenericTraits<RelativeTimeFormat> {
    static unsigned hash(RelativeTimeFormat const& format) { return format.hash(); }
};

struct Locale {
    Vector<RelativeTimeFormatIndexType> time_units;
};

struct UnicodeLocaleData {
    UniqueStringStorage<StringIndexType> unique_strings;
    UniqueStorage<RelativeTimeFormat, RelativeTimeFormatIndexType> unique_formats;

    HashMap<String, Locale> locales;
};

static ErrorOr<void> parse_date_fields(String locale_dates_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath date_fields_path(move(locale_dates_path));
    date_fields_path = date_fields_path.append("dateFields.json"sv);

    auto date_fields_file = TRY(Core::File::open(date_fields_path.string(), Core::OpenMode::ReadOnly));
    auto date_fields = TRY(JsonValue::from_string(date_fields_file->read_all()));

    auto const& main_object = date_fields.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(date_fields_path.parent().basename());
    auto const& dates_object = locale_object.as_object().get("dates"sv);
    auto const& fields_object = dates_object.as_object().get("fields"sv);

    auto is_sanctioned_unit = [](auto unit) {
        // This is a copy of the time units sanctioned for use within ECMA-402.
        // https://tc39.es/ecma402/#sec-singularrelativetimeunit
        return unit.is_one_of("second"sv, "minute"sv, "hour"sv, "day"sv, "week"sv, "month"sv, "quarter"sv, "year"sv);
    };

    auto parse_pattern = [&](auto unit, auto style, auto plurality, auto tense_or_number, auto const& pattern) {
        RelativeTimeFormat format {};
        format.time_unit = unit.to_titlecase_string();
        format.style = style.to_titlecase_string();
        format.plurality = plurality.to_titlecase_string();
        format.tense_or_number = locale_data.unique_strings.ensure(tense_or_number);
        format.pattern = locale_data.unique_strings.ensure(pattern.as_string());

        locale.time_units.append(locale_data.unique_formats.ensure(move(format)));
    };

    fields_object.as_object().for_each_member([&](auto const& unit_and_style, auto const& patterns) {
        auto segments = unit_and_style.split_view('-');
        auto unit = segments[0];
        auto style = (segments.size() > 1) ? segments[1] : "long"sv;

        if (!is_sanctioned_unit(unit))
            return;

        patterns.as_object().for_each_member([&](auto const& type, auto const& pattern_value) {
            constexpr auto number_key = "relative-type-"sv;
            constexpr auto tense_key = "relativeTime-type-"sv;
            constexpr auto plurality_key = "relativeTimePattern-count-"sv;

            if (type.starts_with(number_key)) {
                auto number = type.substring_view(number_key.length());
                parse_pattern(unit, style, "Other"sv, number, pattern_value);
            } else if (type.starts_with(tense_key)) {
                pattern_value.as_object().for_each_member([&](auto const& key, auto const& pattern) {
                    VERIFY(key.starts_with(plurality_key));
                    auto plurality = key.substring_view(plurality_key.length());
                    auto tense = type.substring_view(tense_key.length());

                    parse_pattern(unit, style, plurality, tense, pattern);
                });
            }
        });
    });

    return {};
}

static ErrorOr<void> parse_all_locales(String dates_path, UnicodeLocaleData& locale_data)
{
    auto dates_iterator = TRY(path_to_dir_iterator(move(dates_path)));

    auto remove_variants_from_path = [&](String path) -> ErrorOr<String> {
        auto parsed_locale = TRY(CanonicalLanguageID<StringIndexType>::parse(locale_data.unique_strings, LexicalPath::basename(path)));

        StringBuilder builder;
        builder.append(locale_data.unique_strings.get(parsed_locale.language));
        if (auto script = locale_data.unique_strings.get(parsed_locale.script); !script.is_empty())
            builder.appendff("-{}", script);
        if (auto region = locale_data.unique_strings.get(parsed_locale.region); !region.is_empty())
            builder.appendff("-{}", region);

        return builder.build();
    };

    while (dates_iterator.has_next()) {
        auto dates_path = TRY(next_path_from_dir_iterator(dates_iterator));
        auto language = TRY(remove_variants_from_path(dates_path));

        auto& locale = locale_data.locales.ensure(language);
        TRY(parse_date_fields(move(dates_path), locale_data, locale));
    }

    return {};
}

static void generate_unicode_locale_header(Core::File& file, UnicodeLocaleData&)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once

#include <LibUnicode/Forward.h>

namespace Unicode {
)~~~");

    generator.append(R"~~~(
}
)~~~");

    VERIFY(file.write(generator.as_string_view()));
}

static void generate_unicode_locale_implementation(Core::File& file, UnicodeLocaleData& locale_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.set("string_index_type"sv, s_string_index_type);
    generator.set("relative_time_format_index_type"sv, s_relative_time_format_index_type);

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibUnicode/Locale.h>
#include <LibUnicode/RelativeTimeFormat.h>
#include <LibUnicode/UnicodeRelativeTimeFormat.h>

namespace Unicode {
)~~~");

    locale_data.unique_strings.generate(generator);

    generator.append(R"~~~(
struct RelativeTimeFormatImpl {
    RelativeTimeFormat to_relative_time_format() const
    {
        RelativeTimeFormat relative_time_format {};
        relative_time_format.plurality = plurality;
        relative_time_format.pattern = s_string_list[pattern];

        return relative_time_format;
    }

    TimeUnit time_unit;
    Style style;
    RelativeTimeFormat::Plurality plurality;
    @string_index_type@ tense_or_number { 0 };
    @string_index_type@ pattern { 0 };
};
)~~~");

    locale_data.unique_formats.generate(generator, "RelativeTimeFormatImpl"sv, "s_relative_time_formats"sv, 10);

    auto append_list = [&](String name, auto const& list) {
        generator.set("name", name);
        generator.set("size", String::number(list.size()));

        generator.append(R"~~~(
static constexpr Array<@relative_time_format_index_type@, @size@> @name@ { {)~~~");

        bool first = true;
        for (auto index : list) {
            generator.append(first ? " " : ", ");
            generator.append(String::number(index));
            first = false;
        }

        generator.append(" } };");
    };

    generate_mapping(generator, locale_data.locales, s_relative_time_format_index_type, "s_locale_relative_time_formats"sv, "s_number_systems_digits_{}", nullptr, [&](auto const& name, auto const& value) { append_list(name, value.time_units); });

    generator.append(R"~~~(
Vector<RelativeTimeFormat> get_relative_time_format_patterns(StringView locale, TimeUnit time_unit, StringView tense_or_number, Style style)
{
    Vector<RelativeTimeFormat> formats;

    auto locale_value = locale_from_string(locale);
    if (!locale_value.has_value())
        return formats;

    auto locale_index = to_underlying(*locale_value) - 1; // Subtract 1 because 0 == Locale::None.
    auto const& locale_formats = s_locale_relative_time_formats.at(locale_index);

    for (auto const& locale_format_index : locale_formats) {
        auto const& locale_format = s_relative_time_formats.at(locale_format_index);

        if (locale_format.time_unit != time_unit)
            continue;
        if (locale_format.style != style)
            continue;
        if (s_string_list[locale_format.tense_or_number] != tense_or_number)
            continue;

        formats.append(locale_format.to_relative_time_format());
    }

    return formats;
}

}
)~~~");

    VERIFY(file.write(generator.as_string_view()));
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView dates_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the Unicode locale header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the Unicode locale implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(dates_path, "Path to cldr-dates directory", "dates-path", 'd', "dates-path");
    args_parser.parse(arguments);

    auto open_file = [&](StringView path) -> ErrorOr<NonnullRefPtr<Core::File>> {
        if (path.is_empty()) {
            args_parser.print_usage(stderr, arguments.argv[0]);
            return Error::from_string_literal("Must provide all command line options"sv);
        }

        return Core::File::open(path, Core::OpenMode::ReadWrite);
    };

    auto generated_header_file = TRY(open_file(generated_header_path));
    auto generated_implementation_file = TRY(open_file(generated_implementation_path));

    UnicodeLocaleData locale_data;
    TRY(parse_all_locales(dates_path, locale_data));

    generate_unicode_locale_header(generated_header_file, locale_data);
    generate_unicode_locale_implementation(generated_implementation_file, locale_data);

    return 0;
}
