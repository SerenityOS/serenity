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

WeakMapConstructor::WeakMapConstructor(Realm& realm)
    : NativeFunction(realm.vm().names.WeakMap.as_string(), *realm.intrinsics().function_prototype())
{
}

void WeakMapConstructor::initialize(Realm& realm)
{
    auto& vm = this->vm();
    NativeFunction::initialize(realm);

    // 24.3.2.1 WeakMap.prototype, https://tc39.es/ecma262/#sec-weakmap.prototype
    define_direct_property(vm.names.prototype, realm.intrinsics().weak_map_prototype(), 0);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

// 24.3.1.1 WeakMap ( [ iterable ] ), https://tc39.es/ecma262/#sec-weakmap-iterable
ThrowCompletionOr<Value> WeakMapConstructor::call()
{
    auto& vm = this->vm();
    return vm.throw_completion<TypeError>(ErrorType::ConstructorWithoutNew, vm.names.WeakMap);
}

// 24.3.1.1 WeakMap ( [ iterable ] ), https://tc39.es/ecma262/#sec-weakmap-iterable
ThrowCompletionOr<Object*> WeakMapConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();

    auto* weak_map = TRY(ordinary_create_from_constructor<WeakMap>(vm, new_target, &Intrinsics::weak_map_prototype));

    if (vm.argument(0).is_nullish())
        return weak_map;

    auto adder = TRY(weak_map->get(vm.names.set));
    if (!adder.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, "'set' property of WeakMap");

    (void)TRY(get_iterator_values(vm, vm.argument(0), [&](Value iterator_value) -> Optional<Completion> {
        if (!iterator_value.is_object())
            return vm.throw_completion<TypeError>(ErrorType::NotAnObject, String::formatted("Iterator value {}", iterator_value.to_string_without_side_effects()));

        auto key = TRY(iterator_value.as_object().get(0));
        auto value = TRY(iterator_value.as_object().get(1));
        TRY(JS::call(vm, adder.as_function(), weak_map, key, value));

        return {};
    }));

    return weak_map;
}

}
