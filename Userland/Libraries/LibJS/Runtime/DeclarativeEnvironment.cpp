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
ThrowCompletionOr<bool> DeclarativeEnvironment::has_binding(FlyString const& name, Optional<size_t>* out_index) const
{
    auto it = m_names.find(name);
    if (it == m_names.end())
        return false;
    if (!is_permanently_screwed_by_eval() && out_index)
        *out_index = it->value;
    return true;
}

// 9.1.1.1.2 CreateMutableBinding ( N, D ), https://tc39.es/ecma262/#sec-declarative-environment-records-createmutablebinding-n-d
ThrowCompletionOr<void> DeclarativeEnvironment::create_mutable_binding(GlobalObject&, FlyString const& name, bool can_be_deleted)
{
    // 2. Create a mutable binding in envRec for N and record that it is uninitialized. If D is true, record that the newly created binding may be deleted by a subsequent DeleteBinding call.
    m_bindings.append(Binding {
        .value = {},
        .strict = false,
        .mutable_ = true,
        .can_be_deleted = can_be_deleted,
        .initialized = false,
    });
    auto result = m_names.set(name, m_bindings.size() - 1);

    // 1. Assert: envRec does not already have a binding for N.
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);

    // 3. Return NormalCompletion(empty).
    return {};
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
        (void)create_mutable_binding(global_object, name, true);
        initialize_binding(global_object, name, value);
        return;
    }

    set_mutable_binding_direct(global_object, it->value, value, strict);
}

void DeclarativeEnvironment::set_mutable_binding_direct(GlobalObject& global_object, size_t index, Value value, bool strict)
{
    auto& binding = m_bindings[index];
    if (binding.strict)
        strict = true;

    if (!binding.initialized) {
        global_object.vm().throw_exception<ReferenceError>(global_object, ErrorType::BindingNotInitialized, name_from_index(index));
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
Value DeclarativeEnvironment::get_binding_value(GlobalObject& global_object, FlyString const& name, bool strict)
{
    auto it = m_names.find(name);
    VERIFY(it != m_names.end());
    return get_binding_value_direct(global_object, it->value, strict);
}

Value DeclarativeEnvironment::get_binding_value_direct(GlobalObject& global_object, size_t index, bool)
{
    auto& binding = m_bindings[index];
    if (!binding.initialized) {
        global_object.vm().throw_exception<ReferenceError>(global_object, ErrorType::BindingNotInitialized, name_from_index(index));
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

FlyString const& DeclarativeEnvironment::name_from_index(size_t index) const
{
    for (auto& it : m_names) {
        if (it.value == index)
            return it.key;
    }
    VERIFY_NOT_REACHED();
}

}
