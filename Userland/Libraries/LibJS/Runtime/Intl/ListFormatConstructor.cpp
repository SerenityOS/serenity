/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Intl/ListFormat.h>
#include <LibJS/Runtime/Intl/ListFormatConstructor.h>

namespace JS::Intl {

// 13.2 The Intl.ListFormat Constructor, https://tc39.es/ecma402/#sec-intl-listformat-constructor
ListFormatConstructor::ListFormatConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.ListFormat.as_string(), *global_object.function_prototype())
{
}

void ListFormatConstructor::initialize(GlobalObject& global_object)
{
    NativeFunction::initialize(global_object);

    auto& vm = this->vm();

    // 13.3.1 Intl.ListFormat.prototype, https://tc39.es/ecma402/#sec-Intl.ListFormat.prototype
    define_direct_property(vm.names.prototype, global_object.intl_list_format_prototype(), 0);
    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

// 13.2.1 Intl.ListFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-Intl.ListFormat
Value ListFormatConstructor::call()
{
    // 1. If NewTarget is undefined, throw a TypeError exception.
    vm().throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, "Intl.ListFormat");
    return {};
}

// 13.2.1 Intl.ListFormat ( [ locales [ , options ] ] ), https://tc39.es/ecma402/#sec-Intl.ListFormat
Value ListFormatConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    // 2. Let listFormat be ? OrdinaryCreateFromConstructor(NewTarget, "%ListFormat.prototype%", « [[InitializedListFormat]], [[Locale]], [[Type]], [[Style]], [[Templates]] »).
    auto* list_format = ordinary_create_from_constructor<ListFormat>(global_object, new_target, &GlobalObject::intl_list_format_prototype);
    if (vm.exception())
        return {};

    return list_format;
}

}
