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
    : NativeFunction(vm().names.Set.as_string(), *global_object.function_prototype())
{
}

void SetConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 24.2.2.1 Set.prototype, https://tc39.es/ecma262/#sec-set.prototype
    define_direct_property(vm.names.prototype, global_object.set_prototype(), 0);

    define_old_native_accessor(*vm.well_known_symbol_species(), symbol_species_getter, {}, Attribute::Configurable);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
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
Value SetConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto* set = TRY_OR_DISCARD(ordinary_create_from_constructor<Set>(global_object, new_target, &GlobalObject::set_prototype));

    if (vm.argument(0).is_nullish())
        return set;

    auto adder = TRY_OR_DISCARD(set->get(vm.names.add));
    if (!adder.is_function()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, "'add' property of Set");
        return {};
    }

    TRY_OR_DISCARD(get_iterator_values(global_object, vm.argument(0), [&](Value iterator_value) -> Optional<Completion> {
        TRY(vm.call(adder.as_function(), Value(set), iterator_value));
        return {};
    }));

    return set;
}

// 24.2.2.2 get Set [ @@species ], https://tc39.es/ecma262/#sec-get-set-@@species
JS_DEFINE_OLD_NATIVE_FUNCTION(SetConstructor::symbol_species_getter)
{
    return vm.this_value(global_object);
}

}
