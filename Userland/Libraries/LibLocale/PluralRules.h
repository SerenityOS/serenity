/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibLocale/Forward.h>

namespace Locale {

enum class PluralForm {
    Cardinal,
    Ordinal,
};

enum class PluralCategory : u8 {
    Other,
    Zero,
    One,
    Two,
    Few,
    Many,

    // https://unicode.org/reports/tr35/tr35-numbers.html#Explicit_0_1_rules
    ExactlyZero,
    ExactlyOne,
};

// https://unicode.org/reports/tr35/tr35-numbers.html#Plural_Operand_Meanings
struct PluralOperands {
    static constexpr StringView symbol_to_variable_name(char symbol)
    {
        if (symbol == 'n')
            return "number"sv;
        if (symbol == 'i')
            return "integer_digits"sv;
        if (symbol == 'f')
            return "fraction_digits"sv;
        if (symbol == 'v')
            return "number_of_fraction_digits"sv;
        if (symbol == 't')
            return "fraction_digits_without_trailing"sv;
        if (symbol == 'w')
            return "number_of_fraction_digits_without_trailing"sv;
        VERIFY_NOT_REACHED();
    }

    static constexpr bool symbol_requires_floating_point_modulus(char symbol)
    {
        // From TR-35: "The modulus (% or mod) is a remainder operation as defined in Java; for
        // example, where n = 4.3 the result of n mod 3 is 1.3."
        //
        // So, this returns whether the symbol represents a decimal value, and thus requires fmod.
        return symbol == 'n';
    }

    double number { 0 };
    u64 integer_digits { 0 };
    u64 fraction_digits { 0 };
    u64 number_of_fraction_digits { 0 };
    u64 fraction_digits_without_trailing { 0 };
    u64 number_of_fraction_digits_without_trailing { 0 };
};

PluralForm plural_form_from_string(StringView plural_form);
StringView plural_form_to_string(PluralForm plural_form);

// NOTE: This must be defined inline to be callable from the code generators.
constexpr PluralCategory plural_category_from_string(StringView category)
{
    if (category == "other"sv)
        return PluralCategory::Other;
    if (category == "zero"sv)
        return PluralCategory::Zero;
    if (category == "one"sv)
        return PluralCategory::One;
    if (category == "two"sv)
        return PluralCategory::Two;
    if (category == "few"sv)
        return PluralCategory::Few;
    if (category == "many"sv)
        return PluralCategory::Many;
    if (category == "0"sv)
        return PluralCategory::ExactlyZero;
    if (category == "1"sv)
        return PluralCategory::ExactlyOne;
    VERIFY_NOT_REACHED();
}

// NOTE: This must be defined inline to be callable from the code generators.
constexpr StringView plural_category_to_string(PluralCategory category)
{
    switch (category) {
    case PluralCategory::Other:
        return "other"sv;
    case PluralCategory::Zero:
        return "zero"sv;
    case PluralCategory::One:
        return "one"sv;
    case PluralCategory::Two:
        return "two"sv;
    case PluralCategory::Few:
        return "few"sv;
    case PluralCategory::Many:
        return "many"sv;
    case PluralCategory::ExactlyZero:
        return "0"sv;
    case PluralCategory::ExactlyOne:
        return "1"sv;
    }

    VERIFY_NOT_REACHED();
}

PluralCategory determine_plural_category(StringView locale, PluralForm form, PluralOperands operands);
ReadonlySpan<PluralCategory> available_plural_categories(StringView locale, PluralForm form);
PluralCategory determine_plural_range(StringView locale, PluralCategory start, PluralCategory end);

}
