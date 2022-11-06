/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <LibJS/AST.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

// This holds the list of bindings in a DeclarativeEnvironment object, along with their metadata.
// As an optimization, the same DeclarativeEnvironmentBindings may be used by multiple
// DeclarativeEnvironment objects simultaneously, if we've determined that it's safe to do so.
class DeclarativeEnvironmentBindings final : public Cell {
    JS_CELL(DeclarativeEnvironmentBindings, Cell);

public:
    virtual ~DeclarativeEnvironmentBindings() override;

    struct Binding {
        FlyString name;
        bool strict { false };
        bool mutable_ { false };
        bool can_be_deleted { false };
    };

    // Returns true if these bindings are shareable.
    bool is_shared() const { return m_shared; }

    // Returns a newly-allocated copy of these bindings with sharing turned off.
    JS::NonnullGCPtr<DeclarativeEnvironmentBindings> make_unshared_copy();

    auto& bindings() { return m_bindings; }
    auto const& bindings() const { return m_bindings; }

private:
    bool m_shared { false };
    Vector<Binding> m_bindings;
};

class DeclarativeEnvironment : public Environment {
    JS_ENVIRONMENT(DeclarativeEnvironment, Environment);

public:
    using Binding = DeclarativeEnvironmentBindings::Binding;

    static DeclarativeEnvironment* create_for_per_iteration_bindings(Badge<ForStatement>, DeclarativeEnvironment& other, size_t bindings_size);

    virtual ~DeclarativeEnvironment() override = default;

    virtual ThrowCompletionOr<bool> has_binding(FlyString const& name, Optional<size_t>* = nullptr) const override;
    virtual ThrowCompletionOr<void> create_mutable_binding(VM&, FlyString const& name, bool can_be_deleted) override;
    virtual ThrowCompletionOr<void> create_immutable_binding(VM&, FlyString const& name, bool strict) override;
    virtual ThrowCompletionOr<void> initialize_binding(VM&, FlyString const& name, Value) override;
    virtual ThrowCompletionOr<void> set_mutable_binding(VM&, FlyString const& name, Value, bool strict) override;
    virtual ThrowCompletionOr<Value> get_binding_value(VM&, FlyString const& name, bool strict) override;
    virtual ThrowCompletionOr<bool> delete_binding(VM&, FlyString const& name) override;

    void initialize_or_set_mutable_binding(Badge<ScopeNode>, VM&, FlyString const& name, Value value);
    ThrowCompletionOr<void> initialize_or_set_mutable_binding(VM&, FlyString const& name, Value value);

    JS::NonnullGCPtr<DeclarativeEnvironmentBindings> bindings() { return *m_bindings; }
    Vector<Optional<Value>>& values() { return m_values; }

    ThrowCompletionOr<void> set_mutable_binding_direct(VM&, size_t index, Value, bool strict);
    ThrowCompletionOr<Value> get_binding_value_direct(VM&, size_t index, bool strict);

private:
    ThrowCompletionOr<void> initialize_binding_direct(VM&, size_t index, Value);

protected:
    DeclarativeEnvironment();
    DeclarativeEnvironment(JS::GCPtr<Environment> parent_environment, JS::GCPtr<DeclarativeEnvironmentBindings> cached_bindings = nullptr);

    virtual void initialize_without_realm() override;
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

    virtual Optional<BindingAndIndex> find_binding_and_index(FlyString const& name) const;

private:
    virtual bool is_declarative_environment() const override { return true; }

    void unshare_bindings_if_needed();

    JS::GCPtr<DeclarativeEnvironmentBindings> m_bindings;
    Vector<Optional<Value>> m_values;
};

template<>
inline bool Environment::fast_is<DeclarativeEnvironment>() const { return is_declarative_environment(); }

}
