/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Object.h>

namespace GUI {

Object::Object(Core::EventReceiver* parent)
    : Core::EventReceiver(parent)
{
}

Object::~Object() = default;

void Object::register_property(ByteString const& name, Function<JsonValue()> getter, Function<bool(JsonValue const&)> setter)
{
    m_properties.set(name, make<Property>(name, move(getter), move(setter)));
}

JsonValue Object::property(ByteString const& name) const
{
    auto it = m_properties.find(name);
    if (it == m_properties.end())
        return JsonValue();
    return it->value->get();
}

bool Object::set_property(ByteString const& name, JsonValue const& value)
{
    auto it = m_properties.find(name);
    if (it == m_properties.end())
        return false;
    return it->value->set(value);
}

static HashMap<StringView, ObjectClassRegistration*>& object_classes()
{
    static HashMap<StringView, ObjectClassRegistration*> s_map;
    return s_map;
}

ObjectClassRegistration::ObjectClassRegistration(StringView class_name, Function<ErrorOr<NonnullRefPtr<Object>>()> factory, ObjectClassRegistration* parent_class)
    : m_class_name(class_name)
    , m_factory(move(factory))
    , m_parent_class(parent_class)
{
    object_classes().set(class_name, this);
}

bool ObjectClassRegistration::is_derived_from(ObjectClassRegistration const& base_class) const
{
    if (&base_class == this)
        return true;
    if (!m_parent_class)
        return false;
    return m_parent_class->is_derived_from(base_class);
}

void ObjectClassRegistration::for_each(Function<void(ObjectClassRegistration const&)> callback)
{
    for (auto& it : object_classes()) {
        callback(*it.value);
    }
}

ObjectClassRegistration const* ObjectClassRegistration::find(StringView class_name)
{
    return object_classes().get(class_name).value_or(nullptr);
}

}
