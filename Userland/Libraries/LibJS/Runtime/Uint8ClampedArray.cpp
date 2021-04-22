/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Uint8ClampedArray.h>

namespace JS {

Uint8ClampedArray* Uint8ClampedArray::create(GlobalObject& global_object, u32 length)
{
    return global_object.heap().allocate<Uint8ClampedArray>(global_object, length, *global_object.array_prototype());
}

Uint8ClampedArray::Uint8ClampedArray(u32 length, Object& prototype)
    : Object(prototype)
    , m_length(length)
{
    auto& vm = this->vm();
    define_native_property(vm.names.length, length_getter, {});
    m_data = (u8*)calloc(m_length, 1);
}

Uint8ClampedArray::~Uint8ClampedArray()
{
    VERIFY(m_data);
    free(m_data);
    m_data = nullptr;
}

JS_DEFINE_NATIVE_GETTER(Uint8ClampedArray::length_getter)
{
    auto* this_object = vm.this_value(global_object).to_object(global_object);
    if (!this_object)
        return {};
    if (StringView(this_object->class_name()) != "Uint8ClampedArray") {
        vm.throw_exception<TypeError>(global_object, ErrorType::NotA, "Uint8ClampedArray");
        return {};
    }
    return Value(static_cast<const Uint8ClampedArray*>(this_object)->length());
}

bool Uint8ClampedArray::put_by_index(u32 property_index, Value value)
{
    if (property_index >= m_length)
        return Base::put_by_index(property_index, value);
    auto number = value.to_i32(global_object());
    if (vm().exception())
        return {};
    m_data[property_index] = clamp(number, 0, 255);
    return true;
}

Value Uint8ClampedArray::get_by_index(u32 property_index) const
{
    if (property_index >= m_length)
        return Base::get_by_index(property_index);
    return Value((i32)m_data[property_index]);
}

}
