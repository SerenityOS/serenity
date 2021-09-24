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

    virtual bool is_strict_mode() const { return false; }

    // [[Environment]]
    // The Environment Record that the function was closed over.
    // Used as the outer environment when evaluating the code of the function.
    virtual Environment* environment() { return nullptr; }

    // [[Realm]]
    virtual Realm* realm() const { return nullptr; }

protected:
    explicit FunctionObject(Object& prototype);

private:
    virtual bool is_function() const override { return true; }
};

}
