/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/Set.h>
#include <LibJS/Runtime/SetConstructor.h>

namespace JS {

SetConstructor::SetConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Set, *global_object.function_prototype())
{
}

void SetConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 24.2.2.1 Set.prototype, https://tc39.es/ecma262/#sec-set.prototype
    define_property(vm.names.prototype, global_object.set_prototype(), 0);

    define_property(vm.names.length, Value(0), Attribute::Configurable);

    define_native_accessor(vm.well_known_symbol_species(), symbol_species_getter, {}, Attribute::Configurable);
}

SetConstructor::~SetConstructor()
{
}

// 24.2.1.1 Set ( [ iterable ] ), https://tc39.es/ecma262/#sec-set-iterable
Value SetConstructor::call()
{
    auto& vm = this->vm();
    vm.throw_exception<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.Set);
    return {};
}

// 24.2.1.1 Set ( [ iterable ] ), https://tc39.es/ecma262/#sec-set-iterable
Value SetConstructor::construct(Function& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto* set = ordinary_create_from_constructor<Set>(global_object, new_target, &GlobalObject::set_prototype);
    if (vm.exception())
        return {};

    if (vm.argument(0).is_nullish())
        return set;

    auto adder = set->get(vm.names.add);
    if (vm.exception())
        return {};
    if (!adder.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, "'add' property of Set");
        return {};
    }
    get_iterator_values(global_object, vm.argument(0), [&](Value iterator_value) {
        if (vm.exception())
            return IterationDecision::Break;
        (void)vm.call(adder.as_function(), Value(set), iterator_value);
        return vm.exception() ? IterationDecision::Break : IterationDecision::Continue;
    });
    if (vm.exception())
        return {};
    return set;
}

// 24.2.2.2 get Set [ @@species ], https://tc39.es/ecma262/#sec-get-set-@@species
JS_DEFINE_NATIVE_GETTER(SetConstructor::symbol_species_getter)
{
    return vm.this_value(global_object);
}

}
