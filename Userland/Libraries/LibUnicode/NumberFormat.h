/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibUnicode/Forward.h>

namespace Unicode {

struct NumberGroupings {
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
    CurrencyShort,
};

struct NumberFormat {
    enum class Plurality : u8 {
        Other,
        Zero,
        Single,
        One,
        Two,
        Few,
        Many,
    };

    u8 magnitude { 0 };
    u8 exponent { 0 };
    Plurality plurality { Plurality::Other };
    StringView zero_format {};
    StringView positive_format {};
    StringView negative_format {};
    Vector<StringView> identifiers {};
};

enum class NumericSymbol : u8 {
    Decimal,
    Exponential,
    Group,
    Infinity,
    MinusSign,
    NaN,
    PercentSign,
    PlusSign,
};

Optional<StringView> get_number_system_symbol(StringView locale, StringView system, NumericSymbol symbol);
Optional<NumberGroupings> get_number_system_groupings(StringView locale, StringView system);
Optional<NumberFormat> get_standard_number_system_format(StringView locale, StringView system, StandardNumberFormatType type);
Vector<NumberFormat> get_compact_number_system_formats(StringView locale, StringView system, CompactNumberFormatType type);
Vector<NumberFormat> get_unit_formats(StringView locale, StringView unit, Style style);
Optional<NumberFormat> select_pattern_with_plurality(Vector<NumberFormat> const& formats, double number);
Optional<String> augment_currency_format_pattern(StringView currency_display, StringView base_pattern);

}
