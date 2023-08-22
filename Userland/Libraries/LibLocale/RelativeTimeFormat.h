/*
 * Copyright (c) 2022-2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibLocale/Forward.h>
#include <LibLocale/Locale.h>

namespace Locale {

// These are just the subset of fields in the CLDR required for ECMA-402.
enum class TimeUnit {
    Second,
    Minute,
    Hour,
    Day,
    Week,
    Month,
    Quarter,
    Year,
};

struct RelativeTimeFormat {
    PluralCategory plurality;
    StringView pattern;
};

Optional<TimeUnit> time_unit_from_string(StringView time_unit);
StringView time_unit_to_string(TimeUnit time_unit);

Vector<RelativeTimeFormat> get_relative_time_format_patterns(StringView locale, TimeUnit time_unit, StringView tense_or_number, Style style);

}
