/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/Map.h>
#include <LibJS/Runtime/MapConstructor.h>

namespace JS {

MapConstructor::MapConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Map.as_string(), *global_object.function_prototype())
{
}

void MapConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 24.1.2.1 Map.prototype, https://tc39.es/ecma262/#sec-map.prototype
    define_direct_property(vm.names.prototype, global_object.map_prototype(), 0);

    define_native_accessor(*vm.well_known_symbol_species(), symbol_species_getter, {}, Attribute::Configurable);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

MapConstructor::~MapConstructor()
{
}

// 24.1.1.1 Map ( [ iterable ] ), https://tc39.es/ecma262/#sec-map-iterable
ThrowCompletionOr<Value> MapConstructor::call()
{
    auto& vm = this->vm();
    return vm.throw_completion<TypeError>(global_object(), ErrorType::ConstructorWithoutNew, vm.names.Map);
}

// 24.1.1.1 Map ( [ iterable ] ), https://tc39.es/ecma262/#sec-map-iterable
ThrowCompletionOr<Object*> MapConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();
    auto& global_object = this->global_object();

    auto* map = TRY(ordinary_create_from_constructor<Map>(global_object, new_target, &GlobalObject::map_prototype));

    if (vm.argument(0).is_nullish())
        return map;

    auto adder = TRY(map->get(vm.names.set));
    if (!adder.is_function())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, "'set' property of Map");

    TRY(get_iterator_values(global_object, vm.argument(0), [&](Value iterator_value) -> Optional<Completion> {
        if (!iterator_value.is_object())
            return vm.throw_completion<TypeError>(global_object, ErrorType::NotAnObject, String::formatted("Iterator value {}", iterator_value.to_string_without_side_effects()));

        auto key = TRY(iterator_value.as_object().get(0));
        auto value = TRY(iterator_value.as_object().get(1));
        TRY(vm.call(adder.as_function(), Value(map), key, value));

        return {};
    }));

    return map;
}

// 24.1.2.2 get Map [ @@species ], https://tc39.es/ecma262/#sec-get-map-@@species
JS_DEFINE_NATIVE_FUNCTION(MapConstructor::symbol_species_getter)
{
    return vm.this_value(global_object);
}

}
