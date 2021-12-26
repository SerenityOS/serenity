/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/WeakMap.h>
#include <LibJS/Runtime/WeakMapConstructor.h>

namespace JS {

WeakMapConstructor::WeakMapConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.WeakMap, *global_object.function_prototype())
{
}

void WeakMapConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 24.3.2.1 WeakMap.prototype, https://tc39.es/ecma262/#sec-weakmap.prototype
    define_property(vm.names.prototype, global_object.weak_map_prototype(), 0);

    define_property(vm.names.length, Value(0), Attribute::Configurable);
}

WeakMapConstructor::~WeakMapConstructor()
{
}

// 24.3.1.1 WeakMap ( [ iterable ] ), https://tc39.es/ecma262/#sec-weakmap-iterable
Value WeakMapConstructor::call()
{
    auto& vm = this->vm();
    vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.WeakMap);
    return {};
}

// 24.3.1.1 WeakMap ( [ iterable ] ), https://tc39.es/ecma262/#sec-weakmap-iterable
Value WeakMapConstructor::construct(Function& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto* weak_map = ordinary_create_from_constructor<WeakMap>(global_object, new_target, &GlobalObject::weak_map_prototype);
    if (vm.exception())
        return {};

    if (vm.argument(0).is_nullish())
        return weak_map;

    auto adder = weak_map->get(vm.names.set);
    if (vm.exception())
        return {};
    if (!adder.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, "'set' property of WeakMap");
        return {};
    }
    get_iterator_values(global_object, vm.argument(0), [&](Value iterator_value) {
        if (vm.exception())
            return IterationDecision::Break;
        if (!iterator_value.is_object()) {
            vm.throw_exception<TypeError>(global_object, ErrorType::NotAnObject, String::formatted("Iterator value {}", iterator_value.to_string_without_side_effects()));
            return IterationDecision::Break;
        }
        auto key = iterator_value.as_object().get(0).value_or(js_undefined());
        if (vm.exception())
            return IterationDecision::Break;
        auto value = iterator_value.as_object().get(1).value_or(js_undefined());
        if (vm.exception())
            return IterationDecision::Break;
        (void)vm.call(adder.as_function(), Value(weak_map), key, value);
        return vm.exception() ? IterationDecision::Break : IterationDecision::Continue;
    });
    if (vm.exception())
        return {};
    return weak_map;
}

}
