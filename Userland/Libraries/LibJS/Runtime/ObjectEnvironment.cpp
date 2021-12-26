/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/AST.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/ObjectEnvironment.h>

namespace JS {

ObjectEnvironment::ObjectEnvironment(Object& binding_object, IsWithEnvironment is_with_environment, Environment* outer_environment)
    : Environment(outer_environment)
    , m_binding_object(binding_object)
    , m_with_environment(is_with_environment == IsWithEnvironment::Yes)
{
}

void ObjectEnvironment::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(&m_binding_object);
}

Optional<Variable> ObjectEnvironment::get_from_environment(FlyString const& name) const
{
    if (!m_binding_object.storage_has(name))
        return {};
    auto value = m_binding_object.get(name);
    return Variable { value, DeclarationKind::Var };
}

bool ObjectEnvironment::put_into_environment(FlyString const& name, Variable variable)
{
    return m_binding_object.put(name, variable.value);
}

bool ObjectEnvironment::delete_from_environment(FlyString const& name)
{
    return m_binding_object.internal_delete(name);
}

// 9.1.1.2.1 HasBinding ( N ), https://tc39.es/ecma262/#sec-object-environment-records-hasbinding-n
bool ObjectEnvironment::has_binding(FlyString const& name) const
{
    auto& vm = this->vm();
    bool found_binding = m_binding_object.has_property(name);
    if (vm.exception())
        return {};
    if (!found_binding)
        return false;
    if (!m_with_environment)
        return true;
    auto unscopables = m_binding_object.get(*vm.well_known_symbol_unscopables());
    if (vm.exception())
        return {};
    if (unscopables.is_object()) {
        auto blocked = unscopables.as_object().get(name);
        if (vm.exception())
            return {};
        if (blocked.to_boolean())
            return false;
    }
    return true;
}

// 9.1.1.2.2 CreateMutableBinding ( N, D ), https://tc39.es/ecma262/#sec-object-environment-records-createmutablebinding-n-d
void ObjectEnvironment::create_mutable_binding(GlobalObject&, FlyString const& name, bool can_be_deleted)
{
    // 1. Let bindingObject be envRec.[[BindingObject]].
    // 2. Return ? DefinePropertyOrThrow(bindingObject, N, PropertyDescriptor { [[Value]]: undefined, [[Writable]]: true, [[Enumerable]]: true, [[Configurable]]: D }).
    m_binding_object.define_property_or_throw(name, { .value = js_undefined(), .writable = true, .enumerable = true, .configurable = can_be_deleted });
}

// 9.1.1.2.3 CreateImmutableBinding ( N, S ), https://tc39.es/ecma262/#sec-object-environment-records-createimmutablebinding-n-s
void ObjectEnvironment::create_immutable_binding(GlobalObject&, FlyString const&, bool)
{
    // "The CreateImmutableBinding concrete method of an object Environment Record is never used within this specification."
    VERIFY_NOT_REACHED();
}

// 9.1.1.2.4 InitializeBinding ( N, V ), https://tc39.es/ecma262/#sec-object-environment-records-initializebinding-n-v
void ObjectEnvironment::initialize_binding(GlobalObject& global_object, FlyString const& name, Value value)
{
    set_mutable_binding(global_object, name, value, false);
}

// 9.1.1.2.5 SetMutableBinding ( N, V, S ), https://tc39.es/ecma262/#sec-object-environment-records-setmutablebinding-n-v-s
void ObjectEnvironment::set_mutable_binding(GlobalObject& global_object, FlyString const& name, Value value, bool strict)
{
    auto& vm = this->vm();
    bool still_exists = m_binding_object.has_property(name);
    if (vm.exception())
        return;
    if (!still_exists && strict) {
        global_object.vm().throw_exception<ReferenceError>(global_object, ErrorType::UnknownIdentifier, name);
        return;
    }
    m_binding_object.set(name, value, strict);
}

// 9.1.1.2.6 GetBindingValue ( N, S ), https://tc39.es/ecma262/#sec-object-environment-records-getbindingvalue-n-s
Value ObjectEnvironment::get_binding_value(GlobalObject& global_object, FlyString const& name, bool strict)
{
    auto& vm = this->vm();
    auto value = m_binding_object.has_property(name);
    if (vm.exception())
        return {};
    if (!value) {
        if (!strict)
            return js_undefined();

        global_object.vm().throw_exception<ReferenceError>(global_object, ErrorType::UnknownIdentifier, name);
        return {};
    }
    return m_binding_object.get(name);
}

// 9.1.1.2.7 DeleteBinding ( N ), https://tc39.es/ecma262/#sec-object-environment-records-deletebinding-n
bool ObjectEnvironment::delete_binding(GlobalObject&, FlyString const& name)
{
    return m_binding_object.internal_delete(name);
}

}
