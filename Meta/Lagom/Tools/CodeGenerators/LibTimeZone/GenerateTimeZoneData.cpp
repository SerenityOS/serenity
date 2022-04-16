/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "../LibUnicode/GeneratorUtil.h" // FIXME: Move this somewhere common.
#include <AK/DateConstants.h>
#include <AK/Format.h>
#include <AK/HashMap.h>
#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Stream.h>
#include <LibTimeZone/TimeZone.h>

namespace {

using StringIndexType = u8;
constexpr auto s_string_index_type = "u8"sv;

struct DateTime {
    u16 year { 0 };
    Optional<u8> month;
    Optional<u8> day;

    Optional<u8> last_weekday;
    Optional<u8> after_weekday;
    Optional<u8> before_weekday;

    Optional<u8> hour;
    Optional<u8> minute;
    Optional<u8> second;
};

struct TimeZoneOffset {
    i64 offset { 0 };
    Optional<DateTime> until;

    Optional<String> dst_rule;
    Optional<i32> dst_rule_index;
    i64 dst_offset { 0 };

    StringIndexType standard_format { 0 };
    StringIndexType daylight_format { 0 };
};

struct DaylightSavingsOffset {
    i64 offset { 0 };
    u16 year_from { 0 };
    u16 year_to { 0 };
    DateTime in_effect;

    StringIndexType format { 0 };
};

struct TimeZoneData {
    UniqueStringStorage<StringIndexType> unique_strings;

    HashMap<String, Vector<TimeZoneOffset>> time_zones;
    Vector<String> time_zone_names;
    Vector<Alias> time_zone_aliases;

    HashMap<String, Vector<DaylightSavingsOffset>> dst_offsets;
    Vector<String> dst_offset_names;

    HashMap<String, TimeZone::Location> time_zone_coordinates;
};

}

template<>
struct AK::Formatter<DateTime> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, DateTime const& date_time)
    {
        return Formatter<FormatString>::format(builder,
            "{{ {}, {}, {}, {}, {}, {}, {}, {}, {} }}",
            date_time.year,
            date_time.month.value_or(1),
            date_time.day.value_or(1),
            date_time.last_weekday.value_or(0),
            date_time.after_weekday.value_or(0),
            date_time.before_weekday.value_or(0),
            date_time.hour.value_or(0),
            date_time.minute.value_or(0),
            date_time.second.value_or(0));
    }
};

template<>
struct AK::Formatter<TimeZoneOffset> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, TimeZoneOffset const& time_zone_offset)
    {
        return Formatter<FormatString>::format(builder,
            "{{ {}, {}, {}, {}, {}, {}, {} }}",
            time_zone_offset.offset,
            time_zone_offset.until.value_or({}),
            time_zone_offset.until.has_value(),
            time_zone_offset.dst_rule_index.value_or(-1),
            time_zone_offset.dst_offset,
            time_zone_offset.standard_format,
            time_zone_offset.daylight_format);
    }
};

template<>
struct AK::Formatter<DaylightSavingsOffset> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, DaylightSavingsOffset const& dst_offset)
    {
        return Formatter<FormatString>::format(builder,
            "{{ {}, {}, {}, {}, {} }}",
            dst_offset.offset,
            dst_offset.year_from,
            dst_offset.year_to,
            dst_offset.in_effect,
            dst_offset.format);
    }
};

template<>
struct AK::Formatter<TimeZone::Coordinate> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, TimeZone::Coordinate const& coordinate)
    {
        return Formatter<FormatString>::format(builder,
            "{{ {}, {}, {} }}",
            coordinate.degrees,
            coordinate.minutes,
            coordinate.seconds);
    }
};

template<>
struct AK::Formatter<TimeZone::Location> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, TimeZone::Location const& location)
    {
        return Formatter<FormatString>::format(builder,
            "{{ {}, {} }}",
            location.latitude,
            location.longitude);
    }
};

