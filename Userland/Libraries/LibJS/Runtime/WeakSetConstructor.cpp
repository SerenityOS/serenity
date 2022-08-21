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

WeakSetConstructor::WeakSetConstructor(Realm& realm)
    : NativeFunction(vm().names.WeakSet.as_string(), *realm.global_object().function_prototype())
{
}

void WeakSetConstructor::initialize(Realm& realm)
{
    auto& vm = this->vm();
    NativeFunction::initialize(realm);

    // 24.4.2.1 WeakSet.prototype, https://tc39.es/ecma262/#sec-weakset.prototype
    define_direct_property(vm.names.prototype, realm.global_object().weak_set_prototype(), 0);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

// 24.4.1.1 WeakSet ( [ iterable ] ), https://tc39.es/ecma262/#sec-weakset-iterable
ThrowCompletionOr<Value> WeakSetConstructor::call()
{
    auto& vm = this->vm();
    return vm.throw_completion<TypeError>(ErrorType::ConstructorWithoutNew, vm.names.WeakSet);
}

// 24.4.1.1 WeakSet ( [ iterable ] ), https://tc39.es/ecma262/#sec-weakset-iterable
ThrowCompletionOr<Object*> WeakSetConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto* weak_set = TRY(ordinary_create_from_constructor<WeakSet>(global_object, new_target, &GlobalObject::weak_set_prototype));

    if (vm.argument(0).is_nullish())
        return weak_set;

    auto adder = TRY(weak_set->get(vm.names.add));
    if (!adder.is_function())
        return vm.throw_completion<TypeError>(ErrorType::NotAFunction, "'add' property of WeakSet");

    (void)TRY(get_iterator_values(vm, vm.argument(0), [&](Value iterator_value) -> Optional<Completion> {
        TRY(JS::call(global_object, adder.as_function(), weak_set, iterator_value));
        return {};
    }));

    return weak_set;
}

}
