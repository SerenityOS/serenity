/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/DeclarativeEnvironment.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

DeclarativeEnvironment::DeclarativeEnvironment()
    : Environment(nullptr)
{
}

DeclarativeEnvironment::DeclarativeEnvironment(Environment* parent_scope)
    : Environment(parent_scope)
{
}

DeclarativeEnvironment::~DeclarativeEnvironment()
{
}

void DeclarativeEnvironment::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& binding : m_bindings)
        visitor.visit(binding.value);
}

// 9.1.1.1.1 HasBinding ( N ), https://tc39.es/ecma262/#sec-declarative-environment-records-hasbinding-n
bool DeclarativeEnvironment::has_binding(FlyString const& name) const
{
    return m_names.contains(name);
}

// 9.1.1.1.2 CreateMutableBinding ( N, D ), https://tc39.es/ecma262/#sec-declarative-environment-records-createmutablebinding-n-d
void DeclarativeEnvironment::create_mutable_binding(GlobalObject&, FlyString const& name, bool can_be_deleted)
{
    m_bindings.append(Binding {
        .value = {},
        .strict = false,
        .mutable_ = true,
        .can_be_deleted = can_be_deleted,
        .initialized = false,
    });
    auto result = m_names.set(name, m_bindings.size() - 1);
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
}

// 9.1.1.1.3 CreateImmutableBinding ( N, S ), https://tc39.es/ecma262/#sec-declarative-environment-records-createimmutablebinding-n-s
void DeclarativeEnvironment::create_immutable_binding(GlobalObject&, FlyString const& name, bool strict)
{
    m_bindings.append(Binding {
        .value = {},
        .strict = strict,
        .mutable_ = false,
        .can_be_deleted = false,
        .initialized = false,
    });
    auto result = m_names.set(name, m_bindings.size() - 1);
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
}

// 9.1.1.1.4 InitializeBinding ( N, V ), https://tc39.es/ecma262/#sec-declarative-environment-records-initializebinding-n-v
void DeclarativeEnvironment::initialize_binding(GlobalObject&, FlyString const& name, Value value)
{
    auto it = m_names.find(name);
    VERIFY(it != m_names.end());
    auto& binding = m_bindings[it->value];
    VERIFY(binding.initialized == false);
    binding.value = value;
    binding.initialized = true;
}

// 9.1.1.1.5 SetMutableBinding ( N, V, S ), https://tc39.es/ecma262/#sec-declarative-environment-records-setmutablebinding-n-v-s
void DeclarativeEnvironment::set_mutable_binding(GlobalObject& global_object, FlyString const& name, Value value, bool strict)
{
    auto it = m_names.find(name);
    if (it == m_names.end()) {
        if (strict) {
            global_object.vm().throw_exception<ReferenceError>(global_object, ErrorType::UnknownIdentifier, name);
            return;
        }
        create_mutable_binding(global_object, name, true);
        initialize_binding(global_object, name, value);
        return;
    }

    auto& binding = m_bindings[it->value];
    if (binding.strict)
        strict = true;

    if (!binding.initialized) {
        global_object.vm().throw_exception<ReferenceError>(global_object, ErrorType::BindingNotInitialized, name);
        return;
    }

    if (binding.mutable_) {
        binding.value = value;
    } else {
        if (strict) {
            global_object.vm().throw_exception<TypeError>(global_object, ErrorType::InvalidAssignToConst);
        }
    }
}

// 9.1.1.1.6 GetBindingValue ( N, S ), https://tc39.es/ecma262/#sec-declarative-environment-records-getbindingvalue-n-s
Value DeclarativeEnvironment::get_binding_value(GlobalObject& global_object, FlyString const& name, bool)
{
    auto it = m_names.find(name);
    VERIFY(it != m_names.end());
    auto& binding = m_bindings[it->value];
    if (!binding.initialized) {
        global_object.vm().throw_exception<ReferenceError>(global_object, ErrorType::BindingNotInitialized, name);
        return {};
    }
    return binding.value;
}

// 9.1.1.1.7 DeleteBinding ( N ), https://tc39.es/ecma262/#sec-declarative-environment-records-deletebinding-n
bool DeclarativeEnvironment::delete_binding(GlobalObject&, FlyString const& name)
{
    auto it = m_names.find(name);
    VERIFY(it != m_names.end());
    auto& binding = m_bindings[it->value];
    if (!binding.can_be_deleted)
        return false;
    // NOTE: We keep the entry in m_bindings to avoid disturbing indices.
    binding = {};
    m_names.remove(it);
    return true;
}

void DeclarativeEnvironment::initialize_or_set_mutable_binding(Badge<ScopeNode>, GlobalObject& global_object, FlyString const& name, Value value)
{
    auto it = m_names.find(name);
    VERIFY(it != m_names.end());
    auto& binding = m_bindings[it->value];
    if (!binding.initialized)
        initialize_binding(global_object, name, value);
    else
        set_mutable_binding(global_object, name, value, false);
}

Vector<String> DeclarativeEnvironment::bindings() const
{
    Vector<String> names;
    for (auto& it : m_names) {
        names.append(it.key);
    }
    return names;
}

}
