/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/DisplayNames.h>
#include <LibJS/Runtime/Intl/DisplayNamesConstructor.h>

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
    define_direct_property(vm.names.length, Value(2), Attribute::Configurable);
}

// 12.2.1 Intl.DisplayNames ( locales, options ), https://tc39.es/ecma402/#sec-Intl.DisplayNames
Value DisplayNamesConstructor::call()
{
    // 1. If NewTarget is undefined, throw a TypeError exception.
    vm().throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Intl.DisplayNames");
    return {};
}

// 12.2.1 Intl.DisplayNames ( locales, options ), https://tc39.es/ecma402/#sec-Intl.DisplayNames
Value DisplayNamesConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 2. Let displayNames be ? OrdinaryCreateFromConstructor(NewTarget, "%DisplayNames.prototype%", « [[InitializedDisplayNames]], [[Locale]], [[Style]], [[Type]], [[Fallback]], [[Fields]] »).
    auto* display_names = ordinary_create_from_constructor<DisplayNames>(global_object, new_target, &GlobalObject::intl_display_names_prototype);
    if (vm.exception())
        return {};

    // 28. Return displayNames.
    return display_names;
}

}
