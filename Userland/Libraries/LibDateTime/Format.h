/*
 * Copyright (c) 2024, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/StringView.h>

namespace DateTime {

constexpr StringView ISO8601_FULL_FORMAT = "{Y}-{m}-{d}T{H}:{M}:{S}.{f}{z}"sv;
// Date and time without fractional seconds.
constexpr StringView ISO8601_SHORT_FORMAT = "{Y}-{m}-{d}T{H}:{M}:{S}{z}"sv;
constexpr StringView ISO8601_DATE_FORMAT = "{Y}-{m}-{d}"sv;
// Time without fractional seconds.
constexpr StringView ISO8601_SHORT_TIME_FORMAT = "{H}:{M}:{S}{z}"sv;

enum FormatField {
    // Y
    Year,
    // m
    Month,
    // d
    Day,
    // H, 24h clock hour
    Hour24,
    // I, 12h clock hour
    Hour12,
    // M
    Minute,
    // S
    Second,
    // f
    SecondFraction,
    // Z
    TimezoneName,
    // z
    TimezoneOffset,
    // 0z
    TimezoneOffsetWithColon,
};

AK::StandardFormatter default_format_for_field(FormatField field);

Optional<FormatField> parse_field_name(StringView name);

}
