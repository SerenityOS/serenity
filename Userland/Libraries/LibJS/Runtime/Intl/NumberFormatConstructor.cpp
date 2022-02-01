/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/AbstractOperations.h>
#include <LibJS/Runtime/Intl/NumberFormat.h>
#include <LibJS/Runtime/Intl/NumberFormatConstructor.h>

namespace JS::Intl {

// 15.2 The Intl.NumberFormat Constructor, https://tc39.es/ecma402/#sec-intl-numberformat-constructor
NumberFormatConstructor::NumberFormatConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.NumberFormat.as_string(), *global_object.function_prototype())
{
}

void NumberFormatConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 15.3.1 Intl.NumberFormat.prototype, https://tc39.es/ecma402/#sec-intl.numberformat.prototype
    define_direct_property(vm.names.prototype, global_object.intl_number_format_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.supportedLocalesOf, supported_locales_of, 1, attr);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

// 15.2.1 Intl.NumberFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-intl.numberformat
ThrowCompletionOr<Value> NumberFormatConstructor::call()
{
    // 1. If NewTarget is undefined, let newTarget be the active function object, else let newTarget be NewTarget.
    return TRY(construct(*this));
}

// 15.2.1 Intl.NumberFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-intl.numberformat
ThrowCompletionOr<Object*> NumberFormatConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 2. Let numberFormat be ? OrdinaryCreateFromConstructor(newTarget, "%NumberFormat.prototype%", « [[InitializedNumberFormat]], [[Locale]], [[DataLocale]], [[NumberingSystem]], [[Style]], [[Unit]], [[UnitDisplay]], [[Currency]], [[CurrencyDisplay]], [[CurrencySign]], [[MinimumIntegerDigits]], [[MinimumFractionDigits]], [[MaximumFractionDigits]], [[MinimumSignificantDigits]], [[MaximumSignificantDigits]], [[RoundingType]], [[Notation]], [[CompactDisplay]], [[UseGrouping]], [[SignDisplay]], [[BoundFormat]] »).
    auto* number_format = TRY(ordinary_create_from_constructor<NumberFormat>(global_object, new_target, &GlobalObject::intl_number_format_prototype));

    // 3. Perform ? InitializeNumberFormat(numberFormat, locales, options).
    TRY(initialize_number_format(global_object, *number_format, locales, options));

    // 4. If the implementation supports the normative optional constructor mode of 4.3 Note 1, then
    //     a. Let this be the this value.
    //     b. Return ? ChainNumberFormat(numberFormat, NewTarget, this).

    // 5. Return numberFormat.
    return number_format;
}

// 15.3.2 Intl.NumberFormat.supportedLocalesOf ( locales [ , options ] ), https://tc39.es/ecma402/#sec-intl.numberformat.supportedlocalesof
JS_DEFINE_NATIVE_FUNCTION(NumberFormatConstructor::supported_locales_of)
{
    auto locales = vm.argument(0);
    auto options = vm.argument(1);

    // 1. Let availableLocales be %NumberFormat%.[[AvailableLocales]].

    // 2. Let requestedLocales be ? CanonicalizeLocaleList(locales).
    auto requested_locales = TRY(canonicalize_locale_list(global_object, locales));

    // 3. Return ? SupportedLocales(availableLocales, requestedLocales, options).
    return TRY(supported_locales(global_object, requested_locales, options));
}

}
