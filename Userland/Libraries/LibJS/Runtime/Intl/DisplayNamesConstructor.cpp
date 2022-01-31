/*
 * Copyright (c) 2021-2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/DisplayNames.h>
#include <LibJS/Runtime/Intl/DisplayNamesConstructor.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibUnicode/Locale.h>

namespace JS::Intl {

// 12.2 The Intl.DisplayNames Constructor, https://tc39.es/ecma402/#sec-intl-displaynames-constructor
DisplayNamesConstructor::DisplayNamesConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.DisplayNames.as_string(), *global_object.function_prototype())
{
}

void DisplayNamesConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 12.3.1 Intl.DisplayNames.prototype, https://tc39.es/ecma402/#sec-Intl.DisplayNames.prototype
    define_direct_property(vm.names.prototype, global_object.intl_display_names_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.supportedLocalesOf, supported_locales_of, 1, attr);

    define_direct_property(vm.names.length, Value(2), Attribute::Configurable);
}

// 12.2.1 Intl.DisplayNames ( locales, options ), https://tc39.es/ecma402/#sec-Intl.DisplayNames
ThrowCompletionOr<Value> DisplayNamesConstructor::call()
{
    // 1. If NewTarget is undefined, throw a TypeError exception.
    return vm().throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Intl.DisplayNames");
}

// 12.2.1 Intl.DisplayNames ( locales, options ), https://tc39.es/ecma402/#sec-Intl.DisplayNames
ThrowCompletionOr<Object*> DisplayNamesConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto locale_value = vm.argument(0);
    auto options_value = vm.argument(1);

    // 2. Let displayNames be ? OrdinaryCreateFromConstructor(NewTarget, "%DisplayNames.prototype%", « [[InitializedDisplayNames]], [[Locale]], [[Style]], [[Type]], [[Fallback]], [[LanguageDisplay]], [[Fields]] »).
    auto* display_names = TRY(ordinary_create_from_constructor<DisplayNames>(global_object, new_target, &GlobalObject::intl_display_names_prototype));

    // 3. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(global_object, locale_value));

    // 4. If options is undefined, throw a TypeError exception.
    if (options_value.is_undefined())
        return vm.throw_completion<TypeError>(global_object, ErrorType::IsUndefined, "options"sv);

    // 5. Set options to ? GetOptionsObject(options).
    auto* options = TRY(Temporal::get_options_object(global_object, options_value));

    // 6. Let opt be a new Record.
    LocaleOptions opt {};

    // 7. Let localeData be %DisplayNames%.[[LocaleData]].

    // 8. Let matcher be ? GetOption(options, "localeMatcher", "string", « "lookup", "best fit" », "best fit").
    auto matcher = TRY(get_option(global_object, *options, vm.names.localeMatcher, Value::Type::String, { "lookup"sv, "best fit"sv }, "best fit"sv));

    // 9. Set opt.[[localeMatcher]] to matcher.
    opt.locale_matcher = matcher;

    // 10. Let r be ResolveLocale(%DisplayNames%.[[AvailableLocales]], requestedLocales, opt, %DisplayNames%.[[RelevantExtensionKeys]]).
    auto result = resolve_locale(requested_locales, opt, {});

    // 11. Let style be ? GetOption(options, "style", "string", « "narrow", "short", "long" », "long").
    auto style = TRY(get_option(global_object, *options, vm.names.style, Value::Type::String, { "narrow"sv, "short"sv, "long"sv }, "long"sv));

    // 12. Set displayNames.[[Style]] to style.
    display_names->set_style(style.as_string().string());

    // 13. Let type be ? GetOption(options, "type", "string", « "language", "region", "script", "currency", "calendar", "dateTimeField" », undefined).
    auto type = TRY(get_option(global_object, *options, vm.names.type, Value::Type::String, { "language"sv, "region"sv, "script"sv, "currency"sv, "calendar"sv, "dateTimeField"sv }, Empty {}));

    // 14. If type is undefined, throw a TypeError exception.
    if (type.is_undefined())
        return vm.throw_completion<TypeError>(global_object, ErrorType::IsUndefined, "options.type"sv);

    // 15. Set displayNames.[[Type]] to type.
    display_names->set_type(type.as_string().string());

    // 16. Let fallback be ? GetOption(options, "fallback", "string", « "code", "none" », "code").
    auto fallback = TRY(get_option(global_object, *options, vm.names.fallback, Value::Type::String, { "code"sv, "none"sv }, "code"sv));

    // 17. Set displayNames.[[Fallback]] to fallback.
    display_names->set_fallback(fallback.as_string().string());

    // 18. Set displayNames.[[Locale]] to r.[[locale]].
    display_names->set_locale(move(result.locale));

    // Note: Several of the steps below are skipped in favor of deferring to LibUnicode.

    // 19. Let dataLocale be r.[[dataLocale]].
    // 20. Let dataLocaleData be localeData.[[<dataLocale>]].
    // 21. Let types be dataLocaleData.[[types]].
    // 22. Assert: types is a Record (see 12.4.3).

    // 23. Let languageDisplay be ? GetOption(options, "languageDisplay", "string", « "dialect", "standard" », "dialect").
    auto language_display = TRY(get_option(global_object, *options, vm.names.languageDisplay, Value::Type::String, { "dialect"sv, "standard"sv }, "dialect"sv));

    // 24. Let typeFields be types.[[<type>]].
    // 25. Assert: typeFields is a Record (see 12.4.3).

    // 26. If type is "language", then
    if (display_names->type() == DisplayNames::Type::Language) {
        // a. Set displayNames.[[LanguageDisplay]] to languageDisplay.
        display_names->set_language_display(language_display.as_string().string());

        // b. Let typeFields be typeFields.[[<languageDisplay>]].
        // c. Assert: typeFields is a Record (see 12.4.3).
    }

    // 27. Let styleFields be typeFields.[[<style>]].
    // 28. Assert: styleFields is a Record (see 12.4.3).
    // 29. Set displayNames.[[Fields]] to styleFields.

    // 30. Return displayNames.
    return display_names;
}

// 12.3.2 Intl.DisplayNames.supportedLocalesOf ( locales [ , options ] ), https://tc39.es/ecma402/#sec-Intl.DisplayNames.supportedLocalesOf
JS_DEFINE_NATIVE_FUNCTION(DisplayNamesConstructor::supported_locales_of)
{
    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 1. Let availableLocales be %DisplayNames%.[[AvailableLocales]].
    // No-op, availability of each requested locale is checked via Unicode::is_locale_available()

    // 2. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(global_object, locales));

    // 3. Return ? SupportedLocales(availableLocales, requestedLocales, options).
    return TRY(supported_locales(global_object, requested_locales, options));
}

}
