/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
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
#include <LibUnicode/DateTimeFormat.h>

using StringIndexType = u16;
constexpr auto s_string_index_type = "u16"sv;

struct CalendarPattern : public Unicode::CalendarPattern {
    StringIndexType pattern_index { 0 };
};

struct CalendarFormat {
    CalendarPattern full_format {};
    CalendarPattern long_format {};
    CalendarPattern medium_format {};
    CalendarPattern short_format {};
};

struct Calendar {
    StringIndexType calendar { 0 };
    CalendarFormat date_formats {};
    CalendarFormat time_formats {};
    CalendarFormat date_time_formats {};
    Vector<CalendarPattern> available_formats {};
};

struct Locale {
    HashMap<String, Calendar> calendars;
};

struct UnicodeLocaleData {
    UniqueStringStorage<StringIndexType> unique_strings;
    HashMap<String, Locale> locales;

    Vector<String> calendars;
    Vector<Alias> calendar_aliases {
        // FIXME: Aliases should come from BCP47. See: https://unicode-org.atlassian.net/browse/CLDR-15158
        { "gregorian"sv, "gregory"sv },
    };

    size_t max_available_formats_size { 0 };
};

static void parse_date_time_pattern(CalendarPattern& format, String pattern, UnicodeLocaleData& locale_data)
{
    // FIXME: This is very incomplete. Similar to NumberFormat, the pattern string will need to be
    //        parsed to fill in the CalendarPattern struct, and modified to be useable at runtime.
    //        For now, this is enough to implement the DateTimeFormat constructor.
    //
    // https://unicode.org/reports/tr35/tr35-dates.html#Date_Field_Symbol_Table
    format.pattern_index = locale_data.unique_strings.ensure(move(pattern));
}

static ErrorOr<void> parse_calendars(String locale_calendars_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath calendars_path(move(locale_calendars_path));
    if (!calendars_path.basename().starts_with("ca-"sv))
        return {};

    auto calendars_file = TRY(Core::File::open(calendars_path.string(), Core::OpenMode::ReadOnly));
    auto calendars = TRY(JsonValue::from_string(calendars_file->read_all()));

    auto const& main_object = calendars.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(calendars_path.parent().basename());
    auto const& dates_object = locale_object.as_object().get("dates"sv);
    auto const& calendars_object = dates_object.as_object().get("calendars"sv);

    auto ensure_calendar = [&](auto const& calendar) -> Calendar& {
        return locale.calendars.ensure(calendar, [&]() {
            auto calendar_index = locale_data.unique_strings.ensure(calendar);
            return Calendar { .calendar = calendar_index };
        });
    };

    auto parse_patterns = [&](auto& formats, auto const& patterns_object) {
        auto full_format = patterns_object.get("full"sv);
        parse_date_time_pattern(formats.full_format, full_format.as_string(), locale_data);

        auto long_format = patterns_object.get("long"sv);
        parse_date_time_pattern(formats.long_format, long_format.as_string(), locale_data);

        auto medium_format = patterns_object.get("medium"sv);
        parse_date_time_pattern(formats.medium_format, medium_format.as_string(), locale_data);

        auto short_format = patterns_object.get("short"sv);
        parse_date_time_pattern(formats.short_format, short_format.as_string(), locale_data);
    };

    calendars_object.as_object().for_each_member([&](auto const& calendar_name, JsonValue const& value) {
        auto& calendar = ensure_calendar(calendar_name);

        if (!locale_data.calendars.contains_slow(calendar_name))
            locale_data.calendars.append(calendar_name);

        auto const& date_formats_object = value.as_object().get("dateFormats"sv);
        parse_patterns(calendar.date_formats, date_formats_object.as_object());

        auto const& time_formats_object = value.as_object().get("timeFormats"sv);
        parse_patterns(calendar.time_formats, time_formats_object.as_object());

        auto const& date_time_formats_object = value.as_object().get("dateTimeFormats"sv);
        parse_patterns(calendar.date_time_formats, date_time_formats_object.as_object());

        auto const& available_formats = date_time_formats_object.as_object().get("availableFormats"sv);
        available_formats.as_object().for_each_member([&](auto const&, JsonValue const& pattern) {
            CalendarPattern format {};
            parse_date_time_pattern(format, pattern.as_string(), locale_data);
            calendar.available_formats.append(move(format));
        });

        locale_data.max_available_formats_size = max(locale_data.max_available_formats_size, calendar.available_formats.size());
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
        auto calendars_iterator = TRY(path_to_dir_iterator(dates_path, {}));

        auto language = TRY(remove_variants_from_path(dates_path));
        auto& locale = locale_data.locales.ensure(language);

        while (calendars_iterator.has_next()) {
            auto calendars_path = TRY(next_path_from_dir_iterator(calendars_iterator));
            TRY(parse_calendars(move(calendars_path), locale_data, locale));
        }
    }

    return {};
}

static String format_identifier(StringView, StringView identifier)
{
    return identifier.to_titlecase_string();
}

static void generate_unicode_locale_header(Core::File& file, UnicodeLocaleData& locale_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <LibUnicode/Forward.h>

namespace Unicode {
)~~~");

    generate_enum(generator, format_identifier, "Calendar"sv, {}, locale_data.calendars, locale_data.calendar_aliases);

    generator.append(R"~~~(
namespace Detail {

Optional<Calendar> calendar_from_string(StringView calendar);
Optional<Unicode::CalendarFormat> get_calendar_date_format(StringView locale, StringView calendar);
Optional<Unicode::CalendarFormat> get_calendar_time_format(StringView locale, StringView calendar);
Optional<Unicode::CalendarFormat> get_calendar_date_time_format(StringView locale, StringView calendar);
Vector<Unicode::CalendarPattern> get_calendar_available_formats(StringView locale, StringView calendar);

}

}
)~~~");

    VERIFY(file.write(generator.as_string_view()));
}

