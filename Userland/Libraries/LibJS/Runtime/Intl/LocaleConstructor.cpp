/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/Locale.h>
#include <LibJS/Runtime/Intl/LocaleConstructor.h>

namespace JS::Intl {

// 14.1 The Intl.Locale Constructor, https://tc39.es/ecma402/#sec-intl-locale-constructor
LocaleConstructor::LocaleConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Locale.as_string(), *global_object.function_prototype())
{
}

void LocaleConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 14.2.1 Intl.Locale.prototype, https://tc39.es/ecma402/#sec-Intl.Locale.prototype
    define_direct_property(vm.names.prototype, global_object.intl_locale_prototype(), 0);
    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
}

// 14.1.3 Intl.Locale ( tag [ , options ] ), https://tc39.es/ecma402/#sec-Intl.Locale
Value LocaleConstructor::call()
{
    // 1. If NewTarget is undefined, throw a TypeError exception.
    vm().throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Intl.Locale");
    return {};
}

// 14.1.3 Intl.Locale ( tag [ , options ] ), https://tc39.es/ecma402/#sec-Intl.Locale
Value LocaleConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 6. Let locale be ? OrdinaryCreateFromConstructor(NewTarget, "%Locale.prototype%", internalSlotsList).
    auto* locale = ordinary_create_from_constructor<Locale>(global_object, new_target, &GlobalObject::intl_locale_prototype);
    if (vm.exception())
        return {};

    return locale;
}

}
