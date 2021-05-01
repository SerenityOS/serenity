/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/RegExpConstructor.h>
#include <LibJS/Runtime/RegExpObject.h>

namespace JS {

RegExpConstructor::RegExpConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.RegExp, *global_object.function_prototype())
{
}

void RegExpConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);
    define_property(vm.names.prototype, global_object.regexp_prototype(), 0);
    define_property(vm.names.length, Value(2), Attribute::Configurable);
}

RegExpConstructor::~RegExpConstructor()
{
}

Value RegExpConstructor::call()
{
    return construct(*this);
}

Value RegExpConstructor::construct(Function&)
{
    auto& vm = this->vm();
    String pattern = "";
    String flags = "";
    if (!vm.argument(0).is_undefined()) {
        pattern = vm.argument(0).to_string(global_object());
        if (vm.exception())
            return {};
    }
    if (!vm.argument(1).is_undefined()) {
        flags = vm.argument(1).to_string(global_object());
        if (vm.exception())
            return {};
    }
    return RegExpObject::create(global_object(), pattern, flags);
}

}
