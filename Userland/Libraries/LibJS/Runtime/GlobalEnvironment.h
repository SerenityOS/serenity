/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Environment.h>

namespace JS {

class GlobalEnvironment final : public Environment {
    JS_ENVIRONMENT(GlobalEnvironment, Environment);

public:
    virtual bool has_this_binding() const final { return true; }
    virtual ThrowCompletionOr<Value> get_this_binding(VM&) const final;

    virtual ThrowCompletionOr<bool> has_binding(DeprecatedFlyString const& name, Optional<size_t>* = nullptr) const override;
    virtual ThrowCompletionOr<void> create_mutable_binding(VM&, DeprecatedFlyString const& name, bool can_be_deleted) override;
    virtual ThrowCompletionOr<void> create_immutable_binding(VM&, DeprecatedFlyString const& name, bool strict) override;
    virtual ThrowCompletionOr<void> initialize_binding(VM&, DeprecatedFlyString const& name, Value, Environment::InitializeBindingHint) override;
    virtual ThrowCompletionOr<void> set_mutable_binding(VM&, DeprecatedFlyString const& name, Value, bool strict) override;
    virtual ThrowCompletionOr<Value> get_binding_value(VM&, DeprecatedFlyString const& name, bool strict) override;
    virtual ThrowCompletionOr<bool> delete_binding(VM&, DeprecatedFlyString const& name) override;

    ObjectEnvironment& object_record() { return *m_object_record; }
    Object& global_this_value() { return *m_global_this_value; }
    DeclarativeEnvironment& declarative_record() { return *m_declarative_record; }

    bool has_var_declaration(DeprecatedFlyString const& name) const;
    bool has_lexical_declaration(DeprecatedFlyString const& name) const;
    ThrowCompletionOr<bool> has_restricted_global_property(DeprecatedFlyString const& name) const;
    ThrowCompletionOr<bool> can_declare_global_var(DeprecatedFlyString const& name) const;
    ThrowCompletionOr<bool> can_declare_global_function(DeprecatedFlyString const& name) const;
    ThrowCompletionOr<void> create_global_var_binding(DeprecatedFlyString const& name, bool can_be_deleted);
    ThrowCompletionOr<void> create_global_function_binding(DeprecatedFlyString const& name, Value, bool can_be_deleted);

private:
    GlobalEnvironment(Object&, Object& this_value);

    virtual bool is_global_environment() const override { return true; }
    virtual void visit_edges(Visitor&) override;

    GCPtr<ObjectEnvironment> m_object_record;           // [[ObjectRecord]]
    GCPtr<Object> m_global_this_value;                  // [[GlobalThisValue]]
    GCPtr<DeclarativeEnvironment> m_declarative_record; // [[DeclarativeRecord]]
    Vector<DeprecatedFlyString> m_var_names;            // [[VarNames]]
};

template<>
inline bool Environment::fast_is<GlobalEnvironment>() const { return is_global_environment(); }

}
