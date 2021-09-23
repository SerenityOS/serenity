/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/WeakSet.h>
#include <LibJS/Runtime/WeakSetConstructor.h>

namespace JS {

WeakSetConstructor::WeakSetConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.WeakSet.as_string(), *global_object.function_prototype())
{
}

void WeakSetConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 24.4.2.1 WeakSet.prototype, https://tc39.es/ecma262/#sec-weakset.prototype
    define_direct_property(vm.names.prototype, global_object.weak_set_prototype(), 0);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

WeakSetConstructor::~WeakSetConstructor()
{
}

// 24.4.1.1 WeakSet ( [ iterable ] ), https://tc39.es/ecma262/#sec-weakset-iterable
Value WeakSetConstructor::call()
{
    auto& vm = this->vm();
    vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.WeakSet);
    return {};
}

// 24.4.1.1 WeakSet ( [ iterable ] ), https://tc39.es/ecma262/#sec-weakset-iterable
Value WeakSetConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto* weak_set = TRY_OR_DISCARD(ordinary_create_from_constructor<WeakSet>(global_object, new_target, &GlobalObject::weak_set_prototype));
    if (vm.exception())
        return {};

    if (vm.argument(0).is_nullish())
        return weak_set;

    auto adder = weak_set->get(vm.names.add);
    if (vm.exception())
        return {};
    if (!adder.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, "'add' property of WeakSet");
        return {};
    }
    get_iterator_values(global_object, vm.argument(0), [&](Value iterator_value) {
        if (vm.exception())
            return IterationDecision::Break;
        auto result = vm.call(adder.as_function(), Value(weak_set), iterator_value);
        return result.is_error() ? IterationDecision::Break : IterationDecision::Continue;
    });
    if (vm.exception())
        return {};
    return weak_set;
}

}
