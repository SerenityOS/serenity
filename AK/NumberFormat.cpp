/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/NumberFormat.h>
#include <AK/StringView.h>

namespace AK {

// FIXME: Remove this hackery once printf() supports floats.
static DeprecatedString number_string_with_one_decimal(u64 number, u64 unit, StringView suffix)
{
    int decimal = (number % unit) * 10 / unit;
    return DeprecatedString::formatted("{}.{} {}", number / unit, decimal, suffix);
}

DeprecatedString human_readable_quantity(u64 quantity, StringView unit)
{
    constexpr u64 size_of_unit = 1024;
    constexpr auto unit_prefixes = AK::Array { "", "K", "M", "G", "T", "P", "E" };
    auto full_unit_suffix = [&](int index) {
        auto binary_infix = (size_of_unit == 1024 && index != 0) ? "i"sv : ""sv;
        return DeprecatedString::formatted("{}{}{}",
            unit_prefixes[index], binary_infix, unit);
    };

    auto size_of_current_unit = size_of_unit;

    if (quantity < size_of_unit)
        return DeprecatedString::formatted("{} {}", quantity, full_unit_suffix(0));

    for (size_t i = 1; i < unit_prefixes.size() - 1; i++) {
        auto suffix = full_unit_suffix(i);
        if (quantity < size_of_unit * size_of_current_unit) {
            return number_string_with_one_decimal(quantity, size_of_current_unit, suffix);
        }

        size_of_current_unit *= size_of_unit;
    }

    return number_string_with_one_decimal(quantity,
        size_of_current_unit, full_unit_suffix(unit_prefixes.size() - 1));
}

DeprecatedString human_readable_size(u64 size)
{
    return human_readable_quantity(size, "B"sv);
}

DeprecatedString human_readable_size_long(u64 size)
{
    if (size < 1 * KiB)
        return DeprecatedString::formatted("{} bytes", size);
    else
        return DeprecatedString::formatted("{} ({} bytes)", human_readable_size(size), size);
}

DeprecatedString human_readable_time(i64 time_in_seconds)
{
    auto days = time_in_seconds / 86400;
    time_in_seconds = time_in_seconds % 86400;

    auto hours = time_in_seconds / 3600;
    time_in_seconds = time_in_seconds % 3600;

    auto minutes = time_in_seconds / 60;
    time_in_seconds = time_in_seconds % 60;

    StringBuilder builder;

    if (days > 0)
        builder.appendff("{} day{} ", days, days == 1 ? "" : "s");

    if (hours > 0)
        builder.appendff("{} hour{} ", hours, hours == 1 ? "" : "s");

    if (minutes > 0)
        builder.appendff("{} minute{} ", minutes, minutes == 1 ? "" : "s");

    builder.appendff("{} second{}", time_in_seconds, time_in_seconds == 1 ? "" : "s");

    return builder.to_deprecated_string();
}

DeprecatedString human_readable_digital_time(i64 time_in_seconds)
{
    auto hours = time_in_seconds / 3600;
    time_in_seconds = time_in_seconds % 3600;

    auto minutes = time_in_seconds / 60;
    time_in_seconds = time_in_seconds % 60;

    StringBuilder builder;

    if (hours > 0)
        builder.appendff("{:02}:", hours);
    builder.appendff("{:02}:", minutes);
    builder.appendff("{:02}", time_in_seconds);

    return builder.to_deprecated_string();
}

}
