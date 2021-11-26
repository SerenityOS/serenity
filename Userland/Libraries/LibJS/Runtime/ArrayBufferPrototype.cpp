/*
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/ArrayBufferConstructor.h>
#include <LibJS/Runtime/ArrayBufferPrototype.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

ArrayBufferPrototype::ArrayBufferPrototype(GlobalObject& global_object)
    : PrototypeObject(*global_object.object_prototype())
{
}

void ArrayBufferPrototype::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    u8 attr = Attribute::Writable | Attribute::Configurable;
    define_native_function(vm.names.slice, slice, 2, attr);
    define_native_accessor(vm.names.byteLength, byte_length_getter, {}, Attribute::Configurable);

    // 25.1.5.4 ArrayBuffer.prototype [ @@toStringTag ], https://tc39.es/ecma262/#sec-arraybuffer.prototype-@@tostringtag
    define_direct_property(*vm.well_known_symbol_to_string_tag(), js_string(vm, vm.names.ArrayBuffer.as_string()), Attribute::Configurable);
}

ArrayBufferPrototype::~ArrayBufferPrototype()
{
}

// 25.1.5.3 ArrayBuffer.prototype.slice ( start, end ), https://tc39.es/ecma262/#sec-arraybuffer.prototype.slice
JS_DEFINE_NATIVE_FUNCTION(ArrayBufferPrototype::slice)
{
    auto* array_buffer_object = TRY(typed_this_value(global_object));
    // FIXME: Check for shared buffer
    if (array_buffer_object->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    auto length = array_buffer_object->byte_length();

    auto relative_start = TRY(vm.argument(0).to_integer_or_infinity(global_object));

    double first;
    if (relative_start < 0)
        first = max(length + relative_start, 0.0);
    else
        first = min(relative_start, (double)length);

    auto relative_end = vm.argument(1).is_undefined() ? length : TRY(vm.argument(1).to_integer_or_infinity(global_object));

    double final;
    if (relative_end < 0)
        final = max(length + relative_end, 0.0);
    else
        final = min(relative_end, (double)length);

    auto new_length = max(final - first, 0.0);

    auto* constructor = TRY(species_constructor(global_object, *array_buffer_object, *global_object.array_buffer_constructor()));

    MarkedValueList arguments(vm.heap());
    arguments.append(Value(new_length));
    auto* new_array_buffer = TRY(construct(global_object, *constructor, move(arguments)));

    if (!is<ArrayBuffer>(new_array_buffer))
        return vm.throw_completion<TypeError>(global_object, ErrorType::SpeciesConstructorDidNotCreate, "an ArrayBuffer");
    auto* new_array_buffer_object = static_cast<ArrayBuffer*>(new_array_buffer);

    // FIXME: Check for shared buffer
    if (new_array_buffer_object->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::SpeciesConstructorReturned, "a detached ArrayBuffer");
    if (same_value(new_array_buffer_object, array_buffer_object))
        return vm.throw_completion<TypeError>(global_object, ErrorType::SpeciesConstructorReturned, "same ArrayBuffer instance");
    if (new_array_buffer_object->byte_length() < new_length)
        return vm.throw_completion<TypeError>(global_object, ErrorType::SpeciesConstructorReturned, "an ArrayBuffer smaller than requested");

    if (array_buffer_object->is_detached())
        return vm.throw_completion<TypeError>(global_object, ErrorType::DetachedArrayBuffer);

    // This is ugly, is there a better way to do this?
    array_buffer_object->buffer().span().slice(first, new_length).copy_to(new_array_buffer_object->buffer().span());
    return new_array_buffer_object;
}

// 25.1.5.1 get ArrayBuffer.prototype.byteLength, https://tc39.es/ecma262/#sec-get-arraybuffer.prototype.bytelength
JS_DEFINE_NATIVE_FUNCTION(ArrayBufferPrototype::byte_length_getter)
{
    auto* array_buffer_object = TRY(typed_this_value(global_object));
    // FIXME: Check for shared buffer
    if (array_buffer_object->is_detached())
        return Value(0);

    return Value(array_buffer_object->byte_length());
}

}
