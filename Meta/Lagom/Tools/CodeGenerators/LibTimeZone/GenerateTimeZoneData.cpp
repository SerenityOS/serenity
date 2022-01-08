/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "../LibUnicode/GeneratorUtil.h" // FIXME: Move this somewhere common.
#include <AK/HashMap.h>
#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>

namespace {

struct Time {
    i8 hour { 0 };
    u8 minute { 0 };
    u8 second { 0 };
};

struct DateTime {
    u16 year { 0 };
    Optional<u8> month;
    Optional<u8> last_weekday;
    Optional<u8> after_weekday;
    Optional<u8> day;
    Optional<Time> time;
};

struct TimeZone {
    Time offset;
    Optional<DateTime> until;
};

struct TimeZoneData {
    HashMap<String, Vector<TimeZone>> time_zones;
    Vector<String> time_zone_names;
    Vector<Alias> time_zone_aliases;
};

static Time parse_time(StringView segment)
{
    // FIXME: Some times end with a letter, e.g. "2:00u" and "2:00s". Figure out what this means and handle it.
    auto segments = segment.split_view(':');

    Time time {};
    time.hour = segments[0].to_int().value();
    time.minute = segments.size() > 1 ? segments[1].substring_view(0, 2).to_uint().value() : 0;
    time.second = segments.size() > 2 ? segments[2].substring_view(0, 2).to_uint().value() : 0;

    return time;
}

static Optional<DateTime> parse_date_time(Span<StringView const> segments)
{
    constexpr auto months = Array { "Jan"sv, "Feb"sv, "Mar"sv, "Apr"sv, "May"sv, "Jun"sv, "Jul"sv, "Aug"sv, "Sep"sv, "Oct"sv, "Nov"sv, "Dec"sv };
    constexpr auto weekdays = Array { "Sun"sv, "Mon"sv, "Tue"sv, "Wed"sv, "Thu"sv, "Fri"sv, "Sat"sv };

    auto comment_index = find_index(segments.begin(), segments.end(), "#"sv);
    if (comment_index != segments.size())
        segments = segments.slice(0, comment_index);
    if (segments.is_empty())
        return {};

    DateTime date_time {};
    date_time.year = segments[0].to_uint().value();

    if (segments.size() > 1)
        date_time.month = find_index(months.begin(), months.end(), segments[1]);

    if (segments.size() > 2) {
        if (segments[2].starts_with("last"sv)) {
            auto weekday = segments[2].substring_view("last"sv.length());
            date_time.last_weekday = find_index(weekdays.begin(), weekdays.end(), weekday);
        } else if (auto index = segments[2].find(">="sv); index.has_value()) {
            auto weekday = segments[2].substring_view(0, *index);
            date_time.after_weekday = find_index(weekdays.begin(), weekdays.end(), weekday);

            auto day = segments[2].substring_view(*index + ">="sv.length());
            date_time.day = day.to_uint().value();
        } else {
            date_time.day = segments[2].to_uint().value();
        }
    }

    if (segments.size() > 3)
        date_time.time = parse_time(segments[3]);

    return date_time;
}

static Vector<TimeZone>& parse_zone(StringView zone_line, TimeZoneData& time_zone_data)
{
    auto segments = zone_line.split_view_if([](char ch) { return (ch == '\t') || (ch == ' '); });

    // "Zone" NAME STDOFF RULES FORMAT [UNTIL]
    VERIFY(segments[0] == "Zone"sv);
    auto name = segments[1];

    TimeZone time_zone {};
    time_zone.offset = parse_time(segments[2]);

    if (segments.size() > 5)
        time_zone.until = parse_date_time(segments.span().slice(5));

    auto& time_zones = time_zone_data.time_zones.ensure(name);
    time_zones.append(move(time_zone));

    if (!time_zone_data.time_zone_names.contains_slow(name))
        time_zone_data.time_zone_names.append(name);

    return time_zones;
}

static void parse_zone_continuation(StringView zone_line, Vector<TimeZone>& time_zones)
{
    auto segments = zone_line.split_view_if([](char ch) { return (ch == '\t') || (ch == ' '); });

    // STDOFF RULES FORMAT [UNTIL]
    TimeZone time_zone {};
    time_zone.offset = parse_time(segments[0]);

    if (segments.size() > 3)
        time_zone.until = parse_date_time(segments.span().slice(3));

    time_zones.append(move(time_zone));
}

static void parse_link(StringView link_line, TimeZoneData& time_zone_data)
{
    auto segments = link_line.split_view_if([](char ch) { return (ch == '\t') || (ch == ' '); });

    // Link TARGET LINK-NAME
    VERIFY(segments[0] == "Link"sv);
    auto target = segments[1];
    auto alias = segments[2];

    time_zone_data.time_zone_aliases.append({ target, alias });
}

static ErrorOr<void> parse_time_zones(StringView time_zone_path, TimeZoneData& time_zone_data)
{
    // For reference, the man page for `zic` has the best documentation of the TZDB file format.
    auto file = TRY(Core::File::open(time_zone_path, Core::OpenMode::ReadOnly));
    Vector<TimeZone>* last_parsed_zone = nullptr;

    while (file->can_read_line()) {
        auto line = file->read_line();
        if (line.is_empty() || line.trim_whitespace(TrimMode::Left).starts_with('#'))
            continue;

        if (line.starts_with("Zone"sv)) {
            last_parsed_zone = &parse_zone(line, time_zone_data);
        } else if (line.starts_with('\t')) {
            VERIFY(last_parsed_zone != nullptr);
            parse_zone_continuation(line, *last_parsed_zone);
        } else {
            last_parsed_zone = nullptr;

            if (line.starts_with("Link"sv))
                parse_link(line, time_zone_data);
        }
    }

    return {};
}

static String format_identifier(StringView owner, String identifier)
{
    constexpr auto gmt_time_zones = Array { "Etc/GMT"sv, "GMT"sv };

    for (auto gmt_time_zone : gmt_time_zones) {
        if (identifier.starts_with(gmt_time_zone)) {
            auto offset = identifier.substring_view(gmt_time_zone.length());

            if (offset.starts_with('+'))
                identifier = String::formatted("{}_P{}", gmt_time_zone, offset.substring_view(1));
            else if (offset.starts_with('-'))
                identifier = String::formatted("{}_M{}", gmt_time_zone, offset.substring_view(1));
        }
    }

    identifier = identifier.replace("-"sv, "_"sv, true);
    identifier = identifier.replace("/"sv, "_"sv, true);

    if (all_of(identifier, is_ascii_digit))
        return String::formatted("{}_{}", owner[0], identifier);
    if (is_ascii_lower_alpha(identifier[0]))
        return String::formatted("{:c}{}", to_ascii_uppercase(identifier[0]), identifier.substring_view(1));
    return identifier;
}

static void generate_time_zone_data_header(Core::File& file, TimeZoneData& time_zone_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once

#include <AK/Types.h>

namespace TimeZone {
)~~~");

    generate_enum(generator, format_identifier, "TimeZone"sv, {}, time_zone_data.time_zone_names, time_zone_data.time_zone_aliases);

    generator.append(R"~~~(
}
)~~~");

