/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericLexer.h>
#include <LibTimeZone/DateTime.h>
#include <LibTimeZone/TimeZone.h>

namespace Core {

Optional<StringView> parse_time_zone_name(GenericLexer& lexer)
{
    auto start_position = lexer.tell();

    Optional<StringView> canonicalized_time_zone;

    lexer.ignore_until([&](auto) {
        auto time_zone = lexer.input().substring_view(start_position, lexer.tell() - start_position + 1);

        canonicalized_time_zone = TimeZone::canonicalize_time_zone(time_zone);
        return canonicalized_time_zone.has_value();
    });

    if (canonicalized_time_zone.has_value())
        lexer.ignore();

    return canonicalized_time_zone;
}

void apply_time_zone_offset(StringView time_zone, UnixDateTime& time)
{
    if (auto offset = TimeZone::get_time_zone_offset(time_zone, time); offset.has_value())
        time -= Duration::from_seconds(offset->seconds);
}

}
