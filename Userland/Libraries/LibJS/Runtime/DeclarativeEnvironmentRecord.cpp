/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Interpreter.h>
#include <LibJS/Runtime/DeclarativeEnvironmentRecord.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

DeclarativeEnvironmentRecord::DeclarativeEnvironmentRecord()
    : EnvironmentRecord(nullptr)
{
}

DeclarativeEnvironmentRecord::DeclarativeEnvironmentRecord(EnvironmentRecord* parent_scope)
    : EnvironmentRecord(parent_scope)
{
}

DeclarativeEnvironmentRecord::DeclarativeEnvironmentRecord(HashMap<FlyString, Variable> variables, EnvironmentRecord* parent_scope)
    : EnvironmentRecord(parent_scope)
    , m_variables(move(variables))
{
}

DeclarativeEnvironmentRecord::~DeclarativeEnvironmentRecord()
{
}

void DeclarativeEnvironmentRecord::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& it : m_variables)
        visitor.visit(it.value.value);
    for (auto& it : m_bindings)
        visitor.visit(it.value.value);
}

Optional<Variable> DeclarativeEnvironmentRecord::get_from_environment_record(FlyString const& name) const
{
    return m_variables.get(name);
}

void DeclarativeEnvironmentRecord::put_into_environment_record(FlyString const& name, Variable variable)
{
    m_variables.set(name, variable);
}

bool DeclarativeEnvironmentRecord::delete_from_environment_record(FlyString const& name)
{
    return m_variables.remove(name);
}

// 9.1.1.1.1 HasBinding ( N ), https://tc39.es/ecma262/#sec-declarative-environment-records-hasbinding-n
bool DeclarativeEnvironmentRecord::has_binding(FlyString const& name) const
{
    return m_bindings.contains(name);
}

// 9.1.1.1.2 CreateMutableBinding ( N, D ), https://tc39.es/ecma262/#sec-declarative-environment-records-createmutablebinding-n-d
void DeclarativeEnvironmentRecord::create_mutable_binding(GlobalObject&, FlyString const& name, bool can_be_deleted)
{
    auto result = m_bindings.set(name,
        Binding {
            .value = {},
            .strict = false,
            .mutable_ = true,
            .can_be_deleted = can_be_deleted,
            .initialized = false,
        });
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
}

// 9.1.1.1.3 CreateImmutableBinding ( N, S ), https://tc39.es/ecma262/#sec-declarative-environment-records-createimmutablebinding-n-s
void DeclarativeEnvironmentRecord::create_immutable_binding(GlobalObject&, FlyString const& name, bool strict)
{
    auto result = m_bindings.set(name,
        Binding {
            .value = {},
            .strict = strict,
            .mutable_ = false,
            .can_be_deleted = false,
            .initialized = false,
        });
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
}

// 9.1.1.1.4 InitializeBinding ( N, V ), https://tc39.es/ecma262/#sec-declarative-environment-records-initializebinding-n-v
void DeclarativeEnvironmentRecord::initialize_binding(GlobalObject&, FlyString const& name, Value value)
{
    auto it = m_bindings.find(name);
    VERIFY(it != m_bindings.end());
    VERIFY(it->value.initialized == false);
    it->value.value = value;
    it->value.initialized = true;
}

// 9.1.1.1.5 SetMutableBinding ( N, V, S ), https://tc39.es/ecma262/#sec-declarative-environment-records-setmutablebinding-n-v-s
void DeclarativeEnvironmentRecord::set_mutable_binding(GlobalObject& global_object, FlyString const& name, Value value, bool strict)
{
    auto it = m_bindings.find(name);
    if (it == m_bindings.end()) {
        if (strict) {
            global_object.vm().throw_exception<ReferenceError>(global_object, ErrorType::UnknownIdentifier, name);
            return;
        }
        create_mutable_binding(global_object, name, true);
        initialize_binding(global_object, name, value);
        return;
    }

    if (it->value.strict)
        strict = true;

    if (!it->value.initialized) {
        global_object.vm().throw_exception<ReferenceError>(global_object, ErrorType::BindingNotInitialized, name);
        return;
    }

    if (it->value.mutable_) {
        it->value.value = value;
    } else {
        if (strict) {
            global_object.vm().throw_exception<TypeError>(global_object, ErrorType::InvalidAssignToConst);
        }
    }
}

// 9.1.1.1.6 GetBindingValue ( N, S ), https://tc39.es/ecma262/#sec-declarative-environment-records-getbindingvalue-n-s
Value DeclarativeEnvironmentRecord::get_binding_value(GlobalObject& global_object, FlyString const& name, bool)
{
    auto it = m_bindings.find(name);
    VERIFY(it != m_bindings.end());
    if (!it->value.initialized) {
        global_object.vm().throw_exception<ReferenceError>(global_object, ErrorType::BindingNotInitialized, name);
        return {};
    }
    return it->value.value;
}

// 9.1.1.1.7 DeleteBinding ( N ), https://tc39.es/ecma262/#sec-declarative-environment-records-deletebinding-n
bool DeclarativeEnvironmentRecord::delete_binding(GlobalObject&, FlyString const& name)
{
    auto it = m_bindings.find(name);
    VERIFY(it != m_bindings.end());
    if (!it->value.can_be_deleted)
        return false;
    m_bindings.remove(it);
    return true;
}

}
