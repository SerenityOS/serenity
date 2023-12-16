/*
 * Copyright (c) 2022-2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "../LibUnicode/GeneratorUtil.h" // FIXME: Move this somewhere common.
#include <AK/ByteString.h>
#include <AK/Format.h>
#include <AK/HashMap.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Directory.h>
#include <LibLocale/Locale.h>
#include <LibLocale/RelativeTimeFormat.h>

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

    ByteString time_unit;
    ByteString style;
    ByteString plurality;
    size_t tense_or_number { 0 };
    size_t pattern { 0 };
};

template<>
struct AK::Formatter<RelativeTimeFormat> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, RelativeTimeFormat const& format)
    {
        return Formatter<FormatString>::format(builder,
            "{{ TimeUnit::{}, Style::{}, PluralCategory::{}, {}, {} }}"sv,
            format.time_unit,
            format.style,
            format.plurality,
            format.tense_or_number,
            format.pattern);
    }
};

template<>
struct AK::Traits<RelativeTimeFormat> : public DefaultTraits<RelativeTimeFormat> {
    static unsigned hash(RelativeTimeFormat const& format) { return format.hash(); }
};

struct LocaleData {
    Vector<size_t> time_units;
};

struct CLDR {
    UniqueStringStorage unique_strings;
    UniqueStorage<RelativeTimeFormat> unique_formats;

    HashMap<ByteString, LocaleData> locales;
};

static ErrorOr<void> parse_date_fields(ByteString locale_dates_path, CLDR& cldr, LocaleData& locale)
{
    LexicalPath date_fields_path(move(locale_dates_path));
    date_fields_path = date_fields_path.append("dateFields.json"sv);

    auto date_fields = TRY(read_json_file(date_fields_path.string()));
    auto const& main_object = date_fields.as_object().get_object("main"sv).value();
    auto const& locale_object = main_object.get_object(date_fields_path.parent().basename()).value();
    auto const& dates_object = locale_object.get_object("dates"sv).value();
    auto const& fields_object = dates_object.get_object("fields"sv).value();

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
        format.tense_or_number = cldr.unique_strings.ensure(tense_or_number);
        format.pattern = cldr.unique_strings.ensure(pattern.as_string());

        locale.time_units.append(cldr.unique_formats.ensure(move(format)));
    };

    fields_object.for_each_member([&](auto const& unit_and_style, auto const& patterns) {
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

static ErrorOr<void> parse_all_locales(ByteString dates_path, CLDR& cldr)
{
    auto remove_variants_from_path = [&](ByteString path) -> ErrorOr<ByteString> {
        auto parsed_locale = TRY(CanonicalLanguageID::parse(cldr.unique_strings, LexicalPath::basename(path)));

        StringBuilder builder;
        builder.append(cldr.unique_strings.get(parsed_locale.language));
        if (auto script = cldr.unique_strings.get(parsed_locale.script); !script.is_empty())
            builder.appendff("-{}", script);
        if (auto region = cldr.unique_strings.get(parsed_locale.region); !region.is_empty())
            builder.appendff("-{}", region);

        return builder.to_byte_string();
    };

    TRY(Core::Directory::for_each_entry(TRY(String::formatted("{}/main", dates_path)), Core::DirIterator::SkipParentAndBaseDir, [&](auto& entry, auto& directory) -> ErrorOr<IterationDecision> {
        auto dates_path = LexicalPath::join(directory.path().string(), entry.name).string();
        auto language = TRY(remove_variants_from_path(dates_path));

        auto& locale = cldr.locales.ensure(language);
        TRY(parse_date_fields(move(dates_path), cldr, locale));
        return IterationDecision::Continue;
    }));

    return {};
}

static ErrorOr<void> generate_unicode_locale_header(Core::InputBufferedFile& file, CLDR&)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once

#include <LibLocale/Forward.h>

namespace Locale {
)~~~");

    generator.append(R"~~~(
}
)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

static ErrorOr<void> generate_unicode_locale_implementation(Core::InputBufferedFile& file, CLDR& cldr)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.set("string_index_type"sv, cldr.unique_strings.type_that_fits());
    generator.set("relative_time_format_index_type"sv, cldr.unique_formats.type_that_fits());

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibLocale/Locale.h>
#include <LibLocale/PluralRules.h>
#include <LibLocale/RelativeTimeFormat.h>
#include <LibLocale/RelativeTimeFormatData.h>

namespace Locale {
)~~~");

    cldr.unique_strings.generate(generator);

    generator.append(R"~~~(
struct RelativeTimeFormatImpl {
    RelativeTimeFormat to_relative_time_format() const
    {
        RelativeTimeFormat relative_time_format {};
        relative_time_format.plurality = plurality;
        relative_time_format.pattern = decode_string(pattern);

        return relative_time_format;
    }

    TimeUnit time_unit;
    Style style;
    PluralCategory plurality;
    @string_index_type@ tense_or_number { 0 };
    @string_index_type@ pattern { 0 };
};
)~~~");

    cldr.unique_formats.generate(generator, "RelativeTimeFormatImpl"sv, "s_relative_time_formats"sv, 10);

    auto append_list = [&](ByteString name, auto const& list) {
        generator.set("name", name);
        generator.set("size", ByteString::number(list.size()));

        generator.append(R"~~~(
static constexpr Array<@relative_time_format_index_type@, @size@> @name@ { {)~~~");

        bool first = true;
        for (auto index : list) {
            generator.append(first ? " "sv : ", "sv);
            generator.append(ByteString::number(index));
            first = false;
        }

        generator.append(" } };");
    };

    generate_mapping(generator, cldr.locales, cldr.unique_formats.type_that_fits(), "s_locale_relative_time_formats"sv, "s_number_systems_digits_{}"sv, nullptr, [&](auto const& name, auto const& value) { append_list(name, value.time_units); });

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
        if (decode_string(locale_format.tense_or_number) != tense_or_number)
            continue;

        formats.append(locale_format.to_relative_time_format());
    }

    return formats;
}

}
)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
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

    auto generated_header_file = TRY(open_file(generated_header_path, Core::File::OpenMode::Write));
    auto generated_implementation_file = TRY(open_file(generated_implementation_path, Core::File::OpenMode::Write));

    CLDR cldr;
    TRY(parse_all_locales(dates_path, cldr));

    TRY(generate_unicode_locale_header(*generated_header_file, cldr));
    TRY(generate_unicode_locale_implementation(*generated_implementation_file, cldr));

    return 0;
}
