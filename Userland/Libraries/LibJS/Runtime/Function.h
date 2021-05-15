/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class Function : public Object {
    JS_OBJECT(Function, Object);

public:
    enum class ConstructorKind {
        Base,
        Derived,
    };

    virtual ~Function();
    virtual void initialize(GlobalObject&) override { }

    virtual Value call() = 0;
    virtual Value construct(Function& new_target) = 0;
    virtual const FlyString& name() const = 0;
    virtual LexicalEnvironment* create_environment() = 0;

    BoundFunction* bind(Value bound_this_value, Vector<Value> arguments);

    Value bound_this() const { return m_bound_this; }

    const Vector<Value>& bound_arguments() const { return m_bound_arguments; }

    Value home_object() const { return m_home_object; }
    void set_home_object(Value home_object) { m_home_object = home_object; }

    ConstructorKind constructor_kind() const { return m_constructor_kind; };
    void set_constructor_kind(ConstructorKind constructor_kind) { m_constructor_kind = constructor_kind; }

    virtual bool is_strict_mode() const { return false; }

protected:
    virtual void visit_edges(Visitor&) override;

    explicit Function(Object& prototype);
    Function(Object& prototype, Value bound_this, Vector<Value> bound_arguments);

private:
    virtual bool is_function() const override { return true; }
    Value m_bound_this;
    Vector<Value> m_bound_arguments;
    Value m_home_object;
    ConstructorKind m_constructor_kind = ConstructorKind::Base;
};

}
