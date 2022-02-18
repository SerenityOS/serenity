/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Utf8View.h>
#include <LibUnicode/CharacterTypes.h>
#include <LibUnicode/Locale.h>
#include <LibUnicode/NumberFormat.h>

#if ENABLE_UNICODE_DATA
#    include <LibUnicode/UnicodeData.h>
#endif

namespace Unicode {

Optional<StringView> __attribute__((weak)) get_number_system_symbol(StringView, StringView, NumericSymbol) { return {}; }
Optional<NumberGroupings> __attribute__((weak)) get_number_system_groupings(StringView, StringView) { return {}; }
Optional<NumberFormat> __attribute__((weak)) get_standard_number_system_format(StringView, StringView, StandardNumberFormatType) { return {}; }
Vector<NumberFormat> __attribute__((weak)) get_compact_number_system_formats(StringView, StringView, CompactNumberFormatType) { return {}; }
Vector<NumberFormat> __attribute__((weak)) get_unit_formats(StringView, StringView, Style) { return {}; }

Optional<StringView> get_default_number_system(StringView locale)
{
    if (auto systems = get_keywords_for_locale(locale, "nu"sv); !systems.is_empty())
        return systems[0];
    return {};
}

Optional<Span<u32 const>> __attribute__((weak)) get_digits_for_number_system(StringView)
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

    return builder.build();
}

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

    auto last_code_point = [](StringView string) {
        Utf8View utf8_string { string };
        u32 code_point = 0;

        for (auto it = utf8_string.begin(); it != utf8_string.end(); ++it)
            code_point = *it;

        return code_point;
    };

    if (*number_index < *currency_index) {
        u32 last_pattern_code_point = last_code_point(base_pattern.substring_view(0, *currency_index));

        if (!code_point_has_general_category(last_pattern_code_point, GeneralCategory::Separator)) {
            u32 first_currency_code_point = *utf8_currency_display.begin();

            if (!code_point_has_general_category(first_currency_code_point, GeneralCategory::Symbol))
                currency_key_with_spacing = String::formatted("{}{}", spacing, currency_key);
        }
    } else {
        u32 last_pattern_code_point = last_code_point(base_pattern.substring_view(0, *number_index));

        if (!code_point_has_general_category(last_pattern_code_point, GeneralCategory::Separator)) {
            u32 last_currency_code_point = last_code_point(currency_display);

            if (!code_point_has_general_category(last_currency_code_point, GeneralCategory::Symbol))
                currency_key_with_spacing = String::formatted("{}{}", currency_key, spacing);
        }
    }

    if (currency_key_with_spacing.has_value())
        return base_pattern.replace(currency_key, *currency_key_with_spacing);
#endif

    return {};
}

}
