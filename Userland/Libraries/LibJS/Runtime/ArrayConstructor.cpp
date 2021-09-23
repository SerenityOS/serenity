/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ArrayConstructor.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/Shape.h>

namespace JS {

ArrayConstructor::ArrayConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Array.as_string(), *global_object.function_prototype())
{
}

ArrayConstructor::~ArrayConstructor()
{
}

void ArrayConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 23.1.2.4 Array.prototype, https://tc39.es/ecma262/#sec-array.prototype
    define_direct_property(vm.names.prototype, global_object.array_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.from, from, 1, attr);
    define_native_function(vm.names.isArray, is_array, 1, attr);
    define_native_function(vm.names.of, of, 0, attr);

    // 23.1.2.5 get Array [ @@species ], https://tc39.es/ecma262/#sec-get-array-@@species
    define_native_accessor(*vm.well_known_symbol_species(), symbol_species_getter, {}, Attribute::Configurable);

    define_direct_property(vm.names.length, Value(1), Attribute::Configurable);
}

// 23.1.1.1 Array ( ...values ), https://tc39.es/ecma262/#sec-array
Value ArrayConstructor::call()
{
    return construct(*this);
}

// 23.1.1.1 Array ( ...values ), https://tc39.es/ecma262/#sec-array
Value ArrayConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();

    auto* proto = TRY_OR_DISCARD(get_prototype_from_constructor(global_object(), new_target, &GlobalObject::array_prototype));

    if (vm.argument_count() == 0)
        return Array::create(global_object(), 0, proto);

    if (vm.argument_count() == 1) {
        auto length = vm.argument(0);
        auto* array = Array::create(global_object(), 0, proto);
        size_t int_length;
        if (!length.is_number()) {
            array->create_data_property_or_throw(0, length);
            int_length = 1;
        } else {
            int_length = length.to_u32(global_object());
            if (int_length != length.as_double()) {
                vm.throw_exception<RangeError>(global_object(), ErrorType::InvalidLength, "array");
                return {};
            }
        }
        array->set(vm.names.length, Value(int_length), Object::ShouldThrowExceptions::Yes);
        if (vm.exception())
            return {};
        return array;
    }

    auto* array = Array::create(global_object(), vm.argument_count(), proto);
    if (vm.exception())
        return {};

    for (size_t k = 0; k < vm.argument_count(); ++k)
        array->create_data_property_or_throw(k, vm.argument(k));

    return array;
}

// 23.1.2.1 Array.from ( items [ , mapfn [ , thisArg ] ] ), https://tc39.es/ecma262/#sec-array.from
JS_DEFINE_NATIVE_FUNCTION(ArrayConstructor::from)
{
    auto constructor = vm.this_value(global_object);

    FunctionObject* map_fn = nullptr;
    if (!vm.argument(1).is_undefined()) {
        auto callback = vm.argument(1);
        if (!callback.is_function()) {
            vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, callback.to_string_without_side_effects());
            return {};
        }
        map_fn = &callback.as_function();
    }

    auto this_arg = vm.argument(2);

    auto items = vm.argument(0);
    auto using_iterator = TRY_OR_DISCARD(items.get_method(global_object, *vm.well_known_symbol_iterator()));
    if (using_iterator) {
        Value array;
        if (constructor.is_constructor()) {
            array = vm.construct(constructor.as_function(), constructor.as_function(), {});
            if (vm.exception())
                return {};
        } else {
            array = Array::create(global_object, 0);
        }
        auto iterator = get_iterator(global_object, items, IteratorHint::Sync, using_iterator);
        if (vm.exception())
            return {};

        auto& array_object = array.as_object();

        size_t k = 0;
        while (true) {
            if (k >= MAX_ARRAY_LIKE_INDEX) {
                vm.throw_exception<TypeError>(global_object, ErrorType::ArrayMaxSize);
                iterator_close(*iterator);
                return {};
            }

            auto next = iterator_step(global_object, *iterator);
            if (vm.exception())
                return {};

            if (!next) {
                array_object.set(vm.names.length, Value(k), Object::ShouldThrowExceptions::Yes);
                if (vm.exception())
                    return {};
                return array;
            }

            auto next_value = iterator_value(global_object, *next);
            if (vm.exception())
                return {};

            Value mapped_value;
            if (map_fn) {
                mapped_value = TRY_OR_DISCARD(vm.call(*map_fn, this_arg, next_value, Value(k)));
                if (vm.exception()) {
                    iterator_close(*iterator);
                    return {};
                }
            } else {
                mapped_value = next_value;
            }

            array_object.create_data_property_or_throw(k, mapped_value);
            if (vm.exception()) {
                iterator_close(*iterator);
                return {};
            }

            ++k;
        }
    }

    auto* array_like = items.to_object(global_object);

    auto length = TRY_OR_DISCARD(length_of_array_like(global_object, *array_like));

    Value array;
    if (constructor.is_constructor()) {
        MarkedValueList arguments(vm.heap());
        arguments.empend(length);
        array = vm.construct(constructor.as_function(), constructor.as_function(), move(arguments));
        if (vm.exception())
            return {};
    } else {
        array = Array::create(global_object, length);
        if (vm.exception())
            return {};
    }

    auto& array_object = array.as_object();

    for (size_t k = 0; k < length; ++k) {
        auto k_value = array_like->get(k);
        if (vm.exception())
            return {};
        Value mapped_value;
        if (map_fn)
            mapped_value = TRY_OR_DISCARD(vm.call(*map_fn, this_arg, k_value, Value(k)));
        else
            mapped_value = k_value;
        array_object.create_data_property_or_throw(k, mapped_value);
    }

    array_object.set(vm.names.length, Value(length), Object::ShouldThrowExceptions::Yes);
    if (vm.exception())
        return {};

    return array;
}

// 23.1.2.2 Array.isArray ( arg ), https://tc39.es/ecma262/#sec-array.isarray
JS_DEFINE_NATIVE_FUNCTION(ArrayConstructor::is_array)
{
    auto value = vm.argument(0);
    return Value(TRY_OR_DISCARD(value.is_array(global_object)));
}

// 23.1.2.3 Array.of ( ...items ), https://tc39.es/ecma262/#sec-array.of
JS_DEFINE_NATIVE_FUNCTION(ArrayConstructor::of)
{
    auto this_value = vm.this_value(global_object);
    Value array;
    if (this_value.is_constructor()) {
        MarkedValueList arguments(vm.heap());
        arguments.empend(vm.argument_count());
        array = vm.construct(this_value.as_function(), this_value.as_function(), move(arguments));
        if (vm.exception())
            return {};
    } else {
        array = Array::create(global_object, vm.argument_count());
        if (vm.exception())
            return {};
    }
    auto& array_object = array.as_object();
    for (size_t k = 0; k < vm.argument_count(); ++k) {
        array_object.create_data_property_or_throw(k, vm.argument(k));
        if (vm.exception())
            return {};
    }
    array_object.set(vm.names.length, Value(vm.argument_count()), Object::ShouldThrowExceptions::Yes);
    if (vm.exception())
        return {};
    return array;
}

// 23.1.2.5 get Array [ @@species ], https://tc39.es/ecma262/#sec-get-array-@@species
JS_DEFINE_NATIVE_GETTER(ArrayConstructor::symbol_species_getter)
{
    return vm.this_value(global_object);
}

}
