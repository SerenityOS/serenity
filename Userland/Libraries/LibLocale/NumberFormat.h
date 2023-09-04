/*
 * Copyright (c) 2021-2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibLocale/Forward.h>
#include <LibLocale/PluralRules.h>

namespace Locale {

struct NumberGroupings {
    u8 minimum_grouping_digits { 0 };
    u8 primary_grouping_size { 0 };
    u8 secondary_grouping_size { 0 };
};

enum class StandardNumberFormatType : u8 {
    Decimal,
    Currency,
    Accounting,
    Percent,
    Scientific,
};

enum class CompactNumberFormatType : u8 {
    DecimalLong,
    DecimalShort,
    CurrencyUnit,
};

struct NumberFormat {
    u8 magnitude { 0 };
    u8 exponent { 0 };
    PluralCategory plurality { PluralCategory::Other };
    StringView zero_format {};
    StringView positive_format {};
    StringView negative_format {};
    Vector<StringView> identifiers {};
};

enum class NumericSymbol : u8 {
    ApproximatelySign,
    Decimal,
    Exponential,
    Group,
    Infinity,
    MinusSign,
    NaN,
    PercentSign,
    PlusSign,
    RangeSeparator,
    TimeSeparator,
};

Optional<StringView> get_number_system_symbol(StringView locale, StringView system, NumericSymbol symbol);
Optional<NumberGroupings> get_number_system_groupings(StringView locale, StringView system);

Optional<ReadonlySpan<u32>> get_digits_for_number_system(StringView system);
String replace_digits_for_number_system(StringView system, StringView number);

Optional<NumberFormat> get_standard_number_system_format(StringView locale, StringView system, StandardNumberFormatType type);
Vector<NumberFormat> get_compact_number_system_formats(StringView locale, StringView system, CompactNumberFormatType type);
Vector<NumberFormat> get_unit_formats(StringView locale, StringView unit, Style style);

Optional<String> augment_currency_format_pattern(StringView currency_display, StringView base_pattern);
Optional<String> augment_range_pattern(StringView range_separator, StringView lower, StringView upper);

}
