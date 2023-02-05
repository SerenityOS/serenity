/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibLocale/PluralRules.h>

namespace Locale {

PluralForm plural_form_from_string(StringView plural_form)
{
    if (plural_form == "cardinal"sv)
        return PluralForm::Cardinal;
    if (plural_form == "ordinal"sv)
        return PluralForm::Ordinal;
    VERIFY_NOT_REACHED();
}

StringView plural_form_to_string(PluralForm plural_form)
{
    switch (plural_form) {
    case PluralForm::Cardinal:
        return "cardinal"sv;
    case PluralForm::Ordinal:
        return "ordinal"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

PluralCategory __attribute__((weak)) determine_plural_category(StringView, PluralForm, PluralOperands)
{
    return PluralCategory::Other;
}

ReadonlySpan<PluralCategory> __attribute__((weak)) available_plural_categories(StringView, PluralForm)
{
    static constexpr Array<PluralCategory, 1> categories { { PluralCategory::Other } };
    return categories.span();
}

PluralCategory __attribute__((weak)) determine_plural_range(StringView, PluralCategory, PluralCategory)
{
    return PluralCategory::Other;
}

}
