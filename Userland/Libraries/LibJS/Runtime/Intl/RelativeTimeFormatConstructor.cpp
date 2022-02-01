/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/RelativeTimeFormat.h>
#include <LibJS/Runtime/Intl/RelativeTimeFormatConstructor.h>

namespace JS::Intl {

// 17.2 The Intl.RelativeTimeFormat Constructor, https://tc39.es/ecma402/#sec-intl-relativetimeformat-constructor
RelativeTimeFormatConstructor::RelativeTimeFormatConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.RelativeTimeFormat.as_string(), *global_object.function_prototype())
{
}

void RelativeTimeFormatConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 17.3.1 Intl.RelativeTimeFormat.prototype, https://tc39.es/ecma402/#sec-Intl.RelativeTimeFormat.prototype
    define_direct_property(vm.names.prototype, global_object.intl_relative_time_format_prototype(), 0);
    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.supportedLocalesOf, supported_locales_of, 1, attr);
}

// 17.2.1 Intl.RelativeTimeFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-Intl.RelativeTimeFormat
ThrowCompletionOr<Value> RelativeTimeFormatConstructor::call()
{
    // 1. If NewTarget is undefined, throw a TypeError exception.
    return vm().throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Intl.RelativeTimeFormat");
}

// 17.2.1 Intl.RelativeTimeFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-Intl.RelativeTimeFormat
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

// 17.3.2 Intl.RelativeTimeFormat.supportedLocalesOf ( locales [ , options ] ), https://tc39.es/ecma402/#sec-Intl.RelativeTimeFormat.supportedLocalesOf
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

}
