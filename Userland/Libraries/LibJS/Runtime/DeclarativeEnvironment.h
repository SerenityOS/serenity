/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Environment.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class DeclarativeEnvironment : public Environment {
    JS_ENVIRONMENT(DeclarativeEnvironment, Environment);

public:
    DeclarativeEnvironment();
    explicit DeclarativeEnvironment(Environment* parent_scope);
    virtual ~DeclarativeEnvironment() override;

    virtual ThrowCompletionOr<bool> has_binding(FlyString const& name, Optional<size_t>* = nullptr) const override;
    virtual ThrowCompletionOr<void> create_mutable_binding(GlobalObject&, FlyString const& name, bool can_be_deleted) override;
    virtual ThrowCompletionOr<void> create_immutable_binding(GlobalObject&, FlyString const& name, bool strict) override;
    virtual ThrowCompletionOr<void> initialize_binding(GlobalObject&, FlyString const& name, Value) override;
    virtual ThrowCompletionOr<void> set_mutable_binding(GlobalObject&, FlyString const& name, Value, bool strict) override;
    virtual ThrowCompletionOr<Value> get_binding_value(GlobalObject&, FlyString const& name, bool strict) override;
    virtual ThrowCompletionOr<bool> delete_binding(GlobalObject&, FlyString const& name) override;

    void initialize_or_set_mutable_binding(Badge<ScopeNode>, GlobalObject& global_object, FlyString const& name, Value value);
    ThrowCompletionOr<void> initialize_or_set_mutable_binding(GlobalObject& global_object, FlyString const& name, Value value);

    // This is not a method defined in the spec! Do not use this in any LibJS (or other spec related) code.
    [[nodiscard]] Vector<String> bindings() const;

    ThrowCompletionOr<Value> get_binding_value_direct(GlobalObject&, size_t index, bool strict);
    ThrowCompletionOr<void> set_mutable_binding_direct(GlobalObject&, size_t index, Value, bool strict);

protected:
    virtual void visit_edges(Visitor&) override;

private:
    FlyString const& name_from_index(size_t) const;

    virtual bool is_declarative_environment() const override { return true; }

    struct Binding {
        Value value;
        bool strict { false };
        bool mutable_ { false };
        bool can_be_deleted { false };
        bool initialized { false };
    };

    HashMap<FlyString, size_t> m_names;
    Vector<Binding> m_bindings;
};

template<>
inline bool Environment::fast_is<DeclarativeEnvironment>() const { return is_declarative_environment(); }

}
