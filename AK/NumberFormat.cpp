/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/DeprecatedString.h>
#include <AK/Math.h>
#include <AK/NumberFormat.h>
#include <AK/NumericLimits.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

namespace AK {

// FIXME: Remove this hackery once printf() supports floats.
static DeprecatedString number_string_with_one_decimal(u64 number, u64 unit, StringView suffix)
{
    constexpr auto max_unit_size = NumericLimits<u64>::max() / 10;
    VERIFY(unit < max_unit_size);

    auto integer_part = number / unit;
    auto decimal_part = (number % unit) * 10 / unit;
    return DeprecatedString::formatted("{}.{} {}", integer_part, decimal_part, suffix);
}

DeprecatedString human_readable_quantity(u64 quantity, HumanReadableBasedOn based_on, StringView unit)
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
            return number_string_with_one_decimal(quantity, size_of_current_unit, suffix);
        }

        size_of_current_unit *= size_of_unit;
    }

    return number_string_with_one_decimal(quantity,
        size_of_current_unit, full_unit_suffix(unit_prefixes.size() - 1));
}

DeprecatedString human_readable_size(u64 size, HumanReadableBasedOn based_on)
{
    return human_readable_quantity(size, based_on, "B"sv);
}

DeprecatedString human_readable_size_long(u64 size)
{
    if (size < 1 * KiB)
        return DeprecatedString::formatted("{} bytes", human_readable_integer(size));
    else
        return DeprecatedString::formatted("{} ({} bytes)", human_readable_size(size), human_readable_integer(size));
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

DeprecatedString human_readable_integer(i64 const number)
{
    u64 const abs_number = abs(number);
    // Handle the edge case of number being 0 as log10 is undefined for it.
    u8 const digit_count = ceil(abs_number > 0 ? log10(static_cast<long double>(abs_number)) : 0) + 1;
    u8 const section_count = (digit_count - 1) / 3 + 1;

    // 7 sections is the maximum for i64.
    Vector<u16, 7> separated_sections;
    separated_sections.ensure_capacity(section_count);
    for (u64 remainder = abs_number; remainder > 0 || separated_sections.is_empty(); remainder /= 1000) {
        separated_sections.append(remainder % 1000);
    }

    // Pre-allocate capacity in the result string for the digits themselves, the commas, and the minus sign.
    u8 const string_capacity = digit_count + (section_count - 1) + (number < 0 ? 1 : 0);
    auto result_builder = StringBuilder(string_capacity);
    if (number < 0)
        result_builder.append('-');
    result_builder.appendff("{}", *separated_sections.rbegin());
    for (auto it = separated_sections.rbegin() + 1; it != separated_sections.rend(); ++it) {
        result_builder.appendff(",{:03}", *it);
    }
    return result_builder.to_deprecated_string();
}

}
