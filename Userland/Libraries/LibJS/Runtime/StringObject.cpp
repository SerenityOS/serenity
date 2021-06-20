/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/PrimitiveString.h>
#include <LibJS/Runtime/StringObject.h>

namespace JS {

StringObject* StringObject::create(GlobalObject& global_object, PrimitiveString& primitive_string)
{
    return global_object.heap().allocate<StringObject>(global_object, primitive_string, *global_object.string_prototype());
}

StringObject::StringObject(PrimitiveString& string, Object& prototype)
    : Object(prototype)
    , m_string(string)
{
}

StringObject::~StringObject()
{
}

void StringObject::initialize(GlobalObject& global_object)
{
    auto& vm = this->vm();
    Object::initialize(global_object);
    define_property(vm.names.length, Value(m_string.string().length()), 0);
}

void StringObject::visit_edges(Cell::Visitor& visitor)
{
    Object::visit_edges(visitor);
    visitor.visit(&m_string);
}

Optional<PropertyDescriptor> StringObject::get_own_property_descriptor(PropertyName const& property_name) const
{
    if (!property_name.is_number() || property_name.as_number() >= m_string.string().length())
        return Base::get_own_property_descriptor(property_name);

    PropertyDescriptor descriptor;
    descriptor.value = js_string(heap(), m_string.string().substring(property_name.as_number(), 1));
    descriptor.attributes.set_has_configurable();
    descriptor.attributes.set_has_enumerable();
    descriptor.attributes.set_has_writable();
    descriptor.attributes.set_enumerable();
    return descriptor;
}

}
