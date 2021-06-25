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
    virtual FunctionEnvironmentRecord* create_environment_record(Function&) = 0;

    BoundFunction* bind(Value bound_this_value, Vector<Value> arguments);

    Value bound_this() const { return m_bound_this; }

    const Vector<Value>& bound_arguments() const { return m_bound_arguments; }

    Value home_object() const { return m_home_object; }
    void set_home_object(Value home_object) { m_home_object = home_object; }

    ConstructorKind constructor_kind() const { return m_constructor_kind; };
    void set_constructor_kind(ConstructorKind constructor_kind) { m_constructor_kind = constructor_kind; }

    virtual bool is_strict_mode() const { return false; }

    // [[Environment]]
    // The Environment Record that the function was closed over.
    // Used as the outer environment when evaluating the code of the function.
    virtual EnvironmentRecord* environment() { return nullptr; }

    enum class ThisMode : u8 {
        Lexical,
        Strict,
        Global,
    };

    // [[ThisMode]]
    ThisMode this_mode() const { return m_this_mode; }
    void set_this_mode(ThisMode this_mode) { m_this_mode = this_mode; }

protected:
    virtual void visit_edges(Visitor&) override;

    explicit Function(Object& prototype);
    Function(Value bound_this, Vector<Value> bound_arguments, Object& prototype);

private:
    virtual bool is_function() const override { return true; }
    Value m_bound_this;
    Vector<Value> m_bound_arguments;
    Value m_home_object;
    ConstructorKind m_constructor_kind = ConstructorKind::Base;
    ThisMode m_this_mode { ThisMode::Global };
};

}
