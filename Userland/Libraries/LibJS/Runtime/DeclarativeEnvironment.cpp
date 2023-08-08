/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/DeclarativeEnvironment.h>
#include <LibJS/Runtime/Error.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

DeclarativeEnvironment* DeclarativeEnvironment::create_for_per_iteration_bindings(Badge<ForStatement>, DeclarativeEnvironment& other, size_t bindings_size)
{
    auto bindings = other.m_bindings.span().slice(0, bindings_size);
    auto* parent_environment = other.outer_environment();

    return parent_environment->heap().allocate_without_realm<DeclarativeEnvironment>(parent_environment, bindings);
}

DeclarativeEnvironment::DeclarativeEnvironment()
    : Environment(nullptr)
{
}

DeclarativeEnvironment::DeclarativeEnvironment(Environment* parent_environment)
    : Environment(parent_environment)
{
}

DeclarativeEnvironment::DeclarativeEnvironment(Environment* parent_environment, ReadonlySpan<Binding> bindings)
    : Environment(parent_environment)
    , m_bindings(bindings)
{
}

void DeclarativeEnvironment::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& binding : m_bindings)
        visitor.visit(binding.value);

    for (auto& disposable : m_disposable_resource_stack) {
        visitor.visit(disposable.resource_value);
        visitor.visit(disposable.dispose_method);
    }
}

// 9.1.1.1.1 HasBinding ( N ), https://tc39.es/ecma262/#sec-declarative-environment-records-hasbinding-n
ThrowCompletionOr<bool> DeclarativeEnvironment::has_binding(DeprecatedFlyString const& name, Optional<size_t>* out_index) const
{
    auto binding_and_index = find_binding_and_index(name);
    if (!binding_and_index.has_value())
        return false;
    if (!is_permanently_screwed_by_eval() && out_index && binding_and_index->index().has_value())
        *out_index = *(binding_and_index->index());
    return true;
}

// 9.1.1.1.2 CreateMutableBinding ( N, D ), https://tc39.es/ecma262/#sec-declarative-environment-records-createmutablebinding-n-d
ThrowCompletionOr<void> DeclarativeEnvironment::create_mutable_binding(VM&, DeprecatedFlyString const& name, bool can_be_deleted)
{
    // 1. Assert: envRec does not already have a binding for N.
    // NOTE: We skip this to avoid O(n) traversal of m_bindings.

    // 2. Create a mutable binding in envRec for N and record that it is uninitialized. If D is true, record that the newly created binding may be deleted by a subsequent DeleteBinding call.
    m_bindings.append(Binding {
        .name = name,
        .value = {},
        .strict = false,
        .mutable_ = true,
        .can_be_deleted = can_be_deleted,
        .initialized = false,
    });

    ++m_environment_serial_number;

    // 3. Return unused.
    return {};
}

// 9.1.1.1.3 CreateImmutableBinding ( N, S ), https://tc39.es/ecma262/#sec-declarative-environment-records-createimmutablebinding-n-s
ThrowCompletionOr<void> DeclarativeEnvironment::create_immutable_binding(VM&, DeprecatedFlyString const& name, bool strict)
{
    // 1. Assert: envRec does not already have a binding for N.
    // NOTE: We skip this to avoid O(n) traversal of m_bindings.

    // 2. Create an immutable binding in envRec for N and record that it is uninitialized. If S is true, record that the newly created binding is a strict binding.
    m_bindings.append(Binding {
        .name = name,
        .value = {},
        .strict = strict,
        .mutable_ = false,
        .can_be_deleted = false,
        .initialized = false,
    });

    ++m_environment_serial_number;

    // 3. Return unused.
    return {};
}

// 9.1.1.1.4 InitializeBinding ( N, V ), https://tc39.es/ecma262/#sec-declarative-environment-records-initializebinding-n-v
// 4.1.1.1.1 InitializeBinding ( N, V, hint ), https://tc39.es/proposal-explicit-resource-management/#sec-declarative-environment-records
ThrowCompletionOr<void> DeclarativeEnvironment::initialize_binding(VM& vm, DeprecatedFlyString const& name, Value value, Environment::InitializeBindingHint hint)
{
    auto binding_and_index = find_binding_and_index(name);
    VERIFY(binding_and_index.has_value());
    auto& binding = binding_and_index->binding();

    // 1. Assert: envRec must have an uninitialized binding for N.
    VERIFY(binding.initialized == false);

    // 2. If hint is not normal, perform ? AddDisposableResource(envRec, V, hint).
    if (hint != Environment::InitializeBindingHint::Normal)
        TRY(add_disposable_resource(vm, m_disposable_resource_stack, value, hint));

    // 3. Set the bound value for N in envRec to V.
    binding.value = value;

    // 4. Record that the binding for N in envRec has been initialized.
    binding.initialized = true;

    // 5. Return unused.
    return {};
}

