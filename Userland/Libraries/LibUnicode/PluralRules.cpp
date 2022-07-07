/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibUnicode/PluralRules.h>

#if ENABLE_UNICODE_DATA
#    include <LibUnicode/UnicodePluralRules.h>
#endif

namespace Unicode {

#if !ENABLE_UNICODE_DATA
enum class PluralCategory : u8 {
    Other,
};
#endif

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

Optional<PluralCategory> __attribute__((weak)) plural_category_from_string(StringView category)
{
    VERIFY(category == "other"sv);
    return PluralCategory::Other;
}

StringView __attribute__((weak)) plural_category_to_string(PluralCategory category)
{
    VERIFY(category == PluralCategory::Other);
    return "other"sv;
}

PluralCategory __attribute__((weak)) determine_plural_category(StringView, PluralForm, PluralOperands)
{
    return PluralCategory::Other;
}

Span<PluralCategory const> __attribute__((weak)) available_plural_categories(StringView, PluralForm)
{
    static constexpr Array<PluralCategory, 1> categories { { PluralCategory::Other } };
    return categories.span();
}

}
