/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/PluralRules.h>
#include <LibJS/Runtime/Intl/PluralRulesConstructor.h>

namespace JS::Intl {

// 16.2 The Intl.PluralRules Constructor, https://tc39.es/ecma402/#sec-intl-pluralrules-constructor
PluralRulesConstructor::PluralRulesConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.PluralRules.as_string(), *global_object.function_prototype())
{
}

void PluralRulesConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 16.3.1 Intl.PluralRules.prototype, https://tc39.es/ecma402/#sec-intl.pluralrules.prototype
    define_direct_property(vm.names.prototype, global_object.intl_plural_rules_prototype(), 0);
    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.supportedLocalesOf, supported_locales_of, 1, attr);
}

// 16.2.1 Intl.PluralRules ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-intl.pluralrules
ThrowCompletionOr<Value> PluralRulesConstructor::call()
{
    // 1. If NewTarget is undefined, throw a TypeError exception.
    return vm().throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Intl.PluralRules");
}

// 16.2.1 Intl.PluralRules ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-intl.pluralrules
ThrowCompletionOr<Object*> PluralRulesConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 2. Let pluralRules be ? OrdinaryCreateFromConstructor(NewTarget, "%PluralRules.prototype%", « [[InitializedPluralRules]], [[Locale]], [[Type]], [[MinimumIntegerDigits]], [[MinimumFractionDigits]], [[MaximumFractionDigits]], [[MinimumSignificantDigits]], [[MaximumSignificantDigits]], [[RoundingType]] »).
    auto* plural_rules = TRY(ordinary_create_from_constructor<PluralRules>(global_object, new_target, &GlobalObject::intl_plural_rules_prototype));

    // 3. Return ? InitializePluralRules(pluralRules, locales, options).
    return TRY(initialize_plural_rules(global_object, *plural_rules, locales, options));
}

// 16.3.2 Intl.PluralRules.supportedLocalesOf ( locales [ , options ] ), https://tc39.es/ecma402/#sec-intl.pluralrules.supportedlocalesof
JS_DEFINE_NATIVE_FUNCTION(PluralRulesConstructor::supported_locales_of)
{
    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 1. Let availableLocales be %PluralRules%.[[AvailableLocales]].

    // 2. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(global_object, locales));

    // 3. Return ? SupportedLocales(availableLocales, requestedLocales, options).
    return TRY(supported_locales(global_object, requested_locales, options));
}

}
