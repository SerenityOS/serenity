/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ProxyConstructor.h>
#include <LibJS/Runtime/ProxyObject.h>

namespace JS {

ProxyConstructor::ProxyConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Proxy, *global_object.function_prototype())
{
}

void ProxyConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);
    define_property(vm.names.length, Value(2), Attribute::Configurable);
}

ProxyConstructor::~ProxyConstructor()
{
}

Value ProxyConstructor::call()
{
    auto& vm = this->vm();
    vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.Proxy);
    return {};
}

Value ProxyConstructor::construct(Function&)
{
    auto& vm = this->vm();
    if (vm.argument_count() < 2) {
        vm.throw_exception<TypeError>(global_object(), ErrorType::ProxyTwoArguments);
        return {};
    }

    auto target = vm.argument(0);
    auto handler = vm.argument(1);

    if (!target.is_object()) {
        vm.throw_exception<TypeError>(global_object(), ErrorType::ProxyConstructorBadType, "target", target.to_string_without_side_effects());
        return {};
    }
    if (!handler.is_object()) {
        vm.throw_exception<TypeError>(global_object(), ErrorType::ProxyConstructorBadType, "handler", handler.to_string_without_side_effects());
        return {};
    }
    return ProxyObject::create(global_object(), target.as_object(), handler.as_object());
}

}