static void generate_unicode_locale_implementation(Core::File& file, UnicodeLocaleData& locale_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.set("string_index_type"sv, s_string_index_type);
    generator.set("available_formats_size"sv, String::number(locale_data.max_available_formats_size));

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/BinarySearch.h>
#include <LibUnicode/DateTimeFormat.h>
#include <LibUnicode/Locale.h>
#include <LibUnicode/UnicodeDateTimeFormat.h>

namespace Unicode::Detail {
)~~~");

    locale_data.unique_strings.generate(generator);

    generator.append(R"~~~(
struct CalendarPattern {
    Unicode::CalendarPattern to_unicode_calendar_pattern() const {
        Unicode::CalendarPattern calendar_pattern {};
        calendar_pattern.pattern = s_string_list[pattern];
        return calendar_pattern;
    }

    @string_index_type@ pattern { 0 };
};

struct CalendarFormat {
    Unicode::CalendarFormat to_unicode_calendar_format() const {
        Unicode::CalendarFormat calendar_format {};

        calendar_format.full_format = full_format.to_unicode_calendar_pattern();
        calendar_format.long_format = long_format.to_unicode_calendar_pattern();
        calendar_format.medium_format = medium_format.to_unicode_calendar_pattern();
        calendar_format.short_format = short_format.to_unicode_calendar_pattern();

        return calendar_format;
    }

    CalendarPattern full_format {};
    CalendarPattern long_format {};
    CalendarPattern medium_format {};
    CalendarPattern short_format {};
};

struct CalendarData {
    @string_index_type@ calendar { 0 };
    CalendarFormat date_formats {};
    CalendarFormat time_formats {};
    CalendarFormat date_time_formats {};
    Array<CalendarPattern, @available_formats_size@> available_formats {};
    size_t available_formats_size { 0 };
};
)~~~");

    auto append_calendar_pattern = [&](auto const& calendar_pattern) {
        generator.set("pattern"sv, String::number(calendar_pattern.pattern_index));
        generator.append("{ @pattern@ },");
    };

    auto append_calendar_format = [&](auto const& calendar_format) {
        generator.append("{ ");
        append_calendar_pattern(calendar_format.full_format);
        generator.append(" ");
        append_calendar_pattern(calendar_format.long_format);
        generator.append(" ");
        append_calendar_pattern(calendar_format.medium_format);
        generator.append(" ");
        append_calendar_pattern(calendar_format.short_format);
        generator.append(" },");
    };

    auto append_calendars = [&](String name, auto const& calendars) {
        generator.set("name", name);
        generator.set("size", String::number(calendars.size()));

        generator.append(R"~~~(
static constexpr Array<CalendarData, @size@> @name@ { {)~~~");

        for (auto const& calendar_key : locale_data.calendars) {
            auto const& calendar = calendars.find(calendar_key)->value;

            generator.set("calendar"sv, String::number(calendar.calendar));
            generator.append(R"~~~(
    { @calendar@, )~~~");

            append_calendar_format(calendar.date_formats);
            generator.append(" ");
            append_calendar_format(calendar.time_formats);
            generator.append(" ");
            append_calendar_format(calendar.date_time_formats);
            generator.append(" {{");

            for (auto const& format : calendar.available_formats) {
                generator.append(" ");
                append_calendar_pattern(format);
            }

            generator.set("size", String::number(calendar.available_formats.size()));
            generator.append(" }}, @size@ },");
        }

        generator.append(R"~~~(
} };
)~~~");
    };

    generate_mapping(generator, locale_data.locales, "CalendarData"sv, "s_calendars"sv, "s_calendars_{}", [&](auto const& name, auto const& value) { append_calendars(name, value.calendars); });

    auto append_from_string = [&](StringView enum_title, StringView enum_snake, auto const& values, auto const& aliases) {
        HashValueMap<String> hashes;
        hashes.ensure_capacity(values.size());

        for (auto const& value : values)
            hashes.set(value.hash(), format_identifier(enum_title, value));
        for (auto const& alias : aliases)
            hashes.set(alias.alias.hash(), format_identifier(enum_title, alias.alias));

        generate_value_from_string(generator, "{}_from_string"sv, enum_title, enum_snake, move(hashes));
    };

    append_from_string("Calendar"sv, "calendar"sv, locale_data.calendars, locale_data.calendar_aliases);

    generator.append(R"~~~(
static CalendarData const* find_calendar_data(StringView locale, StringView calendar)
{
    auto locale_value = locale_from_string(locale);
    if (!locale_value.has_value())
        return nullptr;

    auto calendar_value = calendar_from_string(calendar);
    if (!calendar_value.has_value())
        return nullptr;

    auto locale_index = to_underlying(*locale_value) - 1; // Subtract 1 because 0 == Locale::None.
    auto calendar_index = to_underlying(*calendar_value);

    auto const& calendars = s_calendars.at(locale_index);
    return &calendars[calendar_index];
}

Optional<Unicode::CalendarFormat> get_calendar_date_format(StringView locale, StringView calendar)
{
    if (auto const* data = find_calendar_data(locale, calendar); data != nullptr)
        return data->date_formats.to_unicode_calendar_format();
    return {};
}

Optional<Unicode::CalendarFormat> get_calendar_time_format(StringView locale, StringView calendar)
{
    if (auto const* data = find_calendar_data(locale, calendar); data != nullptr)
        return data->time_formats.to_unicode_calendar_format();
    return {};
}

Optional<Unicode::CalendarFormat> get_calendar_date_time_format(StringView locale, StringView calendar)
{
    if (auto const* data = find_calendar_data(locale, calendar); data != nullptr)
        return data->date_time_formats.to_unicode_calendar_format();
    return {};
}

Vector<Unicode::CalendarPattern> get_calendar_available_formats(StringView locale, StringView calendar)
{
    Vector<Unicode::CalendarPattern> result {};

    if (auto const* data = find_calendar_data(locale, calendar); data != nullptr) {
        result.ensure_capacity(data->available_formats_size);

        for (size_t i = 0; i < data->available_formats_size; ++i)
            result.unchecked_append(data->available_formats[i].to_unicode_calendar_pattern());
    }

    return result;
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
