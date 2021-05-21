/*
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibJS/Runtime/TypedArrayPrototype.h>

namespace JS {

TypedArrayPrototype::TypedArrayPrototype(GlobalObject& global_object)
    : Object(*global_object.object_prototype())
{
}

void TypedArrayPrototype::initialize(GlobalObject& object)
{
    auto& vm = this->vm();
    Object::initialize(object);
    u8 attr = Attribute::Writable | Attribute::Configurable;

    // FIXME: This should be an accessor property
    define_native_property(vm.names.length, length_getter, nullptr, Attribute::Configurable);
    define_native_property(vm.names.buffer, buffer_getter, nullptr, Attribute::Configurable);
    define_native_property(vm.names.byteLength, byte_length_getter, nullptr, Attribute::Configurable);
    define_native_property(vm.names.byteOffset, byte_offset_getter, nullptr, Attribute::Configurable);
    define_native_function(vm.names.at, at, 1, attr);
}

TypedArrayPrototype::~TypedArrayPrototype()
{
}

static TypedArrayBase* typed_array_from(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return nullptr;
    if (!this_object->is_typed_array()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "TypedArray");
        return nullptr;
    }
    return static_cast<TypedArrayBase*>(this_object);
}

JS_DEFINE_NATIVE_GETTER(TypedArrayPrototype::length_getter)
{
    auto typed_array = typed_array_from(vm, global_object);
    if (!typed_array)
        return {};
    return Value(typed_array->array_length());
}

JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::at)
{
    auto typed_array = typed_array_from(vm, global_object);
    if (!typed_array)
        return {};
    auto length = typed_array->array_length();
    auto relative_index = vm.argument(0).to_integer_or_infinity(global_object);
    if (vm.exception())
        return {};
    if (Value(relative_index).is_infinity())
        return js_undefined();
    Checked<size_t> index { 0 };
    if (relative_index >= 0) {
        index += relative_index;
    } else {
        index += length;
        index -= -relative_index;
    }
    if (index.has_overflow() || index.value() >= length)
        return js_undefined();
    return typed_array->get(index.value());
}

// https://tc39.es/ecma262/#sec-get-%typedarray%.prototype.buffer
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::buffer_getter)
{
    auto typed_array = typed_array_from(vm, global_object);
    if (!typed_array)
        return {};
    auto* array_buffer = typed_array->viewed_array_buffer();
    VERIFY(array_buffer);
    return Value(array_buffer);
}

// https://tc39.es/ecma262/#sec-get-%typedarray%.prototype.bytelength
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::byte_length_getter)
{
    auto typed_array = typed_array_from(vm, global_object);
    if (!typed_array)
        return {};
    auto* array_buffer = typed_array->viewed_array_buffer();
    VERIFY(array_buffer);
    // FIXME: If array_buffer is detached, return 0.
    return Value(typed_array->byte_length());
}

// https://tc39.es/ecma262/#sec-get-%typedarray%.prototype.byteoffset
JS_DEFINE_NATIVE_FUNCTION(TypedArrayPrototype::byte_offset_getter)
{
    auto typed_array = typed_array_from(vm, global_object);
    if (!typed_array)
        return {};
    auto* array_buffer = typed_array->viewed_array_buffer();
    VERIFY(array_buffer);
    // FIXME: If array_buffer is detached, return 0.
    return Value(typed_array->byte_offset());
}

}
