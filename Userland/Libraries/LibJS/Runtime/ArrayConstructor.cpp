/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2022, Linus Groh <linusg@serenityos.org>
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
ThrowCompletionOr<Value> ArrayConstructor::call()
{
    return TRY(construct(*this));
}

// 23.1.1.1 Array ( ...values ), https://tc39.es/ecma262/#sec-array
ThrowCompletionOr<Object*> ArrayConstructor::construct(FunctionObject& new_target)
{
    auto& vm = this->vm();

    auto* proto = TRY(get_prototype_from_constructor(global_object(), new_target, &GlobalObject::array_prototype));

    if (vm.argument_count() == 0)
        return MUST(Array::create(global_object(), 0, proto));

    if (vm.argument_count() == 1) {
        auto length = vm.argument(0);
        auto* array = MUST(Array::create(global_object(), 0, proto));
        size_t int_length;
        if (!length.is_number()) {
            MUST(array->create_data_property_or_throw(0, length));
            int_length = 1;
        } else {
            int_length = MUST(length.to_u32(global_object()));
            if (int_length != length.as_double())
                return vm.throw_completion<RangeError>(global_object(), ErrorType::InvalidLength, "array");
        }
        TRY(array->set(vm.names.length, Value(int_length), Object::ShouldThrowExceptions::Yes));
        return array;
    }

    auto* array = TRY(Array::create(global_object(), vm.argument_count(), proto));

    for (size_t k = 0; k < vm.argument_count(); ++k)
        MUST(array->create_data_property_or_throw(k, vm.argument(k)));

    return array;
}

// 23.1.2.1 Array.from ( items [ , mapfn [ , thisArg ] ] ), https://tc39.es/ecma262/#sec-array.from
JS_DEFINE_NATIVE_FUNCTION(ArrayConstructor::from)
{
    auto constructor = vm.this_value(global_object);

    FunctionObject* map_fn = nullptr;
    if (!vm.argument(1).is_undefined()) {
        auto callback = vm.argument(1);
        if (!callback.is_function())
            return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, callback.to_string_without_side_effects());
        map_fn = &callback.as_function();
    }

    auto this_arg = vm.argument(2);

    auto items = vm.argument(0);
    auto using_iterator = TRY(items.get_method(global_object, *vm.well_known_symbol_iterator()));
    if (using_iterator) {
        Object* array;
        if (constructor.is_constructor())
            array = TRY(JS::construct(global_object, constructor.as_function(), {}));
        else
            array = MUST(Array::create(global_object, 0));

        auto iterator = TRY(get_iterator(global_object, items, IteratorHint::Sync, using_iterator));

        size_t k = 0;
        while (true) {
            if (k >= MAX_ARRAY_LIKE_INDEX) {
                auto error = vm.throw_completion<TypeError>(global_object, ErrorType::ArrayMaxSize);
                return TRY(iterator_close(global_object, iterator, move(error)));
            }

            auto* next = TRY(iterator_step(global_object, iterator));
            if (!next) {
                TRY(array->set(vm.names.length, Value(k), Object::ShouldThrowExceptions::Yes));
                return array;
            }

            auto next_value = TRY(iterator_value(global_object, *next));

            Value mapped_value;
            if (map_fn) {
                auto mapped_value_or_error = JS::call(global_object, *map_fn, this_arg, next_value, Value(k));
                if (mapped_value_or_error.is_error())
                    return TRY(iterator_close(global_object, iterator, mapped_value_or_error.release_error()));
                mapped_value = mapped_value_or_error.release_value();
            } else {
                mapped_value = next_value;
            }

            auto result_or_error = array->create_data_property_or_throw(k, mapped_value);
            if (result_or_error.is_error())
                return TRY(iterator_close(global_object, iterator, result_or_error.release_error()));

            ++k;
        }
    }

    auto* array_like = MUST(items.to_object(global_object));

    auto length = TRY(length_of_array_like(global_object, *array_like));

    Object* array;
    if (constructor.is_constructor())
        array = TRY(JS::construct(global_object, constructor.as_function(), Value(length)));
    else
        array = TRY(Array::create(global_object, length));

    for (size_t k = 0; k < length; ++k) {
        auto k_value = TRY(array_like->get(k));
        Value mapped_value;
        if (map_fn)
            mapped_value = TRY(JS::call(global_object, *map_fn, this_arg, k_value, Value(k)));
        else
            mapped_value = k_value;
        TRY(array->create_data_property_or_throw(k, mapped_value));
    }

    TRY(array->set(vm.names.length, Value(length), Object::ShouldThrowExceptions::Yes));

    return array;
}

// 23.1.2.2 Array.isArray ( arg ), https://tc39.es/ecma262/#sec-array.isarray
JS_DEFINE_NATIVE_FUNCTION(ArrayConstructor::is_array)
{
    auto value = vm.argument(0);
    return Value(TRY(value.is_array(global_object)));
}

// 23.1.2.3 Array.of ( ...items ), https://tc39.es/ecma262/#sec-array.of
JS_DEFINE_NATIVE_FUNCTION(ArrayConstructor::of)
{
    auto this_value = vm.this_value(global_object);
    Object* array;
    if (this_value.is_constructor())
        array = TRY(JS::construct(global_object, this_value.as_function(), Value(vm.argument_count())));
    else
        array = TRY(Array::create(global_object, vm.argument_count()));
    for (size_t k = 0; k < vm.argument_count(); ++k)
        TRY(array->create_data_property_or_throw(k, vm.argument(k)));
    TRY(array->set(vm.names.length, Value(vm.argument_count()), Object::ShouldThrowExceptions::Yes));
    return array;
}

// 23.1.2.5 get Array [ @@species ], https://tc39.es/ecma262/#sec-get-array-@@species
JS_DEFINE_NATIVE_FUNCTION(ArrayConstructor::symbol_species_getter)
{
    return vm.this_value(global_object);
}

}
