/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/FlyString.h>
#include <YAK/HashMap.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class DeclarativeEnvironment : public Environment {
    JS_ENVIRONMENT(DeclarativeEnvironment, Environment);

public:
    DeclarativeEnvironment();
    explicit DeclarativeEnvironment(Environment* parent_scope);
    DeclarativeEnvironment(HashMap<FlyString, Variable> variables, Environment* parent_scope);
    virtual ~DeclarativeEnvironment() override;

    // ^Environment
    virtual Optional<Variable> get_from_environment(FlyString const&) const override;
    virtual bool put_into_environment(FlyString const&, Variable) override;
    virtual bool delete_from_environment(FlyString const&) override;

    HashMap<FlyString, Variable> const& variables() const { return m_variables; }

    virtual bool has_binding(FlyString const& name) const override;
    virtual void create_mutable_binding(GlobalObject&, FlyString const& name, bool can_be_deleted) override;
    virtual void create_immutable_binding(GlobalObject&, FlyString const& name, bool strict) override;
    virtual void initialize_binding(GlobalObject&, FlyString const& name, Value) override;
    virtual void set_mutable_binding(GlobalObject&, FlyString const& name, Value, bool strict) override;
    virtual Value get_binding_value(GlobalObject&, FlyString const& name, bool strict) override;
    virtual bool delete_binding(GlobalObject&, FlyString const& name) override;

protected:
    virtual void visit_edges(Visitor&) override;

private:
    virtual bool is_declarative_environment() const override { return true; }

    HashMap<FlyString, Variable> m_variables;

    struct Binding {
        Value value;
        bool strict { false };
        bool mutable_ { false };
        bool can_be_deleted { false };
        bool initialized { false };
    };

    HashMap<FlyString, Binding> m_bindings;
};

template<>
inline bool Environment::fast_is<DeclarativeEnvironment>() const { return is_declarative_environment(); }

}
