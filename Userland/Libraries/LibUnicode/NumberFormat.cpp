/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf8View.h>
#include <LibUnicode/CharacterTypes.h>
#include <LibUnicode/Locale.h>
#include <LibUnicode/NumberFormat.h>

#if ENABLE_UNICODE_DATA
#    include <LibUnicode/UnicodeData.h>
#    include <LibUnicode/UnicodeNumberFormat.h>
#endif

namespace Unicode {

Optional<StringView> get_number_system_symbol([[maybe_unused]] StringView locale, [[maybe_unused]] StringView system, [[maybe_unused]] NumericSymbol symbol)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_number_system_symbol(locale, system, symbol);
#else
    return {};
#endif
}

Optional<NumberGroupings> get_number_system_groupings([[maybe_unused]] StringView locale, [[maybe_unused]] StringView system)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_number_system_groupings(locale, system);
#else
    return {};
#endif
}

Optional<NumberFormat> get_standard_number_system_format([[maybe_unused]] StringView locale, [[maybe_unused]] StringView system, [[maybe_unused]] StandardNumberFormatType type)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_standard_number_system_format(locale, system, type);
#else
    return {};
#endif
}

Vector<NumberFormat> get_compact_number_system_formats([[maybe_unused]] StringView locale, [[maybe_unused]] StringView system, [[maybe_unused]] CompactNumberFormatType type)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_compact_number_system_formats(locale, system, type);
#else
    return {};
#endif
}

Vector<NumberFormat> get_unit_formats([[maybe_unused]] StringView locale, [[maybe_unused]] StringView unit, [[maybe_unused]] Style style)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_unit_formats(locale, unit, style);
#else
    return {};
#endif
}

Optional<NumberFormat> select_pattern_with_plurality(Vector<NumberFormat> const& formats, double number)
{
    // FIXME: This is a rather naive and locale-unaware implementation Unicode's TR-35 pluralization
    //        rules: https://www.unicode.org/reports/tr35/tr35-numbers.html#Language_Plural_Rules
    //        Once those rules are implemented for LibJS, we better use them instead.
    auto find_plurality = [&](auto plurality) -> Optional<NumberFormat> {
        if (auto it = formats.find_if([&](auto& patterns) { return patterns.plurality == plurality; }); it != formats.end())
            return *it;
        return {};
    };

    if (number == 0) {
        if (auto patterns = find_plurality(NumberFormat::Plurality::Zero); patterns.has_value())
            return patterns;
    } else if (number == 1) {
        if (auto patterns = find_plurality(NumberFormat::Plurality::One); patterns.has_value())
            return patterns;
    } else if (number == 2) {
        if (auto patterns = find_plurality(NumberFormat::Plurality::Two); patterns.has_value())
            return patterns;
    } else if (number > 2) {
        if (auto patterns = find_plurality(NumberFormat::Plurality::Many); patterns.has_value())
            return patterns;
    }

    return find_plurality(NumberFormat::Plurality::Other);
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
