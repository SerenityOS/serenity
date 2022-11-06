/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
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

DeclarativeEnvironmentBindings::~DeclarativeEnvironmentBindings() = default;

JS::NonnullGCPtr<DeclarativeEnvironmentBindings> DeclarativeEnvironmentBindings::make_unshared_copy()
{
    auto copy = heap().allocate_without_realm<DeclarativeEnvironmentBindings>();
    copy->m_shared = false;
    copy->m_bindings = m_bindings;
    return *copy;
}

DeclarativeEnvironment* DeclarativeEnvironment::create_for_per_iteration_bindings(Badge<ForStatement>, DeclarativeEnvironment& other, size_t bindings_size)
{
    auto* parent_environment = other.outer_environment();

    auto environment = parent_environment->heap().allocate_without_realm<DeclarativeEnvironment>(parent_environment);
    auto bindings = other.m_bindings->bindings().span().slice(0, bindings_size);
    auto values = other.m_values.span().slice(0, bindings_size);
    environment->m_bindings->bindings().append(bindings.data(), bindings.size());
    environment->m_values.append(values.data(), values.size());
    return environment;
}

DeclarativeEnvironment::DeclarativeEnvironment()
    : Environment(nullptr)
{
}

DeclarativeEnvironment::DeclarativeEnvironment(JS::GCPtr<Environment> parent_environment, JS::GCPtr<DeclarativeEnvironmentBindings> shared_bindings)
    : Environment(parent_environment)
    , m_bindings(shared_bindings)
{
}

void DeclarativeEnvironment::unshare_bindings_if_needed()
{
    if (!m_bindings->is_shared())
        return;
    m_bindings = m_bindings->make_unshared_copy();
}

void DeclarativeEnvironment::initialize_without_realm()
{
    Base::initialize_without_realm();
    if (!m_bindings)
        m_bindings = heap().allocate_without_realm<DeclarativeEnvironmentBindings>();
    m_values.resize(m_bindings->bindings().size());
}

void DeclarativeEnvironment::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_bindings);
    for (auto& value : m_values) {
        if (value.has_value())
            visitor.visit(*value);
    }
}

// 9.1.1.1.1 HasBinding ( N ), https://tc39.es/ecma262/#sec-declarative-environment-records-hasbinding-n
ThrowCompletionOr<bool> DeclarativeEnvironment::has_binding(FlyString const& name, Optional<size_t>* out_index) const
{
    auto binding_and_index = find_binding_and_index(name);
    if (!binding_and_index.has_value())
        return false;
    if (!is_permanently_screwed_by_eval() && out_index && binding_and_index->index().has_value())
        *out_index = *(binding_and_index->index());
    return true;
}

// 9.1.1.1.2 CreateMutableBinding ( N, D ), https://tc39.es/ecma262/#sec-declarative-environment-records-createmutablebinding-n-d
ThrowCompletionOr<void> DeclarativeEnvironment::create_mutable_binding(VM&, FlyString const& name, bool can_be_deleted)
{
    unshare_bindings_if_needed();

    // 1. Assert: envRec does not already have a binding for N.
    // NOTE: We skip this to avoid O(n) traversal of m_bindings.

    // 2. Create a mutable binding in envRec for N and record that it is uninitialized. If D is true, record that the newly created binding may be deleted by a subsequent DeleteBinding call.
    m_bindings->bindings().append(Binding {
        .name = name,
        .strict = false,
        .mutable_ = true,
        .can_be_deleted = can_be_deleted,
    });
    m_values.append({});

    // 3. Return unused.
    return {};
}

// 9.1.1.1.3 CreateImmutableBinding ( N, S ), https://tc39.es/ecma262/#sec-declarative-environment-records-createimmutablebinding-n-s
ThrowCompletionOr<void> DeclarativeEnvironment::create_immutable_binding(VM&, FlyString const& name, bool strict)
{
    unshare_bindings_if_needed();

    // 1. Assert: envRec does not already have a binding for N.
    // NOTE: We skip this to avoid O(n) traversal of m_bindings.

    // 2. Create an immutable binding in envRec for N and record that it is uninitialized. If S is true, record that the newly created binding is a strict binding.
    m_bindings->bindings().append(Binding {
        .name = name,
        .strict = strict,
        .mutable_ = false,
        .can_be_deleted = false,
    });
    m_values.append({});

    // 3. Return unused.
    return {};
}

// 9.1.1.1.4 InitializeBinding ( N, V ), https://tc39.es/ecma262/#sec-declarative-environment-records-initializebinding-n-v
ThrowCompletionOr<void> DeclarativeEnvironment::initialize_binding(VM& vm, FlyString const& name, Value value)
{
    auto binding_and_index = find_binding_and_index(name);
    VERIFY(binding_and_index.has_value());

    return initialize_binding_direct(vm, binding_and_index->index().value(), value);
}

ThrowCompletionOr<void> DeclarativeEnvironment::initialize_binding_direct(VM&, size_t index, Value value)
{
    // 1. Assert: envRec must have an uninitialized binding for N.
    VERIFY(m_values[index].has_value() == false);

    VERIFY(!value.is_empty());

    // 2. Set the bound value for N in envRec to V.
    // 3. Record that the binding for N in envRec has been initialized.
    // NOTE: We use the empty Optional<Value> to indicate uninitialized bindings.
    m_values[index] = value;

    // 4. Return unused.
    return {};
}

