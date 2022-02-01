/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/PluralRules.h>

namespace JS::Intl {

// 16 PluralRules Objects, https://tc39.es/ecma402/#pluralrules-objects
PluralRules::PluralRules(Object& prototype)
    : NumberFormatBase(prototype)
{
}

void PluralRules::set_type(StringView type)
{
    if (type == "cardinal"sv) {
        m_type = Type::Cardinal;
    } else if (type == "ordinal"sv) {
        m_type = Type::Ordinal;
    } else {
        VERIFY_NOT_REACHED();
    }
}

StringView PluralRules::type_string() const
{
    switch (m_type) {
    case Type::Cardinal:
        return "cardinal"sv;
    case Type::Ordinal:
        return "ordinal"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

// 16.1.1 InitializePluralRules ( pluralRules, locales, options ), https://tc39.es/ecma402/#sec-initializepluralrules
ThrowCompletionOr<PluralRules*> initialize_plural_rules(GlobalObject& global_object, PluralRules& plural_rules, Value locales_value, Value options_value)
{
    auto& vm = global_object.vm();

    // 1. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(global_object, locales_value));

    // 2. Set options to ? CoerceOptionsToObject(options).
    auto* options = TRY(coerce_options_to_object(global_object, options_value));

    // 3. Let opt be a new Record.
    LocaleOptions opt {};

    // 4. Let matcher be ? GetOption(options, "localeMatcher", "string", « "lookup", "best fit" », "best fit").
    auto matcher = TRY(get_option(global_object, *options, vm.names.localeMatcher, Value::Type::String, AK::Array { "lookup"sv, "best fit"sv }, "best fit"sv));

    // 5. Set opt.[[localeMatcher]] to matcher.
    opt.locale_matcher = matcher;

    // 6. Let t be ? GetOption(options, "type", "string", « "cardinal", "ordinal" », "cardinal").
    auto type = TRY(get_option(global_object, *options, vm.names.type, Value::Type::String, AK::Array { "cardinal"sv, "ordinal"sv }, "cardinal"sv));

    // 7. Set pluralRules.[[Type]] to t.
    plural_rules.set_type(type.as_string().string());

    // 8. Perform ? SetNumberFormatDigitOptions(pluralRules, options, +0𝔽, 3𝔽, "standard").
    TRY(set_number_format_digit_options(global_object, plural_rules, *options, 0, 3, NumberFormat::Notation::Standard));

    // 9. Let localeData be %PluralRules%.[[LocaleData]].
    // 10. Let r be ResolveLocale(%PluralRules%.[[AvailableLocales]], requestedLocales, opt, %PluralRules%.[[RelevantExtensionKeys]], localeData).
    auto result = resolve_locale(requested_locales, opt, {});

    // 11. Set pluralRules.[[Locale]] to r.[[locale]].
    plural_rules.set_locale(move(result.locale));

    // Non-standard, the data locale is used by our NumberFormat implementation.
    plural_rules.set_data_locale(move(result.data_locale));

    // 12. Return pluralRules.
    return &plural_rules;
}

}
