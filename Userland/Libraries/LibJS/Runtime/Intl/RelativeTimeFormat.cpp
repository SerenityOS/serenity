/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/NumberFormat.h>
#include <LibJS/Runtime/Intl/NumberFormatConstructor.h>
#include <LibJS/Runtime/Intl/RelativeTimeFormat.h>

namespace JS::Intl {

// 17 RelativeTimeFormat Objects, https://tc39.es/ecma402/#relativetimeformat-objects
RelativeTimeFormat::RelativeTimeFormat(Object& prototype)
    : Object(prototype)
{
}

void RelativeTimeFormat::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    if (m_number_format)
        visitor.visit(m_number_format);
}

void RelativeTimeFormat::set_numeric(StringView numeric)
{
    if (numeric == "always"sv) {
        m_numeric = Numeric::Always;
    } else if (numeric == "auto"sv) {
        m_numeric = Numeric::Auto;
    } else {
        VERIFY_NOT_REACHED();
    }
}

StringView RelativeTimeFormat::numeric_string() const
{
    switch (m_numeric) {
    case Numeric::Always:
        return "always"sv;
    case Numeric::Auto:
        return "auto"sv;
    default:
        VERIFY_NOT_REACHED();
    }
}

ThrowCompletionOr<RelativeTimeFormat*> initialize_relative_time_format(GlobalObject& global_object, RelativeTimeFormat& relative_time_format, Value locales_value, Value options_value)
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

    // 5. Set opt.[[LocaleMatcher]] to matcher.
    opt.locale_matcher = matcher;

    // 6. Let numberingSystem be ? GetOption(options, "numberingSystem", "string", undefined, undefined).
    auto numbering_system = TRY(get_option(global_object, *options, vm.names.numberingSystem, Value::Type::String, {}, Empty {}));

    // 7. If numberingSystem is not undefined, then
    if (!numbering_system.is_undefined()) {
        // a. If numberingSystem does not match the Unicode Locale Identifier type nonterminal, throw a RangeError exception.
        if (!Unicode::is_type_identifier(numbering_system.as_string().string()))
            return vm.throw_completion<RangeError>(global_object, ErrorType::OptionIsNotValidValue, numbering_system, "numberingSystem"sv);

        // 8. Set opt.[[nu]] to numberingSystem.
        opt.nu = numbering_system.as_string().string();
    }

    // 9. Let localeData be %RelativeTimeFormat%.[[LocaleData]].
    // 10. Let r be ResolveLocale(%RelativeTimeFormat%.[[AvailableLocales]], requestedLocales, opt, %RelativeTimeFormat%.[[RelevantExtensionKeys]], localeData).
    auto result = resolve_locale(requested_locales, opt, RelativeTimeFormat::relevant_extension_keys());

    // 11. Let locale be r.[[locale]].
    auto locale = move(result.locale);

    // 12. Set relativeTimeFormat.[[Locale]] to locale.
    relative_time_format.set_locale(locale);

    // 13. Set relativeTimeFormat.[[DataLocale]] to r.[[dataLocale]].
    relative_time_format.set_data_locale(move(result.data_locale));

    // 14. Set relativeTimeFormat.[[NumberingSystem]] to r.[[nu]].
    if (result.nu.has_value())
        relative_time_format.set_numbering_system(result.nu.release_value());

    // 15. Let style be ? GetOption(options, "style", "string", « "long", "short", "narrow" », "long").
    auto style = TRY(get_option(global_object, *options, vm.names.style, Value::Type::String, { "long"sv, "short"sv, "narrow"sv }, "long"sv));

    // 16. Set relativeTimeFormat.[[Style]] to style.
    relative_time_format.set_style(style.as_string().string());

    // 17. Let numeric be ? GetOption(options, "numeric", "string", « "always", "auto" », "always").
    auto numeric = TRY(get_option(global_object, *options, vm.names.numeric, Value::Type::String, { "always"sv, "auto"sv }, "always"sv));

    // 18. Set relativeTimeFormat.[[Numeric]] to numeric.
    relative_time_format.set_numeric(numeric.as_string().string());

    MarkedValueList arguments { vm.heap() };
    arguments.append(js_string(vm, locale));

    // 19. Let relativeTimeFormat.[[NumberFormat]] be ! Construct(%NumberFormat%, « locale »).
    auto* number_format = MUST(construct(global_object, *global_object.intl_number_format_constructor(), move(arguments)));
    relative_time_format.set_number_format(static_cast<NumberFormat*>(number_format));

    // 20. Let relativeTimeFormat.[[PluralRules]] be ! Construct(%PluralRules%, « locale »).
    // FIXME: We do not yet support Intl.PluralRules.

    // 21. Return relativeTimeFormat.
    return &relative_time_format;
}

}
