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
    GlobalEnvironment(GlobalObject&, Object& this_value);

    virtual bool has_this_binding() const final { return true; }
    virtual ThrowCompletionOr<Value> get_this_binding(GlobalObject&) const final;

    virtual ThrowCompletionOr<bool> has_binding(FlyString const& name, Optional<size_t>* = nullptr) const override;
    virtual ThrowCompletionOr<void> create_mutable_binding(GlobalObject&, FlyString const& name, bool can_be_deleted) override;
    virtual ThrowCompletionOr<void> create_immutable_binding(GlobalObject&, FlyString const& name, bool strict) override;
    virtual ThrowCompletionOr<void> initialize_binding(GlobalObject&, FlyString const& name, Value) override;
    virtual ThrowCompletionOr<void> set_mutable_binding(GlobalObject&, FlyString const& name, Value, bool strict) override;
    virtual ThrowCompletionOr<Value> get_binding_value(GlobalObject&, FlyString const& name, bool strict) override;
    virtual ThrowCompletionOr<bool> delete_binding(GlobalObject&, FlyString const& name) override;

    ObjectEnvironment& object_record() { return *m_object_record; }
    Object& global_this_value() { return *m_global_this_value; }
    DeclarativeEnvironment& declarative_record() { return *m_declarative_record; }

    bool has_var_declaration(FlyString const& name) const;
    bool has_lexical_declaration(FlyString const& name) const;
    bool has_restricted_global_property(FlyString const& name) const;
    bool can_declare_global_var(FlyString const& name) const;
    bool can_declare_global_function(FlyString const& name) const;
    void create_global_var_binding(FlyString const& name, bool can_be_deleted);
    void create_global_function_binding(FlyString const& name, Value, bool can_be_deleted);

private:
    virtual bool is_global_environment() const override { return true; }
    virtual void visit_edges(Visitor&) override;

    ObjectEnvironment* m_object_record { nullptr };           // [[ObjectRecord]]
    Object* m_global_this_value { nullptr };                  // [[GlobalThisValue]]
    DeclarativeEnvironment* m_declarative_record { nullptr }; // [[DeclarativeRecord]]
    Vector<FlyString> m_var_names;                            // [[VarNames]]
};

template<>
inline bool Environment::fast_is<GlobalEnvironment>() const { return is_global_environment(); }

}
