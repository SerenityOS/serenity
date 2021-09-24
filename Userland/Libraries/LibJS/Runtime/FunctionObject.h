/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibJS/Runtime/Object.h>

namespace JS {

class FunctionObject : public Object {
    JS_OBJECT(Function, Object);

public:
    virtual ~FunctionObject();
    virtual void initialize(GlobalObject&) override { }

    virtual Value call() = 0;
    virtual Value construct(FunctionObject& new_target) = 0;
    virtual const FlyString& name() const = 0;
    virtual FunctionEnvironment* create_environment(FunctionObject&) = 0;

    BoundFunction* bind(Value bound_this_value, Vector<Value> arguments);

    Value bound_this() const { return m_bound_this; }

    const Vector<Value>& bound_arguments() const { return m_bound_arguments; }

    virtual bool is_strict_mode() const { return false; }

    // [[Environment]]
    // The Environment Record that the function was closed over.
    // Used as the outer environment when evaluating the code of the function.
    virtual Environment* environment() { return nullptr; }

    // [[Realm]]
    virtual Realm* realm() const { return nullptr; }

protected:
    virtual void visit_edges(Visitor&) override;

    explicit FunctionObject(Object& prototype);
    FunctionObject(Value bound_this, Vector<Value> bound_arguments, Object& prototype);

private:
    virtual bool is_function() const override { return true; }
    Value m_bound_this;
    Vector<Value> m_bound_arguments;
};

}
