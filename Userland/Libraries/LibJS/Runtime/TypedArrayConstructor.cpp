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
    define_direct_property(vm.names.prototype, global_object.typed_array_prototype(), 0);

    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.from, from, 1, attr);
    define_native_function(vm.names.of, of, 0, attr);

    define_native_accessor(*vm.well_known_symbol_species(), symbol_species_getter, {}, Attribute::Configurable);

    define_direct_property(vm.names.length, Value(0), Attribute::Configurable);
}

TypedArrayConstructor::~TypedArrayConstructor()
{
}

// 23.2.1.1 %TypedArray% ( ), https://tc39.es/ecma262/#sec-%typedarray%
ThrowCompletionOr<Value> TypedArrayConstructor::call()
{
    return TRY(construct(*this));
}

// 23.2.1.1 %TypedArray% ( ), https://tc39.es/ecma262/#sec-%typedarray%
ThrowCompletionOr<Object*> TypedArrayConstructor::construct(FunctionObject&)
{
    return vm().throw_completion<TypeError>(global_object(), ErrorType::ClassIsAbstract, "TypedArray");
}

// 23.2.2.1 %TypedArray%.from ( source [ , mapfn [ , thisArg ] ] ), https://tc39.es/ecma262/#sec-%typedarray%.from
JS_DEFINE_NATIVE_FUNCTION(TypedArrayConstructor::from)
{
    auto constructor = vm.this_value(global_object);
    if (!constructor.is_constructor())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAConstructor, constructor.to_string_without_side_effects());

    FunctionObject* map_fn = nullptr;
    if (!vm.argument(1).is_undefined()) {
        auto callback = vm.argument(1);
        if (!callback.is_function())
            return vm.throw_completion<TypeError>(global_object, ErrorType::NotAFunction, callback.to_string_without_side_effects());
        map_fn = &callback.as_function();
    }

    auto source = vm.argument(0);
    auto this_arg = vm.argument(2);

    auto using_iterator = TRY(source.get_method(global_object, *vm.well_known_symbol_iterator()));
    if (using_iterator) {
        auto values = TRY(iterable_to_list(global_object, source, using_iterator));

        MarkedValueList arguments(vm.heap());
        arguments.empend(values.size());
        auto target_object = TRY(typed_array_create(global_object, constructor.as_function(), move(arguments)));

        for (size_t k = 0; k < values.size(); ++k) {
            auto k_value = values[k];
            Value mapped_value;
            if (map_fn)
                mapped_value = TRY(vm.call(*map_fn, this_arg, k_value, Value(k)));
            else
                mapped_value = k_value;
            TRY(target_object->set(k, mapped_value, Object::ShouldThrowExceptions::Yes));
        }

        return target_object;
    }

    auto array_like = MUST(source.to_object(global_object));
    auto length = TRY(length_of_array_like(global_object, *array_like));

    MarkedValueList arguments(vm.heap());
    arguments.empend(length);
    auto target_object = TRY(typed_array_create(global_object, constructor.as_function(), move(arguments)));

    for (size_t k = 0; k < length; ++k) {
        auto k_value = TRY(array_like->get(k));
        Value mapped_value;
        if (map_fn)
            mapped_value = TRY(vm.call(*map_fn, this_arg, k_value, Value(k)));
        else
            mapped_value = k_value;
        TRY(target_object->set(k, mapped_value, Object::ShouldThrowExceptions::Yes));
    }

    return target_object;
}

// 23.2.2.2 %TypedArray%.of ( ...items ), https://tc39.es/ecma262/#sec-%typedarray%.of
JS_DEFINE_NATIVE_FUNCTION(TypedArrayConstructor::of)
{
    auto length = vm.argument_count();
    auto constructor = vm.this_value(global_object);
    if (!constructor.is_constructor())
        return vm.throw_completion<TypeError>(global_object, ErrorType::NotAConstructor, constructor.to_string_without_side_effects());
    MarkedValueList arguments(vm.heap());
    arguments.append(Value(length));
    auto new_object = TRY(typed_array_create(global_object, constructor.as_function(), move(arguments)));
    for (size_t k = 0; k < length; ++k) {
        auto success = TRY(new_object->set(k, vm.argument(k), Object::ShouldThrowExceptions::Yes));
        if (!success)
            return vm.throw_completion<TypeError>(global_object, ErrorType::TypedArrayFailedSettingIndex, k);
    }
    return new_object;
}

// 23.2.2.4 get %TypedArray% [ @@species ], https://tc39.es/ecma262/#sec-get-%typedarray%-@@species
JS_DEFINE_NATIVE_FUNCTION(TypedArrayConstructor::symbol_species_getter)
{
    return vm.this_value(global_object);
}

}
