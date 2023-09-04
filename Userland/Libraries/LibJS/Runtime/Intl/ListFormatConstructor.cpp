/*
 * Copyright (c) 2021-2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/ListFormat.h>
#include <LibJS/Runtime/Intl/ListFormatConstructor.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>

namespace JS::Intl {

// 13.1 The Intl.ListFormat Constructor, https://tc39.es/ecma402/#sec-intl-listformat-constructor
ListFormatConstructor::ListFormatConstructor(Realm& realm)
    : NativeFunction(realm.vm().names.ListFormat.as_string(), realm.intrinsics().function_prototype())
{
}

void ListFormatConstructor::initialize(Realm& realm)
{
    Base::initialize(realm);

    auto& vm = this->vm();

    // 13.2.1 Intl.ListFormat.prototype, https://tc39.es/ecma402/#sec-Intl.ListFormat.prototype
    define_direct_property(vm.names.prototype, realm.intrinsics().intl_list_format_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(realm, vm.names.supportedLocalesOf, supported_locales_of, 1, attr);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

// 13.1.1 Intl.ListFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-Intl.ListFormat
ThrowCompletionOr<Value> ListFormatConstructor::call()
{
    // 1. If NewTarget is undefined, throw a TypeError exception.
    return vm().throw_completion<TypeError>(ErrorType::ConstructorWithoutNew, "Intl.ListFormat");
}

// 13.1.1 Intl.ListFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-Intl.ListFormat
ThrowCompletionOr<NonnullGCPtr<Object>> ListFormatConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();

    auto locale_value = vm.argument(0);
    auto options_value = vm.argument(1);

    // 2. Let listFormat be ? OrdinaryCreateFromConstructor(NewTarget, "%ListFormat.prototype%", « [[InitializedListFormat]], [[Locale]], [[Type]], [[Style]], [[Templates]] »).
    auto list_format = TRY(ordinary_create_from_constructor<ListFormat>(vm, new_target, &Intrinsics::intl_list_format_prototype));

    // 3. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(vm, locale_value));

    // 4. Set options to ? GetOptionsObject(options).
    auto* options = TRY(Temporal::get_options_object(vm, options_value));

    // 5. Let opt be a new Record.
    LocaleOptions opt {};

    // 6. Let matcher be ? GetOption(options, "localeMatcher", string, « "lookup", "best fit" », "best fit").
    auto matcher = TRY(get_option(vm, *options, vm.names.localeMatcher, OptionType::String, { "lookup"sv, "best fit"sv }, "best fit"sv));

    // 7. Set opt.[[localeMatcher]] to matcher.
    opt.locale_matcher = matcher;

    // 8. Let localeData be %ListFormat%.[[LocaleData]].

    // 9. Let r be ResolveLocale(%ListFormat%.[[AvailableLocales]], requestedLocales, opt, %ListFormat%.[[RelevantExtensionKeys]], localeData).
    auto result = resolve_locale(requested_locales, opt, {});

    // 10. Set listFormat.[[Locale]] to r.[[locale]].
    list_format->set_locale(move(result.locale));

    // 11. Let type be ? GetOption(options, "type", string, « "conjunction", "disjunction", "unit" », "conjunction").
    auto type = TRY(get_option(vm, *options, vm.names.type, OptionType::String, { "conjunction"sv, "disjunction"sv, "unit"sv }, "conjunction"sv));

    // 12. Set listFormat.[[Type]] to type.
    list_format->set_type(type.as_string().utf8_string_view());

    // 13. Let style be ? GetOption(options, "style", string, « "long", "short", "narrow" », "long").
    auto style = TRY(get_option(vm, *options, vm.names.style, OptionType::String, { "long"sv, "short"sv, "narrow"sv }, "long"sv));

    // 14. Set listFormat.[[Style]] to style.
    list_format->set_style(style.as_string().utf8_string_view());

    // Note: The remaining steps are skipped in favor of deferring to LibUnicode.

    // 19. Return listFormat.
    return list_format;
}

// 13.2.2 Intl.ListFormat.supportedLocalesOf ( locales [ , options ] ), https://tc39.es/ecma402/#sec-Intl.ListFormat.supportedLocalesOf
JS_DEFINE_NATIVE_FUNCTION(ListFormatConstructor::supported_locales_of)
{
    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 1. Let availableLocales be %ListFormat%.[[AvailableLocales]].

    // 2. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(vm, locales));

    // 3. Return ? SupportedLocales(availableLocales, requestedLocales, options).
    return TRY(supported_locales(vm, requested_locales, options));
}

}
