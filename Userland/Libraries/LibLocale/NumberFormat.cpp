/*
 * Copyright (c) 2021-2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Utf8View.h>
#include <LibLocale/Locale.h>
#include <LibLocale/NumberFormat.h>
#include <LibUnicode/CharacterTypes.h>

#if ENABLE_UNICODE_DATA
#    include <LibUnicode/UnicodeData.h>
#endif

namespace Locale {

Optional<StringView> __attribute__((weak)) get_number_system_symbol(StringView, StringView, NumericSymbol) { return {}; }
Optional<NumberGroupings> __attribute__((weak)) get_number_system_groupings(StringView, StringView) { return {}; }
Optional<NumberFormat> __attribute__((weak)) get_standard_number_system_format(StringView, StringView, StandardNumberFormatType) { return {}; }
Vector<NumberFormat> __attribute__((weak)) get_compact_number_system_formats(StringView, StringView, CompactNumberFormatType) { return {}; }
Vector<NumberFormat> __attribute__((weak)) get_unit_formats(StringView, StringView, Style) { return {}; }

Optional<ReadonlySpan<u32>> __attribute__((weak)) get_digits_for_number_system(StringView)
{
    // Fall back to "latn" digits when Unicode data generation is disabled.
    constexpr Array<u32, 10> digits { { 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39 } };
    return digits.span();
}

String replace_digits_for_number_system(StringView system, StringView number)
{
    auto digits = get_digits_for_number_system(system);
    if (!digits.has_value())
        digits = get_digits_for_number_system("latn"sv);
    VERIFY(digits.has_value());

    StringBuilder builder;

    for (auto ch : number) {
        if (is_ascii_digit(ch)) {
            u32 digit = digits->at(parse_ascii_digit(ch));
            builder.append_code_point(digit);
        } else {
            builder.append(ch);
        }
    }

    return MUST(builder.to_string());
}

#if ENABLE_UNICODE_DATA
static u32 last_code_point(StringView string)
{
    Utf8View utf8_string { string };
    u32 code_point = 0;

    for (auto it = utf8_string.begin(); it != utf8_string.end(); ++it)
        code_point = *it;

    return code_point;
}
#endif

// https://www.unicode.org/reports/tr35/tr35-numbers.html#Currencies
Optional<String> augment_currency_format_pattern([[maybe_unused]] StringView currency_display, [[maybe_unused]] StringView base_pattern)
{
#if ENABLE_UNICODE_DATA
    constexpr auto number_key = "{number}"sv;
    constexpr auto currency_key = "{currency}"sv;
    constexpr auto spacing = "\u00A0"sv; // No-Break Space (NBSP)

    auto number_index = base_pattern.find(number_key);
    VERIFY(number_index.has_value());

    auto currency_index = base_pattern.find(currency_key);
    VERIFY(currency_index.has_value());

    Utf8View utf8_currency_display { currency_display };
    Optional<String> currency_key_with_spacing;

    if (*number_index < *currency_index) {
        u32 last_pattern_code_point = last_code_point(base_pattern.substring_view(0, *currency_index));

        if (!Unicode::code_point_has_general_category(last_pattern_code_point, Unicode::GeneralCategory::Separator)) {
            u32 first_currency_code_point = *utf8_currency_display.begin();

            if (!Unicode::code_point_has_general_category(first_currency_code_point, Unicode::GeneralCategory::Symbol))
                currency_key_with_spacing = MUST(String::formatted("{}{}", spacing, currency_key));
        }
    } else {
        u32 last_pattern_code_point = last_code_point(base_pattern.substring_view(0, *number_index));

        if (!Unicode::code_point_has_general_category(last_pattern_code_point, Unicode::GeneralCategory::Separator)) {
            u32 last_currency_code_point = last_code_point(currency_display);

            if (!Unicode::code_point_has_general_category(last_currency_code_point, Unicode::GeneralCategory::Symbol))
                currency_key_with_spacing = MUST(String::formatted("{}{}", currency_key, spacing));
        }
    }

    if (currency_key_with_spacing.has_value())
        return MUST(MUST(String::from_utf8(base_pattern)).replace(currency_key, *currency_key_with_spacing, ReplaceMode::FirstOnly));
#endif

    return {};
}

// https://unicode.org/reports/tr35/tr35-numbers.html#83-range-pattern-processing
Optional<String> augment_range_pattern([[maybe_unused]] StringView range_separator, [[maybe_unused]] StringView lower, [[maybe_unused]] StringView upper)
{
#if ENABLE_UNICODE_DATA
    auto range_pattern_with_spacing = [&]() {
        return MUST(String::formatted(" {} ", range_separator));
    };

    Utf8View utf8_range_separator { range_separator };
    Utf8View utf8_upper { upper };

    // NOTE: Our implementation does the prescribed checks backwards for simplicity.

    // To determine whether to add spacing, the currently recommended heuristic is:
    // 2. If the range pattern does not contain a character having the White_Space binary Unicode property after the {0} or before the {1} placeholders.
    for (auto it = utf8_range_separator.begin(); it != utf8_range_separator.end(); ++it) {
        if (Unicode::code_point_has_property(*it, Unicode::Property::White_Space))
            return {};
    }

    // 1. If the lower string ends with a character other than a digit, or if the upper string begins with a character other than a digit.
    if (auto it = utf8_upper.begin(); it != utf8_upper.end()) {
        if (!Unicode::code_point_has_general_category(*it, Unicode::GeneralCategory::Decimal_Number))
            return range_pattern_with_spacing();
    }

    if (!Unicode::code_point_has_general_category(last_code_point(lower), Unicode::GeneralCategory::Decimal_Number))
        return range_pattern_with_spacing();
#endif

    return {};
}

}
