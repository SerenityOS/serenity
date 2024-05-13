/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/HashMap.h>
#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class DeclarativeEnvironment : public Environment {
    JS_ENVIRONMENT(DeclarativeEnvironment, Environment);
    JS_DECLARE_ALLOCATOR(DeclarativeEnvironment);

    struct Binding {
        DeprecatedFlyString name;
        Value value;
        bool strict { false };
        bool mutable_ { false };
        bool can_be_deleted { false };
        bool initialized { false };
    };

public:
    static DeclarativeEnvironment* create_for_per_iteration_bindings(Badge<ForStatement>, DeclarativeEnvironment& other, size_t bindings_size);

    virtual ~DeclarativeEnvironment() override = default;

    virtual ThrowCompletionOr<bool> has_binding(DeprecatedFlyString const& name, Optional<size_t>* = nullptr) const override final;
    virtual ThrowCompletionOr<void> create_mutable_binding(VM&, DeprecatedFlyString const& name, bool can_be_deleted) override final;
    virtual ThrowCompletionOr<void> create_immutable_binding(VM&, DeprecatedFlyString const& name, bool strict) override final;
    virtual ThrowCompletionOr<void> initialize_binding(VM&, DeprecatedFlyString const& name, Value, InitializeBindingHint) override final;
    virtual ThrowCompletionOr<void> set_mutable_binding(VM&, DeprecatedFlyString const& name, Value, bool strict) override final;
    virtual ThrowCompletionOr<Value> get_binding_value(VM&, DeprecatedFlyString const& name, bool strict) override;
    virtual ThrowCompletionOr<bool> delete_binding(VM&, DeprecatedFlyString const& name) override;

    void initialize_or_set_mutable_binding(Badge<ScopeNode>, VM&, DeprecatedFlyString const& name, Value value);
    ThrowCompletionOr<void> initialize_or_set_mutable_binding(VM&, DeprecatedFlyString const& name, Value value);

    // This is not a method defined in the spec! Do not use this in any LibJS (or other spec related) code.
    [[nodiscard]] Vector<DeprecatedFlyString> bindings() const
    {
        Vector<DeprecatedFlyString> names;
        names.ensure_capacity(m_bindings.size());

        for (auto const& binding : m_bindings)
            names.unchecked_append(binding.name);

        return names;
    }

    ThrowCompletionOr<void> initialize_binding_direct(VM&, size_t index, Value, InitializeBindingHint);
    ThrowCompletionOr<void> set_mutable_binding_direct(VM&, size_t index, Value, bool strict);
    ThrowCompletionOr<Value> get_binding_value_direct(VM&, size_t index) const;

    void shrink_to_fit();

    void ensure_capacity(size_t needed_capacity)
    {
        m_bindings.ensure_capacity(needed_capacity);
    }

    [[nodiscard]] u64 environment_serial_number() const { return m_environment_serial_number; }

private:
    ThrowCompletionOr<Value> get_binding_value_direct(VM&, Binding const&) const;
    ThrowCompletionOr<void> set_mutable_binding_direct(VM&, Binding&, Value, bool strict);

    friend Completion dispose_resources(VM&, GCPtr<DeclarativeEnvironment>, Completion);
    Vector<DisposableResource> const& disposable_resource_stack() const { return m_disposable_resource_stack; }

protected:
    DeclarativeEnvironment();
    explicit DeclarativeEnvironment(Environment* parent_environment);
    DeclarativeEnvironment(Environment* parent_environment, ReadonlySpan<Binding> bindings);

    virtual void visit_edges(Visitor&) override;

    class BindingAndIndex {
    public:
        Binding& binding()
        {
            if (m_referenced_binding)
                return *m_referenced_binding;
            return m_temporary_binding;
        }

        BindingAndIndex(Binding* binding, Optional<size_t> index)
            : m_referenced_binding(binding)
            , m_index(move(index))
        {
        }

        explicit BindingAndIndex(Binding temporary_binding)
            : m_temporary_binding(move(temporary_binding))
        {
        }

        Optional<size_t> const& index() const { return m_index; }

    private:
        Binding* m_referenced_binding { nullptr };
        Binding m_temporary_binding {};
        Optional<size_t> m_index;
    };

    friend class ModuleEnvironment;

    virtual Optional<BindingAndIndex> find_binding_and_index(DeprecatedFlyString const& name) const
    {
        auto it = m_bindings.find_if([&](auto const& binding) {
            return binding.name == name;
        });

        if (it == m_bindings.end())
            return {};

        return BindingAndIndex { const_cast<Binding*>(&(*it)), it.index() };
    }

private:
    Vector<Binding> m_bindings;
    Vector<DisposableResource> m_disposable_resource_stack;

    u64 m_environment_serial_number { 0 };
};

inline ThrowCompletionOr<Value> DeclarativeEnvironment::get_binding_value_direct(VM& vm, size_t index) const
{
    return get_binding_value_direct(vm, m_bindings[index]);
}

inline ThrowCompletionOr<Value> DeclarativeEnvironment::get_binding_value_direct(VM&, Binding const& binding) const
{
    // 2. If the binding for N in envRec is an uninitialized binding, throw a ReferenceError exception.
    if (!binding.initialized)
        return vm().throw_completion<ReferenceError>(ErrorType::BindingNotInitialized, binding.name);

    // 3. Return the value currently bound to N in envRec.
    return binding.value;
}

template<>
inline bool Environment::fast_is<DeclarativeEnvironment>() const { return is_declarative_environment(); }

}
