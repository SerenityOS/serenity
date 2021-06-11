/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AggregateError.h>
#include <LibJS/Runtime/AggregateErrorConstructor.h>
#include <LibJS/Runtime/ErrorConstructor.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>

namespace JS {

AggregateErrorConstructor::AggregateErrorConstructor(GlobalObject& global_object)
    : NativeFunction(*static_cast<Object*>(global_object.error_constructor()))
{
}

void AggregateErrorConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);
    define_property(vm.names.prototype, global_object.aggregate_error_prototype(), 0);
    define_property(vm.names.length, Value(2), Attribute::Configurable);
}

Value AggregateErrorConstructor::call()
{
    return construct(*this);
}

// 20.5.7.1 The AggregateError Constructor, https://tc39.es/ecma262/#sec-aggregate-error-constructor
Value AggregateErrorConstructor::construct(Function&)
{
    auto& vm = this->vm();
    String message;
    if (!vm.argument(1).is_undefined()) {
        message = vm.argument(1).to_string(global_object());
        if (vm.exception())
            return {};
    }
    auto errors_list = iterable_to_list(global_object(), vm.argument(0));
    if (vm.exception())
        return {};
    // FIXME: 2. Let O be ? OrdinaryCreateFromConstructor(newTarget, "%AggregateError.prototype%", « [[ErrorData]] »).
    return AggregateError::create(global_object(), message, errors_list);
}

}