static Optional<DateTime> parse_date_time(Span<StringView const> segments)
{
    auto comment_index = find_index(segments.begin(), segments.end(), "#"sv);
    if (comment_index != segments.size())
        segments = segments.slice(0, comment_index);
    if (segments.is_empty())
        return {};

    DateTime date_time {};
    date_time.year = segments[0].to_uint().value();

    if (segments.size() > 1)
        date_time.month = find_index(short_month_names.begin(), short_month_names.end(), segments[1]) + 1;

    if (segments.size() > 2) {
        if (segments[2].starts_with("last"sv)) {
            auto weekday = segments[2].substring_view("last"sv.length());
            date_time.last_weekday = find_index(short_day_names.begin(), short_day_names.end(), weekday);
        } else if (auto index = segments[2].find(">="sv); index.has_value()) {
            auto weekday = segments[2].substring_view(0, *index);
            date_time.after_weekday = find_index(short_day_names.begin(), short_day_names.end(), weekday);

            auto day = segments[2].substring_view(*index + ">="sv.length());
            date_time.day = day.to_uint().value();
        } else if (auto index = segments[2].find("<="sv); index.has_value()) {
            auto weekday = segments[2].substring_view(0, *index);
            date_time.before_weekday = find_index(short_day_names.begin(), short_day_names.end(), weekday);

            auto day = segments[2].substring_view(*index + "<="sv.length());
            date_time.day = day.to_uint().value();
        } else {
            date_time.day = segments[2].to_uint().value();
        }
    }

    if (segments.size() > 3) {
        // FIXME: Some times end with a letter, e.g. "2:00u" and "2:00s". Figure out what this means and handle it.
        auto time_segments = segments[3].split_view(':');

        date_time.hour = time_segments[0].to_int().value();
        date_time.minute = time_segments.size() > 1 ? time_segments[1].substring_view(0, 2).to_uint().value() : 0;
        date_time.second = time_segments.size() > 2 ? time_segments[2].substring_view(0, 2).to_uint().value() : 0;
    }

    return date_time;
}

static i64 parse_time_offset(StringView segment)
{
    auto segments = segment.split_view(':');

    i64 hours = segments[0].to_int().value();
    i64 minutes = segments.size() > 1 ? segments[1].to_uint().value() : 0;
    i64 seconds = segments.size() > 2 ? segments[2].to_uint().value() : 0;

    i64 sign = ((hours < 0) || (segments[0] == "-0"sv)) ? -1 : 1;
    return (hours * 3600) + sign * ((minutes * 60) + seconds);
}

static void parse_dst_rule(StringView segment, TimeZoneOffset& time_zone)
{
    if (segment.contains(':'))
        time_zone.dst_offset = parse_time_offset(segment);
    else if (segment != "-"sv)
        time_zone.dst_rule = segment;
}

static void parse_format(StringView format, TimeZoneData& time_zone_data, TimeZoneOffset& time_zone)
{
    auto formats = format.replace("%s"sv, "{}"sv).split('/');
    VERIFY(formats.size() <= 2);

    time_zone.standard_format = time_zone_data.unique_strings.ensure(formats[0]);

    if (formats.size() == 2)
        time_zone.daylight_format = time_zone_data.unique_strings.ensure(formats[1]);
    else
        time_zone.daylight_format = time_zone.standard_format;
}

static Vector<TimeZoneOffset>& parse_zone(StringView zone_line, TimeZoneData& time_zone_data)
{
    auto segments = zone_line.split_view_if([](char ch) { return (ch == '\t') || (ch == ' '); });

    // "Zone" NAME STDOFF RULES FORMAT [UNTIL]
    VERIFY(segments[0] == "Zone"sv);
    auto name = segments[1];

    TimeZoneOffset time_zone {};
    time_zone.offset = parse_time_offset(segments[2]);
    parse_dst_rule(segments[3], time_zone);
    parse_format(segments[4], time_zone_data, time_zone);

    if (segments.size() > 5)
        time_zone.until = parse_date_time(segments.span().slice(5));

    auto& time_zones = time_zone_data.time_zones.ensure(name);
    time_zones.append(move(time_zone));

    if (!time_zone_data.time_zone_names.contains_slow(name))
        time_zone_data.time_zone_names.append(name);

    return time_zones;
}

