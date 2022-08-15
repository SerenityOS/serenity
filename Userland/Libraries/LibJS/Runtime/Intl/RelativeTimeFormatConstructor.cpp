/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/NumberFormat.h>
#include <LibJS/Runtime/Intl/NumberFormatConstructor.h>
#include <LibJS/Runtime/Intl/PluralRules.h>
#include <LibJS/Runtime/Intl/PluralRulesConstructor.h>
#include <LibJS/Runtime/Intl/RelativeTimeFormat.h>
#include <LibJS/Runtime/Intl/RelativeTimeFormatConstructor.h>
#include <LibUnicode/Locale.h>

namespace JS::Intl {

// 17.1 The Intl.RelativeTimeFormat Constructor, https://tc39.es/ecma402/#sec-intl-relativetimeformat-constructor
RelativeTimeFormatConstructor::RelativeTimeFormatConstructor(Realm& realm)
    : NativeFunction(vm().names.RelativeTimeFormat.as_string(), *realm.global_object().function_prototype())
{
}

void RelativeTimeFormatConstructor::initialize(Realm& realm)
{
    NativeFunction::initialize(realm);

    auto& vm = this->vm();

    // 17.2.1 Intl.RelativeTimeFormat.prototype, https://tc39.es/ecma402/#sec-Intl.RelativeTimeFormat.prototype
    define_direct_property(vm.names.prototype, realm.global_object().intl_relative_time_format_prototype(), 0);
    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.supportedLocalesOf, supported_locales_of, 1, attr);
}

// 17.1.1 Intl.RelativeTimeFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-Intl.RelativeTimeFormat
ThrowCompletionOr<Value> RelativeTimeFormatConstructor::call()
{
    // 1. If NewTarget is undefined, throw a TypeError exception.
    return vm().throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Intl.RelativeTimeFormat");
}

// 17.1.1 Intl.RelativeTimeFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-Intl.RelativeTimeFormat
ThrowCompletionOr<Object*> RelativeTimeFormatConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 2. Let relativeTimeFormat be ? OrdinaryCreateFromConstructor(NewTarget, "%RelativeTimeFormat.prototype%", « [[InitializedRelativeTimeFormat]], [[Locale]], [[DataLocale]], [[Style]], [[Numeric]], [[NumberFormat]], [[NumberingSystem]], [[PluralRules]] »).
    auto* relative_time_format = TRY(ordinary_create_from_constructor<RelativeTimeFormat>(global_object, new_target, &GlobalObject::intl_relative_time_format_prototype));

    // 3. Return ? InitializeRelativeTimeFormat(relativeTimeFormat, locales, options).
    return TRY(initialize_relative_time_format(global_object, *relative_time_format, locales, options));
}

// 17.2.2 Intl.RelativeTimeFormat.supportedLocalesOf ( locales [ , options ] ), https://tc39.es/ecma402/#sec-Intl.RelativeTimeFormat.supportedLocalesOf
JS_DEFINE_NATIVE_FUNCTION(RelativeTimeFormatConstructor::supported_locales_of)
{
    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 1. Let availableLocales be %RelativeTimeFormat%.[[AvailableLocales]].

    // 2. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(global_object, locales));

    // 3. Return ? SupportedLocales(availableLocales, requestedLocales, options).
    return TRY(supported_locales(global_object, requested_locales, options));
}

// 17.1.2 InitializeRelativeTimeFormat ( relativeTimeFormat, locales, options ), https://tc39.es/ecma402/#sec-InitializeRelativeTimeFormat
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
    auto matcher = TRY(get_option(global_object, *options, vm.names.localeMatcher, OptionType::String, AK::Array { "lookup"sv, "best fit"sv }, "best fit"sv));

    // 5. Set opt.[[LocaleMatcher]] to matcher.
    opt.locale_matcher = matcher;

    // 6. Let numberingSystem be ? GetOption(options, "numberingSystem", "string", undefined, undefined).
    auto numbering_system = TRY(get_option(global_object, *options, vm.names.numberingSystem, OptionType::String, {}, Empty {}));

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
    auto style = TRY(get_option(global_object, *options, vm.names.style, OptionType::String, { "long"sv, "short"sv, "narrow"sv }, "long"sv));

    // 16. Set relativeTimeFormat.[[Style]] to style.
    relative_time_format.set_style(style.as_string().string());

    // 17. Let numeric be ? GetOption(options, "numeric", "string", « "always", "auto" », "always").
    auto numeric = TRY(get_option(global_object, *options, vm.names.numeric, OptionType::String, { "always"sv, "auto"sv }, "always"sv));

    // 18. Set relativeTimeFormat.[[Numeric]] to numeric.
    relative_time_format.set_numeric(numeric.as_string().string());

    // 19. Let relativeTimeFormat.[[NumberFormat]] be ! Construct(%NumberFormat%, « locale »).
    auto* number_format = MUST(construct(global_object, *global_object.intl_number_format_constructor(), js_string(vm, locale)));
    relative_time_format.set_number_format(static_cast<NumberFormat*>(number_format));

    // 20. Let relativeTimeFormat.[[PluralRules]] be ! Construct(%PluralRules%, « locale »).
    auto* plural_rules = MUST(construct(global_object, *global_object.intl_plural_rules_constructor(), js_string(vm, locale)));
    relative_time_format.set_plural_rules(static_cast<PluralRules*>(plural_rules));

    // 21. Return relativeTimeFormat.
    return &relative_time_format;
}

}
