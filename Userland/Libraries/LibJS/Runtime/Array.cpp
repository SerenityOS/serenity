/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

// 10.4.2.2 ArrayCreate ( length [ , proto ] ), https://tc39.es/ecma262/#sec-arraycreate
Array* Array::create(GlobalObject& global_object, size_t length)
{
    // FIXME: Support proto parameter
    if (length > NumericLimits<u32>::max()) {
        auto& vm = global_object.vm();
        vm.throw_exception<RangeError>(global_object, ErrorType::InvalidLength, "array");
        return nullptr;
    }
    auto* array = global_object.heap().allocate<Array>(global_object, *global_object.array_prototype());
    array->indexed_properties().set_array_like_size(length);
    return array;
}

// 7.3.17 CreateArrayFromList ( elements ), https://tc39.es/ecma262/#sec-createarrayfromlist
Array* Array::create_from(GlobalObject& global_object, const Vector<Value>& elements)
{
    auto* array = Array::create(global_object);
    for (size_t i = 0; i < elements.size(); ++i)
        array->define_property(i, elements[i]);
    return array;
}

Array::Array(Object& prototype)
    : Object(prototype)
{
}

void Array::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    define_native_property(vm.names.length, length_getter, length_setter, Attribute::Writable);
}

Array::~Array()
{
}

Array* Array::typed_this(VM& vm, GlobalObject& global_object)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (!this_object->is_array()) {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotAn, "Array");
        return nullptr;
    }
    return static_cast<Array*>(this_object);
}

JS_DEFINE_NATIVE_GETTER(Array::length_getter)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};

    // TODO: could be incorrect if receiver/this_value is fixed or changed
    if (!this_object->is_array()) {
        Value val = this_object->get_own_property(vm.names.length.to_string_or_symbol(), this_object);
        if (vm.exception())
            return {};
        return val;
    }

    return Value(this_object->indexed_properties().array_like_size());
}

JS_DEFINE_NATIVE_SETTER(Array::length_setter)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return;

    // TODO: could be incorrect if receiver/this_value is fixed or changed
    if (!this_object->is_array()) {
        this_object->define_property(vm.names.length.to_string_or_symbol(), value, default_attributes);
        if (vm.exception())
            return;
        return;
    }

    auto length = value.to_number(global_object);
    if (vm.exception())
        return;

    u32 val = length.as_double();

    if (val != length.as_double()
        || length.is_nan()
        || length.is_infinity()
        || length.as_double() < 0
        || length.as_double() > NumericLimits<u32>::max()) {
        vm.throw_exception<RangeError>(global_object, ErrorType::InvalidLength, "array");
        return;
    }
    this_object->indexed_properties().set_array_like_size(val);
}

}
