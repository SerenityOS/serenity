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
ThrowCompletionOr<Value> WeakMapConstructor::call()
{
    auto& vm = this->vm();
    return vm.throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.WeakMap);
}

// 24.3.1.1 WeakMap ( [ iterable ] ), https://tc39.es/ecma262/#sec-weakmap-iterable
ThrowCompletionOr<Object*> WeakMapConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto* weak_map = TRY(ordinary_create_from_constructor<WeakMap>(global_object, new_target, &GlobalObject::weak_map_prototype));

    if (vm.argument(0).is_nullish())
        return weak_map;

    auto adder = TRY(weak_map->get(vm.names.set));
    if (!adder.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, "'set' property of WeakMap");

    TRY(get_iterator_values(global_object, vm.argument(0), [&](Value iterator_value) -> Optional<Completion> {
        if (!iterator_value.is_object())
            return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, String::formatted("Iterator value {}", iterator_value.to_string_without_side_effects()));

        auto key = TRY(iterator_value.as_object().get(0));
        auto value = TRY(iterator_value.as_object().get(1));
        TRY(vm.call(adder.as_function(), Value(weak_map), key, value));

        return {};
    }));

    return weak_map;
}

}
