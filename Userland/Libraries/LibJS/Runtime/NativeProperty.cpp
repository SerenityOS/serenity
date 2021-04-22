/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/NativeProperty.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

NativeProperty::NativeProperty(AK::Function<Value(VM&, GlobalObject&)> getter, AK::Function<void(VM&, GlobalObject&, Value)> setter)
    : m_getter(move(getter))
    , m_setter(move(setter))
{
}

NativeProperty::~NativeProperty()
{
}

Value NativeProperty::get(VM& vm, GlobalObject& global_object) const
{
    if (!m_getter)
        return js_undefined();
    return m_getter(vm, global_object);
}

void NativeProperty::set(VM& vm, GlobalObject& global_object, Value value)
{
    if (!m_setter)
        return;
    m_setter(vm, global_object, move(value));
}

}
