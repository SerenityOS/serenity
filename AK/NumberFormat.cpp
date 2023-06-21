/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/DeprecatedString.h>
#include <AK/NumberFormat.h>
#include <AK/NumericLimits.h>
#include <AK/StringView.h>

namespace AK {

// FIXME: Remove this hackery once printf() supports floats.
static DeprecatedString number_string_with_one_decimal(u64 number, u64 unit, StringView suffix, UseThousandsSeparator use_thousands_separator)
{
    constexpr auto max_unit_size = NumericLimits<u64>::max() / 10;
    VERIFY(unit < max_unit_size);

    auto integer_part = number / unit;
    auto decimal_part = (number % unit) * 10 / unit;
    if (use_thousands_separator == UseThousandsSeparator::Yes)
        return DeprecatedString::formatted("{:'d}.{} {}", integer_part, decimal_part, suffix);

    return DeprecatedString::formatted("{}.{} {}", integer_part, decimal_part, suffix);
}

DeprecatedString human_readable_quantity(u64 quantity, HumanReadableBasedOn based_on, StringView unit, UseThousandsSeparator use_thousands_separator)
{
    u64 size_of_unit = based_on == HumanReadableBasedOn::Base2 ? 1024 : 1000;
    constexpr auto unit_prefixes = AK::Array { "", "K", "M", "G", "T", "P", "E" };
    auto full_unit_suffix = [&](int index) {
        auto binary_infix = (based_on == HumanReadableBasedOn::Base2 && index != 0) ? "i"sv : ""sv;
        return DeprecatedString::formatted("{}{}{}",
            unit_prefixes[index], binary_infix, unit);
    };

    auto size_of_current_unit = size_of_unit;

    if (quantity < size_of_unit)
        return DeprecatedString::formatted("{} {}", quantity, full_unit_suffix(0));

    for (size_t i = 1; i < unit_prefixes.size() - 1; i++) {
        auto suffix = full_unit_suffix(i);
        if (quantity < size_of_unit * size_of_current_unit) {
            return number_string_with_one_decimal(quantity, size_of_current_unit, suffix, use_thousands_separator);
        }

        size_of_current_unit *= size_of_unit;
    }

    return number_string_with_one_decimal(quantity,
        size_of_current_unit, full_unit_suffix(unit_prefixes.size() - 1), use_thousands_separator);
}

DeprecatedString human_readable_size(u64 size, HumanReadableBasedOn based_on, UseThousandsSeparator use_thousands_separator)
{
    return human_readable_quantity(size, based_on, "B"sv, use_thousands_separator);
}

DeprecatedString human_readable_size_long(u64 size, UseThousandsSeparator use_thousands_separator)
{
    if (size < 1 * KiB) {
        if (use_thousands_separator == UseThousandsSeparator::Yes)
            return DeprecatedString::formatted("{:'d} bytes", size);

        return DeprecatedString::formatted("{} bytes", size);
    }

    auto human_readable_size_string = human_readable_size(size, HumanReadableBasedOn::Base2, use_thousands_separator);
    if (use_thousands_separator == UseThousandsSeparator::Yes)
        return DeprecatedString::formatted("{} ({:'d} bytes)", human_readable_size_string, size);

    return DeprecatedString::formatted("{} ({} bytes)", human_readable_size_string, size);
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
