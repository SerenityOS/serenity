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

    // Table 7: Additional Essential Internal Methods of Function Objects, https://tc39.es/ecma262/#table-additional-essential-internal-methods-of-function-objects

    virtual ThrowCompletionOr<Value> internal_call(Value this_argument, MarkedValueList arguments_list) = 0;
    virtual ThrowCompletionOr<Object*> internal_construct([[maybe_unused]] MarkedValueList arguments_list, [[maybe_unused]] FunctionObject& new_target) { VERIFY_NOT_REACHED(); }

    virtual const FlyString& name() const = 0;
    virtual FunctionEnvironment* new_function_environment(Object* new_target) = 0;

    BoundFunction* bind(Value bound_this_value, Vector<Value> arguments);

    virtual bool is_strict_mode() const { return false; }

    virtual bool has_constructor() const { return false; }

    // [[Realm]]
    virtual Realm* realm() const { return nullptr; }

protected:
    explicit FunctionObject(Object& prototype);

private:
    virtual bool is_function() const override { return true; }
};

}