    VERIFY(file.write(generator.as_string_view()));
}

static void generate_time_zone_data_implementation(Core::File& file, TimeZoneData& time_zone_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/BinarySearch.h>
#include <AK/Optional.h>
#include <AK/StringView.h>
#include <LibTimeZone/TimeZone.h>
#include <LibTimeZone/TimeZoneData.h>

namespace TimeZone {
)~~~");

    auto append_from_string = [&](StringView enum_title, StringView enum_snake, auto const& values, auto const& aliases) {
        HashValueMap<String> hashes;
        hashes.ensure_capacity(values.size());

        for (auto const& value : values)
            hashes.set(value.hash(), format_identifier(enum_title, value));
        for (auto const& alias : aliases)
            hashes.set(alias.alias.hash(), format_identifier(enum_title, alias.alias));

        generate_value_from_string(generator, "{}_from_string"sv, enum_title, enum_snake, move(hashes));
    };

    append_from_string("TimeZone"sv, "time_zone"sv, time_zone_data.time_zone_names, time_zone_data.time_zone_aliases);

    generator.append(R"~~~(
}
)~~~");

    VERIFY(file.write(generator.as_string_view()));
}

}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    Vector<StringView> time_zone_paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the time zone data header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the time zone data implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_positional_argument(time_zone_paths, "Paths to the time zone database files", "time-zone-paths");
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

    TimeZoneData time_zone_data {};
    for (auto time_zone_path : time_zone_paths)
        TRY(parse_time_zones(time_zone_path, time_zone_data));

    generate_time_zone_data_header(generated_header_file, time_zone_data);
    generate_time_zone_data_implementation(generated_implementation_file, time_zone_data);

    return 0;
}
