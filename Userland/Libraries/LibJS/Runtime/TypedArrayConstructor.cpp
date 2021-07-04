/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/IteratorOperations.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/TypedArrayConstructor.h>

namespace JS {

TypedArrayConstructor::TypedArrayConstructor(const FlyString& name, Object& prototype)
    : NativeFunction(name, prototype)
{
}

TypedArrayConstructor::TypedArrayConstructor(GlobalObject& global_object)
    : NativeFunction(vm().names.TypedArray.as_string(), *global_object.function_prototype())
{
}

void TypedArrayConstructor::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    NativeFunction::initialize(global_object);

    // 23.2.2.3 %TypedArray%.prototype, https://tc39.es/ecma262/#sec-%typedarray%.prototype
    define_property(vm.names.prototype, global_object.typed_array_prototype(), 0);

    define_property(vm.names.length, Value(0), Attribute::Configurable);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.from, from, 1, attr);
    define_native_function(vm.names.of, of, 0, attr);

    define_native_accessor(*vm.well_known_symbol_species(), symbol_species_getter, {}, Attribute::Configurable);
}

TypedArrayConstructor::~TypedArrayConstructor()
{
}

// 23.2.1.1 %TypedArray% ( ), https://tc39.es/ecma262/#sec-%typedarray%
Value TypedArrayConstructor::call()
{
    return construct(*this);
}

// 23.2.1.1 %TypedArray% ( ), https://tc39.es/ecma262/#sec-%typedarray%
Value TypedArrayConstructor::construct(FunctionObject&)
{
    vm().throw_exception<TypeError>(global_object(), ErrorType::ClassIsAbstract, "TypedArray");
    return {};
}

// 23.2.4.2 TypedArrayCreate ( constructor, argumentList ), https://tc39.es/ecma262/#typedarray-create
static TypedArrayBase* typed_array_create(GlobalObject& global_object, FunctionObject& constructor, MarkedValueList arguments)
{
    auto& vm = global_object.vm();

    auto argument_count = arguments.size();
    auto first_argument = argument_count > 0 ? arguments[0] : js_undefined();

    auto new_typed_array = vm.construct(constructor, constructor, move(arguments));
    if (vm.exception())
        return nullptr;
    if (!new_typed_array.is_object() || !new_typed_array.as_object().is_typed_array()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "TypedArray");
        return nullptr;
    }
    auto& typed_array = static_cast<TypedArrayBase&>(new_typed_array.as_object());
    if (typed_array.viewed_array_buffer()->is_detached()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::DetachedArrayBuffer);
        return nullptr;
    }
    if (argument_count == 1 && first_argument.is_number() && typed_array.array_length() < first_argument.as_double()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::InvalidLength, "typed array");
        return nullptr;
    }
    return &typed_array;
}

// 23.2.2.1 %TypedArray%.from ( source [ , mapfn [ , thisArg ] ] ), https://tc39.es/ecma262/#sec-%typedarray%.from
JS_DEFINE_NATIVE_FUNCTION(TypedArrayConstructor::from)
{
    auto constructor = vm.this_value(global_object);
    if (!constructor.is_constructor()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAConstructor, constructor.to_string_without_side_effects());
        return {};
    }

    FunctionObject* map_fn = nullptr;
    if (!vm.argument(1).is_undefined()) {
        auto callback = vm.argument(1);
        if (!callback.is_function()) {
            vm.throw_exception<TypeError>(global_object, ErrorType::NotAFunction, callback.to_string_without_side_effects());
            return {};
        }
        map_fn = &callback.as_function();
    }

    auto source = vm.argument(0);
    auto this_arg = vm.argument(2);

    auto using_iterator = source.get_method(global_object, *vm.well_known_symbol_iterator());
    if (vm.exception())
        return {};
    if (using_iterator) {
        auto values = iterable_to_list(global_object, source, using_iterator);
        if (vm.exception())
            return {};

        MarkedValueList arguments(vm.heap());
        arguments.empend(values.size());
        auto target_object = typed_array_create(global_object, constructor.as_function(), move(arguments));
        if (vm.exception())
            return {};

        for (size_t k = 0; k < values.size(); ++k) {
            auto k_value = values[k];
            Value mapped_value;
            if (map_fn) {
                mapped_value = vm.call(*map_fn, this_arg, k_value, Value(k));
                if (vm.exception())
                    return {};
            } else {
                mapped_value = k_value;
            }
            target_object->set(k, mapped_value, true);
            if (vm.exception())
                return {};
        }

        return target_object;
    }

    auto array_like = source.to_object(global_object);
    auto length = length_of_array_like(global_object, *array_like);
    if (vm.exception())
        return {};

    MarkedValueList arguments(vm.heap());
    arguments.empend(length);
    auto target_object = typed_array_create(global_object, constructor.as_function(), move(arguments));
    if (vm.exception())
        return {};

    for (size_t k = 0; k < length; ++k) {
        auto k_value = array_like->get(k);
        Value mapped_value;
        if (map_fn) {
            mapped_value = vm.call(*map_fn, this_arg, k_value, Value(k));
            if (vm.exception())
                return {};
        } else {
            mapped_value = k_value;
        }
        target_object->set(k, mapped_value, true);
        if (vm.exception())
            return {};
    }

    return target_object;
}

// 23.2.2.2 %TypedArray%.of ( ...items ), https://tc39.es/ecma262/#sec-%typedarray%.of
JS_DEFINE_NATIVE_FUNCTION(TypedArrayConstructor::of)
{
    auto length = vm.argument_count();
    auto constructor = vm.this_value(global_object);
    if (!constructor.is_constructor()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAConstructor, constructor.to_string_without_side_effects());
        return {};
    }
    MarkedValueList arguments(vm.heap());
    arguments.append(Value(length));
    auto new_object = typed_array_create(global_object, constructor.as_function(), move(arguments));
    if (vm.exception())
        return {};
    for (size_t k = 0; k < length; ++k) {
        auto success = new_object->put(k, vm.argument(k));
        if (vm.exception())
            return {};
        if (!success) {
            vm.throw_exception<TypeError>(global_object, ErrorType::TypedArrayFailedSettingIndex, k);
            return {};
        }
    }
    return new_object;
}

// 23.2.2.4 get %TypedArray% [ @@species ], https://tc39.es/ecma262/#sec-get-%typedarray%-@@species
JS_DEFINE_NATIVE_GETTER(TypedArrayConstructor::symbol_species_getter)
{
    return vm.this_value(global_object);
}

}