// 9.1.1.1.5 SetMutableBinding ( N, V, S ), https://tc39.es/ecma262/#sec-declarative-environment-records-setmutablebinding-n-v-s
ThrowCompletionOr<void> DeclarativeEnvironment::set_mutable_binding(VM& vm, DeprecatedFlyString const& name, Value value, bool strict)
{
    // 1. If envRec does not have a binding for N, then
    auto binding_and_index = find_binding_and_index(name);
    if (!binding_and_index.has_value()) {
        // a. If S is true, throw a ReferenceError exception.
        if (strict)
            return vm.throw_completion<ReferenceError>(ErrorType::UnknownIdentifier, name);

        // b. Perform ! envRec.CreateMutableBinding(N, true).
        MUST(create_mutable_binding(vm, name, true));

        // c. Perform ! envRec.InitializeBinding(N, V, normal).
        MUST(initialize_binding(vm, name, value, Environment::InitializeBindingHint::Normal));

        // d. Return unused.
        return {};
    }

    // 2-5. (extracted into a non-standard function below)
    TRY(set_mutable_binding_direct(vm, binding_and_index->binding(), value, strict));

    // 6. Return unused.
    return {};
}

ThrowCompletionOr<void> DeclarativeEnvironment::set_mutable_binding_direct(VM& vm, size_t index, Value value, bool strict)
{
    return set_mutable_binding_direct(vm, m_bindings[index], value, strict);
}

ThrowCompletionOr<void> DeclarativeEnvironment::set_mutable_binding_direct(VM& vm, Binding& binding, Value value, bool strict)
{
    if (binding.strict)
        strict = true;

    if (!binding.initialized)
        return vm.throw_completion<ReferenceError>(ErrorType::BindingNotInitialized, binding.name);

    if (binding.mutable_) {
        binding.value = value;
    } else {
        if (strict)
            return vm.throw_completion<TypeError>(ErrorType::InvalidAssignToConst);
    }

    return {};
}

// 9.1.1.1.6 GetBindingValue ( N, S ), https://tc39.es/ecma262/#sec-declarative-environment-records-getbindingvalue-n-s
ThrowCompletionOr<Value> DeclarativeEnvironment::get_binding_value(VM& vm, DeprecatedFlyString const& name, bool strict)
{
    // 1. Assert: envRec has a binding for N.
    auto binding_and_index = find_binding_and_index(name);
    VERIFY(binding_and_index.has_value());

    // 2-3. (extracted into a non-standard function below)
    return get_binding_value_direct(vm, binding_and_index->binding(), strict);
}

ThrowCompletionOr<Value> DeclarativeEnvironment::get_binding_value_direct(VM& vm, size_t index, bool strict)
{
    return get_binding_value_direct(vm, m_bindings[index], strict);
}

ThrowCompletionOr<Value> DeclarativeEnvironment::get_binding_value_direct(VM&, Binding& binding, bool)
{
    // 2. If the binding for N in envRec is an uninitialized binding, throw a ReferenceError exception.
    if (!binding.initialized)
        return vm().throw_completion<ReferenceError>(ErrorType::BindingNotInitialized, binding.name);

    // 3. Return the value currently bound to N in envRec.
    return binding.value;
}

// 9.1.1.1.7 DeleteBinding ( N ), https://tc39.es/ecma262/#sec-declarative-environment-records-deletebinding-n
ThrowCompletionOr<bool> DeclarativeEnvironment::delete_binding(VM&, DeprecatedFlyString const& name)
{
    // 1. Assert: envRec has a binding for the name that is the value of N.
    auto binding_and_index = find_binding_and_index(name);
    VERIFY(binding_and_index.has_value());

    // 2. If the binding for N in envRec cannot be deleted, return false.
    if (!binding_and_index->binding().can_be_deleted)
        return false;

    // 3. Remove the binding for N from envRec.
    // NOTE: We keep the entries in m_bindings to avoid disturbing indices.
    binding_and_index->binding() = {};

    ++m_environment_serial_number;

    // 4. Return true.
    return true;
}

ThrowCompletionOr<void> DeclarativeEnvironment::initialize_or_set_mutable_binding(VM& vm, DeprecatedFlyString const& name, Value value)
{
    auto binding_and_index = find_binding_and_index(name);
    VERIFY(binding_and_index.has_value());

    if (!binding_and_index->binding().initialized)
        TRY(initialize_binding(vm, name, value, Environment::InitializeBindingHint::Normal));
    else
        TRY(set_mutable_binding(vm, name, value, false));
    return {};
}

void DeclarativeEnvironment::initialize_or_set_mutable_binding(Badge<ScopeNode>, VM& vm, DeprecatedFlyString const& name, Value value)
{
    MUST(initialize_or_set_mutable_binding(vm, name, value));
}

void DeclarativeEnvironment::shrink_to_fit()
{
    m_bindings.shrink_to_fit();
}

}