// 9.1.1.1.5 SetMutableBinding ( N, V, S ), https://tc39.es/ecma262/#sec-declarative-environment-records-setmutablebinding-n-v-s
ThrowCompletionOr<void> DeclarativeEnvironment::set_mutable_binding(VM& vm, FlyString const& name, Value value, bool strict)
{
    // 1. If envRec does not have a binding for N, then
    auto binding_and_index = find_binding_and_index(name);
    if (!binding_and_index.has_value()) {
        // a. If S is true, throw a ReferenceError exception.
        if (strict)
            return vm.throw_completion<ReferenceError>(ErrorType::UnknownIdentifier, name);

        // b. Perform ! envRec.CreateMutableBinding(N, true).
        MUST(create_mutable_binding(vm, name, true));

        // c. Perform ! envRec.InitializeBinding(N, V).
        MUST(initialize_binding(vm, name, value));

        // d. Return unused.
        return {};
    }

    // AD-HOC: This is here to deal with the awkward fake bindings from ModuleEnvironment.
    if (!binding_and_index->index().has_value()) {
        auto const& binding = binding_and_index->binding();
        if (binding.strict)
            strict = true;
        if (strict)
            return vm.throw_completion<TypeError>(ErrorType::InvalidAssignToConst);
        return {};
    }

    // 2-5. (extracted into a non-standard function below)
    TRY(set_mutable_binding_direct(vm, binding_and_index->index().value(), value, strict));

    // 6. Return unused.
    return {};
}

ThrowCompletionOr<void> DeclarativeEnvironment::set_mutable_binding_direct(VM& vm, size_t index, Value value, bool strict)
{
    auto const& binding = m_bindings->bindings()[index];
    if (binding.strict)
        strict = true;

    if (!m_values[index].has_value())
        return vm.throw_completion<ReferenceError>(ErrorType::BindingNotInitialized, binding.name);

    if (binding.mutable_) {
        VERIFY(!value.is_empty());
        m_values[index] = value;
    } else {
        if (strict)
            return vm.throw_completion<TypeError>(ErrorType::InvalidAssignToConst);
    }

    return {};
}

// 9.1.1.1.6 GetBindingValue ( N, S ), https://tc39.es/ecma262/#sec-declarative-environment-records-getbindingvalue-n-s
ThrowCompletionOr<Value> DeclarativeEnvironment::get_binding_value(VM& vm, FlyString const& name, bool strict)
{
    // 1. Assert: envRec has a binding for N.
    auto binding_and_index = find_binding_and_index(name);
    VERIFY(binding_and_index.has_value());

    // 2-3. (extracted into a non-standard function below)
    return get_binding_value_direct(vm, binding_and_index->index().value(), strict);
}

ThrowCompletionOr<Value> DeclarativeEnvironment::get_binding_value_direct(VM&, size_t index, bool)
{
    auto const& binding = m_bindings->bindings()[index];

    // 2. If the binding for N in envRec is an uninitialized binding, throw a ReferenceError exception.
    if (!m_values[index].has_value())
        return vm().throw_completion<ReferenceError>(ErrorType::BindingNotInitialized, binding.name);

    // 3. Return the value currently bound to N in envRec.
    return *m_values[index];
}

// 9.1.1.1.7 DeleteBinding ( N ), https://tc39.es/ecma262/#sec-declarative-environment-records-deletebinding-n
ThrowCompletionOr<bool> DeclarativeEnvironment::delete_binding(VM&, FlyString const& name)
{
    unshare_bindings_if_needed();

    // 1. Assert: envRec has a binding for the name that is the value of N.
    auto binding_and_index = find_binding_and_index(name);
    VERIFY(binding_and_index.has_value());

    // 2. If the binding for N in envRec cannot be deleted, return false.
    if (!binding_and_index->binding().can_be_deleted)
        return false;

    // 3. Remove the binding for N from envRec.
    // NOTE: We keep the entries in m_bindings to avoid disturbing indices.
    binding_and_index->binding() = {};

    // 4. Return true.
    return true;
}

ThrowCompletionOr<void> DeclarativeEnvironment::initialize_or_set_mutable_binding(VM& vm, FlyString const& name, Value value)
{
    auto binding_and_index = find_binding_and_index(name);
    VERIFY(binding_and_index.has_value());

    if (!m_values[binding_and_index->index().value()].has_value())
        TRY(initialize_binding(vm, name, value));
    else
        TRY(set_mutable_binding(vm, name, value, false));
    return {};
}

void DeclarativeEnvironment::initialize_or_set_mutable_binding(Badge<ScopeNode>, VM& vm, FlyString const& name, Value value)
{
    MUST(initialize_or_set_mutable_binding(vm, name, value));
}

Optional<DeclarativeEnvironment::BindingAndIndex> DeclarativeEnvironment::find_binding_and_index(FlyString const& name) const
{
    auto it = m_bindings->bindings().find_if([&](auto const& binding) {
        return binding.name == name;
    });

    if (it == m_bindings->bindings().end())
        return {};

    return BindingAndIndex { const_cast<Binding*>(&(*it)), it.index() };
}

}
