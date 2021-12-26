/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Runtime/DeclarativeEnvironmentRecord.h>
#include <LibJS/Runtime/GlobalEnvironmentRecord.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ObjectEnvironmentRecord.h>

namespace JS {

GlobalEnvironmentRecord::GlobalEnvironmentRecord(GlobalObject& global_object)
    : EnvironmentRecord(nullptr)
    , m_global_object(global_object)
{
    m_object_record = global_object.heap().allocate<ObjectEnvironmentRecord>(global_object, global_object, nullptr);
    m_declarative_record = global_object.heap().allocate<DeclarativeEnvironmentRecord>(global_object);
}

void GlobalEnvironmentRecord::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_object_record);
    visitor.visit(m_declarative_record);
}

Optional<Variable> GlobalEnvironmentRecord::get_from_environment_record(FlyString const& name) const
{
    // FIXME: This should be a "composite" of the object record and the declarative record.
    return m_object_record->get_from_environment_record(name);
}

void GlobalEnvironmentRecord::put_into_environment_record(FlyString const& name, Variable variable)
{
    // FIXME: This should be a "composite" of the object record and the declarative record.
    m_object_record->put_into_environment_record(name, variable);
}

bool GlobalEnvironmentRecord::delete_from_environment_record(FlyString const& name)
{
    // FIXME: This should be a "composite" of the object record and the declarative record.
    return object_record().delete_from_environment_record(name);
}

Value GlobalEnvironmentRecord::get_this_binding(GlobalObject&) const
{
    return &m_global_object;
}

Value GlobalEnvironmentRecord::global_this_value() const
{
    return &m_global_object;
}

// 9.1.1.4.1 HasBinding ( N ), https://tc39.es/ecma262/#sec-global-environment-records-hasbinding-n
bool GlobalEnvironmentRecord::has_binding(FlyString const& name) const
{
    if (m_declarative_record->has_binding(name))
        return true;
    return m_object_record->has_binding(name);
}

void GlobalEnvironmentRecord::create_mutable_binding(GlobalObject& global_object, FlyString const& name, bool can_be_deleted)
{
    if (m_declarative_record->has_binding(name)) {
        global_object.vm().throw_exception<TypeError>(global_object, ErrorType::FixmeAddAnErrorString);
        return;
    }
    m_declarative_record->create_mutable_binding(global_object, name, can_be_deleted);
}

void GlobalEnvironmentRecord::create_immutable_binding(GlobalObject& global_object, FlyString const& name, bool strict)
{
    if (m_declarative_record->has_binding(name)) {
        global_object.vm().throw_exception<TypeError>(global_object, ErrorType::FixmeAddAnErrorString);
        return;
    }
    m_declarative_record->create_immutable_binding(global_object, name, strict);
}

void GlobalEnvironmentRecord::initialize_binding(GlobalObject& global_object, FlyString const& name, Value value)
{
    if (m_declarative_record->has_binding(name)) {
        m_declarative_record->initialize_binding(global_object, name, value);
        return;
    }
    m_object_record->initialize_binding(global_object, name, value);
}

void GlobalEnvironmentRecord::set_mutable_binding(GlobalObject& global_object, FlyString const& name, Value value, bool strict)
{
    if (m_declarative_record->has_binding(name)) {
        m_declarative_record->set_mutable_binding(global_object, name, value, strict);
        return;
    }

    m_object_record->set_mutable_binding(global_object, name, value, strict);
}

Value GlobalEnvironmentRecord::get_binding_value(GlobalObject& global_object, FlyString const& name, bool strict)
{
    if (m_declarative_record->has_binding(name))
        return m_declarative_record->get_binding_value(global_object, name, strict);
    return m_object_record->get_binding_value(global_object, name, strict);
}

bool GlobalEnvironmentRecord::delete_binding(GlobalObject& global_object, FlyString const& name)
{
    if (m_declarative_record->has_binding(name))
        return m_declarative_record->delete_binding(global_object, name);

    bool existing_prop = m_object_record->object().has_own_property(name);
    if (existing_prop) {
        bool status = m_object_record->delete_binding(global_object, name);
        if (status) {
            m_var_names.remove_all_matching([&](auto& entry) { return entry == name; });
        }
        return status;
    }
    return true;
}

bool GlobalEnvironmentRecord::has_var_declaration(FlyString const& name) const
{
    return m_var_names.contains_slow(name);
}

bool GlobalEnvironmentRecord::has_lexical_declaration(FlyString const& name) const
{
    return m_declarative_record->has_binding(name);
}

bool GlobalEnvironmentRecord::has_restricted_global_property(FlyString const& name) const
{
    auto existing_prop = m_global_object.get_own_property_descriptor(name);
    if (!existing_prop.has_value() || existing_prop.value().value.is_undefined())
        return false;
    if (existing_prop.value().attributes.is_configurable())
        return false;
    return true;
}

bool GlobalEnvironmentRecord::can_declare_global_var(FlyString const& name) const
{
    bool has_property = m_object_record->object().has_own_property(name);
    if (has_property)
        return true;
    return m_object_record->object().is_extensible();
}

bool GlobalEnvironmentRecord::can_declare_global_function(FlyString const& name) const
{
    auto existing_prop = m_object_record->object().get_own_property_descriptor(name);
    if (!existing_prop.has_value() || existing_prop.value().value.is_undefined())
        return m_object_record->object().is_extensible();
    if (existing_prop.value().attributes.is_configurable())
        return true;
    if (existing_prop.value().is_data_descriptor() && existing_prop.value().attributes.is_writable() && existing_prop.value().attributes.is_enumerable())
        return true;
    return false;
}

void GlobalEnvironmentRecord::create_global_var_binding(FlyString const& name, bool can_be_deleted)
{
    bool has_property = m_object_record->object().has_own_property(name);
    bool extensible = m_object_record->object().is_extensible();
    if (!has_property && extensible) {
        m_object_record->create_mutable_binding(static_cast<GlobalObject&>(m_object_record->object()), name, can_be_deleted);
        m_object_record->initialize_binding(m_object_record->global_object(), name, js_undefined());
    }
    if (!m_var_names.contains_slow(name))
        m_var_names.append(name);
}

void GlobalEnvironmentRecord::create_global_function_binding(FlyString const& name, Value value, bool can_be_deleted)
{
    auto existing_prop = m_object_record->object().get_own_property_descriptor(name);
    PropertyDescriptor desc;
    if (!existing_prop.has_value() || existing_prop.value().value.is_undefined() || existing_prop.value().attributes.is_configurable()) {
        desc.value = value;
        desc.attributes.set_has_writable();
        desc.attributes.set_writable();
        desc.attributes.set_has_enumerable();
        desc.attributes.set_enumerable();
        desc.attributes.set_has_configurable();
        if (can_be_deleted)
            desc.attributes.set_configurable();
    } else {
        desc.value = value;
    }
    // FIXME: This should be DefinePropertyOrThrow, followed by Set
    m_object_record->object().define_property(name, value, desc.attributes);
    if (vm().exception())
        return;
    if (!m_var_names.contains_slow(name))
        m_var_names.append(name);
}

}
