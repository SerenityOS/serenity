/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ObjectEnvironmentRecord.h>

namespace JS {

ObjectEnvironmentRecord::ObjectEnvironmentRecord(Object& binding_object, IsWithEnvironment is_with_environment, EnvironmentRecord* outer_environment)
    : EnvironmentRecord(outer_environment)
    , m_binding_object(binding_object)
    , m_with_environment(is_with_environment == IsWithEnvironment::Yes)
{
}

void ObjectEnvironmentRecord::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_binding_object);
}

Optional<Variable> ObjectEnvironmentRecord::get_from_environment_record(FlyString const& name) const
{
    auto value = m_binding_object.get(name);
    if (value.is_empty())
        return {};
    return Variable { value, DeclarationKind::Var };
}

void ObjectEnvironmentRecord::put_into_environment_record(FlyString const& name, Variable variable)
{
    m_binding_object.put(name, variable.value);
}

bool ObjectEnvironmentRecord::delete_from_environment_record(FlyString const& name)
{
    return m_binding_object.delete_property(name);
}

// 9.1.1.2.1 HasBinding ( N ), https://tc39.es/ecma262/#sec-object-environment-records-hasbinding-n
bool ObjectEnvironmentRecord::has_binding(FlyString const& name) const
{
    bool found_binding = m_binding_object.has_property(name);
    if (!found_binding)
        return false;

    // FIXME: Implement the rest of this operation.

    return true;
}

// 9.1.1.2.2 CreateMutableBinding ( N, D ), https://tc39.es/ecma262/#sec-object-environment-records-createmutablebinding-n-d
void ObjectEnvironmentRecord::create_mutable_binding(GlobalObject&, FlyString const& name, bool can_be_deleted)
{
    PropertyAttributes attributes;
    attributes.set_enumerable();
    attributes.set_has_enumerable();
    attributes.set_writable();
    attributes.set_has_writable();
    attributes.set_has_configurable();
    if (can_be_deleted)
        attributes.set_configurable();
    m_binding_object.define_property(name, js_undefined(), attributes, true);
}

// 9.1.1.2.3 CreateImmutableBinding ( N, S ), https://tc39.es/ecma262/#sec-object-environment-records-createimmutablebinding-n-s
void ObjectEnvironmentRecord::create_immutable_binding(GlobalObject&, FlyString const&, bool)
{
    // "The CreateImmutableBinding concrete method of an object Environment Record is never used within this specification."
    VERIFY_NOT_REACHED();
}

// 9.1.1.2.4 InitializeBinding ( N, V ), https://tc39.es/ecma262/#sec-object-environment-records-initializebinding-n-v
void ObjectEnvironmentRecord::initialize_binding(GlobalObject& global_object, FlyString const& name, Value value)
{
    set_mutable_binding(global_object, name, value, false);
}

// 9.1.1.2.5 SetMutableBinding ( N, V, S ), https://tc39.es/ecma262/#sec-object-environment-records-setmutablebinding-n-v-s
void ObjectEnvironmentRecord::set_mutable_binding(GlobalObject& global_object, FlyString const& name, Value value, bool strict)
{
    bool still_exists = m_binding_object.has_property(name);
    if (!still_exists && strict) {
        global_object.vm().throw_exception<ReferenceError>(global_object, ErrorType::UnknownIdentifier, name);
        return;
    }
    // FIXME: This should use the Set abstract operation.
    // FIXME: Set returns a bool, so this may need to return a bool as well.
    m_binding_object.put(name, value);
}

// 9.1.1.2.6 GetBindingValue ( N, S ), https://tc39.es/ecma262/#sec-object-environment-records-getbindingvalue-n-s
Value ObjectEnvironmentRecord::get_binding_value(GlobalObject& global_object, FlyString const& name, bool strict)
{
    if (!m_binding_object.has_property(name)) {
        if (!strict)
            return js_undefined();

        global_object.vm().throw_exception<ReferenceError>(global_object, ErrorType::UnknownIdentifier, name);
        return {};
    }
    // FIXME: This should use the Get abstract operation.
    return m_binding_object.get(name);
}

// 9.1.1.2.7 DeleteBinding ( N ), https://tc39.es/ecma262/#sec-object-environment-records-deletebinding-n
bool ObjectEnvironmentRecord::delete_binding(GlobalObject&, FlyString const& name)
{
    return m_binding_object.delete_property(name);
}

}
