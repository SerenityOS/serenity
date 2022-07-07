/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Assertions.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibUnicode/Forward.h>

namespace Unicode {

enum class PluralForm {
    Cardinal,
    Ordinal,
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

Optional<PluralCategory> plural_category_from_string(StringView category);
StringView plural_category_to_string(PluralCategory category);

PluralCategory determine_plural_category(StringView locale, PluralForm form, PluralOperands operands);

}
