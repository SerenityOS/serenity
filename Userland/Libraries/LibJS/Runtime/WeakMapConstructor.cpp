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
    : NativeFunction(vm().names.WeakMap.as_string(), *global_object.function_prototype())
{
}

void WeakMapConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 24.3.2.1 WeakMap.prototype, https://tc39.es/ecma262/#sec-weakmap.prototype
    define_direct_property(vm.names.prototype, global_object.weak_map_prototype(), 0);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
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
Value WeakMapConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto* weak_map = TRY_OR_DISCARD(ordinary_create_from_constructor<WeakMap>(global_object, new_target, &GlobalObject::weak_map_prototype));

    if (vm.argument(0).is_nullish())
        return weak_map;

    auto adder = TRY_OR_DISCARD(weak_map->get(vm.names.set));
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
        auto key_or_error = iterator_value.as_object().get(0);
        if (key_or_error.is_error())
            return IterationDecision::Break;
        auto key = key_or_error.release_value();
        auto value_or_error = iterator_value.as_object().get(1);
        if (value_or_error.is_error())
            return IterationDecision::Break;
        auto value = value_or_error.release_value();
        auto result = vm.call(adder.as_function(), Value(weak_map), key, value);
        return result.is_error() ? IterationDecision::Break : IterationDecision::Continue;
    });
    if (vm.exception())
        return {};
    return weak_map;
}

}
