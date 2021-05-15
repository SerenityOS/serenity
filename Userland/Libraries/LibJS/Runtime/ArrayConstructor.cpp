/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/ArrayConstructor.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/Shape.h>

namespace JS {

ArrayConstructor::ArrayConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.Array, *global_object.function_prototype())
{
}

ArrayConstructor::~ArrayConstructor()
{
}

void ArrayConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    define_property(vm.names.prototype, global_object.array_prototype(), 0);
    define_property(vm.names.length, Value(1), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.from, from, 1, attr);
    define_native_function(vm.names.isArray, is_array, 1, attr);
    define_native_function(vm.names.of, of, 0, attr);
}

Value ArrayConstructor::call()
{
    if (vm().argument_count() <= 0)
        return Array::create(global_object());

    if (vm().argument_count() == 1 && vm().argument(0).is_number()) {
        auto array_length_value = vm().argument(0);
        if (!array_length_value.is_integer() || array_length_value.as_i32() < 0) {
            vm().throw_exception<RangeError>(global_object(), ErrorType::InvalidLength, "array");
            return {};
        }
        auto* array = Array::create(global_object());
        array->indexed_properties().set_array_like_size(array_length_value.as_i32());
        return array;
    }

    auto* array = Array::create(global_object());
    for (size_t i = 0; i < vm().argument_count(); ++i)
        array->indexed_properties().append(vm().argument(i));
    return array;
}

Value ArrayConstructor::construct(Function&)
{
    return call();
}

JS_DEFINE_NATIVE_FUNCTION(ArrayConstructor::from)
{
    auto value = vm.argument(0);
    auto object = value.to_object(global_object);
    if (!object)
        return {};

    auto* array = Array::create(global_object);

    Function* map_fn = nullptr;
    if (!vm.argument(1).is_undefined()) {
        auto callback = vm.argument(1);
        if (!callback.is_function()) {
            vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, callback.to_string_without_side_effects());
            return {};
        }
        map_fn = &callback.as_function();
    }

    auto this_arg = vm.argument(2);

    // Array.from() lets you create Arrays from:
    if (auto size = object->indexed_properties().array_like_size()) {
        // * array-like objects (objects with a length property and indexed elements)
        MarkedValueList elements(vm.heap());
        elements.ensure_capacity(size);
        for (size_t i = 0; i < size; ++i) {
            if (map_fn) {
                auto element = object->get(i);
                if (vm.exception())
                    return {};

                auto map_fn_result = vm.call(*map_fn, this_arg, element, Value((i32)i));
                if (vm.exception())
                    return {};

                elements.append(map_fn_result);
            } else {
                elements.append(object->get(i));
                if (vm.exception())
                    return {};
            }
        }
        array->set_indexed_property_elements(move(elements));
    } else {
        // * iterable objects
        i32 i = 0;
        get_iterator_values(global_object, value, [&](Value element) {
            if (vm.exception())
                return IterationDecision::Break;

            if (map_fn) {
                auto map_fn_result = vm.call(*map_fn, this_arg, element, Value(i));
                i++;
                if (vm.exception())
                    return IterationDecision::Break;

                array->indexed_properties().append(map_fn_result);
            } else {
                array->indexed_properties().append(element);
            }

            return IterationDecision::Continue;
        });
        if (vm.exception())
            return {};
    }

    return array;
}

JS_DEFINE_NATIVE_FUNCTION(ArrayConstructor::is_array)
{
    auto value = vm.argument(0);
    return Value(value.is_array());
}

JS_DEFINE_NATIVE_FUNCTION(ArrayConstructor::of)
{
    auto* array = Array::create(global_object);
    for (size_t i = 0; i < vm.argument_count(); ++i)
        array->indexed_properties().append(vm.argument(i));
    return array;
}

}