static void parse_zone_continuation(StringView zone_line, TimeZoneData& time_zone_data, Vector<TimeZoneOffset>& time_zones)
{
    auto segments = zone_line.split_view_if([](char ch) { return (ch == '\t') || (ch == ' '); });

    // STDOFF RULES FORMAT [UNTIL]
    TimeZoneOffset time_zone {};
    time_zone.offset = parse_time_offset(segments[0]);
    parse_dst_rule(segments[1], time_zone);
    parse_format(segments[2], time_zone_data, time_zone);

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

static void parse_rule(StringView rule_line, TimeZoneData& time_zone_data)
{
    auto segments = rule_line.split_view_if([](char ch) { return (ch == '\t') || (ch == ' '); });

    // Rule NAME FROM TO TYPE IN ON AT SAVE LETTER/S
    VERIFY(segments[0] == "Rule"sv);
    auto name = segments[1];

    DaylightSavingsOffset dst_offset {};
    dst_offset.offset = parse_time_offset(segments[8]);
    dst_offset.year_from = segments[2].to_uint().value();

    if (segments[3] == "only")
        dst_offset.year_to = dst_offset.year_from;
    else if (segments[3] == "max"sv)
        dst_offset.year_to = NumericLimits<u16>::max();
    else
        dst_offset.year_to = segments[3].to_uint().value();

    auto in_effect = Array { "0"sv, segments[5], segments[6], segments[7] };
    dst_offset.in_effect = parse_date_time(in_effect).release_value();

    if (segments[9] != "-"sv)
        dst_offset.format = time_zone_data.unique_strings.ensure(segments[9]);

    auto& dst_offsets = time_zone_data.dst_offsets.ensure(name);
    dst_offsets.append(move(dst_offset));

    if (!time_zone_data.dst_offset_names.contains_slow(name))
        time_zone_data.dst_offset_names.append(name);
}

static ErrorOr<void> parse_time_zones(StringView time_zone_path, TimeZoneData& time_zone_data)
{
    // For reference, the man page for `zic` has the best documentation of the TZDB file format.
    auto file = TRY(open_file(time_zone_path, Core::Stream::OpenMode::Read));
    Array<u8, 1024> buffer {};

    Vector<TimeZoneOffset>* last_parsed_zone = nullptr;

    while (TRY(file->can_read_line())) {
        auto line = TRY(file->read_line(buffer));

        if (line.is_empty() || line.trim_whitespace(TrimMode::Left).starts_with('#'))
            continue;

        if (line.starts_with("Zone"sv)) {
            last_parsed_zone = &parse_zone(line, time_zone_data);
        } else if (line.starts_with('\t')) {
            VERIFY(last_parsed_zone != nullptr);
            parse_zone_continuation(line, time_zone_data, *last_parsed_zone);
        } else {
            last_parsed_zone = nullptr;

            if (line.starts_with("Link"sv))
                parse_link(line, time_zone_data);
            else if (line.starts_with("Rule"sv))
                parse_rule(line, time_zone_data);
        }
    }

    return {};
}

static ErrorOr<void> parse_time_zone_coordinates(Core::Stream::BufferedFile& file, TimeZoneData& time_zone_data)
{
    auto parse_coordinate = [](auto coordinate) {
        VERIFY(coordinate.substring_view(0, 1).is_one_of("+"sv, "-"sv));
        TimeZone::Coordinate parsed {};

        if (coordinate.length() == 5) {
            // ±DDMM
            parsed.degrees = coordinate.substring_view(0, 3).to_int().value();
            parsed.minutes = coordinate.substring_view(3).to_int().value();
        } else if (coordinate.length() == 6) {
            // ±DDDMM
            parsed.degrees = coordinate.substring_view(0, 4).to_int().value();
            parsed.minutes = coordinate.substring_view(4).to_int().value();
        } else if (coordinate.length() == 7) {
            // ±DDMMSS
            parsed.degrees = coordinate.substring_view(0, 3).to_int().value();
            parsed.minutes = coordinate.substring_view(3, 2).to_int().value();
            parsed.seconds = coordinate.substring_view(5).to_int().value();
        } else if (coordinate.length() == 8) {
            // ±DDDDMMSS
            parsed.degrees = coordinate.substring_view(0, 4).to_int().value();
            parsed.minutes = coordinate.substring_view(4, 2).to_int().value();
            parsed.seconds = coordinate.substring_view(6).to_int().value();
        } else {
            VERIFY_NOT_REACHED();
        }

        return parsed;
    };

    Array<u8, 1024> buffer {};

    while (TRY(file.can_read_line())) {
        auto line = TRY(file.read_line(buffer));

        if (line.is_empty() || line.trim_whitespace(TrimMode::Left).starts_with('#'))
            continue;

        auto segments = line.split_view('\t');
        auto coordinates = segments[1];
        auto zone = segments[2];

        VERIFY(time_zone_data.time_zones.contains(zone));

        auto index = coordinates.find_any_of("+-"sv, StringView::SearchDirection::Backward).value();
        auto latitude = parse_coordinate(coordinates.substring_view(0, index));
        auto longitude = parse_coordinate(coordinates.substring_view(index));

        time_zone_data.time_zone_coordinates.set(zone, { latitude, longitude });
    }

    return {};
}

static void set_dst_rule_indices(TimeZoneData& time_zone_data)
{
    for (auto& time_zone : time_zone_data.time_zones) {
        for (auto& time_zone_offset : time_zone.value) {
            if (!time_zone_offset.dst_rule.has_value())
                continue;

            auto dst_rule_index = time_zone_data.dst_offset_names.find_first_index(*time_zone_offset.dst_rule);
            time_zone_offset.dst_rule_index = static_cast<i32>(dst_rule_index.value());
        }
    }
}

static String format_identifier(StringView owner, String identifier)
{
    constexpr auto gmt_time_zones = Array { "Etc/GMT"sv, "GMT"sv };

    for (auto gmt_time_zone : gmt_time_zones) {
        if (identifier.starts_with(gmt_time_zone)) {
            auto offset = identifier.substring_view(gmt_time_zone.length());

            if (offset.starts_with('+'))
                identifier = String::formatted("{}_Ahead_{}", gmt_time_zone, offset.substring_view(1));
            else if (offset.starts_with('-'))
                identifier = String::formatted("{}_Behind_{}", gmt_time_zone, offset.substring_view(1));
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

static ErrorOr<void> generate_time_zone_data_header(Core::Stream::BufferedFile& file, TimeZoneData& time_zone_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once

#include <AK/Types.h>

namespace TimeZone {
)~~~");

    generate_enum(generator, format_identifier, "TimeZone"sv, {}, time_zone_data.time_zone_names, time_zone_data.time_zone_aliases);
    generate_enum(generator, format_identifier, "DaylightSavingsRule"sv, {}, time_zone_data.dst_offset_names);

    generator.append(R"~~~(
}
)~~~");

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}

static ErrorOr<void> generate_time_zone_data_implementation(Core::Stream::BufferedFile& file, TimeZoneData& time_zone_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.set("string_index_type"sv, s_string_index_type);

    set_dst_rule_indices(time_zone_data);

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/BinarySearch.h>
#include <AK/Optional.h>
#include <AK/Span.h>
#include <AK/StringView.h>
#include <AK/Time.h>
#include <LibTimeZone/TimeZone.h>
#include <LibTimeZone/TimeZoneData.h>

namespace TimeZone {

struct DateTime {
    AK::Time time_since_epoch() const
    {
        // FIXME: This implementation does not take last_weekday, after_weekday, or before_weekday into account.
        return AK::Time::from_timestamp(year, month, day, hour, minute, second, 0);
    }

    u16 year { 0 };
    u8 month { 1 };
    u8 day { 1 };

    u8 last_weekday { 0 };
    u8 after_weekday { 0 };
    u8 before_weekday { 0 };

    u8 hour { 0 };
    u8 minute { 0 };
    u8 second { 0 };
};

struct TimeZoneOffset {
    i64 offset { 0 };

    DateTime until {};
    bool has_until { false };

    i32 dst_rule { -1 };
    i64 dst_offset { 0 };

    @string_index_type@ standard_format { 0 };
    @string_index_type@ daylight_format { 0 };
};

struct DaylightSavingsOffset {
    AK::Time time_in_effect(AK::Time time) const
    {
        auto in_effect = this->in_effect;
        in_effect.year = seconds_since_epoch_to_year(time.to_seconds());

        return in_effect.time_since_epoch();
    }

    i64 offset { 0 };
    u16 year_from { 0 };
    u16 year_to { 0 };
    DateTime in_effect {};

    @string_index_type@ format { 0 };
};
)~~~");

    time_zone_data.unique_strings.generate(generator);

    auto append_offsets = [&](auto const& name, auto type, auto const& offsets) {
        generator.set("name", name);
        generator.set("type", type);
        generator.set("size", String::number(offsets.size()));

        generator.append(R"~~~(
static constexpr Array<@type@, @size@> @name@ { {
)~~~");

        for (auto const& offset : offsets)
            generator.append(String::formatted("    {},\n", offset));

        generator.append("} };\n");
    };

    generate_mapping(generator, time_zone_data.time_zone_names, "TimeZoneOffset"sv, "s_time_zone_offsets"sv, "s_time_zone_offsets_{}", format_identifier,
        [&](auto const& name, auto const& value) {
            auto const& time_zone_offsets = time_zone_data.time_zones.find(value)->value;
            append_offsets(name, "TimeZoneOffset"sv, time_zone_offsets);
        });

    generate_mapping(generator, time_zone_data.dst_offset_names, "DaylightSavingsOffset"sv, "s_dst_offsets"sv, "s_dst_offsets_{}", format_identifier,
        [&](auto const& name, auto const& value) {
            auto const& dst_offsets = time_zone_data.dst_offsets.find(value)->value;
            append_offsets(name, "DaylightSavingsOffset"sv, dst_offsets);
        });

    generator.set("size", String::number(time_zone_data.time_zone_names.size()));
    generator.append(R"~~~(
static constexpr Array<Location, @size@> s_time_zone_locations { {
)~~~");

    for (auto const& time_zone : time_zone_data.time_zone_names) {
        auto location = time_zone_data.time_zone_coordinates.get(time_zone).value_or({});

        generator.append(String::formatted("    {},\n", location));
    }
    generator.append("} };\n");

    auto append_string_conversions = [&](StringView enum_title, StringView enum_snake, auto const& values, Vector<Alias> const& aliases = {}) {
        HashValueMap<String> hashes;
        hashes.ensure_capacity(values.size());

        auto hash = [](auto const& value) {
            return CaseInsensitiveStringViewTraits::hash(value);
        };

        for (auto const& value : values)
            hashes.set(hash(value), format_identifier(enum_title, value));
        for (auto const& alias : aliases)
            hashes.set(hash(alias.alias), format_identifier(enum_title, alias.alias));

        ValueFromStringOptions options {};
        options.sensitivity = CaseSensitivity::CaseInsensitive;

        generate_value_from_string(generator, "{}_from_string"sv, enum_title, enum_snake, move(hashes), options);
        generate_value_to_string(generator, "{}_to_string"sv, enum_title, enum_snake, format_identifier, values);
    };

    append_string_conversions("TimeZone"sv, "time_zone"sv, time_zone_data.time_zone_names, time_zone_data.time_zone_aliases);
    append_string_conversions("DaylightSavingsRule"sv, "daylight_savings_rule"sv, time_zone_data.dst_offset_names);

    generator.append(R"~~~(
static Array<DaylightSavingsOffset const*, 2> find_dst_offsets(TimeZoneOffset const& time_zone_offset, AK::Time time)
{
    auto const& dst_rules = s_dst_offsets[time_zone_offset.dst_rule];

    DaylightSavingsOffset const* standard_offset = nullptr;
    DaylightSavingsOffset const* daylight_offset = nullptr;

    auto preferred_rule = [&](auto* current_offset, auto& new_offset) {
        if (!current_offset)
            return &new_offset;

        auto new_time_in_effect = new_offset.time_in_effect(time);
        return (time >= new_time_in_effect) ? &new_offset : current_offset;
    };

    for (size_t index = 0; (index < dst_rules.size()) && (!standard_offset || !daylight_offset); ++index) {
        auto const& dst_rule = dst_rules[index];

        auto year_from = AK::Time::from_timestamp(dst_rule.year_from, 1, 1, 0, 0, 0, 0);
        auto year_to = AK::Time::from_timestamp(dst_rule.year_to + 1, 1, 1, 0, 0, 0, 0);
        if ((time < year_from) || (time >= year_to))
            continue;

        if (dst_rule.offset == 0)
            standard_offset = preferred_rule(standard_offset, dst_rule);
        else
            daylight_offset = preferred_rule(daylight_offset, dst_rule);
    }

    // In modern times, there will always be a standard rule in the TZDB, but that isn't true in
    // all time zones in or before the early 1900s. For example, the "US" rules begin in 1918.
    if (!standard_offset) {
        static DaylightSavingsOffset const empty_offset {};
        return { &empty_offset, &empty_offset };
    }

    return { standard_offset, daylight_offset ? daylight_offset : standard_offset };
}

static Offset get_active_dst_offset(TimeZoneOffset const& time_zone_offset, AK::Time time)
{
    auto offsets = find_dst_offsets(time_zone_offset, time);
    if (offsets[0] == offsets[1])
        return { offsets[0]->offset, InDST::No };

    auto standard_time_in_effect = offsets[0]->time_in_effect(time);
    auto daylight_time_in_effect = offsets[1]->time_in_effect(time);

    if (daylight_time_in_effect < standard_time_in_effect) {
        if ((time < daylight_time_in_effect) || (time >= standard_time_in_effect))
            return { offsets[0]->offset, InDST::No };
    } else {
        if ((time >= standard_time_in_effect) && (time < daylight_time_in_effect))
            return { offsets[0]->offset, InDST::No };
    }

    return { offsets[1]->offset, InDST::Yes };
}

static TimeZoneOffset const& find_time_zone_offset(TimeZone time_zone, AK::Time time)
{
    auto const& time_zone_offsets = s_time_zone_offsets[to_underlying(time_zone)];

    size_t index = 0;
    for (; index < time_zone_offsets.size(); ++index) {
        auto const& time_zone_offset = time_zone_offsets[index];

        if (!time_zone_offset.has_until || (time_zone_offset.until.time_since_epoch() > time))
            break;
    }

    VERIFY(index < time_zone_offsets.size());
    return time_zone_offsets[index];
}

Optional<Offset> get_time_zone_offset(TimeZone time_zone, AK::Time time)
{
    auto const& time_zone_offset = find_time_zone_offset(time_zone, time);

    Offset dst_offset {};
    if (time_zone_offset.dst_rule != -1) {
        dst_offset = get_active_dst_offset(time_zone_offset, time);
    } else {
        auto in_dst = time_zone_offset.dst_offset == 0 ? InDST::No : InDST::Yes;
        dst_offset = { time_zone_offset.dst_offset, in_dst };
    }

    dst_offset.seconds += time_zone_offset.offset;
    return dst_offset;
}

Optional<Array<NamedOffset, 2>> get_named_time_zone_offsets(TimeZone time_zone, AK::Time time)
{
    auto const& time_zone_offset = find_time_zone_offset(time_zone, time);
    Array<NamedOffset, 2> named_offsets;

    auto format_name = [](auto format, auto offset) -> String {
        if (offset == 0)
            return s_string_list[format].replace("{}"sv, ""sv);
        return String::formatted(s_string_list[format], s_string_list[offset]);
    };

    auto set_named_offset = [&](auto& named_offset, auto dst_offset, auto in_dst, auto format, auto offset) {
        named_offset.seconds = time_zone_offset.offset + dst_offset;
        named_offset.in_dst = in_dst;
        named_offset.name = format_name(format, offset);
    };

    if (time_zone_offset.dst_rule != -1) {
        auto offsets = find_dst_offsets(time_zone_offset, time);
        auto in_dst = offsets[1]->offset == 0 ? InDST::No : InDST::Yes;

        set_named_offset(named_offsets[0], offsets[0]->offset, InDST::No, time_zone_offset.standard_format, offsets[0]->format);
        set_named_offset(named_offsets[1], offsets[1]->offset, in_dst, time_zone_offset.daylight_format, offsets[1]->format);
    } else {
        auto in_dst = time_zone_offset.dst_offset == 0 ? InDST::No : InDST::Yes;
        set_named_offset(named_offsets[0], time_zone_offset.dst_offset, in_dst, time_zone_offset.standard_format, 0);
        set_named_offset(named_offsets[1], time_zone_offset.dst_offset, in_dst, time_zone_offset.daylight_format, 0);
    }

    return named_offsets;
}

Optional<Location> get_time_zone_location(TimeZone time_zone)
{
    auto is_valid_coordinate = [](auto const& coordinate) {
        return (coordinate.degrees != 0) || (coordinate.minutes != 0) || (coordinate.seconds != 0);
    };

    auto const& location = s_time_zone_locations[to_underlying(time_zone)];

    if (is_valid_coordinate(location.latitude) && is_valid_coordinate(location.longitude))
        return location;
    return {};
}
)~~~");

    generate_available_values(generator, "all_time_zones"sv, time_zone_data.time_zone_names);

    generator.append(R"~~~(

}
)~~~");

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView time_zone_coordinates_path;
    Vector<StringView> time_zone_paths;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the time zone data header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the time zone data implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(time_zone_coordinates_path, "Path to the time zone data coordinates file", "time-zone-coordinates-path", 'z', "time-zone-coordinates-path");
    args_parser.add_positional_argument(time_zone_paths, "Paths to the time zone database files", "time-zone-paths");
    args_parser.parse(arguments);

    auto generated_header_file = TRY(open_file(generated_header_path, Core::Stream::OpenMode::Write));
    auto generated_implementation_file = TRY(open_file(generated_implementation_path, Core::Stream::OpenMode::Write));
    auto time_zone_coordinates_file = TRY(open_file(time_zone_coordinates_path, Core::Stream::OpenMode::Read));

    TimeZoneData time_zone_data {};
    for (auto time_zone_path : time_zone_paths)
        TRY(parse_time_zones(time_zone_path, time_zone_data));

    TRY(parse_time_zone_coordinates(*time_zone_coordinates_file, time_zone_data));

    TRY(generate_time_zone_data_header(*generated_header_file, time_zone_data));
    TRY(generate_time_zone_data_implementation(*generated_implementation_file, time_zone_data));

    return 0;
}
