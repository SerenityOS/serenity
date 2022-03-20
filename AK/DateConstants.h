/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/StringView.h>

namespace AK {

static constexpr Array<StringView, 7> long_day_names = {
    "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
};

static constexpr Array<StringView, 7> short_day_names = {
    "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
};

static constexpr Array<StringView, 7> mini_day_names = {
    "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"
};

static constexpr Array<StringView, 7> micro_day_names = {
    "S", "M", "T", "W", "T", "F", "S"
};

static constexpr Array<StringView, 12> long_month_names = {
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
};

static constexpr Array<StringView, 12> short_month_names = {
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
};

}

using AK::long_day_names;
using AK::long_month_names;
using AK::micro_day_names;
using AK::mini_day_names;
using AK::short_day_names;
using AK::short_month_names;
