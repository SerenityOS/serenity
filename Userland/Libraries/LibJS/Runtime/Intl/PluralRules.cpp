/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Variant.h>
#include <LibJS/Runtime/Intl/PluralRules.h>
#include <math.h>
#include <stdlib.h>

namespace JS::Intl {

// 16 PluralRules Objects, https://tc39.es/ecma402/#pluralrules-objects
PluralRules::PluralRules(Object& prototype)
    : NumberFormatBase(prototype)
{
}

// 16.5.1 GetOperands ( s ), https://tc39.es/ecma402/#sec-getoperands
Unicode::PluralOperands get_operands(String const& string)
{
    // 1.Let n be ! ToNumber(s).
    char* end { nullptr };
    auto number = strtod(string.characters(), &end);
    VERIFY(!*end);

    // 2. Assert: n is finite.
    VERIFY(isfinite(number));

    // 3. Let dp be StringIndexOf(s, ".", 0).
    auto decimal_point = string.find('.');

    Variant<Empty, double, StringView> integer_part;
    StringView fraction_slice;

    // 4. If dp = -1, then
    if (!decimal_point.has_value()) {
        // a. Let intPart be n.
        integer_part = number;

        // b. Let fracSlice be "".
    }
    // 5. Else,
    else {
        // a. Let intPart be the substring of s from 0 to dp.
        integer_part = string.substring_view(0, *decimal_point);

        // b. Let fracSlice be the substring of s from dp + 1.
        fraction_slice = string.substring_view(*decimal_point + 1);
    }

    // 6. Let i be abs(! ToNumber(intPart)).
    auto integer = integer_part.visit(
        [](Empty) -> u64 { VERIFY_NOT_REACHED(); },
        [](double value) {
            return static_cast<u64>(fabs(value));
        },
        [](StringView value) {
            auto value_as_int = value.template to_int<i64>().value();
            return static_cast<u64>(value_as_int);
        });

    // 7. Let fracDigitCount be the length of fracSlice.
    auto fraction_digit_count = fraction_slice.length();

    // 8. Let f be ! ToNumber(fracSlice).
    auto fraction = fraction_slice.is_empty() ? 0u : fraction_slice.template to_uint<u64>().value();

    // 9. Let significantFracSlice be the value of fracSlice stripped of trailing "0".
    auto significant_fraction_slice = fraction_slice.trim("0"sv, TrimMode::Right);

    // 10. Let significantFracDigitCount be the length of significantFracSlice.
    auto significant_fraction_digit_count = significant_fraction_slice.length();

    // 11. Let significantFrac be ! ToNumber(significantFracSlice).
    auto significant_fraction = significant_fraction_slice.is_empty() ? 0u : significant_fraction_slice.template to_uint<u64>().value();

    // 12. Return a new Record { [[Number]]: abs(n), [[IntegerDigits]]: i, [[FractionDigits]]: f, [[NumberOfFractionDigits]]: fracDigitCount, [[FractionDigitsWithoutTrailing]]: significantFrac, [[NumberOfFractionDigitsWithoutTrailing]]: significantFracDigitCount }.
    return Unicode::PluralOperands {
        .number = fabs(number),
        .integer_digits = integer,
        .fraction_digits = fraction,
        .number_of_fraction_digits = fraction_digit_count,
        .fraction_digits_without_trailing = significant_fraction,
        .number_of_fraction_digits_without_trailing = significant_fraction_digit_count,
    };
}

// 16.5.2 PluralRuleSelect ( locale, type, n, operands ), https://tc39.es/ecma402/#sec-pluralruleselect
Unicode::PluralCategory plural_rule_select(StringView locale, Unicode::PluralForm type, Value, Unicode::PluralOperands operands)
{
    return Unicode::determine_plural_category(locale, type, move(operands));
}

// 16.5.3 ResolvePlural ( pluralRules, n ), https://tc39.es/ecma402/#sec-resolveplural
Unicode::PluralCategory resolve_plural(GlobalObject& global_object, PluralRules const& plural_rules, Value number)
{
    // 1. Assert: Type(pluralRules) is Object.
    // 2. Assert: pluralRules has an [[InitializedPluralRules]] internal slot.
    // 3. Assert: Type(n) is Number.

    // 4. If n is not a finite Number, then
    if (!number.is_finite_number()) {
        // a. Return "other".
        return Unicode::PluralCategory::Other;
    }

    // 5. Let locale be pluralRules.[[Locale]].
    auto const& locale = plural_rules.locale();

    // 6. Let type be pluralRules.[[Type]].
    auto type = plural_rules.type();

    // 7. Let res be ! FormatNumericToString(pluralRules, n).
    auto result = format_numeric_to_string(global_object, plural_rules, number);

    // 8. Let s be res.[[FormattedString]].
    auto const& string = result.formatted_string;

    // 9. Let operands be ! GetOperands(s).
    auto operands = get_operands(string);

    // 10. Return ! PluralRuleSelect(locale, type, n, operands).
    return plural_rule_select(locale, type, number, move(operands));
}

}
