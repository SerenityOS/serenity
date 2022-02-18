/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
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

Optional<StringView> get_default_number_system(StringView locale);

Optional<StringView> get_number_system_symbol(StringView locale, StringView system, NumericSymbol symbol);
Optional<NumberGroupings> get_number_system_groupings(StringView locale, StringView system);

Optional<Span<u32 const>> get_digits_for_number_system(StringView system);
String replace_digits_for_number_system(StringView system, StringView number);

Optional<NumberFormat> get_standard_number_system_format(StringView locale, StringView system, StandardNumberFormatType type);
Vector<NumberFormat> get_compact_number_system_formats(StringView locale, StringView system, CompactNumberFormatType type);
Vector<NumberFormat> get_unit_formats(StringView locale, StringView unit, Style style);

Optional<String> augment_currency_format_pattern(StringView currency_display, StringView base_pattern);

template<typename FormatType>
Optional<FormatType> select_pattern_with_plurality(Vector<FormatType> const& formats, double number)
{
    // FIXME: This is a rather naive and locale-unaware implementation Unicode's TR-35 pluralization
    //        rules: https://www.unicode.org/reports/tr35/tr35-numbers.html#Language_Plural_Rules
    //        Once those rules are implemented for LibJS, we better use them instead.
    auto find_plurality = [&](auto plurality) -> Optional<FormatType> {
        if (auto it = formats.find_if([&](auto& patterns) { return patterns.plurality == plurality; }); it != formats.end())
            return *it;
        return {};
    };

    if (number == 0) {
        if (auto patterns = find_plurality(FormatType::Plurality::Zero); patterns.has_value())
            return patterns;
    } else if (number == 1) {
        if (auto patterns = find_plurality(FormatType::Plurality::One); patterns.has_value())
            return patterns;
    } else if (number == 2) {
        if (auto patterns = find_plurality(FormatType::Plurality::Two); patterns.has_value())
            return patterns;
    } else if (number > 2) {
        if (auto patterns = find_plurality(FormatType::Plurality::Many); patterns.has_value())
            return patterns;
    }

    return find_plurality(FormatType::Plurality::Other);
}

}
