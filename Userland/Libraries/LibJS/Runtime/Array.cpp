/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/Array.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>

namespace JS {

Array* Array::create(GlobalObject& global_object)
{
    return global_object.heap().allocate<Array>(global_object, *global_object.array_prototype());
}

// 7.3.17 CreateArrayFromList, https://tc39.es/ecma262/#sec-createarrayfromlist
Array* Array::create_from(GlobalObject& global_object, const Vector<Value>& values)
{
    auto* array = Array::create(global_object);
    for (size_t i = 0; i < values.size(); ++i)
        array->define_property(i, values[i]);
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
    auto* array = typed_this(vm, global_object);
    if (!array)
        return {};
    return Value(array->indexed_properties().array_like_size());
}

JS_DEFINE_NATIVE_SETTER(Array::length_setter)
{
    auto* array = typed_this(vm, global_object);
    if (!array)
        return;
    auto length = value.to_number(global_object);
    if (vm.exception())
        return;
    if (length.is_nan() || length.is_infinity() || length.as_double() < 0) {
        vm.throw_exception<RangeError>(global_object, ErrorType::InvalidLength, "array");
        return;
    }
    array->indexed_properties().set_array_like_size(length.as_double());
}

}
