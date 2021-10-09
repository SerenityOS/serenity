/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Environment.h>

namespace JS {

class ObjectEnvironment : public Environment {
    JS_ENVIRONMENT(ObjectEnvironment, Environment);

public:
    enum class IsWithEnvironment {
        No,
        Yes,
    };
    ObjectEnvironment(Object& binding_object, IsWithEnvironment, Environment* outer_environment);

    virtual ThrowCompletionOr<bool> has_binding(FlyString const& name, Optional<size_t>* = nullptr) const override;
    virtual ThrowCompletionOr<void> create_mutable_binding(GlobalObject&, FlyString const& name, bool can_be_deleted) override;
    virtual ThrowCompletionOr<void> create_immutable_binding(GlobalObject&, FlyString const& name, bool strict) override;
    virtual ThrowCompletionOr<void> initialize_binding(GlobalObject&, FlyString const& name, Value) override;
    virtual ThrowCompletionOr<void> set_mutable_binding(GlobalObject&, FlyString const& name, Value, bool strict) override;
    virtual ThrowCompletionOr<Value> get_binding_value(GlobalObject&, FlyString const& name, bool strict) override;
    virtual ThrowCompletionOr<bool> delete_binding(GlobalObject&, FlyString const& name) override;

    // 9.1.1.2.10 WithBaseObject ( ), https://tc39.es/ecma262/#sec-object-environment-records-withbaseobject
    virtual Object* with_base_object() const override
    {
        if (is_with_environment())
            return &m_binding_object;
        return nullptr;
    }

    // [[BindingObject]], The binding object of this Environment Record.
    Object& binding_object() { return m_binding_object; }

    // [[IsWithEnvironment]], Indicates whether this Environment Record is created for a with statement.
    bool is_with_environment() const { return m_with_environment; }

private:
    virtual void visit_edges(Visitor&) override;

    Object& m_binding_object;
    bool m_with_environment { false };
};

}
